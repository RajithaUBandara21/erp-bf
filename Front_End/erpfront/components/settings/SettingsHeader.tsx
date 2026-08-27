import { ThemeToggle } from "@/components/auth/ThemeToggle";

// Placeholder chrome until the app shell (build-plan 3). Sign-out button is wired in Step 5.
export function SettingsHeader() {
	return (
		<header className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-border bg-surface px-6">
			<span className="flex items-center font-bold tracking-tight">
				<span className="mr-2 inline-block h-5.5 w-5.5 rounded-md bg-accent" />
				Universal ERP
			</span>
			<ThemeToggle />
		</header>
	);
}
