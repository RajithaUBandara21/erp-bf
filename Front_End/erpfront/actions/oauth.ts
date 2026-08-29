"use server";

import { revalidatePath } from "next/cache";
// `redirect` (plain) sends the browser off-site to Google's own OAuth URL - next-intl's locale-aware
// redirect only understands internal app paths, so the internal "/sign-in" bounce below uses the
// separately-imported `redirectLocale` instead.
import { redirect } from "next/navigation";
import { redirect as redirectLocale } from "@/i18n/navigation";
import { authedFetch, postJson } from "@/lib/api";
import type { AuthorizationUrlResponse } from "@/types/oauth";

const SETTINGS_PATH = "/settings";

export interface OAuthActionState {
	error?: string;
}

// Unauthenticated, unlike connectGoogle below - there's no session yet on the sign-in page.
export async function signInWithGoogle(): Promise<OAuthActionState> {
	const result = await postJson<AuthorizationUrlResponse>("/api/auth/oauth/google/login-url", {});

	if (!result.success) {
		return { error: result.error };
	}

	redirect(result.data.authorizationUrl);
}

// See actions/auth.ts's signUp for why `locale` is a bound first argument.
export async function connectGoogle(locale: string): Promise<OAuthActionState> {
	const result = await authedFetch<AuthorizationUrlResponse>("/api/auth/oauth/google/link-url", { method: "POST" });

	if (!result.success) {
		if ("unauthorized" in result) {
			redirectLocale({ href: "/sign-in", locale });
		}
		return { error: result.error };
	}

	redirect(result.data.authorizationUrl);
}

// See actions/auth.ts's signUp for why `locale` is a bound first argument.
export async function disconnectGoogle(locale: string): Promise<OAuthActionState> {
	const result = await authedFetch("/api/auth/oauth/google", { method: "DELETE" });

	if (!result.success) {
		if ("unauthorized" in result) {
			redirectLocale({ href: "/sign-in", locale });
		}
		return { error: result.error };
	}

	revalidatePath(SETTINGS_PATH);
	return {};
}
