package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import ERP.erpbackend.organization.OrganizationService;
import ERP.erpbackend.organization.TenantOrganization;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
	private OrganizationService organizationService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MembershipRepository membershipRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@MockitoSpyBean
	private PasswordEncoder passwordEncoder;

	private void register(String email) {
		registrationService.register(
				new RegisterRequest("Acme Corp " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
	}

	/** Logs in an account expected to have exactly one ACTIVE Membership, returning its issued tokens. */
	private TokenResponse loginSingle(String email, ClientType clientType) {
		LoginResponse response = authenticationService.login(new LoginRequest(email, PASSWORD, clientType));
		assertThat(response.outcome()).isEqualTo(LoginOutcome.AUTHENTICATED);
		return response.session();
	}

	/** Adds a second ACTIVE Membership (new Tenant + Organization) to an existing user. */
	private void addSecondMembership(UUID userId, String label) {
		TenantOrganization second = organizationService.createTenantAndOrganization("Second " + label);
		Membership membership = new Membership();
		membership.setUserId(userId);
		membership.setTenantId(second.tenantId());
		membership.setOrganizationId(second.organizationId());
		membership.setStatus(MembershipStatus.ACTIVE);
		membershipRepository.save(membership);
	}

	@Test
	void singleMembershipLoginReturnsAValidAccessTokenAndAWorkingRefreshToken() {
		String email = "ada@acme.test";
		register(email);

		TokenResponse response = loginSingle(email, ClientType.WEB);

		AuthenticatedUser authenticatedUser = jwtService.parseAccessToken(response.accessToken()).orElseThrow();
		assertThat(authenticatedUser.email()).isEqualTo(email);
		assertThat(authenticatedUser.tenantId()).isEqualTo(response.tenantId());
		assertThat(refreshTokenService.consume(response.refreshToken())).contains(authenticatedUser.sessionId());
	}

	@Test
	void loginCreatesASessionWithTheRequestedClientTypeAndMatchingSessionId() {
		String email = "session@acme.test";
		register(email);

		TokenResponse response = loginSingle(email, ClientType.MOBILE);

		AuthenticatedUser authenticatedUser = jwtService.parseAccessToken(response.accessToken()).orElseThrow();
		Session session = sessionRepository.findById(authenticatedUser.sessionId()).orElseThrow();
		assertThat(session.getClientType()).isEqualTo(ClientType.MOBILE);
		assertThat(session.getUserId()).isEqualTo(response.userId());
		assertThat(session.getTenantId()).isEqualTo(response.tenantId());
	}

	@Test
	void loginProducesAnAuditLogRow() {
		String email = "audit-login@acme.test";
		register(email);

		TokenResponse response = loginSingle(email, ClientType.WEB);

		AuditLog auditLog = auditLogRepository.findAll().stream()
				.filter(entry -> response.userId().equals(entry.getUserId()))
				.filter(entry -> "auth.login".equals(entry.getAction()))
				.findFirst().orElseThrow();
		assertThat(auditLog.getEntityType()).isEqualTo("Session");
		assertThat(auditLog.getTenantId()).isEqualTo(response.tenantId());
	}

	@Test
	void multipleActiveMembershipsReturnASelectionTokenAndTheOrganizationsWithoutASession() {
		String email = "multi@acme.test";
		register(email);
		UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
		addSecondMembership(userId, email);
		long sessionsBefore = sessionRepository.findAll().stream()
				.filter(session -> session.getUserId().equals(userId)).count();

		LoginResponse response = authenticationService.login(new LoginRequest(email, PASSWORD, ClientType.WEB));

		assertThat(response.outcome()).isEqualTo(LoginOutcome.SELECT_ORGANIZATION);
		assertThat(response.session()).isNull();
		assertThat(response.selectionToken()).isNotBlank();
		assertThat(response.organizations()).hasSize(2)
				.allSatisfy(option -> {
					assertThat(option.membershipId()).isNotNull();
					assertThat(option.organizationName()).isNotBlank();
				});
		long sessionsAfter = sessionRepository.findAll().stream()
				.filter(session -> session.getUserId().equals(userId)).count();
		assertThat(sessionsAfter).isEqualTo(sessionsBefore);
	}

	@Test
	void selectOrganizationIssuesASessionForTheChosenMembership() {
		String email = "select-ok@acme.test";
		register(email);
		UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
		addSecondMembership(userId, email);
		LoginResponse login = authenticationService.login(new LoginRequest(email, PASSWORD, ClientType.WEB));
		MembershipOption chosen = login.organizations().getFirst();

		TokenResponse tokens = authenticationService.selectOrganization(
				new LoginSelectRequest(login.selectionToken(), chosen.membershipId(), ClientType.WEB));

		AuthenticatedUser authenticatedUser = jwtService.parseAccessToken(tokens.accessToken()).orElseThrow();
		assertThat(authenticatedUser.membershipId()).isEqualTo(chosen.membershipId());
		Session session = sessionRepository.findById(authenticatedUser.sessionId()).orElseThrow();
		assertThat(session.getMembershipId()).isEqualTo(chosen.membershipId());
	}

	@Test
	void selectOrganizationRejectsAReusedSelectionToken() {
		String email = "select-reuse@acme.test";
		register(email);
		UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
		addSecondMembership(userId, email);
		LoginResponse login = authenticationService.login(new LoginRequest(email, PASSWORD, ClientType.WEB));
		UUID membershipId = login.organizations().getFirst().membershipId();
		authenticationService.selectOrganization(
				new LoginSelectRequest(login.selectionToken(), membershipId, ClientType.WEB));

		assertUnauthorizedSelect(new LoginSelectRequest(login.selectionToken(), membershipId, ClientType.WEB));
	}

	@Test
	void selectOrganizationRejectsAMembershipThatIsNotTheTokenOwners() {
		String victimEmail = "select-victim@acme.test";
		register(victimEmail);
		UUID victimId = userRepository.findByEmail(victimEmail).orElseThrow().getId();
		UUID victimMembership = membershipRepository.findByUserId(victimId).getFirst().getId();

		String attackerEmail = "select-attacker@acme.test";
		register(attackerEmail);
		UUID attackerId = userRepository.findByEmail(attackerEmail).orElseThrow().getId();
		addSecondMembership(attackerId, attackerEmail);
		LoginResponse attackerLogin =
				authenticationService.login(new LoginRequest(attackerEmail, PASSWORD, ClientType.WEB));

		assertUnauthorizedSelect(
				new LoginSelectRequest(attackerLogin.selectionToken(), victimMembership, ClientType.WEB));
	}

	@Test
	void selectOrganizationRejectsATokenForADeactivatedUser() {
		String email = "select-deactivated@acme.test";
		register(email);
		UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
		addSecondMembership(userId, email);
		LoginResponse login = authenticationService.login(new LoginRequest(email, PASSWORD, ClientType.WEB));
		UUID membershipId = login.organizations().getFirst().membershipId();
		User user = userRepository.findById(userId).orElseThrow();
		user.setActive(false);
		userRepository.save(user);

		assertUnauthorizedSelect(new LoginSelectRequest(login.selectionToken(), membershipId, ClientType.WEB));
	}

	@Test
	void loginWithWrongEmailFailsWithInvalidCredentials() {
		register("known@acme.test");

		assertUnauthorized(new LoginRequest("someone-else@acme.test", PASSWORD, ClientType.WEB));
	}

	@Test
	void loginWithWrongPasswordFailsWithInvalidCredentials() {
		String email = "wrong-password@acme.test";
		register(email);

		assertUnauthorized(new LoginRequest(email, "WrongPass9", ClientType.WEB));
	}

	@Test
	void loginRunsThePasswordHashComparisonForAnUnknownEmail() {
		assertUnauthorized(new LoginRequest("stranger@acme.test", PASSWORD, ClientType.WEB));

		verify(passwordEncoder).matches(eq(PASSWORD), anyString());
	}

	@Test
	void loginWithInactiveUserFailsWithInvalidCredentials() {
		String email = "inactive@acme.test";
		register(email);
		User user = userRepository.findByEmail(email).orElseThrow();
		user.setActive(false);
		userRepository.save(user);

		assertUnauthorized(new LoginRequest(email, PASSWORD, ClientType.WEB));
	}

	@Test
	void loginFailsWhenTheUserHasNoActiveMembership() {
		String email = "no-active-membership@acme.test";
		register(email);
		Membership membership = membershipRepository
				.findByUserId(userRepository.findByEmail(email).orElseThrow().getId()).getFirst();
		membership.setStatus(MembershipStatus.PENDING);
		membershipRepository.save(membership);

		assertUnauthorized(new LoginRequest(email, PASSWORD, ClientType.WEB));
	}

	@Test
	void loginCarriesTheCallersMembershipIntoTheAccessTokenAndSession() {
		String email = "membership-claim@acme.test";
		register(email);

		TokenResponse response = loginSingle(email, ClientType.WEB);

		AuthenticatedUser authenticatedUser = jwtService.parseAccessToken(response.accessToken()).orElseThrow();
		Session session = sessionRepository.findById(authenticatedUser.sessionId()).orElseThrow();
		assertThat(authenticatedUser.membershipId()).isEqualTo(session.getMembershipId());
		assertThat(membershipRepository.findById(session.getMembershipId()).orElseThrow().getUserId())
				.isEqualTo(response.userId());
	}

	@Test
	void validRefreshReturnsNewTokensAndInvalidatesTheOldRefreshToken() {
		String email = "refresh@acme.test";
		register(email);
		TokenResponse loginResponse = loginSingle(email, ClientType.WEB);

		TokenResponse refreshed = authenticationService.refresh(new RefreshRequest(loginResponse.refreshToken()));

		assertThat(refreshed.userId()).isEqualTo(loginResponse.userId());
		AuthenticatedUser refreshedUser = jwtService.parseAccessToken(refreshed.accessToken()).orElseThrow();
		assertThat(refreshTokenService.consume(refreshed.refreshToken())).contains(refreshedUser.sessionId());
		assertUnauthorizedRefresh(new RefreshRequest(loginResponse.refreshToken()));
	}

	@Test
	void refreshSlidesTheSessionsLastUsedAtAndExpiresAtForward() {
		String email = "refresh-slide@acme.test";
		register(email);
		TokenResponse loginResponse = loginSingle(email, ClientType.WEB);
		UUID sessionId = jwtService.parseAccessToken(loginResponse.accessToken()).orElseThrow().sessionId();
		Session sessionAfterLogin = sessionRepository.findById(sessionId).orElseThrow();

		authenticationService.refresh(new RefreshRequest(loginResponse.refreshToken()));

		Session sessionAfterRefresh = sessionRepository.findById(sessionId).orElseThrow();
		assertThat(sessionAfterRefresh.getLastUsedAt()).isAfter(sessionAfterLogin.getLastUsedAt());
		assertThat(sessionAfterRefresh.getExpiresAt()).isAfter(sessionAfterLogin.getExpiresAt());
	}

	@Test
	void refreshFailsForARevokedSessionEvenWithAnUnconsumedRefreshToken() {
		String email = "refresh-revoked@acme.test";
		register(email);
		TokenResponse loginResponse = loginSingle(email, ClientType.WEB);
		UUID sessionId = jwtService.parseAccessToken(loginResponse.accessToken()).orElseThrow().sessionId();
		Session session = sessionRepository.findById(sessionId).orElseThrow();
		session.setRevokedAt(Instant.now());
		sessionRepository.save(session);

		assertUnauthorizedRefresh(new RefreshRequest(loginResponse.refreshToken()));
	}

	@Test
	void refreshFailsOnceTheSessionsExpiresAtHasPassed() {
		String email = "refresh-expired@acme.test";
		register(email);
		TokenResponse loginResponse = loginSingle(email, ClientType.WEB);
		UUID sessionId = jwtService.parseAccessToken(loginResponse.accessToken()).orElseThrow().sessionId();
		Session session = sessionRepository.findById(sessionId).orElseThrow();
		session.setExpiresAt(Instant.now().minusSeconds(1));
		sessionRepository.save(session);

		assertUnauthorizedRefresh(new RefreshRequest(loginResponse.refreshToken()));
	}

	@Test
	void refreshingWithADeactivatedUsersTokenFails() {
		String email = "refresh-inactive@acme.test";
		register(email);
		TokenResponse loginResponse = loginSingle(email, ClientType.WEB);
		User user = userRepository.findById(loginResponse.userId()).orElseThrow();
		user.setActive(false);
		userRepository.save(user);

		assertUnauthorizedRefresh(new RefreshRequest(loginResponse.refreshToken()));
	}

	@Test
	void logoutRevokesTheTokenSoAFollowingRefreshFails() {
		String email = "logout@acme.test";
		register(email);
		TokenResponse loginResponse = loginSingle(email, ClientType.WEB);

		authenticationService.logout(new RefreshRequest(loginResponse.refreshToken()));

		assertUnauthorizedRefresh(new RefreshRequest(loginResponse.refreshToken()));
	}

	@Test
	void logoutMarksTheSessionRevokedInPostgres() {
		String email = "logout-session@acme.test";
		register(email);
		TokenResponse loginResponse = loginSingle(email, ClientType.WEB);
		UUID sessionId = jwtService.parseAccessToken(loginResponse.accessToken()).orElseThrow().sessionId();

		authenticationService.logout(new RefreshRequest(loginResponse.refreshToken()));

		Session session = sessionRepository.findById(sessionId).orElseThrow();
		assertThat(session.getRevokedAt()).isNotNull();
	}

	private void assertUnauthorized(LoginRequest request) {
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> authenticationService.login(request))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED))
				.withMessageContaining("Invalid credentials");
	}

	private void assertUnauthorizedSelect(LoginSelectRequest request) {
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> authenticationService.selectOrganization(request))
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
