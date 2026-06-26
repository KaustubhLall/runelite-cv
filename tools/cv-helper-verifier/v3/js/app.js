/* ============================================================================
 * app.js — bootstrap: routing, polling, chrome, control deck wiring.
 * ========================================================================== */
import {
	state, request, requestOptional, ensurePort, discoverPort, setPort,
	baseUrl, readRememberedPorts, DEFAULT_PORT,
} from "./api.js";
import { refreshIcons, icon } from "./icons.js";
import { sanitizePort, formatError, formatDuration, formatRelativeTime, delay } from "./format.js";
import { badge } from "./components.js";
import { renderDashboard } from "./pages/dashboard.js";
import { renderMobFarmer } from "./pages/mobFarmer.js";
import { renderSkillFarmer } from "./pages/skillFarmer.js";
import { renderRaw, renderDebug, renderInventory } from "./pages/misc.js";
import { renderMobConfigTab, applyMobConfig, resetMobConfig, renderMobDebug } from "./pages/mobConfig.js";
import { setGridRadius, getGridRadius } from "./pathGrid.js";
import { listPresets, getPreset, savePreset, deletePreset, isBuiltin, setPluginPresets, PRESET_ENDPOINT } from "./presets.js";
import { renderSkillConfigTab, applySkillConfig, resetSkillConfig, loadSkillConfigIntoDraft, importSkillConfig, exportSkillConfig, renderSkillDebug } from "./pages/skillConfig.js";
import { renderMinimapView } from "./pages/minimapView.js";
import { renderActionsView } from "./pages/actionsView.js";
import { renderGlobalConfigTab, applyGlobalConfig, resetGlobalConfig, loadGlobalConfigIntoDraft, importGlobalConfig, exportGlobalConfig } from "./pages/globalConfig.js";

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

/* ---- shared session state ---------------------------------------------- */
let timer = null;
let events = [];
let lastRefreshAt = null;
let connectedAt = null;
let firstStatus = false;
let lastLoggedIn = null;
let fastMs = Number(localStorage.getItem("cvHelperRefreshMs")) || 1000;

function addEvent(message) {
	events.unshift({ message, time: new Date() });
	if (events.length > 40) events.pop();
}

/* ---- routing ------------------------------------------------------------ */
function showView(view) {
	activeView = view;
	$$(".view").forEach((v) => { v.hidden = v.dataset.view !== view; });
	$$(".nav-item").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
	if (firstStatus) refreshAll();   // render the newly-shown view immediately
}

/* ---- macro / farmer status panel (sidebar) ----------------------------- */
function updateMacro(auto, player) {
	const el = $("#macro-panel");
	if (!el) return;
	const macros = [
		{ key: "mob-farmer", label: "Mob Farmer", ico: "skull", st: auto.mobFarmer },
		{ key: "mining", label: "Mining", ico: "pickaxe", st: auto.mining },
		{ key: "woodcutting", label: "Woodcutting", ico: "trees", st: auto.woodcutting },
	];
	const recovery = obj(obj(auto.mobFarmer).loginRecovery);
	const active = macros.filter((m) => m.st && m.st.running);
	const idle = macros.filter((m) => !(m.st && m.st.running));
	const row = (m, on) => `<button class="macro-row ${on ? "on" : ""}" data-view="${m.key}"><span class="macro-ico">${icon(m.ico)}</span><span class="macro-label">${m.label}</span><span class="macro-state">${on ? `<span class="dot ok"></span>${m.st.live ? "Live" : "Dry"}` : `<span class="dot"></span>idle`}</span></button>`;
	const extra = recovery.recoveryWorkerActive ? `<div class="macro-extra"><span class="dot warn"></span> Login recovery active</div>` : "";
	el.innerHTML = `
		<div class="macro-active">${active.length ? active.map((m) => row(m, true)).join("") : `<div class="macro-none">No macros running</div>`}</div>
		${idle.length ? `<details class="macro-more"${active.length ? "" : " open"}><summary>Idle macros (${idle.length})</summary>${idle.map((m) => row(m, false)).join("")}</details>` : ""}
		${extra}`;
	refreshIcons();
}

