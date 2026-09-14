package ERP.erpbackend.identity;

/**
 * Sends the Super Admin's one-time login code. This is the feature-19 seam: real SMTP /
 * provider delivery swaps the implementation, mirroring {@link JoinVerificationMailer}. Keep
 * the signature stable.
 */
public interface SuperAdminOtpMailer {

	void send(String toEmail, String code);

}
