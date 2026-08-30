"use client";

import { useActionState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { connectGoogle, disconnectGoogle, type OAuthActionState } from "@/actions/oauth";
import type { GoogleLinkStatus } from "@/types/oauth";

const initialState: OAuthActionState = {};

export function GoogleAccountCard({ status }: { status: GoogleLinkStatus }) {
	const locale = useLocale();
	const t = useTranslations("settings.google");
	const [connectState, connectAction, connectPending] = useActionState(connectGoogle.bind(null, locale), initialState);
	const [disconnectState, disconnectAction, disconnectPending] = useActionState(
		disconnectGoogle.bind(null, locale),
		initialState,
	);

	return (
		<div className="rounded-lg border border-border bg-surface p-6 shadow-sm">
			<h2 className="text-sm font-semibold">{t("title")}</h2>
			<p className="mt-1 text-xs text-muted">
				{status.linked ? t("connectedAs", { email: status.linkedEmail ?? "" }) : t("notConnected")}
			</p>

			<div className="mt-4">
				{status.linked ? (
					<form action={disconnectAction} className="flex flex-col items-start gap-1">
						<button
							type="submit"
							disabled={disconnectPending}
							className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-danger hover:border-danger hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-55"
						>
							{disconnectPending ? t("disconnecting") : t("disconnect")}
						</button>
						{/* disconnectState.error is a backend-returned string - not translated here. */}
						{disconnectState.error && <span className="text-[11px] text-danger">{disconnectState.error}</span>}
					</form>
				) : (
					<form action={connectAction} className="flex flex-col items-start gap-1">
						<button
							type="submit"
							disabled={connectPending}
							className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-55"
						>
							{connectPending ? t("connecting") : t("connect")}
						</button>
						{/* connectState.error is a backend-returned string - not translated here. */}
						{connectState.error && <span className="text-[11px] text-danger">{connectState.error}</span>}
					</form>
				)}
			</div>
		</div>
	);
}
