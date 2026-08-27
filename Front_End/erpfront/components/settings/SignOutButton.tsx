"use client";

import { useFormStatus } from "react-dom";
import { signOut } from "@/actions/auth";

function SubmitButton() {
	const { pending } = useFormStatus();

	return (
		<button
			type="submit"
			disabled={pending}
			className="min-h-9 rounded-[5px] border border-border bg-surface px-3 py-1.5 text-xs font-semibold text-text hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-55"
		>
			{pending ? "Signing out..." : "Sign out"}
		</button>
	);
}

export function SignOutButton() {
	return (
		<form action={signOut}>
			<SubmitButton />
		</form>
	);
}
