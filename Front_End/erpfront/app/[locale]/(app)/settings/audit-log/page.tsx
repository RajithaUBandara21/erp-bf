import { getLocale } from "next-intl/server";
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

// i18n keys (build-plan 4): audit_log_title, audit_log_intro, audit_log_forbidden, audit_log_load_error
export default async function AuditLogViewerPage({
	searchParams,
}: {
	searchParams: Promise<AuditLogSearchParams>;
}) {
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
				<h1 className="text-xl font-semibold">Audit logs</h1>
				<p className="mt-1 text-[13px] text-muted">
					Every recorded action across identity, security, and business data - who did what, when, and
					what changed.
				</p>
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
					{forbidden
						? "You don't have access to audit logs. Ask an administrator if you need it."
						: "We couldn't load audit logs right now. Please refresh to try again."}
				</div>
			)}
		</div>
	);
}
