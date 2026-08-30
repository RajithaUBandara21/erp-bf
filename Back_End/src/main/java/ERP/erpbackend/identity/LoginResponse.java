package ERP.erpbackend.identity;

import java.util.List;

/**
 * Result of {@code POST /api/auth/login}. {@code AUTHENTICATED} carries a live {@code session};
 * {@code SELECT_ORGANIZATION} carries a single-use {@code selectionToken} and the Organizations to
 * choose between, with no session issued yet.
 */
public record LoginResponse(
		LoginOutcome outcome,
		TokenResponse session,
		String selectionToken,
		List<MembershipOption> organizations) {

	public static LoginResponse authenticated(TokenResponse session) {
		return new LoginResponse(LoginOutcome.AUTHENTICATED, session, null, null);
	}

	public static LoginResponse selectOrganization(String selectionToken, List<MembershipOption> organizations) {
		return new LoginResponse(LoginOutcome.SELECT_ORGANIZATION, null, selectionToken, organizations);
	}
}
