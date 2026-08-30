"use client";

import { useActionState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { signUp, type SignUpFormState } from "@/actions/auth";

const initialState: SignUpFormState = {};

export function SignUpForm() {
	const locale = useLocale();
	const t = useTranslations("auth.signUp");
	const [state, formAction, pending] = useActionState(signUp.bind(null, locale), initialState);

	return (
		<form action={formAction} noValidate>
			{state.error && (
				<div className="mb-4 flex items-start gap-2 rounded-[5px] bg-danger-bg px-3 py-2.5 text-xs text-danger">
					<span>{state.error}</span>
				</div>
			)}

			<div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
				<Field
					id="organizationName"
					name="organizationName"
					label={t("orgNameLabel")}
					type="text"
					autoComplete="organization"
					placeholder={t("orgNamePlaceholder")}
					defaultValue={state.values?.organizationName}
					error={state.fieldErrors?.organizationName}
					full
				/>
				<Field
					id="fullName"
					name="fullName"
					label={t("fullNameLabel")}
					type="text"
					autoComplete="name"
					placeholder={t("fullNamePlaceholder")}
					defaultValue={state.values?.fullName}
					error={state.fieldErrors?.fullName}
					full
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
					full
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
			</div>

			<label className="mb-1 mt-4 flex items-start gap-2 text-xs text-muted">
				<input
					type="checkbox"
					name="agreeTerms"
					defaultChecked={state.values?.agreeTerms}
					className="mt-0.5 h-3.75 w-3.75 shrink-0 accent-accent"
				/>
				<span>
					{t.rich("agreeTerms", {
						terms: (chunks) => (
							<a href="#" className="font-semibold text-accent hover:underline">
								{chunks}
							</a>
						),
						privacy: (chunks) => (
							<a href="#" className="font-semibold text-accent hover:underline">
								{chunks}
							</a>
						),
					})}
				</span>
			</label>
			{state.fieldErrors?.agreeTerms && (
				<p className="mb-4 text-[11px] text-danger">{state.fieldErrors.agreeTerms}</p>
			)}

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
	full?: boolean;
}

function Field({ id, name, label, type, autoComplete, placeholder, defaultValue, hint, error, full }: FieldProps) {
	return (
		<div className={`flex flex-col gap-1.5 ${full ? "sm:col-span-2" : ""}`}>
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
