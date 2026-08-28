package ERP.erpbackend.identity;

import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Fixed-window limit keyed by client IP. Uses the direct connecting
 * address, not X-Forwarded-For - no trusted reverse-proxy chain is
 * configured yet (see project-overview.md's open deployment questions).
 * Fails open if Redis is unreachable: a rate limit is a safety net, not
 * the system of record, so an outage here must not block the guarded action.
 */
@Slf4j
class IpRateLimiter {

	// One round trip: INCR, then PEXPIRE only on the first hit. Atomic, so a failure can never leave
	// the counter key without a TTL (which would permanently lock an IP out - F-16).
	private static final DefaultRedisScript<Long> INCREMENT_AND_EXPIRE = new DefaultRedisScript<>(
			"""
			local count = redis.call('INCR', KEYS[1])
			if count == 1 then
			  redis.call('PEXPIRE', KEYS[1], ARGV[1])
			end
			return count
			""",
			Long.class);

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
			Long count = redisTemplate.execute(
					INCREMENT_AND_EXPIRE, List.of(key), Long.toString(window.toMillis()));
			return count == null || count <= maxAttempts;
		} catch (DataAccessException ex) {
			log.warn("Rate limiter unavailable, allowing request", ex);
			return true;
		}
	}

}
