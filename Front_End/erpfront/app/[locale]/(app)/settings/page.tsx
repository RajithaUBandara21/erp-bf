import { getLocale, getTranslations } from "next-intl/server";
import { Link, redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { can, fetchMyPermissions } from "@/lib/permissions";
import type { GoogleLinkStatus } from "@/types/oauth";
import { GoogleAccountCard } from "@/components/settings/GoogleAccountCard";

// Auth is enforced by the settings layout (hasSession) and proxy.ts, but that only checks the cookie
// is present, not that the token is still valid - a stale token still needs the sign-in redirect here.
export default async function SettingsPage({
	searchParams,
}: {
	searchParams: Promise<{ oauth?: string }>;
}) {
	const { oauth } = await searchParams;
	const [result, perms] = await Promise.all([
		authedFetch<GoogleLinkStatus>("/api/auth/oauth/google"),
		fetchMyPermissions(),
	]);
	if (!result.success && "unauthorized" in result) {
		redirect({ href: "/sign-in", locale: await getLocale() });
	}
	// A non-auth failure (e.g. the backend being briefly unreachable) falls back to "not connected"
	// rather than blocking the whole settings page - this card is one section among several here.
	const googleStatus: GoogleLinkStatus = result.success ? result.data : { linked: false, linkedEmail: null };
	const t = await getTranslations("settings");

	return (
		<div>
			<div className="mb-6">
				<h1 className="text-xl font-semibold">{t("title")}</h1>
				<p className="mt-1 text-[13px] text-muted">{t("intro")}</p>
			</div>

			{oauth === "linked" && (
				<div className="mb-4 rounded-lg border border-success bg-success-bg px-4 py-3 text-[13px] font-semibold text-success">
					{t("googleLinkedBanner")}
				</div>
			)}

			<div className="flex flex-col gap-3">
				<Link
					href="/settings/sessions"
					className="block rounded-lg border border-border bg-surface p-6 shadow-sm hover:border-accent"
				>
					<h2 className="text-sm font-semibold">{t("sessionsTile.title")}</h2>
					<p className="mt-1 text-xs text-muted">{t("sessionsTile.description")}</p>
				</Link>

				<Link
					href="/settings/roles"
					className="block rounded-lg border border-border bg-surface p-6 shadow-sm hover:border-accent"
				>
					<h2 className="text-sm font-semibold">{t("rolesTile.title")}</h2>
					<p className="mt-1 text-xs text-muted">{t("rolesTile.description")}</p>
				</Link>

				{can(perms, "audit.view") && (
					<Link
						href="/settings/audit-log"
						className="block rounded-lg border border-border bg-surface p-6 shadow-sm hover:border-accent"
					>
						<h2 className="text-sm font-semibold">{t("auditLogTile.title")}</h2>
						<p className="mt-1 text-xs text-muted">{t("auditLogTile.description")}</p>
					</Link>
				)}

				<GoogleAccountCard status={googleStatus} />
			</div>
		</div>
	);
}
