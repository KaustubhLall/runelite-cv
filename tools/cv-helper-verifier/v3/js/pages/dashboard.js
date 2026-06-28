/* ============================================================================
 * pages/dashboard.js — live overview scaffold in the v3 theme.
 * Faithful-enough subset of the reference dashboard; deeper widgets (captures,
 * full warnings) are intentionally staged for a later pass.
 * ========================================================================== */
import { panel, kvList, badge, gpValue } from "../components.js";
import { icon } from "../icons.js";
import { escapeHtml, formatGp, formatRelativeTime, selectedValue } from "../format.js";

const root = () => document.querySelector("#dashboard-page");
const obj = (v) => (v && typeof v === "object" ? v : {});
const num = (v) => (typeof v === "number" ? v : undefined);

function countCard(iconName, label, value) {
	return `<div class="count-card"><span class="cc-ico">${icon(iconName)}</span><div class="cc-body"><span class="cc-value">${escapeHtml(String(value ?? "—"))}</span><span class="cc-label">${escapeHtml(label)}</span></div></div>`;
}

/* Pull the on-demand skill status the farmer view stashes on the status obj. */
function skillScene(status, skill) {
	return obj(obj(status.__skillStatus)[skill]);
}

function farmerCard(title, ico, st, opts = {}) {
	const o = obj(st);
	const running = Boolean(o.running);
	const unavailable = !st || o.unavailable;
	const sc = opts.scene || {};
	const sm = obj(sc.candidateSummary);
	const sel = obj(sc.selected);
	const target = o.target || sc.target;
	const action = opts.action || sc.currentAction || sc.decision;
	const fail = sc.lastFailureReason || sc.completionReason;
	const rows = [
		{ iconName: ico, k: "Target", v: escapeHtml(selectedValue(target, "—")) },
		{ iconName: "activity", k: "State", v: unavailable ? "Unavailable" : running ? "Running" : "Stopped", tone: unavailable ? "warn" : running ? "good" : "" },
		{ iconName: "radio", k: "Mode", v: o.live ? "Live" : "Dry" },
	];
	if (opts.skill) {
		rows.push({ iconName: "crosshair", k: "Selected", v: escapeHtml(selectedValue(sel.name, "—")) });
		rows.push({ iconName: "swords", k: "Action", v: escapeHtml(selectedValue(action, "—")) });
		if (sm.totalCandidates !== undefined) rows.push({ iconName: "radar", k: "Candidates", v: `${selectedValue(sm.matchedReachable, 0)} ✓ / ${selectedValue(sm.totalCandidates, 0)}` });
		if (fail) rows.push({ iconName: "alert-triangle", k: "Last issue", v: escapeHtml(String(fail)), tone: "bad" });
	}
	return panel({ title, iconName: ico, extra: unavailable ? "—" : running ? "running" : "stopped", body: kvList(rows) });
}

function sceneDiagnostics(status) {
	const skills = ["mining", "woodcutting"];
	const blocks = skills.map((skill) => {
		const sc = skillScene(status, skill);
		if (!sc || !Object.keys(sc).length) return "";
		const sm = obj(sc.candidateSummary);
		const sel = obj(sc.selected);
		const footprints = Array.isArray(sc.candidates) ? sc.candidates.filter((c) => {
			const fp = c.objectFootprint || c.objectSize; return fp && (Number(fp.width) > 1 || Number(fp.height) > 1);
		}).length : 0;
		const footprintTiles = Array.isArray(sc.candidates) ? sc.candidates.reduce((n, c) => {
			const fp = c.objectFootprint || c.objectSize; return n + (fp ? Math.max(1, Number(fp.width) || 1) * Math.max(1, Number(fp.height) || 1) : 1);
		}, 0) : 0;
		return `<div class="cfg-section" style="margin-bottom:10px"><h4 class="cfg-section-h">${escapeHtml(skill)}</h4>${kvList([
			{ iconName: "radar", k: "Scan radius", v: `${selectedValue(sc.scanRadiusTiles, "—")} tiles` },
			{ iconName: "list", k: "Candidates", v: `${selectedValue(sm.totalCandidates, 0)} (${selectedValue(sm.matchedReachable, 0)} reachable)` },
			{ iconName: "crosshair", k: "Selected", v: escapeHtml(selectedValue(sel.name, "—")) },
			{ iconName: "x-circle", k: "No-action / blocked", v: `${selectedValue(sm.missingAction, 0)} / ${selectedValue(sm.collisionBlocked, 0)}`, tone: (sm.missingAction || sm.collisionBlocked) ? "bad" : "" },
			{ iconName: "box", k: "Multi-tile objects", v: `${footprints} (~${footprintTiles} tiles)` },
		])}</div>`;
	}).filter(Boolean).join("");
	return blocks || `<p class="empty compact">Open a skill farmer page to populate scene diagnostics.</p>`;
}

