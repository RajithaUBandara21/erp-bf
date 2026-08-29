"use client";

import { useActionState } from "react";
import { connectGoogle, disconnectGoogle, type OAuthActionState } from "@/actions/oauth";
import type { GoogleLinkStatus } from "@/types/oauth";

const initialState: OAuthActionState = {};

// i18n keys (build-plan 4): google_account_title, google_account_intro, connect, disconnect, connected_as
export function GoogleAccountCard({ status }: { status: GoogleLinkStatus }) {
	const [connectState, connectAction, connectPending] = useActionState(connectGoogle, initialState);
	const [disconnectState, disconnectAction, disconnectPending] = useActionState(disconnectGoogle, initialState);

	return (
		<div className="rounded-lg border border-border bg-surface p-6 shadow-sm">
			<h2 className="text-sm font-semibold">Google account</h2>
			<p className="mt-1 text-xs text-muted">
				{status.linked ? `Connected as ${status.linkedEmail}` : "Not connected"}
			</p>

			<div className="mt-4">
				{status.linked ? (
					<form action={disconnectAction} className="flex flex-col items-start gap-1">
						<button
							type="submit"
							disabled={disconnectPending}
							className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-danger hover:border-danger hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-55"
						>
							{disconnectPending ? "Disconnecting..." : "Disconnect"}
						</button>
						{disconnectState.error && <span className="text-[11px] text-danger">{disconnectState.error}</span>}
					</form>
				) : (
					<form action={connectAction} className="flex flex-col items-start gap-1">
						<button
							type="submit"
							disabled={connectPending}
							className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-55"
						>
							{connectPending ? "Connecting..." : "Connect"}
						</button>
						{connectState.error && <span className="text-[11px] text-danger">{connectState.error}</span>}
					</form>
				)}
			</div>
		</div>
	);
}
