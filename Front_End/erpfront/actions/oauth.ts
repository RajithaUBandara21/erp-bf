"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
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

export async function connectGoogle(): Promise<OAuthActionState> {
	const result = await authedFetch<AuthorizationUrlResponse>("/api/auth/oauth/google/link-url", { method: "POST" });

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect("/sign-in");
		}
		return { error: result.error };
	}

	redirect(result.data.authorizationUrl);
}

export async function disconnectGoogle(): Promise<OAuthActionState> {
	const result = await authedFetch("/api/auth/oauth/google", { method: "DELETE" });

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect("/sign-in");
		}
		return { error: result.error };
	}

	revalidatePath(SETTINGS_PATH);
	return {};
}
