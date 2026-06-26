/* ============================================================================
 * pages/mobConfig.js — Mob Farmer sub-tabs: Configuration, Inventory Policy,
 * Targeting (schema-driven config editor) and Debug (live diagnostics).
 * Uses GET/POST /automation/mob-farmer/config (schema + settings).
 * ========================================================================== */
import { request, requestOptional } from "../api.js";
import { panel, badge } from "../components.js";
import { icon, refreshIcons } from "../icons.js";
import { escapeHtml, formatError, structuredRows } from "../format.js";

let configPayload = null;   // { schema:[{key,label,type,description,options}], settings:{} }

/* Which schema keys appear on each focused tab. Configuration shows all. */
const TARGETING_KEYS = ["target", "maxDistance", "requireLineOfSight", "engagedMode", "aggroResponse", "afterLootCombatMode", "attackInteractionMode", "lootInteractionMode"];
const POLICY_KEYS = [
	"lootEnabled", "lootDuringCombat", "attackBeforeLoot", "lootMinValueGe", "lootMinSingleGe", "lootMinStackGe",
	"lootMinStackQuantity", "lootAlwaysStackGe", "lootNeverStackBelowGe", "highPriorityLootValueGe", "lootUrgentDespawnTicks",
	"lootCleanupPileCount", "lootRadius", "lootItems", "lootBlacklist", "lootOwnershipMode", "groundItemsMode",
	"respectGroundItemsHidden", "intermediateActionsEnabled", "intermediateItems", "intermediateActionMappings",
	"neverDropItems", "highAlchEnabled", "highAlchMinHa", "highAlchMinDelta", "highAlchMaxLoss", "highAlchItems", "highAlchBlacklist",
];
const SECTIONS = [
	{ title: "Targeting", keys: TARGETING_KEYS },
	{ title: "Survival & Run", keys: ["autoEatEnabled", "eatHitpointPercent", "foodItems", "stopIfNoFood", "survivalPreemptsActions", "autorunEnabled", "autorunMinEnergy", "recoveryLoopDelayMs", "focusClickAfterLogin", "panicStopHotkey"] },
	{ title: "Login Recovery", keys: ["loginRecoveryEnabled", "loginRecoveryF2pOnly", "loginClickToPlayEnabled", "loginDisconnectRecoveryEnabled", "autoResumeAfterLogin", "preferredLoginWorld"] },
	{ title: "Looting", keys: ["lootEnabled", "lootDuringCombat", "attackBeforeLoot", "lootMinValueGe", "lootMinSingleGe", "lootMinStackGe", "lootMinStackQuantity", "lootAlwaysStackGe", "lootNeverStackBelowGe", "highPriorityLootValueGe", "lootUrgentDespawnTicks", "lootCleanupPileCount", "lootRadius", "lootItems", "lootBlacklist", "lootOwnershipMode", "groundItemsMode", "respectGroundItemsHidden"] },
	{ title: "Intermediate Actions", keys: ["intermediateActionsEnabled", "intermediateItems", "intermediateActionMappings", "neverDropItems"] },
	{ title: "High Alchemy", keys: ["highAlchEnabled", "highAlchMinHa", "highAlchMinDelta", "highAlchMaxLoss", "highAlchItems", "highAlchBlacklist"] },
];

const paneId = (tab) => `cfg-pane-${tab}`;

/* ---- field rendering ---------------------------------------------------- */
function field(f, value) {
	const v = value === undefined || value === null ? "" : value;
	const head = `<div class="cfg-label" title="${escapeHtml(f.description || "")}">${escapeHtml(f.label || f.key)}${f.description ? `<span class="cfg-help">${icon("info")}</span>` : ""}</div>`;
	let input;
	if (f.type === "boolean") {
		input = `<label class="toggle"><input type="checkbox" data-cfg-field="${f.key}" data-cfg-type="boolean" ${v === true || v === "true" ? "checked" : ""}><span class="track"></span></label>`;
	} else if (f.type === "enum") {
		input = `<select class="select" data-cfg-field="${f.key}" data-cfg-type="enum">${(f.options || []).map((o) => `<option value="${escapeHtml(o)}" ${String(v) === String(o) ? "selected" : ""}>${escapeHtml(o)}</option>`).join("")}</select>`;
	} else if (f.type === "number") {
		input = `<input class="input" type="number" data-cfg-field="${f.key}" data-cfg-type="number" value="${escapeHtml(v)}">`;
	} else if (f.type === "textarea") {
		input = `<textarea class="textarea" rows="2" data-cfg-field="${f.key}" data-cfg-type="textarea">${escapeHtml(v)}</textarea>`;
	} else {
		input = `<input class="input" type="text" data-cfg-field="${f.key}" data-cfg-type="text" value="${escapeHtml(v)}">`;
	}
	const wide = f.type === "textarea" || (typeof v === "string" && v.length > 40);
	return `<div class="cfg-field ${wide ? "wide" : ""}">${head}${input}</div>`;
}

function sectionBlock(title, fields, settings) {
	if (!fields.length) return "";
	return `<div class="cfg-section"><h4 class="cfg-section-h">${escapeHtml(title)}</h4><div class="cfg-grid">${fields.map((f) => field(f, settings[f.key])).join("")}</div></div>`;
}

