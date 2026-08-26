package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
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
class UserRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

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

	@Test
	void savesAndFindsUserWithAuditFields() {
		Tenant tenant = newTenant("TEN-USR-1");
		Organization organization = newOrganization(tenant, "ORG-USR-1");

		User user = new User();
		user.setTenantId(tenant.getId());
		user.setOrganizationId(organization.getId());
		user.setEmail("owner@acme.test");
		user.setPasswordHash("hashed-password");
		user.setFullName("Ada Owner");

		User saved = userRepository.saveAndFlush(user);

		User found = userRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getOrganizationId()).isEqualTo(organization.getId());
		assertThat(found.getEmail()).isEqualTo("owner@acme.test");
		assertThat(found.getPasswordHash()).isEqualTo("hashed-password");
		assertThat(found.getFullName()).isEqualTo("Ada Owner");
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateEmailWithinSameTenant() {
		Tenant tenant = newTenant("TEN-USR-2");
		Organization organization = newOrganization(tenant, "ORG-USR-2");

		User first = new User();
		first.setTenantId(tenant.getId());
		first.setOrganizationId(organization.getId());
		first.setEmail("dup@acme.test");
		first.setPasswordHash("hashed-password");
		first.setFullName("First User");
		userRepository.saveAndFlush(first);

		User second = new User();
		second.setTenantId(tenant.getId());
		second.setOrganizationId(organization.getId());
		second.setEmail("dup@acme.test");
		second.setPasswordHash("hashed-password");
		second.setFullName("Second User");

		assertThrows(DataIntegrityViolationException.class,
				() -> userRepository.saveAndFlush(second));
	}

	@Test
	void allowsSameEmailAcrossDifferentTenants() {
		Tenant tenantA = newTenant("TEN-USR-3A");
		Organization organizationA = newOrganization(tenantA, "ORG-USR-3A");
		Tenant tenantB = newTenant("TEN-USR-3B");
		Organization organizationB = newOrganization(tenantB, "ORG-USR-3B");

		User userA = new User();
		userA.setTenantId(tenantA.getId());
		userA.setOrganizationId(organizationA.getId());
		userA.setEmail("shared@acme.test");
		userA.setPasswordHash("hashed-password");
		userA.setFullName("User A");
		userRepository.saveAndFlush(userA);

		User userB = new User();
		userB.setTenantId(tenantB.getId());
		userB.setOrganizationId(organizationB.getId());
		userB.setEmail("shared@acme.test");
		userB.setPasswordHash("hashed-password");
		userB.setFullName("User B");

		User savedB = userRepository.saveAndFlush(userB);

		assertThat(savedB.getId()).isNotNull();
	}

}
