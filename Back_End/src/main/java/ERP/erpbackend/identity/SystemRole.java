package ERP.erpbackend.identity;

import java.util.List;
import java.util.function.Predicate;

/**
 * The built-in roles every tenant is provisioned with. Each carries the rule for which
 * catalog permissions it grants; enforcing that these are not renamed or deleted is 1d.2.
 */
public enum SystemRole {

	OWNER("Owner", "Full, unrestricted access to every module, including billing and subscription.",
			permission -> true),
	TENANT_ADMIN("Tenant Admin",
			"Owner-level access to every Organization under the Tenant, including billing and subscription.",
			permission -> true),
	ADMINISTRATOR("Administrator", "Manages users, roles, organizations, and every business module. Cannot change billing.",
			permission -> !"billing".equals(permission.getResource())),
	VIEWER("Viewer", "Read-only access across every module. Creates or changes nothing.",
			permission -> permission.getAction() == PermissionAction.VIEW);

	private final String displayName;
	private final String description;
	private final Predicate<Permission> permissionRule;

	SystemRole(String displayName, String description, Predicate<Permission> permissionRule) {
		this.displayName = displayName;
		this.description = description;
		this.permissionRule = permissionRule;
	}

	public String displayName() {
		return displayName;
	}

	public String description() {
		return description;
	}

	public List<Permission> permissionsFrom(List<Permission> catalog) {
		return catalog.stream().filter(permissionRule).toList();
	}

}