let currentMobTab = "overview";
function showTab(tab) {
	currentMobTab = tab;
	$$(".tab").forEach((b) => b.classList.toggle("active", b.dataset.tab === tab));
	$$(".tabpane").forEach((p) => { p.hidden = p.dataset.tab !== tab; });
	if (tab === "configuration" || tab === "inventory-policy" || tab === "targeting") {
		renderMobConfigTab(tab);
	} else if (tab === "mf-debug") {
		renderMobDebug(obj(obj(obj(state.lastStatus).player).automation).mobFarmer);
	}
}

/* ---- skill-farmer sub-tabs (Overview / Configuration / Debug) ----------- */
const skillTab = { mining: "overview", woodcutting: "overview" };
function showSkillTab(skill, tab) {
	skillTab[skill] = tab;
	$$(`[data-skill-tabs="${skill}"] .tab`).forEach((b) => b.classList.toggle("active", b.dataset.skillTab === tab));
	$$(`.skill-pane[data-skill="${skill}"]`).forEach((p) => { p.hidden = p.dataset.skillPane !== tab; });
	if (tab === "config") renderSkillConfigTab(skill);
	else if (tab === "debug") renderSkillDebug(skill, obj(state.lastStatus).__skillStatus?.[skill]);
}

/* ---- mob-farmer controls ------------------------------------------------ */
async function mfAction(action) {
	if (!(await ensurePort())) return;
	const target = ($("#mf-target").value || "cow").trim();
	const live = $("#mf-mode").value === "true";
	const path = action === "stop"
		? "/automation/mob-farmer/stop"
		: `/automation/mob-farmer/${action}?target=${encodeURIComponent(target)}&live=${encodeURIComponent(String(live))}`;
	try {
		await request(path, { method: "POST" });
		addEvent(`${action} queued · ${target} (${live ? "live" : "dry"})`);
	} catch (err) {
		addEvent(`${action} failed: ${formatError(err)}`);
	}
	await delay(800);
	await refreshAll();
}

async function skillAction(skill, action) {
	if (!(await ensurePort())) return;
	const target = ($(`#${skill}-target`).value || "").trim();
	const live = $(`#${skill}-mode`).value === "true";
	const path = action === "stop"
		? `/automation/${skill}/stop`
		: `/automation/${skill}/${action}?target=${encodeURIComponent(target)}&live=${encodeURIComponent(String(live))}`;
	try {
		await request(path, { method: "POST" });
		addEvent(`${skill} ${action} · ${target || "(target)"} (${live ? "live" : "dry"})`);
	} catch (err) {
		addEvent(`${skill} ${action} failed: ${formatError(err)}`);
	}
	await delay(800);
	await refreshAll();
}

/* ---- presets ------------------------------------------------------------ */
function targetInputId(farmer) { return farmer === "mob" ? "mf-target" : `${farmer}-target`; }

function populatePresets(farmer) {
	const sel = $(`#${farmer}-preset`);
	if (!sel) return;
	const cur = sel.value;
	const items = listPresets(farmer);
	sel.innerHTML = items.length
		? items.map((p) => `<option value="${p.name}">${p.builtin ? "★ " : ""}${p.name}</option>`).join("")
		: `<option value="">(no presets)</option>`;
	if (cur && items.some((p) => p.name === cur)) sel.value = cur;
}

async function applyPreset(farmer) {
	const sel = $(`#${farmer}-preset`);
	const name = sel && sel.value;
	const preset = name && getPreset(farmer, name);
	if (!preset || !(await ensurePort())) return;
	try {
		const t = $(`#${targetInputId(farmer)}`);
		if (t && preset.target) t.value = preset.target;
		if (farmer === "mob") {
			await request(`/automation/mob-farmer/config`, {
				method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ settings: { target: preset.target } }),
			});
			renderMobConfigTab("configuration", true);
		} else {
			// skill farmers require versioned payload with a target
			await request(`/automation/${PRESET_ENDPOINT[farmer]}/config`, {
				method: "POST", headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ version: 1, target: preset.target, settings: { target: preset.target } }),
			});
			if (skillTab[farmer] === "config") renderSkillConfigTab(farmer, true);
		}
		addEvent(`${farmer} preset “${name}” applied`);
	} catch (err) {
		addEvent(`preset apply failed: ${formatError(err)}`);
	}
	await delay(400);
	await refreshAll();
}

