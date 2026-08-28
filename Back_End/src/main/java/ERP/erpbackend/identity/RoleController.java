package ERP.erpbackend.identity;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleController {

	private final RoleService roleService;

	@GetMapping("/roles")
	@PreAuthorize("@perms.has('role.view')")
	public ResponseEntity<List<RoleSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(roleService.listRoles(caller));
	}

	@GetMapping("/roles/{id}")
	@PreAuthorize("@perms.has('role.view')")
	public ResponseEntity<RoleDetailResponse> get(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id) {
		return ResponseEntity.ok(roleService.getRole(caller, id));
	}

	@PostMapping("/roles")
	@PreAuthorize("@perms.has('role.create')")
	public ResponseEntity<RoleDetailResponse> create(@AuthenticationPrincipal AuthenticatedUser caller,
			@Valid @RequestBody CreateRoleRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(caller, request));
	}

	@PutMapping("/roles/{id}")
	@PreAuthorize("@perms.has('role.edit')")
	public ResponseEntity<RoleDetailResponse> update(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
		return ResponseEntity.ok(roleService.updateRole(caller, id, request));
	}

	@DeleteMapping("/roles/{id}")
	@PreAuthorize("@perms.has('role.delete')")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
		roleService.deleteRole(caller, id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/roles/{id}/members")
	@PreAuthorize("@perms.has('role.edit')")
	public ResponseEntity<Void> assignMember(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id, @Valid @RequestBody AssignMemberRequest request) {
		roleService.assignMember(caller, id, request.userId());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/roles/{id}/members/{userId}")
	@PreAuthorize("@perms.has('role.edit')")
	public ResponseEntity<Void> unassignMember(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id, @PathVariable UUID userId) {
		roleService.unassignMember(caller, id, userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/permissions")
	@PreAuthorize("@perms.has('role.view')")
	public ResponseEntity<List<PermissionResponse>> permissions() {
		return ResponseEntity.ok(roleService.listPermissions());
	}

}
