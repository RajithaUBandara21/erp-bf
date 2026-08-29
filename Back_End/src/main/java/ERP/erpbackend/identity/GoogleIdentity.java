package ERP.erpbackend.identity;

/** {@code providerUserId} is Google's {@code sub} claim - the stable id, never {@code email}, everything is keyed on. */
public record GoogleIdentity(String providerUserId, String email, boolean emailVerified) {
}
