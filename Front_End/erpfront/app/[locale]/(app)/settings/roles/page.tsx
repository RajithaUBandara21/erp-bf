import { getLocale, getTranslations } from "next-intl/server";
import { Link, redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { can, fetchMyPermissions } from "@/lib/permissions";
import type { RoleSummary } from "@/types/roles";
import { RoleList } from "@/components/roles/RoleList";

export default async function RolesPage() {
	const [result, perms, t] = await Promise.all([
		authedFetch<RoleSummary[]>("/api/roles"),
		fetchMyPermissions(),
		getTranslations("roles"),
	]);

	if (!result.success && "unauthorized" in result) {
		redirect({ href: "/sign-in", locale: await getLocale() });
	}

	const forbidden = !result.success && result.status === 403;

	return (
		<div>
			<div className="mb-6 flex flex-wrap items-start justify-between gap-3">
				<div>
					<h1 className="text-xl font-semibold">{t("title")}</h1>
					<p className="mt-1 text-[13px] text-muted">{t("intro")}</p>
				</div>
				{result.success && can(perms, "role.create") && (
					<Link
						href="/settings/roles/new"
						className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover"
					>
						{t("newRole")}
					</Link>
				)}
			</div>

			{result.success ? (
				<RoleList roles={result.data ?? []} />
			) : (
				<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
					{forbidden ? t("forbidden") : t("loadError")}
				</div>
			)}
		</div>
	);
}
