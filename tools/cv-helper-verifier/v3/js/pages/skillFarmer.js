/* ============================================================================
 * pages/skillFarmer.js — Mining & Woodcutting farmers.
 * Rich layout inspired by the reference mockup: Selected Target Details, a wide
 * candidate table, Inventory & Drop Policy, Candidate Summary stat cards, Run &
 * Status — plus the shared 2D path grid. Mount-once + per-cell patching.
 * ========================================================================== */
import { panel, metric, kvList, table, idChip, badge } from "../components.js";
import { icon, refreshIcons, itemIcon } from "../icons.js";
import { escapeHtml, formatGp, formatRelativeTime, selectedValue, humanizeAction, decisionTone, compass, compassLong } from "../format.js";
import { buildPathGrid, gridSignature, getGridRadius } from "../pathGrid.js";

const arr = (v) => (Array.isArray(v) ? v : []);
const obj = (v) => (v && typeof v === "object" && !Array.isArray(v) ? v : {});
const num = (v) => (typeof v === "number" ? v : undefined);
const yesno = (v) => (v === true ? `<span class="cell-yes">yes</span>` : v === false ? `<span class="cell-skip">no</span>` : "—");
const str = (v) => (typeof v === "string" ? v : "");

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

const tile = (w) => (w ? `${w.x}, ${w.y}` : "—");

/* ---- builders ----------------------------------------------------------- */
function metricStrip(skill, s, sel, inv, player) {
	const action = humanizeAction(s.currentAction || s.decision);
	return `
		${metric({ iconName: skill === "mining" ? "pickaxe" : "trees", label: "Selected", value: escapeHtml(sel.name || "—"), sub: sel.id ? `obj ${idChip(sel.id)}` : "none" })}
		${metric({ iconName: "swords", label: "Action", value: action.label, tone: action.tone, sub: escapeHtml(String(s.currentAction || "")) })}
		${metric({ iconName: "route", label: "Path Distance", value: sel.pathDistance !== undefined ? `${sel.pathDistance} tiles` : "—", tone: sel.reachable === false ? "bad" : "good" })}
		${metric({ iconName: "check-circle", label: "Reachable", value: sel.reachable === true ? "Yes" : sel.reachable === false ? "No" : "—", tone: sel.reachable ? "good" : sel.reachable === false ? "bad" : "" })}
		${metric({ iconName: "radar", label: "Scan Radius", value: `${selectedValue(s.scanRadiusTiles, "—")} tiles` })}
		${metric({ iconName: "backpack", label: "Free Slots", value: `${selectedValue(inv.freeSlots, "—")} / ${selectedValue(inv.slotCount, 28)}`, tone: inv.full ? "warn" : "good" })}
		${metric({ iconName: "coins", label: "Inventory GE", value: formatGp(num(inv.gePrice)), tone: "gold" })}
		${metric({ iconName: "wand-2", label: "Inventory HA", value: formatGp(num(inv.haPrice)), tone: "gold" })}`;
}

function selectedDetails(sel) {
	if (!sel.name) return `<p class="empty compact">No object selected.</p>`;
	return kvList([
		{ iconName: "box", k: "Object", v: escapeHtml(sel.name), tone: "gold" },
		{ iconName: "hash", k: "ID", v: idChip(sel.id) || "—" },
		{ iconName: "map-pin", k: "World Tile", v: `${tile(sel.worldLocation)}${sel.worldLocation ? `, ${sel.worldLocation.plane ?? 0}` : ""}` },
		{ iconName: "ruler", k: "Straight Distance", v: sel.distance !== undefined ? `${sel.distance} tiles` : "—" },
		{ iconName: "route", k: "Path Distance", v: sel.pathDistance !== undefined ? `${sel.pathDistance} tiles` : "—" },
		{ iconName: "check-circle", k: "Reachable", v: sel.reachable === true ? "Yes" : sel.reachable === false ? "No" : "—", tone: sel.reachable ? "good" : sel.reachable === false ? "bad" : "" },
		{ iconName: "eye", k: "Visible", v: sel.visible === true ? "Yes" : sel.visible === false ? "No" : "—", tone: sel.visible ? "good" : "" },
		{ iconName: "swords", k: "Action", v: escapeHtml(selectedValue(sel.action || sel.menuAction, "—")) },
		{ iconName: "tag", k: "Match Type", v: escapeHtml(selectedValue(sel.matchType, "—")), tone: sel.targetMatched ? "good" : "" },
	]);
}

