package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SessionServiceTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private SessionService sessionService;

	private record RegisteredOrg(String organizationCode, AuthenticatedUser registrationSession) {
	}

	/** Registration itself issues a session/token pair now, so it counts as one of the user's devices. */
	private RegisteredOrg registerOrg(String email) {
		TokenResponse response = registrationService.register(
				new RegisterRequest("Acme Corp " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		Tenant tenant = tenantRepository.findById(response.tenantId()).orElseThrow();
		AuthenticatedUser registrationSession = jwtService.parseAccessToken(response.accessToken()).orElseThrow();
		return new RegisteredOrg(tenant.getCode(), registrationSession);
	}

	private AuthenticatedUser loginAs(String organizationCode, String email) {
		TokenResponse response = authenticationService.login(
				new LoginRequest(organizationCode, email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.accessToken()).orElseThrow();
	}

	@Test
	void listReturnsOnlyTheCallersOwnActiveSessionsWithCurrentFlaggedCorrectly() {
		String email = "list@acme.test";
		RegisteredOrg org = registerOrg(email);
		AuthenticatedUser firstDevice = loginAs(org.organizationCode(), email);
		AuthenticatedUser secondDevice = loginAs(org.organizationCode(), email);

		List<SessionResponse> sessions = sessionService.listSessions(secondDevice);

		assertThat(sessions).hasSize(3);
		assertThat(sessions.stream().filter(SessionResponse::current).map(SessionResponse::id))
				.containsExactly(secondDevice.sessionId());
		assertThat(sessions).extracting(SessionResponse::id)
				.containsExactlyInAnyOrder(
						org.registrationSession().sessionId(), firstDevice.sessionId(), secondDevice.sessionId());
	}

	@Test
	void listDoesNotReturnAnotherUsersSessions() {
		String emailA = "owner-a@acme.test";
		RegisteredOrg orgA = registerOrg(emailA);
		AuthenticatedUser userA = loginAs(orgA.organizationCode(), emailA);

		String emailB = "owner-b@acme.test";
		RegisteredOrg orgB = registerOrg(emailB);
		loginAs(orgB.organizationCode(), emailB);

		List<SessionResponse> sessions = sessionService.listSessions(userA);

		assertThat(sessions).extracting(SessionResponse::id)
				.containsExactlyInAnyOrder(orgA.registrationSession().sessionId(), userA.sessionId());
	}

	@Test
	void revokingSomeoneElsesSessionIdIsRejected() {
		String emailA = "victim@acme.test";
		RegisteredOrg orgA = registerOrg(emailA);
		AuthenticatedUser victim = loginAs(orgA.organizationCode(), emailA);

		String emailB = "attacker@acme.test";
		RegisteredOrg orgB = registerOrg(emailB);
		AuthenticatedUser attacker = loginAs(orgB.organizationCode(), emailB);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> sessionService.revokeSession(attacker, victim.sessionId()))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

		assertThat(sessionService.listSessions(victim).stream().map(SessionResponse::id))
				.containsExactlyInAnyOrder(orgA.registrationSession().sessionId(), victim.sessionId());
	}

	@Test
	void revokingAnUnknownSessionIdIsRejected() {
		String email = "unknown@acme.test";
		RegisteredOrg org = registerOrg(email);
		AuthenticatedUser user = loginAs(org.organizationCode(), email);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> sessionService.revokeSession(user, UUID.randomUUID()))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void revokeOthersLeavesOnlyTheCurrentSessionListed() {
		String email = "revoke-others@acme.test";
		RegisteredOrg org = registerOrg(email);
		loginAs(org.organizationCode(), email);
		loginAs(org.organizationCode(), email);
		AuthenticatedUser thirdDevice = loginAs(org.organizationCode(), email);
		assertThat(sessionService.listSessions(thirdDevice)).hasSize(4);

		sessionService.revokeOtherSessions(thirdDevice);

		List<SessionResponse> remaining = sessionService.listSessions(thirdDevice);
		assertThat(remaining).extracting(SessionResponse::id).containsExactly(thirdDevice.sessionId());
		assertThat(remaining.get(0).current()).isTrue();
	}

}
