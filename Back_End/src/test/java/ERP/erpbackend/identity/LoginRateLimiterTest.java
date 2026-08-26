package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// Also covers RegistrationRateLimiter: both are identically-shaped delegates
// to IpRateLimiter, differing only in Redis key prefix (see F-03).
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LoginRateLimiterTest {

	@Autowired
	private LoginRateLimiter rateLimiter;

	@Test
	void deniesRequestsOnceLimitIsExceededWithinTheWindow() {
		String clientId = UUID.randomUUID().toString();

		for (int i = 0; i < 5; i++) {
			assertThat(rateLimiter.allow(clientId)).isTrue();
		}

		assertThat(rateLimiter.allow(clientId)).isFalse();
	}

	@Test
	void tracksLimitsIndependentlyPerClient() {
		String clientA = UUID.randomUUID().toString();
		String clientB = UUID.randomUUID().toString();

		for (int i = 0; i < 5; i++) {
			assertThat(rateLimiter.allow(clientA)).isTrue();
		}

		assertThat(rateLimiter.allow(clientB)).isTrue();
	}

}
