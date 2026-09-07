"use server";

import { getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { API_BASE_URL, authedFetch, postJson } from "@/lib/api";
import { clearAuthCookies, getRefreshToken, hasRememberMe, setAuthCookies } from "@/lib/auth-cookies";
import { fetchWithTimeout } from "@/lib/http";
import type { LoginResponse, MembershipOption, SelfJoinResponse, TokenResponse } from "@/types/auth";

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

export interface JoinFormState {
	error?: string;
	fieldErrors?: Record<string, string>;
	// React resets the <form> after a server action runs, so uncontrolled fields need this to survive a failed submit.
	values?: {
		fullName: string;
		email: string;
		inviteCode: string;
	};
	// Set once the backend has accepted the request: the form swaps to the "check your email" panel.
	submitted?: boolean;
}

// See signUp above for why `locale` is a bound first argument. Unlike login/register, the join
// endpoint takes no clientType - it issues no session.
export async function join(locale: string, _prevState: JoinFormState, formData: FormData): Promise<JoinFormState> {
	const fullName = String(formData.get("fullName") ?? "").trim();
	const email = String(formData.get("email") ?? "").trim();
	const password = String(formData.get("password") ?? "");
	const confirmPassword = String(formData.get("confirmPassword") ?? "");
	const inviteCode = String(formData.get("inviteCode") ?? "").trim();
	const values = { fullName, email, inviteCode };
	const t = await getTranslations({ locale, namespace: "auth.join" });

	const fieldErrors: Record<string, string> = {};
	if (!fullName) fieldErrors.fullName = t("fullNameRequired");
	if (!email) fieldErrors.email = t("emailRequired");
	if (!PASSWORD_PATTERN.test(password)) {
		fieldErrors.password = t("passwordInvalid");
	}
	if (password !== confirmPassword) {
		fieldErrors.confirmPassword = t("passwordMismatch");
	}
	if (!inviteCode) fieldErrors.inviteCode = t("inviteCodeRequired");
	if (Object.keys(fieldErrors).length > 0) {
		return { fieldErrors, values };
	}

	const result = await postJson<SelfJoinResponse>("/api/auth/join", { email, password, fullName, inviteCode });

	if (!result.success) {
		// A 400 maps its `errors` onto the fields; the join endpoint's only other distinct failure is the
		// 404 for a bad/inactive invite code, which carries an empty `errors` map and reads fine as a
		// top-of-form banner (do not match on its text).
		if (result.fieldErrors && Object.keys(result.fieldErrors).length > 0) {
			return { fieldErrors: result.fieldErrors, values };
		}
		return { error: result.error, values };
	}

	// No session is issued - the joiner stays signed out until an Org Admin approves them.
	return { submitted: true, values };
}

export interface SignInFormState {
	error?: string;
	fieldErrors?: Record<string, string>;
	values?: {
		email: string;
	};
	// Set when the account has several organizations: the sign-in form swaps to the selector step.
	selection?: {
		selectionToken: string;
		organizations: MembershipOption[];
		remember: boolean;
	};
}

// See signUp above for why `locale` is a bound first argument.
export async function signIn(locale: string, _prevState: SignInFormState, formData: FormData): Promise<SignInFormState> {
	const email = String(formData.get("email") ?? "").trim();
	const password = String(formData.get("password") ?? "");
	const remember = formData.get("remember") === "on";
	const values = { email };
	const t = await getTranslations({ locale, namespace: "auth.signIn" });

	const fieldErrors: Record<string, string> = {};
	if (!email) fieldErrors.email = t("emailRequired");
	if (!password) fieldErrors.password = t("passwordRequired");
	if (Object.keys(fieldErrors).length > 0) {
		return { fieldErrors, values };
	}

	// This is the web app - clientType is always WEB here, unlike mobile/desktop clients hitting the same API.
	const result = await postJson<LoginResponse>("/api/auth/login", {
		email,
		password,
		clientType: "WEB",
	});

	if (!result.success) {
		// The backend collapses every login failure into one generic message - never split it back out per field.
		return { error: result.error, values };
	}

	if (result.data.outcome === "SELECT_ORGANIZATION") {
		return {
			values,
			selection: {
				selectionToken: result.data.selectionToken,
				organizations: result.data.organizations,
				remember,
			},
		};
	}

	await setAuthCookies(result.data.session, remember);
	redirect({ href: "/", locale });
}

export interface SelectOrganizationState {
	error?: string;
}

// `locale` and `selectionToken` are bound arguments (see components/auth/SignInForm.tsx).
export async function selectOrganization(
	locale: string,
	selectionToken: string,
	remember: boolean,
	_prevState: SelectOrganizationState,
	formData: FormData,
): Promise<SelectOrganizationState> {
	const membershipId = String(formData.get("membershipId") ?? "");
	const t = await getTranslations({ locale, namespace: "auth.signIn" });

	if (!membershipId) {
		return { error: t("selectRequired") };
	}

	const result = await postJson<TokenResponse>("/api/auth/login/select", {
		selectionToken,
		membershipId,
		clientType: "WEB",
	});

	if (!result.success) {
		return { error: result.error };
	}

	await setAuthCookies(result.data, remember);
	redirect({ href: "/", locale });
}

export interface SwitchOrganizationState {
	error?: string;
}

// See signUp above for why `locale` is a bound first argument (components/shell/OrganizationSwitcher.tsx
// passes it via useLocale()). Backs the top-nav Organization Switcher; the backend endpoint and its
// audit/auto-provision effects shipped in 5b.3.
export async function switchOrganization(
	locale: string,
	_prevState: SwitchOrganizationState,
	formData: FormData,
): Promise<SwitchOrganizationState> {
	const organizationId = String(formData.get("organizationId") ?? "");
	if (!organizationId) {
		return {};
	}

	// authedFetch sets no Content-Type of its own; this POST has a body, so pass it explicitly.
	const result = await authedFetch<TokenResponse>("/api/auth/switch-organization", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ organizationId }),
	});

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect({ href: "/sign-in", locale });
		}
		// Backend sentence (400 already-current / 403 unreachable / 404 no such org), shown untranslated.
		return { error: result.error };
	}

	// Same session, re-scoped: rewrite the cookies exactly as login does, preserving the remember-me choice.
	await setAuthCookies(result.data, await hasRememberMe());
	// Reload at home so every server component re-renders under the new Organization scope. Never returns.
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
