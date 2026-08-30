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

	@Autowired
	private MembershipRepository membershipRepository;

	private AuthenticatedUser registerOwner(String email) {
		TokenResponse response = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.accessToken()).orElseThrow();
	}

	@Test
	void ownerResolvesEveryCatalogPermission() {
		AuthenticatedUser owner = registerOwner("resolver-owner@acme.test");

		assertThat(resolver.resolve(owner.membershipId())).hasSize(95);
	}

	@Test
	void membershipWithNoRolesResolvesEmptySet() {
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
		user.setEmail("roleless@acme.test");
		user.setPasswordHash("hashed-password");
		user.setFullName("Roleless User");
		user = userRepository.save(user);

		Membership membership = new Membership();
		membership.setUserId(user.getId());
		membership.setTenantId(tenant.getId());
		membership.setOrganizationId(organization.getId());
		membership.setStatus(MembershipStatus.ACTIVE);
		membership = membershipRepository.save(membership);

		assertThat(resolver.resolve(membership.getId())).isEmpty();
	}

	@Test
	void unknownMembershipResolvesEmptySet() {
		registerOwner("resolver-unknown@acme.test");

		assertThat(resolver.resolve(UUID.randomUUID())).isEmpty();
	}

}
