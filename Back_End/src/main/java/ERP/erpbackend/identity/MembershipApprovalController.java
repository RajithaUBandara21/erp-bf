package ERP.erpbackend.identity;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Org Admin's approval queue for verified-but-PENDING self-join requests, all gated on
 * {@code user.approve} (Owner, Tenant Admin, Administrator - not Viewer). Thin: request/response
 * mapping and the {@code @PreAuthorize} gate only; every guard, state change, and audit write lives in
 * {@link MembershipApprovalService}. The Organization is always {@code caller.organizationId()}.
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class MembershipApprovalController {

	private final MembershipApprovalService membershipApprovalService;

	@GetMapping("/join-requests")
	@PreAuthorize("@perms.has('user.approve')")
	public ResponseEntity<List<PendingJoinRequestResponse>> listJoinRequests(
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(membershipApprovalService.listPending(caller));
	}

	@PostMapping("/join-requests/{membershipId}/approve")
	@PreAuthorize("@perms.has('user.approve')")
	public ResponseEntity<Void> approve(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID membershipId, @Valid @RequestBody MembershipApprovalRequest request) {
		membershipApprovalService.approve(caller, membershipId, request.roleId());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/join-requests/{membershipId}/reject")
	@PreAuthorize("@perms.has('user.approve')")
	public ResponseEntity<Void> reject(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID membershipId) {
		membershipApprovalService.reject(caller, membershipId);
		return ResponseEntity.noContent().build();
	}

}
