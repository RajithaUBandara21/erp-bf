"use client";

import { useActionState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { join, type JoinFormState } from "@/actions/auth";

const initialState: JoinFormState = {};

export function JoinForm() {
	const locale = useLocale();
	const t = useTranslations("auth.join");
	const [state, formAction, pending] = useActionState(join.bind(null, locale), initialState);

	if (state.submitted) {
		return (
			<div className="flex flex-col gap-2 rounded-[5px] bg-success-bg px-4 py-4 text-[13px]">
				<p className="font-semibold text-success">{t("checkEmailTitle")}</p>
				<p className="text-text">{t("checkEmailBody")}</p>
				<p className="text-muted">{t("awaitingApprovalNote")}</p>
			</div>
		);
	}

	return (
		<form action={formAction} noValidate>
			{state.error && (
				<div className="mb-4 flex items-start gap-2 rounded-[5px] bg-danger-bg px-3 py-2.5 text-xs text-danger">
					<span>{state.error}</span>
				</div>
			)}

			<div className="flex flex-col gap-4">
				<Field
					id="fullName"
					name="fullName"
					label={t("fullNameLabel")}
					type="text"
					autoComplete="name"
					placeholder={t("fullNamePlaceholder")}
					defaultValue={state.values?.fullName}
					error={state.fieldErrors?.fullName}
				/>
				<Field
					id="email"
					name="email"
					label={t("emailLabel")}
					type="email"
					autoComplete="email"
					placeholder={t("emailPlaceholder")}
					defaultValue={state.values?.email}
					error={state.fieldErrors?.email}
				/>
				<Field
					id="password"
					name="password"
					label={t("passwordLabel")}
					type="password"
					autoComplete="new-password"
					hint={t("passwordHint")}
					error={state.fieldErrors?.password}
				/>
				<Field
					id="confirmPassword"
					name="confirmPassword"
					label={t("confirmPasswordLabel")}
					type="password"
					autoComplete="new-password"
					error={state.fieldErrors?.confirmPassword}
				/>
				<Field
					id="inviteCode"
					name="inviteCode"
					label={t("inviteCodeLabel")}
					type="text"
					autoComplete="off"
					placeholder={t("inviteCodePlaceholder")}
					hint={t("inviteCodeHint")}
					defaultValue={state.values?.inviteCode}
					error={state.fieldErrors?.inviteCode}
				/>
			</div>

			<button
				type="submit"
				disabled={pending}
				className="mt-4 min-h-11 w-full rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink transition-colors hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-55"
			>
				{pending ? t("submitting") : t("submit")}
			</button>
		</form>
	);
}

interface FieldProps {
	id: string;
	name: string;
	label: string;
	type: string;
	autoComplete: string;
	placeholder?: string;
	defaultValue?: string;
	hint?: string;
	error?: string;
}

function Field({ id, name, label, type, autoComplete, placeholder, defaultValue, hint, error }: FieldProps) {
	return (
		<div className="flex flex-col gap-1.5">
			<label htmlFor={id} className="text-xs font-semibold text-muted">
				{label}
			</label>
			<input
				id={id}
				name={name}
				type={type}
				autoComplete={autoComplete}
				placeholder={placeholder}
				defaultValue={defaultValue}
				className={`min-h-11 rounded-[5px] border bg-bg px-2.5 py-2 font-sans text-[13px] text-text focus:outline-none focus:shadow-[inset_0_0_0_2px_var(--accent)] ${
					error ? "border-danger" : "border-border focus:border-accent"
				}`}
			/>
			{error ? (
				<span className="text-[11px] text-danger">{error}</span>
			) : hint ? (
				<span className="text-[11px] text-faint">{hint}</span>
			) : null}
		</div>
	);
}
