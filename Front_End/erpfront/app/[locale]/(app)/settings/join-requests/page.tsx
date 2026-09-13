import { getLocale, getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { fetchMyPermissions } from "@/lib/permissions";
import type { AuthedResult } from "@/types/auth";
import type { InviteCode, PendingJoinRequest } from "@/types/organizations";
import type { RoleSummary } from "@/types/roles";
import { InviteCodePanel } from "@/components/settings/InviteCodePanel";
import { JoinRequestList } from "@/components/settings/JoinRequestList";

function isUnauthorized(result: AuthedResult<unknown>): boolean {
	return !result.success && "unauthorized" in result;
}

function isForbidden(result: AuthedResult<unknown>): boolean {
	return !result.success && "status" in result && result.status === 403;
}

export default async function JoinRequestsPage() {
	const [inviteCodeResult, requestsResult, rolesResult, , t] = await Promise.all([
		authedFetch<InviteCode>("/api/organizations/invite-code"),
		authedFetch<PendingJoinRequest[]>("/api/organizations/join-requests"),
		authedFetch<RoleSummary[]>("/api/roles"),
		fetchMyPermissions(),
		getTranslations("settings.joinRequests"),
	]);

	if (isUnauthorized(inviteCodeResult) || isUnauthorized(requestsResult) || isUnauthorized(rolesResult)) {
		redirect({ href: "/sign-in", locale: await getLocale() });
	}

	const inviteCodeForbidden = isForbidden(inviteCodeResult);
	// The roles fetch only backs the approve picker - a 403 on either request/role call hides the whole queue section.
	const queueForbidden = isForbidden(requestsResult) || isForbidden(rolesResult);

	return (
		<div>
			<div className="mb-6">
				<h1 className="text-xl font-semibold">{t("title")}</h1>
				<p className="mt-1 text-[13px] text-muted">{t("intro")}</p>
			</div>

			<div className="flex flex-col gap-6">
				{inviteCodeResult.success ? (
					<InviteCodePanel inviteCode={inviteCodeResult.data.inviteCode} />
				) : (
					<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
						{inviteCodeForbidden ? t("forbidden") : t("loadError")}
					</div>
				)}

				<section>
					<h2 className="mb-3 text-sm font-semibold">{t("queue.heading")}</h2>
					{requestsResult.success && rolesResult.success ? (
						<JoinRequestList requests={requestsResult.data} roles={rolesResult.data} />
					) : (
						<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
							{queueForbidden ? t("queue.forbidden") : t("loadError")}
						</div>
					)}
				</section>
			</div>
		</div>
	);
}
