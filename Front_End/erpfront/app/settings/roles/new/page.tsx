import { redirect } from "next/navigation";
import { authedFetch } from "@/lib/api";
import { groupCatalog } from "@/lib/permission-matrix";
import { can, fetchMyPermissions } from "@/lib/permissions";
import type { PermissionCatalogEntry } from "@/types/roles";
import { RoleForm } from "@/components/roles/RoleForm";

// i18n keys (build-plan 4): new_role, catalog_load_error
export default async function NewRolePage() {
	const [catalogResult, perms] = await Promise.all([
		authedFetch<PermissionCatalogEntry[]>("/api/permissions"),
		fetchMyPermissions(),
	]);

	if (!catalogResult.success && "unauthorized" in catalogResult) {
		redirect("/sign-in");
	}

	if (!can(perms, "role.create")) {
		redirect("/settings/roles");
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
