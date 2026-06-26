/* ============================================================================
 * pages/minimapView.js — standalone Minimap / reachability grid.
 *
 * Unlike the embedded grids on the Mob/Mining/Woodcutting pages (which only show
 * what that farmer currently selects), this view works independent of any farmer
 * running and lets you pick a "lens":
 *   - scene: every named object within radius, no target filter (/scene/diagnostics)
 *   - mob / mining / woodcutting: that farmer's own candidate list, but rendered
 *     here so the grid is visible even while the farmer is stopped
 * The base reachability layer (/pathing/grid) is always independent of any farmer.
 * ========================================================================== */
import { panel, kvList, badge } from "../components.js";
import { objectIcon, refreshIcons } from "../icons.js";
import { escapeHtml, selectedValue } from "../format.js";
import { buildPathGrid, gridSignature, getGridRadius } from "../pathGrid.js";

const root = () => document.querySelector("#minimap-page");
const arr = (v) => (Array.isArray(v) ? v : []);
const obj = (v) => (v && typeof v === "object" && !Array.isArray(v) ? v : {});

const LENS_LABEL = { scene: "Scene (unfiltered)", mob: "Mob Farmer", mining: "Mining Farmer", woodcutting: "Woodcutting Farmer" };
const LENS_ICON = { scene: "layers", mob: "skull", mining: "pickaxe", woodcutting: "trees" };

function setCell(id, html) {
	const el = document.getElementById(id);
	if (!el || el.__html === html) return false;
	el.__html = html; el.innerHTML = html; return true;
}
function setCellSig(id, sig, build) {
	const el = document.getElementById(id);
	if (!el || el.__sig === sig) return false;
	el.__sig = sig; el.innerHTML = build(); return true;
}

function mount() {
	const el = root();
	if (!el || el.dataset.mounted === "1") return;
	el.dataset.mounted = "1";
	el.innerHTML = `
		<div class="mf-top">
			<div class="mf-top-left" style="grid-template-columns:1fr">
				${panel({ title: "Lens & Scan Info", iconName: "info", body: `<div id="minimap-info"></div>` })}
				${panel({ title: "Candidates in view", iconName: "list", extra: `<span id="minimap-candcount"></span>`, flush: true, body: `<div id="minimap-candidates"></div>` })}
			</div>
			<div class="mf-top-right">
				${panel({ title: "Reachability Grid", iconName: "compass", body: `<div class="path-grid-box"><div class="compass">N</div><div id="minimap-grid"></div></div><div class="path-legend" id="minimap-legend"></div>` })}
			</div>
		</div>`;
	refreshIcons();
}

function infoBody(lens, data, player, farmerRunning) {
	const sm = obj(data?.candidateSummary);
	const rows = [
		{ iconName: LENS_ICON[lens], k: "Lens", v: escapeHtml(LENS_LABEL[lens] || lens), tone: "gold" },
		{ iconName: "map-pin", k: "Player Tile", v: player?.x !== undefined ? `${player.x}, ${player.y}` : "—" },
		{ iconName: "radar", k: "Scan Radius", v: `${selectedValue(data?.radius ?? data?.scanRadiusTiles, getGridRadius())} tiles` },
		{ iconName: "list", k: "Total Found", v: selectedValue(sm.totalCandidates, arr(data?.candidates).length) },
		{ iconName: "check-circle", k: "Reachable", v: selectedValue(sm.matchedReachable, "—"), tone: "good" },
	];
	if (lens !== "scene") {
		rows.push({ iconName: "activity", k: "Farmer State", v: farmerRunning === undefined ? "—" : farmerRunning ? "Running" : "Stopped (grid still live)", tone: farmerRunning ? "good" : "warn" });
	} else {
		rows.push({ iconName: "info", k: "Filter", v: "None — every named object", tone: "good" });
	}
	return kvList(rows);
}

/** First reason code, or a plain status word when there's nothing to reject. */
function statusBadge(c) {
	const reasons = Array.isArray(c.reasons) ? c.reasons : [];
	if (reasons.length) {
		const r = reasons[0];
		const tone = /depleted|stale/.test(r) ? "warn" : /unreachable|missing-action|target-mismatch/.test(r) ? "bad" : "";
		return `<span class="badge ${tone}" title="${escapeHtml(reasons.join(", "))}">${escapeHtml(r)}</span>`;
	}
	if (c.selectable === true) return `<span class="badge good">valid</span>`;
	if (c.visible === false) return `<span class="badge">not visible</span>`;
	return `<span class="badge">evaluated</span>`;
}

function candidateRow(c) {
	const reach = c.reachable === true ? `<span class="cell-yes">yes</span>` : c.reachable === false ? `<span class="cell-skip">no</span>` : "—";
	return `<div class="qc-row">${objectIcon(c.id, c.name || c.label, "sm")}<span class="qc-label">${escapeHtml(c.name || c.label || "object")} <small class="muted">#${escapeHtml(String(c.id ?? "—"))}</small></span><span class="muted" style="margin-right:8px">${selectedValue(c.distance, "—")} tiles</span>${statusBadge(c)}<span style="width:8px;display:inline-block"></span>${reach}</div>`;
}

function candidatesBody(candidates) {
	if (!candidates.length) return `<p class="empty compact">No objects in range at this radius.</p>`;
	return candidates.slice(0, 60).map(candidateRow).join("");
}

function legendHtml(lens) {
	const base = `<span><i class="lg-you"></i>You</span><span><i class="lg-reach"></i>Reachable</span><span><i class="lg-obstacle"></i>Blocked / no-action</span><span><i class="lg-center"></i>Object centre</span>`;
	return lens === "scene"
		? `${base}<span><i class="lg-target"></i>Scanned object</span>`
		: `${base}<span><i class="lg-target"></i>Selected</span>`;
}

/**
 * @param lens "scene"|"mob"|"mining"|"woodcutting"
 * @param data the lens's own status/diagnostics payload ({candidates, selected?, scanRadiusTiles|radius, candidateSummary, player?})
 * @param player live player.worldLocation from /status (always available even if a farmer is stopped)
 * @param farmerRunning boolean|undefined (undefined for scene lens)
 * @param tileGrid the shared /pathing/grid payload
 */
export function renderMinimapView(lens, data, player, farmerRunning, tileGrid) {
	mount();
	const candidates = arr(data?.candidates);
	const selected = obj(data?.selected);
	const loc = data?.player || player;

	let changed = false;
	changed |= setCell("minimap-info", infoBody(lens, data, loc, farmerRunning));
	changed |= setCell("minimap-candidates", candidatesBody(candidates));
	changed |= setCell("minimap-candcount", `${candidates.length} found`);
	changed |= setCellSig(
		"minimap-grid",
		gridSignature(loc, candidates, selected.worldLocation ? selected : null, null, tileGrid) + ":" + lens,
		() => buildPathGrid(loc, candidates, selected.worldLocation ? selected : null, null, tileGrid),
	);
	changed |= setCell("minimap-legend", legendHtml(lens));
	const badgeEl = document.querySelector("#minimap-lens-badge");
	if (badgeEl) badgeEl.innerHTML = badge(LENS_LABEL[lens] || lens, "gold");
	if (changed) refreshIcons();
}
