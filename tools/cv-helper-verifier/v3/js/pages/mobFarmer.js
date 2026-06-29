/* ============================================================================
 * pages/mobFarmer.js — Mob Farmer "Overview".
 *
 * Mount-once skeleton + per-cell patching: each panel body only re-renders when
 * its HTML actually changes (string compare), and the path grid is guarded by a
 * tile signature. This keeps hover tooltips, table scroll, and <details> open
 * state alive across fast polls, and keeps CPU low.
 * ========================================================================== */
import { panel, metric, kvList, table, idChip, gpValue } from "../components.js";
import { icon, refreshIcons, itemIcon, npcIcon } from "../icons.js";
import {
	escapeHtml, formatGp, formatDuration, formatRelativeTime, selectedValue,
	humanizeAction, decisionTone, compass, compassLong, regionId, formatFoodItems,
} from "../format.js";
import { buildPathGrid, gridSignature, getGridRadius } from "../pathGrid.js";

const root = () => document.querySelector("#mf-overview");
const arr = (v) => (Array.isArray(v) ? v : []);
const obj = (v) => (v && typeof v === "object" && !Array.isArray(v) ? v : {});
const num = (v) => (typeof v === "number" ? v : undefined);

const openDetails = new Set();   // expanded <details> keys, preserved across patches

/* ---- cell patching ------------------------------------------------------ */
function setCell(id, html) {
	const el = document.getElementById(id);
	if (!el || el.__html === html) return false;
	el.__html = html;
	el.innerHTML = html;
	return true;
}
function setCellSig(id, sig, build) {
	const el = document.getElementById(id);
	if (!el || el.__sig === sig) return false;
	el.__sig = sig;
	el.innerHTML = build();
	return true;
}

function detailsBlock(key, summaryHtml, bodyHtml, openDefault = false) {
	const open = (openDetails.has(key) || (openDefault && !openDetails.has(key + ":closed"))) ? " open" : "";
	return `<details data-dkey="${key}"${open}><summary class="tbl-summary">${summaryHtml}</summary>${bodyHtml}</details>`;
}

/* ---- helpers ------------------------------------------------------------ */
function scalarRows(source, twoCol = false) {
	const o = obj(source);
	const rows = [];
	for (const [k, v] of Object.entries(o)) {
		if (v === null || v === undefined) continue;
		let value; let tone = "";
		if (Array.isArray(v)) value = `${v.length} ${v.length === 1 ? "item" : "items"}`;
		else if (typeof v === "object") continue;
		else if (typeof v === "boolean") { value = v ? "Yes" : "No"; tone = v ? "good" : ""; }
		else if (typeof v === "string" && (k === "food" || k === "foodItems" || k === "foodList")) { value = formatFoodItems(v); }
		else { value = String(v); tone = decisionTone(v); }
		rows.push({ k: prettyKey(k), v: value, tone });
	}
	return rows.length ? kvList(rows, twoCol) : `<p class="empty compact">No data.</p>`;
}
function prettyKey(k) { return String(k).replace(/([a-z])([A-Z])/g, "$1 $2").replace(/[_-]+/g, " ").replace(/\b\w/g, (c) => c.toUpperCase()); }
function prettyEnum(v) { return String(v).replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()); }

function pickSelected(candidates) {
	if (!candidates.length) return null;
	const fighting = candidates.find((c) => c.engagedWithLocalPlayer === true && c.selectable !== false);
	if (fighting) return fighting;
	const pool = candidates.filter((c) => c.selectable === true);
	const list = pool.length ? pool : candidates.filter((c) => c.reachable !== false);
	if (!list.length) return null;
	return list.reduce((best, c) => {
		const cs = num(c.score) ?? num(c.pathDistance) ?? num(c.distance) ?? 1e9;
		const bs = num(best.score) ?? num(best.pathDistance) ?? num(best.distance) ?? 1e9;
		return cs < bs ? c : best;
	});
}

