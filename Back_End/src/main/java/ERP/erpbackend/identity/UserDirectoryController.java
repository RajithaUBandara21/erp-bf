package ERP.erpbackend.identity;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only tenant user directory that backs the role member picker. Tenant is taken from the security context only. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserDirectoryController {

	private final UserRepository userRepository;

	@GetMapping("/users")
	@PreAuthorize("@perms.has('user.view')")
	public ResponseEntity<List<UserSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser caller) {
		List<UserSummaryResponse> users = userRepository.findByTenantIdOrderByFullNameAsc(caller.tenantId()).stream()
				.map(user -> new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail()))
				.toList();
		return ResponseEntity.ok(users);
	}

}
