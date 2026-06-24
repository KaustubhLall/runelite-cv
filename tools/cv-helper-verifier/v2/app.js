const DEFAULT_PORT = "11777";
const DISCOVERY_HELPER_URL = "http://127.0.0.1:8765/api/discover";
const MAX_REMEMBERED_PORTS = 8;
const PROFILE_STORAGE_KEY = "cvHelperMobFarmerProfiles";
const LOGIN_SCREEN_STATES = new Set([
	"LOGIN_SCREEN",
	"LOGIN_SCREEN_AUTHENTICATOR",
	"LOGGING_IN",
	"STARTING",
]);
const surfaces = ["prayer", "spell", "minimap", "inventory", "equipment", "panels", "combat"];

const navButtons = Array.from(document.querySelectorAll("[data-view]"));
const views = Array.from(document.querySelectorAll("[data-view-panel]"));
const farmerTabButtons = Array.from(document.querySelectorAll("[data-farmer-tab]"));
const farmerPanels = Array.from(document.querySelectorAll("[data-farmer-panel]"));
const form = document.querySelector("#connection-form");
const portInput = document.querySelector("#port");
const connectionStatus = document.querySelector("#connection-status");
const connectionUrl = document.querySelector("#connection-url");
const topBarTitle = document.querySelector("#top-bar-title");
const runtimeStatus = document.querySelector("#runtime-status");
const topBarWorld = document.querySelector("#top-bar-world");
const topBarGameState = document.querySelector("#top-bar-game-state");
const topBarPlayer = document.querySelector("#top-bar-player");
const bigStatus = document.querySelector("#big-status");
const connectionGrid = document.querySelector("#connection-grid");
const vitalsGrid = document.querySelector("#vitals-grid");
const wealthGrid = document.querySelector("#wealth-grid");
const interfaceGrid = document.querySelector("#interface-grid");
const countGrid = document.querySelector("#count-grid");
const warningsList = document.querySelector("#warnings");
const entitiesRoot = document.querySelector("#entities");
const surfacesRoot = document.querySelector("#surfaces");
const inventoryRoot = document.querySelector("#inventory-root");
const inventorySummary = document.querySelector("#inventory-summary");
const inventoryItemsRoot = document.querySelector("#inventory-items-root");
const equipmentItemsRoot = document.querySelector("#equipment-items-root");
const dropPolicyRoot = document.querySelector("#drop-policy-root");
const rawDataRoot = document.querySelector("#raw-data-root");
const refreshNow = document.querySelector("#refresh-now");
const autoRefresh = document.querySelector("#auto-refresh");
const autoDiscover = document.querySelector("#auto-discover");
const bigStatusRow = document.querySelector("#big-status-row");
const topBarUptime = document.querySelector("#top-bar-uptime");
const topBarLastUpdate = document.querySelector("#top-bar-last-update");
const recentActivity = document.querySelector("#recent-activity");
const footerProfile = document.querySelector("#footer-profile");
const footerMode = document.querySelector("#footer-mode");
const footerMemory = document.querySelector("#footer-memory");
const footerCpu = document.querySelector("#footer-cpu");
const captureStatus = document.querySelector("#capture-status");
const captureGrid = document.querySelector("#capture-grid");
const clickLogin = document.querySelector("#click-login");
const discoverySummary = document.querySelector("#discovery-summary");
const discoveryGrid = document.querySelector("#discovery-grid");
const discoveryAttempts = document.querySelector("#discovery-attempts");
const mobFarmerState = document.querySelector("#mob-farmer-state");
const mobFarmerDecision = document.querySelector("#mob-farmer-decision");
const mobFarmerStatusGrid = document.querySelector("#mob-farmer-status-grid");
const mobFarmerRawToggle = document.querySelector("#mob-farmer-raw-toggle");
const mobFarmerClearLog = document.querySelector("#mob-farmer-clear-log");
const mobFarmerLog = document.querySelector("#mob-farmer-log");
const mobFarmerRaw = document.querySelector("#mob-farmer-raw");
const mobFarmerDetails = document.querySelector("#mob-farmer-details");
const mobFarmerTarget = document.querySelector("#mob-farmer-target");
const mobFarmerMode = document.querySelector("#mob-farmer-mode");
const mobFarmerStep = document.querySelector("#mob-farmer-step");
const mobFarmerStart = document.querySelector("#mob-farmer-start");
const mobFarmerStop = document.querySelector("#mob-farmer-stop");
const mobFarmerFocusClick = document.querySelector("#mob-farmer-focus-click");
const mobFarmerLiveDate = document.querySelector("#mob-farmer-live-date");
const mobFarmerUptime = document.querySelector("#mob-farmer-uptime");
const mobFarmerConfigStatus = document.querySelector("#mob-farmer-config-status");
const panicStop = document.querySelector("#panic-stop");
const mobFarmerLoadConfig = document.querySelector("#mob-farmer-load-config");
const mobFarmerSaveConfig = document.querySelector("#mob-farmer-save-config");
const mobFarmerResetDraft = document.querySelector("#mob-farmer-reset-draft");
const mobFarmerExportConfig = document.querySelector("#mob-farmer-export-config");
const mobFarmerImportConfig = document.querySelector("#mob-farmer-import-config");
const mobFarmerProfileSelect = document.querySelector("#mob-farmer-profile-select");
const mobFarmerProfileName = document.querySelector("#mob-farmer-profile-name");
const mobFarmerProfileSave = document.querySelector("#mob-farmer-profile-save");
const mobFarmerProfileLoad = document.querySelector("#mob-farmer-profile-load");
const mobFarmerProfileDuplicate = document.querySelector("#mob-farmer-profile-duplicate");
const mobFarmerProfileDelete = document.querySelector("#mob-farmer-profile-delete");
const mobFarmerConfigErrors = document.querySelector("#mob-farmer-config-errors");
const mobFarmerConfigForm = document.querySelector("#mob-farmer-config-form");
const mobFarmerConfigJson = document.querySelector("#mob-farmer-config-json");
const actionsConfigStatus = document.querySelector("#actions-config-status");
const actionsLoadConfig = document.querySelector("#actions-load-config");
const actionsSaveConfig = document.querySelector("#actions-save-config");
const actionsResetDraft = document.querySelector("#actions-reset-draft");
const actionsConfigErrors = document.querySelector("#actions-config-errors");
const actionsSlotList = document.querySelector("#actions-slot-list");
const skillFarmers = {
	mining: skillFarmerRefs("mining"),
	woodcutting: skillFarmerRefs("woodcutting"),
};

let port = DEFAULT_PORT;
let timer = null;
let mobFarmerEvents = [];
let mobFarmerRawPayload = null;
let mobFarmerConfigPayload = null;
let mobFarmerDraftPayload = null;
let mobFarmerDraftDirty = false;
let mobFarmerLiveChangedWhileDirty = false;
let mobFarmerLastLiveFingerprint = "";
const skillFarmerConfigPayloads = {};
const skillFarmerDraftPayloads = {};
const skillFarmerDraftDirty = {};
const skillFarmerLiveChangedWhileDirty = {};
const skillFarmerLastLiveFingerprints = {};
const skillFarmerRunControlDrafts = {};
let latestRawPayloads = {};
let discoveryState = emptyDiscoveryState();
let recentActivityLog = [];
let lastRefreshAt = null;
let lastLoggedInState = null;
let firstStatusReceived = false;
let connectedAt = null;

function icon(name, extraClass = "") {
	return `<i data-lucide="${escapeHtml(name)}"${extraClass ? ` class="${escapeHtml(extraClass)}"` : ""}></i>`;
}

function refreshIcons() {
	if (window.lucide && lucide.createIcons) {
		lucide.createIcons();
	}
}

const queryPort = sanitizePort(new URLSearchParams(window.location.search).get("port"));
const rememberedPorts = readRememberedPorts();
if (queryPort) {
	port = queryPort;
	rememberPort(queryPort);
} else if (rememberedPorts.length) {
	port = rememberedPorts[0];
}
portInput.value = port;

setConnection("Ready", "neutral");
setRuntime("Waiting for /status", "neutral");
renderDiscovery();
renderMobFarmerProfiles();

navButtons.forEach(button => {
	button.addEventListener("click", () => {
		showView(button.getAttribute("data-view"));
		const farmer = button.getAttribute("data-farmer-target");
		if (farmer) {
			showFarmerTab(farmer);
		}
	});
});

document.addEventListener("click", event => {
	const jump = event.target.closest("[data-jump-config]");
	if (!jump) {
		return;
	}

	const target = jump.getAttribute("data-jump-config") || "mob";
	showView("farmers-view");
	showFarmerTab(target);

	window.setTimeout(() => {
		const panel = document.querySelector(`[data-farmer-panel="${CSS.escape(target)}"]`);
		const config = panel?.querySelector(".config-card");
		if (config) {
			config.scrollIntoView({ behavior: "smooth", block: "start" });
		}
	}, 50);
});

farmerTabButtons.forEach(button => {
	button.addEventListener("click", () => showFarmerTab(button.getAttribute("data-farmer-tab")));
});

form.addEventListener("submit", async event => {
	event.preventDefault();
	const requestedPort = sanitizePort(portInput.value.trim());
	if (!requestedPort) {
		setConnection("Enter a valid local port", "bad");
		return;
	}
	port = requestedPort;
	portInput.value = requestedPort;
	rememberPort(requestedPort);
	applyDiscoveryPayload({
		helperAvailable: discoveryState.helperAvailable,
		activePort: requestedPort,
		source: "manual-port",
		sourceLabel: "manual port",
		summary: `Trying manual port ${requestedPort}. If it is stale, Auto-discover will fall back from 11777 first.`,
		attempts: discoveryState.attempts,
	});
	await refreshAll();
	resetTimer();
});

refreshNow.addEventListener("click", () => refreshAll());
autoDiscover.addEventListener("click", async () => {
	await discoverPort();
	await refreshAll({ allowRediscovery: false });
});

clickLogin.addEventListener("click", async () => {
	if (!(await ensurePort())) {
		return;
	}
	const result = await request("/login/click", { method: "POST" });
	captureStatus.textContent = `${result.action || "login-click"} queued. Refreshing status...`;
	await delay(900);
	await refreshAll();
});

mobFarmerStep.addEventListener("click", () => runMobFarmerAction("step"));
mobFarmerStart.addEventListener("click", () => runMobFarmerAction("start"));
mobFarmerStop.addEventListener("click", () => runMobFarmerAction("stop"));
mobFarmerFocusClick.addEventListener("click", runMobFarmerFocusClick);
panicStop.addEventListener("click", async () => {
	if (!(await ensurePort())) {
		return;
	}
	try {
		await request("/automation/panic-stop", { method: "POST" });
		addActivityEvent("Panic stop triggered", "alert-circle");
		await delay(500);
		await refreshAll();
	} catch (error) {
		addActivityEvent(`Panic stop failed: ${formatError(error)}`, "alert-circle");
		console.error("Panic stop failed:", error);
	}
});
mobFarmerLoadConfig.addEventListener("click", loadMobFarmerConfig);
mobFarmerSaveConfig.addEventListener("click", saveMobFarmerConfig);
mobFarmerResetDraft.addEventListener("click", resetMobFarmerDraft);
mobFarmerExportConfig.addEventListener("click", exportMobFarmerConfig);
mobFarmerImportConfig.addEventListener("click", importMobFarmerConfig);
mobFarmerProfileSave.addEventListener("click", saveMobFarmerProfile);
mobFarmerProfileLoad.addEventListener("click", loadMobFarmerProfile);
mobFarmerProfileDuplicate.addEventListener("click", duplicateMobFarmerProfile);
mobFarmerProfileDelete.addEventListener("click", deleteMobFarmerProfile);
mobFarmerProfileSelect.addEventListener("change", () => {
	const profiles = readMobFarmerProfiles();
	const profile = profiles[mobFarmerProfileSelect.value];
	mobFarmerProfileName.value = profile?.name || mobFarmerProfileSelect.value || "";
});
for (const [skill, refs] of Object.entries(skillFarmers)) {
	refs.step.addEventListener("click", () => runSkillFarmerAction(skill, "step"));
	refs.start.addEventListener("click", () => runSkillFarmerAction(skill, "start"));
	refs.stop.addEventListener("click", () => runSkillFarmerAction(skill, "stop"));
	refs.loadConfig.addEventListener("click", () => loadSkillFarmerConfig(skill));
	refs.saveConfig.addEventListener("click", () => saveSkillFarmerConfig(skill));
	refs.resetDraft.addEventListener("click", () => resetSkillFarmerDraft(skill));
	refs.exportConfig.addEventListener("click", () => exportSkillFarmerConfig(skill));
	refs.importConfig.addEventListener("click", () => importSkillFarmerConfig(skill));
	refs.target.addEventListener("input", () => markSkillFarmerRunControlDirty(skill, "target"));
	refs.mode.addEventListener("change", () => markSkillFarmerRunControlDirty(skill, "mode"));
	refs.preset.addEventListener("change", () => {
		const presetIndex = Number(refs.preset.value);
		const preset = Number.isFinite(presetIndex) ? (refs._presets || [])[presetIndex] : null;
		applySkillFarmerPreset(skill, preset);
	});
}
mobFarmerConfigForm.addEventListener("input", markMobFarmerDraftDirty);
mobFarmerConfigForm.addEventListener("change", markMobFarmerDraftDirty);
actionsSlotList.addEventListener("input", markMobFarmerDraftDirty);
actionsSlotList.addEventListener("change", markMobFarmerDraftDirty);
actionsLoadConfig?.addEventListener("click", loadMobFarmerConfig);
actionsSaveConfig?.addEventListener("click", saveMobFarmerConfig);
actionsResetDraft?.addEventListener("click", resetMobFarmerDraft);
for (const [skill, refs] of Object.entries(skillFarmers)) {
	refs.configForm.addEventListener("input", () => markSkillFarmerDraftDirty(skill));
	refs.configForm.addEventListener("change", () => markSkillFarmerDraftDirty(skill));
}
mobFarmerRawToggle.addEventListener("change", renderMobFarmerRaw);
mobFarmerClearLog.addEventListener("click", () => {
	mobFarmerEvents = [];
	renderMobFarmerLog();
});

document.addEventListener("input", event => {
	const filter = event.target.closest("[data-table-filter]");
	if (filter) {
		filterTable(filter);
	}
});

document.addEventListener("click", event => {
	const header = event.target.closest("th[data-sort]");
	if (header) {
		sortTable(header);
	}
});

discoveryAttempts.addEventListener("click", async event => {
	const button = event.target.closest("[data-use-port]");
	if (!button) {
		return;
	}
	const selectedPort = sanitizePort(button.getAttribute("data-use-port"));
	if (!selectedPort) {
		return;
	}
	port = selectedPort;
	portInput.value = selectedPort;
	rememberPort(selectedPort);
	applyDiscoveryPayload({
		helperAvailable: true,
		activePort: selectedPort,
		source: "manual-discovery-row",
		sourceLabel: "manual discovery row",
		summary: `Using discovered CV Helper port ${selectedPort}.`,
		attempts: discoveryState.attempts,
	});
	await refreshAll({ allowRediscovery: false });
});

document.querySelectorAll("[data-capture]").forEach(button => {
	button.addEventListener("click", async () => {
		if (!(await ensurePort())) {
			return;
		}
		const path = button.getAttribute("data-capture");
		const result = await request(path, { method: "POST" });
		captureStatus.textContent = `${result.capture || "client-frame"} capture queued. Refreshing saved path...`;
		await delay(900);
		await refreshAll();
	});
});

autoRefresh.addEventListener("change", resetTimer);

function skillFarmerRefs(skill) {
	const prefix = `#${skill}-farmer`;
	return {
		state: document.querySelector(`${prefix}-state`),
		decision: document.querySelector(`${prefix}-decision`),
		target: document.querySelector(`${prefix}-target`),
		preset: document.querySelector(`${prefix}-preset`),
		mode: document.querySelector(`${prefix}-mode`),
		step: document.querySelector(`${prefix}-step`),
		start: document.querySelector(`${prefix}-start`),
		stop: document.querySelector(`${prefix}-stop`),
		statusGrid: document.querySelector(`${prefix}-status-grid`),
		configStatus: document.querySelector(`${prefix}-config-status`),
		loadConfig: document.querySelector(`${prefix}-load-config`),
		saveConfig: document.querySelector(`${prefix}-save-config`),
		resetDraft: document.querySelector(`${prefix}-reset-draft`),
		exportConfig: document.querySelector(`${prefix}-export-config`),
		importConfig: document.querySelector(`${prefix}-import-config`),
		configErrors: document.querySelector(`${prefix}-config-errors`),
		configForm: document.querySelector(`${prefix}-config-form`),
		configJson: document.querySelector(`${prefix}-config-json`),
		details: document.querySelector(`${prefix}-details`),
	};
}

