package ERP.erpbackend.identity;

import ERP.erpbackend.organization.OrganizationService;
import java.time.Instant;
import java.util.Locale;
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

	private final OrganizationService organizationService;
	private final UserRepository userRepository;
	private final SessionRepository sessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;
	private final RefreshTokenService refreshTokenService;

	public TokenResponse login(LoginRequest request) {
		UUID tenantId = organizationService.findTenantIdByCode(normalize(request.organizationCode()))
				.orElseThrow(AuthenticationService::invalidCredentials);

		User user = userRepository.findByTenantIdAndEmail(tenantId, request.email().toLowerCase(Locale.ROOT))
				.orElseThrow(AuthenticationService::invalidCredentials);

		if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidCredentials();
		}

		Session session = createSession(user, request.clientType());
		return issueTokens(user, session);
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

		return issueTokens(user, session);
	}

	public void logout(RefreshRequest request) {
		refreshTokenService.consume(request.refreshToken())
				.flatMap(sessionRepository::findById)
				.ifPresent(session -> {
					session.setRevokedAt(Instant.now());
					sessionRepository.save(session);
				});
	}

	private Session createSession(User user, ClientType clientType) {
		Instant now = Instant.now();
		Session session = new Session();
		session.setTenantId(user.getTenantId());
		session.setUserId(user.getId());
		session.setClientType(clientType);
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(jwtProperties.refreshTokenTtl()));
		return sessionRepository.save(session);
	}

	private TokenResponse issueTokens(User user, Session session) {
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(
				user.getId(), user.getTenantId(), user.getOrganizationId(), user.getEmail(), session.getId());
		String accessToken = jwtService.issueAccessToken(authenticatedUser);
		String refreshToken = refreshTokenService.issue(session.getId());
		return new TokenResponse(accessToken, refreshToken, jwtService.accessTokenTtlSeconds(),
				user.getId(), user.getTenantId(), user.getOrganizationId(), user.getEmail(), user.getFullName());
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
