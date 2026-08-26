package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistrationServiceTest {

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registersLinkedTenantOrganizationAndUserWithHashedPassword() {
		RegisterRequest request = new RegisterRequest("Acme Corp", "Ada Owner", "ada@acme.test", "Sunrise8");

		RegisteredAccount account = registrationService.register(request);

		Tenant tenant = tenantRepository.findById(account.tenantId()).orElseThrow();
		Organization organization = organizationRepository.findById(account.organizationId()).orElseThrow();
		User user = userRepository.findById(account.userId()).orElseThrow();

		assertThat(organization.getTenantId()).isEqualTo(tenant.getId());
		assertThat(user.getTenantId()).isEqualTo(tenant.getId());
		assertThat(user.getOrganizationId()).isEqualTo(organization.getId());
		assertThat(user.getEmail()).isEqualTo("ada@acme.test");
		assertThat(user.getFullName()).isEqualTo("Ada Owner");
		assertThat(user.getPasswordHash()).isNotEqualTo("Sunrise8");
		assertThat(passwordEncoder.matches("Sunrise8", user.getPasswordHash())).isTrue();
	}

	@Test
	void independentRegistrationsWithSameOrganizationNameGetDifferentTenants() {
		RegisterRequest first = new RegisterRequest("Acme Corp", "First Owner", "first@acme.test", "Sunrise8");
		RegisterRequest second = new RegisterRequest("Acme Corp", "Second Owner", "second@acme.test", "Sunrise8");

		RegisteredAccount firstAccount = registrationService.register(first);
		RegisteredAccount secondAccount = registrationService.register(second);

		assertThat(secondAccount.tenantId()).isNotEqualTo(firstAccount.tenantId());
		assertThat(secondAccount.organizationId()).isNotEqualTo(firstAccount.organizationId());
	}

}
