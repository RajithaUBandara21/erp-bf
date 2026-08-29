package ERP.erpbackend.audit;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.identity.User;
import ERP.erpbackend.identity.UserRepository;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

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

	private User newUser(Tenant tenant, Organization organization, String email) {
		User user = new User();
		user.setTenantId(tenant.getId());
		user.setOrganizationId(organization.getId());
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName("Audit Actor");
		return userRepository.saveAndFlush(user);
	}

	@Test
	void savesAndFindsAuditLogWithAllFieldsAndJsonSnapshotsIntact() {
		Tenant tenant = newTenant("TEN-AUD-1");
		Organization organization = newOrganization(tenant, "ORG-AUD-1");
		User user = newUser(tenant, organization, "actor@acme.test");
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

}
