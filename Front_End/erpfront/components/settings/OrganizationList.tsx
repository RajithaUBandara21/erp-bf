import { getTranslations } from "next-intl/server";
import type { OrganizationDetail } from "@/types/organizations";
import { formatAbsoluteDate } from "@/lib/format-time";

export async function OrganizationList({ organizations }: { organizations: OrganizationDetail[] }) {
	const t = await getTranslations("settings.organizations");

	if (organizations.length === 0) {
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
						<th className="p-3">{t("list.nameColumn")}</th>
						<th className="p-3">{t("list.codeColumn")}</th>
						<th className="p-3">{t("list.statusColumn")}</th>
						<th className="p-3">{t("list.createdColumn")}</th>
					</tr>
				</thead>
				<tbody>
					{organizations.map((org) => (
						<tr key={org.id} className="border-b border-border last:border-b-0 hover:bg-surface-alt">
							<td className="p-3 font-semibold text-text">{org.name}</td>
							<td className="p-3 font-mono text-xs text-muted">{org.code}</td>
							<td className="p-3">
								<span
									className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${
										org.active ? "bg-success-bg text-success" : "bg-surface-alt text-muted"
									}`}
								>
									{org.active ? t("status.active") : t("status.inactive")}
								</span>
							</td>
							<td className="whitespace-nowrap p-3 text-xs text-muted">{formatAbsoluteDate(org.createdAt)}</td>
						</tr>
					))}
				</tbody>
			</table>
		</div>
	);
}
