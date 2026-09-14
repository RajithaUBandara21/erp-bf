package ERP.erpbackend.identity;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class SuperAdminOtpVerifyRateLimiter {

	private static final String KEY_PREFIX = "ratelimit:superadmin-otp-verify:";
	private static final int MAX_ATTEMPTS = 5;
	private static final Duration WINDOW = Duration.ofMinutes(10);

	private final IpRateLimiter limiter;

	SuperAdminOtpVerifyRateLimiter(StringRedisTemplate redisTemplate) {
		this.limiter = new IpRateLimiter(redisTemplate, KEY_PREFIX, MAX_ATTEMPTS, WINDOW);
	}

	boolean allow(String clientIp) {
		return limiter.allow(clientIp);
	}

}
