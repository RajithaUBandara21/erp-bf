"use client";

import type { ChangeEvent } from "react";
import { useActionState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { switchOrganization, type SwitchOrganizationState } from "@/actions/auth";
import type { ReachableOrganization } from "@/types/auth";

const initialState: SwitchOrganizationState = {};

// Mirrors LanguageSwitcher - a native <select> with the same tokens, no hand-rolled popover. The
// switch is a form submit (onChange -> requestSubmit) so React runs it through the same
// useActionState + FormData path as every other action in the app.
export function OrganizationSwitcher({ reachable }: { reachable: ReachableOrganization[] }) {
	if (reachable.length === 0) {
		return null;
	}

	if (reachable.length === 1) {
		return <span className="text-xs font-semibold text-text">{reachable[0].organizationName}</span>;
	}

	return <SwitcherSelect reachable={reachable} />;
}

function SwitcherSelect({ reachable }: { reachable: ReachableOrganization[] }) {
	const locale = useLocale();
	const t = useTranslations("shell.orgSwitcher");
	const [state, formAction, pending] = useActionState(switchOrganization.bind(null, locale), initialState);

	const current = reachable.find((org) => org.current);
	// Insertion order follows the API's tenant-then-name sort, so the groups stay sorted too.
	const tenantNames = new Map(reachable.map((org) => [org.tenantId, org.tenantName]));
	const grouped = tenantNames.size > 1;

	function onChange(event: ChangeEvent<HTMLSelectElement>) {
		event.currentTarget.form?.requestSubmit();
	}

	const renderOption = (org: ReachableOrganization) => (
		<option key={org.organizationId} value={org.organizationId}>
			{org.viaTenantAdmin ? t("viaTenantAdmin", { name: org.organizationName }) : org.organizationName}
		</option>
	);

	return (
		<form action={formAction} className="flex items-center gap-2">
			<select
				name="organizationId"
				value={current?.organizationId ?? ""}
				onChange={onChange}
				disabled={pending}
				aria-label={t("label")}
				title={t("label")}
				className="h-[34px] flex-shrink-0 rounded-[5px] border border-border bg-surface-alt px-2 text-xs font-semibold text-text disabled:cursor-not-allowed disabled:opacity-55"
			>
				{/* The current org was deactivated mid-session (5b.3 leaves this reachable) - keep switching away possible. */}
				{!current && (
					<option value="" disabled>
						{t("currentUnavailable")}
					</option>
				)}
				{grouped
					? [...tenantNames].map(([tenantId, tenantName]) => (
							<optgroup key={tenantId} label={tenantName}>
								{reachable.filter((org) => org.tenantId === tenantId).map(renderOption)}
							</optgroup>
						))
					: reachable.map(renderOption)}
			</select>
			{/* state.error is a backend sentence - not translated here. */}
			{state.error && <span className="text-[11px] text-danger">{state.error}</span>}
		</form>
	);
}
