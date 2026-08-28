package ERP.erpbackend.identity;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/me")
@RequiredArgsConstructor
public class CurrentUserController {

	private final EffectivePermissionResolver effectivePermissionResolver;

	/** The caller's own effective permission codes - used by the frontend to render the module grid and gate actions. */
	@GetMapping("/permissions")
	public ResponseEntity<MePermissionsResponse> permissions(@AuthenticationPrincipal AuthenticatedUser caller) {
		List<String> codes = effectivePermissionResolver.resolve(caller.userId(), caller.tenantId())
				.stream().sorted().toList();
		return ResponseEntity.ok(new MePermissionsResponse(codes));
	}

}
