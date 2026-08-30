"use client";

import { Fragment, useMemo } from "react";
import { useTranslations } from "next-intl";
import type { PermissionAction } from "@/types/roles";
import { PERMISSION_ACTIONS, type MatrixGroup } from "@/lib/permission-matrix";
import { RESOURCE_GROUPS } from "@/lib/resource-groups";

const ACTION_KEYS: Record<PermissionAction, string> = {
	VIEW: "view",
	CREATE: "create",
	EDIT: "edit",
	DELETE: "delete",
	APPROVE: "approve",
};

const GROUP_KEYS = new Set(RESOURCE_GROUPS.map((group) => group.key));

interface Props {
	groups: MatrixGroup[];
	/** Permission codes currently granted. */
	selectedCodes: readonly string[];
	/** Omit for a read-only matrix. Receives the next code list, sorted. */
	onChange?: (nextCodes: string[]) => void;
	readOnly?: boolean;
}

export function PermissionMatrix({ groups, selectedCodes, onChange, readOnly = false }: Props) {
	const t = useTranslations("permissions");
	const moduleGroupsT = useTranslations("moduleGroups");
	const selected = useMemo(() => new Set(selectedCodes), [selectedCodes]);
	const disabled = readOnly || !onChange;

	function groupLabel(key: string): string {
		return GROUP_KEYS.has(key) ? moduleGroupsT(key) : t("otherGroup");
	}

	// Every code in the matrix, per action column, for the "toggle all" header checkboxes.
	const columnCodes = useMemo(() => {
		const result = {} as Record<PermissionAction, string[]>;
		for (const action of PERMISSION_ACTIONS) result[action] = [];
		for (const group of groups) {
			for (const row of group.rows) {
				for (const action of PERMISSION_ACTIONS) {
					const code = row.codesByAction[action];
					if (code) result[action].push(code);
				}
			}
		}
		return result;
	}, [groups]);

	function emit(next: Set<string>) {
		onChange?.([...next].sort());
	}

	function toggleCode(code: string, checked: boolean) {
		const next = new Set(selected);
		if (checked) next.add(code);
		else next.delete(code);
		emit(next);
	}

	function toggleColumn(action: PermissionAction, checked: boolean) {
		const next = new Set(selected);
		for (const code of columnCodes[action]) {
			if (checked) next.add(code);
			else next.delete(code);
		}
		emit(next);
	}

	return (
		<div className="overflow-x-auto">
			<table className="w-full min-w-[420px] border-collapse text-xs">
				<thead>
					<tr>
						<th className="p-2 text-left text-[10px] font-bold uppercase tracking-wide text-muted">
							{t("moduleColumn")}
						</th>
						{PERMISSION_ACTIONS.map((action) => {
							const codes = columnCodes[action];
							const on = codes.filter((code) => selected.has(code)).length;
							const actionLabel = t(`actions.${ACTION_KEYS[action]}`);
							return (
								<th
									key={action}
									className="p-2 text-center text-[10px] font-bold uppercase tracking-wide text-muted"
								>
									<div className="flex flex-col items-center gap-1.5">
										<span>{actionLabel}</span>
										<input
											type="checkbox"
											aria-label={t("toggleColumnAria", { action: actionLabel })}
											className="h-4 w-4 accent-accent"
											disabled={disabled}
											checked={codes.length > 0 && on === codes.length}
											ref={(el) => {
												if (el) el.indeterminate = on > 0 && on < codes.length;
											}}
											onChange={(event) => toggleColumn(action, event.target.checked)}
										/>
									</div>
								</th>
							);
						})}
					</tr>
				</thead>
				<tbody>
					{groups.map((group) => (
						<Fragment key={group.key}>
							<tr>
								<td
									colSpan={1 + PERMISSION_ACTIONS.length}
									className="bg-bg px-2 pt-3 pb-1.5 text-[10px] font-bold uppercase tracking-wider text-faint"
								>
									{groupLabel(group.key)}
								</td>
							</tr>
							{group.rows.map((row) => (
								<tr key={row.resource} className="border-b border-border hover:bg-surface-alt">
									<td className="py-2 pr-2 font-semibold text-text">{row.label}</td>
									{PERMISSION_ACTIONS.map((action) => {
										const code = row.codesByAction[action];
										return (
											<td key={action} className="p-2 text-center">
												{code ? (
													<input
														type="checkbox"
														aria-label={t("cellAria", { resource: row.label, action: t(`actions.${ACTION_KEYS[action]}`) })}
														className="h-[18px] w-[18px] accent-accent"
														disabled={disabled}
														checked={selected.has(code)}
														onChange={(event) => toggleCode(code, event.target.checked)}
													/>
												) : null}
											</td>
										);
									})}
								</tr>
							))}
						</Fragment>
					))}
				</tbody>
			</table>
		</div>
	);
}