function showView(viewId) {
	const resolvedView = viewId || "dashboard-view";
	for (const view of views) {
		view.classList.toggle("active", view.id === resolvedView);
	}

	for (const button of navButtons) {
		const sameView = button.getAttribute("data-view") === resolvedView;
		const farmerTarget = button.getAttribute("data-farmer-target");
		const farmerActive = resolvedView === "farmers-view"
			&& farmerTarget
			&& document.querySelector(`[data-farmer-panel="${farmerTarget}"]`)?.classList.contains("active");

		button.classList.toggle("active", sameView && (!farmerTarget || farmerActive));
	}

	const activeButton = navButtons.find(button => button.classList.contains("active"));
	const label = activeButton?.innerText?.trim() || "Dashboard";
	if (topBarTitle) {
		topBarTitle.textContent = label;
	}

	refreshIcons();
}

function showFarmerTab(tab) {
	const resolvedTab = tab || "mob";

	for (const button of farmerTabButtons) {
		button.classList.toggle("active", button.getAttribute("data-farmer-tab") === resolvedTab);
	}

	for (const panel of farmerPanels) {
		panel.classList.toggle("active", panel.getAttribute("data-farmer-panel") === resolvedTab);
	}

	for (const button of navButtons) {
		const farmerTarget = button.getAttribute("data-farmer-target");
		if (farmerTarget) {
			button.classList.toggle(
				"active",
				views.find(view => view.id === "farmers-view")?.classList.contains("active") && farmerTarget === resolvedTab
			);
		}
	}

	if (topBarTitle && document.querySelector("#farmers-view")?.classList.contains("active")) {
		topBarTitle.textContent =
			resolvedTab === "mob" ? "Mob Farmer" :
			resolvedTab === "mining" ? "Mining Farmer" :
			resolvedTab === "woodcutting" ? "Woodcutting Farmer" :
			"Farmers";
	}

	refreshIcons();
}

function emptyDiscoveryState() {
	return {
		helperAvailable: false,
		activePort: "",
		source: "startup",
		sourceLabel: "startup",
		summary: "The verifier starts from 11777 and will re-scan if that port goes stale.",
		error: "",
		attempts: [],
	};
}

function baseUrl(activePort = port) {
	return `http://127.0.0.1:${activePort}`;
}

async function ensurePort() {
	if (sanitizePort(port)) {
		return true;
	}
	const discoveredPort = await discoverPort();
	return Boolean(discoveredPort);
}

async function request(path, options = {}, activePort = port) {
	const resolvedPort = sanitizePort(activePort);
	if (!resolvedPort) {
		throw new Error("No active CV Helper port selected.");
	}
	const response = await fetch(`${baseUrl(resolvedPort)}${path}`, options);
	if (!response.ok) {
		let details = "";
		try {
			const payload = await response.json();
			if (Array.isArray(payload.errors)) {
				details = `: ${payload.errors.join("; ")}`;
			} else if (payload.error) {
				details = `: ${payload.error}`;
			}
		} catch (error) {
			details = "";
		}
		throw new Error(`${path} returned ${response.status}${details}`);
	}
	return response.json();
}

async function requestOptional(path, options = {}, activePort = port) {
	try {
		return await request(path, options, activePort);
	} catch (error) {
		return { unavailable: true, error: formatError(error), path };
	}
}

async function requestSurfaceOptional(surface, activePort = port) {
	const payload = await requestOptional(`/targets/${surface}`, {}, activePort);
	if (!payload.unavailable) {
		return payload;
	}
	return {
		...payload,
		surface,
		count: 0,
		fresh: false,
		targets: [],
	};
}

async function requestEntitiesOptional(path, emptyValue, activePort = port) {
	const payload = await requestOptional(path, {}, activePort);
	if (!payload.unavailable) {
		return payload;
	}
	return {
		...payload,
		count: 0,
		...emptyValue,
	};
}

async function refreshAll(options = {}) {
	if (!sanitizePort(port)) {
		await discoverPort();
		if (!sanitizePort(port)) {
			renderDisconnected("No active CV Helper export discovered yet.", "");
			return;
		}
	}

	const attemptedPort = sanitizePort(port);
	try {
		const [status, mobFarmerStatus, configPayload, miningStatus, miningConfig, woodcuttingStatus, woodcuttingConfig, ...targetPayloads] = await Promise.all([
			request("/status", {}, attemptedPort),
			requestOptional("/automation/mob-farmer/status", {}, attemptedPort),
			requestOptional("/automation/mob-farmer/config", {}, attemptedPort),
			requestOptional("/automation/mining/status", {}, attemptedPort),
			requestOptional("/automation/mining/config", {}, attemptedPort),
			requestOptional("/automation/woodcutting/status", {}, attemptedPort),
			requestOptional("/automation/woodcutting/config", {}, attemptedPort),
			requestEntitiesOptional("/entities", { entities: [] }, attemptedPort),
			requestEntitiesOptional("/entities/nearest", { entity: null }, attemptedPort),
			...surfaces.map(surface => requestSurfaceOptional(surface, attemptedPort)),
		]);
		const entitiesPayload = targetPayloads.shift();
		const nearestEntityPayload = targetPayloads.shift();
		const resolvedPort = sanitizePort(status.port) || attemptedPort;
		if (resolvedPort !== attemptedPort) {
			port = resolvedPort;
			portInput.value = resolvedPort;
			rememberPort(resolvedPort);
			applyDiscoveryPayload({
				helperAvailable: discoveryState.helperAvailable,
				activePort: resolvedPort,
				source: "status-report",
				sourceLabel: "status report",
				summary: `CV Helper reported active port ${resolvedPort}.`,
				attempts: discoveryState.attempts,
			});
		}

		setConnection(`Connected to ${baseUrl(resolvedPort)}`, "ok");
		renderRuntime(status);
		renderStatus(status);
		renderMobFarmer(mobFarmerStatus);
		receiveMobFarmerLiveConfig(configPayload);
		renderSkillFarmer("mining", miningStatus, miningConfig);
		renderSkillFarmer("woodcutting", woodcuttingStatus, woodcuttingConfig);
		renderInventoryView(status, mobFarmerStatus, miningStatus, woodcuttingStatus);
		renderCounts(status, targetPayloads, entitiesPayload);
		renderEntities(entitiesPayload, nearestEntityPayload);
		renderSurfaces(targetPayloads);
		renderWarnings(status, targetPayloads, entitiesPayload);
		latestRawPayloads = {
			status,
			mobFarmer: mobFarmerStatus,
			mining: miningStatus,
			woodcutting: woodcuttingStatus,
			entities: entitiesPayload,
			nearestEntity: nearestEntityPayload,
			targets: Object.fromEntries(targetPayloads.map(payload => [payload.surface || payload.path || "unknown", payload])),
		};
		renderRawData();
		lastRefreshAt = new Date();
		if (!firstStatusReceived) {
			firstStatusReceived = true;
			connectedAt = new Date();
			addActivityEvent("CV Helper connected", "refresh-cw");
		}
		if (status?.player?.loggedIn && lastLoggedInState !== true) {
			addActivityEvent(`Logged in as ${escapeHtml(status.player?.name || "player")}`, "globe");
		}
		if (lastLoggedInState === true && !status?.player?.loggedIn) {
			addActivityEvent("Logged out", "log-out");
		}
		lastLoggedInState = Boolean(status?.player?.loggedIn);
		refreshIcons();
	} catch (error) {
		if (options.allowRediscovery !== false) {
			const recoveredPort = await discoverPort();
			if (recoveredPort && recoveredPort !== attemptedPort) {
				await refreshAll({ allowRediscovery: false });
				return;
			}
		}
		renderDisconnected(formatError(error), attemptedPort);
		refreshIcons();
	}
}

async function discoverPort() {
	const helperPayload = await discoverViaHelper();
	if (helperPayload) {
		applyDiscoveryPayload(helperPayload);
		if (sanitizePort(helperPayload.activePort)) {
			return sanitizePort(helperPayload.activePort);
		}
	}

	const fallbackPayload = await discoverInBrowser();
	applyDiscoveryPayload(fallbackPayload);
	return fallbackPayload.activePort || "";
}

async function discoverViaHelper() {
	const knownPorts = candidateSeeds().map(seed => seed.port).join(",");
	const url = new URL(DISCOVERY_HELPER_URL);
	if (knownPorts) {
		url.searchParams.set("known", knownPorts);
	}
	try {
		const response = await fetch(url.toString(), { mode: 'cors' });
		if (!response.ok) {
			throw new Error(`/api/discover returned ${response.status}`);
		}
		return response.json();
	} catch (error) {
		return {
			helperAvailable: false,
			activePort: "",
			source: "helper-offline",
			sourceLabel: "helper offline",
			summary: "The local verifier helper is offline. Browser fallback can probe 11777 and remembered ports, but it cannot scan live Java listeners.",
			error: formatError(error),
			attempts: [],
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
				helperAvailable: false,
				activePort: attempt.activePort,
				source: attempt.source,
				sourceLabel: formatDiscoverySource(attempt.source),
				summary: `Browser fallback recovered CV Helper on ${attempt.activePort}. Start the local verifier helper for deeper fallback scanning if 11777 is busy.`,
				error: "",
				attempts,
			};
		}
	}

	return {
		helperAvailable: false,
		activePort: "",
		source: "browser-fallback",
		sourceLabel: "browser fallback",
		summary: "No live CV Helper export was found through browser fallback. The verifier still prefers 11777 first, then remembered ports.",
		error: attempts.length ? attempts[attempts.length - 1].error || "" : "no-probe-attempts",
		attempts,
	};
}

function candidateSeeds() {
	const seeds = [
		{ port: DEFAULT_PORT, source: "preferred-port" },
		{ port: sanitizePort(portInput.value), source: "manual-field" },
		{ port: sanitizePort(port), source: "current-port" },
		{ port: queryPort, source: "query-port" },
		...readRememberedPorts().map(candidatePort => ({ port: candidatePort, source: "remembered-port" })),
	];
	const seen = new Set();
	return seeds.filter(seed => {
		if (!seed.port || seen.has(seed.port)) {
			return false;
		}
		seen.add(seed.port);
		return true;
	});
}

async function probePort(candidatePort, source) {
	const startedAt = Date.now();
	try {
		const status = await request("/status", {}, candidatePort);
		if (status.plugin !== "CV Helper") {
			throw new Error(`Unexpected plugin ${status.plugin || "unknown"}`);
		}
		return {
			port: candidatePort,
			source,
			ok: true,
			activePort: sanitizePort(status.port) || candidatePort,
			plugin: status.plugin,
			status: status.status || "ok",
			gameState: status.player?.gameState || "Unknown",
			loggedIn: Boolean(status.player?.loggedIn),
			durationMs: Date.now() - startedAt,
		};
	} catch (error) {
		return {
			port: candidatePort,
			source,
			ok: false,
			error: formatError(error),
			durationMs: Date.now() - startedAt,
		};
	}
}

function applyDiscoveryPayload(payload) {
	const activePort = sanitizePort(payload.activePort);
	if (activePort) {
		port = activePort;
		portInput.value = activePort;
		rememberPort(activePort);
	}
	discoveryState = {
		helperAvailable: Boolean(payload.helperAvailable),
		activePort: activePort || "",
		source: payload.source || "unknown",
		sourceLabel: payload.sourceLabel || formatDiscoverySource(payload.source || "unknown"),
		summary: payload.summary || emptyDiscoveryState().summary,
		error: payload.error || "",
		attempts: Array.isArray(payload.attempts) ? payload.attempts : [],
	};
	renderDiscovery();
}

function renderDiscovery() {
	discoverySummary.textContent = discoveryState.summary;
	const cards = [
		["Active port", discoveryState.activePort || port || "Unknown"],
		["Preferred port", DEFAULT_PORT],
		["Source", discoveryState.sourceLabel || "Unknown"],
		["Helper", discoveryState.helperAvailable ? "Online" : "Offline"],
	];
	discoveryGrid.innerHTML = cards.map(([label, value]) => statCard(label, String(value))).join("");

	if (!discoveryState.attempts.length) {
		discoveryAttempts.innerHTML = `<p class="empty compact">No discovery attempts recorded yet.</p>`;
		return;
	}

	const rows = discoveryState.attempts.map(attempt => `
		<tr class="${attempt.ok ? "" : "warn"}">
			<td>${attempt.ok ? `<button type="button" class="port-chip" data-use-port="${escapeHtml(attempt.activePort || attempt.port || "")}">${escapeHtml(attempt.activePort || attempt.port || "")}</button>` : escapeHtml(attempt.port || "")}</td>
			<td>${escapeHtml(formatDiscoverySource(attempt.source || ""))}</td>
			<td>${escapeHtml(attempt.ok ? "CV Helper" : "No match")}</td>
			<td>${escapeHtml(attempt.gameState || "")}</td>
			<td>${escapeHtml(attempt.status || attempt.error || "")}</td>
		</tr>
	`).join("");
	discoveryAttempts.innerHTML = `
		<div class="table-wrap">
			<table>
				<thead>
					<tr>
						<th>Port</th>
						<th>Source</th>
						<th>Result</th>
						<th>Game state</th>
						<th>Details</th>
					</tr>
				</thead>
				<tbody>${rows}</tbody>
			</table>
		</div>
	`;
}

function renderDisconnected(message, attemptedPort) {
	const target = attemptedPort ? baseUrl(attemptedPort) : "the preferred port";

	setConnection("Disconnected", "bad");
	if (connectionUrl) {
		connectionUrl.textContent = `Failed to reach ${target}`;
	}

	setRuntime("Local export unavailable", "bad");

	captureStatus.textContent = attemptedPort === DEFAULT_PORT
		? "11777 did not respond. Auto-discover will keep preferring 11777, then fall back if the plugin had to choose another port."
		: `The last known port ${attemptedPort} is stale or unavailable. Auto-discover can recover the live port without scraping logs.`;

	warningsList.innerHTML = `<li>${escapeHtml(message)}</li>`;
	clearLiveSections();
	lastLoggedInState = false;
	connectedAt = null;
}

function renderRuntime(status) {
	const player = status.player || {};
	const gameState = String(player.gameState || "Unknown");
	const loggedIn = Boolean(player.loggedIn);

	if (LOGIN_SCREEN_STATES.has(gameState) || !loggedIn) {
		setRuntime(`Healthy export at ${gameState}`, "warn");
		if (connectionUrl) {
			connectionUrl.textContent = `Ready: ${gameState}`;
		}
		captureStatus.textContent = "CV Helper is responding, but RuneLite is still at the login screen. Empty widget-dependent targets are expected until the client finishes logging in.";
		return;
	}

	setRuntime(`Ready: ${gameState}`, "ok");
	if (connectionUrl) {
		connectionUrl.textContent = `Ready: ${gameState}`;
	}
	captureStatus.textContent = `Using ${baseUrl(status.port || port)} from ${discoveryState.sourceLabel || "the current discovery path"}. If the export moves off 11777, the verifier will re-scan automatically.`;
}

function clearLiveSections() {
	if (connectionGrid) connectionGrid.innerHTML = "";
	if (vitalsGrid) vitalsGrid.innerHTML = "";
	if (wealthGrid) wealthGrid.innerHTML = "";
	if (interfaceGrid) interfaceGrid.innerHTML = "";
	if (countGrid) countGrid.innerHTML = "";
	if (captureGrid) captureGrid.innerHTML = "";
	if (entitiesRoot) entitiesRoot.innerHTML = "";
	if (surfacesRoot) surfacesRoot.innerHTML = "";
	if (inventorySummary) inventorySummary.innerHTML = "";
	if (inventoryItemsRoot) inventoryItemsRoot.innerHTML = "";
	if (equipmentItemsRoot) equipmentItemsRoot.innerHTML = "";
	if (dropPolicyRoot) dropPolicyRoot.innerHTML = "";

	captureStatus.textContent = "No live CV Helper connection.";

	if (bigStatus) {
		bigStatus.className = "big-status bad";
		bigStatus.innerHTML = `
			<div class="big-status-icon">${icon("shield-alert")}</div>
			<div class="big-status-text">
				<h3>Disconnected</h3>
				<p>No live CV Helper connection. Check the port and click Connect or Auto-discover.</p>
			</div>
		`;
	}

	if (bigStatusRow) {
		bigStatusRow.className = "bad";
		bigStatusRow.innerHTML = `${icon("shield-alert")}Disconnected`;
	}

	if (topBarWorld) topBarWorld.innerHTML = `${icon("globe")}World —`;
	if (topBarGameState) topBarGameState.textContent = "Unknown";
	if (topBarPlayer) topBarPlayer.innerHTML = `${icon("user")}Logged in as —`;
	if (topBarUptime) topBarUptime.innerHTML = `${icon("clock")}Uptime —`;
	if (topBarLastUpdate) topBarLastUpdate.innerHTML = `${icon("rotate-ccw")}Last update —`;
	if (footerMemory) footerMemory.textContent = "—";
	if (footerCpu) footerCpu.textContent = "—";

	mobFarmerState.textContent = "Unavailable";
	mobFarmerState.classList.remove("ok", "warn", "neutral");
	mobFarmerState.classList.add("bad");
	mobFarmerDecision.textContent = "No live mob-farmer payload available.";
	mobFarmerStatusGrid.innerHTML = "";
	mobFarmerDetails.innerHTML = "";
	mobFarmerRawPayload = null;
	renderMobFarmerRaw();

	for (const refs of Object.values(skillFarmers)) {
		refs.state.textContent = "Unavailable";
		refs.state.classList.remove("ok", "warn", "neutral");
		refs.state.classList.add("bad");
		refs.decision.textContent = "No live skill-farmer payload available.";
		refs.statusGrid.innerHTML = "";
		refs.details.innerHTML = "";
	}

	refreshIcons();
}

