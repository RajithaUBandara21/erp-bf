package ERP.erpbackend.identity;

import ERP.erpbackend.organization.OrganizationService;
import java.time.Instant;
import java.util.Locale;
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
	// and response time can't be used to enumerate valid org codes or emails (F-09).
	private static final String DUMMY_PASSWORD_HASH =
			"$2a$10$rj1PzUggzShtDluMqkrXZ.JRijLCAjBsdaPs5nbO6M66EzAE2gyS2";

	private final OrganizationService organizationService;
	private final UserRepository userRepository;
	private final SessionRepository sessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProperties jwtProperties;
	private final RefreshTokenService refreshTokenService;
	private final SessionTokenIssuer sessionTokenIssuer;
	private final RevokedSessionRegistry revokedSessionRegistry;

	public TokenResponse login(LoginRequest request) {
		Optional<User> user = organizationService.findTenantIdByCode(normalize(request.organizationCode()))
				.flatMap(tenantId ->
						userRepository.findByTenantIdAndEmail(tenantId, request.email().toLowerCase(Locale.ROOT)));

		// Run one hash comparison on every path, matched or not, falling back to a fixed dummy hash
		// when no user was found - mirrors Spring's DaoAuthenticationProvider and closes the timing
		// oracle (F-09). Failure stays one generic message regardless of which check failed.
		boolean passwordMatches = passwordEncoder.matches(
				request.password(), user.map(User::getPasswordHash).orElse(DUMMY_PASSWORD_HASH));

		if (user.isEmpty() || !user.get().isActive() || !passwordMatches) {
			throw invalidCredentials();
		}

		Session session = sessionTokenIssuer.createSession(user.get(), request.clientType());
		return sessionTokenIssuer.issueTokens(user.get(), session);
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

		Instant now = Instant.now();
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(jwtProperties.refreshTokenTtl()));
		sessionRepository.save(session);

		return sessionTokenIssuer.issueTokens(user, session);
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

	private static boolean isUsable(Session session) {
		return session.getRevokedAt() == null && session.getExpiresAt().isAfter(Instant.now());
	}

	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static ResponseStatusException invalidCredentials() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
	}

}
