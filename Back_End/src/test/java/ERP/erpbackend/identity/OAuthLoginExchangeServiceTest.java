package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import java.util.List;
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

	private static LoginResponse authenticatedResponse() {
		return LoginResponse.authenticated(new TokenResponse("access-token", "refresh-token", 900, 2_592_000,
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", "Ada Owner"));
	}

	private static LoginResponse selectionResponse() {
		return LoginResponse.selectOrganization("selection-token", List.of(
				new MembershipOption(UUID.randomUUID(), UUID.randomUUID(), "Head Office"),
				new MembershipOption(UUID.randomUUID(), UUID.randomUUID(), "Warehouse")));
	}

	@Test
	void issuingThenConsumingReturnsTheIssuedAuthenticatedResponse() {
		LoginResponse loginResponse = authenticatedResponse();

		String code = oAuthLoginExchangeService.issue(loginResponse);

		assertThat(oAuthLoginExchangeService.consume(code)).contains(loginResponse);
	}

	@Test
	void issuingThenConsumingRoundTripsASelectOrganizationResponse() {
		LoginResponse loginResponse = selectionResponse();

		String code = oAuthLoginExchangeService.issue(loginResponse);

		assertThat(oAuthLoginExchangeService.consume(code)).contains(loginResponse);
	}

	@Test
	void consumingTheSameCodeTwiceReturnsEmptyTheSecondTime() {
		String code = oAuthLoginExchangeService.issue(authenticatedResponse());
		oAuthLoginExchangeService.consume(code);

		assertThat(oAuthLoginExchangeService.consume(code)).isEmpty();
	}

	@Test
	void consumingAnUnknownCodeReturnsEmpty() {
		assertThat(oAuthLoginExchangeService.consume("does-not-exist")).isEmpty();
	}

}
