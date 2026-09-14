package ERP.erpbackend.identity;

/** Result of {@code POST /api/auth/super-admin/login} - an OTP was emailed; pass this token back to verify it. */
public record SuperAdminLoginChallengeResponse(String challengeToken) {
}
