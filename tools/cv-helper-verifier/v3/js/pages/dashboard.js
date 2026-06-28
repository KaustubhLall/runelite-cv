/* ============================================================================
 * pages/dashboard.js — live overview scaffold in the v3 theme.
 * Faithful-enough subset of the reference dashboard; deeper widgets (captures,
 * full warnings) are intentionally staged for a later pass.
 * ========================================================================== */
import { panel, kvList, badge, table } from "../components.js";
import { icon } from "../icons.js";
import { escapeHtml, formatGp, formatRelativeTime, selectedValue } from "../format.js";

const root = () => document.querySelector("#dashboard-page");
const obj = (v) => (v && typeof v === "object" ? v : {});
const num = (v) => (typeof v === "number" ? v : undefined);

const SKILL_ORDER = [
	"attack", "hitpoints", "mining", "strength", "agility", "smithing", "defence", "herblore", "fishing",
	"ranged", "thieving", "cooking", "prayer", "crafting", "firemaking", "magic", "fletching", "woodcutting",
	"runecraft", "slayer", "farming", "construction", "hunter",
];

const SKILL_ICONS = {
	agility: "footprints",
	attack: "swords",
	construction: "hammer",
	cooking: "flame",
	crafting: "gem",
	defence: "shield",
	farming: "sprout",
	firemaking: "flame-kindling",
	fishing: "fish",
	fletching: "bow-arrow",
	herblore: "flask-conical",
	hitpoints: "heart",
	hunter: "radar",
	magic: "wand-2",
	mining: "pickaxe",
	prayer: "sparkles",
	ranged: "crosshair",
	runecraft: "circle-dot",
	slayer: "skull",
	smithing: "anvil",
	strength: "dumbbell",
	thieving: "key-round",
	woodcutting: "trees",
};

let latestStatsExport = null;

document.addEventListener("click", (event) => {
	const target = event.target instanceof Element ? event.target : null;
	const button = target && target.closest("[data-dashboard-export-stats]");
	if (!button) return;
	exportLatestStats(button);
});

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

function normalizeSkillKey(value) {
	return String(value || "")
		.replace(/_/g, " ")
		.replace(/([a-z])([A-Z])/g, "$1 $2")
		.trim()
		.toLowerCase()
		.replace(/\s+/g, " ");
}

