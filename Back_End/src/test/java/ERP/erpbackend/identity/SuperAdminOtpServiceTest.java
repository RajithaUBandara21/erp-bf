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
class SuperAdminOtpServiceTest {

	@Autowired
	private SuperAdminOtpService superAdminOtpService;

	@Test
	void consumingTheRightCodeSucceedsOnceThenFailsOnASecondAttempt() {
		UUID userId = UUID.randomUUID();
		String code = superAdminOtpService.issue(userId);

		assertThat(superAdminOtpService.consume(userId, code)).isTrue();
		assertThat(superAdminOtpService.consume(userId, code)).isFalse();
	}

	@Test
	void aWrongCodeFailsWithoutInvalidatingTheRealOne() {
		UUID userId = UUID.randomUUID();
		String code = superAdminOtpService.issue(userId);
		String wrongCode = "000000".equals(code) ? "999999" : "000000";

		assertThat(superAdminOtpService.consume(userId, wrongCode)).isFalse();
		assertThat(superAdminOtpService.consume(userId, code)).isTrue();
	}

	@Test
	void anUnknownUserIdReturnsFalse() {
		assertThat(superAdminOtpService.consume(UUID.randomUUID(), "123456")).isFalse();
	}

}
