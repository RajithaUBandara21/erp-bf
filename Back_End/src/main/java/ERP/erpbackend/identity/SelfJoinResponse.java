package ERP.erpbackend.identity;

/** Fixed {@code 202} body of {@code POST /api/auth/join} - identical for every outcome, so the endpoint leaks nothing about accounts. */
public record SelfJoinResponse(String message) {
}
