"use client";

import { useActionState } from "react";
import { signInWithGoogle, type OAuthActionState } from "@/actions/oauth";

const initialState: OAuthActionState = {};

// i18n keys (build-plan 4): continue_with_google, redirecting
export function GoogleSignInButton() {
	const [state, formAction, pending] = useActionState(signInWithGoogle, initialState);

	return (
		<form action={formAction} className="flex flex-col items-stretch gap-1">
			<button
				type="submit"
				disabled={pending}
				className="min-h-11 w-full rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-55"
			>
				{pending ? "Redirecting..." : "Continue with Google"}
			</button>
			{state.error && <span className="text-[11px] text-danger">{state.error}</span>}
		</form>
	);
}
