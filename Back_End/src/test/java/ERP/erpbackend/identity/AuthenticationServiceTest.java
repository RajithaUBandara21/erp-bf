package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthenticationServiceTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private RefreshTokenService refreshTokenService;

	private String registerAndGetOrganizationCode(String email) {
		RegisteredAccount account = registrationService.register(
				new RegisterRequest("Acme Corp " + email, "Ada Owner", email, PASSWORD));
		Tenant tenant = tenantRepository.findById(account.tenantId()).orElseThrow();
		return tenant.getCode();
	}

	@Test
	void correctLoginReturnsAValidAccessTokenAndAWorkingRefreshToken() {
		String email = "ada@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);

		TokenResponse response = authenticationService.login(new LoginRequest(organizationCode, email, PASSWORD));

		AuthenticatedUser authenticatedUser = jwtService.parseAccessToken(response.accessToken()).orElseThrow();
		assertThat(authenticatedUser.email()).isEqualTo(email);
		assertThat(authenticatedUser.tenantId()).isEqualTo(response.tenantId());

		assertThat(refreshTokenService.consume(response.refreshToken())).contains(response.userId());
	}

	@Test
	void loginWithWrongOrganizationCodeFailsWithInvalidCredentials() {
		String email = "wrong-org@acme.test";
		registerAndGetOrganizationCode(email);

		assertUnauthorized(new LoginRequest("does-not-exist", email, PASSWORD));
	}

	@Test
	void loginWithWrongEmailFailsWithInvalidCredentials() {
		String email = "wrong-email@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);

		assertUnauthorized(new LoginRequest(organizationCode, "someone-else@acme.test", PASSWORD));
	}

	@Test
	void loginWithWrongPasswordFailsWithInvalidCredentials() {
		String email = "wrong-password@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);

		assertUnauthorized(new LoginRequest(organizationCode, email, "WrongPass9"));
	}

	@Test
	void loginWithInactiveUserFailsWithInvalidCredentials() {
		String email = "inactive@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);
		User user = userRepository.findByTenantIdAndEmail(
				tenantRepository.findByCode(organizationCode).orElseThrow().getId(), email).orElseThrow();
		user.setActive(false);
		userRepository.save(user);

		assertUnauthorized(new LoginRequest(organizationCode, email, PASSWORD));
	}

	@Test
	void validRefreshReturnsNewTokensAndInvalidatesTheOldRefreshToken() {
		String email = "refresh@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);
		TokenResponse loginResponse = authenticationService.login(new LoginRequest(organizationCode, email, PASSWORD));

		TokenResponse refreshed = authenticationService.refresh(new RefreshRequest(loginResponse.refreshToken()));

		assertThat(refreshed.userId()).isEqualTo(loginResponse.userId());
		assertThat(jwtService.parseAccessToken(refreshed.accessToken())).isPresent();
		assertThat(refreshTokenService.consume(refreshed.refreshToken())).contains(loginResponse.userId());

		assertUnauthorizedRefresh(new RefreshRequest(loginResponse.refreshToken()));
	}

	@Test
	void refreshingWithADeactivatedUsersTokenFails() {
		String email = "refresh-inactive@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);
		TokenResponse loginResponse = authenticationService.login(new LoginRequest(organizationCode, email, PASSWORD));
		User user = userRepository.findById(loginResponse.userId()).orElseThrow();
		user.setActive(false);
		userRepository.save(user);

		assertUnauthorizedRefresh(new RefreshRequest(loginResponse.refreshToken()));
	}

	@Test
	void logoutRevokesTheTokenSoAFollowingRefreshFails() {
		String email = "logout@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);
		TokenResponse loginResponse = authenticationService.login(new LoginRequest(organizationCode, email, PASSWORD));

		authenticationService.logout(new RefreshRequest(loginResponse.refreshToken()));

		assertUnauthorizedRefresh(new RefreshRequest(loginResponse.refreshToken()));
	}

	private void assertUnauthorized(LoginRequest request) {
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> authenticationService.login(request))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED))
				.withMessageContaining("Invalid credentials");
	}

	private void assertUnauthorizedRefresh(RefreshRequest request) {
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> authenticationService.refresh(request))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED))
				.withMessageContaining("Invalid credentials");
	}

}
