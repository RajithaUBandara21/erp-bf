package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Body of the approve action: the Role to assign as the joiner's Membership is activated. */
public record MembershipApprovalRequest(@NotNull UUID roleId) {
}
