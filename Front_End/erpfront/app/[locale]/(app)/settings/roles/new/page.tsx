import { getLocale, getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { groupCatalog, resourceLabel } from "@/lib/permission-matrix";
import { can, fetchMyPermissions } from "@/lib/permissions";
import type { PermissionCatalogEntry } from "@/types/roles";
import { RoleForm } from "@/components/roles/RoleForm";

export default async function NewRolePage() {
	const [catalogResult, perms, locale, permissionsT, rolesT] = await Promise.all([
		authedFetch<PermissionCatalogEntry[]>("/api/permissions"),
		fetchMyPermissions(),
		getLocale(),
		getTranslations("permissions"),
		getTranslations("roles"),
	]);

	if (!catalogResult.success && "unauthorized" in catalogResult) {
		redirect({ href: "/sign-in", locale });
	}

	if (!can(perms, "role.create")) {
		redirect({ href: "/settings/roles", locale });
	}

	if (!catalogResult.success) {
		return (
			<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
				{rolesT("catalogLoadError")}
			</div>
		);
	}

	const groups = groupCatalog(catalogResult.data ?? [], (resource) =>
		permissionsT.has(`resources.${resource}`) ? permissionsT(`resources.${resource}`) : resourceLabel(resource),
	);
	return <RoleForm mode="create" groups={groups} />;
}
