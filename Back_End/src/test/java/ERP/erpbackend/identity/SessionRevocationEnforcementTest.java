package ERP.erpbackend.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F-11: revoking a session must block its still-valid access token on the next
 * request, not just its refresh token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SessionRevocationEnforcementTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private SessionService sessionService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TenantRepository tenantRepository;

	private record Device(String organizationCode, String accessToken, AuthenticatedUser user) {
	}

	private Device register(String email) {
		TokenResponse response = registrationService.register(
				new RegisterRequest("Acme Corp " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		Tenant tenant = tenantRepository.findById(response.tenantId()).orElseThrow();
		return new Device(tenant.getCode(), response.accessToken(),
				jwtService.parseAccessToken(response.accessToken()).orElseThrow());
	}

	private Device login(String organizationCode, String email) {
		TokenResponse response = authenticationService.login(
				new LoginRequest(organizationCode, email, PASSWORD, ClientType.WEB));
		return new Device(organizationCode, response.accessToken(),
				jwtService.parseAccessToken(response.accessToken()).orElseThrow());
	}

	private void expectSessions(String accessToken, int expectedStatus) throws Exception {
		mockMvc.perform(get("/api/auth/sessions").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().is(expectedStatus));
	}

	@Test
	void revokingASessionBlocksItsAccessTokenOnTheNextRequest() throws Exception {
		String email = "revoke-access@acme.test";
		Device device = register(email);
		expectSessions(device.accessToken(), 200);

		sessionService.revokeSession(device.user(), device.user().sessionId());

		expectSessions(device.accessToken(), 401);
	}

	@Test
	void revokeOthersBlocksEveryOtherDeviceButKeepsTheCallersOwnTokenWorking() throws Exception {
		String email = "revoke-others-access@acme.test";
		Device registration = register(email);
		Device otherDevice = login(registration.organizationCode(), email);
		Device caller = login(registration.organizationCode(), email);

		sessionService.revokeOtherSessions(caller.user());

		expectSessions(registration.accessToken(), 401);
		expectSessions(otherDevice.accessToken(), 401);
		expectSessions(caller.accessToken(), 200);
	}

}
