package ERP.erpbackend.identity;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class LoginRateLimiter {

	private static final String KEY_PREFIX = "ratelimit:login:";
	private static final int MAX_ATTEMPTS = 5;
	private static final Duration WINDOW = Duration.ofMinutes(10);

	private final StringRedisTemplate redisTemplate;

	/**
	 * Fixed-window limit keyed by client IP. Uses the direct connecting
	 * address, not X-Forwarded-For - no trusted reverse-proxy chain is
	 * configured yet (see project-overview.md's open deployment questions).
	 * Fails open if Redis is unreachable: a rate limit is a safety net, not
	 * the system of record, so an outage here must not block login.
	 */
	boolean allow(String clientIp) {
		String key = KEY_PREFIX + clientIp;
		try {
			Long count = redisTemplate.opsForValue().increment(key);
			if (count != null && count == 1L) {
				redisTemplate.expire(key, WINDOW);
			}
			return count == null || count <= MAX_ATTEMPTS;
		} catch (DataAccessException ex) {
			log.warn("Rate limiter unavailable, allowing request", ex);
			return true;
		}
	}

}
