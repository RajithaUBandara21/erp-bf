"use client";

import { useState, useTransition } from "react";
import { useLocale } from "next-intl";
import { deleteRole } from "@/actions/roles";

// i18n keys (build-plan 4): delete_role, deleting, delete_role_confirm

/** Deletes a custom role after a confirm. Success redirects to the list; a 403/409 shows inline. */
export function DeleteRoleButton({ roleId }: { roleId: string }) {
	const locale = useLocale();
	const [error, setError] = useState<string | null>(null);
	const [pending, startTransition] = useTransition();

	function onClick() {
		if (!window.confirm("Delete this role? Users lose its permissions immediately. This cannot be undone.")) {
			return;
		}
		setError(null);
		startTransition(async () => {
			// deleteRole redirects on success and never resolves; only a failure returns here.
			const result = await deleteRole(locale, roleId);
			if (result?.error) setError(result.error);
		});
	}

	return (
		<div className="flex flex-col items-end gap-1">
			<button
				type="button"
				onClick={onClick}
				disabled={pending}
				className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-danger hover:border-danger hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-55"
			>
				{pending ? "Deleting..." : "Delete role"}
			</button>
			{error && <span className="max-w-[220px] text-right text-[11px] text-danger">{error}</span>}
		</div>
	);
}
