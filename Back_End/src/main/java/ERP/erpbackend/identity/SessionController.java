package ERP.erpbackend.identity;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/sessions")
@RequiredArgsConstructor
public class SessionController {

	private final SessionService sessionService;

	@GetMapping
	public ResponseEntity<List<SessionResponse>> list(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return ResponseEntity.ok(sessionService.listSessions(authenticatedUser));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> revoke(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id) {
		sessionService.revokeSession(authenticatedUser, id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/revoke-others")
	public ResponseEntity<Void> revokeOthers(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		sessionService.revokeOtherSessions(authenticatedUser);
		return ResponseEntity.noContent().build();
	}

}
