"use client";

import { useFormStatus } from "react-dom";
import { useLocale, useTranslations } from "next-intl";
import { signOut } from "@/actions/auth";

function SubmitButton() {
	const { pending } = useFormStatus();
	const t = useTranslations("settings");

	return (
		<button
			type="submit"
			disabled={pending}
			className="min-h-9 rounded-[5px] border border-border bg-surface px-3 py-1.5 text-xs font-semibold text-text hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-55"
		>
			{pending ? t("signingOut") : t("signOut")}
		</button>
	);
}

export function SignOutButton() {
	const locale = useLocale();

	return (
		<form action={signOut.bind(null, locale)}>
			<SubmitButton />
		</form>
	);
}
