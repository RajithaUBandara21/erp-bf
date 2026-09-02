package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import ERP.erpbackend.organization.OrganizationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MembershipApprovalServiceTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private MembershipApprovalService membershipApprovalService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private SelfJoinService selfJoinService;

	@Autowired
	private EmailVerificationTokenService emailVerificationTokenService;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MembershipRepository membershipRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private AuthenticatedUser registerOwner(String email) {
		TokenResponse response = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.accessToken()).orElseThrow();
	}

	private User createUser(String email, String fullName) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName(fullName);
		return userRepository.save(user);
	}

	private Membership addMembership(UUID userId, UUID tenantId, UUID organizationId, MembershipStatus status) {
		Membership membership = new Membership();
		membership.setUserId(userId);
		membership.setTenantId(tenantId);
		membership.setOrganizationId(organizationId);
		membership.setStatus(status);
		return membershipRepository.saveAndFlush(membership);
	}

	private Membership createRequest(AuthenticatedUser org, String email, String fullName, MembershipStatus status) {
		User user = createUser(email, fullName);
		return addMembership(user.getId(), org.tenantId(), org.organizationId(), status);
	}

	private AuthenticatedUser administrator(AuthenticatedUser owner, String email) {
		User user = createUser(email, email);
		addMembership(user.getId(), owner.tenantId(), owner.organizationId(), MembershipStatus.ACTIVE);
		roleService.assignMember(owner, roleIdNamed(owner, "Administrator"), user.getId());
		Membership membership = membershipRepository.findByUserId(user.getId()).getFirst();
		return new AuthenticatedUser(user.getId(), membership.getTenantId(), membership.getOrganizationId(),
				user.getEmail(), UUID.randomUUID(), membership.getId());
	}

	private UUID roleIdNamed(AuthenticatedUser caller, String name) {
		return roleService.listRoles(caller).stream()
				.filter(role -> role.name().equals(name)).findFirst().orElseThrow().id();
	}

	private List<AuditLog> auditRows(UUID entityId, String action) {
		return auditLogRepository.findAll().stream()
				.filter(log -> entityId.equals(log.getEntityId()) && action.equals(log.getAction()))
				.toList();
	}

	/** created_at is @CreatedDate/updatable=false, so backdate it with a raw statement. */
	private void backdate(UUID membershipId, Instant requestedAt) {
		jdbcTemplate.update("UPDATE memberships SET created_at = ? WHERE id = ?",
				Timestamp.from(requestedAt), membershipId);
	}

	private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus status) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
				ex -> assertThat(ex.getStatusCode()).isEqualTo(status));
	}

	@Test
	void pendingRequestInTheCallersOrgIsListedWithUserFieldsAndRequestedAt() {
		AuthenticatedUser owner = registerOwner("approval-list-fields@acme.test");
		Instant requestedAt = Instant.parse("2026-08-01T10:15:00Z");
		Membership request = createRequest(owner, "joiner@approval-list-fields.test", "Joe Joiner",
				MembershipStatus.PENDING);
		backdate(request.getId(), requestedAt);

		List<PendingJoinRequestResponse> pending = membershipApprovalService.listPending(owner);

		assertThat(pending).singleElement().satisfies(entry -> {
			assertThat(entry.membershipId()).isEqualTo(request.getId());
			assertThat(entry.userId()).isEqualTo(request.getUserId());
			assertThat(entry.fullName()).isEqualTo("Joe Joiner");
			assertThat(entry.email()).isEqualTo("joiner@approval-list-fields.test");
			assertThat(entry.requestedAt()).isEqualTo(requestedAt);
		});
	}

	@Test
	void activeMembershipInTheSameOrgIsExcluded() {
		AuthenticatedUser owner = registerOwner("approval-list-active@acme.test");
		createRequest(owner, "active@approval-list-active.test", "Amy Active", MembershipStatus.ACTIVE);
		Membership pendingRequest = createRequest(owner, "pending@approval-list-active.test", "Pat Pending",
				MembershipStatus.PENDING);

		List<PendingJoinRequestResponse> pending = membershipApprovalService.listPending(owner);

		assertThat(pending).extracting(PendingJoinRequestResponse::membershipId)
				.containsExactly(pendingRequest.getId());
	}

	@Test
	void pendingMembershipInAnotherOrgIsExcluded() {
		AuthenticatedUser ownerA = registerOwner("approval-list-org-a@acme.test");
		AuthenticatedUser ownerB = registerOwner("approval-list-org-b@acme.test");
		Membership requestForA = createRequest(ownerA, "joiner@approval-list-org-a.test", "Ann A",
				MembershipStatus.PENDING);
		createRequest(ownerB, "joiner@approval-list-org-b.test", "Bob B", MembershipStatus.PENDING);

		List<PendingJoinRequestResponse> pending = membershipApprovalService.listPending(ownerA);

		assertThat(pending).extracting(PendingJoinRequestResponse::membershipId)
				.containsExactly(requestForA.getId());
	}

	@Test
	void multipleRequestsAreReturnedOldestFirst() {
		AuthenticatedUser owner = registerOwner("approval-list-order@acme.test");
		Membership newer = createRequest(owner, "newer@approval-list-order.test", "Nora Newer",
				MembershipStatus.PENDING);
		Membership older = createRequest(owner, "older@approval-list-order.test", "Otto Older",
				MembershipStatus.PENDING);
		backdate(newer.getId(), Instant.parse("2026-08-02T00:00:00Z"));
		backdate(older.getId(), Instant.parse("2026-08-01T00:00:00Z"));

		List<PendingJoinRequestResponse> pending = membershipApprovalService.listPending(owner);

		assertThat(pending).extracting(PendingJoinRequestResponse::membershipId)
				.containsExactly(older.getId(), newer.getId());
	}

	@Test
	void approveActivatesTheMembershipAssignsTheRoleAndWritesBothAuditRows() {
		AuthenticatedUser owner = registerOwner("approval-approve-ok@acme.test");
		UUID viewerRoleId = roleIdNamed(owner, "Viewer");
		Membership request = createRequest(owner, "joiner@approval-approve-ok.test", "Joe Joiner",
				MembershipStatus.PENDING);

		membershipApprovalService.approve(owner, request.getId(), viewerRoleId);

		assertThat(membershipRepository.findById(request.getId())).hasValueSatisfying(
				membership -> assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE));
		assertThat(userRoleRepository.existsByMembershipIdAndRoleId(request.getId(), viewerRoleId)).isTrue();

		assertThat(auditRows(request.getId(), "membership.approved")).singleElement().satisfies(log -> {
			assertThat(log.getEntityType()).isEqualTo("Membership");
			assertThat(log.getTenantId()).isEqualTo(owner.tenantId());
			assertThat(log.getOrganizationId()).isEqualTo(owner.organizationId());
			assertThat(log.getUserId()).isEqualTo(owner.userId());
			assertThat(log.getBeforeValue()).contains("PENDING");
			assertThat(log.getAfterValue()).contains("ACTIVE").contains(viewerRoleId.toString());
		});
		assertThat(auditRows(viewerRoleId, "role.member_assigned")).hasSize(1);
	}

	@Test
	void approveOnAnUnknownMembershipIdIs404() {
		AuthenticatedUser owner = registerOwner("approval-approve-unknown@acme.test");
		UUID viewerRoleId = roleIdNamed(owner, "Viewer");

		assertStatus(() -> membershipApprovalService.approve(owner, UUID.randomUUID(), viewerRoleId),
				HttpStatus.NOT_FOUND);
	}

	@Test
	void approveOnAPendingRequestInAnotherOrgIs404AndChangesNothing() {
		AuthenticatedUser ownerA = registerOwner("approval-approve-crossorg-a@acme.test");
		AuthenticatedUser ownerB = registerOwner("approval-approve-crossorg-b@acme.test");
		UUID viewerRoleA = roleIdNamed(ownerA, "Viewer");
		Membership requestInB = createRequest(ownerB, "joiner@approval-approve-crossorg.test", "Joe Joiner",
				MembershipStatus.PENDING);

		assertStatus(() -> membershipApprovalService.approve(ownerA, requestInB.getId(), viewerRoleA),
				HttpStatus.NOT_FOUND);

		assertThat(membershipRepository.findById(requestInB.getId())).hasValueSatisfying(
				membership -> assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING));
		assertThat(auditRows(requestInB.getId(), "membership.approved")).isEmpty();
	}

	@Test
	void approveOnAnAlreadyActiveRequestIs409() {
		AuthenticatedUser owner = registerOwner("approval-approve-active@acme.test");
		UUID viewerRoleId = roleIdNamed(owner, "Viewer");
		Membership request = createRequest(owner, "joiner@approval-approve-active.test", "Joe Joiner",
				MembershipStatus.ACTIVE);

		assertStatus(() -> membershipApprovalService.approve(owner, request.getId(), viewerRoleId),
				HttpStatus.CONFLICT);
	}

	@Test
	void approveRollsBackWhenTheRoleGrantIsRejected() {
		AuthenticatedUser owner = registerOwner("approval-approve-rollback@acme.test");
		AuthenticatedUser admin = administrator(owner, "admin@approval-approve-rollback.test");
		UUID ownerRoleId = roleIdNamed(owner, "Owner");
		Membership request = createRequest(owner, "joiner@approval-approve-rollback.test", "Joe Joiner",
				MembershipStatus.PENDING);

		assertStatus(() -> membershipApprovalService.approve(admin, request.getId(), ownerRoleId),
				HttpStatus.FORBIDDEN);

		assertThat(membershipRepository.findById(request.getId())).hasValueSatisfying(
				membership -> assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING));
		assertThat(userRoleRepository.existsByMembershipIdAndRoleId(request.getId(), ownerRoleId)).isFalse();
		assertThat(auditRows(request.getId(), "membership.approved")).isEmpty();
	}

	@Test
	void approvingAJoinerWithAnActiveMembershipInAnotherTenantLeavesTwoActiveMemberships() {
		AuthenticatedUser ownerA = registerOwner("approval-two-tenant-a@acme.test");
		AuthenticatedUser ownerB = registerOwner("approval-two-tenant-b@acme.test");
		UUID viewerRoleA = roleIdNamed(ownerA, "Viewer");

		User joiner = createUser("joiner@approval-two-tenant.test", "Joe Joiner");
		addMembership(joiner.getId(), ownerB.tenantId(), ownerB.organizationId(), MembershipStatus.ACTIVE);
		Membership pendingInA = addMembership(joiner.getId(), ownerA.tenantId(), ownerA.organizationId(),
				MembershipStatus.PENDING);

		membershipApprovalService.approve(ownerA, pendingInA.getId(), viewerRoleA);

		List<Membership> memberships = membershipRepository.findByUserId(joiner.getId());
		assertThat(memberships).hasSize(2)
				.allSatisfy(membership -> assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE));
		assertThat(memberships).extracting(Membership::getTenantId)
				.containsExactlyInAnyOrder(ownerA.tenantId(), ownerB.tenantId());
	}

	@Test
	void rejectDeletesThePendingRowAndWritesOneAuditRow() {
		AuthenticatedUser owner = registerOwner("approval-reject-ok@acme.test");
		Membership request = createRequest(owner, "joiner@approval-reject-ok.test", "Joe Joiner",
				MembershipStatus.PENDING);

		membershipApprovalService.reject(owner, request.getId());

		assertThat(membershipRepository.findById(request.getId())).isEmpty();
		assertThat(auditRows(request.getId(), "membership.rejected")).singleElement().satisfies(log -> {
			assertThat(log.getEntityType()).isEqualTo("Membership");
			assertThat(log.getTenantId()).isEqualTo(owner.tenantId());
			assertThat(log.getOrganizationId()).isEqualTo(owner.organizationId());
			assertThat(log.getUserId()).isEqualTo(owner.userId());
			assertThat(log.getBeforeValue()).contains("PENDING");
			assertThat(log.getAfterValue()).isNull();
		});
	}

	@Test
	void rejectOnAnUnknownMembershipIdIs404() {
		AuthenticatedUser owner = registerOwner("approval-reject-unknown@acme.test");

		assertStatus(() -> membershipApprovalService.reject(owner, UUID.randomUUID()), HttpStatus.NOT_FOUND);
	}

	@Test
	void rejectOnAPendingRequestInAnotherOrgIs404() {
		AuthenticatedUser ownerA = registerOwner("approval-reject-crossorg-a@acme.test");
		AuthenticatedUser ownerB = registerOwner("approval-reject-crossorg-b@acme.test");
		Membership requestInB = createRequest(ownerB, "joiner@approval-reject-crossorg.test", "Joe Joiner",
				MembershipStatus.PENDING);

		assertStatus(() -> membershipApprovalService.reject(ownerA, requestInB.getId()), HttpStatus.NOT_FOUND);

		assertThat(membershipRepository.findById(requestInB.getId())).isPresent();
	}

	@Test
	void rejectOnAnAlreadyActiveRequestIs409() {
		AuthenticatedUser owner = registerOwner("approval-reject-active@acme.test");
		Membership request = createRequest(owner, "joiner@approval-reject-active.test", "Joe Joiner",
				MembershipStatus.ACTIVE);

		assertStatus(() -> membershipApprovalService.reject(owner, request.getId()), HttpStatus.CONFLICT);
	}

	@Test
	void rejectThenAFreshVerifyEmailPutsThePersonBackInTheQueue() {
		AuthenticatedUser owner = registerOwner("approval-reject-rerequest@acme.test");
		String inviteCode = organizationRepository.findById(owner.organizationId()).orElseThrow().getInviteCode();
		String email = "rejoin@approval-reject-rerequest.test";

		selfJoinService.verifyEmail(emailVerificationTokenService.issue(
				new JoinIntent(email, passwordEncoder.encode(PASSWORD), "Re Joiner", inviteCode)));
		Membership firstRequest = membershipRepository.findByOrganizationIdAndStatus(
				owner.organizationId(), MembershipStatus.PENDING).getFirst();

		membershipApprovalService.reject(owner, firstRequest.getId());
		assertThat(membershipRepository.findById(firstRequest.getId())).isEmpty();

		selfJoinService.verifyEmail(emailVerificationTokenService.issue(
				new JoinIntent(email, null, null, inviteCode)));

		assertThat(membershipApprovalService.listPending(owner))
				.extracting(PendingJoinRequestResponse::email).containsExactly(email);
	}

}
