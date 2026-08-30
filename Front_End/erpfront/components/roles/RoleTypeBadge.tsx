"use client";

import { useTranslations } from "next-intl";

// "use client": rendered from both server components (RoleList, RoleDetailView) and the
// client RoleForm - useTranslations works from either side of the boundary, getTranslations
// (server-only) would not compose into RoleForm's client tree.
export function RoleTypeBadge({ systemManaged }: { systemManaged: boolean }) {
	const t = useTranslations("roles.type");
	return systemManaged ? (
		<span className="inline-flex items-center rounded-full bg-surface-alt px-2.5 py-0.5 text-[11px] font-semibold text-muted">
			{t("system")}
		</span>
	) : (
		<span className="inline-flex items-center rounded-full bg-accent px-2.5 py-0.5 text-[11px] font-semibold text-accent-ink">
			{t("custom")}
		</span>
	);
}
