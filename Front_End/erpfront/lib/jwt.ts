import { cookies } from "next/headers";
import { ACCESS_TOKEN_COOKIE } from "@/lib/auth-cookies";

interface AccessTokenClaims {
	email?: string;
}

/**
 * Best-effort email read from the access token's payload, for display only. A plain base64url
 * decode of the JWT's middle segment, not a verified claim - never use this for authorization,
 * which stays server-side in Spring Security. Returns null on a missing token or any decode failure,
 * so callers can fall back to no greeting rather than fail the page.
 */
export async function decodeAccessTokenEmail(): Promise<string | null> {
	const token = (await cookies()).get(ACCESS_TOKEN_COOKIE)?.value;
	if (!token) return null;

	const payload = token.split(".")[1];
	if (!payload) return null;

	try {
		const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf-8")) as AccessTokenClaims;
		return typeof claims.email === "string" ? claims.email : null;
	} catch {
		return null;
	}
}
