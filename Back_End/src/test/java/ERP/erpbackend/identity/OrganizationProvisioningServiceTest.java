package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationDetail;
import ERP.erpbackend.organization.OrganizationListView;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
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
class OrganizationProvisioningServiceTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private OrganizationProvisioningService organizationProvisioningService;

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
	private TenantRepository tenantRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private record Fixture(UUID tenantId, UUID userId, UUID homeOrgId) {
	}

	private Fixture registerTenantAdmin(String email) {
		TokenResponse account = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return new Fixture(account.tenantId(), account.userId(), account.organizationId());
	}

	private UUID siblingOrg(UUID tenantId, String code, boolean active) {
		Organization organization = new Organization();
		organization.setTenantId(tenantId);
		organization.setName(code);
		organization.setCode(code);
		organization.setActive(active);
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

	private AuthenticatedUser caller(UUID userId, UUID tenantId, UUID organizationId) {
		return new AuthenticatedUser(userId, tenantId, organizationId, "caller@acme.test",
				UUID.randomUUID(), UUID.randomUUID());
	}

	private void raiseOrganizationLimit(UUID tenantId, int max) {
		Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
		tenant.setMaxOrganizations(max);
		tenantRepository.save(tenant);
	}

	private List<AuditLog> auditRows(String action, UUID entityId) {
		return auditLogRepository.findAll().stream()
				.filter(log -> action.equals(log.getAction()) && entityId.equals(log.getEntityId()))
				.toList();
	}

	@Test
	void tenantAdminListsEveryOrganizationUnderTheTenantIncludingInactiveOnesWithPlanAndLimit() {
		Fixture fixture = registerTenantAdmin("op-list@acme.test");
		siblingOrg(fixture.tenantId(), "op-list-active", true);
		UUID inactiveId = siblingOrg(fixture.tenantId(), "op-list-inactive", false);

		OrganizationListView view = organizationProvisioningService.list(
				caller(fixture.userId(), fixture.tenantId(), fixture.homeOrgId()));

		assertThat(view.maxOrganizations()).isEqualTo(1);
		assertThat(view.plan()).isNull();
		assertThat(view.organizations()).hasSize(3);
		assertThat(view.organizations()).anySatisfy(org -> {
			assertThat(org.id()).isEqualTo(inactiveId);
			assertThat(org.active()).isFalse();
		});
	}

	@Test
	void aPlainMemberCannotListTheTenantOrganizations() {
		Fixture fixture = registerTenantAdmin("op-list-forbidden@acme.test");
		UUID plainMemberId = addPlainMember(fixture.tenantId(), fixture.homeOrgId(), "plain@op-list-forbidden.test");

		assertThatThrownBy(() -> organizationProvisioningService.list(
				caller(plainMemberId, fixture.tenantId(), fixture.homeOrgId())))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void tenantAdminCreatesASecondOrganizationWithAnActiveOwnerMembershipAndAuditTrail() {
		Fixture fixture = registerTenantAdmin("op-create@acme.test");
		raiseOrganizationLimit(fixture.tenantId(), 2);

		OrganizationDetail created = organizationProvisioningService.create(
				caller(fixture.userId(), fixture.tenantId(), fixture.homeOrgId()), "Colombo Warehouse");

		Organization row = organizationRepository.findById(created.id()).orElseThrow();
		assertThat(row.getTenantId()).isEqualTo(fixture.tenantId());
		assertThat(row.isActive()).isTrue();
		assertThat(row.getCode()).isEqualTo("colombo-warehouse");

		Membership membership = membershipRepository
				.findByUserIdAndOrganizationId(fixture.userId(), created.id()).orElseThrow();
		assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		UUID ownerRoleId = roleRepository.findByTenantIdAndName(fixture.tenantId(), "Owner").orElseThrow().getId();
		assertThat(userRoleRepository.existsByMembershipIdAndRoleId(membership.getId(), ownerRoleId)).isTrue();

		assertThat(auditRows("organization.created", created.id())).singleElement().satisfies(log -> {
			assertThat(log.getEntityType()).isEqualTo("Organization");
			assertThat(log.getUserId()).isEqualTo(fixture.userId());
			assertThat(log.getBeforeValue()).isNull();
			assertThat(log.getAfterValue()).contains("colombo-warehouse");
		});
		assertThat(auditRows("membership.auto_provisioned", membership.getId())).hasSize(1);
	}

	@Test
	void creationIsRejectedWhenTheTenantIsAtItsOrganizationLimitAndNoOrgRowIsCreated() {
		Fixture fixture = registerTenantAdmin("op-create-limit@acme.test");
		// maxOrganizations defaults to 1 and registration already created the home org.

		assertThatThrownBy(() -> organizationProvisioningService.create(
				caller(fixture.userId(), fixture.tenantId(), fixture.homeOrgId()), "Second Org"))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		assertThat(organizationRepository.findByTenantIdOrderByCreatedAtAsc(fixture.tenantId())).hasSize(1);
	}

	@Test
	void aPlainMemberCannotCreateAnOrganization() {
		Fixture fixture = registerTenantAdmin("op-create-forbidden@acme.test");
		raiseOrganizationLimit(fixture.tenantId(), 5);
		UUID plainMemberId = addPlainMember(fixture.tenantId(), fixture.homeOrgId(), "plain@op-create-forbidden.test");

		assertThatThrownBy(() -> organizationProvisioningService.create(
				caller(plainMemberId, fixture.tenantId(), fixture.homeOrgId()), "Nope"))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		assertThat(organizationRepository.findByTenantIdOrderByCreatedAtAsc(fixture.tenantId())).hasSize(1);
	}

	@Test
	void twoOrganizationsWhoseNamesSlugAlikeGetDistinctCodes() {
		Fixture fixture = registerTenantAdmin("op-create-codes@acme.test");
		raiseOrganizationLimit(fixture.tenantId(), 5);
		AuthenticatedUser caller = caller(fixture.userId(), fixture.tenantId(), fixture.homeOrgId());

		OrganizationDetail first = organizationProvisioningService.create(caller, "Main Store");
		OrganizationDetail second = organizationProvisioningService.create(caller, "Main Store");

		assertThat(first.code()).isEqualTo("main-store");
		assertThat(second.code()).isEqualTo("main-store-2");
	}
}
