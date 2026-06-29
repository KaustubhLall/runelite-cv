/* ============================================================================
 * pathGrid.js — real 2D reachability grid (zoomable, hover-rich, door-aware).
 *
 * Built from the existing contract (player tile + candidate tiles + reach
 * flags). If the plugin supplies `pathing.route` / `pathing.doors`, the walked
 * route and door markers (open/closed) render on top. Radius is adjustable so a
 * wider slice of the scene can be shown.
 * ========================================================================== */
import { escapeHtml } from "./format.js";

const VIEW = 320;
let RADIUS = 7;                 // tiles visible each direction; 7 -> 15x15
let CELLS = RADIUS * 2 + 1;
let CELL = VIEW / CELLS;

export function setGridRadius(r) {
	RADIUS = Math.max(4, Math.min(100, Math.round(r)));
	CELLS = RADIUS * 2 + 1;
	CELL = VIEW / CELLS;
	return RADIUS;
}
export function getGridRadius() { return RADIUS; }

/** Cheap signature of the tile layout — lets callers skip rebuilds (hover). */
export function gridSignature(player, candidates = [], selected = null, pathing = null, tileGrid = null) {
	if (!player || player.x === undefined) return "none";
	const cs = candidates.map((c) => {
		const w = c.worldLocation;
		return w ? `${w.x},${w.y},${c.selectable ? 1 : 0},${c.reachable === false ? 1 : 0}` : "";
	}).join("|");
	const sel = selected?.worldLocation ? `${selected.worldLocation.x},${selected.worldLocation.y}` : "";
	const doors = Array.isArray(pathing?.doors) ? pathing.doors.map((d) => `${d.x},${d.y},${d.open ? 1 : 0}`).join("|") : "";
	const grid = tileGrid?.player ? `${tileGrid.radius}:${tileGrid.player.x},${tileGrid.player.y}:${tileGrid.tiles?.length ?? 0}` : "";
	return `${RADIUS}:${player.x},${player.y}:${sel}:${cs}:${doors}:${grid}`;
}

function cellCenter(col, row) { return [(col + 0.5) * CELL, (row + 0.5) * CELL]; }

function project(dx, dy) {
	let col = RADIUS + dx;
	let row = RADIUS - dy;
	const offGrid = col < 0 || col > CELLS - 1 || row < 0 || row > CELLS - 1;
	col = Math.max(0, Math.min(CELLS - 1, col));
	row = Math.max(0, Math.min(CELLS - 1, row));
	return { col, row, offGrid };
}

/* Occupied tiles of an object from its footprint/size (origin = SW corner). */
function footprintTiles(c) {
	const fp = c.objectFootprint || c.objectSize;
	const origin = c.worldLocation;
	if (!origin || origin.x === undefined) return [];
	const w = Math.max(1, Math.min(6, Number(fp && fp.width) || 1));
	const h = Math.max(1, Math.min(6, Number(fp && fp.height) || 1));
	const tiles = [];
	for (let ox = 0; ox < w; ox++) for (let oy = 0; oy < h; oy++) tiles.push({ x: origin.x + ox, y: origin.y + oy });
	return tiles;
}

/* Geometric centre of a (possibly multi-tile) object, in world coords. */
function centerPoint(c) {
	const fp = c.objectFootprint || c.objectSize;
	const o = c.worldLocation;
	if (!o || o.x === undefined) return null;
	const w = Math.max(1, Number(fp && fp.width) || 1);
	const h = Math.max(1, Number(fp && fp.height) || 1);
	return { x: o.x + (w - 1) / 2, y: o.y + (h - 1) / 2 };
}

