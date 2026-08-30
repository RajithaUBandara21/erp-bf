package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.OrganizationService;
import ERP.erpbackend.organization.TenantOrganization;
import java.time.Instant;
import java.util.Comparator;
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
class OrganizationSwitchServiceTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private OrganizationSwitchService organizationSwitchService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private OrganizationService organizationService;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MembershipRepository membershipRepository;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private record Account(UUID userId, UUID tenantId, UUID orgId, String email, String accessToken,
			String refreshToken) {
	}

	private Account register(String email) {
		TokenResponse account = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return new Account(account.userId(), account.tenantId(), account.organizationId(), email,
				account.accessToken(), account.refreshToken());
	}

	/** The caller as the JWT filter would build it: real session id and membership id from the token. */
	private AuthenticatedUser realPrincipal(Account account) {
		return jwtService.parseAccessToken(account.accessToken()).orElseThrow();
	}

	private List<AuditLog> switchAuditRows(UUID sessionId) {
		return auditLogRepository.findAll().stream()
				.filter(log -> "auth.organization_switched".equals(log.getAction())
						&& sessionId.equals(log.getEntityId()))
				.toList();
	}

	private List<String> auditActionsFor(UUID userId) {
		return auditLogRepository.findAll().stream()
				.filter(log -> userId.equals(log.getUserId()))
				.map(AuditLog::getAction)
				.toList();
	}

	private UUID siblingOrg(UUID tenantId, String code, boolean active) {
		Organization organization = new Organization();
		organization.setTenantId(tenantId);
		organization.setName(code);
		organization.setCode(code);
		organization.setActive(active);
		return organizationRepository.save(organization).getId();
	}

	private UUID addActiveMembership(UUID userId, UUID tenantId, UUID organizationId) {
		Membership membership = new Membership();
		membership.setUserId(userId);
		membership.setTenantId(tenantId);
		membership.setOrganizationId(organizationId);
		membership.setStatus(MembershipStatus.ACTIVE);
		return membershipRepository.save(membership).getId();
	}

	private UUID createUser(String email) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName(email);
		return userRepository.save(user).getId();
	}

	private AuthenticatedUser principalFor(Account account) {
		return new AuthenticatedUser(account.userId(), account.tenantId(), account.orgId(), account.email(),
				UUID.randomUUID(), UUID.randomUUID());
	}

	@Test
	void listsEveryActiveMembershipOrgWithCurrentFlaggedAndRowsSorted() {
		Account home = register("switch-two-tenants@acme.test");
		TenantOrganization second = organizationService.createTenantAndOrganization("Zenith Trading");
		addActiveMembership(home.userId(), second.tenantId(), second.organizationId());

		List<ReachableOrganizationResponse> rows = organizationSwitchService.listReachable(principalFor(home));

		assertThat(rows).extracting(ReachableOrganizationResponse::organizationId)
				.containsExactlyInAnyOrder(home.orgId(), second.organizationId());
		assertThat(rows).filteredOn(ReachableOrganizationResponse::current)
				.extracting(ReachableOrganizationResponse::organizationId)
				.containsExactly(home.orgId());
		assertThat(rows).allSatisfy(row -> assertThat(row.viaTenantAdmin()).isFalse());
		assertThat(rows).isSortedAccordingTo(Comparator
				.comparing(ReachableOrganizationResponse::tenantName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(ReachableOrganizationResponse::organizationName, String.CASE_INSENSITIVE_ORDER));
	}

	@Test
	void tenantAdminAlsoSeesActiveSiblingOrgsAndNotInactiveOnes() {
		Account home = register("switch-tenant-admin@acme.test");
		UUID activeSibling = siblingOrg(home.tenantId(), "switch-ta-active-sibling", true);
		siblingOrg(home.tenantId(), "switch-ta-inactive-sibling", false);

		List<ReachableOrganizationResponse> rows = organizationSwitchService.listReachable(principalFor(home));

		assertThat(rows).extracting(ReachableOrganizationResponse::organizationId)
				.containsExactlyInAnyOrder(home.orgId(), activeSibling);
		assertThat(rows).filteredOn(ReachableOrganizationResponse::viaTenantAdmin)
				.extracting(ReachableOrganizationResponse::organizationId)
				.containsExactly(activeSibling);
		assertThat(rows).filteredOn(row -> row.organizationId().equals(home.orgId()))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.current()).isTrue();
					assertThat(row.viaTenantAdmin()).isFalse();
				});
	}

	@Test
	void plainMultiOrgMemberWithoutTenantAdminRoleSeesOnlyHeldMemberships() {
		Account home = register("switch-plain-member@acme.test");
		siblingOrg(home.tenantId(), "switch-plain-sibling", true);
		UUID plainUserId = createUser("plain@switch-plain-member.test");
		UUID membershipId = addActiveMembership(plainUserId, home.tenantId(), home.orgId());

		AuthenticatedUser plain = new AuthenticatedUser(plainUserId, home.tenantId(), home.orgId(),
				"plain@switch-plain-member.test", UUID.randomUUID(), membershipId);
		List<ReachableOrganizationResponse> rows = organizationSwitchService.listReachable(plain);

		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.organizationId()).isEqualTo(home.orgId());
			assertThat(row.current()).isTrue();
			assertThat(row.viaTenantAdmin()).isFalse();
		});
	}

	@Test
	void switchingToAnotherActiveMembershipReissuesTokensMovesTheSessionAndAuditsOnce() {
		Account home = register("switch-existing@acme.test");
		TenantOrganization second = organizationService.createTenantAndOrganization("Zenith Trading");
		UUID secondMembershipId = addActiveMembership(home.userId(), second.tenantId(), second.organizationId());
		AuthenticatedUser caller = realPrincipal(home);

		TokenResponse tokens = organizationSwitchService.switchOrganization(caller, second.organizationId());

		AuthenticatedUser switched = jwtService.parseAccessToken(tokens.accessToken()).orElseThrow();
		assertThat(switched.membershipId()).isEqualTo(secondMembershipId);
		assertThat(switched.organizationId()).isEqualTo(second.organizationId());

		Session session = sessionRepository.findById(caller.sessionId()).orElseThrow();
		assertThat(session.getMembershipId()).isEqualTo(secondMembershipId);
		assertThat(session.getTenantId()).isEqualTo(second.tenantId());

		assertThat(switchAuditRows(caller.sessionId())).singleElement().satisfies(log -> {
			assertThat(log.getEntityType()).isEqualTo("Session");
			assertThat(log.getUserId()).isEqualTo(home.userId());
			assertThat(log.getTenantId()).isEqualTo(second.tenantId());
			assertThat(log.getOrganizationId()).isEqualTo(second.organizationId());
			assertThat(log.getBeforeValue()).contains(home.orgId().toString());
			assertThat(log.getAfterValue()).contains(second.organizationId().toString());
		});

		// The refresh token issued at login still works and now resolves to the switched org.
		TokenResponse refreshed = authenticationService.refresh(new RefreshRequest(home.refreshToken()));
		assertThat(jwtService.parseAccessToken(refreshed.accessToken()).orElseThrow().organizationId())
				.isEqualTo(second.organizationId());
	}

	@Test
	void tenantAdminSwitchingToANotYetJoinedSiblingOrgAutoProvisionsThenSwitches() {
		Account home = register("switch-ta@acme.test");
		UUID siblingOrgId = siblingOrg(home.tenantId(), "switch-ta-sibling", true);
		AuthenticatedUser caller = realPrincipal(home);

		TokenResponse tokens = organizationSwitchService.switchOrganization(caller, siblingOrgId);

		Membership provisioned = membershipRepository.findByUserIdAndOrganizationId(home.userId(), siblingOrgId)
				.orElseThrow();
		assertThat(provisioned.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

		AuthenticatedUser switched = jwtService.parseAccessToken(tokens.accessToken()).orElseThrow();
		assertThat(switched.membershipId()).isEqualTo(provisioned.getId());
		assertThat(switched.organizationId()).isEqualTo(siblingOrgId);
		assertThat(sessionRepository.findById(caller.sessionId()).orElseThrow().getMembershipId())
				.isEqualTo(provisioned.getId());

		assertThat(auditActionsFor(home.userId()))
				.contains("membership.auto_provisioned", "auth.organization_switched");
	}

	@Test
	void switchingToTheCurrentOrganizationIsRejectedAndLeavesTheSessionAndAuditUntouched() {
		Account home = register("switch-noop@acme.test");
		AuthenticatedUser caller = realPrincipal(home);
		Session before = sessionRepository.findById(caller.sessionId()).orElseThrow();

		assertThatThrownBy(() -> organizationSwitchService.switchOrganization(caller, home.orgId()))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

		assertThat(switchAuditRows(caller.sessionId())).isEmpty();
		assertThat(sessionRepository.findById(caller.sessionId()).orElseThrow().getMembershipId())
				.isEqualTo(before.getMembershipId());
	}

	@Test
	void switchingToAnOrganizationTheCallerCannotReachIsForbidden() {
		Account home = register("switch-forbidden@acme.test");
		Account otherTenant = register("switch-forbidden-other@acme.test");
		AuthenticatedUser caller = realPrincipal(home);

		assertThatThrownBy(() -> organizationSwitchService.switchOrganization(caller, otherTenant.orgId()))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void switchingToAnUnknownOrganizationIsNotFound() {
		Account home = register("switch-unknown@acme.test");
		AuthenticatedUser caller = realPrincipal(home);

		assertThatThrownBy(() -> organizationSwitchService.switchOrganization(caller, UUID.randomUUID()))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void switchingWithARevokedSessionIsUnauthorized() {
		Account home = register("switch-revoked@acme.test");
		TenantOrganization second = organizationService.createTenantAndOrganization("Zenith Trading");
		addActiveMembership(home.userId(), second.tenantId(), second.organizationId());
		AuthenticatedUser caller = realPrincipal(home);
		Session session = sessionRepository.findById(caller.sessionId()).orElseThrow();
		session.setRevokedAt(Instant.now());
		sessionRepository.save(session);

		assertThatThrownBy(() -> organizationSwitchService.switchOrganization(caller, second.organizationId()))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}
}
