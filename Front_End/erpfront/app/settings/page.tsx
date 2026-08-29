import Link from "next/link";
import { redirect } from "next/navigation";
import { authedFetch } from "@/lib/api";
import type { GoogleLinkStatus } from "@/types/oauth";
import { GoogleAccountCard } from "@/components/settings/GoogleAccountCard";

// i18n keys (build-plan 4): settings_title, settings_intro, sessions_title, sessions_intro,
// roles_title, roles_intro, google_linked_banner
// Auth is enforced by the settings layout (hasSession) and proxy.ts, but that only checks the cookie
// is present, not that the token is still valid - a stale token still needs the sign-in redirect here.
export default async function SettingsPage({
	searchParams,
}: {
	searchParams: Promise<{ oauth?: string }>;
}) {
	const { oauth } = await searchParams;
	const result = await authedFetch<GoogleLinkStatus>("/api/auth/oauth/google");
	if (!result.success && "unauthorized" in result) {
		redirect("/sign-in");
	}
	// A non-auth failure (e.g. the backend being briefly unreachable) falls back to "not connected"
	// rather than blocking the whole settings page - this card is one section among several here.
	const googleStatus: GoogleLinkStatus = result.success ? result.data : { linked: false, linkedEmail: null };

	return (
		<div>
			<div className="mb-6">
				<h1 className="text-xl font-semibold">Settings</h1>
				<p className="mt-1 text-[13px] text-muted">These preferences apply across every module.</p>
			</div>

			{oauth === "linked" && (
				<div className="mb-4 rounded-lg border border-success bg-success-bg px-4 py-3 text-[13px] font-semibold text-success">
					Your Google account is connected.
				</div>
			)}

			<div className="flex flex-col gap-3">
				<Link
					href="/settings/sessions"
					className="block rounded-lg border border-border bg-surface p-6 shadow-sm hover:border-accent"
				>
					<h2 className="text-sm font-semibold">Active sessions</h2>
					<p className="mt-1 text-xs text-muted">Devices currently signed in to your account.</p>
				</Link>

				<Link
					href="/settings/roles"
					className="block rounded-lg border border-border bg-surface p-6 shadow-sm hover:border-accent"
				>
					<h2 className="text-sm font-semibold">Roles &amp; permissions</h2>
					<p className="mt-1 text-xs text-muted">
						Roles bundle permissions and are assigned to users across the workspace.
					</p>
				</Link>

				<GoogleAccountCard status={googleStatus} />
			</div>
		</div>
	);
}
