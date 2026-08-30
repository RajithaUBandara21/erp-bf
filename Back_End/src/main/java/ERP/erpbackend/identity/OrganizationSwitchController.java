package ERP.erpbackend.identity;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OrganizationSwitchController {

	private final OrganizationSwitchService organizationSwitchService;

	/** Every Organization the caller can switch into: their ACTIVE Memberships plus tenant-admin reach. */
	@GetMapping("/memberships")
	public ResponseEntity<List<ReachableOrganizationResponse>> memberships(
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(organizationSwitchService.listReachable(caller));
	}

	/** Re-point the caller's Session at another reachable Organization and re-issue tokens scoped to it. */
	@PostMapping("/switch-organization")
	public ResponseEntity<TokenResponse> switchOrganization(@AuthenticationPrincipal AuthenticatedUser caller,
			@Valid @RequestBody SwitchOrganizationRequest request) {
		return ResponseEntity.ok(organizationSwitchService.switchOrganization(caller, request.organizationId()));
	}
}