/* ---- content builders --------------------------------------------------- */
function metricStrip(status, player, selected) {
	const running = Boolean(status.running);
	const action = humanizeAction(status.status);
	const vitals = obj(player?.vitals);
	const hp = obj(vitals.hitpoints);
	const hpCur = num(hp.current) ?? num(hp.boosted) ?? num(hp.value);
	const hpMax = num(hp.max) ?? num(hp.real);
	const hpPct = hpCur !== undefined && hpMax ? Math.round((hpCur / hpMax) * 100) : undefined;
	const inCombat = selected?.engagedWithLocalPlayer === true || /attack|combat|loot/i.test(String(status.status));
	const dist = num(selected?.pathDistance) ?? num(selected?.distance);
	const reachable = selected ? selected.reachable !== false : undefined;
	const rawStatus = String(status.status || "").split("@")[0];
	return `
		${metric({ iconName: "swords", label: "Current Action", value: action.label, tone: action.tone, sub: escapeHtml(rawStatus) })}
		${metric({ iconName: "skull", label: "Target", value: escapeHtml(status.target || "—"), sub: selected ? `npc ${idChip(selected.id)}` : "no candidate" })}
		${metric({ iconName: "map-pin", label: "Target Distance", value: dist !== undefined ? `${dist} tiles` : "—", tone: reachable === false ? "bad" : reachable ? "good" : "", sub: reachable === undefined ? "—" : reachable ? "Reachable ✓" : "Unreachable ✗" })}
		${metric({ iconName: "shield", label: "Combat Status", value: inCombat ? "In Combat" : running ? "Hunting" : "Idle", tone: inCombat ? "good" : "", sub: running ? "loop active" : "stopped" })}
		${metric({ iconName: "heart", label: "Health", value: hpCur !== undefined ? `${hpCur} / ${hpMax ?? "?"}` : "—", tone: hpPct !== undefined && hpPct < 40 ? "bad" : "good", bar: hpPct !== undefined ? { percent: hpPct, color: hpPct < 40 ? "var(--bad)" : "var(--good)" } : null })}
		${metric({ iconName: "timer", label: "Runtime", value: status.uptimeMs ? formatDuration(status.uptimeMs) : "—", sub: "session time" })}`;
}

function intentTone(intent) {
	const s = String(intent || "").toLowerCase();
	if (/attack|loot|eat|heal/.test(s)) return { cls: "up", ico: "arrow-up" };
	if (/skip|ignore/.test(s)) return { cls: "skip", ico: "minus" };
	if (/wait|hold/.test(s)) return { cls: "wait", ico: "pause" };
	if (/block|fail|stop/.test(s)) return { cls: "bad", ico: "x" };
	return { cls: "skip", ico: "minus" };
}
function decisionsTimeline(intents, menuEntries) {
	const items = intents.length ? intents.slice().reverse() : menuEntries.slice().reverse();
	if (!items.length) return `<p class="empty compact">No decisions recorded yet.</p>`;
	return `<div class="timeline">${items.slice(0, 8).map((it) => {
		const label = it.intent || it.option || it.menuAction || "decision";
		const tone = intentTone(label);
		const target = it.target || it.npcName || "";
		const at = it.at ? formatRelativeTime(new Date(it.at)) : "";
		const dist = it.distance !== undefined ? `<span class="tl-tag">@${escapeHtml(it.distance)}</span>` : "";
		return `<div class="tl-item"><span class="tl-time">${escapeHtml(at)}</span><span class="tl-arrow ${tone.cls}">${icon(tone.ico)}</span><span class="tl-text">${escapeHtml(prettyKey(label))}${target ? ` <span class="muted">(${escapeHtml(target)})</span>` : ""}</span>${dist}</div>`;
	}).join("")}</div>`;
}

