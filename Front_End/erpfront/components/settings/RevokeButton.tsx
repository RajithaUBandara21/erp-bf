"use client";

import { useActionState } from "react";
import { useLocale } from "next-intl";
import { revokeSession, type RevokeState } from "@/actions/sessions";

const initialState: RevokeState = {};

// i18n key (build-plan 4): revoke
export function RevokeButton({ sessionId }: { sessionId: string }) {
	const locale = useLocale();
	const [state, formAction, pending] = useActionState(revokeSession.bind(null, locale), initialState);

	return (
		<form action={formAction} className="flex flex-col items-end gap-1">
			<input type="hidden" name="sessionId" value={sessionId} />
			<button
				type="submit"
				disabled={pending}
				className="min-h-9 rounded-[5px] border border-border bg-surface px-2.5 py-1.5 text-xs font-semibold text-text hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-55"
			>
				{pending ? "Revoking..." : "Revoke"}
			</button>
			{state.error && <span className="text-[11px] text-danger">{state.error}</span>}
		</form>
	);
}
