package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import ERP.erpbackend.organization.OrganizationDetail;
import ERP.erpbackend.organization.OrganizationListView;
import ERP.erpbackend.organization.OrganizationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Tenant-administration surface for Organizations: a Tenant Admin lists every Organization under
 * their currently selected Tenant and creates new ones (gated by {@code Tenant.maxOrganizations}).
 *
 * <p>Lives in {@code identity} rather than {@code organization} so the {@code identity -> organization}
 * module dependency stays one-directional: this orchestrates the tenant-admin check plus Membership
 * provisioning, delegating the Organization row itself to {@link OrganizationService}.
 */
@Service
@RequiredArgsConstructor
public class OrganizationProvisioningService {

	private final TenantAdminAccessService tenantAdminAccessService;
	private final OrganizationService organizationService;
	private final AuditService auditService;

	/** Every Organization under the caller's Tenant, with its plan and organization limit. */
	@Transactional(readOnly = true)
	public OrganizationListView list(AuthenticatedUser caller) {
		requireTenantAdmin(caller);
		return organizationService.findAllByTenantId(caller.tenantId());
	}

	/**
	 * Create an Organization under the caller's Tenant and give the caller an ACTIVE Owner Membership in
	 * it. Refused with 409 once the Tenant is at or over its {@code maxOrganizations} limit.
	 */
	@Transactional
	public OrganizationDetail create(AuthenticatedUser caller, String name) {
		requireTenantAdmin(caller);
		OrganizationDetail organization = organizationService.createOrganization(caller.tenantId(), name);
		tenantAdminAccessService.ensureOwnerMembership(caller.userId(), organization.id());
		auditService.log(new AuditEvent(caller.tenantId(), organization.id(), caller.userId(),
				"Organization", organization.id(), "organization.created", null,
				Map.of("name", organization.name(), "code", organization.code())));
		return organization;
	}

	private void requireTenantAdmin(AuthenticatedUser caller) {
		if (!tenantAdminAccessService.isTenantAdmin(caller.userId(), caller.tenantId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Only a Tenant Admin can manage this tenant's organizations.");
		}
	}

}
