package ERP.erpbackend.identity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

	private static final String CLAIM_TENANT_ID = "tenantId";
	private static final String CLAIM_ORGANIZATION_ID = "organizationId";
	private static final String CLAIM_EMAIL = "email";
	private static final String CLAIM_SESSION_ID = "sessionId";
	private static final String CLAIM_MEMBERSHIP_ID = "membershipId";

	private final JwtProperties jwtProperties;

	public String issueAccessToken(AuthenticatedUser user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.userId().toString())
				.claim(CLAIM_TENANT_ID, user.tenantId().toString())
				.claim(CLAIM_ORGANIZATION_ID, user.organizationId().toString())
				.claim(CLAIM_EMAIL, user.email())
				.claim(CLAIM_SESSION_ID, user.sessionId().toString())
				.claim(CLAIM_MEMBERSHIP_ID, user.membershipId().toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(jwtProperties.accessTokenTtl())))
				.signWith(signingKey())
				.compact();
	}

	public Optional<AuthenticatedUser> parseAccessToken(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			String membershipId = claims.get(CLAIM_MEMBERSHIP_ID, String.class);
			if (membershipId == null) {
				// A token issued before the Membership cutover (feature 5a.2). Reject it so the
				// caller re-authenticates and picks up a Membership-scoped token.
				return Optional.empty();
			}
			return Optional.of(new AuthenticatedUser(
					UUID.fromString(claims.getSubject()),
					UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class)),
					UUID.fromString(claims.get(CLAIM_ORGANIZATION_ID, String.class)),
					claims.get(CLAIM_EMAIL, String.class),
					UUID.fromString(claims.get(CLAIM_SESSION_ID, String.class)),
					UUID.fromString(membershipId)));
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	public long accessTokenTtlSeconds() {
		return jwtProperties.accessTokenTtl().getSeconds();
	}

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

}
