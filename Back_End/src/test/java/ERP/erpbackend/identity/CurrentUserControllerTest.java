package ERP.erpbackend.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ERP.erpbackend.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CurrentUserControllerTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private UserRepository userRepository;

	private AuthenticatedUser registerOwner(String email) {
		TokenResponse response = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.accessToken()).orElseThrow();
	}

	private String tokenFor(AuthenticatedUser user) {
		return jwtService.issueAccessToken(new AuthenticatedUser(
				user.userId(), user.tenantId(), user.organizationId(), user.email(), UUID.randomUUID()));
	}

	@Test
	void returnsAllNinetyFivePermissionsForAFreshlyRegisteredOwner() throws Exception {
		AuthenticatedUser owner = registerOwner("me-owner@acme.test");

		mockMvc.perform(get("/api/auth/me/permissions").header("Authorization", "Bearer " + tokenFor(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.permissions.length()").value(95));
	}

	@Test
	void twoUsersInTheSameTenantWithDifferentRolesGetDifferentSets() throws Exception {
		AuthenticatedUser owner = registerOwner("me-tenant@acme.test");

		User viewerUser = new User();
		viewerUser.setTenantId(owner.tenantId());
		viewerUser.setOrganizationId(owner.organizationId());
		viewerUser.setEmail("viewer@me-tenant.test");
		viewerUser.setPasswordHash("hashed-password");
		viewerUser.setFullName("Vic Viewer");
		viewerUser = userRepository.save(viewerUser);

		UUID viewerRoleId = roleRepository.findByTenantIdAndName(owner.tenantId(), "Viewer").orElseThrow().getId();
		UserRole assignment = new UserRole();
		assignment.setTenantId(owner.tenantId());
		assignment.setUserId(viewerUser.getId());
		assignment.setRoleId(viewerRoleId);
		userRoleRepository.save(assignment);

		AuthenticatedUser viewer = new AuthenticatedUser(
				viewerUser.getId(), owner.tenantId(), owner.organizationId(), viewerUser.getEmail(), UUID.randomUUID());

		mockMvc.perform(get("/api/auth/me/permissions").header("Authorization", "Bearer " + tokenFor(viewer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.permissions.length()").value(19))
				.andExpect(jsonPath("$.permissions[0]").value("accounting.view"));
	}

	@Test
	void returnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/me/permissions"))
				.andExpect(status().isUnauthorized());
	}

}
