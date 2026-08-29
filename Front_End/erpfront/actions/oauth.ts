"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { authedFetch } from "@/lib/api";
import type { AuthorizationUrlResponse } from "@/types/oauth";

const SETTINGS_PATH = "/settings";

export interface OAuthActionState {
	error?: string;
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
