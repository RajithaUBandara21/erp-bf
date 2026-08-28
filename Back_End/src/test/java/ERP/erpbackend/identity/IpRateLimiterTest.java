package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IpRateLimiterTest {

	private static final String KEY_PREFIX = "ratelimit:test:";
	private static final int MAX_ATTEMPTS = 5;
	private static final Duration WINDOW = Duration.ofMinutes(10);

	@Autowired
	private StringRedisTemplate redisTemplate;

	private IpRateLimiter newLimiter() {
		return new IpRateLimiter(redisTemplate, KEY_PREFIX, MAX_ATTEMPTS, WINDOW);
	}

	@Test
	void setsAPositiveTtlOnTheFirstCall() {
		String clientIp = UUID.randomUUID().toString();

		assertThat(newLimiter().allow(clientIp)).isTrue();

		Long ttlSeconds = redisTemplate.getExpire(KEY_PREFIX + clientIp);
		assertThat(ttlSeconds).isNotNull().isPositive();
		assertThat(ttlSeconds).isLessThanOrEqualTo(WINDOW.toSeconds());
	}

	@Test
	void deniesRequestsOnceTheLimitIsExceededWithinTheWindow() {
		String clientIp = UUID.randomUUID().toString();
		IpRateLimiter limiter = newLimiter();

		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			assertThat(limiter.allow(clientIp)).isTrue();
		}

		assertThat(limiter.allow(clientIp)).isFalse();
	}

	@Test
	void tracksLimitsIndependentlyPerClient() {
		IpRateLimiter limiter = newLimiter();
		String clientA = UUID.randomUUID().toString();
		String clientB = UUID.randomUUID().toString();

		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			limiter.allow(clientA);
		}

		assertThat(limiter.allow(clientB)).isTrue();
	}
}