function cellTone(c, isSel) {
	if (isSel) return { fill: "var(--gold)", op: 0.32 };
	// Beyond maxCandidates: still a real scanned object -- the grid is exhaustive
	// regardless of how low maxCandidates is set -- but deemphasized to grey/low
	// opacity so the prioritized set the farmer actually considers stays legible.
	const deemphasized = c.withinMaxCandidates === false;
	const grey = "rgba(170,179,154,.6)";
	if (c.reachable === false || (Array.isArray(c.reasons) && c.reasons.some((r) => /missing-action|blocked|depleted/.test(r)))) {
		return deemphasized ? { fill: grey, op: 0.10 } : { fill: "var(--obstacle, #c0563a)", op: 0.22 };
	}
	if (c.selectable === true) return deemphasized ? { fill: grey, op: 0.09 } : { fill: "var(--good, #6f9a4a)", op: 0.20 };
	if (c.targetMatched === true) return deemphasized ? { fill: grey, op: 0.07 } : { fill: "var(--path, #58b6c9)", op: 0.14 };
	return null;
}

/* Shaded footprint cells for every candidate, drawn under markers. */
function cellFills(candidates, player, selected) {
	if (!player) return "";
	let out = "";
	for (const c of candidates) {
		const isSel = selected && selected.id === c.id && selected.worldLocation && c.worldLocation
			&& selected.worldLocation.x === c.worldLocation.x && selected.worldLocation.y === c.worldLocation.y;
		const tone = cellTone(c, isSel);
		if (!tone) continue;
		for (const t of footprintTiles(c)) {
			const { col, row, offGrid } = project(t.x - player.x, t.y - player.y);
			if (offGrid) continue;
			out += `<rect x="${col * CELL + 0.5}" y="${row * CELL + 0.5}" width="${CELL - 1}" height="${CELL - 1}" fill="${tone.fill}" fill-opacity="${tone.op}"/>`;
		}
	}
	// player tile highlight
	out += `<rect x="${RADIUS * CELL + 0.5}" y="${RADIUS * CELL + 0.5}" width="${CELL - 1}" height="${CELL - 1}" fill="var(--you, #4b8fe0)" fill-opacity="0.22"/>`;
	return out;
}

/* Small hollow ring at the geometric centre of multi-tile objects. */
function centerMarks(candidates, player) {
	if (!player) return "";
	let out = "";
	for (const c of candidates) {
		const fp = c.objectFootprint || c.objectSize;
		const multi = fp && (Number(fp.width) > 1 || Number(fp.height) > 1);
		if (!multi) continue;
		const cp = centerPoint(c);
		if (!cp) continue;
		const { col, row, offGrid } = project(cp.x - player.x, cp.y - player.y);
		if (offGrid) continue;
		const [cx, cy] = cellCenter(col, row);
		out += `<g><title>${escapeHtml(`${c.name || "object"} centre (${fp.width}×${fp.height})`)}</title><circle cx="${cx}" cy="${cy}" r="${CELL * 0.16}" fill="none" stroke="var(--gold-bright, #e8c870)" stroke-width="1.4"/><circle cx="${cx}" cy="${cy}" r="1.4" fill="var(--gold-bright, #e8c870)"/></g>`;
	}
	return out;
}

function gridLines() {
	let out = "";
	for (let i = 0; i <= CELLS; i++) {
		const p = i * CELL;
		const mid = i === RADIUS || i === RADIUS + 1;
		const stroke = mid ? "var(--grid-line-mid)" : "var(--grid-line)";
		out += `<line x1="${p}" y1="0" x2="${p}" y2="${VIEW}" stroke="${stroke}" stroke-width="1"/>`;
		out += `<line x1="0" y1="${p}" x2="${VIEW}" y2="${p}" stroke="${stroke}" stroke-width="1"/>`;
	}
	return out;
}

function drawRoute(route, player) {
	if (!Array.isArray(route) || !route.length || !player) return "";
	const pts = [cellCenter(RADIUS, RADIUS)];
	for (const t of route) { const { col, row } = project(t.x - player.x, t.y - player.y); pts.push(cellCenter(col, row)); }
	const poly = pts.map((p) => p.join(",")).join(" ");
	return `<polyline points="${poly}" fill="none" stroke="var(--path)" stroke-width="2" stroke-dasharray="4 3" opacity="0.6"/>`;
}