function skillDisplayName(value) {
	const key = normalizeSkillKey(value);
	if (!key) return "Unknown";
	return key.split(" ").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function numberFrom(...values) {
	for (const value of values) {
		if (typeof value === "number" && Number.isFinite(value)) return value;
		if (typeof value === "string" && value.trim() !== "" && Number.isFinite(Number(value))) return Number(value);
	}
	return undefined;
}

function compactNumber(value) {
	return typeof value === "number" && Number.isFinite(value) ? value.toLocaleString() : "—";
}

function xpForLevel(level) {
	let points = 0;
	for (let i = 1; i < level; i += 1) {
		points += Math.floor(i + 300 * Math.pow(2, i / 7));
	}
	return Math.floor(points / 4);
}

function normalizeSkillSnapshot(rawKey, rawValue) {
	const value = obj(rawValue);
	const key = normalizeSkillKey(value.skill || value.name || rawKey);
	if (!key || key === "overall" || key === "total") return null;
	const realLevel = numberFrom(value.real, value.realLevel, value.base, value.baseLevel, value.level, value.staticLevel);
	const currentLevel = numberFrom(value.current, value.currentLevel, value.boosted, value.boostedLevel, value.modifiedLevel, value.level);
	const xp = numberFrom(value.xp, value.experience, value.currentXp, value.skillXp);
	let xpToNext = numberFrom(value.xpToNext, value.xpToNextLevel, value.remainingXp);
	let progressPercent = numberFrom(value.progressPercent, value.progress, value.percentToNextLevel);
	if (xp !== undefined && realLevel !== undefined && realLevel > 0 && realLevel < 99) {
		const currentLevelXp = xpForLevel(realLevel);
		const nextLevelXp = xpForLevel(realLevel + 1);
		xpToNext = xpToNext ?? Math.max(0, nextLevelXp - xp);
		progressPercent = progressPercent ?? ((xp - currentLevelXp) / Math.max(1, nextLevelXp - currentLevelXp)) * 100;
	}
	if (progressPercent !== undefined) progressPercent = Math.max(0, Math.min(100, progressPercent));
	return {
		key,
		name: skillDisplayName(key),
		iconName: SKILL_ICONS[key.replace(/\s+/g, "")] || SKILL_ICONS[key] || "circle-dot",
		realLevel,
		currentLevel,
		xp,
		xpToNext,
		progressPercent,
	};
}

function normalizeSkills(status, player) {
	const source = obj(player.skills || status.skills || obj(status.player).skills);
	if (!Object.keys(source).length) return [];
	let entries;
	if (Array.isArray(source)) {
		entries = source.map((value, index) => normalizeSkillSnapshot(value.skill || value.name || index, value));
	} else {
		entries = Object.entries(source).map(([key, value]) => normalizeSkillSnapshot(key, value));
	}
	return entries.filter(Boolean).sort((a, b) => {
		const ia = SKILL_ORDER.indexOf(a.key);
		const ib = SKILL_ORDER.indexOf(b.key);
		if (ia !== -1 || ib !== -1) return (ia === -1 ? 999 : ia) - (ib === -1 ? 999 : ib);
		return a.name.localeCompare(b.name);
	});
}

function statsSummary(skills) {
	return {
		totalLevel: skills.reduce((sum, skill) => sum + (skill.realLevel || 0), 0),
		totalXp: skills.reduce((sum, skill) => sum + (skill.xp || 0), 0),
		maxedSkills: skills.filter((skill) => (skill.realLevel || 0) >= 99).length,
	};
}

function renderSkillProgress(skill) {
	if (typeof skill.progressPercent !== "number") return "—";
	const label = `${Math.round(skill.progressPercent)}%`;
	return `<span title="${escapeHtml(label)} to next level">${escapeHtml(label)}</span><div class="m-bar"><i style="width:${skill.progressPercent}%"></i></div>`;
}

function renderStatsPanel(status, player, loggedIn) {
	const skills = normalizeSkills(status, player);
	if (!loggedIn) {
		latestStatsExport = null;
		return panel({ title: "Player Stats", iconName: "bar-chart-3", body: `<p class="empty compact">Log in to show the current account's full skill snapshot.</p>` });
	}
	if (!skills.length) {
		latestStatsExport = null;
		return panel({ title: "Player Stats", iconName: "bar-chart-3", body: `<p class="empty compact">No skill snapshot was found in the current /status payload.</p>` });
	}
	const summary = statsSummary(skills);
	latestStatsExport = {
		exportedAt: new Date().toISOString(),
		player: player.localPlayer || null,
		world: player.world ?? null,
		gameState: player.gameState || null,
		totalLevel: summary.totalLevel,
		totalXp: summary.totalXp,
		skills,
	};
	const rows = skills.map((skill) => ({ cells: [
		`<span class="skill-name"><span class="kv-ico">${icon(skill.iconName)}</span>${escapeHtml(skill.name)}</span>`,
		escapeHtml(selectedValue(skill.currentLevel, "—")),
		escapeHtml(selectedValue(skill.realLevel, "—")),
		escapeHtml(compactNumber(skill.xp)),
		escapeHtml(compactNumber(skill.xpToNext)),
		renderSkillProgress(skill),
	] }));
	const summaryHtml = `<div class="count-grid" style="margin-bottom:12px">
		${countCard("bar-chart-3", "Total level", summary.totalLevel)}
		${countCard("sparkles", "Total XP", compactNumber(summary.totalXp))}
		${countCard("award", "99s", summary.maxedSkills)}
		${countCard("user", "Player", player.localPlayer || "—")}
	</div>`;
	const actions = `<button class="btn sm" type="button" data-dashboard-export-stats><i data-lucide="download"></i> Export JSON</button>`;
	return panel({
		title: "Player Stats",
		iconName: "bar-chart-3",
		extra: actions,
		body: summaryHtml + table({
			columns: [
				{ label: "Skill" },
				{ label: "Current" },
				{ label: "Base" },
				{ label: "XP" },
				{ label: "To next" },
				{ label: "Progress" },
			],
			rows,
			empty: "No skill data.",
		}),
	});
}

function exportLatestStats(button) {
	if (!latestStatsExport) return;
	const filenamePlayer = String(latestStatsExport.player || "player").toLowerCase().replace(/[^a-z0-9_-]+/g, "-").replace(/^-|-$/g, "") || "player";
	const blob = new Blob([JSON.stringify(latestStatsExport, null, 2)], { type: "application/json" });
	const url = URL.createObjectURL(blob);
	const link = document.createElement("a");
	link.href = url;
	link.download = `cv-helper-${filenamePlayer}-stats.json`;
	document.body.appendChild(link);
	link.click();
	link.remove();
	URL.revokeObjectURL(url);
	const original = button.innerHTML;
	button.innerHTML = `${icon("check")} Exported`;
	window.setTimeout(() => { button.innerHTML = original; }, 1400);
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
				{ iconName: "coins", k: "Carried (GE)", v: formatGp(num(wealth.totalCarriedValueGe)), tone: "gold" },
				{ iconName: "wand-2", k: "Carried (HA)", v: formatGp(num(wealth.totalCarriedValueHa)), tone: "gold" },
				{ iconName: "shield", k: "Equipment (GE)", v: formatGp(num(obj(wealth.equipment).gePrice)) },
				{ iconName: "alert-triangle", k: "Risked (GE)", v: formatGp(num(wealth.riskedValueGeApprox)) },
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

		<div style="margin-top:var(--gap)">
			${renderStatsPanel(status, player, loggedIn)}
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