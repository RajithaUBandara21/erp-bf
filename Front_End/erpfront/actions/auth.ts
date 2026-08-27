"use server";

import { redirect } from "next/navigation";
import { API_BASE_URL, postJson } from "@/lib/api";
import { clearAuthCookies, getRefreshToken, setAuthCookies } from "@/lib/auth-cookies";
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

export async function signUp(_prevState: SignUpFormState, formData: FormData): Promise<SignUpFormState> {
	const organizationName = String(formData.get("organizationName") ?? "").trim();
	const fullName = String(formData.get("fullName") ?? "").trim();
	const email = String(formData.get("email") ?? "").trim();
	const password = String(formData.get("password") ?? "");
	const confirmPassword = String(formData.get("confirmPassword") ?? "");
	const agreeTerms = formData.get("agreeTerms") === "on";
	const values = { organizationName, fullName, email, agreeTerms };

	const fieldErrors: Record<string, string> = {};
	if (!organizationName) fieldErrors.organizationName = "Organization name is required.";
	if (!fullName) fieldErrors.fullName = "Full name is required.";
	if (!email) fieldErrors.email = "Email is required.";
	if (!PASSWORD_PATTERN.test(password)) {
		fieldErrors.password = "Password must be at least 8 characters, with one number and one uppercase letter.";
	}
	if (password !== confirmPassword) {
		fieldErrors.confirmPassword = "Passwords do not match.";
	}
	if (!agreeTerms) {
		fieldErrors.agreeTerms = "You must agree to the Terms of Service and Privacy Policy.";
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

	await setAuthCookies(result.data);
	redirect("/");
}

export interface SignInFormState {
	error?: string;
	fieldErrors?: Record<string, string>;
	values?: {
		organizationCode: string;
		email: string;
	};
}

export async function signIn(_prevState: SignInFormState, formData: FormData): Promise<SignInFormState> {
	const organizationCode = String(formData.get("organizationCode") ?? "").trim();
	const email = String(formData.get("email") ?? "").trim();
	const password = String(formData.get("password") ?? "");
	const remember = formData.get("remember") === "on";
	const values = { organizationCode, email };

	const fieldErrors: Record<string, string> = {};
	if (!organizationCode) fieldErrors.organizationCode = "Organization code is required.";
	if (!email) fieldErrors.email = "Email is required.";
	if (!password) fieldErrors.password = "Password is required.";
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
	redirect("/");
}

export async function signOut(): Promise<void> {
	const refreshToken = await getRefreshToken();

	if (refreshToken) {
		// Best-effort: revoke the current session server-side. Clearing our own cookies is what
		// actually signs the user out, so a failed/unreachable logout must not block that.
		try {
			await fetch(`${API_BASE_URL}/api/auth/logout`, {
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
	redirect("/sign-in");
}
