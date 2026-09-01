package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import ERP.erpbackend.organization.OrganizationInviteTarget;
import ERP.erpbackend.organization.OrganizationService;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Employee self-join via an Organization invite code, gated on email verification. {@code requestJoin}
 * stashes the intent under a single-use Redis token and dispatches a verification link; nothing
 * touches Postgres until the link is followed. Lives in {@code identity} (not {@code organization}) so
 * the {@code identity -> organization} module dependency stays one-directional.
 */
@Service
@RequiredArgsConstructor
public class SelfJoinService {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private static final String ACCEPTED_MESSAGE =
			"If those details are valid, check your email for a link to finish your request.";

	private final OrganizationService organizationService;
	private final UserRepository userRepository;
	private final MembershipRepository membershipRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationTokenService emailVerificationTokenService;
	private final JoinVerificationMailer joinVerificationMailer;
	private final AuditService auditService;

	@Value("${FRONTEND_BASE_URL:http://localhost:3000}")
	private String frontendBaseUrl;

	/**
	 * Resolve the invite code, then - running exactly one bcrypt operation on every path - stash a
	 * {@link JoinIntent} and send a verification link when the request could plausibly succeed. The
	 * response is a fixed {@code 202} for every email-related outcome; only a bad or inactive invite
	 * code produces a distinct {@code 404}.
	 */
	public SelfJoinResponse requestJoin(JoinRequest request) {
		String inviteCode = normalizeInviteCode(request.inviteCode());
		OrganizationInviteTarget target = organizationService.findByInviteCode(inviteCode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That invite code is not valid."));

		String email = request.email().toLowerCase(Locale.ROOT);
		Optional<User> existing = userRepository.findByEmail(email);

		if (existing.isEmpty()) {
			// encode() is the single bcrypt call and also produces the hash the new-account intent carries.
			String passwordHash = passwordEncoder.encode(request.password());
			dispatch(new JoinIntent(email, passwordHash, request.fullName(), inviteCode), target);
			return new SelfJoinResponse(ACCEPTED_MESSAGE);
		}

		// matches() is the single bcrypt call. Wrong password, inactive account, or an existing
		// Membership in the target org all fall through silently to the same fixed 202.
		User user = existing.get();
		boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
		if (passwordMatches && user.isActive() && !hasMembership(user.getId(), target.organizationId())) {
			dispatch(new JoinIntent(email, null, null, inviteCode), target);
		}
		return new SelfJoinResponse(ACCEPTED_MESSAGE);
	}

	/**
	 * Consume the token, re-resolve its invite code (a rotation or deactivation during the window kills
	 * the pending join with a {@code 409}), attach to or create the account for that email, and record a
	 * PENDING {@link Membership}. Issues no session - the person cannot sign in until an Org Admin
	 * approves them (5c.3). An expired, missing, or already-used token is a {@code 400}.
	 */
	@Transactional
	public VerifyEmailResponse verifyEmail(String token) {
		JoinIntent intent = emailVerificationTokenService.consume(token)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"This verification link is invalid or has expired."));

		OrganizationInviteTarget target = organizationService.findByInviteCode(intent.inviteCode())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
						"This invite link is no longer valid. Ask for a new one."));

		User user = resolveOrCreateAccount(intent);

		Optional<Membership> existing = membershipRepository.findByUserIdAndOrganizationId(
				user.getId(), target.organizationId());
		if (existing.isPresent()) {
			return new VerifyEmailResponse(alreadyMemberMessage(existing.get().getStatus()), target.organizationName());
		}

		Membership membership = new Membership();
		membership.setUserId(user.getId());
		membership.setTenantId(target.tenantId());
		membership.setOrganizationId(target.organizationId());
		membership.setStatus(MembershipStatus.PENDING);
		membership = membershipRepository.save(membership);

		auditService.log(new AuditEvent(target.tenantId(), target.organizationId(), user.getId(),
				"Membership", membership.getId(), "membership.join_requested", null,
				Map.of("status", "PENDING", "via", "INVITE_CODE")));

		return new VerifyEmailResponse(
				"Email verified. Your request to join " + target.organizationName() + " is now awaiting approval.",
				target.organizationName());
	}

	private boolean hasMembership(UUID userId, UUID organizationId) {
		return membershipRepository.findByUserIdAndOrganizationId(userId, organizationId).isPresent();
	}

	/**
	 * The account for {@code intent.email()}, existing or freshly created. An existing account is used
	 * untouched - {@code passwordHash} / {@code fullName} / {@code active} from the intent are ignored.
	 * A genuine concurrent double-verify for a brand-new email loses the {@code users.email} UNIQUE race
	 * and surfaces as a {@code 409}; the person retries into the existing-account path.
	 */
	private User resolveOrCreateAccount(JoinIntent intent) {
		return userRepository.findByEmail(intent.email()).orElseGet(() -> {
			User user = new User();
			user.setEmail(intent.email());
			user.setPasswordHash(intent.passwordHash());
			user.setFullName(intent.fullName());
			user.setActive(true);
			return userRepository.save(user);
		});
	}

	private static String alreadyMemberMessage(MembershipStatus status) {
		return status == MembershipStatus.ACTIVE
				? "You are already a member of this organization."
				: "Your request to join is already awaiting approval.";
	}

	private void dispatch(JoinIntent intent, OrganizationInviteTarget target) {
		String token = emailVerificationTokenService.issue(intent);
		String verificationLink = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/verify-email")
				.queryParam("token", token)
				.build()
				.toUriString();
		joinVerificationMailer.send(intent.email(), target.organizationName(), verificationLink);
	}

	private static String normalizeInviteCode(String raw) {
		return WHITESPACE.matcher(raw).replaceAll("").toUpperCase(Locale.ROOT);
	}

}
