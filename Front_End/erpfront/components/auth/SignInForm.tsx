"use client";

import { useActionState } from "react";
import { signIn, type SignInFormState } from "@/actions/auth";
import { GoogleSignInButton } from "@/components/auth/GoogleSignInButton";

const initialState: SignInFormState = {};

export function SignInForm() {
	const [state, formAction, pending] = useActionState(signIn, initialState);

	return (
		<>
			<form action={formAction} noValidate>
				{state.error && (
					<div className="mb-4 flex items-start gap-2 rounded-[5px] bg-danger-bg px-3 py-2.5 text-xs text-danger">
						<span>{state.error}</span>
					</div>
				)}

				<Field
					id="organizationCode"
					name="organizationCode"
					label="Organization code"
					type="text"
					autoComplete="off"
					defaultValue={state.values?.organizationCode}
					error={state.fieldErrors?.organizationCode}
				/>
				<Field
					id="email"
					name="email"
					label="Email"
					type="email"
					autoComplete="email"
					defaultValue={state.values?.email}
					error={state.fieldErrors?.email}
				/>
				<Field
					id="password"
					name="password"
					label="Password"
					type="password"
					autoComplete="current-password"
					error={state.fieldErrors?.password}
				/>

				<label className="mb-6 flex items-center gap-1.5 text-xs text-muted">
					<input type="checkbox" name="remember" defaultChecked className="h-3.75 w-3.75 accent-accent" />
					<span>Remember me</span>
				</label>

				<button
					type="submit"
					disabled={pending}
					className="min-h-11 w-full rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink transition-colors hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-55"
				>
					{pending ? "Signing in..." : "Sign in"}
				</button>
			</form>

			<div className="my-5 flex items-center gap-3 text-[11px] font-semibold tracking-wide text-muted uppercase">
				<span className="h-px flex-1 bg-border" />
				<span>or</span>
				<span className="h-px flex-1 bg-border" />
			</div>

			<GoogleSignInButton />
		</>
	);
}

interface FieldProps {
	id: string;
	name: string;
	label: string;
	type: string;
	autoComplete: string;
	defaultValue?: string;
	error?: string;
}

function Field({ id, name, label, type, autoComplete, defaultValue, error }: FieldProps) {
	return (
		<div className="mb-4 flex flex-col gap-1.5">
			<label htmlFor={id} className="text-xs font-semibold text-muted">
				{label}
			</label>
			<input
				id={id}
				name={name}
				type={type}
				autoComplete={autoComplete}
				defaultValue={defaultValue}
				className={`min-h-11 rounded-[5px] border bg-bg px-2.5 py-2 font-sans text-[13px] text-text focus:outline-none focus:shadow-[inset_0_0_0_2px_var(--accent)] ${
					error ? "border-danger" : "border-border focus:border-accent"
				}`}
			/>
			{error && <span className="text-[11px] text-danger">{error}</span>}
		</div>
	);
}