export function renderDashboard(status, mobFarmer, events, counts) {
	const el = root();
	if (!el) return;
	if (!status) {
		el.innerHTML = `<p class="scaffold-note">Waiting for the first /status response…</p>`;
		return;
	}
	const player = obj(status.player);
	const auto = obj(player.automation);
	const vitals = obj(player.vitals);
	const wealth = obj(player.wealth);
	const iface = obj(player.interfaces);
	const loggedIn = Boolean(player.loggedIn);
	const c = obj(counts);

	const warnings = Array.isArray(status.warnings) ? status.warnings.slice() : [];
	if (!loggedIn) warnings.push("Client is not logged in — live target surfaces will be empty.");
	const hp = obj(vitals.hitpoints);
	if (loggedIn && num(hp.current) !== undefined && num(hp.max) && hp.current / hp.max < 0.3) warnings.push("Hitpoints below 30%.");

	const prayer = obj(vitals.prayer);
	const hero = `
		<div class="page-head" style="margin-bottom:var(--gap)">
			<span class="emblem">${icon(loggedIn ? "shield" : "swords")}</span>
			<div class="ph-titles">
				<h1>${loggedIn ? "Client Ready" : "Awaiting Login"}</h1>
				<div class="ph-sub">${escapeHtml(player.localPlayer || "Not logged in")} · World ${escapeHtml(selectedValue(player.world, "—"))} · ${escapeHtml(player.gameState || "")}</div>
			</div>
			<div class="ph-actions">${badge(loggedIn ? "Logged in" : "Login screen", loggedIn ? "good" : "warn", { live: loggedIn, pulse: loggedIn })}</div>
		</div>`;

	el.innerHTML = hero + `
		<div class="grid cols-4">
			${panel({ title: "Connection", iconName: "plug", body: kvList([
				{ iconName: "hash", k: "Active port", v: escapeHtml(selectedValue(status.port, "—")) },
				{ iconName: "circle-dot", k: "Status", v: "Connected", tone: "good" },
				{ iconName: "gamepad-2", k: "Game state", v: escapeHtml(player.gameState || "—"), tone: loggedIn ? "good" : "warn" },
				{ iconName: "user", k: "Player", v: escapeHtml(player.localPlayer || "—") },
				{ iconName: "globe", k: "World", v: escapeHtml(selectedValue(player.world, "—")) },
			]) })}
			${panel({ title: "Vitals", iconName: "heart", body: kvList([
				{ iconName: "heart", k: "Hitpoints", v: `${selectedValue(hp.current ?? hp.boosted, "—")} / ${selectedValue(hp.max ?? hp.real, "—")}`, tone: "good" },
				{ iconName: "sparkles", k: "Prayer", v: `${selectedValue(prayer.current ?? prayer.boosted, "—")} / ${selectedValue(prayer.max ?? prayer.real, "—")}`, tone: "info" },
				{ iconName: "zap", k: "Run energy", v: typeof status.player?.runEnergy === "number" ? `${Math.round(player.runEnergy / 100)}%` : "—" },
				{ iconName: "weight", k: "Weight", v: `${selectedValue(player.weight, "—")} kg` },
			]) })}
			${panel({ title: "Wealth", iconName: "coins", body: kvList([
				{ iconName: "coins", k: "Carried (GE)", v: gpValue(num(wealth.totalCarriedValueGe)), tone: "" },
				{ iconName: "wand-2", k: "Carried (HA)", v: gpValue(num(wealth.totalCarriedValueHa)), tone: "" },
				{ iconName: "shield", k: "Equipment (GE)", v: gpValue(num(obj(wealth.equipment).gePrice)), tone: "" },
				{ iconName: "alert-triangle", k: "Risked (GE)", v: gpValue(num(wealth.riskedValueGeApprox)), tone: "" },
			]) })}
			${panel({ title: "Interface", iconName: "layout", body: kvList([
				{ iconName: "panel-top", k: "Open panel", v: escapeHtml(selectedValue(iface.openPanel || iface.tab, "—")) },
				{ iconName: "book", k: "Spellbook", v: escapeHtml(selectedValue(player.spellbook?.name || player.spellbook, "—")) },
				{ iconName: "map-pin", k: "World location", v: player.worldLocation ? `${player.worldLocation.x}, ${player.worldLocation.y}, ${player.worldLocation.plane}` : "—" },
			]) })}
		</div>

		<div class="grid cols-2" style="margin-top:var(--gap)">
			${panel({ title: "Target Counts", iconName: "crosshair", body: `<div class="count-grid">
				${countCard("sparkles", "Prayer", c.prayer)}
				${countCard("wand-2", "Spell", c.spell)}
				${countCard("map", "Minimap", c.minimap)}
				${countCard("backpack", "Inventory", c.inventory)}
				${countCard("shield", "Equipment", c.equipment)}
				${countCard("layout", "Panels", c.panels)}
				${countCard("swords", "Combat", c.combat)}
				${countCard("user", "Entities", c.entities)}
			</div>` })}
			${panel({ title: "Warnings", iconName: "alert-triangle", body: warnings.length
				? `<div class="kv">${warnings.map((w) => `<div class="kv-row"><span class="kv-ico tone-bad">${icon("alert-triangle")}</span><span class="kv-k">${escapeHtml(w)}</span></div>`).join("")}</div>`
				: `<div class="all-nominal">${icon("check-circle")} <span>All systems nominal — no verifier warnings.</span></div>` })}
		</div>

		<div class="grid cols-3" style="margin-top:var(--gap)">
			${farmerCard("Mob Farmer", "skull", mobFarmer, { action: mobFarmer?.status })}
			${farmerCard("Mining", "pickaxe", obj(auto.mining), { skill: "mining", scene: skillScene(status, "mining") })}
			${farmerCard("Woodcutting", "trees", obj(auto.woodcutting), { skill: "woodcutting", scene: skillScene(status, "woodcutting") })}
		</div>

		<div class="grid cols-2" style="margin-top:var(--gap)">
			${panel({ title: "Scene Diagnostics", iconName: "radar", body: sceneDiagnostics(status) })}
			${panel({ title: "Recent Activity", iconName: "scroll-text", body:
				(events && events.length)
					? `<div class="events">${events.slice(0, 12).map((e) => `<div class="ev-row"><span class="ev-t">${escapeHtml(e.time instanceof Date ? e.time.toLocaleTimeString() : "")}</span><span class="ev-d"></span><span class="ev-m">${escapeHtml(e.message)}</span></div>`).join("")}</div>`
					: `<p class="empty compact">No activity recorded yet.</p>`
			})}
		</div>`;
}
