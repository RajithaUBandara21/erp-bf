package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RefreshTokenServiceTest {

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Test
	void issuingThenConsumingReturnsTheIssuedSessionId() {
		UUID sessionId = UUID.randomUUID();

		String token = refreshTokenService.issue(sessionId);

		assertThat(refreshTokenService.consume(token)).contains(sessionId);
	}

	@Test
	void consumingTheSameTokenTwiceReturnsEmptyTheSecondTime() {
		UUID sessionId = UUID.randomUUID();
		String token = refreshTokenService.issue(sessionId);
		refreshTokenService.consume(token);

		assertThat(refreshTokenService.consume(token)).isEmpty();
	}

}
