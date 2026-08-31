package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import ERP.erpbackend.organization.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * View and rotate the invite code of the caller's currently selected Organization. The Organization is
 * always {@code caller.organizationId()}, never a request parameter.
 *
 * <p>Lives in {@code identity} so the {@code identity -> organization} module dependency stays
 * one-directional: it holds the caller-to-actor mapping and the audit write, delegating the code
 * itself to {@link OrganizationService}.
 */
@Service
@RequiredArgsConstructor
public class OrganizationInviteCodeService {

	private final OrganizationService organizationService;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	public InviteCodeResponse read(AuthenticatedUser caller) {
		return new InviteCodeResponse(organizationService.findInviteCode(caller.organizationId()));
	}

	@Transactional
	public InviteCodeResponse rotate(AuthenticatedUser caller) {
		String inviteCode = organizationService.rotateInviteCode(caller.organizationId());
		// The code is a secret: the audit row records who/when/which-org, never the value itself.
		auditService.log(new AuditEvent(caller.tenantId(), caller.organizationId(), caller.userId(),
				"Organization", caller.organizationId(), "organization.invite_code_rotated", null, null));
		return new InviteCodeResponse(inviteCode);
	}

}
