import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { AuthShell } from "@/components/auth/AuthShell";
import { postJson } from "@/lib/api";
import type { VerifyEmailResponse } from "@/types/auth";

export default async function VerifyEmailPage({
	searchParams,
}: {
	searchParams: Promise<{ token?: string }>;
}) {
	const { token } = await searchParams;
	const t = await getTranslations("auth.verifyEmail");

	// A missing token never reaches the backend; every other outcome (200 success, or the 400/409
	// whose `error` is already a human sentence) renders whatever `postJson` returns.
	const result =
		token && token.trim() !== ""
			? await postJson<VerifyEmailResponse>("/api/auth/verify-email", { token })
			: null;

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
			{result?.success ? (
				<p className="text-[13px] text-text">
					{t.rich("success", {
						organizationName: result.data.organizationName,
						org: (chunks) => <span className="font-semibold text-text">{chunks}</span>,
					})}
				</p>
			) : (
				<p className="text-[13px] text-text">{result ? result.error : t("missingToken")}</p>
			)}
		</AuthShell>
	);
}
