const surfaces = ["prayer", "spell", "minimap", "inventory", "equipment", "panels", "combat"];

const form = document.querySelector("#connection-form");
const portInput = document.querySelector("#port");
const connectionStatus = document.querySelector("#connection-status");
const statusGrid = document.querySelector("#status-grid");
const countGrid = document.querySelector("#count-grid");
const warningsList = document.querySelector("#warnings");
const entitiesRoot = document.querySelector("#entities");
const surfacesRoot = document.querySelector("#surfaces");
const refreshNow = document.querySelector("#refresh-now");
const autoRefresh = document.querySelector("#auto-refresh");
const captureStatus = document.querySelector("#capture-status");
const captureGrid = document.querySelector("#capture-grid");

let port = portInput.value.trim();
let timer = null;

const queryPort = new URLSearchParams(window.location.search).get("port");
const savedPort = window.localStorage.getItem("cvHelperPort");
if (queryPort || savedPort) {
	port = queryPort || savedPort;
	portInput.value = port;
}

form.addEventListener("submit", event => {
	event.preventDefault();
	port = portInput.value.trim();
	window.localStorage.setItem("cvHelperPort", port);
	refreshAll();
	resetTimer();
});

refreshNow.addEventListener("click", refreshAll);

document.querySelectorAll("[data-capture]").forEach(button => {
	button.addEventListener("click", async () => {
		const path = button.getAttribute("data-capture");
		const result = await request(path, { method: "POST" });
		captureStatus.textContent = `${result.capture || "client-frame"} capture queued. Refreshing saved path...`;
		await delay(900);
		await refreshAll();
	});
});

autoRefresh.addEventListener("change", resetTimer);

function baseUrl() {
	return `http://127.0.0.1:${port}`;
}

async function request(path, options = {}) {
	const response = await fetch(`${baseUrl()}${path}`, options);
	if (!response.ok) {
		throw new Error(`${path} returned ${response.status}`);
	}
	return response.json();
}

async function refreshAll() {
	if (!port) {
		setConnection("Enter a port", false);
		return;
	}

	try {
		const [status, ...targetPayloads] = await Promise.all([
			request("/status"),
			request("/entities"),
			request("/entities/nearest"),
			...surfaces.map(surface => request(`/targets/${surface}`)),
		]);
		const entitiesPayload = targetPayloads.shift();
		const nearestEntityPayload = targetPayloads.shift();

		setConnection(`Connected to ${baseUrl()}`, true);
		renderStatus(status);
		renderCounts(status, targetPayloads, entitiesPayload);
		renderEntities(entitiesPayload, nearestEntityPayload);
		renderSurfaces(targetPayloads);
		renderWarnings(status, targetPayloads);
	} catch (error) {
		setConnection(error.message, false);
	}
}

function setConnection(message, ok) {
	connectionStatus.textContent = message;
	connectionStatus.classList.toggle("ok", ok);
	connectionStatus.classList.toggle("bad", !ok);
}

function renderStatus(status) {
	const player = status.player || {};
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

	const activePrayerNames = Array.isArray(vitals.activePrayers)
		? vitals.activePrayers
		: Object.entries(prayers).filter(([, state]) => state?.active).map(([name]) => name);

	const groups = [
		{
			title: "Connection",
			items: [
				["API port", status.port || "Unknown"],
				["Status", status.status || "Unknown"],
				["Game state", player.gameState || "Unknown"],
				["Logged in", String(Boolean(player.loggedIn))],
				["Player", player.localPlayer || "Unavailable"],
				["World", player.world || "Unknown"],
			],
		},
		{
			title: "Vitals",
			items: [
				["HP", formatBoosted(hitpoints)],
				["Prayer", formatBoosted(prayer)],
				["Prayer active", String(Boolean(vitals.prayerActive || activePrayerNames.length))],
				["Active prayers", activePrayerNames.length ? activePrayerNames.join(", ") : "None"],
				["Run energy", formatPercent(vitals.runEnergyPercent, player.runEnergy)],
				["Spec energy", formatPercent(vitals.specialAttackPercent)],
				["Spec enabled", selectedValue(vitals.specialAttackEnabled, "Unknown")],
				["Weight", vitals.weight ?? player.weight ?? "Unknown"],
			],
		},
		{
			title: "Wealth",
			items: [
				["Current loot GE", formatGp(wealth.currentLootValueGe ?? inventory.gePrice)],
				["Current loot HA", formatGp(wealth.currentLootValueHa ?? inventory.haPrice)],
				["Equipment GE", formatGp(equipment.gePrice)],
				["Total carried GE", formatGp(wealth.totalCarriedValueGe)],
				["Risked GE approx", formatGp(wealth.riskedValueGeApprox)],
				["Risk model", wealth.riskModel || "Unknown"],
			],
		},
		{
			title: "Interface",
			items: [
				["Open panel", interfaces.activeSidePanel || "Unknown"],
				["Spellbook", spellbook.name || "Unknown"],
				["Mouse", formatPoint(player.mouseCanvasPosition)],
				["World location", formatPoint(player.worldLocation)],
				["Local location", formatPoint(player.localLocation)],
				["Self bounds", formatRect(player.selfBounds || {})],
				["Selected widget", selectedWidget.selected ? `${selectedWidget.widgetId || "?"} ${selectedWidget.name || selectedWidget.text || ""}` : "None"],
				["Latest capture", latestCapture.savedPath || latestCapture.status || "None"],
			],
		},
	];

	renderCaptureStatus(captures);
	statusGrid.innerHTML = groups.map(statusGroup).join("");
}

