package ERP.erpbackend.identity;

import ERP.erpbackend.organization.OrganizationDetail;
import ERP.erpbackend.organization.OrganizationListView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant-administration endpoints for Organizations. Authentication is enforced by the filter chain;
 * the Tenant Admin check is in {@link OrganizationProvisioningService} (see its javadoc for why this
 * is not a {@code @perms.has(...)} gate).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrganizationController {

	private final OrganizationProvisioningService organizationProvisioningService;

	/** Every Organization under the caller's Tenant, plus the Tenant's plan and organization limit. */
	@GetMapping("/organizations")
	public ResponseEntity<OrganizationListView> list(@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(organizationProvisioningService.list(caller));
	}

	/** Create an Organization under the caller's Tenant; 409 once the Tenant is at its organization limit. */
	@PostMapping("/organizations")
	public ResponseEntity<OrganizationDetail> create(@AuthenticationPrincipal AuthenticatedUser caller,
			@Valid @RequestBody CreateOrganizationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(organizationProvisioningService.create(caller, request.name()));
	}
}
