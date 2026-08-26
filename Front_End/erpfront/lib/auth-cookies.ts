import { cookies } from "next/headers";
import type { TokenResponse } from "@/types/auth";

const ACCESS_TOKEN_COOKIE = "accessToken";
const REFRESH_TOKEN_COOKIE = "refreshToken";

/** The only code that should read/write these cookies - later features (refresh, logout, session list) extend this file, not touch cookies directly. */
export async function setAuthCookies(tokens: TokenResponse, remember?: boolean): Promise<void> {
	const cookieStore = await cookies();
	const secure = process.env.NODE_ENV !== "development";

	cookieStore.set(ACCESS_TOKEN_COOKIE, tokens.accessToken, {
		httpOnly: true,
		sameSite: "lax",
		secure,
		path: "/",
		maxAge: tokens.expiresIn,
	});

	cookieStore.set(REFRESH_TOKEN_COOKIE, tokens.refreshToken, {
		httpOnly: true,
		sameSite: "lax",
		secure,
		path: "/",
		...(remember === false ? {} : { maxAge: tokens.refreshExpiresIn }),
	});
}

export async function clearAuthCookies(): Promise<void> {
	const cookieStore = await cookies();
	cookieStore.delete(ACCESS_TOKEN_COOKIE);
	cookieStore.delete(REFRESH_TOKEN_COOKIE);
}

export async function hasSession(): Promise<boolean> {
	const cookieStore = await cookies();
	return cookieStore.has(ACCESS_TOKEN_COOKIE);
}
