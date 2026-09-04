import { getLocale, getTranslations } from "next-intl/server";
import { Link, redirect } from "@/i18n/navigation";
import { AuthShell } from "@/components/auth/AuthShell";
import { JoinForm } from "@/components/auth/JoinForm";
import { hasAccessToken } from "@/lib/auth-cookies";

export default async function JoinPage() {
	if (await hasAccessToken()) {
		redirect({ href: "/", locale: await getLocale() });
	}
	const t = await getTranslations("auth.join");

	return (
		<AuthShell
			title={t("title")}
			description={t("description")}
			footer={
				<>
					{t("haveAccount")}{" "}
					<Link href="/sign-in" className="font-semibold text-accent hover:underline">
						{t("signIn")}
					</Link>
				</>
			}
		>
			<JoinForm />
		</AuthShell>
	);
}
