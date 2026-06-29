/* ============================================================================
 * icons.js — icon source for v3.
 *
 * Signature "emblem" icons (the RuneScape-flavoured ones that define the theme:
 * skull, pickaxe, willow, crest, swords, coins, shield, scroll, heart, wand,
 * crosshair, package) are hand-authored inline SVG baked in below — a real
 * vector sprite sheet, no external assets to cut. Every other (utility) glyph
 * falls through to Lucide (loaded via CDN in index.html), which matches the
 * reference's clean line-icon header glyphs.
 *
 * To swap in a generated raster sheet later, only this file changes.
 * ========================================================================== */
import { escapeHtml } from "./format.js";

// Asset library path - relative to v3 directory
// Asset library is copied to v3/asset-library for serving
const ASSET_BASE = "asset-library";

// 24x24 viewBox, stroke = currentColor, matches Lucide weight so the two blend.
const S = (inner, { fill = "none", w = 1.7 } = {}) =>
	`<svg class="ico" viewBox="0 0 24 24" fill="${fill}" stroke="currentColor" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${inner}</svg>`;

const EMBLEMS = {
	skull: S(`
		<path d="M5 10.5a7 7 0 0 1 14 0c0 2.2-.9 3.6-1.7 4.4-.5.5-.8 1.1-.8 1.8v.6a1.4 1.4 0 0 1-1.4 1.4h-6.2A1.4 1.4 0 0 1 7.5 17.3v-.6c0-.7-.3-1.3-.8-1.8C5.9 14.1 5 12.7 5 10.5Z"/>
		<circle cx="9.2" cy="11" r="1.7" fill="currentColor" stroke="none"/>
		<circle cx="14.8" cy="11" r="1.7" fill="currentColor" stroke="none"/>
		<path d="M12 13.7l-.9 1.8h1.8z" fill="currentColor" stroke="none"/>
		<path d="M9.5 19.5v1.6M12 19.5v1.6M14.5 19.5v1.6"/>`),
	pickaxe: S(`
		<path d="M3 7c4-3 8-3 9-1m9 1c-4-3-8-3-9-1"/>
		<path d="M12 6l0 2"/>
		<path d="M11.4 8.2 5 20.2a1 1 0 0 0 1.7 1l6-11.2z" fill="currentColor" stroke="none"/>
		<path d="M11.4 8.2 5 20.2a1 1 0 0 0 1.7 1l6-11.2"/>`),
	trees: S(`
		<path d="M12 3c-2.4 2-3.5 4-3.5 5.8 0 1 .5 1.8 1.4 2.2-1.6.4-2.6 1.6-2.6 3 0 1.2.8 2.2 2 2.6-1 .4-1.7 1.2-1.7 2.2 0 1.2 1.1 2 2.9 2"/>
		<path d="M12 3c2.4 2 3.5 4 3.5 5.8 0 1-.5 1.8-1.4 2.2 1.6.4 2.6 1.6 2.6 3 0 1.2-.8 2.2-2 2.6 1 .4 1.7 1.2 1.7 2.2 0 1.2-1.1 2-2.9 2"/>
		<path d="M12 11v10"/>`),
	compass: S(`
		<circle cx="12" cy="12" r="9"/>
		<path d="M15.5 8.5 13 13l-4.5 2.5L11 11z" fill="currentColor" stroke="none"/>
		<path d="M12 3v2M12 19v2M3 12h2M19 12h2"/>`),
	swords: S(`
		<path d="M14.5 3H20v5.5l-8 8"/>
		<path d="M9.5 3H4v5.5l8 8"/>
		<path d="M12 16.5 9 19.5M12 16.5l3 3"/>
		<path d="M7 17l-2 2M17 17l2 2"/>`),
	coins: S(`
		<ellipse cx="9" cy="7" rx="5" ry="2.4"/>
		<path d="M4 7v3c0 1.3 2.2 2.4 5 2.4s5-1.1 5-2.4V7"/>
		<ellipse cx="15" cy="13" rx="5" ry="2.4"/>
		<path d="M10 13v3c0 1.3 2.2 2.4 5 2.4s5-1.1 5-2.4v-3"/>`),
	shield: S(`<path d="M12 3 5 6v5c0 4.2 2.9 7.6 7 9 4.1-1.4 7-4.8 7-9V6z"/><path d="M9.5 12l1.8 1.8 3.4-3.6"/>`),
	"shield-half": S(`<path d="M12 3 5 6v5c0 4.2 2.9 7.6 7 9 4.1-1.4 7-4.8 7-9V6z"/><path d="M12 3v17"/>`),
	scroll: S(`
		<path d="M6 4h11a2 2 0 0 1 2 2v10"/>
		<path d="M6 4a2 2 0 0 0-2 2v1h4"/>
		<path d="M8 7v11a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2 2 2 0 0 0-2-2H9"/>
		<path d="M11 9h5M11 12h5"/>`),
	"scroll-text": S(`
		<path d="M6 4h11a2 2 0 0 1 2 2v10"/>
		<path d="M6 4a2 2 0 0 0-2 2v1h4"/>
		<path d="M8 7v11a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2 2 2 0 0 0-2-2H9"/>
		<path d="M11 9h5M11 12h5M11 15h3"/>`),
	heart: S(`<path d="M12 20s-7-4.3-9-9c-1.2-2.8.4-5.8 3.4-6 1.9-.1 3.1 1 3.6 2 .5-1 1.7-2.1 3.6-2 3 .2 4.6 3.2 3.4 6-2 4.7-9 9-9 9Z"/>`),
	"heart-pulse": S(`<path d="M12 20s-7-4.3-9-9c-1.2-2.8.4-5.8 3.4-6 1.9-.1 3.1 1 3.6 2 .5-1 1.7-2.1 3.6-2 3 .2 4.6 3.2 3.4 6-2 4.7-9 9-9 9Z"/><path d="M3.5 12h3l1.5-2.5L10 14l1.5-3 1 1.5h3"/>`),
	wand: S(`<path d="M15 4 5 14l-2 6 6-2L19 8z"/><path d="M14 5l3 3"/><path d="M18 3l.5 1.5L20 5l-1.5.5L18 7l-.5-1.5L16 5l1.5-.5z" fill="currentColor" stroke="none"/>`),
	"wand-2": S(`<path d="M15 4 5 14l-2 6 6-2L19 8z"/><path d="M14 5l3 3"/><path d="M18 3l.5 1.5L20 5l-1.5.5L18 7l-.5-1.5L16 5l1.5-.5z" fill="currentColor" stroke="none"/>`),
	crosshair: S(`<circle cx="12" cy="12" r="6.5"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/><circle cx="12" cy="12" r="1.6" fill="currentColor" stroke="none"/>`),
	package: S(`<path d="M12 3 4 7v10l8 4 8-4V7z"/><path d="M4 7l8 4 8-4M12 11v10"/>`),
	backpack: S(`<path d="M7 9a5 5 0 0 1 10 0v9a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2z"/><path d="M9 7a3 3 0 0 1 6 0"/><path d="M9 13h6v4H9z"/>`),
	pickaxe2: S(``),
};

