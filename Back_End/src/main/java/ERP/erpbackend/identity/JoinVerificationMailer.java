package ERP.erpbackend.identity;

/**
 * Sends the email that lets a prospective employee finish a self-join request by proving control of
 * their address. This is the feature-19 seam: real SMTP / provider delivery swaps the implementation,
 * mirroring {@link GoogleTokenExchangeClient} / {@link GoogleTokenExchangeClientImpl}. Keep the
 * signature stable.
 */
public interface JoinVerificationMailer {

	void send(String toEmail, String organizationName, String verificationLink);

}
