"use client";

import { useState, useTransition } from "react";
import { useLocale, useTranslations } from "next-intl";
import { approveJoinRequest } from "@/actions/join-requests";
import type { RoleSummary } from "@/types/roles";

interface ApproveJoinRequestFormProps {
	membershipId: string;
	roles: RoleSummary[];
}

/** Role picker + approve action for one pending join request - activates the Membership with the chosen Role. */
export function ApproveJoinRequestForm({ membershipId, roles }: ApproveJoinRequestFormProps) {
	const locale = useLocale();
	const t = useTranslations("settings.joinRequests");
	const [roleId, setRoleId] = useState("");
	const [error, setError] = useState<string | null>(null);
	const [pending, startTransition] = useTransition();

	function onApprove() {
		setError(null);
		startTransition(async () => {
			const result = await approveJoinRequest(locale, membershipId, roleId);
			if (result.error) {
				setError(result.error);
			}
		});
	}

	return (
		<div className="flex flex-col items-end gap-1">
			<div className="flex items-center gap-2">
				<select
					value={roleId}
					onChange={(event) => setRoleId(event.target.value)}
					disabled={pending}
					className="min-h-9 rounded-[5px] border border-border bg-bg px-2.5 py-2 text-[13px] text-text focus:border-accent focus:outline-none disabled:cursor-not-allowed disabled:opacity-55"
					aria-label={t("queue.roleAria")}
				>
					<option value="">{t("queue.rolePlaceholder")}</option>
					{roles.map((role) => (
						<option key={role.id} value={role.id}>
							{role.name}
						</option>
					))}
				</select>
				<button
					type="button"
					onClick={onApprove}
					disabled={pending || !roleId}
					className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-55"
				>
					{pending ? t("queue.approving") : t("queue.approve")}
				</button>
			</div>
			{error && <span className="text-[11px] text-danger">{error}</span>}
		</div>
	);
}
