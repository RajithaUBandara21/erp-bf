"use server";

import { revalidatePath } from "next/cache";
import { getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";

const SESSIONS_PATH = "/settings/sessions";

export interface RevokeState {
	error?: string;
}

// `locale` is a bound first argument (see components/settings/SessionList.tsx) - a Server Action has
// no reliable way to read the current locale on its own, so the client passes what it already knows via useLocale().
export async function revokeSession(locale: string, _prev: RevokeState, formData: FormData): Promise<RevokeState> {
	const sessionId = String(formData.get("sessionId") ?? "");
	if (!sessionId) {
		const t = await getTranslations({ locale, namespace: "settings.sessions" });
		return { error: t("missingSession") };
	}

	const result = await authedFetch(`/api/auth/sessions/${encodeURIComponent(sessionId)}`, { method: "DELETE" });

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect({ href: "/sign-in", locale });
		}
		// 404 = the session is already gone; fall through and revalidate so the stale row clears.
		if (result.status !== 404) {
			return { error: result.error };
		}
	}

	revalidatePath(SESSIONS_PATH);
	return {};
}

// See revokeSession above for why `locale` is a bound first argument.
export async function revokeOtherSessions(locale: string): Promise<RevokeState> {
	const result = await authedFetch("/api/auth/sessions/revoke-others", { method: "POST" });

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect({ href: "/sign-in", locale });
		}
		return { error: result.error };
	}

	revalidatePath(SESSIONS_PATH);
	return {};
}
