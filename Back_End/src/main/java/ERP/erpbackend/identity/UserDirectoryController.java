package ERP.erpbackend.identity;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only user directory that backs the role member picker. Organization is taken from the security context only. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserDirectoryController {

	private final UserDirectoryService userDirectoryService;

	@GetMapping("/users")
	@PreAuthorize("@perms.has('user.view')")
	public ResponseEntity<List<UserSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(userDirectoryService.listActiveOrganizationMembers(caller.organizationId()));
	}

}
