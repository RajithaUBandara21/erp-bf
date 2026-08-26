import type { ActionResult, ErrorResponse } from "@/types/auth";

const API_BASE_URL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

/** Server-only: never call from a client component, the base URL and any future auth headers stay off the browser. */
export async function postJson<T>(path: string, body: unknown): Promise<ActionResult<T>> {
	let response: Response;
	try {
		response = await fetch(`${API_BASE_URL}${path}`, {
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
