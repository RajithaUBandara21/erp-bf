package ERP.erpbackend.identity;

/** Outcome of linking a Google identity to a user, for the controller to map to a redirect. */
record OAuthLinkResult(boolean linked, String errorReason) {

	static OAuthLinkResult success() {
		return new OAuthLinkResult(true, null);
	}

	static OAuthLinkResult failure(String reason) {
		return new OAuthLinkResult(false, reason);
	}

}
