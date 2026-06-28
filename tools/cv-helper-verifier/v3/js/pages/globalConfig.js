/* ============================================================================
 * pages/globalConfig.js — shared/global CV Helper Configuration tab.
 *
 * Same draft-editor contract as skillConfig.js/mobConfig.js (load/apply/reset/
 * import/export, draft never clobbered while editing) but scoped to the new
 * GET/POST /automation/global/config endpoint -- settings that aren't specific
 * to any one farmer (overlay toggles, local export, anti-idle).
 * ========================================================================== */
import { request } from "../api.js";
import { badge } from "../components.js";
import { icon, refreshIcons } from "../icons.js";
import { escapeHtml, formatError } from "../format.js";

let live = null;   // cached { version, settings, schema }

const pane = () => document.querySelector("#global-cfg-pane");

function field(f, value) {
	const v = value === undefined || value === null ? "" : value;
	const head = `<div class="cfg-label" title="${escapeHtml(f.description || "")}">${escapeHtml(f.label || f.key)}${f.description ? `<span class="cfg-help">${icon("info")}</span>` : ""}</div>`;
	let input;
	if (f.type === "boolean") {
		input = `<label class="toggle"><input type="checkbox" data-cfg-field="${f.key}" data-cfg-type="boolean" ${v === true || v === "true" ? "checked" : ""}><span class="track"></span></label>`;
	} else if (f.type === "number") {
		input = `<input class="input" type="number" data-cfg-field="${f.key}" data-cfg-type="number" value="${escapeHtml(v)}">`;
	} else {
		input = `<input class="input" type="text" data-cfg-field="${f.key}" data-cfg-type="text" value="${escapeHtml(v)}">`;
	}
	return `<div class="cfg-field">${head}${input}</div>`;
}

function sectionBlock(title, fields, settings) {
	if (!fields.length) return "";
	return `<div class="cfg-section"><h4 class="cfg-section-h">${escapeHtml(title)}</h4><div class="cfg-grid">${fields.map((f) => field(f, settings[f.key])).join("")}</div></div>`;
}

const SECTIONS = [
	{ title: "Overlay & Debug Display", keys: ["showHoverOverlay", "showWidgetInfo"] },
	{ title: "Local Export Server", keys: ["enableLocalExport"] },
	{ title: "Anti-idle", keys: ["antiIdleEnabled", "antiIdleTimeoutMinutes", "antiIdleInputIntervalMinutes", "antiIdleManualOverride", "antiIdleRestoreMouse"] },
];

function setState(msg, tone) {
	const el = document.querySelector("#global-cfg-state");
	if (el) el.innerHTML = badge(msg, tone || "");
}

async function loadLive(force) {
	if (live && !force) return live;
	live = await request("/automation/global/config");
	return live;
}

function renderForm(cfg) {
	const p = pane();
	if (!p) return;
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
	p.innerHTML = html || `<p class="empty">No global settings exposed by this build.</p>`;
	p.dataset.loaded = "1";
	refreshIcons();
}

export async function renderGlobalConfigTab(force = false) {
	const p = pane();
	if (!p) return;
	if (p.dataset.loaded === "1" && !force) return;
	p.innerHTML = `<p class="empty">Loading configuration…</p>`;
	try {
		const cfg = await loadLive(force);
		renderForm(cfg);
		setState("Synced with live config", "good");
	} catch (e) {
		p.innerHTML = `<p class="scaffold-note">Config endpoint unavailable: ${escapeHtml(formatError(e))}</p>`;
		setState("Unavailable", "bad");
	}
}

function readDraft() {
	const p = pane();
	const out = {};
	if (!p) return out;
	p.querySelectorAll("[data-cfg-field]").forEach((el) => {
		const key = el.dataset.cfgField;
		const type = el.dataset.cfgType;
		if (type === "boolean") out[key] = el.checked;
		else if (type === "number") out[key] = el.value === "" ? 0 : Number(el.value);
		else out[key] = el.value;
	});
	return out;
}

function changedCount(draft) {
	const settings = (live && live.settings) || {};
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

export async function applyGlobalConfig() {
	const p = pane();
	if (!p || p.dataset.loaded !== "1") { setState("Open the tab first", "warn"); return; }
	const draft = readDraft();
	const n = changedCount(draft);
	if (!n) { setState("No changes", "warn"); return; }
	setState("Applying…", "");
	try {
		const res = await request("/automation/global/config", {
			method: "POST", headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ version: 1, settings: draft }),
		});
		if (res && res.ok === false) { setState(`Rejected: ${(res.errors || ["unknown"]).join("; ")}`, "bad"); return; }
		if (res && res.config) live = res.config;
		renderForm(live);
		setState(`Applied ${n} change${n === 1 ? "" : "s"}`, "good");
	} catch (e) {
		setState(`Apply failed: ${formatError(e)}`, "bad");
	}
}

export async function resetGlobalConfig() {
	await renderGlobalConfigTab(true);
	setState("Reset to live", "");
}

export async function loadGlobalConfigIntoDraft() {
	await renderGlobalConfigTab(true);
	setState("Loaded live config", "good");
}

export async function exportGlobalConfig() {
	try {
		const cfg = await loadLive(false);
		const json = JSON.stringify({ version: 1, settings: cfg.settings || {} }, null, 2);
		try { await navigator.clipboard.writeText(json); setState("Config copied to clipboard", "good"); }
		catch { window.prompt("Copy config JSON:", json); }
	} catch (e) {
		setState(`Export failed: ${formatError(e)}`, "bad");
	}
}

export async function importGlobalConfig() {
	const raw = window.prompt("Paste config JSON to import:", "");
	if (!raw) return;
	let parsed;
	try { parsed = JSON.parse(raw); } catch { setState("Invalid JSON", "bad"); return; }
	const settings = parsed.settings || parsed;
	setState("Importing…", "");
	try {
		const res = await request("/automation/global/config", {
			method: "POST", headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ version: 1, settings }),
		});
		if (res && res.ok === false) { setState(`Rejected: ${(res.errors || []).join("; ")}`, "bad"); return; }
		if (res && res.config) live = res.config;
		renderForm(live);
		setState("Imported & applied", "good");
	} catch (e) {
		setState(`Import failed: ${formatError(e)}`, "bad");
	}
}
