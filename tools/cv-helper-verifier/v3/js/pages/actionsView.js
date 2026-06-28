/* Action-slot command board: grouped key rows with one focused editor. */
import { panel, badge } from "../components.js";
import { icon, itemIcon, refreshIcons } from "../icons.js";
import { escapeHtml } from "../format.js";
import { request } from "../api.js";

const root = () => document.querySelector("#actions-page");
const arr = (v) => (Array.isArray(v) ? v : []);
const obj = (v) => (v && typeof v === "object" ? v : {});
const GROUPS = [
	{ label: "Numbers", slots: [1, 2, 3, 4, 5] },
	{ label: "Top row", slots: [6, 7, 8, 9, 10] },
	{ label: "Home row", slots: [11, 12, 13, 14, 15] },
	{ label: "Bottom row", slots: [16, 17, 18, 19] },
	{ label: "Special", slots: [20, 21, 22] },
];
const SURFACE_ICON = {
	PRAYER: "shield", SPELL: "wand-2", INVENTORY: "backpack", EQUIPMENT: "shirt",
	PANELS: "panel-right", COMBAT: "swords", MINIMAP: "compass", NEAREST_ENTITY: "crosshair", DISABLED: "ban",
};
let selectedSlot = 1;
let latestConfig = null;
let latestTargets = {};

function pretty(v) {
	return String(v ?? "—").replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}

function normalized(v) { return String(v || "").toLowerCase().replace(/[^a-z0-9]/g, ""); }

function targetsFor(surface) {
	const key = String(surface || "").toLowerCase().replace("nearest_entity", "entities");
	const payload = obj(latestTargets[key]);
	return arr(payload.targets || payload.entities);
}

function targetMeta(slot) {
	const candidates = targetsFor(slot.surface);
	const needles = String(slot.target || "").split(/\s*(?:->|\||,|;|\r?\n)\s*/).filter(Boolean).map(normalized);
	const found = candidates.find((t) => {
		const hay = normalized([t.label, t.name, t.text, t.itemName, t.actions].filter(Boolean).join(" "));
		return needles.some((n) => n && hay.includes(n));
	});
	return found || null;
}

function targetVisual(slot) {
	const meta = targetMeta(slot);
	const itemId = Number(meta?.itemId || 0);
	const title = [
		`Target: ${slot.target || "not configured"}`,
		`Surface: ${pretty(slot.surface)}`,
		meta?.widgetId != null ? `Widget: ${meta.widgetId}` : "",
		meta?.itemId != null ? `Item: ${meta.itemId}` : "",
		meta?.spriteId != null ? `Sprite: ${meta.spriteId}` : "",
		meta ? "Icon source: live target metadata" : "Icon source: surface fallback (target metadata unresolved)",
	].filter(Boolean).join("\n");
	const visual = itemId > 0 ? itemIcon(itemId, meta?.name || slot.target, "action-target-img") : icon(SURFACE_ICON[slot.surface] || "circle-help");
	return `<span class="action-target-icon" title="${escapeHtml(title)}">${visual}</span>`;
}

function keycap(slot) {
	const fallback = ["", "1", "2", "3", "4", "5", "Q", "W", "E", "R", "T", "A", "S", "D", "F", "G", "Z", "X", "C", "V", "`", "Caps", "Tab"];
	return slot.hotkey && slot.hotkey !== "NOT_SET" ? slot.hotkey.replace("KeyCode: ", "") : fallback[slot.slot] || `#${slot.slot}`;
}

function slotCard(slot) {
	const configured = slot.enabled && slot.surface !== "DISABLED" && Boolean(slot.target);
	const last = obj(slot.lastRun);
	return `<button class="action-slot-card ${slot.enabled ? "enabled" : "disabled"} ${configured ? "configured" : ""} ${selectedSlot === slot.slot ? "selected" : ""}" data-action-select="${slot.slot}">
		<span class="action-keycap">${escapeHtml(keycap(slot))}</span>
		${targetVisual(slot)}
		<span class="action-slot-copy"><strong>${escapeHtml(slot.target || pretty(slot.surface) || "Disabled")}</strong><small>${escapeHtml(pretty(slot.surface))}</small></span>
		<span class="action-slot-state">${last.result ? escapeHtml(pretty(last.result)) : slot.enabled ? "Ready" : "Off"}</span>
	</button>`;
}

function summary(slots) {
	const enabled = slots.filter((s) => s.enabled).length;
	const configured = slots.filter((s) => s.enabled && s.surface !== "DISABLED" && s.target).length;
	const failed = slots.filter((s) => /fail|skip/.test(String(s.lastRun?.result || ""))).length;
	const surfaces = new Set(slots.filter((s) => s.enabled && s.surface !== "DISABLED").map((s) => s.surface)).size;
	return `<div class="action-summary">
		<div><strong>${enabled}</strong><span>Enabled</span></div><div><strong>${configured}</strong><span>Configured</span></div>
		<div><strong>${slots.length - enabled}</strong><span>Disabled</span></div><div><strong>${failed}</strong><span>Needs attention</span></div>
		<div><strong>${surfaces}</strong><span>Surface mix</span></div>
	</div>`;
}

function option(value, current) { return `<option value="${escapeHtml(value)}"${value === current ? " selected" : ""}>${escapeHtml(pretty(value))}</option>`; }

