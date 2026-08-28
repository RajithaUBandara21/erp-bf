package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtSecretGuardTest {

	private static final Duration TTL = Duration.ofMinutes(15);
	private static final String STRONG_SECRET = "a-real-production-jwt-signing-secret-value-0123456789";

	private JwtSecretGuard guard(String secret, String... activeProfiles) {
		MockEnvironment environment = new MockEnvironment();
		if (activeProfiles.length > 0) {
			environment.setActiveProfiles(activeProfiles);
		}
		return new JwtSecretGuard(new JwtProperties(secret, TTL, TTL), environment);
	}

	@Test
	void rejectsTheCommittedDevDefaultSecretOutsideDevAndTest() {
		assertThatIllegalStateException()
				.isThrownBy(() -> guard(JwtSecretGuard.DEV_DEFAULT_SECRET, "prod").afterPropertiesSet());
	}

	@Test
	void rejectsABlankSecretOutsideDevAndTest() {
		assertThatIllegalStateException()
				.isThrownBy(() -> guard("   ", "prod").afterPropertiesSet());
	}

	@Test
	void rejectsATooShortSecretOutsideDevAndTest() {
		assertThatIllegalStateException()
				.isThrownBy(() -> guard("too-short-secret", "prod").afterPropertiesSet());
	}

	@Test
	void acceptsAStrongSecretOutsideDevAndTest() {
		assertThatCode(() -> guard(STRONG_SECRET, "prod").afterPropertiesSet())
				.doesNotThrowAnyException();
	}

	@Test
	void skipsValidationUnderTheDevProfile() {
		assertThatCode(() -> guard(JwtSecretGuard.DEV_DEFAULT_SECRET, "dev").afterPropertiesSet())
				.doesNotThrowAnyException();
	}

	@Test
	void skipsValidationUnderTheTestProfile() {
		assertThatCode(() -> guard(JwtSecretGuard.DEV_DEFAULT_SECRET, "test").afterPropertiesSet())
				.doesNotThrowAnyException();
	}
}
