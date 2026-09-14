package ERP.erpbackend.identity;

import java.util.Optional;
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
		return principal().map(user -> resolver.resolve(user.membershipId()).contains(code)).orElse(false);
	}

	/**
	 * True for a Super Admin whose forced password change is already done. The gate every
	 * cross-tenant endpoint besides change-password should use.
	 */
	public boolean isSuperAdmin() {
		return principal().filter(AuthenticatedUser::platformSuperAdmin)
				.filter(user -> !user.mustChangePassword())
				.isPresent();
	}

	/**
	 * True for a Super Admin regardless of a pending forced password change. Only the
	 * change-password endpoint itself should use this - it must stay reachable precisely when
	 * {@link #isSuperAdmin()} is false.
	 */
	public boolean isSuperAdminPrincipal() {
		return principal().map(AuthenticatedUser::platformSuperAdmin).orElse(false);
	}

	private Optional<AuthenticatedUser> principal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			return Optional.empty();
		}
		return Optional.of(user);
	}

}
