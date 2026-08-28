import { cookies } from "next/headers";
import type { ActionResult, AuthedResult, ErrorResponse } from "@/types/auth";
import { ACCESS_TOKEN_COOKIE } from "@/lib/auth-cookies";
import { fetchWithTimeout, API_BASE_URL } from "@/lib/http";

export { API_BASE_URL };

/** Server-only: never call from a client component, the base URL and any future auth headers stay off the browser. */
export async function postJson<T>(path: string, body: unknown): Promise<ActionResult<T>> {
	let response: Response;
	try {
		response = await fetchWithTimeout(`${API_BASE_URL}${path}`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(body),
		});
	} catch {
		return { success: false, error: "Unable to reach the server. Please try again." };
	}

	if (response.ok) {
		const data = (await response.json()) as T;
		return { success: true, data };
	}

	const errorBody = (await response.json().catch(() => null)) as ErrorResponse | null;
	return {
		success: false,
		error: errorBody?.message ?? "Something went wrong. Please try again.",
		fieldErrors: errorBody?.errors,
	};
}

/**
 * Server-only authed request: attaches the `accessToken` cookie as a Bearer header.
 * Returns `{ unauthorized: true }` (never a generic error) when the cookie is absent or the API answers 401,
 * so callers can send the user to /sign-in. A missing cookie short-circuits without touching the backend.
 * The error case carries `status` so callers can special-case a response like 404.
 */
export async function authedFetch<T>(path: string, init?: RequestInit): Promise<AuthedResult<T>> {
	const accessToken = (await cookies()).get(ACCESS_TOKEN_COOKIE)?.value;
	if (!accessToken) {
		return { success: false, unauthorized: true };
	}

	const headers = new Headers(init?.headers);
	headers.set("Authorization", `Bearer ${accessToken}`);

	let response: Response;
	try {
		// Per-user data must never land in Next's fetch cache.
		response = await fetchWithTimeout(`${API_BASE_URL}${path}`, { ...init, headers, cache: "no-store" });
	} catch {
		return { success: false, error: "Unable to reach the server. Please try again." };
	}

	if (response.status === 401) {
		return { success: false, unauthorized: true };
	}

	if (response.ok) {
		const data = response.status === 204 ? (undefined as T) : ((await response.json()) as T);
		return { success: true, data };
	}

	const errorBody = (await response.json().catch(() => null)) as ErrorResponse | null;
	return {
		success: false,
		error: errorBody?.message ?? "Something went wrong. Please try again.",
		fieldErrors: errorBody?.errors,
		status: response.status,
	};
}
