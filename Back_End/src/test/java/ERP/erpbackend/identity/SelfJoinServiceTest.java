package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.OrganizationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SelfJoinServiceTest {

	private static final String PASSWORD = "Sunrise8";
	private static final String ACCEPTED_MESSAGE =
			"If those details are valid, check your email for a link to finish your request.";

	@Autowired
	private SelfJoinService selfJoinService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationService organizationService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MembershipRepository membershipRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private EmailVerificationTokenService emailVerificationTokenService;

	@MockitoBean
	private JoinVerificationMailer joinVerificationMailer;

	@MockitoSpyBean
	private PasswordEncoder passwordEncoder;

	private record Org(UUID tenantId, UUID organizationId, String inviteCode, String ownerEmail) {
	}

	private Org registerOrg(String slug) {
		String ownerEmail = "owner-" + slug + "@acme.test";
		TokenResponse account = registrationService.register(
				new RegisterRequest("Acme " + slug, "Ada Owner", ownerEmail, PASSWORD, ClientType.WEB));
		String inviteCode = organizationRepository.findById(account.organizationId()).orElseThrow().getInviteCode();
		return new Org(account.tenantId(), account.organizationId(), inviteCode, ownerEmail);
	}

	private void registerStandaloneAccount(String email) {
		registrationService.register(
				new RegisterRequest("Co " + email, "Bob Member", email, PASSWORD, ClientType.WEB));
	}

	private String tokenFromLink(String link) {
		return link.substring(link.indexOf("token=") + "token=".length());
	}

	private List<AuditLog> joinRequestedAuditRows(UUID actorId) {
		return auditLogRepository.findAll().stream()
				.filter(log -> "membership.join_requested".equals(log.getAction()) && actorId.equals(log.getUserId()))
				.toList();
	}

	@Test
	void unknownInviteCodeIsRejectedWith404AndSendsNothing() {
		assertThatThrownBy(() -> selfJoinService.requestJoin(
				new JoinRequest("newbie@acme.test", PASSWORD, "New Bie", "ZZZZZZZZZZ")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

		verify(joinVerificationMailer, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void inactiveOrganizationInviteCodeIsRejectedWith404() {
		Org org = registerOrg("inactive-join");
		Organization organization = organizationRepository.findById(org.organizationId()).orElseThrow();
		organization.setActive(false);
		organizationRepository.save(organization);

		assertThatThrownBy(() -> selfJoinService.requestJoin(
				new JoinRequest("newbie-inactive@acme.test", PASSWORD, "New Bie", org.inviteCode())))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

		verify(joinVerificationMailer, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void brandNewEmailStoresAnIntentWithAHashAndSendsTheLink() {
		Org org = registerOrg("new-email");
		Mockito.clearInvocations(passwordEncoder);

		SelfJoinResponse response = selfJoinService.requestJoin(
				new JoinRequest("fresh@acme.test", PASSWORD, "Fresh Joiner", org.inviteCode()));

		assertThat(response.message()).isEqualTo(ACCEPTED_MESSAGE);
		verify(passwordEncoder, times(1)).encode(anyString());
		verify(passwordEncoder, never()).matches(anyString(), anyString());
		assertThat(userRepository.findByEmail("fresh@acme.test")).isEmpty();

		ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
		verify(joinVerificationMailer, times(1)).send(eq("fresh@acme.test"), eq("Acme new-email"), link.capture());
		assertThat(link.getValue()).contains("/verify-email?token=");

		JoinIntent stored = emailVerificationTokenService.consume(tokenFromLink(link.getValue())).orElseThrow();
		assertThat(stored.passwordHash()).isNotNull();
		assertThat(passwordEncoder.matches(PASSWORD, stored.passwordHash())).isTrue();
		assertThat(stored.fullName()).isEqualTo("Fresh Joiner");
		assertThat(stored.inviteCode()).isEqualTo(org.inviteCode());
	}

	@Test
	void existingAccountWithCorrectPasswordStoresAnIntentWithNoHash() {
		Org org = registerOrg("existing-correct");
		registerStandaloneAccount("bob-correct@acme.test");
		Mockito.clearInvocations(passwordEncoder);

		SelfJoinResponse response = selfJoinService.requestJoin(
				new JoinRequest("bob-correct@acme.test", PASSWORD, "Bob Member", org.inviteCode()));

		assertThat(response.message()).isEqualTo(ACCEPTED_MESSAGE);
		verify(passwordEncoder, times(1)).matches(anyString(), anyString());
		verify(passwordEncoder, never()).encode(anyString());

		ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
		verify(joinVerificationMailer, times(1)).send(eq("bob-correct@acme.test"), anyString(), link.capture());

		JoinIntent stored = emailVerificationTokenService.consume(tokenFromLink(link.getValue())).orElseThrow();
		assertThat(stored.passwordHash()).isNull();
		assertThat(stored.fullName()).isNull();
		assertThat(stored.email()).isEqualTo("bob-correct@acme.test");
	}

	@Test
	void existingAccountWithWrongPasswordReturns202ButSendsNothing() {
		Org org = registerOrg("existing-wrong");
		registerStandaloneAccount("carol-wrong@acme.test");
		Mockito.clearInvocations(passwordEncoder);

		SelfJoinResponse response = selfJoinService.requestJoin(
				new JoinRequest("carol-wrong@acme.test", "WrongPass9", "Carol", org.inviteCode()));

		assertThat(response.message()).isEqualTo(ACCEPTED_MESSAGE);
		verify(passwordEncoder, times(1)).matches(anyString(), anyString());
		verify(passwordEncoder, never()).encode(anyString());
		verify(joinVerificationMailer, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void existingActiveMemberOfTheOrgReturns202ButSendsNothing() {
		Org org = registerOrg("already-member");
		Mockito.clearInvocations(passwordEncoder);

		SelfJoinResponse response = selfJoinService.requestJoin(
				new JoinRequest(org.ownerEmail(), PASSWORD, "Ada Owner", org.inviteCode()));

		assertThat(response.message()).isEqualTo(ACCEPTED_MESSAGE);
		verify(passwordEncoder, times(1)).matches(anyString(), anyString());
		verify(joinVerificationMailer, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void verifyEmailCreatesTheAccountPendingMembershipAndOneAuditRow() {
		Org org = registerOrg("verify-new");
		String hash = passwordEncoder.encode(PASSWORD);
		String token = emailVerificationTokenService.issue(
				new JoinIntent("fresh-verify@acme.test", hash, "Fresh Verify", org.inviteCode()));

		VerifyEmailResponse response = selfJoinService.verifyEmail(token);

		assertThat(response.organizationName()).isEqualTo("Acme verify-new");
		assertThat(response.message()).contains("Acme verify-new").contains("awaiting approval");

		User created = userRepository.findByEmail("fresh-verify@acme.test").orElseThrow();
		assertThat(created.isActive()).isTrue();
		assertThat(created.getPasswordHash()).isEqualTo(hash);
		assertThat(created.getFullName()).isEqualTo("Fresh Verify");

		assertThat(membershipRepository.findByUserIdAndOrganizationId(created.getId(), org.organizationId()))
				.hasValueSatisfying(membership -> {
					assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING);
					assertThat(membership.getTenantId()).isEqualTo(org.tenantId());
				});

		assertThat(joinRequestedAuditRows(created.getId())).singleElement().satisfies(log -> {
			assertThat(log.getEntityType()).isEqualTo("Membership");
			assertThat(log.getTenantId()).isEqualTo(org.tenantId());
			assertThat(log.getOrganizationId()).isEqualTo(org.organizationId());
			assertThat(log.getBeforeValue()).isNull();
			assertThat(log.getAfterValue()).contains("PENDING").contains("INVITE_CODE");
		});
	}

	@Test
	void verifyEmailForAnExistingAccountAddsAMembershipWithoutTouchingTheAccount() {
		Org org = registerOrg("verify-existing");
		registerStandaloneAccount("existing-verify@acme.test");
		User before = userRepository.findByEmail("existing-verify@acme.test").orElseThrow();
		String originalHash = before.getPasswordHash();

		String token = emailVerificationTokenService.issue(
				new JoinIntent("existing-verify@acme.test", null, null, org.inviteCode()));

		selfJoinService.verifyEmail(token);

		User after = userRepository.findByEmail("existing-verify@acme.test").orElseThrow();
		assertThat(after.getPasswordHash()).isEqualTo(originalHash);
		assertThat(after.getFullName()).isEqualTo("Bob Member");
		assertThat(after.isActive()).isTrue();
		assertThat(membershipRepository.findByUserIdAndOrganizationId(after.getId(), org.organizationId()))
				.hasValueSatisfying(membership -> assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING));
	}

	@Test
	void verifyEmailWithAnUnknownTokenReturns400() {
		assertThatThrownBy(() -> selfJoinService.verifyEmail("not-a-real-token"))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void verifyEmailWithAReusedTokenReturns400OnTheSecondCall() {
		Org org = registerOrg("verify-reuse");
		String token = emailVerificationTokenService.issue(new JoinIntent(
				"reuse-verify@acme.test", passwordEncoder.encode(PASSWORD), "Re Use", org.inviteCode()));
		selfJoinService.verifyEmail(token);

		assertThatThrownBy(() -> selfJoinService.verifyEmail(token))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void verifyEmailAfterTheInviteCodeRotatesReturns409AndCreatesNoAccount() {
		Org org = registerOrg("verify-rotate");
		String token = emailVerificationTokenService.issue(new JoinIntent(
				"rotate-verify@acme.test", passwordEncoder.encode(PASSWORD), "Ro Tate", org.inviteCode()));
		organizationService.rotateInviteCode(org.organizationId());

		assertThatThrownBy(() -> selfJoinService.verifyEmail(token))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

		assertThat(userRepository.findByEmail("rotate-verify@acme.test")).isEmpty();
	}

	@Test
	void verifyEmailAfterTheOrganizationIsDeactivatedReturns409() {
		Org org = registerOrg("verify-deactivate");
		String token = emailVerificationTokenService.issue(new JoinIntent(
				"deactivate-verify@acme.test", passwordEncoder.encode(PASSWORD), "De Act", org.inviteCode()));
		Organization organization = organizationRepository.findById(org.organizationId()).orElseThrow();
		organization.setActive(false);
		organizationRepository.save(organization);

		assertThatThrownBy(() -> selfJoinService.verifyEmail(token))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void verifyEmailWhenAlreadyAnActiveMemberReturns200WithNoNewRowOrAudit() {
		Org org = registerOrg("verify-active-member");
		User owner = userRepository.findByEmail(org.ownerEmail()).orElseThrow();
		String token = emailVerificationTokenService.issue(
				new JoinIntent(org.ownerEmail(), null, null, org.inviteCode()));

		VerifyEmailResponse response = selfJoinService.verifyEmail(token);

		assertThat(response.message()).isEqualTo("You are already a member of this organization.");
		assertThat(response.organizationName()).isEqualTo("Acme verify-active-member");
		assertThat(membershipRepository.findByUserId(owner.getId())).singleElement()
				.satisfies(membership -> assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE));
		assertThat(joinRequestedAuditRows(owner.getId())).isEmpty();
	}

	@Test
	void verifyEmailWhenAlreadyPendingReturns200WithNoSecondRowOrAudit() {
		Org org = registerOrg("verify-pending");
		String firstToken = emailVerificationTokenService.issue(new JoinIntent(
				"pending-verify@acme.test", passwordEncoder.encode(PASSWORD), "Pen Ding", org.inviteCode()));
		selfJoinService.verifyEmail(firstToken);
		User joiner = userRepository.findByEmail("pending-verify@acme.test").orElseThrow();

		String secondToken = emailVerificationTokenService.issue(
				new JoinIntent("pending-verify@acme.test", null, null, org.inviteCode()));
		VerifyEmailResponse response = selfJoinService.verifyEmail(secondToken);

		assertThat(response.message()).isEqualTo("Your request to join is already awaiting approval.");
		assertThat(membershipRepository.findByUserId(joiner.getId())).hasSize(1);
		assertThat(joinRequestedAuditRows(joiner.getId())).hasSize(1);
	}

}
