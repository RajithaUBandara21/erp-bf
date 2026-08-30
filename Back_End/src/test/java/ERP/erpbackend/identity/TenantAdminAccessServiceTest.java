package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TenantAdminAccessServiceTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private TenantAdminAccessService tenantAdminAccessService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MembershipRepository membershipRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private record Fixture(UUID tenantId, UUID userId, UUID homeOrgId) {
	}

	private Fixture registerTenantAdmin(String email) {
		TokenResponse account = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return new Fixture(account.tenantId(), account.userId(), account.organizationId());
	}

	private UUID siblingOrg(UUID tenantId, String code) {
		Organization organization = new Organization();
		organization.setTenantId(tenantId);
		organization.setName(code);
		organization.setCode(code);
		return organizationRepository.save(organization).getId();
	}

	private UUID addPlainMember(UUID tenantId, UUID organizationId, String email) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName(email);
		user = userRepository.save(user);

		Membership membership = new Membership();
		membership.setUserId(user.getId());
		membership.setTenantId(tenantId);
		membership.setOrganizationId(organizationId);
		membership.setStatus(MembershipStatus.ACTIVE);
		membershipRepository.save(membership);
		return user.getId();
	}

	private List<AuditLog> autoProvisionLogs(UUID membershipId) {
		return auditLogRepository.findAll().stream()
				.filter(log -> "membership.auto_provisioned".equals(log.getAction())
						&& membershipId.equals(log.getEntityId()))
				.toList();
	}

	private long ownerRoleAssignmentCount(UUID tenantId, UUID membershipId) {
		UUID ownerRoleId = roleRepository.findByTenantIdAndName(tenantId, "Owner").orElseThrow().getId();
		return userRoleRepository.findByMembershipId(membershipId).stream()
				.filter(assignment -> assignment.getRoleId().equals(ownerRoleId))
				.count();
	}

	@Test
	void tenantAdminReachingASiblingOrgGetsANewOwnerMembershipAndOneAuditRow() {
		Fixture fixture = registerTenantAdmin("ta-reach@acme.test");
		UUID siblingOrgId = siblingOrg(fixture.tenantId(), "ta-reach-sibling");

		Membership provisioned = tenantAdminAccessService.ensureOwnerMembership(fixture.userId(), siblingOrgId);

		assertThat(provisioned.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(provisioned.getTenantId()).isEqualTo(fixture.tenantId());
		assertThat(provisioned.getOrganizationId()).isEqualTo(siblingOrgId);
		assertThat(ownerRoleAssignmentCount(fixture.tenantId(), provisioned.getId())).isEqualTo(1);
		assertThat(autoProvisionLogs(provisioned.getId())).singleElement().satisfies(log -> {
			assertThat(log.getEntityType()).isEqualTo("Membership");
			assertThat(log.getUserId()).isEqualTo(fixture.userId());
			assertThat(log.getBeforeValue()).isNull();
			assertThat(log.getAfterValue()).contains("TENANT_ADMIN");
		});
	}

	@Test
	void aSecondCallReturnsTheSameMembershipWithoutASecondAuditRow() {
		Fixture fixture = registerTenantAdmin("ta-idempotent@acme.test");
		UUID siblingOrgId = siblingOrg(fixture.tenantId(), "ta-idempotent-sibling");

		Membership first = tenantAdminAccessService.ensureOwnerMembership(fixture.userId(), siblingOrgId);
		Membership second = tenantAdminAccessService.ensureOwnerMembership(fixture.userId(), siblingOrgId);

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(autoProvisionLogs(first.getId())).hasSize(1);
		assertThat(ownerRoleAssignmentCount(fixture.tenantId(), first.getId())).isEqualTo(1);
	}

	@Test
	void aCallerWhoIsNotATenantAdminGetsForbiddenAndNoMembershipIsCreated() {
		Fixture fixture = registerTenantAdmin("ta-forbidden@acme.test");
		UUID siblingOrgId = siblingOrg(fixture.tenantId(), "ta-forbidden-sibling");
		UUID plainMemberId = addPlainMember(fixture.tenantId(), fixture.homeOrgId(), "plain@ta-forbidden.test");

		assertThatThrownBy(() -> tenantAdminAccessService.ensureOwnerMembership(plainMemberId, siblingOrgId))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		assertThat(membershipRepository.findByUserIdAndOrganizationId(plainMemberId, siblingOrgId)).isEmpty();
	}

	@Test
	void anUnknownOrganizationIdGetsNotFound() {
		Fixture fixture = registerTenantAdmin("ta-unknown-org@acme.test");

		assertThatThrownBy(() -> tenantAdminAccessService.ensureOwnerMembership(fixture.userId(), UUID.randomUUID()))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void isTenantAdminStaysTrueForTheHomeTenantAfterReachingASiblingOrg() {
		Fixture fixture = registerTenantAdmin("ta-still-admin@acme.test");
		UUID siblingOrgId = siblingOrg(fixture.tenantId(), "ta-still-admin-sibling");

		assertThat(tenantAdminAccessService.isTenantAdmin(fixture.userId(), fixture.tenantId())).isTrue();
		tenantAdminAccessService.ensureOwnerMembership(fixture.userId(), siblingOrgId);
		assertThat(tenantAdminAccessService.isTenantAdmin(fixture.userId(), fixture.tenantId())).isTrue();
	}

	@Test
	void isTenantAdminIsFalseForAMemberWithoutTheTenantAdminRole() {
		Fixture fixture = registerTenantAdmin("ta-negative@acme.test");
		UUID plainMemberId = addPlainMember(fixture.tenantId(), fixture.homeOrgId(), "plain@ta-negative.test");

		assertThat(tenantAdminAccessService.isTenantAdmin(plainMemberId, fixture.tenantId())).isFalse();
	}

}
