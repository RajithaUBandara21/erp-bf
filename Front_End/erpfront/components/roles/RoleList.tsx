import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import type { RoleSummary } from "@/types/roles";
import { RoleTypeBadge } from "@/components/roles/RoleTypeBadge";

export async function RoleList({ roles }: { roles: RoleSummary[] }) {
	const t = await getTranslations("roles");

	if (roles.length === 0) {
		return (
			<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
				{t("list.empty")}
			</div>
		);
	}

	return (
		<div className="overflow-x-auto rounded-lg border border-border bg-surface shadow-sm">
			<table className="w-full min-w-[560px] border-collapse text-[13px]">
				<thead>
					<tr className="border-b border-border bg-surface-alt text-left text-[11px] font-bold uppercase tracking-wide text-muted">
						<th className="p-3">{t("list.roleColumn")}</th>
						<th className="p-3">{t("list.descriptionColumn")}</th>
						<th className="p-3 text-right">{t("list.membersColumn")}</th>
						<th className="p-3 text-right">{t("list.permissionsColumn")}</th>
						<th className="p-3">{t("list.typeColumn")}</th>
					</tr>
				</thead>
				<tbody>
					{roles.map((role) => (
						<tr key={role.id} className="border-b border-border last:border-b-0 hover:bg-surface-alt">
							<td className="p-3">
								<Link
									href={`/settings/roles/${role.id}`}
									className="font-semibold text-text hover:text-accent"
								>
									{role.name}
								</Link>
							</td>
							<td className="max-w-[340px] p-3 text-xs text-muted">{role.description ?? "-"}</td>
							<td className="p-3 text-right tabular-nums">{role.memberCount}</td>
							<td className="p-3 text-right tabular-nums">{role.permissionCount}</td>
							<td className="p-3">
								<RoleTypeBadge systemManaged={role.systemManaged} />
							</td>
						</tr>
					))}
				</tbody>
			</table>
		</div>
	);
}
