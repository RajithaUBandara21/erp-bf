"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import type { InviteCode } from "@/types/organizations";

const JOIN_REQUESTS_PATH = "/settings/join-requests";

export interface RotateInviteCodeState {
	inviteCode?: string;
	error?: string;
}

// See createOrganization in actions/organizations.ts for why `locale` is a bound first argument.
/** `POST /api/organizations/invite-code/rotate`. Invalidates the old code for anyone still holding it. */
export async function rotateInviteCode(locale: string): Promise<RotateInviteCodeState> {
	const result = await authedFetch<InviteCode>("/api/organizations/invite-code/rotate", { method: "POST" });

	if (result.success) {
		revalidatePath(JOIN_REQUESTS_PATH);
		return { inviteCode: result.data.inviteCode };
	}

	if ("unauthorized" in result) {
		redirect({ href: "/sign-in", locale });
	}

	return { error: result.error };
}

export interface JoinRequestActionState {
	error?: string;
}

// See createOrganization in actions/organizations.ts for why `locale` is a bound first argument.
/** `POST /api/organizations/join-requests/{id}/approve`. Activates the Membership and assigns `roleId`. */
export async function approveJoinRequest(
	locale: string,
	membershipId: string,
	roleId: string,
): Promise<JoinRequestActionState> {
	const result = await authedFetch(`/api/organizations/join-requests/${encodeURIComponent(membershipId)}/approve`, {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ roleId }),
	});

	if (result.success) {
		revalidatePath(JOIN_REQUESTS_PATH);
		return {};
	}

	if ("unauthorized" in result) {
		redirect({ href: "/sign-in", locale });
	}

	// 404 (already resolved elsewhere) / 409 (no longer pending) stay on the page as an inline error.
	return { error: result.error };
}

// See createOrganization in actions/organizations.ts for why `locale` is a bound first argument.
/** `POST /api/organizations/join-requests/{id}/reject`. Drops the request without activating it. */
export async function rejectJoinRequest(locale: string, membershipId: string): Promise<JoinRequestActionState> {
	const result = await authedFetch(`/api/organizations/join-requests/${encodeURIComponent(membershipId)}/reject`, {
		method: "POST",
	});

	if (result.success) {
		revalidatePath(JOIN_REQUESTS_PATH);
		return {};
	}

	if ("unauthorized" in result) {
		redirect({ href: "/sign-in", locale });
	}

	return { error: result.error };
}
