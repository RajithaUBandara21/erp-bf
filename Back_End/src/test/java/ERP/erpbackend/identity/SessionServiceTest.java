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

	private String registerAndGetOrganizationCode(String email) {
		RegisteredAccount account = registrationService.register(
				new RegisterRequest("Acme Corp " + email, "Ada Owner", email, PASSWORD));
		Tenant tenant = tenantRepository.findById(account.tenantId()).orElseThrow();
		return tenant.getCode();
	}

	private AuthenticatedUser loginAs(String organizationCode, String email) {
		TokenResponse response = authenticationService.login(
				new LoginRequest(organizationCode, email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.accessToken()).orElseThrow();
	}

	@Test
	void listReturnsOnlyTheCallersOwnActiveSessionsWithCurrentFlaggedCorrectly() {
		String email = "list@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);
		AuthenticatedUser firstDevice = loginAs(organizationCode, email);
		AuthenticatedUser secondDevice = loginAs(organizationCode, email);

		List<SessionResponse> sessions = sessionService.listSessions(secondDevice);

		assertThat(sessions).hasSize(2);
		assertThat(sessions.stream().filter(SessionResponse::current).map(SessionResponse::id))
				.containsExactly(secondDevice.sessionId());
		assertThat(sessions).extracting(SessionResponse::id)
				.containsExactlyInAnyOrder(firstDevice.sessionId(), secondDevice.sessionId());
	}

	@Test
	void listDoesNotReturnAnotherUsersSessions() {
		String emailA = "owner-a@acme.test";
		String organizationCodeA = registerAndGetOrganizationCode(emailA);
		AuthenticatedUser userA = loginAs(organizationCodeA, emailA);

		String emailB = "owner-b@acme.test";
		String organizationCodeB = registerAndGetOrganizationCode(emailB);
		loginAs(organizationCodeB, emailB);

		List<SessionResponse> sessions = sessionService.listSessions(userA);

		assertThat(sessions).extracting(SessionResponse::id).containsExactly(userA.sessionId());
	}

	@Test
	void revokingSomeoneElsesSessionIdIsRejected() {
		String emailA = "victim@acme.test";
		String organizationCodeA = registerAndGetOrganizationCode(emailA);
		AuthenticatedUser victim = loginAs(organizationCodeA, emailA);

		String emailB = "attacker@acme.test";
		String organizationCodeB = registerAndGetOrganizationCode(emailB);
		AuthenticatedUser attacker = loginAs(organizationCodeB, emailB);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> sessionService.revokeSession(attacker, victim.sessionId()))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

		assertThat(sessionService.listSessions(victim).stream().map(SessionResponse::id))
				.containsExactly(victim.sessionId());
	}

	@Test
	void revokingAnUnknownSessionIdIsRejected() {
		String email = "unknown@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);
		AuthenticatedUser user = loginAs(organizationCode, email);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> sessionService.revokeSession(user, UUID.randomUUID()))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void revokeOthersLeavesOnlyTheCurrentSessionListed() {
		String email = "revoke-others@acme.test";
		String organizationCode = registerAndGetOrganizationCode(email);
		loginAs(organizationCode, email);
		loginAs(organizationCode, email);
		AuthenticatedUser thirdDevice = loginAs(organizationCode, email);
		assertThat(sessionService.listSessions(thirdDevice)).hasSize(3);

		sessionService.revokeOtherSessions(thirdDevice);

		List<SessionResponse> remaining = sessionService.listSessions(thirdDevice);
		assertThat(remaining).extracting(SessionResponse::id).containsExactly(thirdDevice.sessionId());
		assertThat(remaining.get(0).current()).isTrue();
	}

}
