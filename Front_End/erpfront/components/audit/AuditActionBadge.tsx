// Action strings are free text written by whatever module logged the event (e.g. `role.created`,
// `auth.login_failed`) - there's no fixed enum to switch on, so this classifies by keyword instead.
function badgeClass(action: string): string {
	const lower = action.toLowerCase();
	if (lower.includes("delete") || lower.includes("failed") || lower.includes("unassign") || lower.includes("revoke")) {
		return "bg-danger-bg text-danger";
	}
	if (lower.includes("update") || lower.includes("adjust")) {
		return "bg-warning-bg text-warning";
	}
	if (lower.includes("creat") || lower.includes("login") || lower.includes("assign") || lower.includes("grant")) {
		return "bg-success-bg text-success";
	}
	return "bg-surface-alt text-muted";
}

export function AuditActionBadge({ action }: { action: string }) {
	return (
		<span
			className={`inline-flex items-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${badgeClass(action)}`}
		>
			{action}
		</span>
	);
}
