package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import ERP.erpbackend.organization.OrganizationService;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gives a Tenant Admin Owner-level reach into every Organization under their own Tenant. The
 * first time they touch a sibling Organization they do not yet belong to, {@link #ensureOwnerMembership}
 * provisions an ACTIVE Membership there and assigns it that Tenant's Owner role.
 *
 * <p>No HTTP caller yet: this is the primitive 5b.3's Organization list and in-session switcher
 * build on. Keep both method signatures stable.
 */
@Service
@RequiredArgsConstructor
public class TenantAdminAccessService implements TenantAdminChecker {

	private final OrganizationService organizationService;
	private final MembershipRepository membershipRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final AuditService auditService;

	@Override
	public boolean isTenantAdmin(UUID userId, UUID tenantId) {
		return membershipRepository.existsActiveMembershipAssignedSystemRole(
				userId, tenantId, SystemRole.TENANT_ADMIN.displayName());
	}

	/** Every tenant the user administers, resolved in a single query (vs. one {@link #isTenantAdmin} call per tenant). */
	public Set<UUID> administeredTenantIds(UUID userId) {
		return membershipRepository.findTenantIdsWithActiveMembershipAssignedSystemRole(
				userId, SystemRole.TENANT_ADMIN.displayName());
	}

	/**
	 * The caller's ACTIVE Membership in {@code organizationId}, auto-provisioning one with the tenant's
	 * Owner role if the caller is a Tenant Admin of that tenant and has none yet.
	 *
	 * @throws ResponseStatusException 404 if no organization has that id, 403 if the caller is not a
	 *     Tenant Admin of the owning tenant, 409 if a non-ACTIVE Membership already exists for the pair
	 */
	@Transactional
	public Membership ensureOwnerMembership(UUID userId, UUID organizationId) {
		UUID tenantId = organizationService.findTenantId(organizationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

		if (!isTenantAdmin(userId, tenantId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a Tenant Admin of this organization's tenant.");
		}

		Membership existing = membershipRepository.findByUserIdAndOrganizationId(userId, organizationId).orElse(null);
		if (existing != null) {
			if (existing.getStatus() != MembershipStatus.ACTIVE) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"A non-active membership already exists for this organization.");
			}
			return existing;
		}

		Membership membership = new Membership();
		membership.setUserId(userId);
		membership.setTenantId(tenantId);
		membership.setOrganizationId(organizationId);
		membership.setStatus(MembershipStatus.ACTIVE);
		membership = membershipRepository.save(membership);

		Role ownerRole = roleRepository.findByTenantIdAndName(tenantId, SystemRole.OWNER.displayName())
				.orElseThrow(() -> new IllegalStateException("Tenant " + tenantId + " has no system Owner role"));
		UserRole assignment = new UserRole();
		assignment.setTenantId(tenantId);
		assignment.setMembershipId(membership.getId());
		assignment.setRoleId(ownerRole.getId());
		userRoleRepository.save(assignment);

		auditService.log(new AuditEvent(tenantId, organizationId, userId, "Membership", membership.getId(),
				"membership.auto_provisioned", null, Map.of("via", "TENANT_ADMIN")));
		return membership;
	}

}
