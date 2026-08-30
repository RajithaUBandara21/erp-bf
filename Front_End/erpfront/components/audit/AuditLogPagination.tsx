import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import type { AuditLogQueryParams } from "@/types/audit";
import { buildAuditLogQuery } from "@/lib/audit-log";

export async function AuditLogPagination({
	page,
	size,
	totalElements,
	currentCount,
	filters,
}: {
	page: number;
	size: number;
	totalElements: number;
	currentCount: number;
	filters: AuditLogQueryParams;
}) {
	if (totalElements === 0) return null;

	const t = await getTranslations("auditLog.pagination");

	const rangeStart = page * size + 1;
	const rangeEnd = page * size + currentCount;
	const hasPrevious = page > 0;
	const hasNext = rangeEnd < totalElements;

	function href(targetPage: number): string {
		return `/settings/audit-log${buildAuditLogQuery({ ...filters, page: targetPage })}`;
	}

	return (
		<div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-[13px]">
			<span className="text-muted">
				{t("showing", { start: rangeStart, end: rangeEnd, total: totalElements })}
			</span>
			<div className="flex gap-2">
				<PageLink href={href(page - 1)} disabled={!hasPrevious}>
					{t("previous")}
				</PageLink>
				<PageLink href={href(page + 1)} disabled={!hasNext}>
					{t("next")}
				</PageLink>
			</div>
		</div>
	);
}

function PageLink({ href, disabled, children }: { href: string; disabled: boolean; children: string }) {
	if (disabled) {
		return (
			<span className="min-h-9 cursor-not-allowed rounded-[5px] border border-border px-3.5 py-1.5 font-semibold text-faint">
				{children}
			</span>
		);
	}
	return (
		<Link
			href={href}
			className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-1.5 font-semibold text-text hover:bg-surface-alt"
		>
			{children}
		</Link>
	);
}