async function loadConfig(force) {
	if (configPayload && !force) return configPayload;
	configPayload = await request("/automation/mob-farmer/config");
	return configPayload;
}

export async function renderMobConfigTab(tab, force = false) {
	const pane = document.querySelector(`#${paneId(tab)}`);
	if (!pane) return;
	if (pane.dataset.loaded === "1" && !force) return;   // don't clobber edits on every poll
	pane.innerHTML = `<p class="empty">Loading configuration…</p>`;
	try {
		const cfg = await loadConfig(force);
		const schema = Array.isArray(cfg.schema) ? cfg.schema : [];
		const settings = cfg.settings || {};
		const byKey = Object.fromEntries(schema.map((f) => [f.key, f]));
		let html;
		if (tab === "configuration") {
			html = SECTIONS.map((s) => sectionBlock(s.title, s.keys.map((k) => byKey[k]).filter(Boolean), settings)).join("");
		} else {
			const keys = tab === "targeting" ? TARGETING_KEYS : POLICY_KEYS;
			html = `<div class="cfg-grid">${keys.map((k) => byKey[k]).filter(Boolean).map((f) => field(f, settings[f.key])).join("")}</div>`;
		}
		pane.innerHTML = html || `<p class="empty">No matching settings exposed by this build.</p>`;
		pane.dataset.loaded = "1";
		refreshIcons();
		setState(tab, "Synced with live config", "good");
	} catch (e) {
		pane.innerHTML = `<p class="scaffold-note">Config endpoint unavailable: ${escapeHtml(formatError(e))}</p>`;
	}
}

function setState(tab, msg, tone) {
	const el = document.querySelector(`#cfg-state-${tab}`);
	if (el) el.innerHTML = badge(msg, tone || "");
}

function collectChanged(pane) {
	const settings = (configPayload && configPayload.settings) || {};
	const changed = {};
	pane.querySelectorAll("[data-cfg-field]").forEach((el) => {
		const key = el.dataset.cfgField;
		const type = el.dataset.cfgType;
		let val;
		if (type === "boolean") val = el.checked;
		else if (type === "number") val = el.value === "" ? 0 : Number(el.value);
		else val = el.value;
		const current = settings[key];
		const same = type === "boolean" ? Boolean(current) === val
			: type === "number" ? Number(current) === val
			: String(current ?? "") === String(val);
		if (!same) changed[key] = val;
	});
	return changed;
}

export async function applyMobConfig(tab) {
	const pane = document.querySelector(`#${paneId(tab)}`);
	if (!pane) return;
	const changed = collectChanged(pane);
	const n = Object.keys(changed).length;
	if (!n) { setState(tab, "No changes", "warn"); return; }
	setState(tab, "Applying…", "");
	try {
		const result = await request("/automation/mob-farmer/config", {
			method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ settings: changed }),
		});
		configPayload = result.config || configPayload;
		if (result.config) configPayload = result.config;
		await renderMobConfigTab(tab, true);
		setState(tab, `Applied ${result.updatedSettings ?? n} change${n === 1 ? "" : "s"}`, "good");
	} catch (e) {
		setState(tab, `Apply failed: ${formatError(e)}`, "bad");
	}
}

export async function resetMobConfig(tab) {
	await renderMobConfigTab(tab, true);
	setState(tab, "Reset to live", "");
}

/* ---- Debug tab ---------------------------------------------------------- */
export function renderMobDebug(mobStatus) {
	const pane = document.querySelector("#cfg-pane-mf-debug");
	if (!pane) return;
	const s = mobStatus && typeof mobStatus === "object" ? mobStatus : {};
	const block = (title, ico, data) => panel({ title, iconName: ico, body: rows(data) });
	const html = `
		<div class="grid cols-3" style="margin-bottom:var(--gap)">
			${block("Scheduler", "timer", s.scheduler)}
			${block("Progress", "activity", s.progress)}
			${block("Last Action Attempt", "swords", s.lastActionAttempt)}
		</div>
		<div class="grid cols-3" style="margin-bottom:var(--gap)">
			${block("Decision", "brain", s.decision)}
			${block("Death-loot Timing", "skull", s.deathLootTiming)}
			${block("Reattach After Pickup", "repeat", s.reattachAfterPickup)}
		</div>
		${panel({ title: "Raw mob-farmer payload", iconName: "scroll-text", flush: true, body: `<details><summary class="raw-summary">full JSON</summary><pre class="raw-json">${escapeHtml(JSON.stringify(s, null, 2))}</pre></details>` })}`;
	const sig = html.length + ":" + JSON.stringify(s.scheduler || {}).length;
	if (pane.__sig === sig) return;
	pane.__sig = sig;
	pane.innerHTML = html;
	refreshIcons();
}

function rows(data) {
	const r = structuredRows(data);
	if (!r.length) return `<p class="empty compact">No data.</p>`;
	return `<div class="tbl-wrap"><table class="tbl"><tbody>${r.map(([k, v]) => `<tr><td class="muted">${escapeHtml(k)}</td><td>${escapeHtml(v)}</td></tr>`).join("")}</tbody></table></div>`;
}
