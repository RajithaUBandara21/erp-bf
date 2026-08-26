package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private static final String SECRET = "test-signing-secret-at-least-32-bytes-long-0123456789";
	private static final AuthenticatedUser USER = new AuthenticatedUser(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", UUID.randomUUID());

	private static JwtService serviceWithAccessTtl(Duration accessTokenTtl) {
		return new JwtService(new JwtProperties(SECRET, accessTokenTtl, Duration.ofDays(30)));
	}

	@Test
	void issuesAndParsesAccessTokenWithAllClaimsRoundTripping() {
		JwtService jwtService = serviceWithAccessTtl(Duration.ofMinutes(15));

		String token = jwtService.issueAccessToken(USER);

		assertThat(jwtService.parseAccessToken(token)).contains(USER);
	}

	@Test
	void rejectsExpiredToken() {
		JwtService jwtService = serviceWithAccessTtl(Duration.ofMillis(-1000));

		String expiredToken = jwtService.issueAccessToken(USER);

		assertThat(jwtService.parseAccessToken(expiredToken)).isEmpty();
	}

	@Test
	void rejectsSignatureTamperedToken() {
		JwtService jwtService = serviceWithAccessTtl(Duration.ofMinutes(15));
		String token = jwtService.issueAccessToken(USER);
		String[] parts = token.split("\\.");
		String tamperedSignature = parts[2].startsWith("A") ? "B" + parts[2].substring(1) : "A" + parts[2].substring(1);
		String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;

		assertThat(jwtService.parseAccessToken(tamperedToken)).isEmpty();
	}

	@Test
	void accessTokenTtlSecondsReflectsConfiguredDuration() {
		JwtService jwtService = serviceWithAccessTtl(Duration.ofMinutes(15));

		assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(900L);
	}

}
