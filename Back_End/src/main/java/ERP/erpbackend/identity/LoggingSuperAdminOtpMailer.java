package ERP.erpbackend.identity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Development stand-in that logs the one-time code at INFO instead of delivering an email.
 * Feature 19 replaces this with real delivery, matching {@link LoggingJoinVerificationMailer}.
 */
@Slf4j
@Component
public class LoggingSuperAdminOtpMailer implements SuperAdminOtpMailer {

	@Override
	public void send(String toEmail, String code) {
		log.info("Super Admin login code for {}: {}", toEmail, code);
	}

}
