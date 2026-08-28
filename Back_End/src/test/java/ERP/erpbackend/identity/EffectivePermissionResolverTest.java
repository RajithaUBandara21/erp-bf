package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EffectivePermissionResolverTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private EffectivePermissionResolver resolver;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	private AuthenticatedUser registerOwner(String email) {
		TokenResponse response = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.accessToken()).orElseThrow();
	}

	@Test
	void ownerResolvesEveryCatalogPermission() {
		AuthenticatedUser owner = registerOwner("resolver-owner@acme.test");

		assertThat(resolver.resolve(owner.userId(), owner.tenantId())).hasSize(95);
	}

	@Test
	void userWithNoRolesResolvesEmptySet() {
		Tenant tenant = new Tenant();
		tenant.setName("no-roles");
		tenant.setCode("resolver-no-roles");
		tenant = tenantRepository.save(tenant);

		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName("no-roles");
		organization.setCode("resolver-no-roles");
		organization = organizationRepository.save(organization);

		User user = new User();
		user.setTenantId(tenant.getId());
		user.setOrganizationId(organization.getId());
		user.setEmail("roleless@acme.test");
		user.setPasswordHash("hashed-password");
		user.setFullName("Roleless User");
		user = userRepository.save(user);

		assertThat(resolver.resolve(user.getId(), tenant.getId())).isEmpty();
	}

	@Test
	void assignmentUnderAnotherTenantIsExcluded() {
		AuthenticatedUser owner = registerOwner("resolver-tenant@acme.test");

		assertThat(resolver.resolve(owner.userId(), UUID.randomUUID())).isEmpty();
	}

}