function boostedPercent(skill) {
	if (!skill || typeof skill.boosted !== "number" || typeof skill.real !== "number" || skill.real === 0) {
		return 0;
	}
	return Math.max(0, Math.min(100, (skill.boosted / skill.real) * 100));
}

function renderProgressStat(iconName, label, value, percent, progressClass) {
	const clamped = Math.max(0, Math.min(100, percent || 0));
	return `
		<div class="stat-card">
			<div class="label">${iconName ? icon(iconName) : ""}${escapeHtml(label)}</div>
			<div class="value">${escapeHtml(value)}</div>
			<div class="progress-bar"><div class="${escapeHtml(progressClass)}" style="width: ${clamped}%"></div></div>
		</div>
	`;
}

function setPill(element, message, tone) {
	element.textContent = message;
	element.classList.remove("ok", "bad", "warn", "neutral");
	element.classList.add(tone || "neutral");
}

function setConnection(message, tone) {
	connectionStatus.textContent = message;
	connectionStatus.className = `status-dot ${tone || "neutral"}`;

	if (connectionUrl && tone === "neutral") {
		connectionUrl.textContent = `Ready: ${port || DEFAULT_PORT}`;
	}
}

function setRuntime(message, tone) {
	setPill(runtimeStatus, message, tone);
}

function renderStatus(status) {
	const player = status.player || {};
	const gameState = String(player.gameState || "Unknown");
	const spellbook = status.spellbook || player.spellbook || {};
	const interfaces = player.interfaces || status.interfaces || {};
	const vitals = player.vitals || {};
	const hitpoints = vitals.hitpoints || (player.skills || {}).hitpoints || {};
	const prayer = vitals.prayer || (player.skills || {}).prayer || {};
	const prayers = player.prayers || {};
	const wealth = player.wealth || {};
	const inventory = wealth.inventory || {};
	const equipment = wealth.equipment || {};
	const selectedWidget = player.selectedWidget || {};
	const captures = status.captures || player.captures || {};
	const captureEntries = Object.values(captures);
	const latestCapture = captureEntries[captureEntries.length - 1] || {};
	const loggedIn = Boolean(player.loggedIn);
	const ready = loggedIn && !LOGIN_SCREEN_STATES.has(gameState);

	const activePrayerNames = Array.isArray(vitals.activePrayers)
		? vitals.activePrayers
		: Object.entries(prayers).filter(([, state]) => state?.active).map(([name]) => name);

	const bigStatusClass = ready ? "" : (LOGIN_SCREEN_STATES.has(gameState) ? "warn" : "bad");
	const bigStatusIcon = ready ? "shield-check" : (LOGIN_SCREEN_STATES.has(gameState) ? "clock" : "shield-alert");
	const bigStatusText = ready ? "Client Ready" : (LOGIN_SCREEN_STATES.has(gameState) ? "At Login Screen" : "Client Not Ready");
	const bigStatusSub = ready
		? `Logged in as ${player.localPlayer || "Unknown"}`
		: LOGIN_SCREEN_STATES.has(gameState)
			? "CV Helper is healthy. Empty widget targets are expected until login completes."
			: "No live player data available.";

	if (bigStatusRow) {
		bigStatusRow.className = bigStatusClass;
		bigStatusRow.innerHTML = `${icon(bigStatusIcon)}${bigStatusText}`;
	}

	if (bigStatus) {
		bigStatus.className = `big-status ${bigStatusClass}`;
		bigStatus.innerHTML = `
			<div class="big-status-icon">${icon(bigStatusIcon)}</div>
			<div class="big-status-text">
				<h3>${escapeHtml(bigStatusText)}</h3>
				<p>${escapeHtml(bigStatusSub)}</p>
			</div>
		`;
	}

	const worldLabel = player.world ? (player.worldType || "Members") : "—";
	if (topBarWorld) topBarWorld.innerHTML = `${icon("globe")}World ${escapeHtml(player.world || "—")} <small class="world-type">${escapeHtml(worldLabel)}</small>`;
	if (topBarGameState) topBarGameState.textContent = gameState;
	if (topBarPlayer) topBarPlayer.innerHTML = `${icon("user")}Logged in as ${escapeHtml(player.localPlayer || "—")}`;

	const uptimeMs = connectedAt ? Date.now() - connectedAt.getTime() : 0;
	if (topBarUptime) topBarUptime.innerHTML = `${icon("clock")}Uptime ${formatDuration(uptimeMs)}`;
	if (topBarLastUpdate) topBarLastUpdate.innerHTML = `${icon("rotate-ccw")}Last update ${lastRefreshAt ? formatRelativeTime(lastRefreshAt) : "—"}`;

	const hpPercent = boostedPercent(hitpoints);
	const prayerPercent = boostedPercent(prayer);
	const runPercent = typeof vitals.runEnergyPercent === "number"
		? vitals.runEnergyPercent
		: (typeof player.runEnergy === "number" ? player.runEnergy / 100 : 100);
	const specPercent = typeof vitals.specialAttackPercent === "number" ? vitals.specialAttackPercent : 100;

	if (connectionGrid) {
		connectionGrid.innerHTML = [
			["server", "API Port", status.port || "Unknown"],
			["git-commit", "Preferred Port", status.preferredPort || DEFAULT_PORT],
			["activity", "Status", status.status || "Unknown"],
			["search", "Discovery", discoveryState.sourceLabel || "Unknown"],
			["gamepad-2", "Game State", gameState],
			["log-in", "Logged In", String(loggedIn)],
			["user", "Player", player.localPlayer || "Unavailable"],
			["globe", "World", player.world || "Unknown"],
		].map(([iconName, label, value]) => statCard(label, value, iconName)).join("");
	}

	if (vitalsGrid) {
		vitalsGrid.innerHTML = [
			renderProgressStat("heart", "Hitpoints", formatBoosted(hitpoints), hpPercent, "progress-hp"),
			renderProgressStat("sparkles", "Prayer", formatBoosted(prayer), prayerPercent, "progress-prayer"),
			statCard("Player Active", String(Boolean(vitals.playerActive ?? player.playerActive)), "activity"),
			statCard("Active Prayers", activePrayerNames.length ? activePrayerNames.join(", ") : "None", "sparkles"),
			renderProgressStat("zap", "Run Energy", formatPercent(vitals.runEnergyPercent, player.runEnergy), runPercent, "progress-run"),
			renderProgressStat("battery-charging", "Spec Energy", formatPercent(vitals.specialAttackPercent), specPercent, "progress-spec"),
			statCard("Spec Enabled", selectedValue(vitals.specialAttackEnabled, "Unknown"), "shield"),
			statCard("Weight", vitals.weight ?? player.weight ?? "Unknown", "scale"),
		].join("");
	}

	if (wealthGrid) {
		wealthGrid.innerHTML = [
			["coins", "Current Loot GE", formatGp(wealth.currentLootValueGe ?? inventory.gePrice)],
			["coins", "Current Loot HA", formatGp(wealth.currentLootValueHa ?? inventory.haPrice)],
			["gem", "Equipment GE", formatGp(equipment.gePrice)],
			["wallet", "Total Carried GE", formatGp(wealth.totalCarriedValueGe)],
			["alert-circle", "Risked GE approx", formatGp(wealth.riskedValueGeApprox)],
			["shield-check", "Risk Model", wealth.riskModel || "Unknown"],
		].map(([iconName, label, value]) => statCard(label, value, iconName)).join("");
	}

	if (interfaceGrid) {
		interfaceGrid.innerHTML = [
			["layout-grid", "Open Panel", interfaces.activeSidePanel || "Unknown"],
			["book-open", "Spellbook", spellbook.name || "Unknown"],
			["mouse-pointer", "Mouse", formatPoint(player.mouseCanvasPosition)],
			["map-pin", "World Location", formatPoint(player.worldLocation)],
			["navigation", "Local Location", formatPoint(player.localLocation)],
			["square", "Self Bounds", formatRect(player.selfBounds || {})],
			["mouse-pointer-click", "Selected Widget", selectedWidget.selected ? `${selectedWidget.widgetId || "?"} ${selectedWidget.name || selectedWidget.text || ""}` : "None"],
			["camera", "Latest Capture", latestCapture.savedPath || latestCapture.status || "None"],
		].map(([iconName, label, value]) => statCard(label, value, iconName)).join("");
	}

	renderCaptureStatus(captures);
	renderRecentActivity();
	updateFooter(status);
	refreshIcons();
}

function renderInventoryView(status, mobFarmerStatus, miningStatus, woodcuttingStatus) {
	const wealth = status?.player?.wealth || status?.wealth || {};
	const inventory = wealth.inventory || mobFarmerStatus?.inventory || miningStatus?.inventory || woodcuttingStatus?.inventory || {};
	const equipment = wealth.equipment || {};
	const policy = inventory.policy || mobFarmerStatus?.loot || miningStatus?.loot || woodcuttingStatus?.loot || {};
	const summaries = [
		["Inventory Slots", `${selectedValue(inventory.occupiedSlots, 0)} / ${selectedValue(inventory.slotCount, 28)}`],
		["Free Slots", selectedValue(inventory.freeSlots, inventory.slotCount !== undefined && inventory.occupiedSlots !== undefined ? inventory.slotCount - inventory.occupiedSlots : "Unknown")],
		["Inventory GE", formatGp(inventory.gePrice)],
		["Inventory HA", formatGp(inventory.haPrice)],
		["Equipment GE", formatGp(equipment.gePrice)],
		["Protected Items", inventory.neverDropItems || policy?.neverDropItems || "None"],
		["Drop Candidate", inventory.dropCandidate?.name || "None"],
		["Policy State", policy?.enabled ? "Enabled" : "Disabled"],
		["Drop Opportunity", policy?.dropOpportunity?.name || "None"],
	];

	if (inventorySummary) {
		inventorySummary.innerHTML = summaries.map(([label, value]) => statCard(label, String(value))).join("");
	}
	if (inventoryItemsRoot) {
		inventoryItemsRoot.innerHTML = renderInventoryTable("", inventory.items || [], "inventory-items");
	}
	if (equipmentItemsRoot) {
		equipmentItemsRoot.innerHTML = renderInventoryTable("", equipment.items || [], "equipment-items");
	}
	if (dropPolicyRoot) {
		dropPolicyRoot.innerHTML = renderDropPolicy(policy, inventory);
	}
}

function renderDropPolicy(policy, inventory) {
	if (!policy || Object.keys(policy).length === 0) {
		return `<p class="empty compact">No drop policy reported.</p>`;
	}
	const rows = [
		["Policy Enabled", policy.enabled ? "ON" : "OFF", policy.enabled ? "ok" : "bad"],
		["Drop Mode", policy.dropMode || "—", ""],
		["Trigger Mode", policy.triggerMode || "—", ""],
		["Free Slots Threshold", selectedValue(policy.freeSlotsThreshold, "—"), ""],
		["Safe to Drop Now", policy.safeToDropNow ? "YES" : "NO", policy.safeToDropNow ? "ok" : "warn"],
		["Valid Drop Opportunity", policy.validDropOpportunity ? "YES" : "NO", policy.validDropOpportunity ? "ok" : "warn"],
		["Protected Items", inventory.neverDropItems || policy.neverDropItems || "None", ""],
		["Max Drop Value", formatGp(policy.maxDropValueGe), ""],
		["Drop Allowlist", policy.dropAllowlist || "None", ""],
		["Droppable Candidates", policy.droppableCandidates?.length ?? 0, ""],
	].map(([label, value, tone]) => `
		<div class="policy-row">
			<span class="label">${escapeHtml(label)}</span>
			<span class="value ${escapeHtml(tone)}">${escapeHtml(value)}</span>
		</div>
	`).join("");

	return `
		<div class="policy-panel">
			<h4>Drop Policy</h4>
			${rows}
		</div>
		<div class="policy-panel">
			<h4>Protected Items</h4>
			<div class="policy-row">
				<span class="value">${escapeHtml(inventory.neverDropItems || policy.neverDropItems || "None")}</span>
			</div>
		</div>
		<div class="policy-panel">
			<h4>Droppable Candidates</h4>
			${(policy.droppableCandidates || []).length ? `<ul class="compact-list">${policy.droppableCandidates.map(c => `<li>${escapeHtml(c.name || c.id || "?")} × ${escapeHtml(c.quantity ?? "?")}</li>`).join("")}</ul>` : `<p class="empty compact">No droppable candidates.</p>`}
		</div>
	`;
}

function renderInventoryTable(title, items, tableId) {
	const rows = (items || []).map(item => `
		<tr>
			<td>${escapeHtml(item.name || "")}<br><small>${escapeHtml(item.id ?? "")}</small></td>
			<td>${escapeHtml(item.quantity ?? "")}</td>
			<td>${escapeHtml(item.slot ?? "")}</td>
			<td>${formatGp(item.gePriceEach)}</td>
			<td>${formatGp(item.gePrice)}</td>
			<td>${formatGp(item.haPriceEach)}</td>
			<td>${formatGp(item.haPrice)}</td>
		</tr>
	`).join("");
	const header = title ? `
			<header>
				<h4>${escapeHtml(title)}</h4>
				<span class="summary">${escapeHtml(items.length)} rows</span>
			</header>
	` : "";
	const filter = title ? `
			<input class="table-filter" data-table-filter="${escapeHtml(tableId)}" placeholder="Filter ${escapeHtml(title.toLowerCase())}">
	` : "";
	return `
		<div class="detail-block">
			${header}
			${items.length ? `
				${filter}
				<div class="table-wrap compact">
					<table data-table-id="${escapeHtml(tableId)}">
						<thead>
							<tr>
								<th data-sort>Item</th>
								<th data-sort>Qty</th>
								<th data-sort>Slot</th>
								<th data-sort>GE each</th>
								<th data-sort>GE stack</th>
								<th data-sort>HA each</th>
								<th data-sort>HA stack</th>
							</tr>
						</thead>
						<tbody>${rows}</tbody>
					</table>
				</div>
			` : `<p class="empty compact">No items reported.</p>`}
		</div>
	`;
}

