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
	void issuingThenConsumingReturnsTheIssuedUserId() {
		UUID userId = UUID.randomUUID();

		String token = refreshTokenService.issue(userId);

		assertThat(refreshTokenService.consume(token)).contains(userId);
	}

	@Test
	void consumingTheSameTokenTwiceReturnsEmptyTheSecondTime() {
		UUID userId = UUID.randomUUID();
		String token = refreshTokenService.issue(userId);
		refreshTokenService.consume(token);

		assertThat(refreshTokenService.consume(token)).isEmpty();
	}

	@Test
	void revokingALiveTokenMakesAFollowingConsumeReturnEmpty() {
		UUID userId = UUID.randomUUID();
		String token = refreshTokenService.issue(userId);

		refreshTokenService.revoke(token);

		assertThat(refreshTokenService.consume(token)).isEmpty();
	}

}
