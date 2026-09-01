import { getLocale, getTranslations } from "next-intl/server";
import type { SignInFormState } from "@/actions/auth";
import { Link, redirect } from "@/i18n/navigation";
import { AuthShell } from "@/components/auth/AuthShell";
import { SignInForm } from "@/components/auth/SignInForm";
import { postJson } from "@/lib/api";
import { hasAccessToken, setAuthCookies } from "@/lib/auth-cookies";
import type { LoginResponse } from "@/types/auth";

const OAUTH_ERROR_KEYS: Record<string, string> = {
	"not-linked": "notLinked",
	"email-unverified": "emailUnverified",
	"already-linked": "alreadyLinked",
};

export default async function SignInPage({
	searchParams,
}: {
	searchParams: Promise<{ oauth?: string; code?: string; reason?: string }>;
}) {
	const { oauth, code, reason } = await searchParams;
	const locale = await getLocale();
	const t = await getTranslations("auth");

	// Driven by the backend's callback redirect (?oauth=code&code=...), not a form submission, so the
	// exchange runs here rather than as a server action. A failed/reused code falls through to the
	// generic error below instead of throwing.
	let googleError: string | undefined;
	let oauthSelection: SignInFormState["selection"];
	if (oauth === "code" && code) {
		const result = await postJson<LoginResponse>("/api/auth/oauth/google/exchange", { code });
		if (!result.success) {
			googleError = t("google.signInFailed");
		} else if (result.data.outcome === "AUTHENTICATED") {
			// No "remember me" checkbox in this flow - default to a persistent session, like signUp does.
			await setAuthCookies(result.data.session, true);
			redirect({ href: "/", locale });
		} else {
			// Several organizations: hand the selection straight to the same selector the password flow uses.
			oauthSelection = {
				selectionToken: result.data.selectionToken,
				organizations: result.data.organizations,
				remember: true,
			};
		}
	} else if (oauth === "error") {
		const key = reason && OAUTH_ERROR_KEYS[reason];
		googleError = key ? t(`google.${key}`) : t("google.signInFailed");
	}

	// Only a live session bounces you home - a stale/revoked refresh cookie must not lock you out of the
	// form. Runs after the code exchange above so an in-flight Google sign-in is never short-circuited.
	if (await hasAccessToken()) {
		redirect({ href: "/", locale });
	}

	return (
		<AuthShell
			title={t("signIn.title")}
			description={t("signIn.description")}
			footer={
				<>
					{t("signIn.noAccount")}{" "}
					<Link href="/sign-up" className="font-semibold text-accent hover:underline">
						{t("signIn.createWorkspace")}
					</Link>
				</>
			}
		>
			{googleError && (
				<div className="mb-4 flex items-start gap-2 rounded-[5px] bg-danger-bg px-3 py-2.5 text-xs text-danger">
					<span>{googleError}</span>
				</div>
			)}
			<SignInForm initialSelection={oauthSelection} />
		</AuthShell>
	);
}