function renderMobFarmer(status) {
	if (status?.unavailable) {
		mobFarmerState.textContent = "Unavailable";
		mobFarmerState.classList.remove("ok", "bad", "neutral");
		mobFarmerState.classList.add("warn");
		mobFarmerDecision.textContent = status.error || "Mob-farmer endpoint unavailable on this client build.";
		mobFarmerStatusGrid.innerHTML = "";
		mobFarmerDetails.innerHTML = `<p class="empty compact">${escapeHtml(status.path || "/automation/mob-farmer/status")} is unavailable on the connected CV Helper export. Relaunch the newest client build if you need these controls.</p>`;
		mobFarmerRawPayload = null;
		renderMobFarmerRaw();
		return;
	}

	status = status || {};
	mobFarmerRawPayload = status;

	const running = Boolean(status.running);
	const live = Boolean(status.live);
	const latestReason = mobFarmerLatestReason(status);
	const targetCandidates = Array.isArray(status.targetCandidates) ? status.targetCandidates : [];
	const lootCandidates = Array.isArray(status.lootCandidates) ? status.lootCandidates : [];
	const recentMenuEntries = Array.isArray(status.recentMenuEntries) ? status.recentMenuEntries : [];
	const inventory = status.inventory || {};
	const highAlchCandidates = Array.isArray(status.loot?.highAlch?.candidates) ? status.loot.highAlch.candidates : [];

	mobFarmerState.textContent = `${running ? "Running" : "Stopped"} - ${live ? "Live" : "Dry"}`;
	mobFarmerState.classList.remove("ok", "bad", "warn", "neutral");
	mobFarmerState.classList.add(running ? "ok" : "bad");
	mobFarmerDecision.textContent = latestReason || status.status || "No decision yet.";

	if (mobFarmerLiveDate) {
		mobFarmerLiveDate.textContent = `Live Data: ${status.timestamp ? formatRelativeTime(new Date(status.timestamp)) : "—"}`;
	}
	if (mobFarmerUptime) {
		mobFarmerUptime.textContent = `Uptime: ${status.uptimeMs ? formatDuration(status.uptimeMs) : "—"}`;
	}

	mobFarmerStatusGrid.innerHTML = `
		<div class="reference-metric-strip">
			${metricCard("skull", "Target", status.target || "Unknown")}
			${metricCard("activity", "Running", String(running), running ? "good" : "bad")}
			${metricCard("radio", "Live", String(live), live ? "good" : "warn")}
			${metricCard("clock", "Loop Delay", status.loopDelayMs ? `${status.loopDelayMs} ms` : "Unknown")}
			${metricCard("coins", "Inventory GE", formatGp(inventory.gePrice), "gold")}
			${metricCard("sparkles", "Inventory HA", formatGp(inventory.haPrice), "gold")}
			${metricCard("backpack", "Free Slots", selectedValue(inventory.freeSlots, "Unknown"))}
			${metricCard("zap", "Run", status.autorun ? `${status.autorun.runEnabled ? "on" : "off"} @ ${selectedValue(status.autorun.runEnergyPercent, "?")}%` : "Unknown")}
			${metricCard("wand-2", "Alch Candidates", highAlchCandidates.length)}
			${metricCard("brain", "Decision", (typeof status.decision === "object" ? (status.decision?.decision || status.decision?.status || "—") : compactValue(status.decision)) || "Unknown", compactDecisionTone(typeof status.decision === "object" ? status.decision?.decision : status.decision))}
			${metricCard("swords", "Aggro", status.aggroResponse || "Unknown")}
			${metricCard("users", "Engaged", status.engagedMode || "Unknown")}
			${metricCard("eye", "Line of Sight", selectedValue(status.requireLineOfSight, "Unknown"))}
			${metricCard("route", "Max Distance", selectedValue(status.maxDistance, "Unknown"))}
			${metricCard("crosshair", "Candidates", targetCandidates.length)}
			${metricCard("package", "Loot Candidates", lootCandidates.length)}
			${metricCard("menu", "Menu Entries", recentMenuEntries.length)}
		</div>
	`;

	mobFarmerDetails.innerHTML = `
		<div class="farmer-runtime-grid">
			<div class="card side">
				<div class="card-header">
					<h3>${icon("shield")}Survival</h3>
				</div>
				<div class="card-body">${renderRuntimeTableBlock("Survival", status.survivalDecision)}</div>
			</div>

			<div class="card side">
				<div class="card-header">
					<h3>${icon("workflow")}Intermediate</h3>
				</div>
				<div class="card-body">${renderRuntimeTableBlock("Intermediate", status.intermediateDecision)}</div>
			</div>

			<div class="card side">
				<div class="card-header">
					<h3>${icon("package")}Loot & Inventory</h3>
				</div>
				<div class="card-body">${renderRuntimeTableBlock("Loot", status.lootDecision)}</div>
			</div>

			<div class="card full">
				<div class="card-header split">
					<h3>${icon("crosshair")}Target Candidates</h3>
					<span class="summary">${targetCandidates.length} entries</span>
				</div>
				${renderMobTargetCandidateTableInner(targetCandidates)}
			</div>

			<div class="card full">
				<div class="card-header split">
					<h3>${icon("package")}Loot Candidates</h3>
					<span class="summary">${lootCandidates.length} entries with stack GE/HA diagnostics</span>
				</div>
				${renderLootCandidateTableInner(lootCandidates)}
			</div>

			<div class="card main">
				<div class="card-header">
					<h3>${icon("history")}Recent Menu Entries</h3>
				</div>
				${renderMobFarmerMenuEntriesInner(recentMenuEntries)}
			</div>

			<div class="card side">
				<div class="card-header">
					<h3>${icon("wand-2")}High Alch Candidates</h3>
				</div>
				${renderHighAlchCandidatesInner(highAlchCandidates)}
			</div>
		</div>
	`;

	renderMobFarmerLog();
	renderMobFarmerRaw();
	refreshIcons();
}

async function runMobFarmerAction(action) {
	if (!(await ensurePort())) {
		return;
	}
	const target = mobFarmerTarget.value.trim() || "cow";
	const live = mobFarmerMode.value === "true";
	const path = action === "stop"
		? "/automation/mob-farmer/stop"
		: `/automation/mob-farmer/${action}?target=${encodeURIComponent(target)}&live=${encodeURIComponent(String(live))}`;
	const result = await request(path, { method: "POST" });
	logMobFarmerEvent(`${action} queued for ${target} (${live ? "live" : "dry"})`);
	if (result?.status) {
		renderMobFarmer(result);
	}
	await delay(900);
	await refreshAll();
}

async function runMobFarmerFocusClick() {
	if (!(await ensurePort())) {
		return;
	}
	const result = await request("/automation/mob-farmer/focus-click", { method: "POST" });
	logMobFarmerEvent("focus click queued");
	if (result?.mobFarmer) {
		renderMobFarmer(result.mobFarmer);
	}
	await delay(700);
	await refreshAll();
}

async function loadMobFarmerConfig() {
	if (!(await ensurePort())) {
		return;
	}
	try {
		clearMobFarmerConfigErrors();
		const payload = await request("/automation/mob-farmer/config");
		mobFarmerConfigPayload = payload;
		mobFarmerLastLiveFingerprint = configFingerprint(payload);
		mobFarmerDraftPayload = deepClone(payload);
		mobFarmerDraftDirty = false;
		mobFarmerLiveChangedWhileDirty = false;
		renderMobFarmerConfig(mobFarmerDraftPayload, "Synced", "ok");
		logMobFarmerEvent("current config loaded into draft");
	} catch (error) {
		showMobFarmerConfigErrors([formatError(error)]);
	}
}

function resetMobFarmerDraft() {
	if (mobFarmerConfigPayload) {
		mobFarmerDraftPayload = deepClone(mobFarmerConfigPayload);
		mobFarmerDraftDirty = false;
		mobFarmerLiveChangedWhileDirty = false;
		renderMobFarmerConfig(mobFarmerDraftPayload, "Synced", "ok");
		clearMobFarmerConfigErrors();
		logMobFarmerEvent("draft reset from live config");
	} else {
		mobFarmerDraftPayload = null;
		mobFarmerDraftDirty = false;
		renderMobFarmerDraftPlaceholder("No live config loaded yet. Use Load current into draft first.");
	}
}

async function saveMobFarmerConfig() {
	if (!(await ensurePort())) {
		return;
	}
	if (!mobFarmerDraftPayload) {
		showMobFarmerConfigErrors(["Load the current config into the draft before applying changes."]);
		return;
	}
	try {
		clearMobFarmerConfigErrors();
		const payload = collectMobFarmerConfigFromForm();
		const result = await request("/automation/mob-farmer/config", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(payload),
		});
		mobFarmerConfigPayload = result.config || payload;
		mobFarmerLastLiveFingerprint = configFingerprint(mobFarmerConfigPayload);
		mobFarmerDraftPayload = deepClone(mobFarmerConfigPayload);
		mobFarmerDraftDirty = false;
		mobFarmerLiveChangedWhileDirty = false;
		renderMobFarmerConfig(mobFarmerDraftPayload, "Applied successfully", "ok");
		logMobFarmerEvent(`draft applied (${result.updatedSettings || 0} updates)`);
		await refreshAll();
	} catch (error) {
		showMobFarmerConfigErrors([formatError(error)]);
		setConfigState(mobFarmerConfigStatus, "Apply failed", "bad");
	}
}

function exportMobFarmerConfig() {
	const payload = mobFarmerDraftPayload ? collectMobFarmerConfigFromForm() : (mobFarmerConfigPayload || { version: 1, settings: {}, actionSlots: [] });
	payload.version = mobFarmerDraftPayload?.version || mobFarmerConfigPayload?.version || 1;
	mobFarmerConfigJson.value = JSON.stringify(payload, null, 2);
	setConfigState(mobFarmerConfigStatus, mobFarmerDraftDirty ? "Unsaved changes" : "Synced", mobFarmerDraftDirty ? "warn" : "ok");
}

function importMobFarmerConfig() {
	try {
		clearMobFarmerConfigErrors();
		const raw = mobFarmerConfigJson.value.trim();
		if (!raw) {
			showMobFarmerConfigErrors(["Paste exported JSON before importing."]);
			return;
		}
		mobFarmerDraftPayload = normalizeMobFarmerDraft(JSON.parse(raw));
		mobFarmerDraftDirty = true;
		renderMobFarmerConfig(mobFarmerDraftPayload, "Draft modified", "warn");
		logMobFarmerEvent("JSON imported into draft");
	} catch (error) {
		showMobFarmerConfigErrors([formatError(error)]);
	}
}

function receiveMobFarmerLiveConfig(payload) {
	if (!payload) {
		return;
	}
	if (payload.unavailable) {
		if (!mobFarmerDraftPayload) {
			setConfigState(mobFarmerConfigStatus, "Config endpoint unavailable", "bad");
			mobFarmerConfigForm.innerHTML = `<p class="empty compact">The running CV Helper build does not expose ${escapeHtml(payload.path || "the config endpoint")} yet.</p>`;
		}
		renderActionsView();
		return;
	}

	const nextFingerprint = configFingerprint(payload);
	const previousFingerprint = mobFarmerLastLiveFingerprint;
	mobFarmerConfigPayload = payload;
	mobFarmerLastLiveFingerprint = nextFingerprint;
	if (mobFarmerDraftPayload && nextFingerprint !== configFingerprint(mobFarmerDraftPayload)) {
		mobFarmerLiveChangedWhileDirty = true;
		if (mobFarmerDraftDirty) {
			showMobFarmerConfigWarning("Live config changed while this draft has unsaved changes. The draft was not overwritten.");
			setConfigState(mobFarmerConfigStatus, "Unsaved changes", "warn");
		} else if (previousFingerprint && nextFingerprint !== previousFingerprint) {
			showMobFarmerConfigWarning("Live config changed after this draft was loaded. Use Load current into draft or Reset draft to refresh it.");
			setConfigState(mobFarmerConfigStatus, "Live changed", "warn");
		}
		return;
	}
	if (!mobFarmerDraftPayload) {
		renderMobFarmerDraftPlaceholder("Live config is available. Load current into draft to edit safely.");
	} else if (!mobFarmerDraftDirty) {
		setConfigState(mobFarmerConfigStatus, "Synced", "ok");
	}
}

function renderMobFarmerDraftPlaceholder(message) {
	setConfigState(mobFarmerConfigStatus, mobFarmerConfigPayload ? "Live available" : "No draft loaded", "neutral");
	mobFarmerConfigForm.innerHTML = `<p class="empty compact">${escapeHtml(message)}</p>`;
	renderActionsView();
}

function renderMobFarmerConfig(payload, stateLabel = "Synced", tone = "ok") {
	if (!payload) {
		renderMobFarmerDraftPlaceholder("No draft loaded. Load current config or import JSON to begin editing.");
		return;
	}
	if (payload.unavailable) {
		setConfigState(mobFarmerConfigStatus, `Unavailable: ${payload.error}`, "bad");
		mobFarmerConfigForm.innerHTML = `<p class="empty compact">The running CV Helper build does not expose ${escapeHtml(payload.path || "the config endpoint")} yet.</p>`;
		renderActionsView();
		return;
	}

	const settings = payload.settings || {};
	const schema = Array.isArray(payload.schema) ? payload.schema : [];

	setConfigState(mobFarmerConfigStatus, `${stateLabel} - v${payload.version || 1}`, tone);
	mobFarmerConfigForm.innerHTML = schema.length
		? schema.map(field => renderConfigField(field, settings[field.key], "mob-config")).join("")
		: `<p class="empty compact">This draft has no editable schema. Load current into draft before importing partial settings.</p>`;
	mobFarmerTarget.value = settings.target || mobFarmerTarget.value || "cow";
	mobFarmerProfileName.value = settings.profileName || mobFarmerProfileName.value || settings.target || "Mob farmer profile";
	renderMobFarmerProfiles();
	renderActionsView();
	clearMobFarmerConfigErrors();
}

function renderActionsView() {
	if (!actionsConfigStatus || !actionsSlotList) {
		return;
	}
	if (!mobFarmerConfigPayload) {
		actionsSlotList.innerHTML = `<p class="empty compact">No live action config loaded yet.</p>`;
		setConfigState(actionsConfigStatus, "No draft loaded", "neutral");
		return;
	}
	if (mobFarmerConfigPayload.unavailable) {
		actionsSlotList.innerHTML = `<p class="empty compact">The running CV Helper build does not expose the action config endpoint yet.</p>`;
		setConfigState(actionsConfigStatus, "Unavailable", "bad");
		return;
	}
	const payload = mobFarmerDraftPayload || mobFarmerConfigPayload;
	const readOnly = !mobFarmerDraftPayload;
	const slots = payload?.actionSlots || [];
	actionsSlotList.innerHTML = slots.length
		? slots.map(slot => renderActionSlotFieldset(slot, readOnly)).join("")
		: `<p class="empty compact">No action hotkeys configured.</p>`;
	if (mobFarmerDraftDirty) {
		setConfigState(actionsConfigStatus, "Unsaved changes", "warn");
	} else if (mobFarmerLiveChangedWhileDirty) {
		setConfigState(actionsConfigStatus, "Live changed", "warn");
	} else if (readOnly) {
		setConfigState(actionsConfigStatus, "Live available", "neutral");
	} else {
		setConfigState(actionsConfigStatus, "Synced", "ok");
	}
}

function renderConfigField(field, value, prefix = "config") {
	const key = escapeHtml(field.key);
	const id = `${escapeHtml(prefix)}-${key}`;
	const label = escapeHtml(field.label || field.key);
	const description = escapeHtml(field.description || "");
	const type = field.type || "text";
	if (type === "boolean") {
		return `
			<div class="schema-field checkbox-field" title="${description}">
				<input id="${id}" data-config-key="${key}" type="checkbox" ${value ? "checked" : ""}>
				<label for="${id}">${label}</label>
				<small>${description}</small>
			</div>
		`;
	}
	if (type === "enum" || type === "select") {
		const options = (field.options || []).map(option => `<option value="${escapeHtml(option)}" ${String(value) === String(option) ? "selected" : ""}>${escapeHtml(option)}</option>`).join("");
		return `
			<div class="schema-field" title="${description}">
				<label for="${id}">${label}</label>
				<select id="${id}" data-config-key="${key}">${options}</select>
				<small>${description}</small>
			</div>
		`;
	}
	if (type === "textarea") {
		return `
			<div class="schema-field" title="${description}">
				<label for="${id}">${label}</label>
				<textarea id="${id}" data-config-key="${key}" spellcheck="false">${escapeHtml(value ?? "")}</textarea>
				<small>${description}</small>
			</div>
		`;
	}
	return `
		<div class="schema-field" title="${description}">
			<label for="${id}">${label}</label>
			<input id="${id}" data-config-key="${key}" type="${type === "number" ? "number" : "text"}" value="${escapeHtml(value ?? "")}">
			<small>${description}</small>
		</div>
	`;
}

function renderActionSlotFieldset(slot, readOnly = false) {
	const slotNumber = Number(slot.slot || 0);
	const disabled = readOnly ? "disabled" : "";
	const readonlyClass = readOnly ? "readonly" : "";
	return `
		<article class="action-slot-card ${readonlyClass}" data-action-slot="${slotNumber}">
			<header>
				<h4>Action ${slotNumber}</h4>
				<label><input data-slot-key="enabled" type="checkbox" ${slot.enabled ? "checked" : ""} ${disabled}> Enabled</label>
			</header>
			<label class="slot-row"><span>Hotkey</span><input data-slot-key="hotkey" value="${escapeHtml(slot.hotkey || "NOT_SET")}" title="Use F12, CTRL+1, ALT+Q, SHIFT+F, or NOT_SET." ${disabled}></label>
			<label class="slot-row"><span>Surface</span>${enumSelect("surface", slot.surface, ["DISABLED", "PRAYER", "SPELL", "MINIMAP", "INVENTORY", "EQUIPMENT", "PANELS", "COMBAT", "NEAREST_ENTITY"], readOnly)}</label>
			<label class="slot-row"><span>Target</span><input data-slot-key="target" value="${escapeHtml(slot.target || "")}" ${disabled}></label>
			<label class="slot-row"><span>Click after</span>${enumSelect("clickAfterMode", slot.clickAfterMode, ["AUTO", "ALWAYS", "NEVER"], readOnly)}</label>
			<label class="slot-row"><span>Invoke</span>${enumSelect("invocationMode", slot.invocationMode, ["AUTO", "WIDGET", "CLICK"], readOnly)}</label>
			<label class="slot-row"><span>Prayer</span>${enumSelect("prayerMode", slot.prayerMode, ["TOGGLE", "ON_ONLY", "OFF_ONLY"], readOnly)}</label>
			<label class="slot-row"><span>Spell guard</span>${enumSelect("spellAvailabilityMode", slot.spellAvailabilityMode, ["GUARD_UNAVAILABLE", "ALLOW_ATTEMPT"], readOnly)}</label>
			<label class="slot-row"><span>Return panel</span><input data-slot-key="returnPanel" type="checkbox" ${slot.returnPanel ? "checked" : ""} ${disabled}></label>
			<label class="slot-row"><span>Center Mouse</span><input data-slot-key="returnMouseCenter" type="checkbox" ${slot.returnMouseCenter ? "checked" : ""} ${disabled}></label>
		</article>
	`;
}

function enumSelect(key, value, options, readOnly = false) {
	const disabled = readOnly ? "disabled" : "";
	return `<select data-slot-key="${escapeHtml(key)}" ${disabled}>${options.map(option => `<option value="${escapeHtml(option)}" ${String(value) === option ? "selected" : ""}>${escapeHtml(option)}</option>`).join("")}</select>`;
}

