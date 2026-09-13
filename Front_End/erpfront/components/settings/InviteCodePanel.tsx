"use client";

import { useEffect, useState, useTransition } from "react";
import { useLocale, useTranslations } from "next-intl";
import { rotateInviteCode } from "@/actions/join-requests";

/** View/copy/rotate the Organization's self-join invite code. Rotate is destructive to anyone still holding the old code, so it sits behind a confirm step (mirrors CreateOrganizationForm's open/confirm/cancel). */
export function InviteCodePanel({ inviteCode }: { inviteCode: string }) {
	const locale = useLocale();
	const t = useTranslations("settings.joinRequests");
	const [code, setCode] = useState(inviteCode);
	const [confirming, setConfirming] = useState(false);
	const [copied, setCopied] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [pending, startTransition] = useTransition();

	useEffect(() => {
		if (!copied) return;
		const timer = setTimeout(() => setCopied(false), 2000);
		return () => clearTimeout(timer);
	}, [copied]);

	function onCopy() {
		navigator.clipboard
			.writeText(code)
			.then(() => setCopied(true))
			.catch(() => setError(t("inviteCode.copyError")));
	}

	function onRotate() {
		setError(null);
		startTransition(async () => {
			const result = await rotateInviteCode(locale);
			if (result.error) {
				setError(result.error);
			} else if (result.inviteCode) {
				setCode(result.inviteCode);
			}
			setConfirming(false);
		});
	}

	return (
		<section className="rounded-lg border border-border bg-surface p-5 shadow-sm">
			<h2 className="mb-1 text-sm font-semibold">{t("inviteCode.heading")}</h2>
			<p className="mb-3 text-xs text-muted">{t("inviteCode.description")}</p>

			<div className="flex items-center gap-2">
				<code className="min-h-9 flex-1 rounded-[5px] border border-border bg-bg px-2.5 py-2 text-[13px] text-text">
					{code}
				</code>
				<button
					type="button"
					onClick={onCopy}
					className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt"
				>
					{copied ? t("inviteCode.copied") : t("inviteCode.copy")}
				</button>
			</div>

			{error && <p className="mt-2 text-[11px] text-danger">{error}</p>}

			<div className="mt-3 flex items-center justify-end gap-2 border-t border-border pt-3">
				{confirming ? (
					<>
						<span className="mr-auto text-[11px] text-muted">{t("inviteCode.rotateConfirmMessage")}</span>
						<button
							type="button"
							onClick={() => setConfirming(false)}
							disabled={pending}
							className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-55"
						>
							{t("inviteCode.cancel")}
						</button>
						<button
							type="button"
							onClick={onRotate}
							disabled={pending}
							className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-55"
						>
							{pending ? t("inviteCode.rotating") : t("inviteCode.rotateConfirmButton")}
						</button>
					</>
				) : (
					<button
						type="button"
						onClick={() => setConfirming(true)}
						className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt"
					>
						{t("inviteCode.rotate")}
					</button>
				)}
			</div>
		</section>
	);
}