function candidateTable(candidates, sel) {
	const rows = candidates.slice(0, 80).map((c, i) => {
		const isSel = sel && c.id === sel.id && c.worldLocation && sel.worldLocation && c.worldLocation.x === sel.worldLocation.x && c.worldLocation.y === sel.worldLocation.y;
		const decision = isSel ? `<span class="cell-take">Select</span>` : c.selectable ? `<span class="cell-yes">ok</span>` : `<span class="cell-skip">Skip</span>`;
		return {
			cls: isSel ? "row-sel" : c.reachable === false ? "row-bad" : c.selectable ? "row-good" : "",
			cells: [
				`${i + 1}`,
				escapeHtml(c.name || c.label || "object"),
				idChip(c.id) || "—",
				escapeHtml(tile(c.worldLocation)),
				escapeHtml(selectedValue(c.distance, "—")),
				escapeHtml(selectedValue(c.pathDistance, "—")),
				yesno(c.reachable),
				yesno(c.visible),
				c.depleted === undefined ? "—" : yesno(!c.depleted),
				isSel ? `<span class="cell-yes">✓</span>` : "",
				decision,
				`<span class="muted">${escapeHtml(arr(c.reasons).join(", ") || (c.targetMatched ? "match" : "—"))}</span>`,
			],
		};
	});
	return table({ columns: [
		{ label: "#" }, { label: "Object" }, { label: "ID" }, { label: "World Tile" }, { label: "Str" }, { label: "Path" },
		{ label: "Reach" }, { label: "Vis" }, { label: "Live" }, { label: "Sel" }, { label: "Decision" }, { label: "Reason" },
	], rows, empty: "No candidates in scan radius." });
}

function parsePolicyList(v) {
	const value = str(v).trim();
	if (!value) return [];
	return value.split(/[|,;\r\n]+/).map((token) => token.trim()).filter(Boolean);
}

function policyChip(token, type) {
	const allowed = type === "allow";
	const label = allowed ? "Allowed to drop" : "Protected / never drop";
	return `<span class="policy-chip ${allowed ? "allow" : "protect"}" title="${escapeHtml(label)}: ${escapeHtml(token)}">${icon(allowed ? "check" : "shield")}<span class="policy-label">${escapeHtml(token)}</span></span>`;
}

function policyChips(dp) {
	const allow = parsePolicyList(dp.configuredAllowlist);
	const protect = parsePolicyList(dp.configuredProtected);
	const allowSection = allow.length
		? `<div class="policy-h">${icon("check")} Allowed to drop (${allow.length})</div><div class="policy-chips">${allow.map((token) => policyChip(token, "allow")).join("")}</div>`
		: `<div class="policy-h">${icon("check")} Allowed to drop</div><p class="policy-empty">Any non-protected item below the maximum value is droppable.</p>`;
	const visibleProtected = protect.slice(0, 16);
	const protectSection = protect.length
		? `<div class="policy-h">${icon("shield")} Protected / never drop (${protect.length})</div><div class="policy-chips">${visibleProtected.map((token) => policyChip(token, "protect")).join("")}${protect.length > visibleProtected.length ? `<span class="policy-more">+${protect.length - visibleProtected.length} more</span>` : ""}</div>`
		: `<div class="policy-h">${icon("shield")} Protected / never drop</div><p class="policy-empty">No configured list; built-in safeguards still apply.</p>`;
	return `<div class="policy-lists">${allowSection}${protectSection}</div>`;
}

function dropPolicy(dp, inv) {
	const cands = arr(dp.candidates);
	const status = dp.enabled ? badge("Active", "good") : badge("Idle", "");
	const rows = kvList([
		{ iconName: "settings", k: "Mode", v: escapeHtml(selectedValue(dp.mode, "—")), tone: "gold" },
		{ iconName: "sliders-horizontal", k: "Trigger (free ≤)", v: selectedValue(dp.thresholdSlots, "—") },
		{ iconName: "backpack", k: "Current Free", v: `${selectedValue(inv.freeSlots, "—")} / ${selectedValue(inv.slotCount, 28)}` },
		{ iconName: "activity", k: "Opportunity", v: escapeHtml(selectedValue(dp.opportunity, "None")), tone: decisionTone(dp.opportunity) },
		{ iconName: "shield", k: "Protected", v: `${arr(inv.protectedSlots).length} slots` },
		{ iconName: "alert-triangle", k: "Last Result", v: escapeHtml(selectedValue(dp.lastFailureReason || dp.decision, "—")), tone: decisionTone(dp.lastFailureReason) },
	]);
	const dropList = cands.length
		? `<div class="drop-cands"><div class="drop-cands-h">Drop Candidates (${cands.length})</div>${cands.slice(0, 5).map((d) => {
			const itemId = d.id ?? d.itemId;
			const itemIconHtml = itemIcon(itemId, d.name, "sm");
			return `<div class="drop-cand"><span class="dc-name">${itemIconHtml} ${escapeHtml(d.name || "item")} <small>${idChip(itemId)}</small></span><span class="dc-meta">×${escapeHtml(d.quantity ?? 1)} · ${formatGp(num(d.gePriceEach) ?? num(d.gePrice))}</span></div>`;
		}).join("")}</div>`
		: `<p class="empty compact">No drop candidates.</p>`;
	return `<div class="dp-head"><span class="gilt-label">Policy Status</span>${status}</div>${rows}${policyChips(dp)}${dropList}`;
}

