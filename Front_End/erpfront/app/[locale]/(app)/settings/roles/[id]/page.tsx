import type { ReactNode } from "react";
import { getLocale, getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { groupCatalog, resourceLabel } from "@/lib/permission-matrix";
import { can, fetchMyPermissions } from "@/lib/permissions";
import type { PermissionCatalogEntry, RoleDetail, UserSummary } from "@/types/roles";
import { DeleteRoleButton } from "@/components/roles/DeleteRoleButton";
import { RoleDetailView } from "@/components/roles/RoleDetailView";
import { RoleForm } from "@/components/roles/RoleForm";
import { RoleMembers } from "@/components/roles/RoleMembers";

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export default async function RoleDetailPage({ params }: { params: Promise<{ id: string }> }) {
	const { id } = await params;
	const rolesT = await getTranslations("roles");

	if (!UUID_RE.test(id)) {
		return <Notice>{rolesT("notFound")}</Notice>;
	}

	const [roleResult, catalogResult, usersResult, perms, permissionsT] = await Promise.all([
		authedFetch<RoleDetail>(`/api/roles/${id}`),
		authedFetch<PermissionCatalogEntry[]>("/api/permissions"),
		authedFetch<UserSummary[]>("/api/users"),
		fetchMyPermissions(),
		getTranslations("permissions"),
	]);

	if (
		(!roleResult.success && "unauthorized" in roleResult) ||
		(!catalogResult.success && "unauthorized" in catalogResult)
	) {
		redirect({ href: "/sign-in", locale: await getLocale() });
	}

	if (!roleResult.success && roleResult.status === 404) {
		return <Notice>{rolesT("notFound")}</Notice>;
	}

	if (!roleResult.success || !catalogResult.success) {
		return <Notice>{rolesT("detailLoadError")}</Notice>;
	}

	const role = roleResult.data;
	const groups = groupCatalog(catalogResult.data ?? [], (resource) =>
		permissionsT.has(`resources.${resource}`) ? permissionsT(`resources.${resource}`) : resourceLabel(resource),
	);
	const canEditMembers = can(perms, "role.edit");
	const editable = !role.systemManaged && canEditMembers;
	const deletable = !role.systemManaged && can(perms, "role.delete");
	const deleteButton = deletable ? <DeleteRoleButton roleId={role.id} /> : undefined;

	// /api/users needs `user.view`; a 403/error just means no picker, so fall back to an empty directory.
	const candidates = usersResult.success ? usersResult.data : [];
	const membersSlot = (
		<RoleMembers roleId={role.id} members={role.members} candidates={candidates} canEdit={canEditMembers} />
	);

	return editable ? (
		<RoleForm mode="edit" role={role} groups={groups} headerActions={deleteButton} membersSlot={membersSlot} />
	) : (
		<RoleDetailView role={role} groups={groups} headerActions={deleteButton} membersSlot={membersSlot} />
	);
}

function Notice({ children }: { children: ReactNode }) {
	return (
		<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
			{children}
		</div>
	);
}
