import Link from "next/link";
import { redirect } from "next/navigation";
import { authedFetch } from "@/lib/api";
import type { SessionSummary } from "@/types/session";

// i18n keys (build-plan 4): settings_title, settings_intro, sessions_title, sessions_intro
export default async function SettingsPage() {
	const result = await authedFetch<SessionSummary[]>("/api/auth/sessions");

	if (!result.success && "unauthorized" in result) {
		redirect("/sign-in");
	}

	const count = result.success ? (result.data?.length ?? 0) : null;
	const blurb =
		count === null
			? "Devices currently signed in to your account."
			: `${count} ${count === 1 ? "device" : "devices"} currently signed in to your account.`;

	return (
		<div>
			<div className="mb-6">
				<h1 className="text-xl font-semibold">Settings</h1>
				<p className="mt-1 text-[13px] text-muted">These preferences apply across every module.</p>
			</div>

			<Link
				href="/settings/sessions"
				className="block rounded-lg border border-border bg-surface p-6 shadow-sm hover:border-accent"
			>
				<h2 className="text-sm font-semibold">Active sessions</h2>
				<p className="mt-1 text-xs text-muted">{blurb}</p>
			</Link>
		</div>
	);
}
