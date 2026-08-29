"use client";

import { useEffect } from "react";
import type { AuditLogEntry } from "@/types/audit";
import { computeDiff, type DiffFieldStatus } from "@/lib/audit-log";
import { formatAbsoluteDate } from "@/lib/format-time";

// i18n keys (build-plan 4): change_details, field_col, before_label, after_label, no_fields_recorded
export function AuditLogDetailPanel({ entry, onClose }: { entry: AuditLogEntry | null; onClose: () => void }) {
	useEffect(() => {
		if (!entry) return;
		function onKeyDown(event: KeyboardEvent) {
			if (event.key === "Escape") onClose();
		}
		document.addEventListener("keydown", onKeyDown);
		return () => document.removeEventListener("keydown", onKeyDown);
	}, [entry, onClose]);

	if (!entry) return null;

	const diff = computeDiff(entry.beforeValue, entry.afterValue);

	return (
		<>
			<div className="fixed inset-0 z-20 bg-black/40" onClick={onClose} aria-hidden />
			<aside
				className="fixed inset-y-0 right-0 z-21 flex w-full max-w-[460px] flex-col border-l border-border bg-surface shadow-lg"
				aria-label="Audit entry details"
			>
				<div className="flex items-start justify-between gap-3 border-b border-border p-4">
					<div>
						<h2 className="text-sm font-semibold">Change details</h2>
						<div className="mt-1 text-xs text-muted">
							{entry.action} &middot; {entry.entityType} {entry.entityId ?? ""}
						</div>
					</div>
					<button
						type="button"
						onClick={onClose}
						aria-label="Close"
						className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-[5px] border border-border bg-surface text-text hover:bg-surface-alt"
					>
						&times;
					</button>
				</div>

				<div className="flex-1 overflow-y-auto p-4">
					<ul className="mb-4 flex flex-col gap-2 text-xs text-muted">
						<MetaRow label="Timestamp" value={formatAbsoluteDate(entry.createdAt)} />
						<MetaRow label="Actor" value={`${entry.actorName ?? "Unknown"} (${entry.actorEmail ?? "-"})`} />
						<MetaRow label="Entity" value={`${entry.entityType} - ${entry.entityId ?? "-"}`} />
						<MetaRow label="Action" value={entry.action} mono />
						<MetaRow label="Organization" value={entry.organizationName ?? "-"} />
					</ul>

					{diff.length === 0 ? (
						<p className="text-xs italic text-faint">No fields recorded for this action.</p>
					) : (
						<table className="w-full border-collapse text-xs">
							<thead>
								<tr className="border-b border-border bg-surface-alt text-left text-[10px] font-bold uppercase tracking-wide text-muted">
									<th className="p-2">Field</th>
									<th className="p-2">Before</th>
									<th className="p-2">After</th>
								</tr>
							</thead>
							<tbody>
								{diff.map((field) => (
									<tr key={field.key} className="border-b border-border last:border-b-0">
										<td className="p-2 font-semibold">{field.key}</td>
										<td className="p-2">
											<DiffValue value={field.oldValue} status={field.status} side="old" />
										</td>
										<td className="p-2">
											<DiffValue value={field.newValue} status={field.status} side="new" />
										</td>
									</tr>
								))}
							</tbody>
						</table>
					)}
				</div>
			</aside>
		</>
	);
}

function MetaRow({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
	return (
		<li>
			<span className="inline-block min-w-[108px] text-faint">{label}</span>
			<span className={mono ? "font-mono text-text" : "text-text"}>{value}</span>
		</li>
	);
}

function DiffValue({ value, status, side }: { value: unknown; status: DiffFieldStatus; side: "old" | "new" }) {
	if (value === undefined || value === null || value === "") {
		return <span className="italic text-faint">-</span>;
	}
	const text = String(value);
	if (status === "unchanged") return <span className="text-muted">{text}</span>;
	if (side === "old" && (status === "changed" || status === "removed")) {
		return <span className="rounded bg-danger-bg px-1.5 py-0.5 text-danger line-through">{text}</span>;
	}
	if (side === "new" && (status === "changed" || status === "added")) {
		return <span className="rounded bg-success-bg px-1.5 py-0.5 text-success">{text}</span>;
	}
	return <span className="text-muted">{text}</span>;
}
