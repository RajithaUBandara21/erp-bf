package ERP.erpbackend.identity;

import ERP.erpbackend.organization.OrganizationService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private static final String INVALID_CREDENTIALS = "Invalid credentials";

	// A valid BCrypt hash (strength 10, matching BCryptPasswordEncoder's default) of a throwaway
	// value. Compared against when no user matches so every login path runs one full hash comparison
	// and response time can't be used to enumerate valid emails (F-09).
	private static final String DUMMY_PASSWORD_HASH =
			"$2a$10$rj1PzUggzShtDluMqkrXZ.JRijLCAjBsdaPs5nbO6M66EzAE2gyS2";

	private final OrganizationService organizationService;
	private final UserRepository userRepository;
	private final MembershipRepository membershipRepository;
	private final SessionRepository sessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProperties jwtProperties;
	private final RefreshTokenService refreshTokenService;
	private final SessionTokenIssuer sessionTokenIssuer;
	private final RevokedSessionRegistry revokedSessionRegistry;
	private final LoginSelectionService loginSelectionService;

	public LoginResponse login(LoginRequest request) {
		Optional<User> user = userRepository.findByEmail(request.email().toLowerCase(Locale.ROOT));

		// Run one hash comparison on every path, matched or not, falling back to a fixed dummy hash
		// when no user was found - mirrors Spring's DaoAuthenticationProvider and closes the timing
		// oracle (F-09). Failure stays one generic message regardless of which check failed.
		boolean passwordMatches = passwordEncoder.matches(
				request.password(), user.map(User::getPasswordHash).orElse(DUMMY_PASSWORD_HASH));

		if (user.isEmpty() || !user.get().isActive() || !passwordMatches) {
			throw invalidCredentials();
		}

		// A person with no ACTIVE Membership (all PENDING, or none) is turned away with the same
		// generic message as a bad password - login must not leak that the account exists.
		List<Membership> memberships = membershipRepository.findByUserIdAndStatus(
				user.get().getId(), MembershipStatus.ACTIVE);
		if (memberships.isEmpty()) {
			throw invalidCredentials();
		}
		if (memberships.size() == 1) {
			return LoginResponse.authenticated(
					issueForMembership(user.get(), memberships.getFirst(), request.clientType()));
		}

		String selectionToken = loginSelectionService.issue(user.get().getId());
		return LoginResponse.selectOrganization(selectionToken, toOptions(memberships));
	}

	public TokenResponse selectOrganization(LoginSelectRequest request) {
		UUID userId = loginSelectionService.consume(request.selectionToken())
				.orElseThrow(AuthenticationService::invalidCredentials);

		User user = userRepository.findById(userId)
				.filter(User::isActive)
				.orElseThrow(AuthenticationService::invalidCredentials);

		// Re-resolve ACTIVE Memberships now: the token carries only the userId, so a Membership that
		// went PENDING or was removed since login must not still be selectable.
		Membership membership = membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
				.filter(candidate -> candidate.getId().equals(request.membershipId()))
				.findFirst()
				.orElseThrow(AuthenticationService::invalidCredentials);

		return issueForMembership(user, membership, request.clientType());
	}

	public TokenResponse refresh(RefreshRequest request) {
		UUID sessionId = refreshTokenService.consume(request.refreshToken())
				.orElseThrow(AuthenticationService::invalidCredentials);

		Session session = sessionRepository.findById(sessionId)
				.filter(AuthenticationService::isUsable)
				.orElseThrow(AuthenticationService::invalidCredentials);

		User user = userRepository.findById(session.getUserId())
				.filter(User::isActive)
				.orElseThrow(AuthenticationService::invalidCredentials);

		Membership membership = membershipRepository.findById(session.getMembershipId())
				.orElseThrow(AuthenticationService::invalidCredentials);

		Instant now = Instant.now();
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(jwtProperties.refreshTokenTtl()));
		sessionRepository.save(session);

		return sessionTokenIssuer.issueTokens(user, membership, session);
	}

	public void logout(RefreshRequest request) {
		refreshTokenService.consume(request.refreshToken())
				.flatMap(sessionRepository::findById)
				.ifPresent(session -> {
					session.setRevokedAt(Instant.now());
					sessionRepository.save(session);
					revokedSessionRegistry.revoke(session.getId());
				});
	}

	private TokenResponse issueForMembership(User user, Membership membership, ClientType clientType) {
		Session session = sessionTokenIssuer.createSession(user, membership, clientType);
		return sessionTokenIssuer.issueTokens(user, membership, session);
	}

	private List<MembershipOption> toOptions(List<Membership> memberships) {
		Map<UUID, String> names = organizationService.findNamesByIds(
				memberships.stream().map(Membership::getOrganizationId).toList());
		return memberships.stream()
				.map(membership -> new MembershipOption(membership.getId(), membership.getOrganizationId(),
						names.getOrDefault(membership.getOrganizationId(), "")))
				.sorted(Comparator.comparing(MembershipOption::organizationName, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	private static boolean isUsable(Session session) {
		return session.getRevokedAt() == null && session.getExpiresAt().isAfter(Instant.now());
	}

	private static ResponseStatusException invalidCredentials() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
	}

}
