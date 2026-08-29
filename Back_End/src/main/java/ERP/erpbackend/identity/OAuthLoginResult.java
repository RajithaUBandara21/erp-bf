package ERP.erpbackend.identity;

/** Outcome of a Google sign-in attempt, for the controller to map to a redirect. */
record OAuthLoginResult(String exchangeCode, String errorReason) {

	static OAuthLoginResult success(String exchangeCode) {
		return new OAuthLoginResult(exchangeCode, null);
	}

	static OAuthLoginResult failure(String reason) {
		return new OAuthLoginResult(null, reason);
	}

	boolean succeeded() {
		return exchangeCode != null;
	}

}
