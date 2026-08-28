package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ERP.erpbackend.TestcontainersConfiguration;
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
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class RolePermissionUserRoleRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private RolePermissionRepository rolePermissionRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	private Tenant tenant(String code) {
		Tenant tenant = new Tenant();
		tenant.setName(code);
		tenant.setCode(code);
		return tenantRepository.saveAndFlush(tenant);
	}

	private User user(Tenant tenant, String email) {
		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName(email);
		organization.setCode(email);
		organization = organizationRepository.saveAndFlush(organization);

		User user = new User();
		user.setTenantId(tenant.getId());
		user.setOrganizationId(organization.getId());
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName("Role Holder");
		return userRepository.saveAndFlush(user);
	}

	private Role role(UUID tenantId, String name) {
		Role role = new Role();
		role.setTenantId(tenantId);
		role.setName(name);
		role.setSystemManaged(true);
		return roleRepository.saveAndFlush(role);
	}

	private RolePermission rolePermission(UUID roleId, UUID permissionId) {
		RolePermission mapping = new RolePermission();
		mapping.setRoleId(roleId);
		mapping.setPermissionId(permissionId);
		return mapping;
	}

	private UserRole userRole(UUID tenantId, UUID userId, UUID roleId) {
		UserRole assignment = new UserRole();
		assignment.setTenantId(tenantId);
		assignment.setUserId(userId);
		assignment.setRoleId(roleId);
		return assignment;
	}

	@Test
	void savesAndFindsRolePermissionsForARole() {
		Tenant tenant = tenant("TEN-RP-1");
		Role role = role(tenant.getId(), "Owner");
		UUID firstPermission = permissionRepository.findAll().get(0).getId();
		UUID secondPermission = permissionRepository.findAll().get(1).getId();

		rolePermissionRepository.saveAndFlush(rolePermission(role.getId(), firstPermission));
		rolePermissionRepository.saveAndFlush(rolePermission(role.getId(), secondPermission));

		assertThat(rolePermissionRepository.findByRoleId(role.getId())).hasSize(2);
	}

	@Test
	void rejectsDuplicatePermissionOnSameRole() {
		Tenant tenant = tenant("TEN-RP-2");
		Role role = role(tenant.getId(), "Owner");
		UUID permissionId = permissionRepository.findAll().get(0).getId();
		rolePermissionRepository.saveAndFlush(rolePermission(role.getId(), permissionId));
		RolePermission duplicate = rolePermission(role.getId(), permissionId);

		assertThrows(DataIntegrityViolationException.class,
				() -> rolePermissionRepository.saveAndFlush(duplicate));
	}

	@Test
	void rejectsRolePermissionWithUnknownPermission() {
		Tenant tenant = tenant("TEN-RP-3");
		Role role = role(tenant.getId(), "Owner");
		RolePermission orphan = rolePermission(role.getId(), UUID.randomUUID());

		assertThrows(DataIntegrityViolationException.class,
				() -> rolePermissionRepository.saveAndFlush(orphan));
	}

	@Test
	void savesAndFindsUserRoleAssignments() {
		Tenant tenant = tenant("TEN-UR-1");
		User holder = user(tenant, "holder@acme.test");
		Role owner = role(tenant.getId(), "Owner");
		Role viewer = role(tenant.getId(), "Viewer");

		userRoleRepository.saveAndFlush(userRole(tenant.getId(), holder.getId(), owner.getId()));
		userRoleRepository.saveAndFlush(userRole(tenant.getId(), holder.getId(), viewer.getId()));

		assertThat(userRoleRepository.findByUserId(holder.getId())).hasSize(2);
		assertThat(userRoleRepository.existsByUserIdAndRoleId(holder.getId(), owner.getId())).isTrue();
	}

	@Test
	void rejectsDuplicateRoleForSameUser() {
		Tenant tenant = tenant("TEN-UR-2");
		User holder = user(tenant, "dupe@acme.test");
		Role owner = role(tenant.getId(), "Owner");
		userRoleRepository.saveAndFlush(userRole(tenant.getId(), holder.getId(), owner.getId()));
		UserRole duplicate = userRole(tenant.getId(), holder.getId(), owner.getId());

		assertThrows(DataIntegrityViolationException.class,
				() -> userRoleRepository.saveAndFlush(duplicate));
	}

}