async function savePresetFlow(farmer) {
	const name = (window.prompt(`Save current ${farmer} config as a preset:`, "") || "").trim();
	if (!name) return;
	let settings = {};
	try {
		const cfg = await request(`/automation/${PRESET_ENDPOINT[farmer]}/config`);
		const all = cfg.settings || {};
		settings = farmer === "mob" ? all : { target: all.target, scanRadiusTiles: all.scanRadiusTiles, maxCandidates: all.maxCandidates };
	} catch {
		const t = $(`#${targetInputId(farmer)}`);
		if (t && t.value) settings = { target: t.value };
	}
	savePreset(farmer, name, settings);
	populatePresets(farmer);
	const sel = $(`#${farmer}-preset`); if (sel) sel.value = name;
	addEvent(`${farmer} preset “${name}” saved`);
}

function deletePresetFlow(farmer) {
	const sel = $(`#${farmer}-preset`);
	const name = sel && sel.value;
	if (!name) return;
	if (isBuiltin(farmer, name)) { addEvent(`“${name}” is a built-in preset`); return; }
	if (deletePreset(farmer, name)) { populatePresets(farmer); addEvent(`preset “${name}” deleted`); }
}

async function patchMobConfig(key, value) {
	if (!(await ensurePort())) return;
	try {
		await request("/automation/mob-farmer/config", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ settings: { [key]: value } }),
		});
		addEvent(`config · ${key} = ${value}`);
	} catch (err) {
		addEvent(`config ${key} failed: ${formatError(err)}`);
	}
	await delay(400);
	await refreshAll();
}

async function mfFocusClick() {
	if (!(await ensurePort())) return;
	try {
		await request("/automation/mob-farmer/focus-click", { method: "POST" });
		addEvent("focus click queued");
	} catch (err) {
		addEvent(`focus click failed: ${formatError(err)}`);
	}
	await delay(700);
	await refreshAll();
}

/* ---- tiered refresh -----------------------------------------------------
 * Fast tick: ONE /status call (carries the full mob-farmer status + skill
 * summaries + macro states under player.automation). Only the active view is
 * rendered; skill views fetch their own candidates on demand. Slow tick polls
 * the dashboard target surfaces for counts. This keeps fast polling cheap.
 * ------------------------------------------------------------------------- */
const obj = (v) => (v && typeof v === "object" ? v : {});
const SURFACES = ["prayer", "spell", "minimap", "inventory", "equipment", "panels", "combat"];
let activeView = "mob-farmer";
let lastCounts = {};
let slowCounter = 0;
// Full per-tile reachability grid (every tile in radius, not just candidates),
// fetched asynchronously per active view so it never blocks the fast status poll.
let tileGridCache = {};
// Standalone Minimap page: lens + its own radius, independent of farmer running
// state. Shares the SAME getGridRadius()/setGridRadius() the farmer pages' zoom
// buttons use (pathGrid.js keeps one global visual radius) so the radius control
// actually changes what's rendered, not just how much data is fetched underneath it.
let minimapLens = "scene";
let minimapTileGrid = null;
function refreshMinimapTileGrid(attempted) {
	requestOptional(`/pathing/grid?radius=${getGridRadius()}`, {}, attempted)
		.then((g) => { if (g && Array.isArray(g.tiles)) minimapTileGrid = g; })
		.catch(() => { /* keep showing the last good grid */ });
}

function refreshTileGrid(view, attempted) {
	requestOptional(`/pathing/grid?radius=${getGridRadius()}`, {}, attempted)
		.then((g) => { if (g && Array.isArray(g.tiles)) tileGridCache[view] = g; })
		.catch(() => { /* keep showing the last good grid */ });
}