function editor(slot) {
	if (!slot) return `<div class="empty">Select an action slot.</div>`;
	const last = obj(slot.lastRun);
	return `<div class="action-editor" data-action-editor="${slot.slot}">
		<div class="action-editor-head">${targetVisual(slot)}<div><span class="eyebrow">Slot ${slot.slot}</span><h3>${escapeHtml(keycap(slot))} · ${escapeHtml(slot.target || "Unconfigured")}</h3></div>${slot.enabled ? badge("Enabled", "good") : badge("Disabled", "")}</div>
		<div class="action-form-grid">
			<label><span>Enabled</span><input data-action-field="enabled" type="checkbox"${slot.enabled ? " checked" : ""}></label>
			<label><span>Hotkey</span><input data-action-field="hotkey" value="${escapeHtml(slot.hotkey || "NOT_SET")}"></label>
			<label><span>Surface</span><select data-action-field="surface">${Object.keys(SURFACE_ICON).map((v) => option(v, slot.surface)).join("")}</select></label>
			<label class="wide"><span>Target / fallback list / sequence</span><input data-action-field="target" value="${escapeHtml(slot.target || "")}" placeholder="Bind | Ice Barrage or food -> brew"></label>
			<label><span>Click-after</span><select data-action-field="clickAfterMode">${["AUTO", "ALWAYS", "NEVER"].map((v) => option(v, slot.clickAfterMode)).join("")}</select></label>
			<label><span>Invocation</span><select data-action-field="invocationMode">${["AUTO", "WIDGET", "CLICK"].map((v) => option(v, slot.invocationMode)).join("")}</select></label>
			<label><span>Prayer guard</span><select data-action-field="prayerMode">${["TOGGLE", "ON_ONLY", "OFF_ONLY"].map((v) => option(v, slot.prayerMode)).join("")}</select></label>
			<label><span>Spell guard</span><select data-action-field="spellAvailabilityMode">${["REQUIRE_AVAILABLE", "ALLOW_ATTEMPT"].map((v) => option(v, slot.spellAvailabilityMode)).join("")}</select></label>
			<label><span>Return panel</span><input data-action-field="returnPanel" type="checkbox"${slot.returnPanel ? " checked" : ""}></label>
			<label><span>Restore mouse</span><input data-action-field="returnMouseCenter" type="checkbox"${slot.returnMouseCenter ? " checked" : ""}></label>
		</div>
		<div class="action-editor-actions"><button class="btn primary" data-action-save="${slot.slot}">${icon("save")} Apply slot</button><button class="btn" data-action-run="${slot.slot}">${icon("play")} Run now</button><button class="btn ghost" data-action-reset="${slot.slot}">${icon("rotate-ccw")} Reset sequence (${slot.sequenceIndex || 0})</button></div>
		<div class="action-last-run"><strong>Last result</strong><span>${escapeHtml(last.result ? pretty(last.result) : "No run recorded")}</span><small>${escapeHtml(last.requestedAt || "")}</small></div>
		<details><summary>Raw slot payload</summary><pre class="raw-json">${escapeHtml(JSON.stringify(slot, null, 2))}</pre></details>
	</div>`;
}

function mount() {
	const el = root();
	if (!el || el.dataset.mounted === "1") return;
	el.dataset.mounted = "1";
	el.addEventListener("click", async (event) => {
		const select = event.target.closest("[data-action-select]");
		if (select) { selectedSlot = Number(select.dataset.actionSelect); renderActionsView(latestConfig, latestTargets); return; }
		const run = event.target.closest("[data-action-run]");
		const reset = event.target.closest("[data-action-reset]");
		const save = event.target.closest("[data-action-save]");
		try {
			if (run) await request(`/automation/actions/run?slot=${run.dataset.actionRun}`, { method: "POST" });
			if (reset) await request(`/automation/actions/reset-sequence?slot=${reset.dataset.actionReset}`, { method: "POST" });
			if (save) {
				const box = el.querySelector("[data-action-editor]");
				const slot = { slot: Number(save.dataset.actionSave) };
				box.querySelectorAll("[data-action-field]").forEach((input) => { slot[input.dataset.actionField] = input.type === "checkbox" ? input.checked : input.value; });
				const response = await request("/automation/mob-farmer/config", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ actionSlots: [slot] }) });
				latestConfig = response.config || latestConfig;
			}
			latestConfig = await request("/automation/mob-farmer/config");
			renderActionsView(latestConfig, latestTargets);
		} catch (error) {
			const result = el.querySelector(".action-last-run span"); if (result) result.textContent = `Failed: ${error.message}`;
		}
	});
}

export function renderActionsView(config, targetPayloads = {}) {
	mount(); latestConfig = config; latestTargets = targetPayloads;
	const slots = arr(config?.actionSlots);
	if (!slots.length) { root().innerHTML = `<p class="scaffold-note">${config?.unavailable ? escapeHtml(config.error || "Actions unavailable") : "Connecting…"}</p>`; return; }
	if (!slots.some((s) => s.slot === selectedSlot)) selectedSlot = slots[0].slot;
	const board = GROUPS.map((group) => `<section class="action-key-row"><header><span>${escapeHtml(group.label)}</span><small>${group.slots.length} slots</small></header><div>${group.slots.map((n) => slots.find((s) => s.slot === n)).filter(Boolean).map(slotCard).join("")}</div></section>`).join("");
	root().innerHTML = `${summary(slots)}<div class="actions-layout"><div class="actions-board">${board}</div>${panel({ title: "Selected action", iconName: "sliders-horizontal", body: editor(slots.find((s) => s.slot === selectedSlot)) })}</div>`;
	refreshIcons();
}
