package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SystemRoleProvisionerTest {

	@Autowired
	private SystemRoleProvisioner systemRoleProvisioner;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private RolePermissionRepository rolePermissionRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private MembershipRepository membershipRepository;

	private record Fixture(UUID tenantId, UUID membershipId) {
	}

	private Fixture provisionFreshTenant(String code) {
		Tenant tenant = new Tenant();
		tenant.setName(code);
		tenant.setCode(code);
		tenant = tenantRepository.save(tenant);

		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName(code);
		organization.setCode(code);
		organization = organizationRepository.save(organization);

		User user = new User();
		user.setEmail(code + "@acme.test");
		user.setPasswordHash("hashed-password");
		user.setFullName("First Owner");
		user = userRepository.save(user);

		Membership membership = new Membership();
		membership.setUserId(user.getId());
		membership.setTenantId(tenant.getId());
		membership.setOrganizationId(organization.getId());
		membership.setStatus(MembershipStatus.ACTIVE);
		membership = membershipRepository.save(membership);

		systemRoleProvisioner.provisionForNewTenant(tenant.getId(), membership.getId());
		return new Fixture(tenant.getId(), membership.getId());
	}

	private Role roleNamed(UUID tenantId, SystemRole systemRole) {
		return roleRepository.findByTenantIdAndName(tenantId, systemRole.displayName()).orElseThrow();
	}

	private long grantCount(Role role) {
		return rolePermissionRepository.findByRoleId(role.getId()).size();
	}

	@Test
	void provisionsThreeSystemManagedRoles() {
		Fixture fixture = provisionFreshTenant("prov-roles");

		List<Role> roles = roleRepository.findByTenantId(fixture.tenantId());
		assertThat(roles).extracting(Role::getName)
				.containsExactlyInAnyOrder("Owner", "Administrator", "Viewer");
		assertThat(roles).allMatch(Role::isSystemManaged);
	}

	@Test
	void ownerGrantsEveryPermissionAdministratorExcludesBillingViewerIsViewOnly() {
		Fixture fixture = provisionFreshTenant("prov-grants");

		assertThat(grantCount(roleNamed(fixture.tenantId(), SystemRole.OWNER))).isEqualTo(95);
		assertThat(grantCount(roleNamed(fixture.tenantId(), SystemRole.ADMINISTRATOR))).isEqualTo(90);
		assertThat(grantCount(roleNamed(fixture.tenantId(), SystemRole.VIEWER))).isEqualTo(19);
	}

	@Test
	void assignsOnlyTheOwnerRoleToTheFirstMembership() {
		Fixture fixture = provisionFreshTenant("prov-assign");

		List<UserRole> assignments = userRoleRepository.findByMembershipId(fixture.membershipId());
		assertThat(assignments).hasSize(1);
		assertThat(assignments.get(0).getRoleId())
				.isEqualTo(roleNamed(fixture.tenantId(), SystemRole.OWNER).getId());
		assertThat(assignments.get(0).getTenantId()).isEqualTo(fixture.tenantId());
	}

}
