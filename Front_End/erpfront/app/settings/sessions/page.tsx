import { redirect } from "next/navigation";
import { authedFetch } from "@/lib/api";
import type { SessionSummary } from "@/types/session";
import { SessionList } from "@/components/settings/SessionList";
import { RevokeOthersButton } from "@/components/settings/RevokeOthersButton";

// i18n keys (build-plan 4): sessions_title, sessions_intro
export default async function SessionsPage() {
	const result = await authedFetch<SessionSummary[]>("/api/auth/sessions");

	if (!result.success && "unauthorized" in result) {
		redirect("/sign-in");
	}

	const sessions = result.success ? (result.data ?? []) : [];
	const hasOtherSessions = sessions.some((session) => !session.current);

	return (
		<div>
			<div className="mb-6 flex flex-wrap items-start justify-between gap-3">
				<div>
					<h1 className="text-xl font-semibold">Active sessions</h1>
					<p className="mt-1 text-[13px] text-muted">Devices currently signed in to your account.</p>
				</div>
				{hasOtherSessions && <RevokeOthersButton />}
			</div>

			{result.success ? (
				<SessionList sessions={sessions} />
			) : (
				<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
					We couldn&apos;t load your sessions right now. Please refresh to try again.
				</div>
			)}
		</div>
	);
}
