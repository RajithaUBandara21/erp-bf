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
class StoreRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private StoreRepository storeRepository;

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
	void savesAndFindsStoreWithAuditFields() {
		Tenant tenant = newTenant("TEN-ST-1");
		Organization organization = newOrganization(tenant, "ORG-ST-1");

		Store store = new Store();
		store.setTenantId(tenant.getId());
		store.setOrganizationId(organization.getId());
		store.setName("Downtown Store");
		store.setCode("ST-DT");

		Store saved = storeRepository.saveAndFlush(store);

		Store found = storeRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getOrganizationId()).isEqualTo(organization.getId());
		assertThat(found.getName()).isEqualTo("Downtown Store");
		assertThat(found.getCode()).isEqualTo("ST-DT");
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateCodeWithinSameTenant() {
		Tenant tenant = newTenant("TEN-ST-2");
		Organization organization = newOrganization(tenant, "ORG-ST-2");

		Store first = new Store();
		first.setTenantId(tenant.getId());
		first.setOrganizationId(organization.getId());
		first.setName("Downtown Store");
		first.setCode("DUP");
		storeRepository.saveAndFlush(first);

		Store second = new Store();
		second.setTenantId(tenant.getId());
		second.setOrganizationId(organization.getId());
		second.setName("Uptown Store");
		second.setCode("DUP");

		assertThrows(DataIntegrityViolationException.class,
				() -> storeRepository.saveAndFlush(second));
	}

	@Test
	void allowsSameCodeAcrossDifferentTenants() {
		Tenant tenantA = newTenant("TEN-ST-3A");
		Organization organizationA = newOrganization(tenantA, "ORG-ST-3A");
		Tenant tenantB = newTenant("TEN-ST-3B");
		Organization organizationB = newOrganization(tenantB, "ORG-ST-3B");

		Store storeA = new Store();
		storeA.setTenantId(tenantA.getId());
		storeA.setOrganizationId(organizationA.getId());
		storeA.setName("Downtown Store");
		storeA.setCode("ST-DT");
		storeRepository.saveAndFlush(storeA);

		Store storeB = new Store();
		storeB.setTenantId(tenantB.getId());
		storeB.setOrganizationId(organizationB.getId());
		storeB.setName("Downtown Store");
		storeB.setCode("ST-DT");

		Store savedB = storeRepository.saveAndFlush(storeB);

		assertThat(savedB.getId()).isNotNull();
	}

}
