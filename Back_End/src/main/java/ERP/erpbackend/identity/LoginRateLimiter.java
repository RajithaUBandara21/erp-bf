package ERP.erpbackend.identity;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class LoginRateLimiter {

	private static final String KEY_PREFIX = "ratelimit:login:";
	private static final int MAX_ATTEMPTS = 5;
	private static final Duration WINDOW = Duration.ofMinutes(10);

	private final IpRateLimiter limiter;

	LoginRateLimiter(StringRedisTemplate redisTemplate) {
		this.limiter = new IpRateLimiter(redisTemplate, KEY_PREFIX, MAX_ATTEMPTS, WINDOW);
	}

	boolean allow(String clientIp) {
		return limiter.allow(clientIp);
	}

}