// Emblem names that have a real OSRS skill-icon sprite available -- when one
// matches, the authentic in-game icon replaces the hand-drawn placeholder
// everywhere that emblem is used (nav rail, page headers, macro panel), no
// per-call-site changes needed.
const SKILL_ICON_ALIASES = { pickaxe: "mining", trees: "woodcutting" };

export function icon(name, extraClass = "") {
	const cls = extraClass ? ` ${escapeHtml(extraClass)}` : "";
	const skillAlias = SKILL_ICON_ALIASES[name];
	if (skillAlias) return skillIcon(skillAlias, extraClass);
	const custom = EMBLEMS[name];
	if (custom) return `<span class="ico-wrap${cls}">${custom}</span>`;
	return `<i data-lucide="${escapeHtml(name)}"${extraClass ? ` class="${escapeHtml(extraClass)}"` : ""}></i>`;
}

/** Authentic OSRS skill-icon sprite (mining.png, woodcutting.png, attack.png, ...). */
export function skillIcon(skill, extraClass = "") {
	const cls = extraClass ? ` ${escapeHtml(extraClass)}` : "";
	const safeSkill = escapeHtml(skill || "");
	return `<img src="${ASSET_BASE}/skill-icons/${safeSkill}.png" alt="${safeSkill}" class="skill-icon${cls}" onerror="this.style.display='none'"/>`;
}

