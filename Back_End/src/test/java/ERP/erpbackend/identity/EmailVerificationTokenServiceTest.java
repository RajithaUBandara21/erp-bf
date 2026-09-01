package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EmailVerificationTokenServiceTest {

	@Autowired
	private EmailVerificationTokenService emailVerificationTokenService;

	private static JoinIntent newAccountIntent() {
		return new JoinIntent("ada@acme.test", "$2a$10$abcdefghijklmnopqrstuv", "Ada Joiner", "ABCDEFGHJK");
	}

	@Test
	void issuingThenConsumingReturnsTheJoinIntent() {
		JoinIntent intent = newAccountIntent();

		String token = emailVerificationTokenService.issue(intent);

		assertThat(emailVerificationTokenService.consume(token)).contains(intent);
	}

	@Test
	void consumingTheSameTokenTwiceReturnsEmptyTheSecondTime() {
		String token = emailVerificationTokenService.issue(newAccountIntent());
		emailVerificationTokenService.consume(token);

		assertThat(emailVerificationTokenService.consume(token)).isEmpty();
	}

	@Test
	void consumingAnUnknownTokenReturnsEmpty() {
		assertThat(emailVerificationTokenService.consume("not-a-real-token")).isEmpty();
	}

	@Test
	void nullPasswordHashAndFullNameSurviveTheJsonRoundTrip() {
		JoinIntent attachToExistingAccount = new JoinIntent("ada@acme.test", null, null, "ABCDEFGHJK");

		String token = emailVerificationTokenService.issue(attachToExistingAccount);

		assertThat(emailVerificationTokenService.consume(token)).hasValueSatisfying(intent -> {
			assertThat(intent.passwordHash()).isNull();
			assertThat(intent.fullName()).isNull();
			assertThat(intent.email()).isEqualTo("ada@acme.test");
			assertThat(intent.inviteCode()).isEqualTo("ABCDEFGHJK");
		});
	}

}
