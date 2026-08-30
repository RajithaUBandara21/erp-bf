"use client";

import type { FormEvent, ReactNode } from "react";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import type { AuditLogQueryParams } from "@/types/audit";
import type { UserSummary } from "@/types/roles";
import { buildAuditLogQuery } from "@/lib/audit-log";

export interface AuditLogFilterValues {
	entityType: string;
	action: string;
	actorId: string;
	from: string;
	to: string;
}

const EMPTY_FILTERS: AuditLogFilterValues = { entityType: "", action: "", actorId: "", from: "", to: "" };

export function AuditLogFilters({
	initialFilters,
	actors,
}: {
	initialFilters: AuditLogFilterValues;
	actors: UserSummary[];
}) {
	const t = useTranslations("auditLog.filters");
	const router = useRouter();
	const [values, setValues] = useState(initialFilters);
	const hasActiveFilters = Object.values(initialFilters).some((value) => value !== "");

	function set<K extends keyof AuditLogFilterValues>(key: K) {
		return (event: { target: { value: string } }) => setValues((prev) => ({ ...prev, [key]: event.target.value }));
	}

	function onSubmit(event: FormEvent<HTMLFormElement>) {
		event.preventDefault();
		const params: AuditLogQueryParams = {
			entityType: values.entityType.trim(),
			action: values.action.trim(),
			actorId: values.actorId,
			from: values.from ? new Date(values.from).toISOString() : "",
			to: values.to ? new Date(`${values.to}T23:59:59.999Z`).toISOString() : "",
		};
		router.push(`/settings/audit-log${buildAuditLogQuery(params)}`);
	}

	function onClear() {
		setValues(EMPTY_FILTERS);
		router.push("/settings/audit-log");
	}

	return (
		<form onSubmit={onSubmit} className="mb-4 flex flex-col items-stretch gap-3 sm:flex-row sm:flex-wrap sm:items-end">
			<Field label={t("entityTypeLabel")}>
				<input
					type="text"
					value={values.entityType}
					onChange={set("entityType")}
					placeholder={t("entityTypePlaceholder")}
					className="min-h-9 w-full rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none sm:w-36"
				/>
			</Field>
			<Field label={t("actionLabel")}>
				<input
					type="text"
					value={values.action}
					onChange={set("action")}
					placeholder={t("actionPlaceholder")}
					className="min-h-9 w-full rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none sm:w-40"
				/>
			</Field>
			<Field label={t("actorLabel")}>
				<select
					value={values.actorId}
					onChange={set("actorId")}
					className="min-h-9 w-full rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none sm:w-44"
				>
					<option value="">{t("allActors")}</option>
					{actors.map((actor) => (
						<option key={actor.id} value={actor.id}>
							{actor.fullName}
						</option>
					))}
				</select>
			</Field>
			<Field label={t("fromLabel")}>
				<input
					type="date"
					value={values.from}
					onChange={set("from")}
					className="min-h-9 w-full rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none sm:w-auto"
				/>
			</Field>
			<Field label={t("toLabel")}>
				<input
					type="date"
					value={values.to}
					onChange={set("to")}
					className="min-h-9 w-full rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none sm:w-auto"
				/>
			</Field>
			<button
				type="submit"
				className="min-h-9 rounded-[5px] bg-accent px-3.5 py-1.5 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover"
			>
				{t("apply")}
			</button>
			{hasActiveFilters && (
				<button
					type="button"
					onClick={onClear}
					className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-1.5 text-[13px] font-semibold text-text hover:bg-surface-alt"
				>
					{t("clear")}
				</button>
			)}
		</form>
	);
}

function Field({ label, children }: { label: string; children: ReactNode }) {
	return (
		<label className="flex flex-col gap-1.5">
			<span className="text-xs font-semibold text-muted">{label}</span>
			{children}
		</label>
	);
}
