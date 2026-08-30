import { getLocale, getTranslations } from "next-intl/server";
import { Link, redirect } from "@/i18n/navigation";
import { AuthShell } from "@/components/auth/AuthShell";
import { SignUpForm } from "@/components/auth/SignUpForm";
import { hasAccessToken } from "@/lib/auth-cookies";

export default async function SignUpPage() {
	if (await hasAccessToken()) {
		redirect({ href: "/", locale: await getLocale() });
	}
	const t = await getTranslations("auth.signUp");

	return (
		<AuthShell
			title={t("title")}
			description={t("description")}
			wide
			footer={
				<>
					{t("haveAccount")}{" "}
					<Link href="/sign-in" className="font-semibold text-accent hover:underline">
						{t("signIn")}
					</Link>
				</>
			}
		>
			<SignUpForm />
		</AuthShell>
	);
}
