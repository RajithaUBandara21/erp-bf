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
class WarehouseRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

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
	void savesAndFindsWarehouseWithAuditFields() {
		Tenant tenant = newTenant("TEN-WH-1");
		Organization organization = newOrganization(tenant, "ORG-WH-1");

		Warehouse warehouse = new Warehouse();
		warehouse.setTenantId(tenant.getId());
		warehouse.setOrganizationId(organization.getId());
		warehouse.setName("Main Warehouse");
		warehouse.setCode("WH-MAIN");

		Warehouse saved = warehouseRepository.saveAndFlush(warehouse);

		Warehouse found = warehouseRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getOrganizationId()).isEqualTo(organization.getId());
		assertThat(found.getName()).isEqualTo("Main Warehouse");
		assertThat(found.getCode()).isEqualTo("WH-MAIN");
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateCodeWithinSameTenant() {
		Tenant tenant = newTenant("TEN-WH-2");
		Organization organization = newOrganization(tenant, "ORG-WH-2");

		Warehouse first = new Warehouse();
		first.setTenantId(tenant.getId());
		first.setOrganizationId(organization.getId());
		first.setName("Main Warehouse");
		first.setCode("DUP");
		warehouseRepository.saveAndFlush(first);

		Warehouse second = new Warehouse();
		second.setTenantId(tenant.getId());
		second.setOrganizationId(organization.getId());
		second.setName("Secondary Warehouse");
		second.setCode("DUP");

		assertThrows(DataIntegrityViolationException.class,
				() -> warehouseRepository.saveAndFlush(second));
	}

	@Test
	void allowsSameCodeAcrossDifferentTenants() {
		Tenant tenantA = newTenant("TEN-WH-3A");
		Organization organizationA = newOrganization(tenantA, "ORG-WH-3A");
		Tenant tenantB = newTenant("TEN-WH-3B");
		Organization organizationB = newOrganization(tenantB, "ORG-WH-3B");

		Warehouse warehouseA = new Warehouse();
		warehouseA.setTenantId(tenantA.getId());
		warehouseA.setOrganizationId(organizationA.getId());
		warehouseA.setName("Main Warehouse");
		warehouseA.setCode("WH-MAIN");
		warehouseRepository.saveAndFlush(warehouseA);

		Warehouse warehouseB = new Warehouse();
		warehouseB.setTenantId(tenantB.getId());
		warehouseB.setOrganizationId(organizationB.getId());
		warehouseB.setName("Main Warehouse");
		warehouseB.setCode("WH-MAIN");

		Warehouse savedB = warehouseRepository.saveAndFlush(warehouseB);

		assertThat(savedB.getId()).isNotNull();
	}

}
