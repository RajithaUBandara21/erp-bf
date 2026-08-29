package ERP.erpbackend.identity;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fast-path enforcement for revoked sessions. Postgres stays the system of
 * record (every revoke sets {@code Session.revokedAt}); this only lets the
 * stateless auth filter reject a still-valid access token before its ~15 min
 * TTL runs out (F-11). Entries self-expire after the access-token TTL, by which
 * point the token is dead anyway. Fails open if Redis is unreachable - same
 * philosophy as {@link IpRateLimiter}: it degrades to "revoke blocks refresh
 * immediately, access lingers <= TTL", which is exactly the pre-fix behaviour.
 */
// Public only so a @WebMvcTest in another module's package (e.g. AuditLogControllerTest) can
// declare a @MockitoBean of this type to satisfy SecurityConfig's constructor when it imports
// SecurityConfig for its own authorization slice test; methods stay package-private.
@Slf4j
@Component
@RequiredArgsConstructor
public class RevokedSessionRegistry {

	private static final String KEY_PREFIX = "revoked-session:";

	private final StringRedisTemplate redisTemplate;
	private final JwtProperties jwtProperties;

	void revoke(UUID sessionId) {
		try {
			redisTemplate.opsForValue().set(keyFor(sessionId), "1", jwtProperties.accessTokenTtl());
		} catch (DataAccessException ex) {
			log.warn("Could not record revoked session {} in Redis; access token blocked only after its TTL",
					sessionId, ex);
		}
	}

	boolean isRevoked(UUID sessionId) {
		try {
			return Boolean.TRUE.equals(redisTemplate.hasKey(keyFor(sessionId)));
		} catch (DataAccessException ex) {
			log.warn("Revoked-session check unavailable, allowing request for session {}", sessionId, ex);
			return false;
		}
	}

	private static String keyFor(UUID sessionId) {
		return KEY_PREFIX + sessionId;
	}

}
