/* ============================================================================
 * pages/skillConfig.js — Mining & Woodcutting farmer Configuration + Debug tabs.
 *
 * Schema-driven draft/live editor over GET/POST /automation/{skill}/config.
 * Live state (the plugin's current config) is loaded into a DRAFT that is never
 * clobbered while the user is editing — only an explicit Load/Reset re-syncs it.
 * POST contract: { version:1, settings:{ target, ... } } (target required).
 * ========================================================================== */
import { request } from "../api.js";
import { panel, badge } from "../components.js";
import { icon, refreshIcons } from "../icons.js";
import { escapeHtml, formatError, structuredRows } from "../format.js";

// per-skill cached live payload { version, settings, schema, presets }
const live = { mining: null, woodcutting: null };

/* Group schema keys into readable sections. Unlisted keys fall into "Other". */
const SECTIONS = [
	{ title: "Target & Scan", keys: ["target", "live", "scanRadiusTiles", "maxCandidates"] },
	{ title: "Drop Policy", keys: ["dropPolicyEnabled", "dropPolicyMode", "dropPolicyThresholdSlots", "dropPolicyItems", "dropPolicyProtectedItems", "dropPolicyMaxValue"] },
	{ title: "Inventory", keys: ["protectedItems", "inventoryPolicy"] },
	{ title: "Woodcutting Behaviour", keys: ["woodcuttingStickToTarget", "woodcuttingReclickWhenActivelyChopping"] },
];

const cfgPane = (skill) => document.querySelector(`#${skill}-cfg-pane`);
const dbgPane = (skill) => document.querySelector(`#${skill}-debug-pane`);

function setState(skill, msg, tone) {
	const el = document.querySelector(`#${skill}-cfg-state`);
	if (el) el.innerHTML = badge(msg, tone || "");
}

/* ---- field rendering (mirrors mobConfig.js) ----------------------------- */
function field(f, value) {
	const v = value === undefined || value === null ? "" : value;
	const head = `<div class="cfg-label" title="${escapeHtml(f.description || "")}">${escapeHtml(f.label || f.key)}${f.description ? `<span class="cfg-help">${icon("info")}</span>` : ""}</div>`;
	let input;
	if (f.type === "boolean") {
		input = `<label class="toggle"><input type="checkbox" data-cfg-field="${f.key}" data-cfg-type="boolean" ${v === true || v === "true" ? "checked" : ""}><span class="track"></span></label>`;
	} else if (f.type === "enum" || f.type === "select") {
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

async function loadLive(skill, force) {
	if (live[skill] && !force) return live[skill];
	live[skill] = await request(`/automation/${skill}/config`);
	return live[skill];
}

function renderForm(skill, cfg) {
	const pane = cfgPane(skill);
	if (!pane) return;
	const schema = Array.isArray(cfg.schema) ? cfg.schema : [];
	const settings = cfg.settings || {};
	const byKey = Object.fromEntries(schema.map((f) => [f.key, f]));
	const used = new Set();
	let html = SECTIONS.map((s) => {
		const fields = s.keys.map((k) => byKey[k]).filter(Boolean);
		fields.forEach((f) => used.add(f.key));
		return sectionBlock(s.title, fields, settings);
	}).join("");
	const other = schema.filter((f) => !used.has(f.key));
	if (other.length) html += sectionBlock("Other", other, settings);
	pane.innerHTML = html || `<p class="empty">No settings exposed by this build.</p>`;
	pane.dataset.loaded = "1";
	refreshIcons();
}

/* ---- public: render config tab (first open or explicit reload) ---------- */
export async function renderSkillConfigTab(skill, force = false) {
	const pane = cfgPane(skill);
	if (!pane) return;
	if (pane.dataset.loaded === "1" && !force) return;   // keep draft edits across polls
	pane.innerHTML = `<p class="empty">Loading configuration…</p>`;
	try {
		const cfg = await loadLive(skill, force);
		renderForm(skill, cfg);
		setState(skill, "Synced with live config", "good");
	} catch (e) {
		pane.innerHTML = `<p class="scaffold-note">Config endpoint unavailable: ${escapeHtml(formatError(e))}</p>`;
		setState(skill, "Unavailable", "bad");
	}
}

function readDraft(skill) {
	const pane = cfgPane(skill);
	const out = {};
	if (!pane) return out;
	pane.querySelectorAll("[data-cfg-field]").forEach((el) => {
		const key = el.dataset.cfgField;
		const type = el.dataset.cfgType;
		if (type === "boolean") out[key] = el.checked;
		else if (type === "number") out[key] = el.value === "" ? 0 : Number(el.value);
		else out[key] = el.value;
	});
	return out;
}

function changedKeys(skill, draft) {
	const settings = (live[skill] && live[skill].settings) || {};
	let n = 0;
	for (const k of Object.keys(draft)) {
		const cur = settings[k];
		const same = typeof draft[k] === "boolean" ? Boolean(cur) === draft[k]
			: typeof draft[k] === "number" ? Number(cur) === draft[k]
			: String(cur ?? "") === String(draft[k]);
		if (!same) n++;
	}
	return n;
}

/* ---- public: apply / reset / load / import / export --------------------- */
export async function applySkillConfig(skill) {
	const pane = cfgPane(skill);
	if (!pane || pane.dataset.loaded !== "1") { setState(skill, "Open the tab first", "warn"); return; }
	const draft = readDraft(skill);
	const target = (draft.target ?? (live[skill]?.settings?.target) ?? "").toString().trim();
	if (!target) { setState(skill, "Target is required", "bad"); return; }
	const settings = { ...draft, target };
	const n = changedKeys(skill, settings);
	setState(skill, "Applying…", "");
	try {
		const res = await request(`/automation/${skill}/config`, {
			method: "POST", headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ version: 1, target, settings }),
		});
		if (res && res.ok === false) {
			setState(skill, `Rejected: ${(res.errors || ["unknown"]).join("; ")}`, "bad");
			return;
		}
		if (res && res.config) live[skill] = res.config;
		renderForm(skill, live[skill]);
		setState(skill, `Applied ${n} change${n === 1 ? "" : "s"}`, "good");
	} catch (e) {
		setState(skill, `Apply failed: ${formatError(e)}`, "bad");
	}
}

