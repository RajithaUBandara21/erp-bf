import Link from "next/link";
import { redirect } from "next/navigation";
import { AuthShell } from "@/components/auth/AuthShell";
import { SignUpForm } from "@/components/auth/SignUpForm";
import { hasAccessToken } from "@/lib/auth-cookies";

export default async function SignUpPage() {
	if (await hasAccessToken()) {
		redirect("/");
	}

	return (
		<AuthShell
			title="Create your workspace"
			description="Set up a new organization and admin account"
			wide
			footer={
				<>
					Already have an account?{" "}
					<Link href="/sign-in" className="font-semibold text-accent hover:underline">
						Sign in
					</Link>
				</>
			}
		>
			<SignUpForm />
		</AuthShell>
	);
}
