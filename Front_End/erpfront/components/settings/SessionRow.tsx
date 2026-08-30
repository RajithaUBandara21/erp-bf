import { getTranslations } from "next-intl/server";
import type { SessionSummary } from "@/types/session";
import { clientTypeIcon, clientTypeLabelKey } from "@/components/settings/clientType";
import { RevokeButton } from "@/components/settings/RevokeButton";
import { formatAbsoluteDate, formatRelativeTime } from "@/lib/format-time";

export async function SessionRow({ session }: { session: SessionSummary }) {
	const t = await getTranslations("settings");
	const icon = clientTypeIcon(session.clientType);
	const label = t(`clientType.${clientTypeLabelKey(session.clientType)}`);

	return (
		<li className="flex items-center gap-4 p-4">
			<span
				className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg ${
					session.current ? "bg-accent text-accent-ink" : "bg-surface-alt text-muted"
				}`}
			>
				{icon}
			</span>

			<div className="min-w-0 flex-1">
				<div className="mb-0.5 flex flex-wrap items-center gap-2 font-semibold">
					{label}
					{session.current && (
						<span className="inline-flex items-center rounded-full bg-success-bg px-2.5 py-0.5 text-[11px] font-semibold text-success">
							{t("sessions.thisDevice")}
						</span>
					)}
				</div>
				<div className="flex flex-wrap gap-x-2.5 gap-y-1 text-xs text-muted">
					<span>{t("sessions.signedIn", { date: formatAbsoluteDate(session.createdAt) })}</span>
					<span className="text-faint" aria-hidden>
						&middot;
					</span>
					<span>{t("sessions.lastActive", { time: formatRelativeTime(session.lastUsedAt) })}</span>
				</div>
			</div>

			{!session.current && (
				<div className="flex-shrink-0">
					<RevokeButton sessionId={session.id} />
				</div>
			)}
		</li>
	);
}
