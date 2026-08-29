import { redirect } from "next/navigation";
import { hasSession } from "@/lib/auth-cookies";
import { AppHeader } from "@/components/shell/AppHeader";

// Shared shell for every authenticated route (build-plan 3b): session guard + shared topbar.
// proxy.ts (widened to every authenticated route) also guards this, but the layout never trusts
// that it ran - same defense-in-depth the settings layout used to do on its own.
export default async function AppLayout({ children }: LayoutProps<"/">) {
	if (!(await hasSession())) {
		redirect("/sign-in");
	}

	return (
		<div className="flex min-h-full flex-1 flex-col bg-bg text-text">
			<AppHeader />
			<main className="flex flex-1 flex-col">{children}</main>
		</div>
	);
}
