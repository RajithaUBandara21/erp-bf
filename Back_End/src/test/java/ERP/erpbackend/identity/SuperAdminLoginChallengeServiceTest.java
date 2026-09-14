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
class SuperAdminLoginChallengeServiceTest {

	@Autowired
	private SuperAdminLoginChallengeService challengeService;

	@Test
	void issuingThenResolvingReturnsTheIssuedUserId() {
		UUID userId = UUID.randomUUID();

		String token = challengeService.issue(userId);

		assertThat(challengeService.resolve(token)).contains(userId);
	}

	@Test
	void resolvingDoesNotInvalidateTheToken() {
		UUID userId = UUID.randomUUID();
		String token = challengeService.issue(userId);

		challengeService.resolve(token);

		assertThat(challengeService.resolve(token)).contains(userId);
	}

	@Test
	void invalidateMakesTheTokenUnresolvable() {
		UUID userId = UUID.randomUUID();
		String token = challengeService.issue(userId);

		challengeService.invalidate(token);

		assertThat(challengeService.resolve(token)).isEmpty();
	}

	@Test
	void resolvingAnUnknownTokenReturnsEmpty() {
		assertThat(challengeService.resolve("unknown-token")).isEmpty();
	}

}
