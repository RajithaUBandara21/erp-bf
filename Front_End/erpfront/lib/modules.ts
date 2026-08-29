export type ModuleGroup = "Operations" | "Business partners" | "Finance" | "Insight & system";

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
export const MODULE_REGISTRY: ModuleEntry[] = [
	{ id: "products", label: "Products", path: "/products", description: "Catalog, pricing, variants", group: "Operations", permissionResource: "product" },
	{ id: "inventory", label: "Inventory", path: "/inventory", description: "Stock, warehouses, transfers", group: "Operations", permissionResource: "inventory" },
	{ id: "purchasing", label: "Purchasing", path: "/purchasing", description: "Purchase orders, goods receiving", group: "Operations", permissionResource: "purchasing" },
	{ id: "sales", label: "Sales", path: "/sales", description: "Quotations, orders, invoices", group: "Operations", permissionResource: "sales" },
	{ id: "pos", label: "Point of Sale", path: "/pos", description: "Cashier checkout & receipts", group: "Operations", permissionResource: "pos" },
	{ id: "ecommerce", label: "E-Commerce", path: "/ecommerce", description: "Storefront & online orders", group: "Operations", permissionResource: "ecommerce" },
	{ id: "customers", label: "Customers", path: "/customers", description: "Profiles, addresses, history", group: "Business partners", permissionResource: "customer" },
	{ id: "suppliers", label: "Suppliers", path: "/suppliers", description: "Contacts & purchase history", group: "Business partners", permissionResource: "supplier" },
	{ id: "shipping", label: "Shipping", path: "/shipping", description: "Deliveries & tracking", group: "Business partners", permissionResource: "shipping" },
	{ id: "payments", label: "Payments", path: "/payments", description: "Methods, transactions, refunds", group: "Finance", permissionResource: "payment" },
	{ id: "accounting", label: "Accounting", path: "/accounting", description: "Journals, receivables, payables", group: "Finance", permissionResource: "accounting" },
	{ id: "promotions", label: "Promotions", path: "/promotions", description: "Discounts & coupons", group: "Finance", permissionResource: "promotion" },
	{ id: "reporting", label: "Reporting", path: "/reporting", description: "Sales, stock & revenue KPIs", group: "Insight & system", permissionResource: "reporting" },
	{ id: "notifications", label: "Notifications", path: "/notifications", description: "Alerts & order updates", group: "Insight & system", permissionResource: "notification" },
	{ id: "audit-log", label: "Audit Logs", path: "/settings/audit-log", description: "User actions & data changes", group: "Insight & system", permissionResource: "audit" },
	{ id: "settings", label: "Settings", path: "/settings", description: "Tenant & organization setup", group: "Insight & system" },
];
