"use client";

import type { FormEvent, ReactNode } from "react";
import { useState } from "react";
import { useRouter } from "next/navigation";
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

// i18n keys (build-plan 4): entity_type_filter_label, action_filter_label, actor_filter_label,
// date_from, date_to, apply_filters, clear_filters, all_actors
export function AuditLogFilters({
	initialFilters,
	actors,
}: {
	initialFilters: AuditLogFilterValues;
	actors: UserSummary[];
}) {
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
		<form onSubmit={onSubmit} className="mb-4 flex flex-wrap items-end gap-3">
			<Field label="Entity type">
				<input
					type="text"
					value={values.entityType}
					onChange={set("entityType")}
					placeholder="e.g. Role"
					className="min-h-9 w-36 rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none"
				/>
			</Field>
			<Field label="Action">
				<input
					type="text"
					value={values.action}
					onChange={set("action")}
					placeholder="e.g. role.created"
					className="min-h-9 w-40 rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none"
				/>
			</Field>
			<Field label="Actor">
				<select
					value={values.actorId}
					onChange={set("actorId")}
					className="min-h-9 w-44 rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none"
				>
					<option value="">All actors</option>
					{actors.map((actor) => (
						<option key={actor.id} value={actor.id}>
							{actor.fullName}
						</option>
					))}
				</select>
			</Field>
			<Field label="From">
				<input
					type="date"
					value={values.from}
					onChange={set("from")}
					className="min-h-9 rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none"
				/>
			</Field>
			<Field label="To">
				<input
					type="date"
					value={values.to}
					onChange={set("to")}
					className="min-h-9 rounded-[5px] border border-border bg-bg px-2.5 py-1.5 text-[13px] text-text focus:border-accent focus:outline-none"
				/>
			</Field>
			<button
				type="submit"
				className="min-h-9 rounded-[5px] bg-accent px-3.5 py-1.5 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover"
			>
				Apply filters
			</button>
			{hasActiveFilters && (
				<button
					type="button"
					onClick={onClear}
					className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-1.5 text-[13px] font-semibold text-text hover:bg-surface-alt"
				>
					Clear
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
