"use client";

import { useState, useTransition } from "react";
import { useLocale, useTranslations } from "next-intl";
import { rejectJoinRequest } from "@/actions/join-requests";
import type { PendingJoinRequest } from "@/types/organizations";
import type { RoleSummary } from "@/types/roles";
import { formatAbsoluteDate } from "@/lib/format-time";
import { ApproveJoinRequestForm } from "@/components/settings/ApproveJoinRequestForm";

interface JoinRequestListProps {
	requests: PendingJoinRequest[];
	roles: RoleSummary[];
}

/** The pending self-join queue: name/email/requested-at per row, with an Approve (role picker) and Reject action. */
export function JoinRequestList({ requests, roles }: JoinRequestListProps) {
	const t = useTranslations("settings.joinRequests");

	if (requests.length === 0) {
		return (
			<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
				{t("queue.empty")}
			</div>
		);
	}

	return (
		<div className="overflow-x-auto rounded-lg border border-border bg-surface shadow-sm">
			<table className="w-full min-w-[640px] border-collapse text-[13px]">
				<thead>
					<tr className="border-b border-border bg-surface-alt text-left text-[11px] font-bold uppercase tracking-wide text-muted">
						<th className="p-3">{t("queue.nameColumn")}</th>
						<th className="p-3">{t("queue.emailColumn")}</th>
						<th className="p-3">{t("queue.requestedColumn")}</th>
						<th className="p-3 text-right">{t("queue.actionsColumn")}</th>
					</tr>
				</thead>
				<tbody>
					{requests.map((request) => (
						<JoinRequestRow key={request.membershipId} request={request} roles={roles} />
					))}
				</tbody>
			</table>
		</div>
	);
}

function JoinRequestRow({ request, roles }: { request: PendingJoinRequest; roles: RoleSummary[] }) {
	const locale = useLocale();
	const t = useTranslations("settings.joinRequests");
	const [error, setError] = useState<string | null>(null);
	const [pending, startTransition] = useTransition();

	function onReject() {
		setError(null);
		startTransition(async () => {
			const result = await rejectJoinRequest(locale, request.membershipId);
			if (result.error) {
				setError(result.error);
			}
		});
	}

	return (
		<tr className="border-b border-border align-top last:border-b-0 hover:bg-surface-alt">
			<td className="p-3 font-semibold text-text">{request.fullName}</td>
			<td className="p-3 text-xs text-muted">{request.email}</td>
			<td className="whitespace-nowrap p-3 text-xs text-muted">{formatAbsoluteDate(request.requestedAt)}</td>
			<td className="p-3">
				<div className="flex flex-col items-end gap-1.5">
					<div className="flex items-center gap-2">
						<button
							type="button"
							onClick={onReject}
							disabled={pending}
							className="min-h-9 rounded-[5px] border border-border bg-surface px-2.5 py-1.5 text-xs font-semibold text-danger hover:border-danger hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-55"
						>
							{pending ? t("queue.rejecting") : t("queue.reject")}
						</button>
						<ApproveJoinRequestForm membershipId={request.membershipId} roles={roles} />
					</div>
					{error && <span className="text-[11px] text-danger">{error}</span>}
				</div>
			</td>
		</tr>
	);
}
