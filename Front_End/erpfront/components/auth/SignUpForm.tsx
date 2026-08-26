"use client";

import { useActionState } from "react";
import { signUp, type SignUpFormState } from "@/actions/auth";

const initialState: SignUpFormState = {};

export function SignUpForm() {
	const [state, formAction, pending] = useActionState(signUp, initialState);

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
					label="Organization name"
					type="text"
					autoComplete="organization"
					placeholder="Northstar Retail"
					defaultValue={state.values?.organizationName}
					error={state.fieldErrors?.organizationName}
					full
				/>
				<Field
					id="fullName"
					name="fullName"
					label="Full name"
					type="text"
					autoComplete="name"
					placeholder="Nimal Perera"
					defaultValue={state.values?.fullName}
					error={state.fieldErrors?.fullName}
					full
				/>
				<Field
					id="email"
					name="email"
					label="Email"
					type="email"
					autoComplete="email"
					placeholder="owner@northstar-retail.com"
					defaultValue={state.values?.email}
					error={state.fieldErrors?.email}
					full
				/>
				<Field
					id="password"
					name="password"
					label="Password"
					type="password"
					autoComplete="new-password"
					hint="Password must be at least 8 characters, with one number and one uppercase letter."
					error={state.fieldErrors?.password}
				/>
				<Field
					id="confirmPassword"
					name="confirmPassword"
					label="Confirm password"
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
					I agree to the{" "}
					<a href="#" className="font-semibold text-accent hover:underline">
						Terms of Service
					</a>{" "}
					and{" "}
					<a href="#" className="font-semibold text-accent hover:underline">
						Privacy Policy
					</a>
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
				{pending ? "Creating account..." : "Create account"}
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
