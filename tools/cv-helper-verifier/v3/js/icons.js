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

export function icon(name, extraClass = "") {
	const cls = extraClass ? ` ${escapeHtml(extraClass)}` : "";
	const custom = EMBLEMS[name];
	if (custom) return `<span class="ico-wrap${cls}">${custom}</span>`;
	return `<i data-lucide="${escapeHtml(name)}"${extraClass ? ` class="${escapeHtml(extraClass)}"` : ""}></i>`;
}

let raf = 0;
export function refreshIcons() {
	if (raf) return;
	raf = window.requestAnimationFrame(() => {
		raf = 0;
		if (window.lucide && window.lucide.createIcons) window.lucide.createIcons();
	});
}
