import { getTranslations } from "next-intl/server";
import { ThemeToggle } from "@/components/auth/ThemeToggle";
import { LanguageSwitcher } from "@/components/shell/LanguageSwitcher";
import { SignOutButton } from "@/components/settings/SignOutButton";

// Shared topbar for every authenticated route (build-plan 3b), replacing the settings-only
// SettingsHeader placeholder. Notifications are out of scope here (see current-feature.md Out of
// scope).
export async function AppHeader() {
	const t = await getTranslations("shell");

	return (
		<header className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-border bg-surface px-6">
			<span className="flex items-center font-bold tracking-tight">
				<span className="mr-2 inline-block h-5.5 w-5.5 rounded-md bg-accent" />
				{t("brand")}
			</span>
			<div className="flex items-center gap-2">
				<LanguageSwitcher />
				<ThemeToggle />
				<SignOutButton />
			</div>
		</header>
	);
}
