import { getLocale } from "next-intl/server";
import { Link, redirect } from "@/i18n/navigation";
import { AuthShell } from "@/components/auth/AuthShell";
import { SignInForm } from "@/components/auth/SignInForm";
import { postJson } from "@/lib/api";
import { hasAccessToken, setAuthCookies } from "@/lib/auth-cookies";
import type { TokenResponse } from "@/types/auth";

const OAUTH_ERROR_MESSAGES: Record<string, string> = {
	"not-linked": "No account is linked to this Google account yet. Sign in with your email and password, then connect Google from Settings.",
	"email-unverified": "Your Google account's email isn't verified. Verify it with Google, then try again.",
	"already-linked": "This Google account is already linked to a different user.",
};
const DEFAULT_OAUTH_ERROR = "Google sign-in failed. Please try again.";

export default async function SignInPage({
	searchParams,
}: {
	searchParams: Promise<{ oauth?: string; code?: string; reason?: string }>;
}) {
	const { oauth, code, reason } = await searchParams;
	const locale = await getLocale();

	// Driven by the backend's callback redirect (?oauth=code&code=...), not a form submission, so the
	// exchange runs here rather than as a server action. A failed/reused code falls through to the
	// generic error below instead of throwing.
	let googleError: string | undefined;
	if (oauth === "code" && code) {
		const result = await postJson<TokenResponse>("/api/auth/oauth/google/exchange", { code });
		if (result.success) {
			// No "remember me" checkbox in this flow - default to a persistent session, like signUp does.
			await setAuthCookies(result.data, true);
			redirect({ href: "/", locale });
		}
		googleError = DEFAULT_OAUTH_ERROR;
	} else if (oauth === "error") {
		googleError = (reason && OAUTH_ERROR_MESSAGES[reason]) ?? DEFAULT_OAUTH_ERROR;
	}

	// Only a live session bounces you home - a stale/revoked refresh cookie must not lock you out of the
	// form. Runs after the code exchange above so an in-flight Google sign-in is never short-circuited.
	if (await hasAccessToken()) {
		redirect({ href: "/", locale });
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
			{googleError && (
				<div className="mb-4 flex items-start gap-2 rounded-[5px] bg-danger-bg px-3 py-2.5 text-xs text-danger">
					<span>{googleError}</span>
				</div>
			)}
			<SignInForm />
		</AuthShell>
	);
}