function renderCounts(status, targetPayloads, entitiesPayload) {
	const payloadCounts = Object.fromEntries(targetPayloads.map(payload => [payload.surface, payload.count]));
	const counts = surfaces.map(surface => [
		surface,
		payloadCounts[surface] ?? status[`${surface}Targets`] ?? 0,
	]);
	counts.push(["entities", entitiesPayload?.count ?? status.entities ?? 0]);

	countGrid.innerHTML = counts.map(([surface, count]) => `
		<div class="count">
			<span>${escapeHtml(surface)}</span>
			<strong>${count}</strong>
		</div>
	`).join("");
}

function renderSurfaces(targetPayloads) {
	surfacesRoot.innerHTML = targetPayloads.map(renderSurface).join("");
}

function renderEntities(payload, nearestPayload) {
	const rows = payload?.entities || [];
	const nearest = nearestPayload?.entity || null;
	const body = rows
		.slice()
		.sort((a, b) => (a.distance ?? 9999) - (b.distance ?? 9999))
		.map(renderEntityRow)
		.join("");
	entitiesRoot.innerHTML = `
		<article class="surface entity-surface">
			<header>
				<div class="surface-title">
					<h2>nearby entities</h2>
					<span class="badge">${payload?.count || 0}</span>
				</div>
				<small>${escapeHtml(payload?.gameState || "")}</small>
			</header>
			${nearest ? `
				<div class="entity-nearest">
					<strong>Nearest click target:</strong>
					<span>${escapeHtml(nearest.type || "")} ${escapeHtml(nearest.name || "(unnamed)")}</span>
					<span>distance ${escapeHtml(nearest.distance ?? "")}</span>
					<span>click ${escapeHtml(formatPoint(nearest.clickPoint))}</span>
				</div>
			` : ""}
			${rows.length ? `
				<div class="table-wrap">
					<table>
						<thead>
							<tr>
								<th>Name</th>
								<th>Type</th>
								<th>ID</th>
								<th>Distance</th>
								<th>World</th>
								<th>Bounds</th>
								<th>Click</th>
							</tr>
						</thead>
						<tbody>${body}</tbody>
					</table>
				</div>
			` : `<p class="empty">No nearby players/NPCs exported yet. Log in and refresh after the scene is visible.</p>`}
		</article>
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
	const rows = payload.targets || [];
	const body = rows.map(target => renderTargetRow(payload.surface, target)).join("");
	const empty = rows.length === 0
		? `<p class="empty">No targets. If this surface depends on a RuneLite tab, leave that tab visible and refresh.</p>`
		: "";

	return `
		<article class="surface">
			<header>
				<div class="surface-title">
					<h2>${escapeHtml(payload.surface || "unknown")}</h2>
					<span class="badge">${payload.count || 0}</span>
				</div>
				<small>${escapeHtml(payload.fresh === false ? `cached from ${payload.lastSeenAt || "unknown"}` : payload.gameState || "")}</small>
			</header>
			${empty}
			${rows.length ? `
				<div class="table-wrap">
					<table>
						<thead>
							<tr>
								<th>Label</th>
								<th>Widget</th>
								<th>Item</th>
								<th>Bounds</th>
								<th>Center</th>
								<th>Actions</th>
							</tr>
						</thead>
						<tbody>${body}</tbody>
					</table>
				</div>
			` : ""}
		</article>
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

function renderWarnings(status, targetPayloads) {
	const warnings = [];
	const player = status.player || {};

	if (!player.loggedIn) {
		warnings.push("RuneLite is not logged in; empty target surfaces are expected.");
	}

	for (const payload of targetPayloads) {
		if ((payload.count || 0) === 0) {
			warnings.push(`${payload.surface} has zero targets. Verify the corresponding tab/interface is visible.`);
		}
		for (const target of payload.targets || []) {
			for (const warning of targetWarnings(payload.surface, target)) {
				warnings.push(`${payload.surface}: ${warning}`);
			}
		}
	}

	warningsList.innerHTML = warnings.length
		? warnings.map(warning => `<li>${escapeHtml(warning)}</li>`).join("")
		: "<li>No verifier warnings right now.</li>";
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

function statCard(label, value) {
	return `
		<div class="stat">
			<span>${escapeHtml(label)}</span>
			<strong>${escapeHtml(value)}</strong>
		</div>
	`;
}

function statusGroup(group) {
	return `
		<article class="status-group">
			<h3>${escapeHtml(group.title)}</h3>
			<div class="stat-grid status-group-grid">
				${group.items.map(([label, value]) => statCard(label, value)).join("")}
			</div>
		</article>
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
	return `${Math.round(value).toLocaleString()} gp`;
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
	return `
		<article class="capture-preview">
			<header>
				<h2>${escapeHtml(capture.type)} capture</h2>
				<small>${escapeHtml(capture.updatedAt || "")}</small>
			</header>
			<img src="${src}" alt="${escapeHtml(capture.type)} capture preview">
			<p>${escapeHtml(capture.savedPath || "")}</p>
		</article>
	`;
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

refreshAll();
resetTimer();
