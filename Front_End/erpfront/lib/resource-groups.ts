export interface ResourceGroupDef {
	key: string;
	label: string;
	resources: readonly string[];
}

/**
 * Single source of truth for the backend-resource -> UI-group taxonomy, shared by the module
 * launcher (modules.ts) and the permission matrix (permission-matrix.ts). Every backend resource
 * (V6__create_permissions_table.sql) must be listed here. A resource missing from every group
 * falls through to an "Other" section in the matrix so it stays visible instead of silently
 * dropping out; the module launcher has no such fallback, so every module entry's
 * `permissionResource` must resolve to one of these groups.
 */
export const RESOURCE_GROUPS: readonly ResourceGroupDef[] = [
	{ key: "operations", label: "Operations", resources: ["product", "inventory", "purchasing", "sales", "pos", "ecommerce"] },
	{ key: "business_partners", label: "Business partners", resources: ["customer", "supplier", "shipping"] },
	{ key: "finance", label: "Finance", resources: ["payment", "accounting", "promotion"] },
	{ key: "insight_system", label: "Insight & system", resources: ["reporting", "notification", "audit"] },
	{ key: "administration", label: "Administration", resources: ["user", "role", "organization", "billing"] },
];

/** Group key for a backend resource, or undefined if the resource isn't in any group. */
export function resourceGroupKey(resource: string): string | undefined {
	return RESOURCE_GROUPS.find((group) => group.resources.includes(resource))?.key;
}
