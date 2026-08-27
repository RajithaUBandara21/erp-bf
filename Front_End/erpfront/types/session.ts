import type { ClientType } from "@/types/auth";

/** Mirrors the backend `SessionResponse` one-for-one. `current` is the only source of "This device". */
export interface SessionSummary {
	id: string;
	clientType: ClientType;
	createdAt: string;
	lastUsedAt: string;
	current: boolean;
}
