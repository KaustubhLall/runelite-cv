/* ============================================================================
 * presets.js — farmer target presets (v2-style).
 *
 * Skill presets are PLUGIN-provided (mining/woodcutting /config.presets), each
 * {name, target} where target is a pipe-separated token list. Selecting a preset
 * just fills the target field; the farmer uses that on Start. The Mob farmer has
 * no plugin presets, so it gets a small built-in set plus any the user saves.
 * User presets live in localStorage and can be deleted; built-ins/plugin can't.
 * ========================================================================== */
const KEY = (farmer) => `cvHelperUserPresets:${farmer}`;

// plugin-provided presets, set at runtime from each skill's /config
const pluginPresets = { mob: [], mining: [], woodcutting: [] };

const MOB_BUILTINS = [
	{ name: "Cows", target: "cow" },
	{ name: "Chickens", target: "chicken" },
	{ name: "Goblins", target: "goblin" },
	{ name: "Guards", target: "guard" },
	{ name: "Hill giants", target: "hill giant" },
	{ name: "Men / Women", target: "man|woman" },
];

// API endpoint paths for each farmer type
export const PRESET_ENDPOINT = {
	mob: "mob-farmer",
	mining: "mining",
	woodcutting: "woodcutting",
};

export function setPluginPresets(farmer, list) {
	pluginPresets[farmer] = Array.isArray(list)
		? list.filter((p) => p && p.target).map((p) => ({ name: p.name || p.target, target: p.target, builtin: true }))
		: [];
}

function readUser(farmer) {
	try { return JSON.parse(localStorage.getItem(KEY(farmer)) || "[]") || []; }
	catch { return []; }
}
function writeUser(farmer, list) {
	try { localStorage.setItem(KEY(farmer), JSON.stringify(list)); } catch { /* ignore */ }
}

/** Built-in/plugin presets first, then user-saved. */
export function allPresets(farmer) {
	const base = farmer === "mob"
		? MOB_BUILTINS.map((p) => ({ ...p, builtin: true }))
		: (pluginPresets[farmer] || []);
	const user = readUser(farmer).map((p) => ({ ...p, builtin: false }));
	return [...base, ...user];
}

// Alias for app.js compatibility
export function listPresets(farmer) {
	return allPresets(farmer);
}

export function getPreset(farmer, name) {
	return allPresets(farmer).find((p) => p.name === name);
}

export function savePreset(farmer, name, settings) {
	const target = settings?.target || "";
	saveUserPreset(farmer, name, target);
}

export function deletePreset(farmer, name) {
	return deleteUserPreset(farmer, name);
}

export function isBuiltin(farmer, name) {
	if (farmer === "mob") {
		return MOB_BUILTINS.some((p) => p.name === name);
	}
	return (pluginPresets[farmer] || []).some((p) => p.name === name);
}

export function saveUserPreset(farmer, name, target) {
	const list = readUser(farmer).filter((p) => p.name !== name);
	list.push({ name, target });
	writeUser(farmer, list);
}

export function deleteUserPreset(farmer, name) {
	const list = readUser(farmer);
	const next = list.filter((p) => p.name !== name);
	if (next.length === list.length) return false;
	writeUser(farmer, next);
	return true;
}