function quickControls(status) {
	const tog = (label, on, key, ico) => `<div class="qc-row"><span class="qc-ico">${icon(ico)}</span><span class="qc-label">${escapeHtml(label)}</span><label class="toggle"><input type="checkbox" data-cfg-key="${key}" ${on ? "checked" : ""}><span class="track"></span></label></div>`;
	const sel = (label, value, key, options, ico) => `<div class="qc-row span"><span class="qc-ico">${icon(ico)}</span><span class="qc-label">${escapeHtml(label)}</span><select class="select sm" data-cfg-key="${key}">${options.map((o) => `<option value="${o}" ${o === value ? "selected" : ""}>${escapeHtml(prettyEnum(o))}</option>`).join("")}</select></div>`;
	const loot = obj(status.loot); const autoEat = obj(status.autoEat); const autorun = obj(status.autorun);
	return `<div class="qc-grid">
		${tog("Auto Eat", autoEat.enabled, "autoEatEnabled", "heart")}
		${tog("Looting", loot.enabled, "lootEnabled", "package")}
		${tog("Auto Run", autorun.enabled, "autorunEnabled", "zap")}
		${tog("Attack Before Loot", loot.attackBeforeLoot, "attackBeforeLoot", "swords")}
		${tog("Loot In Combat", loot.duringCombat, "lootDuringCombat", "crosshair")}
		${tog("Require LOS", status.requireLineOfSight, "requireLineOfSight", "eye")}
		${sel("Engaged Mode", status.engagedMode, "engagedMode", ["FREE_ONLY", "PREFER_FREE", "ALLOW_ENGAGED"], "users")}
		${sel("Aggro Response", status.aggroResponse, "aggroResponse", ["WAIT", "CONTINUE_ANY_ATTACKER", "IGNORE_IN_MULTI"], "shield")}
	</div>`;
}

function pathingBody(status, player, candidates, selected, tileGrid) {
	const loc = player?.worldLocation;
	const grid = buildPathGrid(loc, candidates, selected, status.pathing, tileGrid);
	let dir = "—";
	if (loc && selected?.worldLocation) dir = compassLong(compass(selected.worldLocation.x - loc.x, selected.worldLocation.y - loc.y));
	const reachable = selected ? selected.reachable !== false : undefined;
	const region = loc ? regionId(loc) : null;
	const reachCount = candidates.filter((c) => c.selectable === true).length;
	const doorCount = arr(status.pathing?.doors).length;
	const cells = [
		{ k: "Reachable", v: reachable === undefined ? "—" : reachable ? "Yes ✓" : "No ✗", tone: reachable ? "good" : reachable === false ? "bad" : "" },
		{ k: "Distance", v: selected?.distance !== undefined ? `${selected.distance} tiles` : "—" },
		{ k: "Path dist", v: selectedValue(selected?.pathDistance, "—") },
		{ k: "Next step", v: dir, tone: dir !== "—" ? "good" : "" },
		{ k: "Obstacles", v: selected?.pathFailureReason ? escapeHtml(selected.pathFailureReason) : "None", tone: selected?.pathFailureReason ? "bad" : "" },
		{ k: "Doors", v: doorCount ? `${doorCount} on route` : "None", tone: doorCount ? "warn" : "" },
		{ k: "Selectable", v: `${reachCount} / ${candidates.length}`, tone: reachCount ? "good" : "" },
		{ k: "Region", v: region ?? "—" },
	];
	return `
		<div class="path-info-grid">${cells.map((r) => `<div class="pi-cell"><span class="pi-k">${escapeHtml(r.k)}</span><span class="pi-v ${r.tone || ""}">${r.v}</span></div>`).join("")}</div>
		<div class="path-grid-box"><div class="compass">N</div>${grid}</div>
		<div class="path-legend"><span><i class="lg-you"></i>You</span><span><i class="lg-path"></i>Target</span><span><i class="lg-target"></i>Selected</span><span><i class="lg-obstacle"></i>Obstacle</span><span><i class="lg-unreachable"></i>Unreachable</span><span><i class="lg-door-pending"></i>Pending door</span><span><i class="lg-door-blocked"></i>Blocked door</span><span><i class="lg-door"></i>Door</span></div>`;
}

