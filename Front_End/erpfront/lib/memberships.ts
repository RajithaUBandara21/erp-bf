import { getLocale } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import type { ReachableOrganization } from "@/types/auth";

/**
 * The Organizations the caller can switch into, from `GET /api/auth/memberships`. Server-only.
 *
 * Redirects to `/sign-in` when there is no usable session. On any other failure it returns an empty
 * list, so `AppHeader` degrades to no switcher rather than erroring the whole page.
 */
export async function fetchReachableOrganizations(): Promise<ReachableOrganization[]> {
	const result = await authedFetch<ReachableOrganization[]>("/api/auth/memberships");

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect({ href: "/sign-in", locale: await getLocale() });
		}
		return [];
	}

	return result.data ?? [];
}
