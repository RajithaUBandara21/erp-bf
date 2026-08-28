import Link from "next/link";

// i18n keys (build-plan 4): settings_title, settings_intro, sessions_title, sessions_intro
// Auth is enforced by the settings layout (hasSession) and proxy.ts; this page holds no per-user data.
// The device count moves to /settings/sessions so the list is fetched once, not on every settings visit
// (build-plan 3 adds a shared cached session loader).
export default function SettingsPage() {
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
				<p className="mt-1 text-xs text-muted">Devices currently signed in to your account.</p>
			</Link>
		</div>
	);
}
