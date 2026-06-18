/*
 * Copyright (c) 2026
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 */
package net.runelite.client.plugins.cvhelper;

import net.runelite.client.config.Keybind;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(CvHelperConfig.GROUP)
public interface CvHelperConfig extends Config
{
	String GROUP = "cvhelper";
	String SHOW_HOVER_OVERLAY = "showHoverOverlay";
	String SHOW_WIDGET_INFO = "showWidgetInfo";
	String SHOW_PRAYER_TARGETS = "showPrayerTargets";
	String SHOW_SPELL_TARGETS = "showSpellTargets";
	String SHOW_MINIMAP_TARGETS = "showMinimapTargets";
	String SHOW_INVENTORY_TARGETS = "showInventoryTargets";
	String SHOW_EQUIPMENT_TARGETS = "showEquipmentTargets";
	String SHOW_PANEL_TARGETS = "showPanelTargets";
	String SHOW_COMBAT_TARGETS = "showCombatTargets";
	String SHOW_ENTITY_TARGETS = "showEntityTargets";
	String SHOW_TARGET_LABELS = "showTargetLabels";
	String ENABLE_LOCAL_EXPORT = "enableLocalExport";
	String LOCAL_PORT = "localPort";
	String WEBHOOK_URL = "webhookUrl";
	String DEBUG_HOTKEY = "debugHotkey";
	String PRINT_BOUNDS_HOTKEY = "printBoundsHotkey";
	String CAPTURE_SCREEN_HOTKEY = "captureScreenHotkey";
	String REFRESH_ENTITIES_HOTKEY = "refreshEntitiesHotkey";
	String NEAREST_ENTITY_HOTKEY = "nearestEntityHotkey";

	@ConfigItem(
		keyName = SHOW_HOVER_OVERLAY,
		name = "Show hover overlay",
		description = "Draw the currently hovered widget bounds and coordinates."
	)
	default boolean showHoverOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_WIDGET_INFO,
		name = "Show widget info",
		description = "Display widget parent/group identifiers in the overlay."
	)
	default boolean showWidgetInfo()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_PRAYER_TARGETS,
		name = "Show prayer targets",
		description = "Draw known prayer target bounds when the prayer interface is visible."
	)
	default boolean showPrayerTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_SPELL_TARGETS,
		name = "Show spell targets",
		description = "Draw known spell target bounds when the magic interface is visible."
	)
	default boolean showSpellTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_MINIMAP_TARGETS,
		name = "Show minimap targets",
		description = "Draw minimap and orb target bounds, including HP, prayer, run, spec, and world map controls."
	)
	default boolean showMinimapTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_INVENTORY_TARGETS,
		name = "Show inventory targets",
		description = "Draw inventory slot target bounds when visible."
	)
	default boolean showInventoryTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_EQUIPMENT_TARGETS,
		name = "Show equipment targets",
		description = "Draw equipment slot target bounds when visible."
	)
	default boolean showEquipmentTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_PANEL_TARGETS,
		name = "Show panel tab targets",
		description = "Draw side-panel tab/navigation target bounds such as inventory, equipment, prayer, magic, and combat."
	)
	default boolean showPanelTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_COMBAT_TARGETS,
		name = "Show combat targets",
		description = "Draw combat option targets such as attack styles, auto-retaliate, and autocast controls when visible."
	)
	default boolean showCombatTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_ENTITY_TARGETS,
		name = "Show nearby entity boxes",
		description = "Draw nearby player and NPC canvas bounds where RuneLite exposes them."
	)
	default boolean showEntityTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_TARGET_LABELS,
		name = "Show target labels",
		description = "Draw target names next to prayer, spell, and UI target boxes."
	)
	default boolean showTargetLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = ENABLE_LOCAL_EXPORT,
		name = "Enable localhost export",
		description = "Expose a small local HTTP endpoint with the latest capture status."
	)
	default boolean enableLocalExport()
	{
		return true;
	}

	@ConfigItem(
		keyName = LOCAL_PORT,
		name = "Local export port",
		description = "Preferred fixed localhost port. If the port is busy, CV Helper falls back to an open port and reports it in /status."
	)
	default int localPort()
	{
		return 11777;
	}

	@ConfigItem(
		keyName = WEBHOOK_URL,
		name = "Python webhook URL",
		description = "Optional local webhook URL for forwarding target snapshots to Python."
	)
	default String webhookUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = DEBUG_HOTKEY,
		name = "Debug status hotkey",
		description = "Print CV Helper status and target counts to in-game chat."
	)
	default Keybind debugHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = PRINT_BOUNDS_HOTKEY,
		name = "Print bounds hotkey",
		description = "Print current overlay/widget bounds to in-game chat."
	)
	default Keybind printBoundsHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = CAPTURE_SCREEN_HOTKEY,
		name = "Capture screen hotkey",
		description = "Queue a raw client-canvas capture through CV Helper."
	)
	default Keybind captureScreenHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = REFRESH_ENTITIES_HOTKEY,
		name = "Refresh entities hotkey",
		description = "Refresh nearby player/NPC exports and forward them to the configured webhook."
	)
	default Keybind refreshEntitiesHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = NEAREST_ENTITY_HOTKEY,
		name = "Nearest entity hotkey",
		description = "Print the nearest exported entity and its preferred canvas click point to in-game chat."
	)
	default Keybind nearestEntityHotkey()
	{
		return Keybind.NOT_SET;
	}
}
