"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { authedFetch } from "@/lib/api";
import type { AuthedResult } from "@/types/auth";
import type { RoleWriteRequest } from "@/types/roles";

export interface RoleFormState {
	error?: string;
	fieldErrors?: Record<string, string>;
}

type Failure = Extract<AuthedResult<unknown>, { success: false }>;

/** Map a failed authed request to form state: bounce on auth loss, route a duplicate-name 409 to the name field. */
function failureState(result: Failure): RoleFormState {
	if ("unauthorized" in result) {
		redirect("/sign-in");
	}
	if (result.status === 409 && /already exists/i.test(result.error)) {
		return { fieldErrors: { name: result.error } };
	}
	return { error: result.error, fieldErrors: result.fieldErrors };
}

/** `POST /api/roles`. Redirects to the new role's detail page on success. */
export async function createRole(body: RoleWriteRequest): Promise<RoleFormState> {
	const result = await authedFetch<{ id: string }>("/api/roles", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(body),
	});

	if (result.success) {
		revalidatePath("/settings/roles");
		redirect(`/settings/roles/${result.data.id}`);
	}

	return failureState(result);
}

/** `POST /api/roles/{id}/members`. Adds a user to the role; returns an error for a 403/409. */
export async function assignMember(roleId: string, userId: string): Promise<RoleFormState> {
	const result = await authedFetch(`/api/roles/${encodeURIComponent(roleId)}/members`, {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ userId }),
	});

	return memberActionState(result, roleId);
}

/** `DELETE /api/roles/{id}/members/{userId}`. Removes a user from the role; returns an error for a 403/409. */
export async function unassignMember(roleId: string, userId: string): Promise<RoleFormState> {
	const result = await authedFetch(
		`/api/roles/${encodeURIComponent(roleId)}/members/${encodeURIComponent(userId)}`,
		{ method: "DELETE" },
	);

	return memberActionState(result, roleId);
}

function memberActionState(result: AuthedResult<unknown>, roleId: string): RoleFormState {
	if (result.success) {
		revalidatePath("/settings/roles");
		revalidatePath(`/settings/roles/${roleId}`);
		return {};
	}
	if ("unauthorized" in result) {
		redirect("/sign-in");
	}
	// 403 (Owner-grant / permission ceiling) and 409 (last Owner) stay on the page as an inline error.
	return { error: result.error };
}

/** `DELETE /api/roles/{id}`. Redirects to the role list on success; returns an error for a 403/409 conflict. */
export async function deleteRole(roleId: string): Promise<RoleFormState> {
	const result = await authedFetch(`/api/roles/${encodeURIComponent(roleId)}`, { method: "DELETE" });

	if (result.success) {
		revalidatePath("/settings/roles");
		redirect("/settings/roles");
	}

	if ("unauthorized" in result) {
		redirect("/sign-in");
	}
	// 409 (system role / still has members) and 403 stay on the page as an inline error.
	return { error: result.error };
}

/** `PUT /api/roles/{id}`. Returns `{}` on success. */
export async function updateRole(roleId: string, body: RoleWriteRequest): Promise<RoleFormState> {
	const result = await authedFetch(`/api/roles/${encodeURIComponent(roleId)}`, {
		method: "PUT",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(body),
	});

	if (result.success) {
		revalidatePath("/settings/roles");
		revalidatePath(`/settings/roles/${roleId}`);
		return {};
	}

	return failureState(result);
}
