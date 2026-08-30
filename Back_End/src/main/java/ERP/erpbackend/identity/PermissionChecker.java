package ERP.erpbackend.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** SpEL entry point for {@code @PreAuthorize("@perms.has('role.edit')")}. */
@Component("perms")
@RequiredArgsConstructor
public class PermissionChecker {

	private final EffectivePermissionResolver resolver;

	public boolean has(String code) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			return false;
		}
		return resolver.resolve(user.membershipId()).contains(code);
	}

}
