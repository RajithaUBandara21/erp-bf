package ERP.erpbackend.identity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Development stand-in that logs the verification link at INFO instead of delivering an email.
 * Feature 19 replaces this with real delivery. Logging a link that embeds the token is an accepted
 * gap until then: the token is single-use and expires in 24h. No config flag suppresses it by design.
 */
@Slf4j
@Component
public class LoggingJoinVerificationMailer implements JoinVerificationMailer {

	@Override
	public void send(String toEmail, String organizationName, String verificationLink) {
		log.info("Join verification link for {} joining \"{}\": {}", toEmail, organizationName, verificationLink);
	}

}
