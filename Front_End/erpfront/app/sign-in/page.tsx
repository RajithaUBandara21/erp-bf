import Link from "next/link";
import { redirect } from "next/navigation";
import { AuthShell } from "@/components/auth/AuthShell";
import { SignInForm } from "@/components/auth/SignInForm";
import { hasSession } from "@/lib/auth-cookies";

export default async function SignInPage() {
	if (await hasSession()) {
		redirect("/");
	}

	return (
		<AuthShell
			title="Sign in"
			description="Sign in to your Universal ERP workspace"
			footer={
				<>
					Don&apos;t have an account?{" "}
					<Link href="/sign-up" className="font-semibold text-accent hover:underline">
						Create your workspace
					</Link>
				</>
			}
		>
			<SignInForm />
		</AuthShell>
	);
}
