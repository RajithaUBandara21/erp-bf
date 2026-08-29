import { Link } from "@/i18n/navigation";
import type { RoleSummary } from "@/types/roles";
import { RoleTypeBadge } from "@/components/roles/RoleTypeBadge";

// i18n keys (build-plan 4): role_col, description_col, members_col, permissions_col, type_col, roles_empty

export function RoleList({ roles }: { roles: RoleSummary[] }) {
	if (roles.length === 0) {
		return (
			<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
				No roles yet.
			</div>
		);
	}

	return (
		<div className="overflow-x-auto rounded-lg border border-border bg-surface shadow-sm">
			<table className="w-full min-w-[560px] border-collapse text-[13px]">
				<thead>
					<tr className="border-b border-border bg-surface-alt text-left text-[11px] font-bold uppercase tracking-wide text-muted">
						<th className="p-3">Role</th>
						<th className="p-3">Description</th>
						<th className="p-3 text-right">Members</th>
						<th className="p-3 text-right">Permissions</th>
						<th className="p-3">Type</th>
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
