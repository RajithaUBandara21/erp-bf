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
class OAuthStateServiceTest {

	@Autowired
	private OAuthStateService oAuthStateService;

	@Test
	void issuingThenConsumingForALoginFlowReturnsEmptyString() {
		String state = oAuthStateService.issue(null);

		assertThat(oAuthStateService.consume(state)).contains("");
	}

	@Test
	void issuingThenConsumingForALinkFlowReturnsTheLinkedUserId() {
		UUID userId = UUID.randomUUID();
		String state = oAuthStateService.issue(userId);

		assertThat(oAuthStateService.consume(state)).contains(userId.toString());
	}

	@Test
	void consumingTheSameStateTwiceReturnsEmptyTheSecondTime() {
		String state = oAuthStateService.issue(null);
		oAuthStateService.consume(state);

		assertThat(oAuthStateService.consume(state)).isEmpty();
	}

}