function collectMobFarmerConfigFromForm() {
	const settings = {};
	for (const element of mobFarmerConfigForm.querySelectorAll("[data-config-key]")) {
		const key = element.getAttribute("data-config-key");
		settings[key] = element.type === "checkbox" ? element.checked : element.value;
	}
	const actionSlots = Array.from(actionsSlotList.querySelectorAll("[data-action-slot]")).map(card => {
		const slot = { slot: Number(card.getAttribute("data-action-slot")) };
		for (const element of card.querySelectorAll("[data-slot-key]")) {
			const key = element.getAttribute("data-slot-key");
			slot[key] = element.type === "checkbox" ? element.checked : element.value;
		}
		return slot;
	});
	return {
		version: mobFarmerConfigPayload?.version || 1,
		settings,
		actionSlots,
	};
}

function showMobFarmerConfigErrors(errors) {
	mobFarmerConfigErrors.classList.add("visible");
	mobFarmerConfigErrors.innerHTML = `<strong>Config not applied.</strong><ul>${errors.map(error => `<li>${escapeHtml(error)}</li>`).join("")}</ul>`;
	setConfigState(mobFarmerConfigStatus, "Validation failed", "bad");
	if (actionsConfigErrors) {
		actionsConfigErrors.classList.add("visible");
		actionsConfigErrors.innerHTML = `<strong>Config not applied.</strong><ul>${errors.map(error => `<li>${escapeHtml(error)}</li>`).join("")}</ul>`;
		setConfigState(actionsConfigStatus, "Validation failed", "bad");
	}
}

function clearMobFarmerConfigErrors() {
	mobFarmerConfigErrors.classList.remove("visible");
	mobFarmerConfigErrors.classList.remove("warning");
	mobFarmerConfigErrors.innerHTML = "";
	if (actionsConfigErrors) {
		actionsConfigErrors.classList.remove("visible");
		actionsConfigErrors.classList.remove("warning");
		actionsConfigErrors.innerHTML = "";
	}
}

function showMobFarmerConfigWarning(message) {
	mobFarmerConfigErrors.classList.add("visible", "warning");
	mobFarmerConfigErrors.innerHTML = `<strong>Draft preserved.</strong> ${escapeHtml(message)}`;
}

function setConfigState(element, message, tone = "neutral") {
	element.textContent = message;
	element.classList.remove("ok", "bad", "warn", "neutral");
	element.classList.add(tone);
}

function deepClone(value) {
	return JSON.parse(JSON.stringify(value || {}));
}

function normalizeMobFarmerDraft(payload) {
	const live = mobFarmerConfigPayload || {};
	const draft = {
		...deepClone(live),
		...deepClone(payload),
		version: payload?.version || live.version || 1,
		settings: {
			...(live.settings || {}),
			...(payload?.settings || {}),
		},
	};
	if (Array.isArray(payload?.schema)) {
		draft.schema = payload.schema;
	} else if (Array.isArray(live.schema)) {
		draft.schema = live.schema;
	}
	if (Array.isArray(payload?.actionSlots)) {
		draft.actionSlots = payload.actionSlots;
	} else if (Array.isArray(live.actionSlots)) {
		draft.actionSlots = live.actionSlots;
	}
	return draft;
}

function normalizeSkillFarmerDraft(skill, payload) {
	const live = skillFarmerConfigPayloads[skill] || {};
	return {
		...deepClone(live),
		...deepClone(payload),
		version: payload?.version || live.version || 1,
		skill,
		settings: {
			...(live.settings || {}),
			...(payload?.settings || {}),
		},
		schema: Array.isArray(payload?.schema) ? payload.schema : (live.schema || []),
		presets: Array.isArray(payload?.presets) ? payload.presets : (live.presets || []),
	};
}

function configFingerprint(payload) {
	if (!payload || payload.unavailable) {
		return "";
	}
	return JSON.stringify({
		version: payload.version || 1,
		settings: payload.settings || {},
		actionSlots: payload.actionSlots || [],
	});
}

function renderSkillFarmer(skill, statusPayload, configPayload) {
	const refs = skillFarmers[skill];
	if (!refs) {
		return;
	}

	receiveSkillFarmerLiveConfig(skill, configPayload);

	const status = statusPayload?.unavailable ? null : statusPayload;
	if (!status) {
		refs.state.textContent = "Unavailable";
		refs.state.classList.remove("ok", "warn", "neutral");
		refs.state.classList.add("bad");
		refs.decision.textContent = statusPayload?.error || "Endpoint unavailable until client relaunch.";
		refs.statusGrid.innerHTML = "";
		refs.details.innerHTML = `<p class="empty compact">Relaunch the newly compiled client to expose /automation/${skill}/status.</p>`;
		return;
	}

	const running = Boolean(status.running);
	const live = Boolean(status.live);
	const selected = status.selected || {};
	const inventory = status.inventory || {};
	const candidates = Array.isArray(status.candidates) ? status.candidates : [];
	const candidateSummary = status.candidateSummary || {};

	refs.state.textContent = `${running ? "Running" : "Stopped"} - ${live ? "Live" : "Dry"}`;
	refs.state.classList.remove("ok", "bad", "warn", "neutral");
	refs.state.classList.add(running ? "ok" : "bad");

	const decisionText = status.decision || status.currentAction || status.lastFailureReason || "No decision yet.";
	refs.decision.textContent = decisionText;

	syncSkillFarmerRunControlsFromStatus(skill, status);

	refs.statusGrid.innerHTML = `
		<div class="reference-metric-strip">
			${metricCard(skill === "mining" ? "pickaxe" : "tree-pine", "Target", status.target || "Unknown")}
			${metricCard("activity", "Action", status.currentAction || "Unknown", compactDecisionTone(status.currentAction))}
			${metricCard("crosshair", "Selected", selected.name || selected.label || "None", selected.name || selected.label ? "good" : "warn")}
			${metricCard("route", "Path Distance", selected.pathDistance ?? selected.interactionPathDistance ?? "None")}
			${metricCard("check-circle", "Reachable", selected.reachable === undefined ? "Unknown" : String(Boolean(selected.reachable)), selected.reachable ? "good" : "bad")}
			${metricCard("radar", "Scan Radius", `${selectedValue(status.scanRadiusTiles, "Unknown")} tiles`)}
			${metricCard("users", "Max Candidates", selectedValue(status.maxCandidates, "Unknown"))}
			${metricCard("coins", "Inventory GE", formatGp(inventory.gePrice), "gold")}
			${metricCard("sparkles", "Inventory HA", formatGp(inventory.haPrice), "gold")}
			${metricCard("backpack", "Free Slots", selectedValue(inventory.freeSlots, "Unknown"))}
			${metricCard("trash-2", "Drop Candidate", inventory.dropCandidate?.name || "None", inventory.dropCandidate?.name ? "warn" : "")}
		</div>
	`;

	refs.details.innerHTML = `
		<div class="farmer-runtime-grid">
			<div class="card side">
				<div class="card-header">
					<h3>${icon("crosshair")}${skill === "mining" ? "Selected Rock Details" : "Selected Tree Details"}</h3>
				</div>
				<div class="card-body">
					${renderSkillSelectedBlock(skill, selected)}
				</div>
			</div>

			<div class="card">
				<div class="card-header split">
					<h3>${icon("list")}${skill === "mining" ? "Candidate Rocks" : "Candidate Trees"} (${candidates.length})</h3>
					<span class="summary">${escapeHtml(decisionText)}</span>
				</div>
				${renderSkillCandidateTableInner(skill, candidates, candidateSummary)}
			</div>

			<div class="card side">
				<div class="card-header">
					<h3>${icon("backpack")}Inventory & Drop Policy</h3>
				</div>
				<div class="card-body">
					${renderDropPolicy(status.dropPolicy || inventory.policy || status.loot || {}, inventory)}
				</div>
			</div>
		</div>
	`;

	refreshIcons();
}

function receiveSkillFarmerLiveConfig(skill, payload) {
	const refs = skillFarmers[skill];
	if (!refs || !payload) {
		return;
	}
	if (payload.unavailable) {
		if (!skillFarmerDraftPayloads[skill]) {
			setConfigState(refs.configStatus, "Config endpoint unavailable", "bad");
			refs.configForm.innerHTML = `<p class="empty compact">The running CV Helper build does not expose ${escapeHtml(payload.path || "this config endpoint")} yet.</p>`;
		}
		return;
	}
	const nextFingerprint = configFingerprint(payload);
	const previousFingerprint = skillFarmerLastLiveFingerprints[skill] || "";
	skillFarmerConfigPayloads[skill] = payload;
	skillFarmerLastLiveFingerprints[skill] = nextFingerprint;
	renderSkillFarmerPresets(skill, payload);
	if (skillFarmerDraftPayloads[skill] && nextFingerprint !== configFingerprint(skillFarmerDraftPayloads[skill])) {
		skillFarmerLiveChangedWhileDirty[skill] = true;
		if (skillFarmerDraftDirty[skill]) {
			showSkillFarmerConfigWarning(skill, "Live config changed while this draft has unsaved changes. The draft was not overwritten.");
			setConfigState(refs.configStatus, "Unsaved changes", "warn");
		} else if (previousFingerprint && nextFingerprint !== previousFingerprint) {
			showSkillFarmerConfigWarning(skill, "Live config changed after this draft was loaded. Use Load current into draft or Reset draft to refresh it.");
			setConfigState(refs.configStatus, "Live changed", "warn");
		}
		return;
	}
	if (!skillFarmerDraftPayloads[skill]) {
		renderSkillFarmerDraftPlaceholder(skill, "Live config is available. Load current into draft to edit safely.");
	} else if (!skillFarmerDraftDirty[skill]) {
		setConfigState(refs.configStatus, "Synced", "ok");
	}
}

function renderSkillFarmerDraftPlaceholder(skill, message) {
	const refs = skillFarmers[skill];
	setConfigState(refs.configStatus, skillFarmerConfigPayloads[skill] ? "Live available" : "No draft loaded", "neutral");
	refs.configForm.innerHTML = `<p class="empty compact">${escapeHtml(message)}</p>`;
}

function renderSkillFarmerConfig(skill, payload, stateLabel = "Synced", tone = "ok") {
	const refs = skillFarmers[skill];
	if (!refs || !payload) {
		renderSkillFarmerDraftPlaceholder(skill, "No draft loaded. Load current config or import JSON to begin editing.");
		return;
	}
	if (payload.unavailable) {
		setConfigState(refs.configStatus, `Unavailable: ${payload.error}`, "bad");
		refs.configForm.innerHTML = `<p class="empty compact">The running CV Helper build does not expose ${escapeHtml(payload.path || "this config endpoint")} yet.</p>`;
		return;
	}
	const settings = payload.settings || {};
	const schema = Array.isArray(payload.schema) ? payload.schema : [];
	setConfigState(refs.configStatus, `${stateLabel} - v${payload.version || 1}`, tone);
	refs.configForm.innerHTML = schema.length
		? schema.map(field => renderConfigField(field, settings[field.key], `${skill}-config`)).join("")
		: `<p class="empty compact">This draft has no editable schema. Load current into draft before importing partial settings.</p>`;
	syncSkillFarmerRunControlsFromDraft(skill, settings);
	renderSkillFarmerPresets(skill, payload);
	clearSkillFarmerConfigErrors(skill);
}

function renderSkillFarmerPresets(skill, payload) {
	const refs = skillFarmers[skill];
	const presets = Array.isArray(payload.presets) ? payload.presets : [];
	const previousValue = refs.preset.value;
	refs._presets = presets;
	refs.preset.innerHTML = presets.length
		? presets.map((profile, index) => `<option value="${index}">${escapeHtml(profile.name || profile.target || `Preset ${index + 1}`)}</option>`).join("")
		: `<option value="">No presets</option>`;
	if (!presets.length) {
		return;
	}

	const previousIndex = Number(previousValue);
	if (Number.isInteger(previousIndex) && previousIndex >= 0 && previousIndex < presets.length) {
		refs.preset.value = previousValue;
		return;
	}

	const targetValue = refs.target.value.trim().toLowerCase();
	const matchingIndex = presets.findIndex(profile => String(profile?.target || "").trim().toLowerCase() === targetValue);
	if (matchingIndex >= 0) {
		refs.preset.value = String(matchingIndex);
	}
}

async function runSkillFarmerAction(skill, action) {
	if (!(await ensurePort())) {
		return;
	}
	const refs = skillFarmers[skill];
	const target = refs.target.value.trim();
	const live = refs.mode.value === "true";
	setSkillFarmerRunControlDraft(skill, {
		targetDirty: false,
		modeDirty: false,
	});
	const path = action === "stop"
		? `/automation/${skill}/stop`
		: `/automation/${skill}/${action}?target=${encodeURIComponent(target)}&live=${encodeURIComponent(String(live))}`;
	const result = await request(path, { method: "POST" });
	renderSkillFarmer(skill, result, skillFarmerConfigPayloads[skill]);
	await delay(700);
	await refreshAll();
}

async function loadSkillFarmerConfig(skill) {
	if (!(await ensurePort())) {
		return;
	}
	try {
		clearSkillFarmerConfigErrors(skill);
		const payload = await request(`/automation/${skill}/config`);
		skillFarmerConfigPayloads[skill] = payload;
		skillFarmerLastLiveFingerprints[skill] = configFingerprint(payload);
		skillFarmerDraftPayloads[skill] = deepClone(payload);
		skillFarmerDraftDirty[skill] = false;
		skillFarmerLiveChangedWhileDirty[skill] = false;
		renderSkillFarmerConfig(skill, skillFarmerDraftPayloads[skill], "Synced", "ok");
	} catch (error) {
		showSkillFarmerConfigErrors(skill, [formatError(error)]);
	}
}

function resetSkillFarmerDraft(skill) {
	if (skillFarmerConfigPayloads[skill]) {
		skillFarmerDraftPayloads[skill] = deepClone(skillFarmerConfigPayloads[skill]);
		skillFarmerDraftDirty[skill] = false;
		skillFarmerLiveChangedWhileDirty[skill] = false;
		renderSkillFarmerConfig(skill, skillFarmerDraftPayloads[skill], "Synced", "ok");
		clearSkillFarmerConfigErrors(skill);
	} else {
		skillFarmerDraftPayloads[skill] = null;
		skillFarmerDraftDirty[skill] = false;
		renderSkillFarmerDraftPlaceholder(skill, "No live config loaded yet. Use Load current into draft first.");
	}
}

async function saveSkillFarmerConfig(skill) {
	if (!(await ensurePort())) {
		return;
	}
	try {
		clearSkillFarmerConfigErrors(skill);
		const payload = collectSkillFarmerConfigFromForm(skill);
		const result = await request(`/automation/${skill}/config`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(payload),
		});
		skillFarmerConfigPayloads[skill] = result.config || payload;
		skillFarmerLastLiveFingerprints[skill] = configFingerprint(skillFarmerConfigPayloads[skill]);
		skillFarmerDraftPayloads[skill] = deepClone(skillFarmerConfigPayloads[skill]);
		skillFarmerDraftDirty[skill] = false;
		skillFarmerLiveChangedWhileDirty[skill] = false;
		renderSkillFarmerConfig(skill, skillFarmerDraftPayloads[skill], "Applied successfully", "ok");
		await refreshAll();
	} catch (error) {
		showSkillFarmerConfigErrors(skill, [formatError(error)]);
		setConfigState(skillFarmers[skill].configStatus, "Apply failed", "bad");
	}
}

function exportSkillFarmerConfig(skill) {
	const refs = skillFarmers[skill];
	const payload = skillFarmerDraftPayloads[skill]
		? collectSkillFarmerConfigFromForm(skill)
		: (skillFarmerConfigPayloads[skill] || { version: 1, skill, settings: {} });
	refs.configJson.value = JSON.stringify(payload, null, 2);
	setConfigState(refs.configStatus, skillFarmerDraftDirty[skill] ? "Unsaved changes" : "Synced", skillFarmerDraftDirty[skill] ? "warn" : "ok");
}

function importSkillFarmerConfig(skill) {
	const refs = skillFarmers[skill];
	try {
		clearSkillFarmerConfigErrors(skill);
		const raw = refs.configJson.value.trim();
		if (!raw) {
			showSkillFarmerConfigErrors(skill, ["Paste exported JSON before importing."]);
			return;
		}
		skillFarmerDraftPayloads[skill] = normalizeSkillFarmerDraft(skill, JSON.parse(raw));
		skillFarmerDraftDirty[skill] = true;
		renderSkillFarmerConfig(skill, skillFarmerDraftPayloads[skill], "Draft modified", "warn");
	} catch (error) {
		showSkillFarmerConfigErrors(skill, [formatError(error)]);
	}
}

function collectSkillFarmerConfigFromForm(skill) {
	const refs = skillFarmers[skill];
	const settings = {};
	for (const element of refs.configForm.querySelectorAll("[data-config-key]")) {
		const key = element.getAttribute("data-config-key");
		settings[key] = element.type === "checkbox" ? element.checked : element.value;
	}
	if (!settings.target) {
		settings.target = refs.target.value.trim();
	}
	settings.live = refs.mode.value === "true";
	return {
		version: skillFarmerDraftPayloads[skill]?.version || skillFarmerConfigPayloads[skill]?.version || 1,
		skill,
		settings,
	};
}

