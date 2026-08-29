package ERP.erpbackend.identity;

public interface GoogleTokenExchangeClient {

	/** Exchanges an authorization code for the Google identity that authorized it. */
	GoogleIdentity exchange(String code);

}
