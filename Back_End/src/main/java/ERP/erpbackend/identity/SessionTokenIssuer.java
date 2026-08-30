package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionTokenIssuer {

	private final SessionRepository sessionRepository;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;
	private final RefreshTokenService refreshTokenService;
	private final AuditService auditService;

	public Session createSession(User user, Membership membership, ClientType clientType) {
		Instant now = Instant.now();
		Session session = new Session();
		session.setTenantId(membership.getTenantId());
		session.setUserId(user.getId());
		session.setMembershipId(membership.getId());
		session.setClientType(clientType);
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(jwtProperties.refreshTokenTtl()));
		session = sessionRepository.save(session);

		auditService.log(new AuditEvent(membership.getTenantId(), membership.getOrganizationId(), user.getId(),
				"Session", session.getId(), "auth.login", null, Map.of("clientType", clientType)));

		return session;
	}

	public TokenResponse issueTokens(User user, Membership membership, Session session) {
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(
				user.getId(), membership.getTenantId(), membership.getOrganizationId(), user.getEmail(),
				session.getId(), membership.getId());
		String accessToken = jwtService.issueAccessToken(authenticatedUser);
		String refreshToken = refreshTokenService.issue(session.getId());
		return new TokenResponse(accessToken, refreshToken, jwtService.accessTokenTtlSeconds(),
				jwtProperties.refreshTokenTtl().getSeconds(),
				user.getId(), membership.getTenantId(), membership.getOrganizationId(), user.getEmail(),
				user.getFullName());
	}

}
