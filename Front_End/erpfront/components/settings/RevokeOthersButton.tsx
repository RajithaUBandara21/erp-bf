"use client";

import { useActionState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { revokeOtherSessions, type RevokeState } from "@/actions/sessions";

const initialState: RevokeState = {};

export function RevokeOthersButton() {
	const locale = useLocale();
	const t = useTranslations("settings.sessions");
	const [state, formAction, pending] = useActionState(revokeOtherSessions.bind(null, locale), initialState);

	return (
		<form action={formAction} className="flex flex-col items-end gap-1">
			<button
				type="submit"
				disabled={pending}
				className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-danger hover:border-danger hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-55"
			>
				{pending ? t("revoking") : t("revokeAllOthers")}
			</button>
			{state.error && <span className="text-[11px] text-danger">{state.error}</span>}
		</form>
	);
}
