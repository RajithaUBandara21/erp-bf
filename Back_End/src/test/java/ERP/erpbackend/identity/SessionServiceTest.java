package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.OrganizationService;
import ERP.erpbackend.organization.TenantOrganization;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
	private JwtService jwtService;

	@Autowired
	private SessionService sessionService;

	@Autowired
	private OrganizationService organizationService;

	@Autowired
	private MembershipRepository membershipRepository;

	@Autowired
	private SessionRepository sessionRepository;

	@MockitoSpyBean
	private RevokedSessionRegistry revokedSessionRegistry;

	/** Registration itself issues a session/token pair now, so it counts as one of the user's devices. */
	private AuthenticatedUser registerOrg(String email) {
		TokenResponse response = registrationService.register(
				new RegisterRequest("Acme Corp " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.accessToken()).orElseThrow();
	}

	private AuthenticatedUser loginAs(String email) {
		LoginResponse response = authenticationService.login(new LoginRequest(email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.session().accessToken()).orElseThrow();
	}

	@Test
	void listReturnsOnlyTheCallersOwnActiveSessionsWithCurrentFlaggedCorrectly() {
		String email = "list@acme.test";
		AuthenticatedUser registrationSession = registerOrg(email);
		AuthenticatedUser firstDevice = loginAs(email);
		AuthenticatedUser secondDevice = loginAs(email);

		List<SessionResponse> sessions = sessionService.listSessions(secondDevice);

		assertThat(sessions).hasSize(3);
		assertThat(sessions.stream().filter(SessionResponse::current).map(SessionResponse::id))
				.containsExactly(secondDevice.sessionId());
		assertThat(sessions).extracting(SessionResponse::id)
				.containsExactlyInAnyOrder(
						registrationSession.sessionId(), firstDevice.sessionId(), secondDevice.sessionId());
	}

	@Test
	void listDoesNotReturnAnotherUsersSessions() {
		String emailA = "owner-a@acme.test";
		AuthenticatedUser registrationA = registerOrg(emailA);
		AuthenticatedUser userA = loginAs(emailA);

		String emailB = "owner-b@acme.test";
		registerOrg(emailB);
		loginAs(emailB);

		List<SessionResponse> sessions = sessionService.listSessions(userA);

		assertThat(sessions).extracting(SessionResponse::id)
				.containsExactlyInAnyOrder(registrationA.sessionId(), userA.sessionId());
	}

	@Test
	void revokingSomeoneElsesSessionIdIsRejected() {
		String emailA = "victim@acme.test";
		AuthenticatedUser registrationA = registerOrg(emailA);
		AuthenticatedUser victim = loginAs(emailA);

		String emailB = "attacker@acme.test";
		registerOrg(emailB);
		AuthenticatedUser attacker = loginAs(emailB);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> sessionService.revokeSession(attacker, victim.sessionId()))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

		assertThat(sessionService.listSessions(victim).stream().map(SessionResponse::id))
				.containsExactlyInAnyOrder(registrationA.sessionId(), victim.sessionId());
	}

	@Test
	void revokingAnUnknownSessionIdIsRejected() {
		String email = "unknown@acme.test";
		registerOrg(email);
		AuthenticatedUser user = loginAs(email);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> sessionService.revokeSession(user, UUID.randomUUID()))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void revokeOthersLeavesOnlyTheCurrentSessionListed() {
		String email = "revoke-others@acme.test";
		registerOrg(email);
		loginAs(email);
		loginAs(email);
		AuthenticatedUser thirdDevice = loginAs(email);
		assertThat(sessionService.listSessions(thirdDevice)).hasSize(4);

		sessionService.revokeOtherSessions(thirdDevice);

		List<SessionResponse> remaining = sessionService.listSessions(thirdDevice);
		assertThat(remaining).extracting(SessionResponse::id).containsExactly(thirdDevice.sessionId());
		assertThat(remaining.get(0).current()).isTrue();
	}

	@Test
	void revokeOthersRecordsEveryOtherSessionInTheRegistryButNotTheCurrentOne() {
		String email = "revoke-registry@acme.test";
		registerOrg(email);
		loginAs(email);
		loginAs(email);
		AuthenticatedUser current = loginAs(email);

		List<UUID> otherIds = sessionService.listSessions(current).stream()
				.filter(session -> !session.current())
				.map(SessionResponse::id)
				.toList();
		assertThat(otherIds).hasSize(3);

		sessionService.revokeOtherSessions(current);

		otherIds.forEach(id -> verify(revokedSessionRegistry).revoke(id));
		verify(revokedSessionRegistry, never()).revoke(current.sessionId());
	}

	@Test
	void revokeOthersStillCompletesAndPersistsWhenTheRegistryCannotReachRedis() {
		doThrow(new DataAccessResourceFailureException("redis unavailable"))
				.when(revokedSessionRegistry).revoke(any(UUID.class));

		String email = "revoke-redis-down@acme.test";
		registerOrg(email);
		AuthenticatedUser current = loginAs(email);

		assertThatNoException().isThrownBy(() -> sessionService.revokeOtherSessions(current));

		assertThat(sessionService.listSessions(current))
				.extracting(SessionResponse::id)
				.containsExactly(current.sessionId());
	}

	@Test
	void listAndIndividualRevokeReachSessionsScopedToAnotherTenant() {
		CrossTenantUser user = userWithSessionsInTwoTenants("cross-tenant-list@acme.test");

		assertThat(sessionService.listSessions(user.deviceInTenantA()))
				.extracting(SessionResponse::id)
				.containsExactlyInAnyOrder(user.deviceInTenantA().sessionId(), user.sessionInTenantB());

		sessionService.revokeSession(user.deviceInTenantA(), user.sessionInTenantB());

		assertThat(sessionService.listSessions(user.deviceInTenantA()))
				.extracting(SessionResponse::id)
				.containsExactly(user.deviceInTenantA().sessionId());
	}

	@Test
	void revokeOtherSessionsReachesSessionsScopedToAnotherTenant() {
		CrossTenantUser user = userWithSessionsInTwoTenants("cross-tenant-revoke-others@acme.test");

		sessionService.revokeOtherSessions(user.deviceInTenantA());

		assertThat(sessionService.listSessions(user.deviceInTenantA()))
				.extracting(SessionResponse::id)
				.containsExactly(user.deviceInTenantA().sessionId());
		verify(revokedSessionRegistry).revoke(user.sessionInTenantB());
	}

	private record CrossTenantUser(AuthenticatedUser deviceInTenantA, UUID sessionInTenantB) {
	}

	/**
	 * One account with an ACTIVE Membership and a live device session in each of two Tenants. No product
	 * flow grants cross-tenant memberships yet, so the second Tenant's Membership and Session are built
	 * straight through the repositories.
	 */
	private CrossTenantUser userWithSessionsInTwoTenants(String email) {
		AuthenticatedUser deviceInTenantA = registerOrg(email);

		TenantOrganization tenantB = organizationService.createTenantAndOrganization("Zenith " + email);
		Membership membershipInTenantB = new Membership();
		membershipInTenantB.setUserId(deviceInTenantA.userId());
		membershipInTenantB.setTenantId(tenantB.tenantId());
		membershipInTenantB.setOrganizationId(tenantB.organizationId());
		membershipInTenantB.setStatus(MembershipStatus.ACTIVE);
		membershipInTenantB = membershipRepository.save(membershipInTenantB);

		Instant now = Instant.now();
		Session sessionInTenantB = new Session();
		sessionInTenantB.setUserId(deviceInTenantA.userId());
		sessionInTenantB.setMembershipId(membershipInTenantB.getId());
		sessionInTenantB.setClientType(ClientType.MOBILE);
		sessionInTenantB.setLastUsedAt(now);
		sessionInTenantB.setExpiresAt(now.plusSeconds(3600));
		sessionInTenantB = sessionRepository.save(sessionInTenantB);

		return new CrossTenantUser(deviceInTenantA, sessionInTenantB.getId());
	}

}
