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
class TenantRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Test
	void savesAndFindsTenantWithAuditFields() {
		Tenant tenant = new Tenant();
		tenant.setName("Acme Corp");
		tenant.setCode("ACME");

		Tenant saved = tenantRepository.saveAndFlush(tenant);

		Tenant found = tenantRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getName()).isEqualTo("Acme Corp");
		assertThat(found.getCode()).isEqualTo("ACME");
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateCode() {
		Tenant first = new Tenant();
		first.setName("Acme Corp");
		first.setCode("DUP");
		tenantRepository.saveAndFlush(first);

		Tenant second = new Tenant();
		second.setName("Acme Corp Two");
		second.setCode("DUP");

		assertThrows(DataIntegrityViolationException.class,
				() -> tenantRepository.saveAndFlush(second));
	}

}
