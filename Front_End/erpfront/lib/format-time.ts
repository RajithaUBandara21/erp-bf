// English-only for now; i18n (build-plan 4) swaps the locale arg. No date library - Intl covers it.

const RELATIVE_DIVISIONS: readonly { amount: number; unit: Intl.RelativeTimeFormatUnit }[] = [
	{ amount: 60, unit: "second" },
	{ amount: 60, unit: "minute" },
	{ amount: 24, unit: "hour" },
	{ amount: 7, unit: "day" },
	{ amount: 4.34524, unit: "week" },
	{ amount: 12, unit: "month" },
];

/** "just now" / "5 minutes ago" / "yesterday" / "in 3 days". `now` is injectable for tests. */
export function formatRelativeTime(iso: string, now: Date = new Date()): string {
	const target = new Date(iso).getTime();
	if (Number.isNaN(target)) return "unknown";

	let duration = (target - now.getTime()) / 1000;
	if (Math.abs(duration) < 45) return "just now";

	const rtf = new Intl.RelativeTimeFormat("en", { numeric: "auto" });
	for (const { amount, unit } of RELATIVE_DIVISIONS) {
		if (Math.abs(duration) < amount) return rtf.format(Math.round(duration), unit);
		duration /= amount;
	}
	return rtf.format(Math.round(duration), "year");
}

/** Fixed UTC wall-clock, e.g. "Aug 27, 2026, 2:30 PM UTC" - never assumes the server's timezone. */
export function formatAbsoluteDate(iso: string): string {
	const date = new Date(iso);
	if (Number.isNaN(date.getTime())) return "unknown";

	return new Intl.DateTimeFormat("en-US", {
		year: "numeric",
		month: "short",
		day: "numeric",
		hour: "numeric",
		minute: "2-digit",
		timeZone: "UTC",
		timeZoneName: "short",
	}).format(date);
}
