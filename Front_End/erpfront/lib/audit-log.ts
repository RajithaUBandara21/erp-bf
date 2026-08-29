import type { AuditLogQueryParams } from "@/types/audit";

/** `AuditLogQueryParams` -> a `GET /api/audit-logs` query string (including the leading `?`, or
 * `""` when every field is empty). Blank strings and `undefined` are omitted rather than sent as
 * empty filters, since the backend treats an absent param and an empty one differently for `page`/`size`. */
export function buildAuditLogQuery(params: AuditLogQueryParams): string {
	const search = new URLSearchParams();
	for (const [key, value] of Object.entries(params)) {
		if (value === undefined || value === null) continue;
		if (typeof value === "string" && value.trim() === "") continue;
		search.set(key, String(value));
	}
	const query = search.toString();
	return query ? `?${query}` : "";
}

export type DiffFieldStatus = "added" | "removed" | "changed" | "unchanged";

export interface DiffField {
	key: string;
	oldValue: unknown;
	newValue: unknown;
	status: DiffFieldStatus;
}

/** Flattens one level of before/after snapshot keys into a field-by-field diff, since audit
 * snapshots are plain objects, not deeply nested documents. `null` for a side means that side
 * wasn't recorded at all (e.g. `before` is `null` for a create), not that every field was removed. */
export function computeDiff(
	before: Record<string, unknown> | null,
	after: Record<string, unknown> | null,
): DiffField[] {
	const keys = new Set([...Object.keys(before ?? {}), ...Object.keys(after ?? {})]);
	return [...keys].sort().map((key) => {
		const hasOld = before !== null && key in before;
		const hasNew = after !== null && key in after;
		const oldValue = hasOld ? before[key] : undefined;
		const newValue = hasNew ? after[key] : undefined;

		let status: DiffFieldStatus;
		if (!hasOld) status = "added";
		else if (!hasNew) status = "removed";
		else status = String(oldValue) === String(newValue) ? "unchanged" : "changed";

		return { key, oldValue, newValue, status };
	});
}
