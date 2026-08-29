"use client";

import type { ChangeEvent } from "react";
import { useSearchParams } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { useRouter, usePathname } from "@/i18n/navigation";
import { routing } from "@/i18n/routing";

// Matches ThemeToggle's visual pattern (h-[34px], border-border, bg-surface-alt, rounded-[5px]).
// A native <select> rather than a custom dropdown - three languages, no reason to hand-roll a
// popover with outside-click handling for what the browser already does accessibly.
export function LanguageSwitcher() {
	const t = useTranslations("languageSwitcher");
	const locale = useLocale();
	const router = useRouter();
	const pathname = usePathname();
	const searchParams = useSearchParams();

	function onChange(event: ChangeEvent<HTMLSelectElement>) {
		const nextLocale = event.target.value;
		router.replace({ pathname, query: Object.fromEntries(searchParams) }, { locale: nextLocale });
	}

	return (
		<select
			value={locale}
			onChange={onChange}
			aria-label={t("label")}
			title={t("label")}
			className="h-[34px] flex-shrink-0 rounded-[5px] border border-border bg-surface-alt px-2 text-xs font-semibold text-text"
		>
			{routing.locales.map((code) => (
				<option key={code} value={code} title={t(`locales.${code}`)}>
					{code.toUpperCase()}
				</option>
			))}
		</select>
	);
}
