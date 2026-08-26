import type { ReactNode } from "react";
import { ThemeToggle } from "@/components/auth/ThemeToggle";

interface AuthShellProps {
	title: string;
	description: string;
	children: ReactNode;
	footer?: ReactNode;
	/** Sign-up's two-column field grid needs the prototype's wider 440px card; sign-in stays at 380px. */
	wide?: boolean;
}

export function AuthShell({ title, description, children, footer, wide }: AuthShellProps) {
	return (
		<div className="flex min-h-full flex-1 flex-col bg-bg text-text">
			<header className="flex h-14 items-center justify-between px-6">
				<span className="flex items-center font-bold tracking-tight">
					<span className="mr-2 inline-block h-5.5 w-5.5 rounded-md bg-accent" />
					Universal ERP
				</span>
				<ThemeToggle />
			</header>
			<main className="flex flex-1 items-center justify-center p-6">
				<div
					className={`w-full ${wide ? "max-w-110" : "max-w-95"} rounded-lg border border-border bg-surface p-6 shadow-sm`}
				>
					<h1 className="mb-1 text-xl font-semibold">{title}</h1>
					<p className="mb-6 text-[13px] text-muted">{description}</p>
					{children}
					{footer && <p className="mt-6 text-center text-[13px] text-muted">{footer}</p>}
				</div>
			</main>
		</div>
	);
}
