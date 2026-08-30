import { RESOURCE_GROUPS } from "@/lib/resource-groups";
import type { PermissionAction, PermissionCatalogEntry } from "@/types/roles";

// Column order and the resource grouping / labels mirror prototypes/role-detail.html.

export const PERMISSION_ACTIONS: readonly PermissionAction[] = ["VIEW", "CREATE", "EDIT", "DELETE", "APPROVE"];

const RESOURCE_LABELS: Record<string, string> = {
	product: "Products",
	inventory: "Inventory",
	purchasing: "Purchasing",
	sales: "Sales",
	pos: "Point of Sale",
	ecommerce: "E-Commerce",
	customer: "Customers",
	supplier: "Suppliers",
	shipping: "Shipping",
	payment: "Payments",
	accounting: "Accounting",
	promotion: "Promotions",
	reporting: "Reporting",
	notification: "Notifications",
	audit: "Audit Logs",
	user: "Users",
	role: "Roles & permissions",
	organization: "Organization",
	billing: "Billing & plan",
};

export function resourceLabel(resource: string): string {
	return RESOURCE_LABELS[resource] ?? resource.charAt(0).toUpperCase() + resource.slice(1);
}

export interface MatrixResourceRow {
	resource: string;
	label: string;
	/** action -> permission code, present only for actions the catalog defines for this resource */
	codesByAction: Partial<Record<PermissionAction, string>>;
}

export interface MatrixGroup {
	key: string;
	label: string;
	rows: MatrixResourceRow[];
}

/**
 * Flat permission catalog -> ordered, grouped resource rows for the matrix UI. `labelForResource`
 * is caller-supplied (built from `getTranslations("permissions")`) so row labels render in the
 * active locale; `resourceLabel` stays as the untranslated fallback for a resource with no catalog
 * key. `MatrixGroup.label` keeps the untranslated `RESOURCE_GROUPS` text - `PermissionMatrix`
 * resolves the translated group header from `group.key` itself rather than reading `.label`.
 */
export function groupCatalog(
	entries: readonly PermissionCatalogEntry[],
	labelForResource: (resource: string) => string = resourceLabel,
): MatrixGroup[] {
	const byResource = new Map<string, Partial<Record<PermissionAction, string>>>();
	for (const entry of entries) {
		const row = byResource.get(entry.resource) ?? {};
		row[entry.action] = entry.code;
		byResource.set(entry.resource, row);
	}

	const placed = new Set<string>();
	const groups: MatrixGroup[] = [];
	for (const group of RESOURCE_GROUPS) {
		const rows: MatrixResourceRow[] = [];
		for (const resource of group.resources) {
			const codesByAction = byResource.get(resource);
			if (!codesByAction) continue;
			placed.add(resource);
			rows.push({ resource, label: labelForResource(resource), codesByAction });
		}
		if (rows.length > 0) {
			groups.push({ key: group.key, label: group.label, rows });
		}
	}

	const leftover: MatrixResourceRow[] = [];
	for (const [resource, codesByAction] of byResource) {
		if (placed.has(resource)) continue;
		leftover.push({ resource, label: labelForResource(resource), codesByAction });
	}
	if (leftover.length > 0) {
		leftover.sort((a, b) => a.resource.localeCompare(b.resource));
		groups.push({ key: "other", label: "Other", rows: leftover });
	}

	return groups;
}

/** Role permission codes -> the Set the matrix uses to mark checked cells. */
export function selectionFromCodes(codes: readonly string[]): Set<string> {
	return new Set(codes);
}

/**
 * The matrix's checked-cell Set -> the sorted code list to send to the API, keeping only codes the
 * catalog actually defines (a stale checked code from an outdated render never reaches the backend).
 */
export function codesFromSelection(
	selection: ReadonlySet<string>,
	catalog: readonly PermissionCatalogEntry[],
): string[] {
	const known = new Set(catalog.map((entry) => entry.code));
	return [...selection].filter((code) => known.has(code)).sort();
}
