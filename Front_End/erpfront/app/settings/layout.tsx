import { redirect } from "next/navigation";
import { hasSession } from "@/lib/auth-cookies";
import { SettingsHeader } from "@/components/settings/SettingsHeader";

// Defense-in-depth: proxy.ts (Step 3) also guards /settings/*, but the layout never trusts that it ran.
export default async function SettingsLayout({ children }: LayoutProps<"/settings">) {
	if (!(await hasSession())) {
		redirect("/sign-in");
	}

	return (
		<div className="flex min-h-full flex-1 flex-col bg-bg text-text">
			<SettingsHeader />
			<main className="mx-auto w-full max-w-[780px] flex-1 p-6">{children}</main>
		</div>
	);
}