function hpDisplay(c) {
	if (typeof c.healthRatio === "number" && typeof c.healthScale === "number" && c.healthScale > 0 && c.healthRatio >= 0) return `${Math.round((c.healthRatio / c.healthScale) * 100)}%`;
	return "—";
}
function targetCandidateTable(candidates, selected) {
	const rows = candidates.slice(0, 60).map((c) => {
		const sel = c === selected;
		const engaged = c.engagedWithLocalPlayer ? `<span class="cell-yes">self</span>` : c.engagedByOther ? `<span class="tone-warn">other</span>` : `<span class="cell-no">no</span>`;
		const los = c.lineOfSightToLocalPlayer === true ? `<span class="cell-yes">yes</span>` : c.lineOfSightToLocalPlayer === false ? `<span class="cell-no">no</span>` : "—";
		return { cls: sel ? "row-sel" : c.selectable ? "row-good" : "row-bad", cells: [`${sel ? icon("chevron-right") + " " : ""}${npcIcon("sm")} ${escapeHtml(c.name || "npc")}`, escapeHtml(c.type || "npc"), idChip(c.id) || "—", escapeHtml(selectedValue(c.distance, "—")), hpDisplay(c), engaged, los, c.selectable ? `<span class="cell-yes">yes</span>` : `<span class="cell-skip">no</span>`, `<span class="muted">${escapeHtml(arr(c.reasons).join(", ") || "—")}</span>`] };
	});
	return table({ columns: [{ label: "Name" }, { label: "Type" }, { label: "ID" }, { label: "Dist" }, { label: "HP" }, { label: "Engaged" }, { label: "LOS" }, { label: "Sel" }, { label: "Reason" }], rows, empty: "No target candidates in range." });
}
function lootCandidateTable(candidates) {
	const rows = candidates.slice(0, 50).map((c) => {
		const take = c.highPriority || c.selectable;
		const decision = c.highPriority ? `<span class="cell-prio">priority</span>` : c.selectable ? `<span class="cell-take">take</span>` : `<span class="cell-skip">skip</span>`;
		const itemIconHtml = itemIcon(c.itemId, c.name, "sm");
		return { cls: take ? "row-good" : "row-bad", cells: [`${itemIconHtml} ${escapeHtml(c.name || "item")} <small>${idChip(c.itemId)} ×${escapeHtml(c.quantity ?? 1)}</small>`, gpValue(num(c.gePriceEach)), gpValue(num(c.totalStackGeValue) ?? num(c.gePrice)), gpValue(num(c.haPriceEach)), gpValue(num(c.totalStackHaValue) ?? num(c.haPrice)), decision, `<span class="muted">${escapeHtml(arr(c.reasons).join(", ") || "—")}</span>`] };
	});
	return table({ columns: [{ label: "Item" }, { label: "GE each" }, { label: "GE stack" }, { label: "HA each" }, { label: "HA stack" }, { label: "Decision" }, { label: "Reason" }], rows, empty: "No loot candidates nearby." });
}
function menuEntryTable(entries) {
	const rows = entries.slice(0, 30).map((e) => typeof e === "string" ? { cells: [escapeHtml(e), "—", "—", "—"] } : { cells: [escapeHtml(e.option || "—"), escapeHtml(e.npcName || e.target || "—"), `<span class="muted">${escapeHtml(e.menuAction || "—")}</span>`, e.at ? formatRelativeTime(new Date(e.at)) : "—"] });
	return table({ columns: [{ label: "Option" }, { label: "Target" }, { label: "Action" }, { label: "At" }], rows, empty: "No recent menu entries." });
}
function highAlchTable(candidates) {
	const rows = candidates.slice(0, 20).map((c) => {
		const itemIconHtml = itemIcon(c.id, c.name, "sm");
		return { cls: c.eligible ? "row-good" : "row-bad", cells: [`${itemIconHtml} ${escapeHtml(c.name || "item")} <small>${idChip(c.id)}</small>`, gpValue(num(c.geEach)), gpValue(num(c.haEach)), gpValue(num(c.deltaEach)), c.eligible ? `<span class="cell-take">alch</span>` : `<span class="cell-skip">skip</span>`] };
	});
	return table({ columns: [{ label: "Item" }, { label: "GE" }, { label: "HA" }, { label: "Delta" }, { label: "Decision" }], rows, empty: "No high-alch candidates." });
}
function inventoryOverview(inventory) {
	const i = obj(inventory);
	const slots = i.slotsUsed !== undefined ? `${i.slotsUsed} / ${i.totalSlots ?? 28}` : selectedValue(i.occupiedSlots, "—");
	return `<div class="inv-summary">
		<div class="inv-stat"><div class="s-label">Slots used</div><div class="s-value">${escapeHtml(slots)}</div></div>
		<div class="inv-stat"><div class="s-label">Free slots</div><div class="s-value">${escapeHtml(selectedValue(i.freeSlots, "—"))}</div></div>
		<div class="inv-stat"><div class="s-label">Total GE</div><div class="s-value">${gpValue(num(i.gePrice))}</div></div>
		<div class="inv-stat"><div class="s-label">Total HA</div><div class="s-value">${gpValue(num(i.haPrice))}</div></div>
	</div>`;
}
function eventsPanel(events) {
	if (!events || !events.length) return `<p class="empty compact">No events yet.</p>`;
	return `<div class="events">${events.slice(0, 20).map((e) => `<div class="ev-row"><span class="ev-t">${escapeHtml(e.time instanceof Date ? e.time.toLocaleTimeString() : "")}</span><span class="ev-d"></span><span class="ev-m">${escapeHtml(e.message)}</span></div>`).join("")}</div>`;
}
function systemHealth(status, player) {
	return kvList([
		{ iconName: "cpu", k: "CPU", v: typeof status.cpu === "number" ? `${status.cpu.toFixed(1)}%` : "—" },
		{ iconName: "memory-stick", k: "Memory", v: status.memory ? `${Math.round(status.memory / 1048576)} MB` : "—" },
		{ iconName: "activity", k: "Status", v: escapeHtml(status.status || "—"), tone: decisionTone(status.status) },
		{ iconName: "globe", k: "Game state", v: escapeHtml(player?.gameState || "—") },
		{ iconName: "wifi", k: "Web export", v: "Running", tone: "good" },
	]);
}

