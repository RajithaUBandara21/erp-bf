package ERP.erpbackend.identity;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Fixed-window limit keyed by client IP. Uses the direct connecting
 * address, not X-Forwarded-For - no trusted reverse-proxy chain is
 * configured yet (see project-overview.md's open deployment questions).
 * Fails open if Redis is unreachable: a rate limit is a safety net, not
 * the system of record, so an outage here must not block the guarded action.
 */
@Slf4j
class IpRateLimiter {

	private final StringRedisTemplate redisTemplate;
	private final String keyPrefix;
	private final int maxAttempts;
	private final Duration window;

	IpRateLimiter(StringRedisTemplate redisTemplate, String keyPrefix, int maxAttempts, Duration window) {
		this.redisTemplate = redisTemplate;
		this.keyPrefix = keyPrefix;
		this.maxAttempts = maxAttempts;
		this.window = window;
	}

	boolean allow(String clientIp) {
		String key = keyPrefix + clientIp;
		try {
			Long count = redisTemplate.opsForValue().increment(key);
			if (count != null && count == 1L) {
				redisTemplate.expire(key, window);
			}
			return count == null || count <= maxAttempts;
		} catch (DataAccessException ex) {
			log.warn("Rate limiter unavailable, allowing request", ex);
			return true;
		}
	}

}
