package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ERP.erpbackend.TestcontainersConfiguration;
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
class RoleRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private RoleRepository roleRepository;

	private Tenant newTenant(String code) {
		Tenant tenant = new Tenant();
		tenant.setName(code);
		tenant.setCode(code);
		return tenantRepository.saveAndFlush(tenant);
	}

	private Role newRole(UUID tenantId, String name, boolean systemManaged) {
		Role role = new Role();
		role.setTenantId(tenantId);
		role.setName(name);
		role.setDescription(name + " description");
		role.setSystemManaged(systemManaged);
		return role;
	}

	@Test
	void savesAndFindsRoleWithAllFieldsAndAuditTimestamps() {
		Tenant tenant = newTenant("TEN-ROLE-1");

		Role saved = roleRepository.saveAndFlush(newRole(tenant.getId(), "Warehouse Manager", false));

		Role found = roleRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getName()).isEqualTo("Warehouse Manager");
		assertThat(found.getDescription()).isEqualTo("Warehouse Manager description");
		assertThat(found.isSystemManaged()).isFalse();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void findsRolesScopedToOneTenant() {
		Tenant tenantA = newTenant("TEN-ROLE-2A");
		Tenant tenantB = newTenant("TEN-ROLE-2B");
		roleRepository.saveAndFlush(newRole(tenantA.getId(), "Owner", true));
		roleRepository.saveAndFlush(newRole(tenantA.getId(), "Viewer", true));
		roleRepository.saveAndFlush(newRole(tenantB.getId(), "Owner", true));

		assertThat(roleRepository.findByTenantId(tenantA.getId())).hasSize(2);
		assertThat(roleRepository.findByTenantIdAndName(tenantA.getId(), "Owner")).isPresent();
		assertThat(roleRepository.existsByTenantIdAndName(tenantA.getId(), "Missing")).isFalse();
	}

	@Test
	void rejectsDuplicateNameWithinSameTenant() {
		Tenant tenant = newTenant("TEN-ROLE-3");
		roleRepository.saveAndFlush(newRole(tenant.getId(), "Owner", true));
		Role duplicate = newRole(tenant.getId(), "Owner", true);

		assertThrows(DataIntegrityViolationException.class,
				() -> roleRepository.saveAndFlush(duplicate));
	}

	@Test
	void allowsSameNameAcrossDifferentTenants() {
		Tenant tenantA = newTenant("TEN-ROLE-4A");
		Tenant tenantB = newTenant("TEN-ROLE-4B");
		roleRepository.saveAndFlush(newRole(tenantA.getId(), "Owner", true));

		Role savedB = roleRepository.saveAndFlush(newRole(tenantB.getId(), "Owner", true));

		assertThat(savedB.getId()).isNotNull();
	}

}
