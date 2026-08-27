"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { authedFetch } from "@/lib/api";

const SESSIONS_PATH = "/settings/sessions";

export interface RevokeState {
	error?: string;
}

export async function revokeSession(_prev: RevokeState, formData: FormData): Promise<RevokeState> {
	const sessionId = String(formData.get("sessionId") ?? "");
	if (!sessionId) {
		return { error: "Missing session." };
	}

	const result = await authedFetch(`/api/auth/sessions/${encodeURIComponent(sessionId)}`, { method: "DELETE" });

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect("/sign-in");
		}
		// 404 = the session is already gone; fall through and revalidate so the stale row clears.
		if (result.status !== 404) {
			return { error: result.error };
		}
	}

	revalidatePath(SESSIONS_PATH);
	return {};
}

export async function revokeOtherSessions(): Promise<RevokeState> {
	const result = await authedFetch("/api/auth/sessions/revoke-others", { method: "POST" });

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect("/sign-in");
		}
		return { error: result.error };
	}

	revalidatePath(SESSIONS_PATH);
	return {};
}