function summaryCards(sm, candidates) {
	const depleted = candidates.filter((c) => c.depleted === true).length;
	const cards = [
		{ label: "Total Found", v: sm.totalCandidates ?? candidates.length, tone: "" },
		{ label: "Selectable", v: sm.matchedReachable ?? candidates.filter((c) => c.selectable).length, tone: "good" },
		{ label: "Mismatch", v: sm.targetMismatches ?? "—", tone: "" },
		{ label: "Unreachable", v: sm.matchedUnreachable ?? "—", tone: "bad" },
		{ label: "Depleted", v: depleted, tone: "warn" },
		{ label: "No Action", v: sm.missingAction ?? "—", tone: "bad" },
	];
	return `<div class="sum-grid">${cards.map((c) => `<div class="sum-card ${c.tone}"><div class="sum-v">${escapeHtml(String(c.v))}</div><div class="sum-l">${escapeHtml(c.label)}</div></div>`).join("")}</div>`;
}

function runStatus(s, player) {
	const vitals = obj(player?.vitals);
	const energy = num(vitals.runEnergyPercent) ?? (num(vitals.runEnergyRaw) !== undefined ? Math.round(vitals.runEnergyRaw / 100) : undefined);
	const energyBar = energy !== undefined ? `<div class="m-bar" style="margin:6px 0 10px"><i style="width:${energy}%;background:var(--good)"></i></div>` : "";
	return `
		<div class="run-energy"><span class="gilt-label">Run Energy</span><span class="run-pct">${energy !== undefined ? energy + "%" : "—"}</span></div>
		${energyBar}
		${kvList([
			{ iconName: "zap", k: "Auto Run", v: vitals.runEnabled ? "On" : "Off", tone: vitals.runEnabled ? "good" : "" },
			{ iconName: "globe", k: "World", v: selectedValue(player?.world, "—") },
			{ iconName: "map-pin", k: "Player Tile", v: tile(player?.worldLocation) },
			{ iconName: "gamepad-2", k: "Game State", v: escapeHtml(player?.gameState || "—"), tone: player?.loggedIn ? "good" : "warn" },
		])}`;
}

function pathingBody(skill, s, candidates, sel, player, tileGrid) {
	const loc = player?.worldLocation;
	const selCand = candidates.find((c) => c.id === sel.id && c.worldLocation && sel.worldLocation && c.worldLocation.x === sel.worldLocation.x && c.worldLocation.y === sel.worldLocation.y) || null;
	let dir = "—";
	if (loc && sel.worldLocation) dir = compassLong(compass(sel.worldLocation.x - loc.x, sel.worldLocation.y - loc.y));
	const cells = [
		{ k: "Reachable", v: sel.reachable === true ? "Yes ✓" : sel.reachable === false ? "No ✗" : "—", tone: sel.reachable ? "good" : sel.reachable === false ? "bad" : "" },
		{ k: "Path dist", v: selectedValue(sel.pathDistance, "—") },
		{ k: "Next step", v: dir, tone: dir !== "—" ? "good" : "" },
		{ k: "Matches", v: `${num(s.candidateSummary?.targetMatches) ?? "—"} / ${candidates.length}`, tone: "good" },
	];
	return `
		<div class="path-info-grid">${cells.map((r) => `<div class="pi-cell"><span class="pi-k">${escapeHtml(r.k)}</span><span class="pi-v ${r.tone || ""}">${r.v}</span></div>`).join("")}</div>
		<div class="path-grid-box"><div class="compass">N</div>${buildPathGrid(loc, candidates, selCand, null, tileGrid)}</div>
		<div class="path-legend"><span><i class="lg-you"></i>You</span><span><i class="lg-reach"></i>Reachable</span><span><i class="lg-target"></i>Selected</span><span><i class="lg-obstacle"></i>Obstacle</span><span><i class="lg-unreachable"></i>Unreachable / no-action</span><span><i class="lg-door-pending"></i>Pending door</span><span><i class="lg-center"></i>Object centre</span></div>`;
}

