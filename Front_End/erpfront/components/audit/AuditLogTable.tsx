"use client";

import { useState } from "react";
import type { AuditLogEntry } from "@/types/audit";
import { AuditActionBadge } from "@/components/audit/AuditActionBadge";
import { AuditLogDetailPanel } from "@/components/audit/AuditLogDetailPanel";
import { formatAbsoluteDate } from "@/lib/format-time";

// i18n keys (build-plan 4): timestamp_col, actor_col, entity_col, action_col, organization_col,
// unknown_actor, audit_log_empty
export function AuditLogTable({ entries }: { entries: AuditLogEntry[] }) {
	const [selected, setSelected] = useState<AuditLogEntry | null>(null);

	if (entries.length === 0) {
		return (
			<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
				No audit log entries match your filters.
			</div>
		);
	}

	return (
		<div className="overflow-x-auto rounded-lg border border-border bg-surface shadow-sm">
			<table className="w-full min-w-205 border-collapse text-[13px]">
				<thead>
					<tr className="border-b border-border bg-surface-alt text-left text-[11px] font-bold uppercase tracking-wide text-muted">
						<th className="p-3">Timestamp</th>
						<th className="p-3">Actor</th>
						<th className="p-3">Entity</th>
						<th className="p-3">Action</th>
						<th className="p-3">Organization</th>
					</tr>
				</thead>
				<tbody>
					{entries.map((entry) => (
						<tr
							key={entry.id}
							onClick={() => setSelected(entry)}
							className="cursor-pointer border-b border-border last:border-b-0 hover:bg-surface-alt"
						>
							<td className="whitespace-nowrap p-3 text-xs text-muted">{formatAbsoluteDate(entry.createdAt)}</td>
							<td className="p-3">
								<div className="font-semibold text-text">{entry.actorName ?? "Unknown"}</div>
								{entry.actorEmail && <div className="text-xs text-muted">{entry.actorEmail}</div>}
							</td>
							<td className="p-3">
								<div className="font-semibold text-text">{entry.entityType}</div>
								{entry.entityId && <div className="font-mono text-xs text-muted">{entry.entityId}</div>}
							</td>
							<td className="p-3">
								<AuditActionBadge action={entry.action} />
							</td>
							<td className="p-3 text-xs text-muted">{entry.organizationName ?? "-"}</td>
						</tr>
					))}
				</tbody>
			</table>

			<AuditLogDetailPanel entry={selected} onClose={() => setSelected(null)} />
		</div>
	);
}
