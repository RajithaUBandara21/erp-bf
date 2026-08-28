package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import java.util.List;
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

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Test
	void registersLinkedTenantOrganizationAndUserWithHashedPassword() {
		RegisterRequest request =
				new RegisterRequest("Acme Corp", "Ada Owner", "ada@acme.test", "Sunrise8", ClientType.WEB);

		TokenResponse account = registrationService.register(request);

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
	void registrationReturnsUsableAccessAndRefreshTokens() {
		RegisterRequest request =
				new RegisterRequest("Acme Corp", "Ada Owner", "ada-tokens@acme.test", "Sunrise8", ClientType.WEB);

		TokenResponse response = registrationService.register(request);

		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.refreshToken()).isNotBlank();
		assertThat(response.expiresIn()).isPositive();
		assertThat(response.refreshExpiresIn()).isPositive();
	}

	@Test
	void independentRegistrationsWithSameOrganizationNameGetDifferentTenants() {
		RegisterRequest first =
				new RegisterRequest("Acme Corp", "First Owner", "first@acme.test", "Sunrise8", ClientType.WEB);
		RegisterRequest second =
				new RegisterRequest("Acme Corp", "Second Owner", "second@acme.test", "Sunrise8", ClientType.WEB);

		TokenResponse firstAccount = registrationService.register(first);
		TokenResponse secondAccount = registrationService.register(second);

		assertThat(secondAccount.tenantId()).isNotEqualTo(firstAccount.tenantId());
		assertThat(secondAccount.organizationId()).isNotEqualTo(firstAccount.organizationId());
	}

	@Test
	void normalizesEmailCasingBeforeStorage() {
		RegisterRequest request =
				new RegisterRequest("Acme Corp", "Ada Owner", "Ada.Owner@ACME.test", "Sunrise8", ClientType.WEB);

		TokenResponse account = registrationService.register(request);

		User user = userRepository.findById(account.userId()).orElseThrow();
		assertThat(user.getEmail()).isEqualTo("ada.owner@acme.test");
		assertThat(account.email()).isEqualTo("ada.owner@acme.test");
	}

	@Test
	void provisionsSystemRolesAndAssignsOwnerToTheRegisteringUser() {
		RegisterRequest request =
				new RegisterRequest("Acme Corp", "Ada Owner", "ada-roles@acme.test", "Sunrise8", ClientType.WEB);

		TokenResponse account = registrationService.register(request);

		List<Role> roles = roleRepository.findByTenantId(account.tenantId());
		assertThat(roles).extracting(Role::getName)
				.containsExactlyInAnyOrder("Owner", "Administrator", "Viewer");
		assertThat(roles).allMatch(Role::isSystemManaged);

		Role owner = roleRepository.findByTenantIdAndName(account.tenantId(), "Owner").orElseThrow();
		assertThat(userRoleRepository.findByUserId(account.userId()))
				.singleElement()
				.satisfies(assignment -> assertThat(assignment.getRoleId()).isEqualTo(owner.getId()));
	}

	@Test
	void rollsBackTenantOrganizationAndRolesWhenUserSaveFails() {
		long tenantsBefore = tenantRepository.count();
		long organizationsBefore = organizationRepository.count();
		long rolesBefore = roleRepository.count();
		RegisterRequest request = new RegisterRequest("Acme Corp", null, "ada@acme.test", "Sunrise8", ClientType.WEB);

		assertThatException().isThrownBy(() -> registrationService.register(request));

		assertThat(tenantRepository.count()).isEqualTo(tenantsBefore);
		assertThat(organizationRepository.count()).isEqualTo(organizationsBefore);
		assertThat(roleRepository.count()).isEqualTo(rolesBefore);
	}

}
