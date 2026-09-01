package ERP.erpbackend.identity;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Per-IP fixed-window guard shared by {@code POST /api/auth/join} and {@code POST /api/auth/verify-email}. Mirrors {@link RegistrationRateLimiter}. */
@Component
class SelfJoinRateLimiter {

	private static final String KEY_PREFIX = "ratelimit:selfjoin:";
	private static final int MAX_ATTEMPTS = 5;
	private static final Duration WINDOW = Duration.ofMinutes(10);

	private final IpRateLimiter limiter;

	SelfJoinRateLimiter(StringRedisTemplate redisTemplate) {
		this.limiter = new IpRateLimiter(redisTemplate, KEY_PREFIX, MAX_ATTEMPTS, WINDOW);
	}

	boolean allow(String clientIp) {
		return limiter.allow(clientIp);
	}

}
