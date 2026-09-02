import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { AuthShell } from "@/components/auth/AuthShell";
import { postJson } from "@/lib/api";
import type { VerifyEmailResponse } from "@/types/auth";

// Interim landing page so the emailed verification link resolves end to end now. 5d replaces this
// with the full-styled self-join flow (join-with-invite-code form, sign-in entry links, org switcher).
export default async function VerifyEmailPage({
	searchParams,
}: {
	searchParams: Promise<{ token?: string }>;
}) {
	const { token } = await searchParams;
	const t = await getTranslations("auth.verifyEmail");

	let message: string;
	if (!token || token.trim() === "") {
		message = t("missingToken");
	} else {
		const result = await postJson<VerifyEmailResponse>("/api/auth/verify-email", { token });
		message = result.success ? result.data.message : result.error;
	}

	return (
		<AuthShell
			title={t("title")}
			description={t("description")}
			footer={
				<Link href="/sign-in" className="font-semibold text-accent hover:underline">
					{t("backToSignIn")}
				</Link>
			}
		>
			<p className="text-[13px] text-text">{message}</p>
		</AuthShell>
	);
}