function zoomControls() {
	return `<span class="zoom-ctl"><button class="zoom-btn" data-grid-zoom="out" title="Zoom out (wider)">−</button><span class="zoom-val" id="mfc-zoomval">${getGridRadius() * 2 + 1}</span><button class="zoom-btn" data-grid-zoom="in" title="Zoom in">+</button></span>`;
}

/* ---- mount + update ----------------------------------------------------- */
export function mountMobFarmer() {
	const el = root();
	if (!el || el.dataset.mounted === "1") return;
	el.dataset.mounted = "1";
	el.innerHTML = `
		<div class="metric-strip" id="mfc-metrics"></div>
		<div class="mf-top">
			<div class="mf-top-left">
				${panel({ title: "Target & Combat", iconName: "crosshair", body: `<div id="mfc-targetcombat"></div>` })}
				${panel({ title: "Quick Controls", iconName: "sliders-horizontal", body: `<div id="mfc-quick"></div>` })}
				${panel({ title: "Recent Decisions", iconName: "history", body: `<div id="mfc-decisions"></div>` })}
				${panel({ title: "Loot / Inventory Policy", iconName: "package", body: `<div id="mfc-lootpolicy"></div>` })}
			</div>
			<div class="mf-top-right">
				${panel({ title: "Pathing / Reachability", iconName: "compass", extra: zoomControls(), body: `<div id="mfc-pathing"></div>` })}
			</div>
		</div>
		<div class="grid cols-4" style="margin-bottom:var(--gap)">
			${panel({ title: "Survival", iconName: "shield-half", body: `<div id="mfc-survival"></div>` })}
			${panel({ title: "Intermediate", iconName: "workflow", body: `<div id="mfc-intermediate"></div>` })}
			${panel({ title: "Loot & Inventory", iconName: "coins", body: `<div id="mfc-lootinv"></div>` })}
			${panel({ title: "High Alch", iconName: "wand-2", body: `<div id="mfc-highalch"></div>` })}
		</div>
		<div class="grid cols-3" style="margin-bottom:var(--gap)">
			${panel({ title: "Inventory Overview", iconName: "backpack", body: `<div id="mfc-inventory"></div>` })}
			${panel({ title: "Recent Events", iconName: "scroll-text", body: `<div id="mfc-events"></div>` })}
			${panel({ title: "System Health", iconName: "heart-pulse", body: `<div id="mfc-system"></div>` })}
		</div>
		<div class="grid cols-3">
			${panel({ title: "Target Candidates", iconName: "crosshair", flush: true, body: `<div id="mfc-candidates"></div>` })}
			${panel({ title: "Loot Candidates", iconName: "package", flush: true, body: `<div id="mfc-loot"></div>` })}
			${panel({ title: "Recent Menu Entries", iconName: "list", flush: true, body: `<div id="mfc-menu"></div>` })}
		</div>`;
	el.addEventListener("toggle", (e) => {
		const d = e.target.closest && e.target.closest("details[data-dkey]");
		if (!d) return;
		if (d.open) { openDetails.add(d.dataset.dkey); openDetails.delete(d.dataset.dkey + ":closed"); }
		else { openDetails.delete(d.dataset.dkey); openDetails.add(d.dataset.dkey + ":closed"); }
	}, true);
	refreshIcons();
}

