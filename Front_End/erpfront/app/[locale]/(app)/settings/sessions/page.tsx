import { getLocale, getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import type { SessionSummary } from "@/types/session";
import { SessionList } from "@/components/settings/SessionList";
import { RevokeOthersButton } from "@/components/settings/RevokeOthersButton";

export default async function SessionsPage() {
	const result = await authedFetch<SessionSummary[]>("/api/auth/sessions");

	if (!result.success && "unauthorized" in result) {
		redirect({ href: "/sign-in", locale: await getLocale() });
	}

	const sessions = result.success ? (result.data ?? []) : [];
	const hasOtherSessions = sessions.some((session) => !session.current);
	const t = await getTranslations("settings.sessions");

	return (
		<div>
			<div className="mb-6 flex flex-wrap items-start justify-between gap-3">
				<div>
					<h1 className="text-xl font-semibold">{t("title")}</h1>
					<p className="mt-1 text-[13px] text-muted">{t("intro")}</p>
				</div>
				{hasOtherSessions && <RevokeOthersButton />}
			</div>

			{result.success ? (
				<SessionList sessions={sessions} />
			) : (
				<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
					{t("loadError")}
				</div>
			)}
		</div>
	);
}
