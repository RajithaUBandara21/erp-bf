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
class BranchRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private BranchRepository branchRepository;

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
	void savesAndFindsBranchWithAuditFields() {
		Tenant tenant = newTenant("TEN-BR-1");
		Organization organization = newOrganization(tenant, "ORG-BR-1");

		Branch branch = new Branch();
		branch.setTenantId(tenant.getId());
		branch.setOrganizationId(organization.getId());
		branch.setName("Downtown Branch");
		branch.setCode("BR-DT");

		Branch saved = branchRepository.saveAndFlush(branch);

		Branch found = branchRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getOrganizationId()).isEqualTo(organization.getId());
		assertThat(found.getName()).isEqualTo("Downtown Branch");
		assertThat(found.getCode()).isEqualTo("BR-DT");
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateCodeWithinSameTenant() {
		Tenant tenant = newTenant("TEN-BR-2");
		Organization organization = newOrganization(tenant, "ORG-BR-2");

		Branch first = new Branch();
		first.setTenantId(tenant.getId());
		first.setOrganizationId(organization.getId());
		first.setName("Downtown Branch");
		first.setCode("DUP");
		branchRepository.saveAndFlush(first);

		Branch second = new Branch();
		second.setTenantId(tenant.getId());
		second.setOrganizationId(organization.getId());
		second.setName("Uptown Branch");
		second.setCode("DUP");

		assertThrows(DataIntegrityViolationException.class,
				() -> branchRepository.saveAndFlush(second));
	}

	@Test
	void allowsSameCodeAcrossDifferentTenants() {
		Tenant tenantA = newTenant("TEN-BR-3A");
		Organization organizationA = newOrganization(tenantA, "ORG-BR-3A");
		Tenant tenantB = newTenant("TEN-BR-3B");
		Organization organizationB = newOrganization(tenantB, "ORG-BR-3B");

		Branch branchA = new Branch();
		branchA.setTenantId(tenantA.getId());
		branchA.setOrganizationId(organizationA.getId());
		branchA.setName("Downtown Branch");
		branchA.setCode("BR-DT");
		branchRepository.saveAndFlush(branchA);

		Branch branchB = new Branch();
		branchB.setTenantId(tenantB.getId());
		branchB.setOrganizationId(organizationB.getId());
		branchB.setName("Downtown Branch");
		branchB.setCode("BR-DT");

		Branch savedB = branchRepository.saveAndFlush(branchB);

		assertThat(savedB.getId()).isNotNull();
	}

}
