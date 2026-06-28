/* ============================================================================
 * format.js — pure formatting helpers (no DOM, no side effects)
 * Ported/cleaned from v1/v2 so value semantics stay identical.
 * ========================================================================== */

import { getGpTier } from "./gpSettings.js";

export function escapeHtml(value) {
	return String(value ?? "")
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;")
		.replace(/'/g, "&#039;");
}

export function formatError(error) {
	return error instanceof Error ? error.message : String(error || "Unknown error");
}

export function delay(ms) {
	return new Promise((resolve) => window.setTimeout(resolve, ms));
}

export function sanitizePort(value) {
	const text = String(value || "").trim();
	return /^[0-9]{2,5}$/.test(text) ? text : "";
}

export function selectedValue(value, fallback) {
	return value === undefined || value === null ? fallback : String(value);
}

export function formatDuration(ms) {
	if (!ms || ms <= 0) return "00:00:00";
	const s = Math.floor(ms / 1000) % 60;
	const m = Math.floor(ms / 60000) % 60;
	const h = Math.floor(ms / 3600000);
	return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

export function formatRelativeTime(date) {
	if (!date) return "—";
	const diffMs = Date.now() - date.getTime();
	if (diffMs < 0) return "just now";
	const sec = Math.floor(diffMs / 1000);
	const min = Math.floor(sec / 60);
	const hour = Math.floor(min / 60);
	const day = Math.floor(hour / 24);
	if (sec < 60) return `${sec}s ago`;
	if (min < 60) return `${min}m ${sec % 60}s ago`;
	if (hour < 24) return `${hour}h ${min % 60}m ago`;
	return `${day}d ${hour % 24}h ago`;
}

/** GP value colored by the shared frontend tier settings; returns HTML span. */
export function formatGp(value, { label = "GP value" } = {}) {
	if (typeof value !== "number" || !Number.isFinite(value)) return `<span class="gp-value gp-tier-unknown" title="${escapeHtml(label)}: unknown">—</span>`;
	const abs = Math.abs(value);
	let text;
	if (abs >= 1_000_000_000) text = `${(value / 1e9).toFixed(1)}b`;
	else if (abs >= 1_000_000) text = `${(value / 1e6).toFixed(abs >= 100_000_000 ? 0 : 1)}m`;
	else if (abs >= 1_000) text = `${(value / 1e3).toFixed(0)}k`;
	else text = `${Math.round(value)}`;
	const tier = getGpTier(value);
	const exact = Math.round(value).toLocaleString("en-US");
	return `<span class="gp-value gp-tier-${tier}" data-gp-value="${value}" data-gp-tier="${tier}" title="${escapeHtml(label)}: ${exact} GP (${tier})">${text}</span>`;
}

export function formatPoint(point) {
	if (!point) return "—";
	if (point.plane !== undefined) return `${point.x}, ${point.y}, ${point.plane}`;
	if (point.x !== undefined && point.y !== undefined) return `${point.x}, ${point.y}`;
	return point.value || "—";
}

/** OSRS region id from world coords (64x64 region grid). */
export function regionId(point) {
	if (!point || point.x === undefined) return null;
	return ((point.x >> 6) << 8) | (point.y >> 6);
}

/** 8-way compass direction from a tile delta (screen N = +y in world). */
export function compass(dx, dy) {
	if (!dx && !dy) return "—";
	const dirs = ["E", "NE", "N", "NW", "W", "SW", "S", "SE"];
	const ang = Math.atan2(dy, dx); // dy up = north
	let idx = Math.round((ang / (Math.PI / 4)));
	idx = ((idx % 8) + 8) % 8;
	return dirs[idx];
}

const DIR_LABELS = { N: "North", S: "South", E: "East", W: "West", NE: "North-east", NW: "North-west", SE: "South-east", SW: "South-west" };
export function compassLong(dir) { return DIR_LABELS[dir] || dir; }

/** Collapse a noisy farmer status string into a short human action verb. */
export function humanizeAction(raw) {
	const s = String(raw || "").toLowerCase();
	if (!s) return { label: "IDLE", tone: "muted" };
	if (s.includes("panic")) return { label: "PANIC STOP", tone: "bad" };
	if (s.startsWith("stop")) return { label: "STOPPED", tone: "muted" };
	if (s.includes("attack")) return { label: "ATTACKING", tone: "good" };
	if (s.includes("loot")) return { label: "LOOTING", tone: "gold" };
	if (s.includes("waiting-for-loot") || s.includes("loot-spawn")) return { label: "AWAITING LOOT", tone: "warn" };
	if (s.includes("eat") || s.includes("food")) return { label: "EATING", tone: "good" };
	if (s.includes("login")) return { label: "LOGIN RECOVERY", tone: "warn" };
	if (s.includes("continuing") || s.includes("engag")) return { label: "ENGAGING", tone: "good" };
	if (s.includes("focus")) return { label: "FOCUS CLICK", tone: "info" };
	if (s.includes("intermediate")) return { label: "INTERMEDIATE", tone: "info" };
	if (s.includes("blocked") || s.includes("no-food")) return { label: "BLOCKED", tone: "bad" };
	if (s.includes("off-canvas")) return { label: "REPOSITIONING", tone: "warn" };
	if (s.includes("skip") || s.includes("wait")) return { label: "WAITING", tone: "warn" };
	if (s.startsWith("target@") || s.startsWith("dry-target")) return { label: "TARGETING", tone: "good" };
	return { label: raw.split(":")[0].replace(/-/g, " ").toUpperCase(), tone: "" };
}

/** Tone for an arbitrary decision/reason string. */
export function decisionTone(value) {
	const t = String(value || "").toLowerCase();
	if (t.includes("fail") || t.includes("blocked") || t.includes("unreachable") || t.includes("error") || t.includes("reject")) return "bad";
	if (t.includes("skip") || t.includes("wait") || t.includes("unknown") || t.includes("hold")) return "warn";
	if (t.includes("ok") || t.includes("select") || t.includes("reachable") || t.includes("interact") || t.includes("attack") || t.includes("loot")) return "good";
	return "";
}

export function compactValue(value) {
	if (value === null || value === undefined) return "";
	if (Array.isArray(value)) return value.map(compactValue).join(", ");
	if (typeof value === "object") {
		const label = value.name || value.label || value.decision || value.status || value.target || value.result;
		return label ? String(label) : JSON.stringify(value);
	}
	return String(value);
}

/** Format pipe-separated food items as a compact list with HP placeholder. */
export function formatFoodItems(value) {
	if (!value || typeof value !== "string") return "";
	const items = value.split("|").map((s) => s.trim()).filter((s) => s);
	if (!items.length) return "";
	return items.map((item) => `${escapeHtml(item)} <span class="food-hp">❤ -</span>`).join(", ");
}

/** Flatten an object into [label, value] rows for compact detail tables. */
export function structuredRows(payload, prefix = "") {
	if (payload === null || payload === undefined) return [];
	if (typeof payload !== "object") return [[prefix || "value", String(payload)]];
	if (Array.isArray(payload)) return payload.slice(0, 12).map((item, i) => [`${prefix || "item"} ${i + 1}`, compactValue(item)]);
	return Object.entries(payload).flatMap(([key, value]) => {
		const label = prefix ? `${prefix}.${key}` : key;
		if (value && typeof value === "object" && !Array.isArray(value)) {
			const nested = structuredRows(value, label);
			return nested.length ? nested.slice(0, 16) : [[label, ""]];
		}
		return [[label, compactValue(value)]];
	}).slice(0, 28);
}
