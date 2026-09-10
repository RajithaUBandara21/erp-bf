import { getLocale, getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { fetchMyPermissions } from "@/lib/permissions";
import type { OrganizationListView } from "@/types/organizations";
import { OrganizationList } from "@/components/settings/OrganizationList";

export default async function OrganizationsPage() {
	const [result, , t] = await Promise.all([
		authedFetch<OrganizationListView>("/api/organizations"),
		fetchMyPermissions(),
		getTranslations("settings.organizations"),
	]);

	if (!result.success && "unauthorized" in result) {
		redirect({ href: "/sign-in", locale: await getLocale() });
	}

	const forbidden = !result.success && result.status === 403;

	return (
		<div>
			<div className="mb-6">
				<h1 className="text-xl font-semibold">{t("title")}</h1>
				<p className="mt-1 text-[13px] text-muted">{t("intro")}</p>
			</div>

			{result.success ? (
				<>
					<p className="mb-4 text-[13px] text-muted">
						{t("count", { count: result.data.organizations.length, max: result.data.maxOrganizations })}
						{" · "}
						{result.data.plan ? t("planLabel", { plan: result.data.plan }) : t("noPlan")}
					</p>
					<OrganizationList organizations={result.data.organizations} />
				</>
			) : (
				<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
					{forbidden ? t("forbidden") : t("loadError")}
				</div>
			)}
		</div>
	);
}