/**
 * Render an item icon from the asset library.
 * Falls back to a placeholder if the icon doesn't exist.
 * @param {number} itemId - The item ID
 * @param {string} name - The item name (for alt text)
 * @param {string} extraClass - Additional CSS classes
 * @returns {string} HTML for the item icon
 */
export function itemIcon(itemId, name = "", extraClass = "") {
	const cls = extraClass ? ` ${escapeHtml(extraClass)}` : "";
	const safeName = escapeHtml(name || "item");
	const iconPath = `${ASSET_BASE}/items/${itemId}.png`;
	
	// Use an img with error handling to show placeholder if icon doesn't exist
	return `<img 
		src="${iconPath}" 
		alt="${safeName}" 
		class="item-icon${cls}" 
		onerror="this.style.display='none'; this.nextElementSibling.style.display='inline';"
	/><span class="item-icon-fallback" style="display:none">?</span>`;
}

/**
 * Map a GP value to the OSRS coin stack visual item ID.
 * OSRS uses different item models for different stack sizes.
 * @param {number} value - GP value
 * @returns {number} Coin item ID
 */
export function gpCoinItemId(value) {
	if (typeof value !== "number") return 995;
	const abs = Math.abs(value);
	if (abs >= 250_000_000) return 1004; // 10000 coins pile (250m+)
	if (abs >= 1_000_000) return 1003; // 1000 coins pile (1m-249m)
	if (abs >= 100_000) return 1002; // 250 coins pile (100k-999k)
	if (abs >= 25_000) return 1001; // 100 coins pile (25k-99k)
	if (abs >= 1_000) return 1000; // 25 coins pile (1k-24k)
	if (abs >= 2) return 999; // 5 coins pile (2-999)
	return 995; // 1 coin
}

/**
 * Render a GP/coin icon using the actual game coin stack item for the given value.
 * Falls back to the coins emblem if the asset doesn't exist.
 * @param {number} value - GP value to determine stack size
 * @param {string} extraClass - Additional CSS classes
 * @returns {string} HTML for the GP icon
 */
export function gpIcon(value = 0, extraClass = "") {
	const cls = extraClass ? ` ${escapeHtml(extraClass)}` : "";
	const itemId = gpCoinItemId(value);
	const iconPath = `${ASSET_BASE}/items/${itemId}.png`;
	
	// Try to use the actual coin item sprite, fall back to emblem
	return `<img 
		src="${iconPath}" 
		alt="GP" 
		class="gp-icon${cls}" 
		onerror="this.style.display='none'; this.nextElementSibling.style.display='inline';"
	/><span class="gp-icon-fallback" style="display:none">${icon("coins", "")}</span>`;
}

/**
 * Render an object icon from the asset library.
 * Falls back to a placeholder if the icon doesn't exist.
 * @param {number} objectId - The object ID
 * @param {string} name - The object name (for alt text)
 * @param {string} extraClass - Additional CSS classes
 * @returns {string} HTML for the object icon
 */
const MINING_PRESET_FALLBACKS = {
	"clay rocks": "clay",
	"copper rocks": "copper-tin",
	"tin rocks": "copper-tin",
	"blurite rocks": "blurite",
	"iron rocks": "iron",
	"iron ore vein": "iron",
	"silver rocks": "silver",
	"coal rocks": "coal",
	"coal ore vein": "coal",
	"gold rocks": "gold",
	"gold vein": "gold",
	"mithril rocks": "mithril",
	"mithril ore vein": "mithril",
	"adamantite rocks": "adamantite",
	"adamant ore vein": "adamantite",
	"runite rocks": "runite",
	"amethyst crystals": "amethyst",
	"gem rocks": "gem-rocks",
	"granite rocks": "granite",
	"sandstone rocks": "sandstone",
	"lovakite rocks": "lovakite",
	"daeyalt rocks": "daeyalt",
	"daeyalt essence": "daeyalt",
	"limestone rock": "limestone",
	"limestone rocks": "limestone",
	"volcanic sulphur": "volcanic-sulphur",
	"rune essence": "rune-essence",
	"pure essence": "pure-essence",
	"ancient essence crystals": "ancient-essence",
	"ancient essence": "ancient-essence"
};

