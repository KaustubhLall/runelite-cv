/* ============================================================================
 * components.js — pure HTML-string builders for the themed UI atoms.
 * Every helper returns a string; callers inject and then call refreshIcons().
 * ========================================================================== */
import { icon, gpIcon } from "./icons.js";
import { escapeHtml, getGpTier, formatGpValue } from "./format.js";

const isHtml = (v) => typeof v === "string" && /<\/?[a-z]/i.test(v);
const safe = (v) => (isHtml(v) ? v : escapeHtml(v));

/** Ornate panel with gilt header. body is raw HTML. */
export function panel({ title, iconName, extra = "", body = "", flush = false, className = "" }) {
	return `
		<section class="panel ${className}">
			<div class="panel-head">
				${iconName ? `<span class="ph-icon">${icon(iconName)}</span>` : ""}
				<h3>${escapeHtml(title)}</h3>
				${extra ? `<span class="ph-extra">${safe(extra)}</span>` : ""}
			</div>
			<div class="panel-body ${flush ? "flush" : ""}">${body}</div>
		</section>`;
}

/** Hero metric card for the top strip. */
export function metric({ iconName, iconHtml, label, value, sub = "", tone = "", bar = null }) {
	const barHtml = bar && typeof bar.percent === "number"
		? `<div class="m-bar"><i style="width:${Math.max(0, Math.min(100, bar.percent))}%;background:${bar.color || "var(--good)"}"></i></div>`
		: "";
	return `
		<div class="metric ${tone}">
			<span class="m-ico">${iconHtml || icon(iconName || "circle")}</span>
			<span class="m-body">
				<span class="m-label">${escapeHtml(label)}</span>
				<span class="m-value ${tone}">${safe(value)}</span>
				${sub ? `<span class="m-sub">${safe(sub)}</span>` : ""}
				${barHtml}
			</span>
		</div>`;
}

/** Key/value row inside a panel. */
export function kvRow({ iconName, k, v, tone = "" }) {
	return `
		<div class="kv-row">
			${iconName ? `<span class="kv-ico">${icon(iconName)}</span>` : ""}
			<span class="kv-k">${escapeHtml(k)}</span>
			<span class="kv-v ${tone}">${safe(v)}</span>
		</div>`;
}

export function kvList(rows, twoCol = false) {
	return `<div class="kv${twoCol ? " two" : ""}">${rows.map(kvRow).join("")}</div>`;
}

export function badge(text, tone = "", { pulse = false, live = false } = {}) {
	return `<span class="badge ${tone} ${live ? "live" : ""}">${pulse ? '<span class="pulse"></span>' : ""}${escapeHtml(text)}</span>`;
}

/** An italic entity id chip. */
export function idChip(value) {
	if (value === undefined || value === null || value === "") return "";
	return `<span class="id">#${escapeHtml(value)}</span>`;
}

/**
 * Generic table.
 * columns: [{ label, align? }]; rows: [{ cells: [html], cls?: "" }]
 */
export function table({ columns, rows, empty = "No data." }) {
	if (!rows || !rows.length) return `<p class="empty compact">${escapeHtml(empty)}</p>`;
	const head = columns.map((c) => `<th${c.align ? ` style="text-align:${c.align}"` : ""}>${escapeHtml(c.label)}</th>`).join("");
	const body = rows.map((r) => `<tr class="${r.cls || ""}">${r.cells.map((cell) => `<td>${cell}</td>`).join("")}</tr>`).join("");
	return `<div class="tbl-wrap"><table class="tbl"><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>`;
}

/** Compact field=value detail table (used for runtime decision blocks). */
export function detailTable(rows, empty = "No data.") {
	if (!rows.length) return `<p class="empty compact">${escapeHtml(empty)}</p>`;
	return `<div class="tbl-wrap"><table class="tbl">
		<tbody>${rows.map(([k, v]) => `<tr><td class="muted">${escapeHtml(k)}</td><td>${safe(v)}</td></tr>`).join("")}</tbody>
	</table></div>`;
}

/** GP value badge with coin icon, tier color, and tooltip. */
export function gpValue(value, { label = "", showIcon = true, tooltip = true } = {}) {
	if (typeof value !== "number") return `<span class="gp-value gp-muted">—</span>`;
	const tier = getGpTier(value);
	const compact = formatGpValue(value);
	const rawFormatted = value.toLocaleString("en-US");
	const iconHtml = showIcon ? `<span class="gp-ico">${gpIcon(value)}</span>` : "";
	const labelHtml = label ? `<span class="gp-label">${escapeHtml(label)}</span>` : "";
	const tooltipAttr = tooltip ? `title="${rawFormatted} GP"` : "";
	return `<span class="gp-badge ${tier.colorClass}" ${tooltipAttr}>${iconHtml}${labelHtml}<span class="gp-text">${compact}</span></span>`;
}

/**
 * Item value cell. Shows the TOTAL stack value by default (`total` = price × quantity); for real
 * stacks (qty > 1) it appends the per-item value in brackets, each rendered with its own GP
 * signature. `total` falls back to `each` when the stack total is missing. Use this anywhere an
 * item-with-quantity value is shown so stacks never display the misleading single-item price.
 */
export function itemValue(total, each, quantity) {
	const t = typeof total === "number" ? total : undefined;
	const e = typeof each === "number" ? each : undefined;
	const qty = typeof quantity === "number" ? quantity : 1;
	const base = gpValue(t ?? e);
	if (qty > 1 && e != null && (t == null || e !== t)) {
		return `${base} <small class="muted">(${gpValue(e, { tooltip: true })}&nbsp;ea)</small>`;
	}
	return base;
}
