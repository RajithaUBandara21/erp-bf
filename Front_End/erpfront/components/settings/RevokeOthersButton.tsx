"use client";

import { useActionState } from "react";
import { revokeOtherSessions, type RevokeState } from "@/actions/sessions";

const initialState: RevokeState = {};

// i18n key (build-plan 4): revoke_all_others
export function RevokeOthersButton() {
	const [state, formAction, pending] = useActionState(revokeOtherSessions, initialState);

	return (
		<form action={formAction} className="flex flex-col items-end gap-1">
			<button
				type="submit"
				disabled={pending}
				className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-danger hover:border-danger hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-55"
			>
				{pending ? "Revoking..." : "Revoke all other sessions"}
			</button>
			{state.error && <span className="text-[11px] text-danger">{state.error}</span>}
		</form>
	);
}