export function renderMobFarmer(mobFarmer, player, events, tileGrid) {
	const el = root();
	if (!el) return { running: false, live: false };
	if (mobFarmer?.unavailable) {
		el.dataset.mounted = "";
		el.innerHTML = panel({ title: "Mob Farmer Unavailable", iconName: "alert-triangle", body: `<p class="scaffold-note">${escapeHtml(mobFarmer.path || "/automation/mob-farmer/status")} is not exposed on this build.</p>` });
		return { running: false, live: false };
	}
	mountMobFarmer();
	const status = obj(mobFarmer);
	const candidates = arr(status.candidates).length ? arr(status.candidates) : arr(status.targetCandidates);
	const lootCandidates = arr(status.lootCandidates);
	const menuEntries = arr(status.recentMenuEntries);
	const intents = arr(status.recentIntents);
	const highAlch = arr(obj(obj(status.loot).highAlch).candidates);
	const inventory = obj(status.inventory);
	const selected = pickSelected(candidates);

	let changed = false;
	changed |= setCell("mfc-metrics", metricStrip(status, player, selected));
	changed |= setCell("mfc-targetcombat", scalarRows({ target: status.target, engagedMode: status.engagedMode, aggroResponse: status.aggroResponse, afterLootCombat: status.afterLootCombatMode, requireLineOfSight: status.requireLineOfSight, maxDistance: status.maxDistance, engagedByOther: selected?.engagedByOther, pathDistance: selected?.pathDistance }, true));
	changed |= setCell("mfc-quick", quickControls(status));
	changed |= setCell("mfc-decisions", decisionsTimeline(intents, menuEntries));
	changed |= setCell("mfc-lootpolicy", scalarRows(status.loot, true));
	changed |= setCellSig("mfc-pathing", gridSignature(player?.worldLocation, candidates, selected, status.pathing, tileGrid), () => pathingBody(status, player, candidates, selected, tileGrid));
	changed |= setCell("mfc-survival", scalarRows({ ...obj(status.autoEat), ...obj(status.survivalDecision) }));
	changed |= setCell("mfc-intermediate", scalarRows(status.intermediateDecision));
	changed |= setCell("mfc-lootinv", scalarRows({ afterLootCombat: status.afterLootCombatMode, ...obj(status.lootDecision), dropPolicy: obj(status.dropPolicy) }));
	changed |= setCell("mfc-highalch", highAlchTable(highAlch));
	changed |= setCell("mfc-inventory", inventoryOverview(inventory));
	changed |= setCell("mfc-events", eventsPanel(events));
	changed |= setCell("mfc-system", systemHealth(status, player));
	const selCount = candidates.filter((c) => c.selectable).length;
	changed |= setCell("mfc-candidates", detailsBlock("cand", `<b>${candidates.length}</b> candidates · <span class="cell-yes">${selCount} selectable</span> · ${candidates.length - selCount} rejected`, targetCandidateTable(candidates, selected)));
	changed |= setCell("mfc-loot", detailsBlock("loot", `<b>${lootCandidates.length}</b> loot candidates`, lootCandidateTable(lootCandidates)));
	changed |= setCell("mfc-menu", detailsBlock("menu", `<b>${menuEntries.length}</b> recent menu entries`, menuEntryTable(menuEntries)));
	if (changed) refreshIcons();

	return { running: Boolean(status.running), live: Boolean(status.live), action: humanizeAction(status.status) };
}
