package ERP.erpbackend.identity;

public enum LoginOutcome {

	/** Credentials verified and the caller has exactly one ACTIVE Membership - {@code session} is populated. */
	AUTHENTICATED,

	/** Credentials verified but the caller has several ACTIVE Memberships - pick one via {@code /api/auth/login/select}. */
	SELECT_ORGANIZATION
}
