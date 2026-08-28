package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PermissionRepositoryTest {

	@Autowired
	private PermissionRepository permissionRepository;

	@Test
	void seedsFullResourceByActionCatalog() {
		List<Permission> all = permissionRepository.findAll();

		Set<String> codes = all.stream().map(Permission::getCode).collect(Collectors.toSet());
		Set<PermissionAction> actions = all.stream().map(Permission::getAction).collect(Collectors.toSet());

		assertThat(all).hasSize(95);
		assertThat(codes).hasSize(95);
		assertThat(actions).containsExactlyInAnyOrder(PermissionAction.values());
	}

	@Test
	void seedsCanonicalCodeResourceAndActionForEachRow() {
		Permission productView = permissionRepository.findAll().stream()
				.filter(permission -> "product.view".equals(permission.getCode()))
				.findFirst()
				.orElseThrow();

		assertThat(productView.getResource()).isEqualTo("product");
		assertThat(productView.getAction()).isEqualTo(PermissionAction.VIEW);
		assertThat(productView.getCreatedAt()).isNotNull();
		assertThat(productView.getUpdatedAt()).isNotNull();
	}

}
