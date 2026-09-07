import { getTranslations } from "next-intl/server";
import { ThemeToggle } from "@/components/auth/ThemeToggle";
import { LanguageSwitcher } from "@/components/shell/LanguageSwitcher";
import { OrganizationSwitcher } from "@/components/shell/OrganizationSwitcher";
import { SignOutButton } from "@/components/settings/SignOutButton";
import { fetchReachableOrganizations } from "@/lib/memberships";

// Shared topbar for every authenticated route (build-plan 3b), replacing the settings-only
// SettingsHeader placeholder. Notifications are out of scope here (see current-feature.md Out of
// scope).
export async function AppHeader() {
	const t = await getTranslations("shell");
	const reachable = await fetchReachableOrganizations();

	return (
		<header className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-border bg-surface px-6">
			<div className="flex items-center gap-3">
				<span className="flex items-center font-bold tracking-tight">
					<span className="mr-2 inline-block h-5.5 w-5.5 rounded-md bg-accent" />
					{t("brand")}
				</span>
				<OrganizationSwitcher reachable={reachable} />
			</div>
			<div className="flex items-center gap-2">
				<LanguageSwitcher />
				<ThemeToggle />
				<SignOutButton />
			</div>
		</header>
	);
}
