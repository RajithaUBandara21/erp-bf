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
class OAuthLoginExchangeServiceTest {

	@Autowired
	private OAuthLoginExchangeService oAuthLoginExchangeService;

	private static TokenResponse testTokenResponse() {
		return new TokenResponse("access-token", "refresh-token", 900, 2_592_000,
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", "Ada Owner");
	}

	@Test
	void issuingThenConsumingReturnsTheIssuedTokenResponse() {
		TokenResponse tokenResponse = testTokenResponse();

		String code = oAuthLoginExchangeService.issue(tokenResponse);

		assertThat(oAuthLoginExchangeService.consume(code)).contains(tokenResponse);
	}

	@Test
	void consumingTheSameCodeTwiceReturnsEmptyTheSecondTime() {
		String code = oAuthLoginExchangeService.issue(testTokenResponse());
		oAuthLoginExchangeService.consume(code);

		assertThat(oAuthLoginExchangeService.consume(code)).isEmpty();
	}

	@Test
	void consumingAnUnknownCodeReturnsEmpty() {
		assertThat(oAuthLoginExchangeService.consume("does-not-exist")).isEmpty();
	}

}