function markSkillFarmerDraftDirty(skill) {
	if (!skillFarmerDraftPayloads[skill]) {
		return;
	}
	skillFarmerDraftDirty[skill] = true;
	const refs = skillFarmers[skill];
	setConfigState(refs.configStatus, skillFarmerLiveChangedWhileDirty[skill] ? "Unsaved changes; live changed" : "Unsaved changes", "warn");
}

function ensureSkillFarmerRunControlDraft(skill) {
	if (!skillFarmerRunControlDrafts[skill]) {
		skillFarmerRunControlDrafts[skill] = {
			targetDirty: false,
			modeDirty: false,
		};
	}
	return skillFarmerRunControlDrafts[skill];
}

function setSkillFarmerRunControlDraft(skill, patch) {
	const draft = ensureSkillFarmerRunControlDraft(skill);
	Object.assign(draft, patch || {});
}

function markSkillFarmerRunControlDirty(skill, control) {
	const refs = skillFarmers[skill];
	if (!refs) {
		return;
	}
	setSkillFarmerRunControlDraft(skill, {
		targetDirty: control === "target" ? true : ensureSkillFarmerRunControlDraft(skill).targetDirty,
		modeDirty: control === "mode" ? true : ensureSkillFarmerRunControlDraft(skill).modeDirty,
	});
	if (skillFarmerDraftPayloads[skill]) {
		markSkillFarmerDraftDirty(skill);
	}
}

function syncSkillFarmerRunControlsFromStatus(skill, status) {
	const refs = skillFarmers[skill];
	const draft = ensureSkillFarmerRunControlDraft(skill);
	if (!draft.targetDirty && document.activeElement !== refs.target) {
		refs.target.value = status.target || refs.target.value;
	}
	if (!draft.modeDirty && document.activeElement !== refs.mode) {
		refs.mode.value = String(Boolean(status.live));
	}
}

function syncSkillFarmerRunControlsFromDraft(skill, settings) {
	const refs = skillFarmers[skill];
	if ((settings.target || "") && document.activeElement !== refs.target) {
		refs.target.value = settings.target;
	}
	if (settings.live !== undefined && document.activeElement !== refs.mode) {
		refs.mode.value = String(Boolean(settings.live));
	}
	setSkillFarmerRunControlDraft(skill, {
		targetDirty: false,
		modeDirty: false,
	});
}

function resolveSkillFarmerPresetLive(preset) {
	if (!preset || typeof preset !== "object") {
		return undefined;
	}
	if (typeof preset.live === "boolean") {
		return preset.live;
	}
	if (preset.settings && typeof preset.settings.live === "boolean") {
		return preset.settings.live;
	}
	return undefined;
}

function applySkillFarmerPreset(skill, preset) {
	const refs = skillFarmers[skill];
	if (!refs || !preset) {
		return;
	}
	if (preset.target) {
		refs.target.value = preset.target;
	}
	const presetLive = resolveSkillFarmerPresetLive(preset);
	if (presetLive !== undefined) {
		refs.mode.value = String(Boolean(presetLive));
	}
	setSkillFarmerRunControlDraft(skill, {
		targetDirty: true,
		modeDirty: presetLive !== undefined,
	});
	if (skillFarmerDraftPayloads[skill]) {
		skillFarmerDraftPayloads[skill].settings.target = preset.target;
		if (presetLive !== undefined) {
			skillFarmerDraftPayloads[skill].settings.live = presetLive;
		}
		markSkillFarmerDraftDirty(skill);
		renderSkillFarmerConfig(skill, skillFarmerDraftPayloads[skill], "Draft modified", "warn");
	}
}

function showSkillFarmerConfigErrors(skill, errors) {
	const refs = skillFarmers[skill];
	refs.configErrors.classList.add("visible");
	refs.configErrors.innerHTML = `<strong>Config not applied.</strong><ul>${errors.map(error => `<li>${escapeHtml(error)}</li>`).join("")}</ul>`;
	setConfigState(refs.configStatus, "Validation failed", "bad");
}

function showSkillFarmerConfigWarning(skill, message) {
	const refs = skillFarmers[skill];
	refs.configErrors.classList.add("visible", "warning");
	refs.configErrors.innerHTML = `<strong>Draft preserved.</strong> ${escapeHtml(message)}`;
}

function clearSkillFarmerConfigErrors(skill) {
	const refs = skillFarmers[skill];
	refs.configErrors.classList.remove("visible");
	refs.configErrors.classList.remove("warning");
	refs.configErrors.innerHTML = "";
}

function renderSkillSelectedBlock(skill, selected) {
	const title = `${skill} selected target`;
	if (!selected || !Object.keys(selected).length) {
		return renderMobFarmerBlock(title, "No selected target.");
	}
	return renderMobFarmerBlock(title, {
		name: selected.name || selected.label,
		objectType: selected.objectType,
		id: selected.id,
		objectOrigin: selected.objectOrigin,
		objectSize: selected.objectSize,
		objectFootprint: selected.objectFootprint,
		interactionTile: selected.interactionTile,
		worldLocation: selected.worldLocation,
		straightDistance: selected.distance,
		pathDistance: selected.pathDistance,
		interactionPathDistance: selected.interactionPathDistance,
		interactionReachable: selected.interactionReachable,
		reachable: selected.reachable,
		visible: selected.visible,
		bounds: formatRect(selected.bounds || selected.canvasBounds || {}),
		clickPoint: selected.clickPoint,
		menuAction: selected.menuAction,
		pathFailureReason: selected.pathFailureReason,
		reasons: selected.reasons,
		matchedToken: selected.matchedToken,
		matchType: selected.matchType,
		targetMatched: selected.targetMatched,
		actionMatched: selected.actionMatched,
		targetText: selected.targetText,
		matchedCount: selected.evaluatedInteractionTiles,
		walkableTiles: selected.walkableInteractionTiles,
		blockedTiles: selected.blockedInteractionTiles,
	});
}

function renderSkillCandidateSummary(candidates, summary) {
	if (!summary || typeof summary !== "object") {
		return "";
	}
	const s = summary;
	const selectable = candidates.filter(c => c.selectable).length;
	const matched = candidates.filter(c => c.targetMatched).length;
	const unreachable = candidates.filter(c => c.targetMatched && !c.reachable).length;
	const missingAction = candidates.filter(c => !c.actionMatched).length;
	return `
		<div class="candidate-summary">
			<strong>Candidate summary:</strong>
			total ${candidates.length},
			matched ${matched},
			selectable ${selectable},
			target mismatch ${s.targetMismatches ?? 0},
			unreachable ${unreachable},
			missing action ${missingAction}
		</div>
	`;
}

function renderSkillCandidateTable(skill, candidates, summary) {
	const rows = candidates.slice(0, 40).map(candidate => `
		<tr class="${candidate.selectable ? "" : "warn"}">
			<td>${escapeHtml(candidate.name || candidate.label || "")}<br><small>${escapeHtml(candidate.id ?? "")}</small></td>
			<td>${escapeHtml(candidate.objectType || "")}</td>
			<td>${escapeHtml(formatPoint(candidate.worldLocation))}</td>
			<td>${escapeHtml(formatPoint(candidate.objectFootprint) || "")}</td>
			<td>${escapeHtml(formatPoint(candidate.interactionTile) || "")}</td>
			<td>${escapeHtml(candidate.distance ?? "")}</td>
			<td>${escapeHtml(candidate.pathDistance ?? "")}</td>
			<td>${candidate.reachable ? "yes" : "no"}</td>
			<td>${candidate.visible ? "yes" : "no"}</td>
			<td>${candidate.matchedToken ? escapeHtml(candidate.matchedToken) : ""}<br><small>${escapeHtml(candidate.matchType || "")}</small></td>
			<td>${candidate.selectable ? "selectable" : "skip"}</td>
			<td>${escapeHtml((candidate.reasons || []).join(", ") || candidate.pathFailureReason || "")}</td>
		</tr>
	`).join("");
	return `
		<div class="detail-block wide">
			<header>
				<h4>${escapeHtml(skill)} candidates</h4>
				<span class="summary">${escapeHtml(candidates.length)} rocks/trees with path-distance filtering</span>
			</header>
			${renderSkillCandidateSummary(candidates, summary)}
			${candidates.length ? `
				<div class="table-wrap compact">
					<table>
						<thead>
							<tr>
								<th>Object</th>
								<th>Type</th>
								<th>Tile</th>
								<th>Footprint</th>
								<th>Interact Tile</th>
								<th>Straight</th>
								<th>Path</th>
								<th>Reach</th>
								<th>Vis</th>
								<th>Match</th>
								<th>Decision</th>
								<th>Reason</th>
							</tr>
						</thead>
						<tbody>${rows}</tbody>
					</table>
				</div>
			` : `<p class="empty compact">No visible matching objects yet.</p>`}
		</div>
	`;
}

function renderSkillCandidateTableInner(skill, candidates, summary) {
	const rows = candidates.slice(0, 80).map((candidate, index) => {
		const selectable = Boolean(candidate.selectable);
		const reachable = candidate.reachable === undefined ? "" : Boolean(candidate.reachable);
		const visible = candidate.visible === undefined ? "" : Boolean(candidate.visible);
		const depleted = candidate.depleted === undefined ? "" : Boolean(candidate.depleted);
		const rowClass = selectable ? "ok" : (!reachable ? "bad" : "warn");

		return `
			<tr class="${rowClass}">
				<td>${index + 1}</td>
				<td>${escapeHtml(candidate.name || candidate.label || "")}<br><small>${escapeHtml(candidate.id ?? "")}</small></td>
				<td>${escapeHtml(formatPoint(candidate.worldLocation))}</td>
				<td>${escapeHtml(candidate.distance ?? "")}</td>
				<td>${escapeHtml(candidate.pathDistance ?? candidate.interactionPathDistance ?? "")}</td>
				<td>${reachable === "" ? "Unknown" : reachable ? "Yes" : "No"}</td>
				<td>${visible === "" ? "Unknown" : visible ? "Yes" : "No"}</td>
				<td>${depleted === "" ? "Unknown" : depleted ? "Yes" : "No"}</td>
				<td>${selectable ? "Select" : "Skip"}</td>
				<td>${escapeHtml((candidate.reasons || []).join(", ") || candidate.pathFailureReason || candidate.decisionReason || "")}</td>
			</tr>
		`;
	}).join("");

	const s = summary || {};
	const selectableCount = candidates.filter(c => c.selectable).length;
	const unreachable = candidates.filter(c => c.targetMatched && c.reachable === false).length;

	return `
		<div class="candidate-summary">
			<strong>Candidate summary:</strong>
			total ${escapeHtml(candidates.length)},
			selectable ${escapeHtml(selectableCount)},
			target mismatch ${escapeHtml(s.targetMismatches ?? 0)},
			unreachable ${escapeHtml(unreachable)},
			missing action ${escapeHtml(s.missingAction ?? 0)}
		</div>
		${candidates.length ? `
			<div class="table-wrap compact">
				<table>
					<thead>
						<tr>
							<th>#</th>
							<th>Object</th>
							<th>World Tile</th>
							<th>Straight</th>
							<th>Path</th>
							<th>Reachable</th>
							<th>Visible</th>
							<th>Depleted</th>
							<th>Decision</th>
							<th>Reason</th>
						</tr>
					</thead>
					<tbody>${rows}</tbody>
				</table>
			</div>
		` : `<p class="empty compact">No visible matching objects yet.</p>`}
	`;
}

function readMobFarmerProfiles() {
	try {
		const parsed = JSON.parse(window.localStorage.getItem(PROFILE_STORAGE_KEY) || "{}");
		return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
	} catch (error) {
		return {};
	}
}

function writeMobFarmerProfiles(profiles) {
	window.localStorage.setItem(PROFILE_STORAGE_KEY, JSON.stringify(profiles, null, 2));
	renderMobFarmerProfiles();
}

function renderMobFarmerProfiles() {
	const profiles = readMobFarmerProfiles();
	const names = Object.keys(profiles).sort((a, b) => a.localeCompare(b));
	mobFarmerProfileSelect.innerHTML = names.length
		? names.map(name => `<option value="${escapeHtml(name)}">${escapeHtml(name)}</option>`).join("")
		: `<option value="">No saved profiles</option>`;
}

function saveMobFarmerProfile() {
	const name = (mobFarmerProfileName.value || "").trim();
	if (!name) {
		showMobFarmerConfigErrors(["Enter a profile name before saving."]);
		return;
	}
	const profiles = readMobFarmerProfiles();
	const formPayload = collectMobFarmerConfigFromForm();
	const payload = {
		...(mobFarmerConfigPayload || {}),
		version: formPayload.version,
		settings: formPayload.settings,
		actionSlots: formPayload.actionSlots,
	};
	payload.profileName = name;
	payload.savedAt = new Date().toISOString();
	profiles[name] = { name, savedAt: payload.savedAt, payload };
	writeMobFarmerProfiles(profiles);
	mobFarmerProfileSelect.value = name;
	mobFarmerConfigStatus.textContent = `Saved profile "${name}" locally`;
	logMobFarmerEvent(`profile saved: ${name}`);
}

function loadMobFarmerProfile() {
	const profiles = readMobFarmerProfiles();
	const profile = profiles[mobFarmerProfileSelect.value];
	if (!profile) {
		showMobFarmerConfigErrors(["Select a saved profile first."]);
		return;
	}
	mobFarmerDraftPayload = normalizeMobFarmerDraft(profile.payload);
	mobFarmerDraftDirty = true;
	mobFarmerLiveChangedWhileDirty = Boolean(
		mobFarmerConfigPayload
		&& configFingerprint(mobFarmerConfigPayload) !== configFingerprint(mobFarmerDraftPayload)
	);
	renderMobFarmerConfig(mobFarmerDraftPayload, "Draft modified", "warn");
	mobFarmerConfigJson.value = JSON.stringify(mobFarmerDraftPayload, null, 2);
	setConfigState(mobFarmerConfigStatus, `Loaded profile "${profile.name}" into draft`, "warn");
	logMobFarmerEvent(`profile loaded: ${profile.name}`);
}

function duplicateMobFarmerProfile() {
	const profiles = readMobFarmerProfiles();
	const profile = profiles[mobFarmerProfileSelect.value];
	if (!profile) {
		showMobFarmerConfigErrors(["Select a profile to duplicate."]);
		return;
	}
	const base = `${profile.name} copy`;
	let name = base;
	let suffix = 2;
	while (profiles[name]) {
		name = `${base} ${suffix++}`;
	}
	const payload = JSON.parse(JSON.stringify(profile.payload));
	payload.profileName = name;
	payload.savedAt = new Date().toISOString();
	profiles[name] = { name, savedAt: payload.savedAt, payload };
	writeMobFarmerProfiles(profiles);
	mobFarmerProfileSelect.value = name;
	mobFarmerProfileName.value = name;
	logMobFarmerEvent(`profile duplicated: ${name}`);
}

function deleteMobFarmerProfile() {
	const name = mobFarmerProfileSelect.value;
	if (!name) {
		showMobFarmerConfigErrors(["Select a profile to delete."]);
		return;
	}
	const profiles = readMobFarmerProfiles();
	delete profiles[name];
	writeMobFarmerProfiles(profiles);
	mobFarmerProfileName.value = "";
	mobFarmerConfigStatus.textContent = `Deleted profile "${name}" locally`;
	logMobFarmerEvent(`profile deleted: ${name}`);
}

function markMobFarmerDraftDirty() {
	if (!mobFarmerDraftPayload) {
		return;
	}
	mobFarmerDraftDirty = true;
	setConfigState(mobFarmerConfigStatus, mobFarmerLiveChangedWhileDirty ? "Unsaved changes; live changed" : "Unsaved changes", "warn");
}

const surfaceIcons = {
	prayer: "sparkles",
	spell: "book-open",
	minimap: "map",
	inventory: "backpack",
	equipment: "layers",
	panels: "layout-grid",
	combat: "sword",
	entities: "users",
};

function renderCounts(status, targetPayloads, entitiesPayload) {
	const payloadCounts = Object.fromEntries(targetPayloads
		.filter(payload => payload && !payload.unavailable)
		.map(payload => [payload.surface, payload.count]));
	const counts = surfaces.map(surface => {
		const payload = targetPayloads.find(candidate => candidate.surface === surface);
		const value = payloadCounts[surface] ?? status[`${surface}Targets`] ?? (payload?.unavailable ? "Unavailable" : 0);
		return [surface, value];
	});
	counts.push(["entities", entitiesPayload?.unavailable ? (status.entities ?? "Unavailable") : (entitiesPayload?.count ?? status.entities ?? 0)]);

	if (countGrid) {
		countGrid.innerHTML = counts.map(([surface, count]) => `
			<div class="count-card">
				${icon(surfaceIcons[surface] || "circle", "count-icon")}
				<span class="label">${escapeHtml(surface)}</span>
				<strong class="value">${count}</strong>
			</div>
		`).join("");
	}
}

function renderSurfaces(targetPayloads) {
	surfacesRoot.innerHTML = targetPayloads.map(renderSurface).join("");
}

