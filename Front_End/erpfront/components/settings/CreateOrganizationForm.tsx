"use client";

import { useActionState, useEffect, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { createOrganization, type CreateOrganizationState } from "@/actions/organizations";

const initialState: CreateOrganizationState = {};

/** Name-only create control, gated by the Tenant's `maxOrganizations` limit. */
export function CreateOrganizationForm({ atLimit }: { atLimit: boolean }) {
	const t = useTranslations("settings.organizations");
	const [open, setOpen] = useState(false);
	const [confirmed, setConfirmed] = useState(false);

	if (atLimit) {
		return <p className="text-[13px] text-muted">{t("atLimitNote")}</p>;
	}

	if (!open) {
		return (
			<div className="flex items-center justify-end gap-2">
				{confirmed && <span className="text-[13px] font-semibold text-success">{t("created")}</span>}
				<button
					type="button"
					onClick={() => {
						setConfirmed(false);
						setOpen(true);
					}}
					className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover"
				>
					{t("newOrganization")}
				</button>
			</div>
		);
	}

	return (
		<CreatePanel
			onCancel={() => setOpen(false)}
			onCreated={() => {
				setOpen(false);
				setConfirmed(true);
			}}
		/>
	);
}

// A fresh instance each time the panel opens (the parent swaps branches), so useActionState always
// starts from initialState - no manual reset needed for a stale error or field value from a prior open.
function CreatePanel({ onCancel, onCreated }: { onCancel: () => void; onCreated: () => void }) {
	const locale = useLocale();
	const t = useTranslations("settings.organizations");
	const [state, formAction, pending] = useActionState(createOrganization.bind(null, locale), initialState);

	useEffect(() => {
		if (state.created) {
			onCreated();
		}
		// onCreated is a fresh closure every render; only state.created transitioning should re-fire this.
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [state]);

	return (
		<form action={formAction} className="mt-3 flex flex-col gap-3 rounded-lg border border-border bg-surface p-4 shadow-sm">
			<div className="flex flex-col gap-1.5">
				<label htmlFor="org-name" className="text-xs font-semibold text-muted">
					{t("form.nameLabel")}
				</label>
				<input
					id="org-name"
					name="name"
					maxLength={255}
					placeholder={t("form.namePlaceholder")}
					className={`min-h-9 rounded-[5px] border bg-bg px-2.5 py-2 text-[13px] text-text focus:border-accent focus:outline-none ${
						state.fieldErrors?.name ? "border-danger" : "border-border"
					}`}
				/>
				{state.fieldErrors?.name && <span className="text-[11px] text-danger">{state.fieldErrors.name}</span>}
			</div>
			{/* state.error is a backend-returned string - not translated here. */}
			{state.error && <span className="text-[11px] text-danger">{state.error}</span>}
			<div className="flex items-center justify-end gap-2">
				<button
					type="button"
					onClick={onCancel}
					className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt"
				>
					{t("form.cancel")}
				</button>
				<button
					type="submit"
					disabled={pending}
					className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-55"
				>
					{pending ? t("form.creating") : t("form.create")}
				</button>
			</div>
		</form>
	);
}
