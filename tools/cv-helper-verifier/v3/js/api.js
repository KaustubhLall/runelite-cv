/* ============================================================================
 * api.js — CV Helper transport: port discovery, fetch, remembered ports.
 * Same endpoints + discovery semantics as v1/v2 so it stays compatible with
 * every plugin build. No rendering here — callers own the DOM.
 * ========================================================================== */
import { sanitizePort, formatError } from "./format.js";

export const DEFAULT_PORT = "11777";
const DISCOVERY_HELPER_URL = "http://127.0.0.1:8765/api/discover";
const REMEMBERED_KEY = "cvHelperRememberedPorts";
const MAX_REMEMBERED = 8;

export const state = {
	port: DEFAULT_PORT,
	discovery: emptyDiscovery(),
	lastStatus: null,       // most recent /status payload (shared with pages)
};

export function emptyDiscovery() {
	return {
		helperAvailable: false,
		activePort: "",
		source: "startup",
		sourceLabel: "startup",
		summary: "Starting from 11777; will re-scan if that port goes stale.",
		error: "",
		attempts: [],
	};
}

export function baseUrl(port = state.port) {
	return `http://127.0.0.1:${port}`;
}

export async function request(path, options = {}, port = state.port) {
	const resolved = sanitizePort(port);
	if (!resolved) throw new Error("No active CV Helper port selected.");
	const response = await fetch(`${baseUrl(resolved)}${path}`, options);
	if (!response.ok) {
		let details = "";
		try {
			const payload = await response.json();
			if (Array.isArray(payload.errors)) details = `: ${payload.errors.join("; ")}`;
			else if (payload.error) details = `: ${payload.error}`;
		} catch { /* ignore */ }
		throw new Error(`${path} returned ${response.status}${details}`);
	}
	return response.json();
}

export async function requestOptional(path, options = {}, port = state.port) {
	try {
		return await request(path, options, port);
	} catch (error) {
		return { unavailable: true, error: formatError(error), path };
	}
}

export async function ensurePort() {
	if (sanitizePort(state.port)) return true;
	return Boolean(await discoverPort());
}

export function setPort(next) {
	const p = sanitizePort(next);
	if (!p) return false;
	state.port = p;
	rememberPort(p);
	return true;
}

/* ---- Remembered ports --------------------------------------------------- */
export function readRememberedPorts() {
	try {
		const raw = window.localStorage.getItem(REMEMBERED_KEY);
		const parsed = raw ? JSON.parse(raw) : [];
		return Array.isArray(parsed) ? parsed.map(sanitizePort).filter(Boolean) : [];
	} catch { return []; }
}

export function rememberPort(port) {
	const p = sanitizePort(port);
	if (!p) return;
	const next = [p, ...readRememberedPorts().filter((x) => x !== p)].slice(0, MAX_REMEMBERED);
	try { window.localStorage.setItem(REMEMBERED_KEY, JSON.stringify(next)); } catch { /* ignore */ }
}

/* ---- Discovery ---------------------------------------------------------- */
export async function discoverPort() {
	const viaHelper = await discoverViaHelper();
	if (viaHelper) {
		applyDiscovery(viaHelper);
		if (sanitizePort(viaHelper.activePort)) return sanitizePort(viaHelper.activePort);
	}
	const fallback = await discoverInBrowser();
	applyDiscovery(fallback);
	return fallback.activePort || "";
}

function applyDiscovery(payload) {
	const activePort = sanitizePort(payload.activePort);
	if (activePort) setPort(activePort);
	state.discovery = {
		helperAvailable: Boolean(payload.helperAvailable),
		activePort: activePort || "",
		source: payload.source || "unknown",
		sourceLabel: payload.sourceLabel || (payload.source || "unknown"),
		summary: payload.summary || emptyDiscovery().summary,
		error: payload.error || "",
		attempts: Array.isArray(payload.attempts) ? payload.attempts : [],
	};
}

async function discoverViaHelper() {
	const known = candidateSeeds().map((s) => s.port).join(",");
	try {
		const url = new URL(DISCOVERY_HELPER_URL);
		if (known) url.searchParams.set("known", known);
		const response = await fetch(url.toString(), { mode: "cors" });
		if (!response.ok) throw new Error(`/api/discover returned ${response.status}`);
		return response.json();
	} catch (error) {
		return {
			helperAvailable: false, activePort: "", source: "helper-offline", sourceLabel: "helper offline",
			summary: "Verifier helper offline. Browser fallback can still probe 11777 and remembered ports.",
			error: formatError(error), attempts: [],
		};
	}
}

async function discoverInBrowser() {
	const attempts = [];
	for (const seed of candidateSeeds()) {
		const attempt = await probePort(seed.port, seed.source);
		attempts.push(attempt);
		if (attempt.ok) {
			return {
				helperAvailable: false, activePort: attempt.activePort, source: attempt.source,
				sourceLabel: attempt.source, summary: `Browser fallback recovered CV Helper on ${attempt.activePort}.`,
				error: "", attempts,
			};
		}
	}
	return {
		helperAvailable: false, activePort: "", source: "browser-fallback", sourceLabel: "browser fallback",
		summary: "No live CV Helper export found through browser fallback.",
		error: attempts.length ? attempts[attempts.length - 1].error || "" : "no-probe-attempts", attempts,
	};
}

function candidateSeeds() {
	const seeds = [
		{ port: DEFAULT_PORT, source: "preferred-port" },
		{ port: sanitizePort(state.port), source: "current-port" },
		...readRememberedPorts().map((p) => ({ port: p, source: "remembered-port" })),
	];
	const seen = new Set();
	return seeds.filter((s) => s.port && !seen.has(s.port) && seen.add(s.port));
}

async function probePort(port, source) {
	const started = Date.now();
	try {
		const status = await request("/status", {}, port);
		if (status.plugin !== "CV Helper") throw new Error(`Unexpected plugin ${status.plugin || "unknown"}`);
		return {
			port, source, ok: true,
			activePort: sanitizePort(status.port) || port,
			gameState: status.player?.gameState || "Unknown",
			status: status.status || "ok",
			durationMs: Date.now() - started,
		};
	} catch (error) {
		return { port, source, ok: false, error: formatError(error), durationMs: Date.now() - started };
	}
}
