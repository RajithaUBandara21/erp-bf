package ERP.erpbackend.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * View and rotate the invite code of the caller's currently selected Organization. Gated on
 * {@code organization.edit} (Owner, Tenant Admin, Administrator - not Viewer); a Tenant Admin rotating
 * a sibling Organization's code switches into it first.
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationInviteCodeController {

	private final OrganizationInviteCodeService organizationInviteCodeService;

	@GetMapping("/invite-code")
	@PreAuthorize("@perms.has('organization.edit')")
	public ResponseEntity<InviteCodeResponse> get(@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(organizationInviteCodeService.read(caller));
	}

	@PostMapping("/invite-code/rotate")
	@PreAuthorize("@perms.has('organization.edit')")
	public ResponseEntity<InviteCodeResponse> rotate(@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(organizationInviteCodeService.rotate(caller));
	}

}