async function refreshAll(options = {}) {
	if (!sanitizePort(state.port)) {
		await discoverPort();
		if (!sanitizePort(state.port)) { renderDisconnected("No CV Helper export discovered yet."); return; }
	}
	const attempted = sanitizePort(state.port);
	try {
		const status = await request("/status", {}, attempted);
		const resolved = sanitizePort(status.port) || attempted;
		if (resolved !== attempted) setPort(resolved);

		state.lastStatus = status;
		const player = status.player || {};
		const auto = obj(player.automation);

		if (!firstStatus) { firstStatus = true; connectedAt = new Date(); addEvent("Connected to CV Helper"); }
		if (player.loggedIn && lastLoggedIn !== true) addEvent(`Logged in as ${player.localPlayer || "player"}`);
		if (lastLoggedIn === true && !player.loggedIn) addEvent("Logged out");
		lastLoggedIn = Boolean(player.loggedIn);

		// always-on chrome + macro panel + farmer badges (cheap)
		const mob = auto.mobFarmer || {};
		updateChrome(status, mob, { running: !!mob.running, live: !!mob.live }, resolved);
		updateMacro(auto, player);
		setSkillBadge("mining", auto.mining, { running: !!auto.mining?.running, live: !!auto.mining?.live });
		setSkillBadge("woodcutting", auto.woodcutting, { running: !!auto.woodcutting?.running, live: !!auto.woodcutting?.live });
		syncDeck(mob);
		syncSkillDeck("mining", auto.mining);
		syncSkillDeck("woodcutting", auto.woodcutting);

		await renderActiveView(status, player, auto, attempted);

		if (--slowCounter <= 0) {
			slowCounter = Math.max(1, Math.round(4000 / fastMs));
			refreshSlow(attempted).catch(() => {});
		}

		lastRefreshAt = new Date();
		refreshIcons();
	} catch (err) {
		console.error("[refreshAll] failed:", err);
		if (options.allowRediscovery !== false) {
			const recovered = await discoverPort();
			if (recovered && recovered !== attempted) { await refreshAll({ allowRediscovery: false }); return; }
		}
		renderDisconnected(formatError(err), attempted);
		refreshIcons();
	}
}

async function renderActiveView(status, player, auto, attempted) {
	switch (activeView) {
		case "mob-farmer": {
			renderMobFarmer(auto.mobFarmer, player, events, tileGridCache[activeView]);
			refreshTileGrid(activeView, attempted);
			const ov = $("#mf-overview"); if (ov) ov.dataset.hasContent = "1";
			if (currentMobTab === "mf-debug") renderMobDebug(auto.mobFarmer);
			break;
		}
		case "mining":
		case "woodcutting": {
			const st = await requestOptional(`/automation/${activeView}/status`, {}, attempted);
			renderSkillFarmer(activeView, st, player, events, tileGridCache[activeView]);
			refreshTileGrid(activeView, attempted);
			// stash status so the Debug sub-tab can read it without a second fetch
			if (state.lastStatus) {
				state.lastStatus.__skillStatus = state.lastStatus.__skillStatus || {};
				state.lastStatus.__skillStatus[activeView] = st;
			}
			if (skillTab[activeView] === "debug") renderSkillDebug(activeView, st);
			// Load presets asynchronously without blocking rendering
			requestOptional(`/automation/${activeView}/config`, {}, attempted).then(cfg => {
				if (cfg?.presets && Array.isArray(cfg.presets)) {
					setPluginPresets(activeView, cfg.presets);
					populatePresets(activeView);
				}
			}).catch(() => {
				// Silently fail - presets will load on next refresh or manual interaction
			});
			break;
		}
		case "dashboard":
			renderDashboard(status, auto.mobFarmer || null, events, lastCounts);
			break;
		case "inventory":
			renderInventory(status);
			break;
		case "raw": {
			const [mining, woodcutting] = await Promise.all([
				requestOptional("/automation/mining/status", {}, attempted),
				requestOptional("/automation/woodcutting/status", {}, attempted),
			]);
			renderRaw({ status, mobFarmer: auto.mobFarmer, mining, woodcutting });
			break;
		}
		case "debug":
			renderDebug(state.discovery);
			break;
		case "minimap": {
			refreshMinimapTileGrid(attempted);
			let data; let running;
			if (minimapLens === "scene") {
				data = await requestOptional(`/scene/diagnostics?radius=${getGridRadius()}`, {}, attempted);
			} else if (minimapLens === "mob") {
				data = auto.mobFarmer; running = !!auto.mobFarmer?.running;
			} else {
				data = await requestOptional(`/automation/${minimapLens}/status`, {}, attempted);
				running = !!data?.running;
			}
			renderMinimapView(minimapLens, data, player?.worldLocation, running, minimapTileGrid);
			break;
		}
		case "actions": {
			const cfg = await requestOptional("/automation/mob-farmer/config", {}, attempted);
			renderActionsView(cfg);
			break;
		}
		case "configuration":
			renderGlobalConfigTab();
			break;
	}
}

