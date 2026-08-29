import type { ComponentType, ReactNode, SVGProps } from "react";

// Subset of prototypes/launcher.html's inline SVG icon sprite used by the module grid tiles
// and its grid/list view toggle - see current-feature.md Step 3.
type IconProps = SVGProps<SVGSVGElement>;

function Icon({ children, ...props }: IconProps & { children: ReactNode }) {
	return (
		<svg
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			strokeWidth={1.8}
			strokeLinecap="round"
			strokeLinejoin="round"
			{...props}
		>
			{children}
		</svg>
	);
}

function TagIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<g transform="rotate(45 12 12)">
				<rect x="4" y="4" width="16" height="16" rx="3" />
			</g>
			<circle cx="8.5" cy="8.5" r="1.2" fill="currentColor" stroke="none" />
		</Icon>
	);
}

function BoxIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<polyline points="3,7 12,3 21,7 12,11 3,7" />
			<polyline points="3,7 3,17 12,21 12,11" />
			<polyline points="21,7 21,17 12,21" />
		</Icon>
	);
}

function TruckIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<rect x="2" y="7" width="13" height="9" rx="1" />
			<path d="M15 10h4l3 3v3h-7z" />
			<circle cx="7" cy="18" r="1.6" />
			<circle cx="17" cy="18" r="1.6" />
		</Icon>
	);
}

function CartIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<circle cx="9" cy="20" r="1.4" />
			<circle cx="18" cy="20" r="1.4" />
			<path d="M2 3h2l2.4 12.2a2 2 0 0 0 2 1.8h8.2a2 2 0 0 0 2-1.6L21 8H6" />
		</Icon>
	);
}

function RegisterIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<rect x="3" y="4" width="18" height="12" rx="2" />
			<rect x="7" y="7" width="6" height="4" rx="1" />
			<line x1="8" y1="20" x2="16" y2="20" />
			<line x1="12" y1="16" x2="12" y2="20" />
		</Icon>
	);
}

function GlobeIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<circle cx="12" cy="12" r="9" />
			<line x1="3" y1="12" x2="21" y2="12" />
			<path d="M12 3c3 3.5 3 14 0 18" />
			<path d="M12 3c-3 3.5 -3 14 0 18" />
		</Icon>
	);
}

function UserIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<circle cx="12" cy="8" r="4" />
			<path d="M4 20c0-4.4 3.6-7 8-7s8 2.6 8 7" />
		</Icon>
	);
}

function BuildingIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<rect x="4" y="3" width="16" height="18" rx="1" />
			<rect x="7" y="6" width="2" height="2" fill="currentColor" stroke="none" />
			<rect x="11" y="6" width="2" height="2" fill="currentColor" stroke="none" />
			<rect x="15" y="6" width="2" height="2" fill="currentColor" stroke="none" />
			<rect x="7" y="10" width="2" height="2" fill="currentColor" stroke="none" />
			<rect x="11" y="10" width="2" height="2" fill="currentColor" stroke="none" />
			<rect x="15" y="10" width="2" height="2" fill="currentColor" stroke="none" />
			<rect x="9" y="15" width="6" height="6" />
		</Icon>
	);
}

function ShipIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<polyline points="3,9 11,5 19,9 11,13 3,9" />
			<polyline points="3,9 3,17 11,21 11,13" />
			<polyline points="19,9 19,17 11,21" />
			<line x1="17" y1="2" x2="22" y2="2" />
			<polyline points="20,0.5 22,2 20,3.5" />
		</Icon>
	);
}

function CardIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<rect x="2" y="5" width="20" height="14" rx="2" />
			<line x1="2" y1="10" x2="22" y2="10" />
			<line x1="6" y1="15" x2="10" y2="15" />
		</Icon>
	);
}

function CalcIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<rect x="4" y="2" width="16" height="20" rx="2" />
			<line x1="8" y1="6" x2="16" y2="6" />
			{[11, 15, 19].flatMap((y) =>
				[8, 12, 16].map((x) => <circle key={`${x}-${y}`} cx={x} cy={y} r="1" fill="currentColor" stroke="none" />),
			)}
		</Icon>
	);
}

function PercentIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<line x1="19" y1="5" x2="5" y2="19" />
			<circle cx="6.5" cy="6.5" r="2.5" />
			<circle cx="17.5" cy="17.5" r="2.5" />
		</Icon>
	);
}

function ChartIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<rect x="4" y="10" width="3" height="10" fill="currentColor" stroke="none" />
			<rect x="10.5" y="5" width="3" height="15" fill="currentColor" stroke="none" />
			<rect x="17" y="13" width="3" height="7" fill="currentColor" stroke="none" />
		</Icon>
	);
}

function BellIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<path d="M6 8a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6" />
			<path d="M10 20a2 2 0 0 0 4 0" />
		</Icon>
	);
}

function ShieldIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<path d="M12 2l8 4v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V6l8-4z" />
			<polyline points="9,12 11,14 15,10" />
		</Icon>
	);
}

function GearIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<circle cx="12" cy="12" r="3" />
			<line x1="12" y1="2" x2="12" y2="5" />
			<line x1="12" y1="19" x2="12" y2="22" />
			<line x1="2" y1="12" x2="5" y2="12" />
			<line x1="19" y1="12" x2="22" y2="12" />
			<line x1="4.9" y1="4.9" x2="7" y2="7" />
			<line x1="17" y1="17" x2="19.1" y2="19.1" />
			<line x1="4.9" y1="19.1" x2="7" y2="17" />
			<line x1="17" y1="7" x2="19.1" y2="4.9" />
		</Icon>
	);
}

export function GridIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<rect x="3" y="3" width="7" height="7" rx="1" />
			<rect x="14" y="3" width="7" height="7" rx="1" />
			<rect x="3" y="14" width="7" height="7" rx="1" />
			<rect x="14" y="14" width="7" height="7" rx="1" />
		</Icon>
	);
}

export function ListIcon(props: IconProps) {
	return (
		<Icon {...props}>
			<line x1="8" y1="6" x2="21" y2="6" />
			<line x1="8" y1="12" x2="21" y2="12" />
			<line x1="8" y1="18" x2="21" y2="18" />
			<circle cx="4" cy="6" r="1" fill="currentColor" stroke="none" />
			<circle cx="4" cy="12" r="1" fill="currentColor" stroke="none" />
			<circle cx="4" cy="18" r="1" fill="currentColor" stroke="none" />
		</Icon>
	);
}

/** Icon per `ModuleEntry.id` (`lib/modules.ts`). */
export const MODULE_ICONS: Record<string, ComponentType<IconProps>> = {
	products: TagIcon,
	inventory: BoxIcon,
	purchasing: TruckIcon,
	sales: CartIcon,
	pos: RegisterIcon,
	ecommerce: GlobeIcon,
	customers: UserIcon,
	suppliers: BuildingIcon,
	shipping: ShipIcon,
	payments: CardIcon,
	accounting: CalcIcon,
	promotions: PercentIcon,
	reporting: ChartIcon,
	notifications: BellIcon,
	"audit-log": ShieldIcon,
	settings: GearIcon,
};

/** Tile icon background color per `ModuleEntry.id`, matching prototypes/launcher.html. */
export const MODULE_TILE_COLORS: Record<string, string> = {
	products: "#2f6fed",
	inventory: "#0891b2",
	purchasing: "#7c3aed",
	sales: "#16a34a",
	pos: "#dc2626",
	ecommerce: "#0284c7",
	customers: "#d97706",
	suppliers: "#65a30d",
	shipping: "#9333ea",
	payments: "#0d9488",
	accounting: "#334155",
	promotions: "#be123c",
	reporting: "#4f46e5",
	notifications: "#57534e",
	"audit-log": "#1f2937",
	settings: "#475569",
};
