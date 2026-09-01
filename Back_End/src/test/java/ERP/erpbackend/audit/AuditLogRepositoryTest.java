package ERP.erpbackend.audit;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.identity.User;
import ERP.erpbackend.identity.UserRepository;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class AuditLogRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	private Tenant newTenant(String code) {
		Tenant tenant = new Tenant();
		tenant.setName(code);
		tenant.setCode(code);
		return tenantRepository.saveAndFlush(tenant);
	}

	private Organization newOrganization(Tenant tenant, String code) {
		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName(code);
		organization.setCode(code);
		return organizationRepository.saveAndFlush(organization);
	}

	private User newUser(String email) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName("Audit Actor");
		return userRepository.saveAndFlush(user);
	}

	@Test
	void savesAndFindsAuditLogWithAllFieldsAndJsonSnapshotsIntact() {
		Tenant tenant = newTenant("TEN-AUD-1");
		Organization organization = newOrganization(tenant, "ORG-AUD-1");
		User user = newUser("actor@acme.test");
		UUID entityId = UUID.randomUUID();

		AuditLog auditLog = new AuditLog();
		auditLog.setTenantId(tenant.getId());
		auditLog.setOrganizationId(organization.getId());
		auditLog.setUserId(user.getId());
		auditLog.setEntityType("User");
		auditLog.setEntityId(entityId);
		auditLog.setAction("user.role_changed");
		auditLog.setBeforeValue("{\"role\":\"MEMBER\"}");
		auditLog.setAfterValue("{\"role\":\"ADMIN\"}");

		AuditLog saved = auditLogRepository.saveAndFlush(auditLog);

		AuditLog found = auditLogRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getOrganizationId()).isEqualTo(organization.getId());
		assertThat(found.getUserId()).isEqualTo(user.getId());
		assertThat(found.getEntityType()).isEqualTo("User");
		assertThat(found.getEntityId()).isEqualTo(entityId);
		assertThat(found.getAction()).isEqualTo("user.role_changed");
		assertThat(found.getBeforeValue()).isEqualTo("{\"role\":\"MEMBER\"}");
		assertThat(found.getAfterValue()).isEqualTo("{\"role\":\"ADMIN\"}");
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void savesAuditLogWithoutOrganizationActorOrTargetOrSnapshots() {
		Tenant tenant = newTenant("TEN-AUD-2");

		AuditLog auditLog = new AuditLog();
		auditLog.setTenantId(tenant.getId());
		auditLog.setEntityType("Session");
		auditLog.setAction("auth.login_failed");

		AuditLog saved = auditLogRepository.saveAndFlush(auditLog);

		AuditLog found = auditLogRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getOrganizationId()).isNull();
		assertThat(found.getUserId()).isNull();
		assertThat(found.getEntityId()).isNull();
		assertThat(found.getBeforeValue()).isNull();
		assertThat(found.getAfterValue()).isNull();
		assertThat(found.getAction()).isEqualTo("auth.login_failed");
	}

	private static AuditLogFilter noFilter() {
		return new AuditLogFilter(null, null, null, null, null);
	}

	private AuditLog newAuditLog(UUID tenantId, UUID userId, String entityType, String action, Instant createdAt) {
		return newAuditLog(tenantId, null, userId, entityType, action, createdAt);
	}

	private AuditLog newAuditLog(UUID tenantId, UUID organizationId, UUID userId, String entityType, String action,
			Instant createdAt) {
		AuditLog auditLog = new AuditLog();
		auditLog.setTenantId(tenantId);
		auditLog.setOrganizationId(organizationId);
		auditLog.setUserId(userId);
		auditLog.setEntityType(entityType);
		auditLog.setAction(action);
		AuditLog saved = auditLogRepository.saveAndFlush(auditLog);
		// created_at is @CreatedDate/updatable=false, so Hibernate won't write it
		// on an update - backdate it with a raw statement instead, then evict the
		// persistence context so later queries in the test see the new value.
		jdbcTemplate.update("UPDATE audit_logs SET created_at = ? WHERE id = ?", Timestamp.from(createdAt),
				saved.getId());
		entityManager.clear();
		return saved;
	}

	@Test
	void searchFiltersByEntityTypeActionActorAndDateRangeWithinTenant() {
		Tenant tenant = newTenant("TEN-AUD-3");
		User actor = newUser("actor3@acme.test");
		Tenant otherTenant = newTenant("TEN-AUD-4");

		Instant day1 = Instant.parse("2026-08-01T00:00:00Z");
		Instant day2 = Instant.parse("2026-08-02T00:00:00Z");
		Instant day3 = Instant.parse("2026-08-03T00:00:00Z");

		AuditLog roleCreated = newAuditLog(tenant.getId(), actor.getId(), "Role", "role.created", day1);
		AuditLog roleUpdated = newAuditLog(tenant.getId(), actor.getId(), "Role", "role.updated", day2);
		AuditLog login = newAuditLog(tenant.getId(), actor.getId(), "Session", "auth.login", day3);
		newAuditLog(otherTenant.getId(), null, "Role", "role.created", day1);

		Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<AuditLog> byEntityType = auditLogRepository.search(tenant.getId(), null,
				new AuditLogFilter("Role", null, null, null, null), pageable);
		assertThat(byEntityType.getContent()).extracting(AuditLog::getId)
				.containsExactly(roleUpdated.getId(), roleCreated.getId());

		Page<AuditLog> byAction = auditLogRepository.search(tenant.getId(), null,
				new AuditLogFilter(null, "auth.login", null, null, null), pageable);
		assertThat(byAction.getContent()).extracting(AuditLog::getId).containsExactly(login.getId());

		Page<AuditLog> byActor = auditLogRepository.search(tenant.getId(), null,
				new AuditLogFilter(null, null, actor.getId(), null, null), pageable);
		assertThat(byActor.getContent()).hasSize(3);

		Page<AuditLog> byDateRange = auditLogRepository.search(tenant.getId(), null,
				new AuditLogFilter(null, null, null, day2, day3), pageable);
		assertThat(byDateRange.getContent()).extracting(AuditLog::getId)
				.containsExactly(login.getId(), roleUpdated.getId());

		Page<AuditLog> combined = auditLogRepository.search(tenant.getId(), null,
				new AuditLogFilter("Role", "role.updated", actor.getId(), day1, day3), pageable);
		assertThat(combined.getContent()).extracting(AuditLog::getId).containsExactly(roleUpdated.getId());

		Page<AuditLog> noMatch = auditLogRepository.search(tenant.getId(), null,
				new AuditLogFilter("Product", null, null, null, null), pageable);
		assertThat(noMatch.getContent()).isEmpty();

		Page<AuditLog> allForTenant = auditLogRepository.search(tenant.getId(), null, noFilter(), pageable);
		assertThat(allForTenant.getContent()).extracting(AuditLog::getId)
				.containsExactly(login.getId(), roleUpdated.getId(), roleCreated.getId());
	}

	@Test
	void searchScopedToAnOrganizationExcludesSiblingOrganizationRowsUnderTheSameTenant() {
		Tenant tenant = newTenant("TEN-AUD-ORG");
		Organization orgA = newOrganization(tenant, "ORG-AUD-A");
		Organization orgB = newOrganization(tenant, "ORG-AUD-B");

		Instant day1 = Instant.parse("2026-08-01T00:00:00Z");
		Instant day2 = Instant.parse("2026-08-02T00:00:00Z");
		Instant day3 = Instant.parse("2026-08-03T00:00:00Z");

		AuditLog orgARow = newAuditLog(tenant.getId(), orgA.getId(), null, "Role", "role.created", day1);
		AuditLog orgBRow = newAuditLog(tenant.getId(), orgB.getId(), null, "Role", "role.created", day2);
		AuditLog tenantLevelRow = newAuditLog(tenant.getId(), null, null, "Session", "auth.login", day3);

		Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<AuditLog> scopedToOrgA = auditLogRepository.search(tenant.getId(), orgA.getId(), noFilter(), pageable);
		assertThat(scopedToOrgA.getContent()).extracting(AuditLog::getId).containsExactly(orgARow.getId());

		Page<AuditLog> tenantWide = auditLogRepository.search(tenant.getId(), null, noFilter(), pageable);
		assertThat(tenantWide.getContent()).extracting(AuditLog::getId)
				.containsExactly(tenantLevelRow.getId(), orgBRow.getId(), orgARow.getId());
	}

	@Test
	void searchPaginatesInDescendingCreatedAtOrder() {
		Tenant tenant = newTenant("TEN-AUD-5");
		Instant day1 = Instant.parse("2026-08-01T00:00:00Z");
		Instant day2 = Instant.parse("2026-08-02T00:00:00Z");
		Instant day3 = Instant.parse("2026-08-03T00:00:00Z");

		AuditLog first = newAuditLog(tenant.getId(), null, "Role", "role.created", day1);
		AuditLog second = newAuditLog(tenant.getId(), null, "Role", "role.created", day2);
		AuditLog third = newAuditLog(tenant.getId(), null, "Role", "role.created", day3);

		Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
		Page<AuditLog> pageOne = auditLogRepository.search(tenant.getId(), null, noFilter(),
				PageRequest.of(0, 2, sort));
		assertThat(pageOne.getContent()).extracting(AuditLog::getId).containsExactly(third.getId(), second.getId());
		assertThat(pageOne.getTotalElements()).isEqualTo(3);
		assertThat(pageOne.getTotalPages()).isEqualTo(2);

		Page<AuditLog> pageTwo = auditLogRepository.search(tenant.getId(), null, noFilter(),
				PageRequest.of(1, 2, sort));
		assertThat(pageTwo.getContent()).extracting(AuditLog::getId).containsExactly(first.getId());
	}

}
