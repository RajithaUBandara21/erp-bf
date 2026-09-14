package ERP.erpbackend.identity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
	private static final String CLAIM_PLATFORM_SUPER_ADMIN = "platformSuperAdmin";
	private static final String CLAIM_MUST_CHANGE_PASSWORD = "mustChangePassword";

	private final JwtProperties jwtProperties;

	public String issueAccessToken(AuthenticatedUser user) {
		Instant now = Instant.now();
		JwtBuilder builder = Jwts.builder()
				.subject(user.userId().toString())
				.claim(CLAIM_EMAIL, user.email())
				.claim(CLAIM_PLATFORM_SUPER_ADMIN, user.platformSuperAdmin())
				.claim(CLAIM_MUST_CHANGE_PASSWORD, user.mustChangePassword())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(ttlFor(user))));
		putIfPresent(builder, CLAIM_TENANT_ID, user.tenantId());
		putIfPresent(builder, CLAIM_ORGANIZATION_ID, user.organizationId());
		putIfPresent(builder, CLAIM_SESSION_ID, user.sessionId());
		putIfPresent(builder, CLAIM_MEMBERSHIP_ID, user.membershipId());
		return builder.signWith(signingKey()).compact();
	}

	public Optional<AuthenticatedUser> parseAccessToken(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			String membershipId = claims.get(CLAIM_MEMBERSHIP_ID, String.class);
			boolean platformSuperAdmin = Boolean.TRUE.equals(claims.get(CLAIM_PLATFORM_SUPER_ADMIN, Boolean.class));
			if (membershipId == null && !platformSuperAdmin) {
				// Neither a Membership-scoped token nor a Super Admin one - e.g. a token issued
				// before the Membership cutover (feature 5a.2). Reject it so the caller
				// re-authenticates and picks up a token this contract recognizes.
				return Optional.empty();
			}
			boolean mustChangePassword = Boolean.TRUE.equals(claims.get(CLAIM_MUST_CHANGE_PASSWORD, Boolean.class));
			return Optional.of(new AuthenticatedUser(
					UUID.fromString(claims.getSubject()),
					uuidOrNull(claims, CLAIM_TENANT_ID),
					uuidOrNull(claims, CLAIM_ORGANIZATION_ID),
					claims.get(CLAIM_EMAIL, String.class),
					uuidOrNull(claims, CLAIM_SESSION_ID),
					membershipId == null ? null : UUID.fromString(membershipId),
					platformSuperAdmin,
					mustChangePassword));
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	public long accessTokenTtlSeconds() {
		return jwtProperties.accessTokenTtl().getSeconds();
	}

	public long superAdminAccessTokenTtlSeconds() {
		return jwtProperties.superAdminAccessTokenTtl().getSeconds();
	}

	private Duration ttlFor(AuthenticatedUser user) {
		return user.platformSuperAdmin() ? jwtProperties.superAdminAccessTokenTtl() : jwtProperties.accessTokenTtl();
	}

	private static void putIfPresent(JwtBuilder builder, String claimName, UUID value) {
		if (value != null) {
			builder.claim(claimName, value.toString());
		}
	}

	private static UUID uuidOrNull(Claims claims, String claimName) {
		String value = claims.get(claimName, String.class);
		return value == null ? null : UUID.fromString(value);
	}

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

}
