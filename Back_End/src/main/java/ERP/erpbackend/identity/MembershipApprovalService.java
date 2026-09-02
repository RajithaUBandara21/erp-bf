package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Org Admin's queue for verified-but-PENDING self-join requests: list them, approve one (activate
 * the Membership and assign a Role in one action, so a Membership never goes live without a Role), or
 * reject one (drop the request). The Organization is always {@code caller.organizationId()} from the
 * currently selected Membership, never a request field.
 *
 * <p>Lives in {@code identity} (which already depends on {@code organization} and {@code audit}); it
 * acts on a membership id the caller already sees in the queue and never re-resolves an invite code.
 */
@Service
@RequiredArgsConstructor
public class MembershipApprovalService {

	private final MembershipRepository membershipRepository;
	private final UserRepository userRepository;
	private final RoleService roleService;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	public List<PendingJoinRequestResponse> listPending(AuthenticatedUser caller) {
		List<Membership> pending = membershipRepository.findByOrganizationIdAndStatus(
				caller.organizationId(), MembershipStatus.PENDING);
		if (pending.isEmpty()) {
			return List.of();
		}

		Map<UUID, User> usersById = userRepository.findAllById(
						pending.stream().map(Membership::getUserId).toList()).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));

		return pending.stream()
				.filter(membership -> usersById.containsKey(membership.getUserId()))
				.map(membership -> {
					User user = usersById.get(membership.getUserId());
					return new PendingJoinRequestResponse(membership.getId(), user.getId(),
							user.getFullName(), user.getEmail(), membership.getCreatedAt());
				})
				.sorted(Comparator.comparing(PendingJoinRequestResponse::requestedAt))
				.toList();
	}

	/**
	 * Flip the PENDING Membership to ACTIVE, then hand the Role grant to {@link RoleService#assignMember}
	 * - which resolves the role in the caller's Tenant, enforces the Owner-equivalent and
	 * cannot-exceed-held-permissions guards, dedups, and writes its own {@code role.member_assigned}
	 * audit row. The status flip is flushed first so {@code assignMember}'s Membership lookup sees the
	 * ACTIVE row; any guard failure inside it rolls the flip back with the rest of the transaction.
	 */
	@Transactional
	public void approve(AuthenticatedUser caller, UUID membershipId, UUID roleId) {
		Membership membership = requirePendingJoinRequest(caller, membershipId);
		membership.setStatus(MembershipStatus.ACTIVE);
		membershipRepository.saveAndFlush(membership);

		roleService.assignMember(caller, roleId, membership.getUserId());

		auditService.log(new AuditEvent(caller.tenantId(), caller.organizationId(), caller.userId(),
				"Membership", membership.getId(), "membership.approved",
				Map.of("status", "PENDING"),
				Map.of("status", "ACTIVE", "roleId", roleId.toString())));
	}

	/**
	 * Drop the PENDING Membership. A join request carries no history, {@code user_roles}, or downstream
	 * data, so a hard delete keeps {@link MembershipStatus} two-valued and lets a rejected person
	 * re-request later (5c.2's {@code verifyEmail} finds no row and writes a fresh PENDING one). The
	 * audit trail ({@code membership.join_requested} then {@code membership.rejected}) is the durable
	 * record.
	 */
	@Transactional
	public void reject(AuthenticatedUser caller, UUID membershipId) {
		Membership membership = requirePendingJoinRequest(caller, membershipId);
		membershipRepository.delete(membership);

		auditService.log(new AuditEvent(caller.tenantId(), caller.organizationId(), caller.userId(),
				"Membership", membership.getId(), "membership.rejected",
				Map.of("status", "PENDING"), null));
	}

	private Membership requirePendingJoinRequest(AuthenticatedUser caller, UUID membershipId) {
		Membership membership = membershipRepository.findById(membershipId)
				.filter(candidate -> caller.organizationId().equals(candidate.getOrganizationId()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Join request not found"));
		if (membership.getStatus() != MembershipStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This join request is no longer pending.");
		}
		return membership;
	}

}