export async function resetSkillConfig(skill) {
	await renderSkillConfigTab(skill, true);
	setState(skill, "Reset to live", "");
}

export async function loadSkillConfigIntoDraft(skill) {
	await renderSkillConfigTab(skill, true);
	setState(skill, "Loaded live config", "good");
}

export async function exportSkillConfig(skill) {
	try {
		const cfg = await loadLive(skill, false);
		const json = JSON.stringify({ version: 1, target: cfg.settings?.target || "", settings: cfg.settings || {} }, null, 2);
		try { await navigator.clipboard.writeText(json); setState(skill, "Config copied to clipboard", "good"); }
		catch { window.prompt("Copy config JSON:", json); }
	} catch (e) {
		setState(skill, `Export failed: ${formatError(e)}`, "bad");
	}
}

export async function importSkillConfig(skill) {
	const raw = window.prompt("Paste config JSON to import:", "");
	if (!raw) return;
	let parsed;
	try { parsed = JSON.parse(raw); } catch { setState(skill, "Invalid JSON", "bad"); return; }
	const settings = parsed.settings || parsed;
	const target = (settings.target ?? parsed.target ?? "").toString().trim();
	if (!target) { setState(skill, "Imported JSON needs a target", "bad"); return; }
	setState(skill, "Importing…", "");
	try {
		const res = await request(`/automation/${skill}/config`, {
			method: "POST", headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ version: 1, target, settings: { ...settings, target } }),
		});
		if (res && res.ok === false) { setState(skill, `Rejected: ${(res.errors || []).join("; ")}`, "bad"); return; }
		if (res && res.config) live[skill] = res.config;
		renderForm(skill, live[skill]);
		setState(skill, "Imported & applied", "good");
	} catch (e) {
		setState(skill, `Import failed: ${formatError(e)}`, "bad");
	}
}

/* ---- public: Debug tab -------------------------------------------------- */
function rows(data) {
	const r = structuredRows(data);
	if (!r.length) return `<p class="empty compact">No data.</p>`;
	return `<div class="tbl-wrap"><table class="tbl"><tbody>${r.map(([k, v]) => `<tr><td class="muted">${escapeHtml(k)}</td><td>${escapeHtml(v)}</td></tr>`).join("")}</tbody></table></div>`;
}

export function renderSkillDebug(skill, status) {
	const pane = dbgPane(skill);
	if (!pane) return;
	const s = status && typeof status === "object" ? status : {};
	const sm = s.candidateSummary || {};
	const sel = s.selected || {};
	const block = (title, ico, data) => panel({ title, iconName: ico, body: rows(data) });
	const lifecycle = {
		running: s.running, live: s.live, source: s.source, currentAction: s.currentAction,
		decision: s.decision, target: s.target, runtimeTarget: s.runtimeTarget,
		selected: sel.name ? `${sel.name} (#${sel.id})` : "—",
		lastCompletedTarget: s.lastCompletedTarget, completionReason: s.completionReason,
		lastInvalidationTick: s.lastInvalidationTick, rockDepleted: s.rockDepleted,
		rejectedStaleTiles: Array.isArray(s.rejectedStaleTiles) ? s.rejectedStaleTiles.length : s.rejectedStaleTiles,
		lastFailureReason: s.lastFailureReason, updatedAt: s.updatedAt,
	};
	const scan = {
		scanRadiusTiles: s.scanRadiusTiles, maxCandidates: s.maxCandidates,
		totalCandidates: sm.totalCandidates, targetMatches: sm.targetMatches,
		matchedReachable: sm.matchedReachable, matchedUnreachable: sm.matchedUnreachable,
		missingAction: sm.missingAction, collisionBlocked: sm.collisionBlocked, sceneBlocked: sm.sceneBlocked,
	};
	const html = `
		<div class="grid cols-3" style="margin-bottom:var(--gap)">
			${block("Target Lifecycle", "target", lifecycle)}
			${block("Scan Summary", "radar", scan)}
			${block("Drop Policy", "package", s.dropPolicy)}
		</div>
		${panel({ title: `Raw ${escapeHtml(skill)} payload`, iconName: "scroll-text", flush: true, body: `<details><summary class="raw-summary">full JSON</summary><pre class="raw-json">${escapeHtml(JSON.stringify(s, null, 2))}</pre></details>` })}`;
	const sig = html.length + ":" + JSON.stringify(lifecycle).length;
	if (pane.__sig === sig) return;
	pane.__sig = sig;
	pane.innerHTML = html;
	refreshIcons();
}