function drawDoors(doors, player) {
	if (!Array.isArray(doors) || !doors.length || !player) return "";
	return doors.map((d) => {
		const { col, row } = project(d.x - player.x, d.y - player.y);
		const [cx, cy] = cellCenter(col, row);
		const s = CELL * 0.34;
		const open = d.open === true;
		// open door = hollow amber bracket; closed = filled amber bar (a barrier)
		const glyph = open
			? `<path d="M ${cx - s} ${cy - s} L ${cx - s} ${cy + s} M ${cx + s} ${cy - s} L ${cx + s} ${cy + s}" stroke="var(--door)" stroke-width="2"/>`
			: `<rect x="${cx - s}" y="${cy - s * 0.55}" width="${s * 2}" height="${s * 1.1}" rx="1" fill="var(--door)" opacity="0.85"/>`;
		return `<g><title>${escapeHtml(open ? "Open door" : "Closed door")} (${d.x}, ${d.y})</title>${glyph}</g>`;
	}).join("");
}

/**
 * Base layer: every tile the backend's /pathing/grid flood-fill reports within
 * its radius, reachable or not. Drawn first (lowest z) so candidate footprint
 * fills, the selected-target highlight, route and doors render on top of it.
 * `tileGrid` is the raw payload: {player:{x,y,plane}, radius, tiles:[{x,y,dx,dy,
 * reachable,pathDistance,blockedReason}]}.
 */
function tileGridFills(tileGrid, player) {
	const tiles = Array.isArray(tileGrid?.tiles) ? tileGrid.tiles : [];
	if (!tiles.length || !player) return "";
	let out = "";
	for (const t of tiles) {
		// Recompute the offset from the CURRENT player position rather than trusting
		// the payload's snapshot-time dx/dy -- the grid can be a tick or two stale
		// (fetched async, not on the critical render path) while the player walks,
		// and reprojecting keeps every tile aligned under the live "you are here" dot.
		const { col, row, offGrid } = project(t.x - player.x, t.y - player.y);
		if (offGrid) continue;
		// Four distinct states, not one red "blocked" bucket:
		//  - reachable          : walkable right now, no action needed (green)
		//  - reachable-via-door : walkable ONLY after CV Helper opens/closes a permitted
		//                         door and that's verified -- shown hatched amber so it
		//                         reads as "conditionally clear", not plain green
		//  - blocked-by-door    : a door blocks it but CV Helper can't act on it (denylisted,
		//                         or the auto-open/auto-close flag is off) -- solid amber
		//  - collision-blocked  : a real physical obstacle (rock/wall/tree) -- orange
		//  - scene-blocked/no-route : off-loaded-scene or no path found -- muted grey
		const viaDoor = t.reachableViaDoor === true;
		const transitionCount = Number(t.transitionCount || 0);
		const multiTransition = transitionCount > 1;
		const isDoorBlocked = typeof t.blockedReason === "string" && t.blockedReason.startsWith("blocked-by-door");
		const isObstacle = !viaDoor && !isDoorBlocked && t.blockedReason === "collision-blocked";
		const fill = t.reachable ? "var(--good, #6f9a4a)"
			: viaDoor || isDoorBlocked ? "var(--door, #e0a82e)"
			: isObstacle ? "var(--obstacle, #c0563a)"
			: "var(--unreachable, #8d96a8)";
		const op = t.reachable ? 0.07 : viaDoor ? 0.14 : isDoorBlocked ? 0.20 : isObstacle ? 0.16 : 0.08;
		const scene = (t.sceneX !== undefined && t.sceneX !== null) ? `  scene ${t.sceneX},${t.sceneY}` : "";
		const reasonLabel = viaDoor ? (multiTransition ? `multi-transition route (${transitionCount})` : "reachable via transition (pending)") : isDoorBlocked ? "manual action required" : isObstacle ? "obstacle" : "unreachable";
		const door = t.blockingDoor;
		const doorLine = door ? `\n${escapeHtml(door.name || "door")} (#${door.id}) · ${escapeHtml(door.requiredAction ? door.requiredAction + " required" : "")} · ${escapeHtml(door.allowlistStatus || "unknown")}${Array.isArray(door.actions) && door.actions.length ? ` · actions: ${escapeHtml(door.actions.join(", "))}` : ""}` : "";
		const title = t.reachable
			? `world ${t.x}, ${t.y}${scene}\nreachable · ${t.pathDistance} tile${t.pathDistance === 1 ? "" : "s"}`
			: viaDoor
				? `world ${t.x}, ${t.y}${scene}\n${reasonLabel} · ${t.pathDistance} tile${t.pathDistance === 1 ? "" : "s"}${doorLine}`
				: `world ${t.x}, ${t.y}${scene}\n${reasonLabel} · ${escapeHtml(t.blockedReason || "no-route")}${doorLine}`;
		// Hatch pattern for "reachable-via-door" so it's visually distinct from a solid
		// blocked tile AND from plain green -- a diagonal stripe overlay reads as "conditional".
		const hatch = viaDoor
			? `<g clip-path="url(#pg-door-clip-${col}-${row})"><clipPath id="pg-door-clip-${col}-${row}"><rect x="${col * CELL + 0.5}" y="${row * CELL + 0.5}" width="${CELL - 1}" height="${CELL - 1}"/></clipPath>${[...Array(4)].map((_, i) => {
				const lx = col * CELL + i * (CELL / 4);
				return `<line x1="${lx}" y1="${row * CELL + CELL}" x2="${lx + CELL}" y2="${row * CELL}" stroke="var(--door, #e0a82e)" stroke-width="1.5" opacity="0.5"/>`;
			}).join("")}</g>`
			: "";
		const multiMark = multiTransition ? `<circle cx="${(col + .5) * CELL}" cy="${(row + .5) * CELL}" r="${Math.max(1.5, CELL * .14)}" fill="var(--gold-bright)"/>` : "";
		out += `<g><title>${title}</title><rect x="${col * CELL + 0.5}" y="${row * CELL + 0.5}" width="${CELL - 1}" height="${CELL - 1}" fill="${fill}" fill-opacity="${op}"/>${hatch}${multiMark}</g>`;
	}
	return out;
}

