import type { ReactNode } from "react";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import type { MatrixGroup } from "@/lib/permission-matrix";
import type { RoleDetail } from "@/types/roles";
import { PermissionMatrix } from "@/components/roles/PermissionMatrix";
import { RoleMembersList } from "@/components/roles/RoleMembersList";
import { RoleTypeBadge } from "@/components/roles/RoleTypeBadge";

/** Read-only role detail. Step 4 layers an editable form over this for custom roles. */
export async function RoleDetailView({
	role,
	groups,
	headerActions,
	membersSlot,
}: {
	role: RoleDetail;
	groups: MatrixGroup[];
	headerActions?: ReactNode;
	membersSlot?: ReactNode;
}) {
	const [t, tp] = await Promise.all([getTranslations("roles"), getTranslations("permissions")]);
	return (
		<div>
			<div className="mb-6 flex flex-wrap items-start justify-between gap-3">
				<div>
					<h1 className="text-xl font-semibold">{role.name}</h1>
					<div className="mt-1.5 flex items-center gap-2.5 text-[13px] text-muted">
						<RoleTypeBadge systemManaged={role.systemManaged} />
						<span>{t("memberCount", { count: role.memberCount })}</span>
					</div>
				</div>
				<div className="flex items-center gap-2">
					{headerActions}
					<Link
						href="/settings/roles"
						className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt"
					>
						{t("backToRoles")}
					</Link>
				</div>
			</div>

			<div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_260px] lg:items-start">
				<section className="rounded-lg border border-border bg-surface p-5 shadow-sm">
					<div className="mb-3 flex items-center justify-between gap-2">
						<h2 className="text-sm font-semibold">{tp("heading")}</h2>
						<span className="text-[11px] text-faint">{tp("scopeNote")}</span>
					</div>
					{role.systemManaged && (
						<p className="mb-3 rounded-[5px] border border-border bg-surface-alt px-3 py-2.5 text-xs text-muted">
							{t("systemRoleNote")}
						</p>
					)}
					<PermissionMatrix groups={groups} selectedCodes={role.permissionCodes} readOnly />
					<p className="mt-3 text-[11px] text-faint">{tp("legend")}</p>
				</section>

				<div className="flex flex-col gap-4">
					<section className="rounded-lg border border-border bg-surface p-5 shadow-sm">
						<h2 className="mb-3 text-sm font-semibold">{t("info.heading")}</h2>
						<ReadOnlyField label={t("info.nameLabel")} value={role.name} />
						<ReadOnlyField label={t("info.descriptionLabel")} value={role.description ?? ""} multiline />
						<ReadOnlyField label={t("info.scopeLabel")} value={t("info.scopeValue")} hint={t("info.scopeHint")} />
					</section>

					{membersSlot ?? <RoleMembersList members={role.members} />}
				</div>
			</div>
		</div>
	);
}

function ReadOnlyField({
	label,
	value,
	multiline = false,
	hint,
}: {
	label: string;
	value: string;
	multiline?: boolean;
	hint?: string;
}) {
	return (
		<div className="mb-3 flex flex-col gap-1.5 last:mb-0">
			<span className="text-xs font-semibold text-muted">{label}</span>
			<div
				className={`rounded-[5px] border border-border bg-bg px-2.5 py-2 text-[13px] text-muted ${
					multiline ? "min-h-[60px] whitespace-pre-wrap" : ""
				}`}
			>
				{value || "-"}
			</div>
			{hint && <span className="text-[11px] text-faint">{hint}</span>}
		</div>
	);
}
