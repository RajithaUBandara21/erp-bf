package ERP.erpbackend.identity;

/** {@code 200} body of {@code POST /api/auth/verify-email}. Naming the Organization back is not a leak - the caller supplied its invite code. */
public record VerifyEmailResponse(String message, String organizationName) {
}
