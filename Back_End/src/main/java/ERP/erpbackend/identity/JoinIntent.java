package ERP.erpbackend.identity;

/**
 * A self-join request held in Redis under a single-use verification token until the person proves
 * control of the email. {@code passwordHash} and {@code fullName} are {@code null} when the intent
 * attaches to an account that already exists. Redis-only, never persisted; {@code inviteCode} is
 * stored (not the resolved Organization id) so it is re-resolved on verification and a rotation
 * during the token's window invalidates the pending join.
 */
public record JoinIntent(String email, String passwordHash, String fullName, String inviteCode) {
}