function zoomControls() {
	return `<span class="zoom-ctl"><button class="zoom-btn" data-grid-zoom="out" title="Zoom out">−</button><span class="zoom-val">${getGridRadius() * 2 + 1}</span><button class="zoom-btn" data-grid-zoom="in" title="Zoom in">+</button></span>`;
}

/* ---- mount + render ----------------------------------------------------- */
function mount(skill) {
	const el = document.querySelector(`#${skill}-overview`);
	if (!el || el.dataset.mounted === "1") return;
	el.dataset.mounted = "1";
	const id = (n) => `${skill}-c-${n}`;
	el.innerHTML = `
		<div class="metric-strip" id="${id("metrics")}"></div>
		<div class="skill-row1">
			${panel({ title: "Selected Target Details", iconName: "crosshair", body: `<div id="${id("selected")}"></div>` })}
			${panel({ title: "Candidates", iconName: "list", extra: `<span id="${id("candcount")}"></span>`, flush: true, body: `<div id="${id("candidates")}"></div>` })}
			${panel({ title: "Inventory & Drop Policy", iconName: "package", body: `<div id="${id("droppolicy")}"></div>` })}
		</div>
		<div class="skill-row2">
			${panel({ title: "Pathing / Reachability", iconName: "compass", extra: zoomControls(), body: `<div id="${id("pathing")}"></div>` })}
			${panel({ title: "Candidate Summary", iconName: "list", body: `<div id="${id("summary")}"></div>` })}
			${panel({ title: "Recent Activity", iconName: "scroll-text", className: "activity-panel", body: `<div id="${id("events")}" class="activity-body"></div>` })}
			${panel({ title: "Run & Status", iconName: "heart-pulse", body: `<div id="${id("run")}"></div>` })}
		</div>`;
	refreshIcons();
}

export function renderSkillFarmer(skill, status, player, events, tileGrid) {
	const el = document.querySelector(`#${skill}-overview`);
	if (!el) return { running: false, live: false };
	if (!status || status.unavailable) {
		el.dataset.mounted = "";
		el.innerHTML = panel({ title: `${skill} unavailable`, iconName: "alert-triangle", body: `<p class="scaffold-note">${escapeHtml(status?.path || `/automation/${skill}/status`)} is not exposed on this build.</p>` });
		return { running: false, live: false };
	}
	mount(skill);
	const s = obj(status);
	const candidates = arr(s.candidates);
	const sel = obj(s.selected);
	const inv = obj(s.inventory);
	const dp = obj(s.dropPolicy);
	const sm = obj(s.candidateSummary);
	const id = (n) => `${skill}-c-${n}`;

	let changed = false;
	changed |= setCell(id("metrics"), metricStrip(skill, s, sel, inv, player));
	changed |= setCell(id("selected"), selectedDetails(sel));
	changed |= setCell(id("candidates"), candidateTable(candidates, sel));
	changed |= setCell(id("candcount"), `${candidates.length} found`);
	changed |= setCell(id("droppolicy"), dropPolicy(dp, inv));
	changed |= setCell(id("summary"), summaryCards(sm, candidates));
	changed |= setCell(id("events"), (events && events.length) ? `<div class="events">${events.slice(0, 16).map((e) => `<div class="ev-row"><span class="ev-t">${escapeHtml(e.time instanceof Date ? e.time.toLocaleTimeString() : "")}</span><span class="ev-d"></span><span class="ev-m">${escapeHtml(e.message)}</span></div>`).join("")}</div>` : `<p class="empty compact">No events yet.</p>`);
	changed |= setCell(id("run"), runStatus(s, player));
	changed |= setCellSig(id("pathing"), gridSignature(player?.worldLocation, candidates, sel.worldLocation ? sel : null, null, tileGrid) + ":" + skill, () => pathingBody(skill, s, candidates, sel, player, tileGrid));
	if (changed) refreshIcons();

	return { running: Boolean(s.running), live: Boolean(s.live), action: humanizeAction(s.currentAction) };
}
