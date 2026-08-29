import { getLocale } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { groupCatalog } from "@/lib/permission-matrix";
import { can, fetchMyPermissions } from "@/lib/permissions";
import type { PermissionCatalogEntry } from "@/types/roles";
import { RoleForm } from "@/components/roles/RoleForm";

// i18n keys (build-plan 4): new_role, catalog_load_error
export default async function NewRolePage() {
	const [catalogResult, perms, locale] = await Promise.all([
		authedFetch<PermissionCatalogEntry[]>("/api/permissions"),
		fetchMyPermissions(),
		getLocale(),
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
				We couldn&apos;t load the permission catalog right now. Please refresh to try again.
			</div>
		);
	}

	return <RoleForm mode="create" groups={groupCatalog(catalogResult.data ?? [])} />;
}