function renderEntities(payload, nearestPayload) {
	if (payload?.unavailable) {
		entitiesRoot.innerHTML = `
			<div class="surface-card">
				<header>
					<div class="surface-title">
						<h3>nearby entities</h3>
						<span class="badge">Unavailable</span>
					</div>
				</header>
				<div class="body"><p class="empty">${escapeHtml(payload.path || "/entities")} is unavailable on the connected export. Relaunch the newest client build if you need entity verification here.</p></div>
			</div>
		`;
		return;
	}
	const rows = payload?.entities || [];
	const nearest = nearestPayload?.unavailable ? null : (nearestPayload?.entity || null);
	const body = rows
		.slice()
		.sort((a, b) => (a.distance ?? 9999) - (b.distance ?? 9999))
		.map(renderEntityRow)
		.join("");
	entitiesRoot.innerHTML = `
		<div class="surface-card">
			<header>
				<div class="surface-title">
					<h3>nearby entities</h3>
					<span class="badge">${payload?.count || 0}</span>
				</div>
				<small>${escapeHtml(payload?.gameState || "")}</small>
			</header>
			<div class="body">
				${nearest ? `
					<div class="nearest-banner">
						<strong>Nearest click target:</strong>
						<span>${escapeHtml(nearest.type || "")} ${escapeHtml(nearest.name || "(unnamed)")}</span>
						<span>distance ${escapeHtml(nearest.distance ?? "")}</span>
						<span>click ${escapeHtml(formatPoint(nearest.clickPoint))}</span>
					</div>
				` : ""}
				${rows.length ? `
					<input class="table-filter" data-table-filter="entities-table" placeholder="Filter entities">
					<div class="table-wrap">
						<table data-table-id="entities-table">
							<thead>
								<tr>
									<th data-sort>Name</th>
									<th data-sort>Type</th>
									<th data-sort>ID</th>
									<th data-sort>Distance</th>
									<th data-sort>World</th>
									<th data-sort>Bounds</th>
									<th data-sort>Click</th>
								</tr>
							</thead>
							<tbody>${body}</tbody>
						</table>
					</div>
				` : `<p class="empty">No nearby players/NPCs exported yet. Log in and refresh after the scene is visible.</p>`}
			</div>
		</div>
	`;
}

function renderEntityRow(entity) {
	return `
		<tr>
			<td>${escapeHtml(entity.name || "(unnamed)")}</td>
			<td>${escapeHtml(entity.type || "")}</td>
			<td>${escapeHtml(entity.id ?? "")}</td>
			<td>${escapeHtml(entity.distance ?? "")}</td>
			<td>${escapeHtml(formatPoint(entity.worldLocation))}</td>
			<td>${formatRect(entity.canvasBounds || {})}</td>
			<td>${escapeHtml(formatPoint(entity.clickPoint))}</td>
		</tr>
	`;
}

function renderSurface(payload) {
	if (payload?.unavailable) {
		return `
			<div class="surface-card">
				<header>
					<div class="surface-title">
						<h3>${escapeHtml(payload.surface || "unknown")}</h3>
						<span class="badge">Unavailable</span>
					</div>
				</header>
				<div class="body"><p class="empty">${escapeHtml(payload.path || `/targets/${payload.surface || "unknown"}`)} is unavailable on the connected export. Relaunch the newest client build if you need this surface.</p></div>
			</div>
		`;
	}
	const rows = payload.targets || [];
	const body = rows.map(target => renderTargetRow(payload.surface, target)).join("");
	const tableId = `surface-${payload.surface || "unknown"}-table`;
	const empty = rows.length === 0
		? `<p class="empty">No targets. If this surface depends on a RuneLite tab, leave that tab visible and refresh.</p>`
		: "";

	return `
		<div class="surface-card">
			<header>
				<div class="surface-title">
					<h3>${escapeHtml(payload.surface || "unknown")}</h3>
					<span class="badge">${payload.count || 0}</span>
				</div>
				<small>${escapeHtml(payload.fresh === false ? `cached from ${payload.lastSeenAt || "unknown"}` : payload.gameState || "")}</small>
			</header>
			<div class="body">
				${empty}
				${rows.length ? `
					<input class="table-filter" data-table-filter="${escapeHtml(tableId)}" placeholder="Filter ${escapeHtml(payload.surface || "targets")}">
					<div class="table-wrap">
						<table data-table-id="${escapeHtml(tableId)}">
							<thead>
								<tr>
									<th data-sort>Label</th>
									<th data-sort>Widget</th>
									<th data-sort>Item</th>
									<th data-sort>Bounds</th>
									<th data-sort>Center</th>
									<th data-sort>Actions</th>
								</tr>
							</thead>
							<tbody>${body}</tbody>
						</table>
					</div>
				` : ""}
			</div>
		</div>
	`;
}

function renderTargetRow(surface, target) {
	const suspicious = targetWarnings(surface, target).length > 0;
	const bounds = target.bounds || {};
	const center = target.center || {};
	const actions = (target.actions || []).filter(Boolean).join(", ");

	return `
		<tr class="${suspicious ? "warn" : ""}">
			<td>${escapeHtml(target.label || target.name || target.text || "(unnamed)")}</td>
			<td>${escapeHtml(target.widgetId ?? "")}</td>
			<td>${escapeHtml(target.itemId ?? "")}${target.itemQuantity ? ` x${escapeHtml(target.itemQuantity)}` : ""}</td>
			<td>${formatRect(bounds)}</td>
			<td>${escapeHtml(center.x ?? "?")}, ${escapeHtml(center.y ?? "?")}</td>
			<td>${escapeHtml(actions)}</td>
		</tr>
	`;
}

function renderWarnings(status, targetPayloads, entitiesPayload) {
	const warnings = [];
	const player = status.player || {};
	const gameState = String(player.gameState || "Unknown");

	if (LOGIN_SCREEN_STATES.has(gameState) || !player.loggedIn) {
		warnings.push(`CV Helper is healthy but RuneLite is at ${gameState}. Empty target surfaces are expected until login completes.`);
	}

	if (status.port && status.preferredPort && String(status.port) !== String(status.preferredPort)) {
		warnings.push(`CV Helper recovered onto fallback port ${status.port} because preferred port ${status.preferredPort} was unavailable at startup.`);
	}

	for (const payload of targetPayloads) {
		if (payload?.unavailable) {
			warnings.push(`${payload.surface} endpoint unavailable on this export (${payload.error || payload.path}).`);
			continue;
		}
		if ((payload.count || 0) === 0) {
			warnings.push(`${payload.surface} has zero targets. Verify the corresponding tab/interface is visible.`);
		}
		for (const target of payload.targets || []) {
			for (const warning of targetWarnings(payload.surface, target)) {
				warnings.push(`${payload.surface}: ${warning}`);
			}
		}
	}

	if (entitiesPayload?.unavailable) {
		warnings.push(`entities endpoint unavailable on this export (${entitiesPayload.error || entitiesPayload.path}).`);
	}

	warningsList.innerHTML = warnings.length
		? warnings.map(warning => `<li>${escapeHtml(warning)}</li>`).join("")
		: `<li class="all-clear">${icon("check-circle")}<div><strong>No verifier warnings right now.</strong><span>All systems nominal.</span></div></li>`;
}

function mobFarmerLatestReason(status) {
	const candidates = [
		status.status,
		status.decision,
		status.survivalDecision,
		status.intermediateDecision,
		status.lootDecision,
		status.lastActionAttempt?.decision,
		status.lastActionAttempt?.guardReason,
		status.lastActionAttempt?.failureReason,
	];
	return candidates.find(value => typeof value === "string" && value.trim()) || "";
}

function logMobFarmerEvent(message) {
	mobFarmerEvents.unshift({
		at: new Date(),
		message,
	});
	mobFarmerEvents = mobFarmerEvents.slice(0, 8);
	renderMobFarmerLog();
}

function renderMobFarmerLog() {
	mobFarmerLog.innerHTML = mobFarmerEvents.length
		? `<ul>${mobFarmerEvents.map(entry => `<li><strong>${formatRelativeTime(entry.at)}</strong> ${escapeHtml(entry.message)}</li>`).join("")}</ul>`
		: `<p class="empty compact">No mob-farmer actions yet.</p>`;
}

function renderMobFarmerRaw() {
	if (!mobFarmerRawToggle.checked) {
		mobFarmerRaw.hidden = true;
		mobFarmerRaw.textContent = "";
		return;
	}
	mobFarmerRaw.hidden = false;
	mobFarmerRaw.textContent = JSON.stringify(mobFarmerRawPayload ?? {}, null, 2);
}

function renderRawData() {
	renderMobFarmerRaw();
	const entries = Object.entries(latestRawPayloads || {}).filter(([, value]) => value !== undefined);
	rawDataRoot.innerHTML = entries.map(([key, value]) => `
		<details>
			<summary>${escapeHtml(key)}</summary>
			<pre class="code-block">${escapeHtml(JSON.stringify(value ?? {}, null, 2))}</pre>
		</details>
	`).join("");
}

function renderMobFarmerBlock(title, payload) {
	const rows = structuredRows(payload);
	const summary = typeof payload === "string" ? payload : rows[0]?.[1] || "No data";
	return `
		<div class="detail-block">
			<header>
				<h4>${escapeHtml(title)}</h4>
				<span class="summary">${escapeHtml(summary)}</span>
			</header>
			${rows.length ? `
				<div class="table-wrap compact">
					<table>
						<thead><tr><th data-sort>Field</th><th data-sort>Value</th></tr></thead>
						<tbody>${rows.map(([key, value]) => `<tr><td>${escapeHtml(key)}</td><td>${escapeHtml(value)}</td></tr>`).join("")}</tbody>
					</table>
				</div>
			` : `<p class="empty compact">No data.</p>`}
		</div>
	`;
}

function renderMobFarmerCandidates(title, candidates) {
	if (title.toLowerCase().includes("loot")) {
		return renderLootCandidateTable(title, candidates);
	}
	const rows = candidates.map(candidate => `<li>${escapeHtml(formatMobFarmerCandidate(candidate))}</li>`).join("");
	return `
		<div class="detail-block">
			<header>
				<h4>${escapeHtml(title)}</h4>
				<span class="summary">${escapeHtml(candidates.length)} entries</span>
			</header>
			${candidates.length ? `<ul class="compact-list">${rows}</ul>` : `<p class="empty compact">No entries.</p>`}
		</div>
	`;
}

function renderLootCandidateTable(title, candidates) {
	const rows = candidates.slice(0, 30).map(candidate => `
		<tr class="${candidate.selectable ? "" : "warn"}">
			<td>${escapeHtml(candidate.name || "")}<br><small>${escapeHtml(candidate.itemId ?? "")} x${escapeHtml(candidate.quantity ?? 1)}</small></td>
			<td>${formatGp(candidate.gePriceEach)}</td>
			<td>${formatGp(candidate.totalStackGeValue ?? candidate.gePrice)}</td>
			<td>${formatGp(candidate.haPriceEach)}</td>
			<td>${formatGp(candidate.totalStackHaValue ?? candidate.haPrice)}</td>
			<td>${candidate.highPriority ? "priority" : candidate.selectable ? "loot" : "skip"}</td>
			<td>${escapeHtml((candidate.reasons || []).join(", "))}</td>
		</tr>
	`).join("");
	return `
		<div class="detail-block wide">
			<header>
				<h4>${escapeHtml(title)}</h4>
				<span class="summary">${escapeHtml(candidates.length)} entries with stack GE/HA diagnostics</span>
			</header>
			${candidates.length ? `
				<div class="table-wrap compact">
					<table>
						<thead>
							<tr>
								<th>Item</th>
								<th>GE each</th>
								<th>GE stack</th>
								<th>HA each</th>
								<th>HA stack</th>
								<th>Decision</th>
								<th>Reason</th>
							</tr>
						</thead>
						<tbody>${rows}</tbody>
					</table>
				</div>
			` : `<p class="empty compact">No entries.</p>`}
		</div>
	`;
}

function renderHighAlchCandidates(candidates) {
	const rows = candidates.slice(0, 20).map(candidate => `
		<tr class="${candidate.eligible ? "" : "warn"}">
			<td>${escapeHtml(candidate.name || "")}<br><small>${escapeHtml(candidate.id ?? "")} slot ${escapeHtml(candidate.slot ?? "")}</small></td>
			<td>${formatGp(candidate.geEach)}</td>
			<td>${formatGp(candidate.haEach)}</td>
			<td>${formatGp(candidate.deltaEach)}</td>
			<td>${candidate.eligible ? "eligible" : "skip"}</td>
			<td>${escapeHtml((candidate.reasons || []).join(", "))}</td>
		</tr>
	`).join("");
	return `
		<div class="detail-block wide">
			<header>
				<h4>High Alch candidates</h4>
				<span class="summary">${escapeHtml(candidates.length)} inventory candidates, policy-only this pass</span>
			</header>
			${candidates.length ? `
				<div class="table-wrap compact">
					<table>
						<thead>
							<tr>
								<th>Item</th>
								<th>GE each</th>
								<th>HA each</th>
								<th>Delta</th>
								<th>Decision</th>
								<th>Reason</th>
							</tr>
						</thead>
						<tbody>${rows}</tbody>
					</table>
				</div>
			` : `<p class="empty compact">No high-alch candidates reported.</p>`}
		</div>
	`;
}

function renderMobFarmerMenuEntries(entries) {
	const rows = entries.slice(0, 30).map(entry => {
		if (typeof entry === "string") {
			return `<tr><td>${escapeHtml(entry)}</td><td></td><td></td><td></td></tr>`;
		}
		const timestamp = entry.at ? formatRelativeTime(new Date(entry.at)) : "—";
		return `
			<tr>
				<td>${escapeHtml(entry.option || "")}</td>
				<td>${escapeHtml(entry.npcName || entry.target || "")}</td>
				<td>${escapeHtml(entry.menuAction || "")}</td>
				<td>${escapeHtml(timestamp)}</td>
			</tr>
		`;
	}).join("");
	return `
		<div class="detail-block">
			<header>
				<h4>Recent menu entries</h4>
				<span class="summary">${escapeHtml(entries.length)} entries</span>
			</header>
			${entries.length ? `
				<div class="table-wrap compact">
					<table>
						<thead>
							<tr><th data-sort>Option</th><th data-sort>Target</th><th data-sort>Action</th><th data-sort>At</th></tr>
						</thead>
						<tbody>${rows}</tbody>
					</table>
				</div>
			` : `<p class="empty compact">No recent menu entries.</p>`}
		</div>
	`;
}

function renderMobTargetCandidateTableInner(candidates) {
	if (!candidates.length) {
		return `<p class="empty compact">No target candidates.</p>`;
	}

	const rows = candidates.slice(0, 50).map(candidate => `
		<tr class="${candidate.selectable ? "ok" : "warn"}">
			<td>${escapeHtml(candidate.name || candidate.label || candidate.npcName || candidate.id || candidate.npcId || "candidate")}</td>
			<td>${escapeHtml(candidate.type || "npc")}</td>
			<td>${escapeHtml(candidate.id || candidate.npcId || "")}</td>
			<td>${escapeHtml(candidate.distance ?? "")}</td>
			<td>${escapeHtml(candidate.hitpointsRatio || candidate.hp || "")}</td>
			<td>${escapeHtml(candidate.engaged ?? "")}</td>
			<td>${escapeHtml(candidate.lineOfSight ?? candidate.los ?? "")}</td>
			<td>${candidate.selectable ? "yes" : "no"}</td>
			<td>${escapeHtml((candidate.reasons || []).join(", ") || candidate.reason || "")}</td>
		</tr>
	`).join("");

	return `
		<div class="table-wrap compact">
			<table>
				<thead>
					<tr>
						<th>Name</th>
						<th>Type</th>
						<th>ID</th>
						<th>Dist</th>
						<th>HP</th>
						<th>Engaged</th>
						<th>LOS</th>
						<th>Selectable</th>
						<th>Reason</th>
					</tr>
				</thead>
				<tbody>${rows}</tbody>
			</table>
		</div>
	`;
}

function renderLootCandidateTableInner(candidates) {
	if (!candidates.length) {
		return `<p class="empty compact">No loot candidates.</p>`;
	}

	const rows = candidates.slice(0, 40).map(candidate => `
		<tr class="${candidate.selectable ? "ok" : "warn"}">
			<td>${escapeHtml(candidate.name || "")}<br><small>${escapeHtml(candidate.itemId ?? "")} x${escapeHtml(candidate.quantity ?? 1)}</small></td>
			<td>${formatGp(candidate.gePriceEach)}</td>
			<td>${formatGp(candidate.totalStackGeValue ?? candidate.gePrice)}</td>
			<td>${formatGp(candidate.haPriceEach)}</td>
			<td>${formatGp(candidate.totalStackHaValue ?? candidate.haPrice)}</td>
			<td>${candidate.highPriority ? "priority" : candidate.selectable ? "loot" : "skip"}</td>
			<td>${escapeHtml((candidate.reasons || []).join(", "))}</td>
		</tr>
	`).join("");

	return `
		<div class="table-wrap compact">
			<table>
				<thead>
					<tr>
						<th>Item</th>
						<th>GE each</th>
						<th>GE stack</th>
						<th>HA each</th>
						<th>HA stack</th>
						<th>Decision</th>
						<th>Reason</th>
					</tr>
				</thead>
				<tbody>${rows}</tbody>
			</table>
		</div>
	`;
}

