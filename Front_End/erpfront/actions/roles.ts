"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import type { AuthedResult } from "@/types/auth";
import type { RoleWriteRequest } from "@/types/roles";

export interface RoleFormState {
	error?: string;
	fieldErrors?: Record<string, string>;
}

type Failure = Extract<AuthedResult<unknown>, { success: false }>;

/** Map a failed authed request to form state: bounce on auth loss, route a duplicate-name 409 to the name field. */
function failureState(result: Failure, locale: string): RoleFormState {
	if ("unauthorized" in result) {
		redirect({ href: "/sign-in", locale });
	}
	if (result.status === 409 && /already exists/i.test(result.error)) {
		return { fieldErrors: { name: result.error } };
	}
	return { error: result.error, fieldErrors: result.fieldErrors };
}

// `locale` is a bound first argument (see components/roles/RoleForm.tsx) - a Server Action has no
// reliable way to read the current locale on its own, so the client passes what it already knows via useLocale().
/** `POST /api/roles`. Redirects to the new role's detail page on success. */
export async function createRole(locale: string, body: RoleWriteRequest): Promise<RoleFormState> {
	const result = await authedFetch<{ id: string }>("/api/roles", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(body),
	});

	if (result.success) {
		revalidatePath("/settings/roles");
		redirect({ href: `/settings/roles/${result.data.id}`, locale });
	}

	return failureState(result, locale);
}

// See createRole above for why `locale` is a bound first argument.
/** `POST /api/roles/{id}/members`. Adds a user to the role; returns an error for a 403/409. */
export async function assignMember(locale: string, roleId: string, userId: string): Promise<RoleFormState> {
	const result = await authedFetch(`/api/roles/${encodeURIComponent(roleId)}/members`, {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ userId }),
	});

	return memberActionState(result, roleId, locale);
}

// See createRole above for why `locale` is a bound first argument.
/** `DELETE /api/roles/{id}/members/{userId}`. Removes a user from the role; returns an error for a 403/409. */
export async function unassignMember(locale: string, roleId: string, userId: string): Promise<RoleFormState> {
	const result = await authedFetch(
		`/api/roles/${encodeURIComponent(roleId)}/members/${encodeURIComponent(userId)}`,
		{ method: "DELETE" },
	);

	return memberActionState(result, roleId, locale);
}

function memberActionState(result: AuthedResult<unknown>, roleId: string, locale: string): RoleFormState {
	if (result.success) {
		revalidatePath("/settings/roles");
		revalidatePath(`/settings/roles/${roleId}`);
		return {};
	}
	if ("unauthorized" in result) {
		redirect({ href: "/sign-in", locale });
	}
	// 403 (Owner-grant / permission ceiling) and 409 (last Owner) stay on the page as an inline error.
	return { error: result.error };
}

// See createRole above for why `locale` is a bound first argument.
/** `DELETE /api/roles/{id}`. Redirects to the role list on success; returns an error for a 403/409 conflict. */
export async function deleteRole(locale: string, roleId: string): Promise<RoleFormState> {
	const result = await authedFetch(`/api/roles/${encodeURIComponent(roleId)}`, { method: "DELETE" });

	if (result.success) {
		revalidatePath("/settings/roles");
		redirect({ href: "/settings/roles", locale });
	}

	if ("unauthorized" in result) {
		redirect({ href: "/sign-in", locale });
	}
	// 409 (system role / still has members) and 403 stay on the page as an inline error.
	return { error: result.error };
}

// See createRole above for why `locale` is a bound first argument.
/** `PUT /api/roles/{id}`. Returns `{}` on success. */
export async function updateRole(locale: string, roleId: string, body: RoleWriteRequest): Promise<RoleFormState> {
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

	return failureState(result, locale);
}