function targetLine(player, selected) {
	if (!player || !selected?.worldLocation) return "";
	const { col, row } = project(selected.worldLocation.x - player.x, selected.worldLocation.y - player.y);
	const [tx, ty] = cellCenter(col, row);
	const [px, py] = cellCenter(RADIUS, RADIUS);
	const tone = selected.reachable === false ? "var(--obstacle)" : "var(--path)";
	return `<line x1="${px}" y1="${py}" x2="${tx}" y2="${ty}" stroke="${tone}" stroke-width="1.5" stroke-dasharray="5 4" opacity="0.5"/>`;
}

function tooltip(c, dx, dy) {
	const lines = [
		`${c.name || "npc"}${c.id !== undefined ? ` (#${c.id})` : ""}`,
		`tile: ${c.worldLocation.x}, ${c.worldLocation.y}  (Δ ${dx >= 0 ? "+" : ""}${dx}, ${dy >= 0 ? "+" : ""}${dy})`,
		`distance: ${c.distance ?? "?"}   path: ${c.pathDistance ?? "—"}`,
		`reachable: ${c.reachable === undefined ? "?" : c.reachable ? "yes" : "no"}   visible: ${c.visible === undefined ? "?" : c.visible ? "yes" : "no"}`,
		`selectable: ${c.selectable ? "yes" : "no"}${c.lineOfSightToLocalPlayer !== undefined ? `   los: ${c.lineOfSightToLocalPlayer ? "yes" : "no"}` : ""}`,
	];
	const reasons = Array.isArray(c.reasons) && c.reasons.length ? `reasons: ${c.reasons.join(", ")}` : "";
	if (reasons) lines.push(reasons);
	return escapeHtml(lines.join("\n"));
}

function marker(c, player, selected) {
	if (!c?.worldLocation || !player) return "";
	const dx = c.worldLocation.x - player.x;
	const dy = c.worldLocation.y - player.y;
	if (dx === 0 && dy === 0) return "";
	const { col, row, offGrid } = project(dx, dy);
	const [cx, cy] = cellCenter(col, row);
	const isSel = c === selected || (selected && selected.id === c.id && selected.worldLocation && selected.worldLocation.x === c.worldLocation.x && selected.worldLocation.y === c.worldLocation.y);
	const engagedOther = c.engagedByOther === true;
	const isTarget = c.selectable === true || c.engagedWithLocalPlayer === true || c.targetMatched === true;
	// Beyond maxCandidates: same marker shape, deemphasized to grey/low opacity
	// instead of being omitted, so the minimap stays exhaustive.
	const deemphasized = c.withinMaxCandidates === false && !isSel;
	const dimOpacity = deemphasized ? 0.45 : 1;
	const colorFor = (normal) => (deemphasized ? "rgba(170,179,154,.55)" : normal);

	let glyph;
	if (isSel) {
		const s = CELL * 0.4;
		glyph = `<path d="M ${cx} ${cy - s} L ${cx + s} ${cy} L ${cx} ${cy + s} L ${cx - s} ${cy} Z" fill="var(--gold)" stroke="var(--gold-bright)" stroke-width="1.5"/>`;
	} else if (c.reachable === false) {
		const s = CELL * 0.26;
		glyph = `<path d="M ${cx - s} ${cy - s} L ${cx + s} ${cy + s} M ${cx + s} ${cy - s} L ${cx - s} ${cy + s}" stroke="${colorFor("var(--obstacle)")}" stroke-width="2" opacity="${dimOpacity}"/>`;
	} else if (isTarget) {
		glyph = `<circle cx="${cx}" cy="${cy}" r="${CELL * 0.26}" fill="${colorFor("var(--path)")}" opacity="${dimOpacity}"/>`;
	} else {
		glyph = `<circle cx="${cx}" cy="${cy}" r="${CELL * 0.18}" fill="none" stroke="rgba(170,179,154,.55)" stroke-width="1.4" opacity="${dimOpacity}"/>`;
	}
	const ring = engagedOther ? `<circle cx="${cx}" cy="${cy}" r="${CELL * 0.42}" fill="none" stroke="var(--door)" stroke-width="1.5"/>` : "";
	const edge = offGrid ? `<circle cx="${cx}" cy="${cy}" r="${CELL * 0.46}" fill="none" stroke="rgba(255,255,255,.18)" stroke-width="1" stroke-dasharray="2 2"/>` : "";
	// transparent hit area enlarges the hover target so tooltips are easy to trigger
	const hit = `<circle cx="${cx}" cy="${cy}" r="${CELL * 0.5}" fill="transparent"/>`;
	return `<g class="pg-marker"><title>${tooltip(c, dx, dy)}</title>${edge}${ring}${glyph}${hit}</g>`;
}

function playerMarker(player) {
	const [cx, cy] = cellCenter(RADIUS, RADIUS);
	const title = player ? escapeHtml(`You\ntile: ${player.x}, ${player.y}, ${player.plane ?? 0}`) : "You";
	return `<g><title>${title}</title>
		<circle cx="${cx}" cy="${cy}" r="${CELL * 0.55}" fill="none" stroke="var(--you)" stroke-width="1.5" opacity="0.6"/>
		<circle cx="${cx}" cy="${cy}" r="${CELL * 0.28}" fill="var(--you)"/></g>`;
}

export function buildPathGrid(player, candidates = [], selected = null, pathing = null, tileGrid = null) {
	if (!player || player.x === undefined) {
		return `<div class="empty compact">Awaiting player location…</div>`;
	}
	const markers = candidates.map((c) => marker(c, player, selected)).join("");
	return `
		<svg viewBox="0 0 ${VIEW} ${VIEW}" preserveAspectRatio="xMidYMid meet" role="img" aria-label="Reachability grid">
			<rect x="0" y="0" width="${VIEW}" height="${VIEW}" fill="transparent"/>
			${tileGridFills(tileGrid, player)}
			${cellFills(candidates, player, selected)}
			${gridLines()}
			${centerMarks(candidates, player)}
			${targetLine(player, selected)}
			${drawRoute(pathing?.route, player)}
			${markers}
			${drawDoors(pathing?.doors, player)}
			${playerMarker(player)}
		</svg>`;
}
