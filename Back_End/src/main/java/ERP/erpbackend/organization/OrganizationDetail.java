package ERP.erpbackend.organization;

import java.time.Instant;
import java.util.UUID;

/** One Organization as the tenant-administration API exposes it: identity, code, active flag, and creation time. */
public record OrganizationDetail(UUID id, String name, String code, boolean active, Instant createdAt) {
}
