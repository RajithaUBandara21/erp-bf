import { resourceGroupKey } from "@/lib/resource-groups";

export type ModuleGroup = "operations" | "business_partners" | "finance" | "insight_system";

export interface ModuleEntry {
	id: string;
	label: string;
	path: string;
	description: string;
	group: ModuleGroup;
	/** `<resource>.view` gates this tile; omitted means always visible (e.g. Settings). */
	permissionResource?: string;
}

/**
 * Locked route registry for every remaining MVP module (build-plan 5-18). Every later module
 * feature builds its real UI at the exact `path` recorded here - see current-feature.md
 * "Route registry". Audit Logs and Settings already exist at their listed paths and are not
 * stub routes (see Step 2), but are included so the module grid (Step 3) can render them.
 */
/** Derives a module entry's group from its backend resource; throws on a resource with no group, since every module-launcher resource must be listed in resource-groups.ts. */
function moduleGroup(resource: string): ModuleGroup {
	const key = resourceGroupKey(resource);
	if (!key) {
		throw new Error(`resource "${resource}" is not listed in any resource group`);
	}
	return key as ModuleGroup;
}

export const MODULE_REGISTRY: ModuleEntry[] = [
	{ id: "products", label: "Products", path: "/products", description: "Catalog, pricing, variants", group: moduleGroup("product"), permissionResource: "product" },
	{ id: "inventory", label: "Inventory", path: "/inventory", description: "Stock, warehouses, transfers", group: moduleGroup("inventory"), permissionResource: "inventory" },
	{ id: "purchasing", label: "Purchasing", path: "/purchasing", description: "Purchase orders, goods receiving", group: moduleGroup("purchasing"), permissionResource: "purchasing" },
	{ id: "sales", label: "Sales", path: "/sales", description: "Quotations, orders, invoices", group: moduleGroup("sales"), permissionResource: "sales" },
	{ id: "pos", label: "Point of Sale", path: "/pos", description: "Cashier checkout & receipts", group: moduleGroup("pos"), permissionResource: "pos" },
	{ id: "ecommerce", label: "E-Commerce", path: "/ecommerce", description: "Storefront & online orders", group: moduleGroup("ecommerce"), permissionResource: "ecommerce" },
	{ id: "customers", label: "Customers", path: "/customers", description: "Profiles, addresses, history", group: moduleGroup("customer"), permissionResource: "customer" },
	{ id: "suppliers", label: "Suppliers", path: "/suppliers", description: "Contacts & purchase history", group: moduleGroup("supplier"), permissionResource: "supplier" },
	{ id: "shipping", label: "Shipping", path: "/shipping", description: "Deliveries & tracking", group: moduleGroup("shipping"), permissionResource: "shipping" },
	{ id: "payments", label: "Payments", path: "/payments", description: "Methods, transactions, refunds", group: moduleGroup("payment"), permissionResource: "payment" },
	{ id: "accounting", label: "Accounting", path: "/accounting", description: "Journals, receivables, payables", group: moduleGroup("accounting"), permissionResource: "accounting" },
	{ id: "promotions", label: "Promotions", path: "/promotions", description: "Discounts & coupons", group: moduleGroup("promotion"), permissionResource: "promotion" },
	{ id: "reporting", label: "Reporting", path: "/reporting", description: "Sales, stock & revenue KPIs", group: moduleGroup("reporting"), permissionResource: "reporting" },
	{ id: "notifications", label: "Notifications", path: "/notifications", description: "Alerts & order updates", group: moduleGroup("notification"), permissionResource: "notification" },
	{ id: "audit-log", label: "Audit Logs", path: "/settings/audit-log", description: "User actions & data changes", group: moduleGroup("audit"), permissionResource: "audit" },
	{ id: "settings", label: "Settings", path: "/settings", description: "Tenant & organization setup", group: "insight_system" },
];
