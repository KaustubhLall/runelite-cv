/* ============================================================================
 * pages/misc.js — Raw Data, Debug, Inventory pages.
 * ========================================================================== */
import { panel, kvList, table, badge, gpValue } from "../components.js";
import { escapeHtml, selectedValue } from "../format.js";

const obj = (v) => (v && typeof v === "object" ? v : {});
const num = (v) => (typeof v === "number" ? v : undefined);

/* ---- Raw Data ----------------------------------------------------------- */
export function renderRaw(payloads) {
	const el = document.querySelector("#raw-page");
	if (!el) return;
	const blocks = [
		["status", payloads.status],
		["mob-farmer", payloads.mobFarmer],
		["mining", payloads.mining],
		["woodcutting", payloads.woodcutting],
	];
	el.innerHTML = `<div class="grid cols-2">${blocks.map(([name, data]) => panel({
		title: name, iconName: "scroll-text", flush: true,
		body: `<details${name === "status" ? " open" : ""}><summary class="raw-summary">${escapeHtml(name)} payload</summary><pre class="raw-json">${escapeHtml(JSON.stringify(data ?? {}, null, 2))}</pre></details>`,
	})).join("")}</div>`;
}

/* ---- Debug -------------------------------------------------------------- */
export function renderDebug(discovery) {
	const el = document.querySelector("#debug-page");
	if (!el) return;
	const d = obj(discovery);
	const attempts = Array.isArray(d.attempts) ? d.attempts : [];
	const rows = attempts.map((a) => ({
		cls: a.ok ? "row-good" : "row-bad",
		cells: [escapeHtml(a.activePort || a.port || ""), escapeHtml(a.source || ""), a.ok ? `<span class="cell-yes">CV Helper</span>` : `<span class="cell-skip">no match</span>`, escapeHtml(a.gameState || a.status || a.error || "")],
	}));
	el.innerHTML = `<div class="grid cols-2">
		${panel({ title: "Port Discovery", iconName: "plug", body: kvList([
			{ iconName: "hash", k: "Active port", v: escapeHtml(d.activePort || "—") },
			{ iconName: "compass", k: "Source", v: escapeHtml(d.sourceLabel || d.source || "—") },
			{ iconName: "wifi", k: "Helper", v: d.helperAvailable ? "Online" : "Offline", tone: d.helperAvailable ? "good" : "" },
			{ iconName: "alert-triangle", k: "Last error", v: escapeHtml(d.error || "none"), tone: d.error ? "bad" : "" },
		]) })}
		${panel({ title: "Discovery Attempts", iconName: "list", extra: `${attempts.length}`, flush: true, body: table({ columns: [{ label: "Port" }, { label: "Source" }, { label: "Result" }, { label: "Detail" }], rows, empty: "No discovery attempts recorded." }) })}
	</div>
	<p class="muted" style="margin-top:14px;font-size:11px">${escapeHtml(d.summary || "")}</p>`;
}

/* ---- Inventory ---------------------------------------------------------- */
function itemsTable(items) {
	const list = Array.isArray(items) ? items : [];
	const rows = list.slice(0, 40).map((it) => ({
		cls: it.protected ? "row-sel" : "",
		cells: [
			`${escapeHtml(it.name || "item")} <small>×${escapeHtml(it.quantity ?? 1)}</small>`,
			gpValue(num(it.gePriceEach), "Item GE each"),
			gpValue(num(it.gePrice), "Item GE stack total"),
			gpValue(num(it.haPriceEach), "Item HA each"),
			gpValue(num(it.haPrice), "Item HA stack total"),
			it.protected ? `<span class="cell-prio">protected</span>` : "—",
		],
	}));
	return table({ columns: [{ label: "Item" }, { label: "GE each" }, { label: "GE stack" }, { label: "HA each" }, { label: "HA stack" }, { label: "Flag" }], rows, empty: "No items." });
}

export function renderInventory(status) {
	const el = document.querySelector("#inventory-page");
	if (!el) return;
	const wealth = obj(obj(status.player).wealth);
	const inv = obj(wealth.inventory);
	const equip = obj(wealth.equipment);
	el.innerHTML = `
		<div class="grid cols-4" style="margin-bottom:var(--gap)">
			${panel({ title: "Carried (GE)", iconName: "coins", body: `<div class="big-stat">${gpValue(num(wealth.totalCarriedValueGe), "Carried GE total")}</div>` })}
			${panel({ title: "Carried (HA)", iconName: "wand-2", body: `<div class="big-stat">${gpValue(num(wealth.totalCarriedValueHa), "Carried HA total")}</div>` })}
			${panel({ title: "Equipment (GE)", iconName: "shield", body: `<div class="big-stat">${gpValue(num(equip.gePrice) ?? num(wealth.equipmentGe), "Equipment GE total")}</div>` })}
			${panel({ title: "Risked (GE)", iconName: "alert-triangle", body: `<div class="big-stat">${gpValue(num(wealth.riskedValueGeApprox), "Risked GE total")}</div>` })}
		</div>
		<div class="grid cols-2">
			${panel({ title: "Inventory", iconName: "backpack", extra: `${selectedValue(inv.occupiedSlots, "")} slots`, flush: true, body: itemsTable(inv.items) })}
			${panel({ title: "Equipment", iconName: "shield", flush: true, body: itemsTable(equip.items) })}
		</div>`;
}
