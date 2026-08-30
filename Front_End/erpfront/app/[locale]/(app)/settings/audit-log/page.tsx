import { getLocale, getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { buildAuditLogQuery } from "@/lib/audit-log";
import type { AuditLogPage, AuditLogQueryParams } from "@/types/audit";
import type { UserSummary } from "@/types/roles";
import { AuditLogFilters, type AuditLogFilterValues } from "@/components/audit/AuditLogFilters";
import { AuditLogPagination } from "@/components/audit/AuditLogPagination";
import { AuditLogTable } from "@/components/audit/AuditLogTable";

interface AuditLogSearchParams {
	entityType?: string;
	action?: string;
	actorId?: string;
	from?: string;
	to?: string;
	page?: string;
}

export default async function AuditLogViewerPage({
	searchParams,
}: {
	searchParams: Promise<AuditLogSearchParams>;
}) {
	const t = await getTranslations("auditLog");
	const params = await searchParams;
	const requestedPage = Number(params.page);
	const page = Number.isInteger(requestedPage) && requestedPage >= 0 ? requestedPage : undefined;
	const filters: AuditLogQueryParams = {
		entityType: params.entityType,
		action: params.action,
		actorId: params.actorId,
		from: params.from,
		to: params.to,
	};
	const query: AuditLogQueryParams = { ...filters, page };

	const [result, usersResult] = await Promise.all([
		authedFetch<AuditLogPage>(`/api/audit-logs${buildAuditLogQuery(query)}`),
		authedFetch<UserSummary[]>("/api/users"),
	]);

	if (!result.success && "unauthorized" in result) {
		redirect({ href: "/sign-in", locale: await getLocale() });
	}

	const forbidden = !result.success && result.status === 403;
	// /api/users needs `user.view`; a 403/error just means the actor filter has no options, not a page failure.
	const actors = usersResult.success ? usersResult.data : [];
	const filterValues: AuditLogFilterValues = {
		entityType: params.entityType ?? "",
		action: params.action ?? "",
		actorId: params.actorId ?? "",
		from: params.from ? params.from.slice(0, 10) : "",
		to: params.to ? params.to.slice(0, 10) : "",
	};

	return (
		<div>
			<div className="mb-6">
				<h1 className="text-xl font-semibold">{t("title")}</h1>
				<p className="mt-1 text-[13px] text-muted">{t("intro")}</p>
			</div>

			{result.success && <AuditLogFilters initialFilters={filterValues} actors={actors} />}

			{result.success ? (
				<>
					<AuditLogTable entries={result.data.content} />
					<AuditLogPagination
						page={result.data.page}
						size={result.data.size}
						totalElements={result.data.totalElements}
						currentCount={result.data.content.length}
						filters={filters}
					/>
				</>
			) : (
				<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
					{forbidden ? t("forbidden") : t("loadError")}
				</div>
			)}
		</div>
	);
}