function renderMobFarmerMenuEntriesInner(entries) {
	if (!entries.length) {
		return `<p class="empty compact">No recent menu entries.</p>`;
	}

	const rows = entries.slice(0, 30).map(entry => {
		if (typeof entry === "string") {
			return `<tr><td>${escapeHtml(entry)}</td><td></td><td></td><td></td></tr>`;
		}
		const timestamp = entry.at ? formatRelativeTime(new Date(entry.at)) : "—";
		return `
			<tr>
				<td>${escapeHtml(entry.option || "")}</td>
				<td>${escapeHtml(entry.npcName || entry.target || "")}</td>
				<td>${escapeHtml(entry.menuAction || "")}</td>
				<td>${escapeHtml(timestamp)}</td>
			</tr>
		`;
	}).join("");

	return `
		<div class="table-wrap compact">
			<table>
				<thead>
					<tr><th>Option</th><th>Target</th><th>Action</th><th>At</th></tr>
				</thead>
				<tbody>${rows}</tbody>
			</table>
		</div>
	`;
}

function renderHighAlchCandidatesInner(candidates) {
	if (!candidates.length) {
		return `<p class="empty compact">No high-alch candidates reported.</p>`;
	}

	const rows = candidates.slice(0, 20).map(candidate => `
		<tr class="${candidate.eligible ? "ok" : "warn"}">
			<td>${escapeHtml(candidate.name || "")}<br><small>${escapeHtml(candidate.id ?? "")} slot ${escapeHtml(candidate.slot ?? "")}</small></td>
			<td>${formatGp(candidate.geEach)}</td>
			<td>${formatGp(candidate.haEach)}</td>
			<td>${formatGp(candidate.deltaEach)}</td>
			<td>${candidate.eligible ? "eligible" : "skip"}</td>
		</tr>
	`).join("");

	return `
		<div class="table-wrap compact">
			<table>
				<thead>
					<tr>
						<th>Item</th>
						<th>GE</th>
						<th>HA</th>
						<th>Delta</th>
						<th>Decision</th>
					</tr>
				</thead>
				<tbody>${rows}</tbody>
			</table>
		</div>
	`;
}

function renderCaptureStatus(captures) {
	const entries = Object.values(captures || {});
	if (!entries.length) {
		captureStatus.textContent = "No captures saved yet. Use the capture buttons to queue one.";
		captureGrid.innerHTML = "";
		return;
	}
	captureStatus.innerHTML = entries.map(capture => {
		const path = capture.savedPath || "(pending path)";
		return `<span><strong>${escapeHtml(capture.type)}:</strong> ${escapeHtml(capture.status)} ${escapeHtml(path)}</span>`;
	}).join("");
	captureGrid.innerHTML = entries
		.filter(capture => capture.status === "saved" && capture.savedPath)
		.map(renderCapturePreview)
		.join("");
}

function renderCapturePreview(capture) {
	const type = encodeURIComponent(capture.type);
	const src = `${baseUrl()}/capture/latest/${type}?t=${encodeURIComponent(capture.updatedAt || Date.now())}`;
	const updatedAt = capture.updatedAt ? formatRelativeTime(new Date(capture.updatedAt)) : "—";
	return `
		<div class="capture-card">
			<div class="capture-image">
				<img src="${src}" alt="${escapeHtml(capture.type)} capture preview" loading="lazy">
				<div class="capture-overlay">${icon("camera")}</div>
			</div>
			<div class="capture-meta">
				<span class="capture-type">${escapeHtml(capture.type)}</span>
				<span class="capture-time">${updatedAt}</span>
			</div>
		</div>
	`;
}

function filterTable(input) {
	const tableId = input.getAttribute("data-table-filter");
	const table = document.querySelector(`[data-table-id="${CSS.escape(tableId)}"]`);
	if (!table) {
		return;
	}
	const needle = input.value.trim().toLowerCase();
	for (const row of table.tBodies[0]?.rows || []) {
		row.hidden = needle && !row.textContent.toLowerCase().includes(needle);
	}
}

function sortTable(header) {
	const table = header.closest("table");
	const tbody = table?.tBodies[0];
	if (!table || !tbody) {
		return;
	}
	const column = Array.from(header.parentElement.children).indexOf(header);
	const direction = header.dataset.direction === "asc" ? "desc" : "asc";
	for (const cell of header.parentElement.children) {
		delete cell.dataset.direction;
	}
	header.dataset.direction = direction;
	const rows = Array.from(tbody.rows);
	rows.sort((left, right) => {
		const a = sortValue(left.cells[column]?.textContent || "");
		const b = sortValue(right.cells[column]?.textContent || "");
		if (typeof a === "number" && typeof b === "number") {
			return direction === "asc" ? a - b : b - a;
		}
		return direction === "asc" ? String(a).localeCompare(String(b)) : String(b).localeCompare(String(a));
	});
	tbody.append(...rows);
}

function sortValue(value) {
	const trimmed = String(value || "").replace(/,/g, "").trim();
	const numeric = Number(trimmed.match(/-?\d+(\.\d+)?/)?.[0]);
	return Number.isFinite(numeric) ? numeric : trimmed.toLowerCase();
}

function readRememberedPorts() {
	try {
		const raw = window.localStorage.getItem("cvHelperPorts");
		if (!raw) {
			const legacy = sanitizePort(window.localStorage.getItem("cvHelperPort"));
			return legacy ? [legacy] : [];
		}
		const parsed = JSON.parse(raw);
		return Array.isArray(parsed)
			? parsed.map(sanitizePort).filter(Boolean)
			: [];
	} catch (error) {
		return [];
	}
}

function rememberPort(nextPort) {
	const resolvedPort = sanitizePort(nextPort);
	if (!resolvedPort) {
		return;
	}
	const merged = [resolvedPort, ...readRememberedPorts().filter(savedPort => savedPort !== resolvedPort)].slice(0, MAX_REMEMBERED_PORTS);
	window.localStorage.setItem("cvHelperPorts", JSON.stringify(merged));
	window.localStorage.setItem("cvHelperPort", resolvedPort);
}

function sanitizePort(value) {
	const text = String(value || "").trim();
	return /^[0-9]{2,5}$/.test(text) ? text : "";
}

function formatDiscoverySource(source) {
	switch (source) {
		case "preferred-port": return "preferred port";
		case "manual-field": return "manual field";
		case "manual-port": return "manual port";
		case "current-port": return "current port";
		case "query-port": return "query port";
		case "remembered-port": return "remembered port";
		case "known-port": return "known port";
		case "java-listener": return "java listener";
		case "helper-offline": return "helper offline";
		case "browser-fallback": return "browser fallback";
		case "status-report": return "status report";
		default: return source || "unknown";
	}
}

function formatError(error) {
	if (error instanceof Error) {
		return error.message;
	}
	return String(error || "Unknown error");
}

function delay(ms) {
	return new Promise(resolve => window.setTimeout(resolve, ms));
}

function escapeHtml(value) {
	return String(value)
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;")
		.replace(/'/g, "&#039;");
}

function resetTimer() {
	if (timer) {
		window.clearInterval(timer);
		timer = null;
	}

	if (autoRefresh.checked) {
		timer = window.setInterval(refreshAll, 2500);
	}
}

function statCard(label, value, iconName) {
	return `
		<div class="stat-card">
			<span class="label">${iconName ? icon(iconName) : ""}${escapeHtml(label)}</span>
			<span class="value">${typeof value === "string" && (value.includes("<") || value.includes(">")) ? value : escapeHtml(value)}</span>
		</div>
	`;
}

function formatDuration(ms) {
	if (!ms || ms <= 0) {
		return "00:00:00";
	}
	const seconds = Math.floor(ms / 1000) % 60;
	const minutes = Math.floor(ms / 60000) % 60;
	const hours = Math.floor(ms / 3600000);
	return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function formatRelativeTime(date) {
	if (!date) return "—";
	const now = new Date();
	const diffMs = now.getTime() - date.getTime();
	if (diffMs < 0) return "just now";
	const diffSec = Math.floor(diffMs / 1000);
	const diffMin = Math.floor(diffSec / 60);
	const diffHour = Math.floor(diffMin / 60);
	const diffDay = Math.floor(diffHour / 24);

	if (diffSec < 60) return `${diffSec}s ago`;
	if (diffMin < 60) return `${diffMin}m ${diffSec % 60}s ago`;
	if (diffHour < 24) return `${diffHour}h ${diffMin % 60}m ago`;
	return `${diffDay}d ${diffHour % 24}h ago`;
}

function updateFooter(status) {
	const selectedProfile = mobFarmerProfileSelect?.value;
	footerProfile.textContent = selectedProfile || "Default";
	footerMode.textContent = mobFarmerMode?.value === "true" ? "Live" : "Dry";
	footerMemory.textContent = status?.memory ? `${Math.round(status.memory / 1024 / 1024)} MB` : "—";
	footerCpu.textContent = status?.cpu ? `${status.cpu.toFixed(1)}%` : "—";
}

function addActivityEvent(message, iconName = "info") {
	recentActivityLog.unshift({ message, icon: iconName, time: new Date() });
	if (recentActivityLog.length > 20) {
		recentActivityLog.pop();
	}
	renderRecentActivity();
}

function renderRecentActivity() {
	if (!recentActivityLog.length) {
		recentActivity.innerHTML = `<li class="empty">${icon("info")}No activity recorded yet.</li>`;
		return;
	}
	recentActivity.innerHTML = recentActivityLog.map(entry => `
		<li>
			<div class="activity-icon">${icon(entry.icon)}</div>
			<div class="activity-text">${escapeHtml(entry.message)}</div>
			<div class="activity-time">${formatRelativeTime(entry.time)}</div>
		</li>
	`).join("");
}

function statusGroup(group) {
	return `
		<div class="stat-group">
			<h4>${escapeHtml(group.title)}</h4>
			<div class="stat-grid">
				${group.items.map(([label, value]) => statCard(label, value)).join("")}
			</div>
		</div>
	`;
}

function formatBoosted(skill) {
	if (!skill || skill.boosted === undefined || skill.real === undefined) {
		return "Unknown";
	}
	return `${skill.boosted}/${skill.real}`;
}

function formatPercent(value, rawFallback) {
	if (typeof value === "number") {
		const normalized = value > 100 ? value / 100 : value;
		return `${Math.round(normalized)}%`;
	}
	if (typeof rawFallback === "number") {
		return formatRunEnergy(rawFallback);
	}
	return "Unknown";
}

function formatRunEnergy(value) {
	if (typeof value !== "number") {
		return "Unknown";
	}
	return `${(value / 100).toFixed(0)}%`;
}

function formatGp(value) {
	if (typeof value !== "number") {
		return "Unknown";
	}
	const absValue = Math.abs(value);
	let formatted;
	let colorClass = "";

	if (absValue >= 1_000_000_000) {
		formatted = `${(value / 1_000_000_000).toFixed(1)}b`;
		colorClass = "gp-gold";
	} else if (absValue >= 100_000_000) {
		formatted = `${(value / 1_000_000).toFixed(0)}m`;
		colorClass = "gp-purple";
	} else if (absValue >= 1_000_000) {
		formatted = `${(value / 1_000_000).toFixed(1)}m`;
		colorClass = "gp-green";
	} else if (absValue >= 1_000) {
		formatted = `${(value / 1_000).toFixed(0)}k`;
		colorClass = "gp-yellow";
	} else {
		formatted = `${Math.round(value)}`;
		colorClass = "gp-white";
	}

	return colorClass ? `<span class="gp-value ${colorClass}">${formatted}</span>` : formatted;
}

function selectedValue(value, fallback) {
	return value === undefined || value === null ? fallback : String(value);
}

function formatRect(rect) {
	return `${escapeHtml(rect.x ?? "?")}, ${escapeHtml(rect.y ?? "?")} / ${escapeHtml(rect.width ?? "?")}x${escapeHtml(rect.height ?? "?")}`;
}

function formatPoint(point) {
	if (!point) {
		return "Unavailable";
	}
	if (point.plane !== undefined) {
		return `${point.x}, ${point.y}, ${point.plane}`;
	}
	if (point.x !== undefined && point.y !== undefined) {
		return `${point.x}, ${point.y}`;
	}
	return point.value || "Unavailable";
}

function flattenMobFarmerPayload(payload, indent = 0) {
	if (payload === null || payload === undefined) {
		return [];
	}
	if (typeof payload === "string" || typeof payload === "number" || typeof payload === "boolean") {
		return [`${" ".repeat(indent)}${payload}`];
	}
	if (Array.isArray(payload)) {
		return payload.flatMap(item => flattenMobFarmerPayload(item, indent));
	}
	return Object.entries(payload).flatMap(([key, value]) => {
		if (value && typeof value === "object") {
			const nested = flattenMobFarmerPayload(value, indent + 2);
			return [`${" ".repeat(indent)}${key}:`, ...nested];
		}
		return [`${" ".repeat(indent)}${key}: ${value}`];
	});
}

function structuredRows(payload, prefix = "") {
	if (payload === null || payload === undefined) {
		return [];
	}
	if (typeof payload === "string" || typeof payload === "number" || typeof payload === "boolean") {
		return [[prefix || "value", String(payload)]];
	}
	if (Array.isArray(payload)) {
		return payload.slice(0, 12).map((item, index) => [`${prefix || "item"} ${index + 1}`, compactValue(item)]);
	}
	return Object.entries(payload).flatMap(([key, value]) => {
		const label = prefix ? `${prefix}.${key}` : key;
		if (value && typeof value === "object" && !Array.isArray(value)) {
			const nested = structuredRows(value, label);
			return nested.length ? nested.slice(0, 16) : [[label, ""]];
		}
		return [[label, compactValue(value)]];
	}).slice(0, 28);
}

function compactValue(value) {
	if (value === null || value === undefined) {
		return "";
	}
	if (Array.isArray(value)) {
		return value.map(compactValue).join(", ");
	}
	if (typeof value === "object") {
		const label = value.name || value.label || value.decision || value.status || value.target || value.result;
		return label ? String(label) : JSON.stringify(value);
	}
	return String(value);
}

function formatMobFarmerCandidate(candidate) {
	if (candidate === null || candidate === undefined) {
		return "Unavailable";
	}
	if (typeof candidate === "string") {
		return candidate;
	}
	const name = candidate.name || candidate.label || candidate.id || "candidate";
	const reason = Array.isArray(candidate.reasons) && candidate.reasons.length ? ` - ${candidate.reasons.join(", ")}` : "";
	const score = candidate.score !== undefined ? ` (score ${candidate.score})` : "";
	const distance = candidate.distance !== undefined ? ` @ ${candidate.distance}` : "";
	return `${name}${distance}${score}${reason}`;
}

function targetWarnings(surface, target) {
	const warnings = [];
	const bounds = target.bounds || {};
	const width = bounds.width || 0;
	const height = bounds.height || 0;
	const label = target.label || target.name || "";

	if ((surface === "inventory" || surface === "equipment") && (width > 70 || height > 70)) {
		warnings.push(`${label || "target"} is larger than a normal slot (${width}x${height}).`);
	}

	if (surface === "equipment" && label === "equipment slot -1") {
		warnings.push("equipment target has fallback label `equipment slot -1`; semantic naming needs cleanup.");
	}

	return warnings;
}

function metricCard(iconName, label, value, tone = "") {
	return `
		<div class="reference-metric">
			<span class="metric-icon">${icon(iconName || "circle")}</span>
			<span>
				<span class="metric-label">${escapeHtml(label)}</span>
				<span class="metric-value ${escapeHtml(tone)}">${typeof value === "string" && value.includes("<span class=\"gp-value") ? value : escapeHtml(value)}</span>
			</span>
		</div>
	`;
}

function renderRuntimeTableBlock(title, payload, summary = "") {
	const rows = structuredRows(payload);
	return `
		<div class="detail-block">
			<header>
				<h4>${escapeHtml(title)}</h4>
				<span class="summary">${escapeHtml(summary || rows[0]?.[1] || "No data")}</span>
			</header>
			${rows.length ? `
				<div class="table-wrap compact">
					<table>
						<thead><tr><th>Field</th><th>Value</th></tr></thead>
						<tbody>${rows.map(([key, value]) => `<tr><td>${escapeHtml(key)}</td><td>${escapeHtml(value)}</td></tr>`).join("")}</tbody>
					</table>
				</div>
			` : `<p class="empty compact">No data.</p>`}
		</div>
	`;
}

function compactDecisionTone(value) {
	const text = String(value || "").toLowerCase();
	if (text.includes("ok") || text.includes("select") || text.includes("reachable") || text.includes("interacting")) return "good";
	if (text.includes("fail") || text.includes("blocked") || text.includes("unreachable") || text.includes("error")) return "bad";
	if (text.includes("skip") || text.includes("wait") || text.includes("unknown")) return "warn";
	return "";
}

discoverPort().then(() => refreshAll());
resetTimer();



