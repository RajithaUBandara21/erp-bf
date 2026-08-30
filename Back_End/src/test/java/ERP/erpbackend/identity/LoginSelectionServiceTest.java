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
class LoginSelectionServiceTest {

	@Autowired
	private LoginSelectionService loginSelectionService;

	@Test
	void issuingThenConsumingReturnsTheUserId() {
		UUID userId = UUID.randomUUID();
		String token = loginSelectionService.issue(userId);

		assertThat(loginSelectionService.consume(token)).contains(userId);
	}

	@Test
	void consumingTheSameTokenTwiceReturnsEmptyTheSecondTime() {
		String token = loginSelectionService.issue(UUID.randomUUID());
		loginSelectionService.consume(token);

		assertThat(loginSelectionService.consume(token)).isEmpty();
	}

	@Test
	void consumingAnUnknownTokenReturnsEmpty() {
		assertThat(loginSelectionService.consume("not-a-real-token")).isEmpty();
	}

}
