import type { ReactElement } from "react";
import type { ClientType } from "@/types/auth";

interface ClientTypeDisplay {
	label: string;
	icon: ReactElement;
}

const svgProps = {
	viewBox: "0 0 24 24",
	fill: "none",
	stroke: "currentColor",
	strokeWidth: 1.8,
	strokeLinecap: "round",
	strokeLinejoin: "round",
	className: "h-[18px] w-[18px]",
} as const;

// i18n keys (build-plan 4): client_type_web / client_type_mobile / client_type_desktop / client_type_tablet
const CLIENT_TYPES: Record<ClientType, ClientTypeDisplay> = {
	WEB: {
		label: "Web browser",
		icon: (
			<svg {...svgProps}>
				<rect x="2" y="4" width="20" height="14" rx="2" />
				<line x1="8" y1="21" x2="16" y2="21" />
				<line x1="12" y1="18" x2="12" y2="21" />
			</svg>
		),
	},
	MOBILE: {
		label: "Mobile app",
		icon: (
			<svg {...svgProps}>
				<rect x="6" y="2" width="12" height="20" rx="2" />
				<line x1="11" y1="18" x2="13" y2="18" />
			</svg>
		),
	},
	DESKTOP: {
		label: "Desktop app",
		icon: (
			<svg {...svgProps}>
				<rect x="3" y="3" width="18" height="12" rx="2" />
				<line x1="8" y1="21" x2="16" y2="21" />
				<line x1="12" y1="15" x2="12" y2="21" />
			</svg>
		),
	},
	TABLET: {
		label: "Tablet",
		icon: (
			<svg {...svgProps}>
				<rect x="4" y="3" width="16" height="14" rx="2" />
				<line x1="8" y1="21" x2="16" y2="21" />
				<line x1="12" y1="17" x2="12" y2="21" />
			</svg>
		),
	},
};

export function clientTypeDisplay(clientType: ClientType): ClientTypeDisplay {
	return CLIENT_TYPES[clientType] ?? CLIENT_TYPES.WEB;
}
