package ERP.erpbackend.identity;

import java.util.UUID;

/** One Organization the caller can sign in to, offered during the two-step login. */
public record MembershipOption(UUID membershipId, UUID organizationId, String organizationName) {
}
