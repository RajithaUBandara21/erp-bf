import { redirect } from "next/navigation";
import { authedFetch } from "@/lib/api";
import type { MePermissions } from "@/types/roles";

/**
 * The caller's effective permission codes, from `GET /api/auth/me/permissions`. Server-only.
 *
 * Redirects to `/sign-in` when there is no usable session. On any other failure it returns an
 * empty set, so the UI hides privileged controls rather than showing them by mistake - every
 * endpoint still enforces the real check server-side regardless of what the UI renders.
 */
export async function fetchMyPermissions(): Promise<Set<string>> {
	const result = await authedFetch<MePermissions>("/api/auth/me/permissions");

	if (!result.success) {
		if ("unauthorized" in result) {
			redirect("/sign-in");
		}
		return new Set();
	}

	return new Set(result.data?.permissions ?? []);
}

/** Whether `perms` grants `code`, e.g. `can(perms, "role.create")`. UX gating only. */
export function can(perms: ReadonlySet<string>, code: string): boolean {
	return perms.has(code);
}