const WOODCUTTING_PRESET_FALLBACKS = {
	"tree": "tree",
	"oak": "oak",
	"willow": "willow",
	"maple": "maple",
	"yew": "yew",
	"magic": "magic",
	"redwood": "redwood",
	"dead tree": "dead-tree",
	"dying tree": "dying-tree",
	"evergreen": "evergreen",
	"jungle tree": "jungle-tree",
	"teak": "teak",
	"mahogany": "mahogany",
	"sulliusceps": "sulliusceps",
	"cursed magic": "cursed-magic",
	"arctic pine": "arctic-pine",
	"blisterwood": "blisterwood",
	"cave oak": "cave-oak",
	"cave willow": "cave-willow",
	"cave maple": "cave-maple"
};

export function objectIcon(objectId, name = "", extraClass = "") {
	const cls = extraClass ? ` ${escapeHtml(extraClass)}` : "";
	const safeName = escapeHtml(name || "object");
	const iconPath = `${ASSET_BASE}/objects/${objectId}.png`;
	const lowerName = (name || "").toLowerCase();

	// Choose a context-aware fallback icon based on the object name
	let fallbackIcon = "box";
	if (lowerName.includes("rock") || lowerName.includes("ore") || lowerName.includes("min")) {
		fallbackIcon = "pickaxe";
	} else if (lowerName.includes("tree") || lowerName.includes("oak") || lowerName.includes("willow") || lowerName.includes("maple") || lowerName.includes("yew") || lowerName.includes("magic") || lowerName.includes("wood")) {
		fallbackIcon = "trees";
	} else if (lowerName.includes("fishing") || lowerName.includes("fish") || lowerName.includes("spot")) {
		fallbackIcon = "fish";
	}

	// Try per-object sprite first, then sheet-based preset sprite for known mining/woodcutting objects, then Lucide fallback
	let presetPath = "";
	if (MINING_PRESET_FALLBACKS[lowerName]) {
		presetPath = `${ASSET_BASE}/mining-presets/mining-preset-${MINING_PRESET_FALLBACKS[lowerName]}.png`;
	} else if (WOODCUTTING_PRESET_FALLBACKS[lowerName]) {
		presetPath = `${ASSET_BASE}/woodcutting-presets/woodcutting-preset-${WOODCUTTING_PRESET_FALLBACKS[lowerName]}.png`;
	}
	const onerror = presetPath
		? `if (this.dataset.fallback) { this.style.display='none'; this.nextElementSibling.style.display='inline'; } else { this.dataset.fallback='1'; this.src='${presetPath}'; }`
		: `this.style.display='none'; this.nextElementSibling.style.display='inline';`;
	
	return `<img 
		src="${iconPath}" 
		alt="${safeName}" 
		class="object-icon${cls}" 
		onerror="${onerror}"
	/><span class="object-icon-fallback" style="display:none">${icon(fallbackIcon, "")}</span>`;
}

/**
 * Render an NPC icon using a generic user/swords icon.
 * @param {string} extraClass - Additional CSS classes
 * @returns {string} HTML for the NPC icon
 */
export function npcIcon(extraClass = "") {
	const cls = extraClass ? ` ${escapeHtml(extraClass)}` : "";
	return `<span class="npc-icon${cls}">${icon("swords", "")}</span>`;
}

let raf = 0;
export function refreshIcons() {
	if (raf) return;
	raf = window.requestAnimationFrame(() => {
		raf = 0;
		if (window.lucide && window.lucide.createIcons) window.lucide.createIcons();
	});
}
