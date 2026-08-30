import type { ReactElement } from "react";
import type { ClientType } from "@/types/auth";

const svgProps = {
	viewBox: "0 0 24 24",
	fill: "none",
	stroke: "currentColor",
	strokeWidth: 1.8,
	strokeLinecap: "round",
	strokeLinejoin: "round",
	className: "h-[18px] w-[18px]",
} as const;

// Icon only - the label is translated by the caller via settings.clientType.<key>, where <key> is
// this same ClientType lowercased (see clientTypeLabelKey below). Keeping this leaf module free of
// a next-intl dependency.
const CLIENT_TYPE_ICONS: Record<ClientType, ReactElement> = {
	WEB: (
		<svg {...svgProps}>
			<rect x="2" y="4" width="20" height="14" rx="2" />
			<line x1="8" y1="21" x2="16" y2="21" />
			<line x1="12" y1="18" x2="12" y2="21" />
		</svg>
	),
	MOBILE: (
		<svg {...svgProps}>
			<rect x="6" y="2" width="12" height="20" rx="2" />
			<line x1="11" y1="18" x2="13" y2="18" />
		</svg>
	),
	DESKTOP: (
		<svg {...svgProps}>
			<rect x="3" y="3" width="18" height="12" rx="2" />
			<line x1="8" y1="21" x2="16" y2="21" />
			<line x1="12" y1="15" x2="12" y2="21" />
		</svg>
	),
	TABLET: (
		<svg {...svgProps}>
			<rect x="4" y="3" width="16" height="14" rx="2" />
			<line x1="8" y1="21" x2="16" y2="21" />
			<line x1="12" y1="17" x2="12" y2="21" />
		</svg>
	),
};

export function clientTypeIcon(clientType: ClientType): ReactElement {
	return CLIENT_TYPE_ICONS[clientType] ?? CLIENT_TYPE_ICONS.WEB;
}

/** Key into the settings.clientType.* catalog namespace, e.g. clientTypeLabelKey("MOBILE") -> "mobile". */
export function clientTypeLabelKey(clientType: ClientType): string {
	return clientType in CLIENT_TYPE_ICONS ? clientType.toLowerCase() : "web";
}