async function refreshSlow(attempted) {
	const [entities, ...surfaces] = await Promise.all([
		requestOptional("/entities", {}, attempted),
		...SURFACES.map((s) => requestOptional(`/targets/${s}`, {}, attempted)),
	]);
	const counts = {};
	SURFACES.forEach((s, i) => { counts[s] = surfaces[i]?.count ?? (Array.isArray(surfaces[i]?.targets) ? surfaces[i].targets.length : 0); });
	counts.entities = entities?.count ?? (Array.isArray(entities?.entities) ? entities.entities.length : 0);
	lastCounts = counts;
	if (activeView === "dashboard" && state.lastStatus) {
		renderDashboard(state.lastStatus, obj(obj(state.lastStatus.player).automation).mobFarmer || null, events, lastCounts);
		refreshIcons();
	}
}

function syncDeck(mobFarmer) {
	if (!mobFarmer || mobFarmer.unavailable) return;
	const target = $("#mf-target");
	if (document.activeElement !== target && mobFarmer.target) target.value = mobFarmer.target;
	if (document.activeElement !== $("#mf-mode")) $("#mf-mode").value = String(Boolean(mobFarmer.live));
	const md = $("#mf-maxdist"); if (mobFarmer.maxDistance !== undefined && document.activeElement !== md) md.value = mobFarmer.maxDistance;
	const lp = $("#mf-loop"); if (mobFarmer.loopDelayMs !== undefined && document.activeElement !== lp) lp.value = mobFarmer.loopDelayMs;
}

function setSkillBadge(skill, status, summary) {
	const el = $(`#${skill}-badge`);
	if (!el) return;
	if (!status || status.unavailable) { el.innerHTML = badge("Unavailable", "warn"); return; }
	el.innerHTML = badge(summary.running ? `Running · ${summary.live ? "Live" : "Dry"}` : "Stopped", summary.running ? "good" : "bad", { live: summary.running, pulse: summary.running });
}

function syncSkillDeck(skill, status) {
	if (!status || status.unavailable) return;
	const t = $(`#${skill}-target`);
	const live = $(`#${skill}-mode`);
	if (t && document.activeElement !== t && status.target) t.value = status.target;
	if (live && document.activeElement !== live && status.live !== undefined) live.value = String(Boolean(status.live));
}

function updateChrome(status, mobFarmer, summary, port) {
	const player = status.player || {};
	const loggedIn = Boolean(player.loggedIn);
	// topbar
	$("#tb-conn").textContent = `127.0.0.1:${port}`;
	$("#tb-world").textContent = player.world ?? "—";
	$("#tb-state").textContent = player.gameState || "—";
	$("#tb-player").textContent = player.localPlayer || "—";
	$("#tb-update").textContent = lastRefreshAt ? lastRefreshAt.toLocaleTimeString() : "—";
	const dot = $("#tb-dot"); dot.className = `dot ${loggedIn ? "ok" : "warn"}`;
	// dock
	$("#dock-status").textContent = "Connected";
	$("#dock-uptime").textContent = connectedAt ? formatDuration(Date.now() - connectedAt.getTime()) : "—";
	$("#dock-version").textContent = status.version || status.apiVersion || "—";
	$("#dock-conn-dot").className = "dot ok";
	// mob-farmer header badge
	const mfBadge = $("#mf-badge");
	if (mobFarmer && !mobFarmer.unavailable) {
		mfBadge.innerHTML = badge(
			summary.running ? `Running · ${summary.live ? "Live" : "Dry"}` : "Stopped",
			summary.running ? "good" : "bad",
			{ live: summary.running, pulse: summary.running },
		);
	} else {
		mfBadge.innerHTML = badge("Unavailable", "warn");
	}
	// footer
	$("#ft-live").innerHTML = `Live data <b>${lastRefreshAt ? formatRelativeTime(lastRefreshAt) : "—"}</b>`;
	$("#ft-server").innerHTML = `Server <b>Connected</b>`;
	$("#ft-uptime").innerHTML = `Uptime <b>${connectedAt ? formatDuration(Date.now() - connectedAt.getTime()) : "—"}</b>`;
	$("#ft-export").innerHTML = `Local export <b>Enabled</b>`;
}

