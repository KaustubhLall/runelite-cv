/* ============================================================================
 * pages/actionsView.js — configured action-hotkey slots.
 *
 * Reuses the existing actionSlots array already returned by
 * GET /automation/mob-farmer/config (no new backend behavior — this just
 * surfaces it as a table instead of raw JSON). Manual run/test and per-slot
 * last-result are not currently tracked by the backend, so they're shown as
 * "Pending" rather than invented.
 * ========================================================================== */
import { panel, table, badge } from "../components.js";
import { icon, refreshIcons } from "../icons.js";
import { escapeHtml, selectedValue } from "../format.js";

const root = () => document.querySelector("#actions-page");
const arr = (v) => (Array.isArray(v) ? v : []);
function prettyEnum(v) { return String(v ?? "—").replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()); }

function mount() {
	const el = root();
	if (!el || el.dataset.mounted === "1") return;
	el.dataset.mounted = "1";
	el.innerHTML = panel({
		title: "Action Slots", iconName: "swords", flush: true,
		extra: `<span id="actions-count"></span>`,
		body: `<div id="actions-table"></div>`,
	});
	refreshIcons();
}

function pending() { return `<span class="muted" title="Not tracked by the current backend">Pending</span>`; }

function slotRows(slots) {
	return slots.map((s) => ({
		cls: s.enabled ? "row-good" : "",
		cells: [
			`#${escapeHtml(String(s.slot))}`,
			s.enabled ? `<span class="cell-yes">on</span>` : `<span class="cell-skip">off</span>`,
			`<span class="muted">${escapeHtml(s.hotkey === "NOT_SET" ? "—" : s.hotkey || "—")}</span>`,
			escapeHtml(prettyEnum(s.surface)),
			`<span class="muted">${escapeHtml(s.target || "—")}</span>`,
			escapeHtml(prettyEnum(s.clickAfterMode)),
			escapeHtml(prettyEnum(s.invocationMode)),
			escapeHtml(prettyEnum(s.prayerMode)),
			escapeHtml(prettyEnum(s.spellAvailabilityMode)),
			s.returnPanel ? `<span class="cell-yes">yes</span>` : `<span class="cell-skip">no</span>`,
			s.returnMouseCenter ? `<span class="cell-yes">yes</span>` : `<span class="cell-skip">no</span>`,
			pending(),
			pending(),
		],
	}));
}

export function renderActionsView(mobFarmerConfig) {
	mount();
	const slots = arr(mobFarmerConfig?.actionSlots);
	const countEl = document.querySelector("#actions-count");
	if (countEl) countEl.innerHTML = badge(`${slots.length} slot${slots.length === 1 ? "" : "s"}`, slots.some((s) => s.enabled) ? "good" : "");
	const tableHtml = table({
		columns: [
			{ label: "Slot" }, { label: "Enabled" }, { label: "Hotkey" }, { label: "Surface" }, { label: "Target" },
			{ label: "Click-after" }, { label: "Invocation" }, { label: "Prayer" }, { label: "Spell guard" },
			{ label: "Return panel" }, { label: "Restore mouse" }, { label: "Manual run" }, { label: "Last result" },
		],
		rows: slotRows(slots),
		empty: mobFarmerConfig ? "No action slots configured." : "Connecting…",
	});
	const wrap = document.querySelector("#actions-table");
	if (wrap && wrap.__html !== tableHtml) { wrap.__html = tableHtml; wrap.innerHTML = tableHtml; refreshIcons(); }
}
