/* Frontend-only GP tier preferences. Kept separate from plugin automation config. */
const STORAGE_KEY = "cvHelperGpSettingsV1";

export const DEFAULT_GP_SETTINGS = Object.freeze({
	enabled: true,
	thresholds: Object.freeze({ common: 1_000, valuable: 100_000, wealthy: 1_000_000, elite: 100_000_000, legendary: 1_000_000_000 }),
	colors: Object.freeze({ trivial: "#6f7a64", common: "#d7ddc8", valuable: "#e0c84e", wealthy: "#74c66f", elite: "#b98ce0", legendary: "#f0a83e" }),
});

const TIERS = ["trivial", "common", "valuable", "wealthy", "elite", "legendary"];
const THRESHOLD_TIERS = TIERS.slice(1);
let current = null;

const escapeHtml = (value) => String(value ?? "")
	.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
	.replace(/"/g, "&quot;").replace(/'/g, "&#039;");

function cloneDefaults() {
	return { enabled: DEFAULT_GP_SETTINGS.enabled, thresholds: { ...DEFAULT_GP_SETTINGS.thresholds }, colors: { ...DEFAULT_GP_SETTINGS.colors } };
}

function normalized(candidate) {
	const out = cloneDefaults();
	if (!candidate || typeof candidate !== "object") return out;
	out.enabled = candidate.enabled !== false;
	const thresholds = candidate.thresholds || {};
	let previous = -1;
	for (const tier of THRESHOLD_TIERS) {
		const value = Number(thresholds[tier]);
		if (!Number.isFinite(value) || value < 0 || value <= previous) return cloneDefaults();
		out.thresholds[tier] = Math.round(value);
		previous = value;
	}
	const colors = candidate.colors || {};
	for (const tier of TIERS) {
		if (/^#[0-9a-f]{6}$/i.test(String(colors[tier] || ""))) out.colors[tier] = String(colors[tier]);
	}
	return out;
}

export function getGpSettings() {
	if (current) return current;
	try { current = normalized(JSON.parse(localStorage.getItem(STORAGE_KEY) || "null")); }
	catch { current = cloneDefaults(); }
	return current;
}

export function getGpTier(value, settings = getGpSettings()) {
	if (typeof value !== "number" || !Number.isFinite(value)) return "unknown";
	if (!settings.enabled) return "neutral";
	const amount = Math.abs(value);
	if (amount >= settings.thresholds.legendary) return "legendary";
	if (amount >= settings.thresholds.elite) return "elite";
	if (amount >= settings.thresholds.wealthy) return "wealthy";
	if (amount >= settings.thresholds.valuable) return "valuable";
	if (amount >= settings.thresholds.common) return "common";
	return "trivial";
}

export function applyGpSettings(settings = getGpSettings()) {
	const root = document.documentElement;
	for (const tier of TIERS) root.style.setProperty(`--gp-${tier}`, settings.colors[tier]);
}

function save(settings) {
	current = normalized(settings);
	localStorage.setItem(STORAGE_KEY, JSON.stringify(current));
	applyGpSettings(current);
	window.dispatchEvent(new CustomEvent("cvhelper:gp-settings-changed"));
}

function renderForm(root, message = "Saved in this browser") {
	const settings = getGpSettings();
	const fields = THRESHOLD_TIERS.map((tier) => `<label class="gp-setting-field"><span>${escapeHtml(tier)} starts at</span><input class="input" type="number" min="0" step="1" data-gp-threshold="${tier}" value="${settings.thresholds[tier]}"></label>`).join("");
	const colors = TIERS.map((tier) => `<label class="gp-color-field"><span class="gp-tier-${tier}">${escapeHtml(tier)}</span><input type="color" data-gp-color="${tier}" value="${settings.colors[tier]}"></label>`).join("");
	root.innerHTML = `<section class="panel gp-settings-panel">
		<div class="panel-head"><h3>GP Value Styling</h3><span class="ph-extra" id="gp-settings-state">${escapeHtml(message)}</span></div>
		<div class="panel-body">
			<div class="gp-settings-head"><label class="check"><input type="checkbox" data-gp-enabled ${settings.enabled ? "checked" : ""}> Enable tiered GP coloring</label><span class="muted">Frontend-only; applies to GE, HA, totals, loot, and thresholds.</span></div>
			<div class="gp-threshold-grid">${fields}</div>
			<div class="gp-color-grid">${colors}</div>
			<div class="gp-preview">${[0, 1_000, 100_000, 1_000_000, 100_000_000, 1_000_000_000].map((value, i) => `<span class="gp-value gp-tier-${TIERS[i]}">${value.toLocaleString("en-US")} GP</span>`).join("")}</div>
			<div class="gp-settings-actions"><button class="btn" type="button" data-gp-action="reset">Reset defaults</button><button class="btn gold" type="button" data-gp-action="apply">Apply GP styling</button></div>
		</div>
	</section>`;
}

export function mountGpSettings() {
	const root = document.querySelector("#gp-style-config");
	if (!root) return;
	applyGpSettings();
	renderForm(root);
	root.addEventListener("click", (event) => {
		const action = event.target.closest("[data-gp-action]")?.dataset.gpAction;
		if (!action) return;
		if (action === "reset") {
			current = cloneDefaults();
			localStorage.removeItem(STORAGE_KEY);
			applyGpSettings(current);
			renderForm(root, "Defaults restored");
			window.dispatchEvent(new CustomEvent("cvhelper:gp-settings-changed"));
			return;
		}
		const thresholds = {};
		root.querySelectorAll("[data-gp-threshold]").forEach((input) => { thresholds[input.dataset.gpThreshold] = Number(input.value); });
		const colors = {};
		root.querySelectorAll("[data-gp-color]").forEach((input) => { colors[input.dataset.gpColor] = input.value; });
		const draft = { enabled: root.querySelector("[data-gp-enabled]").checked, thresholds, colors };
		const checked = normalized(draft);
		const invalid = THRESHOLD_TIERS.some((tier) => checked.thresholds[tier] !== Math.round(Number(thresholds[tier])));
		if (invalid) {
			const state = root.querySelector("#gp-settings-state");
			if (state) state.textContent = "Thresholds must be ascending non-negative numbers";
			return;
		}
		save(draft);
		renderForm(root, "Applied and saved locally");
	});
}