function renderDisconnected(message, port) {
	$("#tb-conn").textContent = port ? `127.0.0.1:${port} (offline)` : "Disconnected";
	$("#tb-dot").className = "dot bad";
	$("#dock-status").textContent = "Disconnected";
	$("#dock-conn-dot").className = "dot bad";
	$("#mf-badge").innerHTML = badge("Offline", "bad");
	$("#ft-server").innerHTML = `Server <b>Disconnected</b>`;
	const ov = $("#mf-overview");
	if (ov && !ov.dataset.hasContent) {
		ov.innerHTML = `<p class="scaffold-note">${message}</p>`;
	}
	addEvent(`Connection issue: ${message}`);
}

/* ---- timer -------------------------------------------------------------- */
function resetTimer() {
	if (timer) { clearInterval(timer); timer = null; }
	if ($("#auto-refresh").checked) timer = setInterval(() => refreshAll(), fastMs);
}

/* ---- wiring ------------------------------------------------------------- */
function wire() {
	$$(".nav-item").forEach((b) => b.addEventListener("click", () => showView(b.dataset.view)));
	$$(".tab").forEach((b) => b.addEventListener("click", () => showTab(b.dataset.tab)));
	$$("[data-mf-action]").forEach((b) => b.addEventListener("click", () => {
		const a = b.dataset.mfAction;
		if (a === "focus") mfFocusClick(); else mfAction(a);
	}));
	$$("[data-skill-action]").forEach((b) => b.addEventListener("click", () => skillAction(b.dataset.skill, b.dataset.skillAction)));

	// skill-farmer sub-tabs (Overview / Configuration / Debug)
	document.addEventListener("click", (e) => {
		const t = e.target.closest("[data-skill-tab]");
		if (t) showSkillTab(t.dataset.skill, t.dataset.skillTab);
	});
	// skill config bar actions (load / import / export / reset / apply)
	document.addEventListener("click", (e) => {
		const b = e.target.closest("[data-skill-cfg]");
		if (!b) return;
		const skill = b.dataset.skill;
		switch (b.dataset.skillCfg) {
			case "apply": applySkillConfig(skill); break;
			case "reset": resetSkillConfig(skill); break;
			case "load": loadSkillConfigIntoDraft(skill); break;
			case "import": importSkillConfig(skill); break;
			case "export": exportSkillConfig(skill); break;
		}
	});

	// minimap lens tabs + radius control
	document.addEventListener("click", (e) => {
		const lens = e.target.closest("[data-minimap-lens]");
		if (lens) {
			minimapLens = lens.dataset.minimapLens;
			$$("[data-minimap-lens]").forEach((b) => b.classList.toggle("active", b === lens));
			refreshAll();
			return;
		}
		const z = e.target.closest("[data-minimap-radius]");
		if (z) {
			// Shared with the farmer-page zoom buttons (one global visual radius in
			// pathGrid.js) so the SVG actually re-renders at the new size, not just
			// the underlying fetch -- otherwise the control changes data without
			// changing what's drawn.
			const r = setGridRadius(getGridRadius() + (z.dataset.minimapRadius === "in" ? 3 : -3));
			localStorage.setItem("cvHelperGridRadius", String(r));
			const lbl = $("#minimap-radius-val"); if (lbl) lbl.textContent = r;
			refreshAll();
		}
	});

	// global configuration tab actions
	document.addEventListener("click", (e) => {
		const b = e.target.closest("[data-global-cfg]");
		if (!b) return;
		switch (b.dataset.globalCfg) {
			case "apply": applyGlobalConfig(); break;
			case "reset": resetGlobalConfig(); break;
			case "load": loadGlobalConfigIntoDraft(); break;
			case "import": importGlobalConfig(); break;
			case "export": exportGlobalConfig(); break;
		}
	});

	$("#btn-refresh").addEventListener("click", () => refreshAll());
	$("#auto-refresh").addEventListener("change", resetTimer);

	$("#refresh-rate").addEventListener("change", (e) => {
		fastMs = Number(e.target.value) || 1000;
		localStorage.setItem("cvHelperRefreshMs", String(fastMs));
		slowCounter = 0;
		resetTimer();
	});

	// macro panel navigation
	document.addEventListener("click", (e) => {
		const m = e.target.closest(".macro-row");
		if (m) showView(m.dataset.view);
	});

	// config sub-tab apply / reset
	document.addEventListener("click", (e) => {
		const ap = e.target.closest("[data-cfg-apply]");
		if (ap) { applyMobConfig(ap.dataset.cfgApply); return; }
		const rs = e.target.closest("[data-cfg-reset]");
		if (rs) resetMobConfig(rs.dataset.cfgReset);
	});

	// presets
	document.addEventListener("click", (e) => {
		const ap = e.target.closest("[data-preset-apply]"); if (ap) { applyPreset(ap.dataset.presetApply); return; }
		const sv = e.target.closest("[data-preset-save]"); if (sv) { savePresetFlow(sv.dataset.presetSave); return; }
		const dl = e.target.closest("[data-preset-del]"); if (dl) deletePresetFlow(dl.dataset.presetDel);
	});
	// selecting a preset applies it immediately -- the checkmark button next to each
	// dropdown remains as an explicit re-apply, but requiring that extra click before
	// anything happens is not how a preset picker is expected to behave.
	["mob-preset", "mining-preset", "woodcutting-preset"].forEach((id) => {
		const sel = $(`#${id}`);
		if (sel) sel.addEventListener("change", () => applyPreset(id.replace("-preset", "")));
	});

	// path-grid zoom
	document.addEventListener("click", (e) => {
		const z = e.target.closest("[data-grid-zoom]");
		if (!z) return;
		const r = setGridRadius(getGridRadius() + (z.dataset.gridZoom === "in" ? -1 : 1));
		localStorage.setItem("cvHelperGridRadius", String(r));
		const lbl = $("#mfc-zoomval"); if (lbl) lbl.textContent = r * 2 + 1;
		refreshAll();
	});

	// quick-control config patches (event delegation; survives re-renders)
	document.addEventListener("change", (e) => {
		const ctl = e.target.closest("[data-cfg-key]");
		if (!ctl) return;
		const key = ctl.dataset.cfgKey;
		const value = ctl.type === "checkbox" ? ctl.checked : ctl.value;
		patchMobConfig(key, value);
	});

	$$("[data-capture]").forEach((b) => b.addEventListener("click", async () => {
		if (!(await ensurePort())) return;
		try { const r = await request(b.dataset.capture, { method: "POST" }); addEvent(`${r.capture || "capture"} queued`); }
		catch (err) { addEvent(`capture failed: ${formatError(err)}`); }
		await delay(800); await refreshAll();
	}));

	$("#btn-login").addEventListener("click", async () => {
		if (!(await ensurePort())) return;
		try { await request("/login/click", { method: "POST" }); addEvent("login click queued"); }
		catch (err) { addEvent(`login click failed: ${formatError(err)}`); }
		await delay(800); await refreshAll();
	});

	$("#dock-form").addEventListener("submit", async (e) => {
		e.preventDefault();
		if (!setPort($("#dock-port").value.trim())) { $("#dock-status").textContent = "Invalid port"; return; }
		await refreshAll(); resetTimer();
	});

	$("#dock-disconnect").addEventListener("click", () => {
		if (timer) { clearInterval(timer); timer = null; }
		$("#auto-refresh").checked = false;
		renderDisconnected("Disconnected by user.", state.port);
	});
}

/* ---- init --------------------------------------------------------------- */
function init() {
	const queryPort = sanitizePort(new URLSearchParams(location.search).get("port"));
	const remembered = readRememberedPorts();
	if (queryPort) setPort(queryPort);
	else if (remembered.length) setPort(remembered[0]);
	$("#dock-port").value = state.port;

	const savedRadius = Number(localStorage.getItem("cvHelperGridRadius"));
	if (savedRadius) setGridRadius(savedRadius);
	const minimapRadiusLbl = $("#minimap-radius-val"); if (minimapRadiusLbl) minimapRadiusLbl.textContent = getGridRadius();
	const rateSel = $("#refresh-rate"); if (rateSel) rateSel.value = String(fastMs);

	// fill bespoke emblem placeholders with custom inline SVG art
	$$("[data-emblem]").forEach((el) => { el.innerHTML = icon(el.dataset.emblem); });

	wire();
	["mob", "mining", "woodcutting"].forEach(populatePresets);
	showView("mob-farmer");   // open on the centrepiece per spec
	showTab("overview");

	discoverPort().then(() => refreshAll());
	resetTimer();
}

document.addEventListener("DOMContentLoaded", init);
