package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import ERP.erpbackend.organization.OrganizationService;
import ERP.erpbackend.organization.OrganizationSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Organizations an authenticated person can move between without logging out: every Organization
 * they hold an ACTIVE Membership in, plus every other Organization under a Tenant they administer
 * (reachable through {@link TenantAdminAccessService}'s auto-provision primitive).
 */
@Service
@RequiredArgsConstructor
public class OrganizationSwitchService {

	private final MembershipRepository membershipRepository;
	private final TenantAdminAccessService tenantAdminAccessService;
	private final OrganizationService organizationService;
	private final SessionRepository sessionRepository;
	private final UserRepository userRepository;
	private final SessionTokenIssuer sessionTokenIssuer;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	public List<ReachableOrganizationResponse> listReachable(AuthenticatedUser caller) {
		List<Membership> activeMemberships =
				membershipRepository.findByUserIdAndStatus(caller.userId(), MembershipStatus.ACTIVE);

		Set<UUID> tenantIds = activeMemberships.stream()
				.map(Membership::getTenantId)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<OrganizationSummary> activeOrgs = organizationService.findActiveByTenantIds(tenantIds);
		Map<UUID, OrganizationSummary> activeOrgById = activeOrgs.stream()
				.collect(Collectors.toMap(OrganizationSummary::id, summary -> summary));
		Map<UUID, String> tenantNames = organizationService.findTenantNamesByIds(tenantIds);
		Set<UUID> heldOrgIds = activeMemberships.stream()
				.map(Membership::getOrganizationId)
				.collect(Collectors.toSet());

		List<ReachableOrganizationResponse> rows = new ArrayList<>();

		// The caller's own ACTIVE Memberships, skipping any whose Organization is inactive
		// (a deactivated org is absent from activeOrgById).
		for (Membership membership : activeMemberships) {
			OrganizationSummary org = activeOrgById.get(membership.getOrganizationId());
			if (org != null) {
				rows.add(toRow(org, tenantNames, caller, false));
			}
		}

		// Tenant-admin reach: every other active Organization under a Tenant the caller administers.
		Set<UUID> administeredTenantIds = tenantIds.stream()
				.filter(tenantId -> tenantAdminAccessService.isTenantAdmin(caller.userId(), tenantId))
				.collect(Collectors.toSet());
		for (OrganizationSummary org : activeOrgs) {
			if (administeredTenantIds.contains(org.tenantId()) && !heldOrgIds.contains(org.id())) {
				rows.add(toRow(org, tenantNames, caller, true));
			}
		}

		rows.sort(Comparator
				.comparing(ReachableOrganizationResponse::tenantName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(ReachableOrganizationResponse::organizationName, String.CASE_INSENSITIVE_ORDER));
		return rows;
	}

	private ReachableOrganizationResponse toRow(OrganizationSummary org, Map<UUID, String> tenantNames,
			AuthenticatedUser caller, boolean viaTenantAdmin) {
		return new ReachableOrganizationResponse(
				org.id(),
				org.name(),
				org.tenantId(),
				tenantNames.getOrDefault(org.tenantId(), ""),
				org.id().equals(caller.organizationId()),
				viaTenantAdmin);
	}

	/**
	 * Re-points the caller's existing Session at another reachable Organization and hands back a fresh
	 * access + refresh token scoped to it. The Session row is reused: {@code expiresAt} and its identity
	 * are untouched, so the refresh token issued at login keeps working and follows the switch.
	 */
	@Transactional
	public TokenResponse switchOrganization(AuthenticatedUser caller, UUID organizationId) {
		Session session = sessionRepository.findById(caller.sessionId())
				.filter(candidate -> candidate.getUserId().equals(caller.userId()))
				.filter(OrganizationSwitchService::isUsable)
				.orElseThrow(OrganizationSwitchService::sessionNoLongerValid);

		User user = userRepository.findById(session.getUserId())
				.filter(User::isActive)
				.orElseThrow(OrganizationSwitchService::sessionNoLongerValid);

		Membership target = resolveTarget(session.getUserId(), organizationId);

		// The Session is the source of truth for the currently selected Organization; the token claim
		// can lag it after an earlier in-session switch, so guard against the Session's membership id.
		if (target.getId().equals(session.getMembershipId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already in this organization.");
		}

		UUID previousMembershipId = session.getMembershipId();
		UUID previousOrganizationId = membershipRepository.findById(previousMembershipId)
				.map(Membership::getOrganizationId)
				.orElse(null);

		session.setMembershipId(target.getId());
		session.setTenantId(target.getTenantId());
		session.setLastUsedAt(Instant.now());
		sessionRepository.save(session);

		auditService.log(new AuditEvent(target.getTenantId(), target.getOrganizationId(), session.getUserId(),
				"Session", session.getId(), "auth.organization_switched",
				organizationSnapshot(previousOrganizationId, previousMembershipId),
				organizationSnapshot(target.getOrganizationId(), target.getId())));

		return sessionTokenIssuer.issueTokens(user, target, session);
	}

	/**
	 * The caller's own ACTIVE Membership in the target Organization, or - when they hold none there - a
	 * Tenant-Admin Owner Membership auto-provisioned by the 5b.2 primitive (which 404s an unknown org,
	 * 403s a non-admin, and 409s a pre-existing non-ACTIVE Membership).
	 */
	private Membership resolveTarget(UUID userId, UUID organizationId) {
		return membershipRepository.findByUserIdAndOrganizationId(userId, organizationId)
				.filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
				.orElseGet(() -> tenantAdminAccessService.ensureOwnerMembership(userId, organizationId));
	}

	private static Map<String, Object> organizationSnapshot(UUID organizationId, UUID membershipId) {
		Map<String, Object> snapshot = new HashMap<>();
		snapshot.put("organizationId", organizationId);
		snapshot.put("membershipId", membershipId);
		return snapshot;
	}

	private static boolean isUsable(Session session) {
		return session.getRevokedAt() == null && session.getExpiresAt().isAfter(Instant.now());
	}

	private static ResponseStatusException sessionNoLongerValid() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is no longer valid.");
	}
}
