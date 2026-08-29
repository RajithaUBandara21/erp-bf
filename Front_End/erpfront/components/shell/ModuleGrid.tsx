"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import type { ModuleEntry, ModuleGroup } from "@/lib/modules";
import { GridIcon, ListIcon, MODULE_ICONS, MODULE_TILE_COLORS } from "@/components/shell/icons";

export interface ModuleTile extends ModuleEntry {
	enabled: boolean;
}

const GROUP_ORDER: ModuleGroup[] = ["operations", "business_partners", "finance", "insight_system"];

type ViewMode = "grid" | "list";

export function ModuleGrid({ tiles }: { tiles: ModuleTile[] }) {
	const t = useTranslations("moduleLauncher");
	const tGroups = useTranslations("moduleGroups");
	const [query, setQuery] = useState("");
	const [view, setView] = useState<ViewMode>("grid");

	const groups = useMemo(() => {
		const needle = query.trim().toLowerCase();
		const filtered = needle ? tiles.filter((tile) => tile.label.toLowerCase().includes(needle)) : tiles;
		return GROUP_ORDER.map((group) => ({
			group,
			tiles: filtered.filter((tile) => tile.group === group),
		})).filter((entry) => entry.tiles.length > 0);
	}, [tiles, query]);

	return (
		<div>
			<div className="mb-4 flex items-center justify-between gap-3">
				<input
					type="text"
					value={query}
					onChange={(event) => setQuery(event.target.value)}
					placeholder={t("searchPlaceholder")}
					aria-label={t("searchLabel")}
					className="min-h-9 w-full max-w-[360px] rounded-[5px] border border-border bg-surface px-3 py-2 text-[13px] text-text placeholder:text-faint focus:border-accent focus:outline-none"
				/>
				<div className="flex flex-shrink-0 overflow-hidden rounded-[5px] border border-border">
					<button
						type="button"
						onClick={() => setView("grid")}
						aria-label={t("gridView")}
						title={t("gridView")}
						className={`flex h-[34px] w-[34px] items-center justify-center ${view === "grid" ? "bg-accent text-accent-ink" : "bg-surface text-muted"}`}
					>
						<GridIcon className="h-4 w-4" />
					</button>
					<button
						type="button"
						onClick={() => setView("list")}
						aria-label={t("listView")}
						title={t("listView")}
						className={`flex h-[34px] w-[34px] items-center justify-center border-l border-border ${view === "list" ? "bg-accent text-accent-ink" : "bg-surface text-muted"}`}
					>
						<ListIcon className="h-4 w-4" />
					</button>
				</div>
			</div>

			{groups.length === 0 && <p className="text-[13px] text-muted">{t("noMatches", { query })}</p>}

			{groups.map(({ group, tiles: groupTiles }) => (
				<section key={group} className="mb-6">
					<h2 className="mb-2 text-xs font-bold uppercase tracking-wide text-muted">{tGroups(group)}</h2>
					<div className={view === "grid" ? "grid grid-cols-[repeat(auto-fill,minmax(140px,1fr))] gap-3" : "flex flex-col gap-1.5"}>
						{groupTiles.map((tile) => (
							<Tile key={tile.id} tile={tile} view={view} noAccessLabel={t("noAccess", { label: tile.label })} />
						))}
					</div>
				</section>
			))}
		</div>
	);
}

function Tile({ tile, view, noAccessLabel }: { tile: ModuleTile; view: ViewMode; noAccessLabel: string }) {
	const TileIcon = MODULE_ICONS[tile.id];
	const color = MODULE_TILE_COLORS[tile.id];

	const iconBox = (
		<span
			className={`flex flex-shrink-0 items-center justify-center rounded-xl text-white ${view === "grid" ? "h-11 w-11" : "h-9 w-9"}`}
			style={{ background: color }}
		>
			{TileIcon && <TileIcon className={view === "grid" ? "h-[22px] w-[22px]" : "h-[18px] w-[18px]"} />}
		</span>
	);

	const content =
		view === "grid" ? (
			<>
				{iconBox}
				<span className="text-center text-[13px] font-semibold">{tile.label}</span>
			</>
		) : (
			<>
				{iconBox}
				<span className="flex flex-col text-left">
					<span className="text-[13px] font-semibold">{tile.label}</span>
					<span className="text-xs font-normal text-muted">{tile.description}</span>
				</span>
			</>
		);

	const className =
		view === "grid"
			? "flex flex-col items-center gap-2.5 rounded-lg border border-border bg-surface px-2 py-3 text-text shadow-sm"
			: "flex flex-row items-center gap-3 rounded-lg border border-border bg-surface px-3 py-2.5 text-text shadow-sm";

	if (!tile.enabled) {
		return (
			<div className={`${className} cursor-not-allowed opacity-45`} aria-disabled="true" title={noAccessLabel}>
				{content}
			</div>
		);
	}

	return (
		<Link href={tile.path} className={`${className} transition hover:-translate-y-px hover:shadow-md`}>
			{content}
		</Link>
	);
}
