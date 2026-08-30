"use server";

import { getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { API_BASE_URL, postJson } from "@/lib/api";
import { clearAuthCookies, getRefreshToken, setAuthCookies } from "@/lib/auth-cookies";
import { fetchWithTimeout } from "@/lib/http";
import type { TokenResponse } from "@/types/auth";

// Mirrors the backend's @Pattern on RegisterRequest.password - UX only, the backend stays authoritative.
const PASSWORD_PATTERN = /^(?=.*[0-9])(?=.*[A-Z]).{8,}$/;

export interface SignUpFormState {
	error?: string;
	fieldErrors?: Record<string, string>;
	// React resets the <form> after a server action runs, so uncontrolled fields need this to survive a failed submit.
	values?: {
		organizationName: string;
		fullName: string;
		email: string;
		agreeTerms: boolean;
	};
}

// `locale` is a bound first argument (see components/auth/SignUpForm.tsx) - a Server Action has no
// reliable way to read the current locale on its own, so the client passes what it already knows via useLocale().
export async function signUp(locale: string, _prevState: SignUpFormState, formData: FormData): Promise<SignUpFormState> {
	const organizationName = String(formData.get("organizationName") ?? "").trim();
	const fullName = String(formData.get("fullName") ?? "").trim();
	const email = String(formData.get("email") ?? "").trim();
	const password = String(formData.get("password") ?? "");
	const confirmPassword = String(formData.get("confirmPassword") ?? "");
	const agreeTerms = formData.get("agreeTerms") === "on";
	const values = { organizationName, fullName, email, agreeTerms };
	const t = await getTranslations({ locale, namespace: "auth.signUp" });

	const fieldErrors: Record<string, string> = {};
	if (!organizationName) fieldErrors.organizationName = t("orgNameRequired");
	if (!fullName) fieldErrors.fullName = t("fullNameRequired");
	if (!email) fieldErrors.email = t("emailRequired");
	if (!PASSWORD_PATTERN.test(password)) {
		fieldErrors.password = t("passwordInvalid");
	}
	if (password !== confirmPassword) {
		fieldErrors.confirmPassword = t("passwordMismatch");
	}
	if (!agreeTerms) {
		fieldErrors.agreeTerms = t("agreeTermsRequired");
	}
	if (Object.keys(fieldErrors).length > 0) {
		return { fieldErrors, values };
	}

	const result = await postJson<TokenResponse>("/api/auth/register", {
		organizationName,
		fullName,
		email,
		password,
		clientType: "WEB",
	});

	if (!result.success) {
		return { error: result.error, fieldErrors: result.fieldErrors, values };
	}

	// Onboarding on a fresh org, no shared-machine signal - persist the session like today.
	await setAuthCookies(result.data, true);
	redirect({ href: "/", locale });
}

export interface SignInFormState {
	error?: string;
	fieldErrors?: Record<string, string>;
	values?: {
		organizationCode: string;
		email: string;
	};
}

// See signUp above for why `locale` is a bound first argument.
export async function signIn(locale: string, _prevState: SignInFormState, formData: FormData): Promise<SignInFormState> {
	const organizationCode = String(formData.get("organizationCode") ?? "").trim();
	const email = String(formData.get("email") ?? "").trim();
	const password = String(formData.get("password") ?? "");
	const remember = formData.get("remember") === "on";
	const values = { organizationCode, email };
	const t = await getTranslations({ locale, namespace: "auth.signIn" });

	const fieldErrors: Record<string, string> = {};
	if (!organizationCode) fieldErrors.organizationCode = t("orgCodeRequired");
	if (!email) fieldErrors.email = t("emailRequired");
	if (!password) fieldErrors.password = t("passwordRequired");
	if (Object.keys(fieldErrors).length > 0) {
		return { fieldErrors, values };
	}

	// This is the web app - clientType is always WEB here, unlike mobile/desktop clients hitting the same API.
	const result = await postJson<TokenResponse>("/api/auth/login", {
		organizationCode,
		email,
		password,
		clientType: "WEB",
	});

	if (!result.success) {
		// The backend collapses every login failure into one generic message - never split it back out per field.
		return { error: result.error, values };
	}

	await setAuthCookies(result.data, remember);
	redirect({ href: "/", locale });
}

// See signUp above for why `locale` is a bound first argument.
export async function signOut(locale: string): Promise<void> {
	const refreshToken = await getRefreshToken();

	if (refreshToken) {
		// Best-effort: revoke the current session server-side. Clearing our own cookies is what
		// actually signs the user out, so a failed/unreachable logout must not block that.
		try {
			await fetchWithTimeout(`${API_BASE_URL}/api/auth/logout`, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ refreshToken }),
				cache: "no-store",
			});
		} catch {
			// swallow - logout is best-effort
		}
	}

	await clearAuthCookies();
	redirect({ href: "/sign-in", locale });
}
