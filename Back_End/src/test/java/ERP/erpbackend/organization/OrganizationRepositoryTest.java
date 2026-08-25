package ERP.erpbackend.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ERP.erpbackend.TestcontainersConfiguration;
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
class OrganizationRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	private Tenant newTenant(String code) {
		Tenant tenant = new Tenant();
		tenant.setName(code);
		tenant.setCode(code);
		return tenantRepository.saveAndFlush(tenant);
	}

	@Test
	void savesAndFindsOrganizationWithAuditFields() {
		Tenant tenant = newTenant("TEN-ORG-1");

		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName("Head Office");
		organization.setCode("HQ");

		Organization saved = organizationRepository.saveAndFlush(organization);

		Organization found = organizationRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getName()).isEqualTo("Head Office");
		assertThat(found.getCode()).isEqualTo("HQ");
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateCodeWithinSameTenant() {
		Tenant tenant = newTenant("TEN-ORG-2");

		Organization first = new Organization();
		first.setTenantId(tenant.getId());
		first.setName("Head Office");
		first.setCode("DUP");
		organizationRepository.saveAndFlush(first);

		Organization second = new Organization();
		second.setTenantId(tenant.getId());
		second.setName("Branch Office");
		second.setCode("DUP");

		assertThrows(DataIntegrityViolationException.class,
				() -> organizationRepository.saveAndFlush(second));
	}

	@Test
	void allowsSameCodeAcrossDifferentTenants() {
		Tenant tenantA = newTenant("TEN-ORG-3A");
		Tenant tenantB = newTenant("TEN-ORG-3B");

		Organization orgA = new Organization();
		orgA.setTenantId(tenantA.getId());
		orgA.setName("Head Office");
		orgA.setCode("HQ");
		organizationRepository.saveAndFlush(orgA);

		Organization orgB = new Organization();
		orgB.setTenantId(tenantB.getId());
		orgB.setName("Head Office");
		orgB.setCode("HQ");

		Organization savedB = organizationRepository.saveAndFlush(orgB);

		assertThat(savedB.getId()).isNotNull();
	}

}
