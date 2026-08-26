package ERP.erpbackend.identity;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionTokenIssuer {

	private final SessionRepository sessionRepository;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;
	private final RefreshTokenService refreshTokenService;

	public Session createSession(User user, ClientType clientType) {
		Instant now = Instant.now();
		Session session = new Session();
		session.setTenantId(user.getTenantId());
		session.setUserId(user.getId());
		session.setClientType(clientType);
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(jwtProperties.refreshTokenTtl()));
		return sessionRepository.save(session);
	}

	public TokenResponse issueTokens(User user, Session session) {
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(
				user.getId(), user.getTenantId(), user.getOrganizationId(), user.getEmail(), session.getId());
		String accessToken = jwtService.issueAccessToken(authenticatedUser);
		String refreshToken = refreshTokenService.issue(session.getId());
		return new TokenResponse(accessToken, refreshToken, jwtService.accessTokenTtlSeconds(),
				jwtProperties.refreshTokenTtl().getSeconds(),
				user.getId(), user.getTenantId(), user.getOrganizationId(), user.getEmail(), user.getFullName());
	}

}
