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
import net.runelite.client.config.ConfigSection;

@ConfigGroup(CvHelperConfig.GROUP)
public interface CvHelperConfig extends Config
{
	String GROUP = "cvhelper";
	String ACTION_SECTION = "actionSection";
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
	String ACTION_ENABLED_1 = "actionEnabled1";
	String ACTION_HOTKEY_1 = "actionHotkey1";
	String ACTION_SURFACE_1 = "actionSurface1";
	String ACTION_TARGET_1 = "actionTarget1";
	String ACTION_CLICK_MOUSE_1 = "actionClickMouse1";
	String ACTION_CLICK_AFTER_MODE_1 = "actionClickAfterMode1";
	String ACTION_INVOCATION_MODE_1 = "actionInvocationMode1";
	String ACTION_RETURN_PANEL_1 = "actionReturnPanel1";
	String ACTION_RETURN_MOUSE_CENTER_1 = "actionReturnMouseCenter1";
	String ACTION_ENABLED_2 = "actionEnabled2";
	String ACTION_HOTKEY_2 = "actionHotkey2";
	String ACTION_SURFACE_2 = "actionSurface2";
	String ACTION_TARGET_2 = "actionTarget2";
	String ACTION_CLICK_MOUSE_2 = "actionClickMouse2";
	String ACTION_CLICK_AFTER_MODE_2 = "actionClickAfterMode2";
	String ACTION_INVOCATION_MODE_2 = "actionInvocationMode2";
	String ACTION_RETURN_PANEL_2 = "actionReturnPanel2";
	String ACTION_RETURN_MOUSE_CENTER_2 = "actionReturnMouseCenter2";
	String ACTION_ENABLED_3 = "actionEnabled3";
	String ACTION_HOTKEY_3 = "actionHotkey3";
	String ACTION_SURFACE_3 = "actionSurface3";
	String ACTION_TARGET_3 = "actionTarget3";
	String ACTION_CLICK_MOUSE_3 = "actionClickMouse3";
	String ACTION_CLICK_AFTER_MODE_3 = "actionClickAfterMode3";
	String ACTION_INVOCATION_MODE_3 = "actionInvocationMode3";
	String ACTION_RETURN_PANEL_3 = "actionReturnPanel3";
	String ACTION_RETURN_MOUSE_CENTER_3 = "actionReturnMouseCenter3";
	String ACTION_ENABLED_4 = "actionEnabled4";
	String ACTION_HOTKEY_4 = "actionHotkey4";
	String ACTION_SURFACE_4 = "actionSurface4";
	String ACTION_TARGET_4 = "actionTarget4";
	String ACTION_CLICK_MOUSE_4 = "actionClickMouse4";
	String ACTION_CLICK_AFTER_MODE_4 = "actionClickAfterMode4";
	String ACTION_INVOCATION_MODE_4 = "actionInvocationMode4";
	String ACTION_RETURN_PANEL_4 = "actionReturnPanel4";
	String ACTION_RETURN_MOUSE_CENTER_4 = "actionReturnMouseCenter4";
	String ACTION_PANEL_OPEN_DELAY_MS = "actionPanelOpenDelayMs";
	String ACTION_WIDGET_TARGET_DELAY_MS = "actionWidgetTargetDelayMs";
	String ACTION_SELECTED_WIDGET_TIMEOUT_MS = "actionSelectedWidgetTimeoutMs";
	String ACTION_RETURN_PANEL_DELAY_MS = "actionReturnPanelDelayMs";
	String ACTION_MOUSE_RESTORE_DELAY_MS = "actionMouseRestoreDelayMs";

	@ConfigSection(
		name = "Action hotkeys",
		description = "Configurable action-click hotkeys for prayer, spell, UI, and entity targets.",
		position = 100
	)
	String actionSection = ACTION_SECTION;

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

	@ConfigItem(
		keyName = ACTION_PANEL_OPEN_DELAY_MS,
		name = "Panel-open delay ms",
		description = "Delay after opening a required side panel before resolving and invoking the action.",
		section = actionSection
	)
	default int actionPanelOpenDelayMs()
	{
		return 125;
	}

	@ConfigItem(
		keyName = ACTION_WIDGET_TARGET_DELAY_MS,
		name = "Widget-target delay ms",
		description = "Delay after a spell/widget action is selected before clicking the current mouse target.",
		section = actionSection
	)
	default int actionWidgetTargetDelayMs()
	{
		return 350;
	}

	@ConfigItem(
		keyName = ACTION_SELECTED_WIDGET_TIMEOUT_MS,
		name = "Spell selected timeout ms",
		description = "Maximum time to wait for RuneLite to report a selected spell/widget before target-clicking.",
		section = actionSection
	)
	default int actionSelectedWidgetTimeoutMs()
	{
		return 900;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_DELAY_MS,
		name = "Return-panel delay ms",
		description = "Delay before clicking back to the previously open side panel.",
		section = actionSection
	)
	default int actionReturnPanelDelayMs()
	{
		return 80;
	}

	@ConfigItem(
		keyName = ACTION_MOUSE_RESTORE_DELAY_MS,
		name = "Mouse-restore delay ms",
		description = "Delay before restoring the OS mouse to its original position.",
		section = actionSection
	)
	default int actionMouseRestoreDelayMs()
	{
		return 40;
	}

	@ConfigItem(
		keyName = ACTION_ENABLED_1,
		name = "Action 1 enabled",
		description = "Enable or disable action slot 1 without clearing its hotkey.",
		section = actionSection
	)
	default boolean actionEnabled1()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_HOTKEY_1,
		name = "Action 1 hotkey",
		description = "Hotkey for action slot 1.",
		section = actionSection
	)
	default Keybind actionHotkey1()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = ACTION_SURFACE_1,
		name = "Action 1 surface",
		description = "Which exported target surface action slot 1 should click.",
		section = actionSection
	)
	default CvHelperActionSurface actionSurface1()
	{
		return CvHelperActionSurface.DISABLED;
	}

	@ConfigItem(
		keyName = ACTION_TARGET_1,
		name = "Action 1 target label",
		description = "Case-insensitive target label text to match, such as Protect from Magic or High Level Alchemy.",
		section = actionSection
	)
	default String actionTarget1()
	{
		return "";
	}

	@ConfigItem(
		keyName = ACTION_CLICK_MOUSE_1,
		name = "Action 1 click mouse after",
		description = "After clicking the target widget, click the current mouse canvas position. Useful for target spells.",
		section = actionSection
	)
	default boolean actionClickMouse1()
	{
		return false;
	}

	@ConfigItem(
		keyName = ACTION_CLICK_AFTER_MODE_1,
		name = "Action 1 click-after mode",
		description = "Whether action slot 1 should click the current mouse target after selecting its target.",
		section = actionSection
	)
	default CvHelperClickAfterMode actionClickAfterMode1()
	{
		return CvHelperClickAfterMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_INVOCATION_MODE_1,
		name = "Action 1 invocation",
		description = "How action slot 1 selects its target: AUTO prefers RuneLite widget actions for spells/prayers; CLICK uses Java Robot.",
		section = actionSection
	)
	default CvHelperActionInvocationMode actionInvocationMode1()
	{
		return CvHelperActionInvocationMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_1,
		name = "Action 1 return panel",
		description = "After action slot 1 finishes, click back to the previously open side panel.",
		section = actionSection
	)
	default boolean actionReturnPanel1()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_MOUSE_CENTER_1,
		name = "Action 1 restore mouse",
		description = "After action slot 1 finishes, restore the mouse to its original screen position.",
		section = actionSection
	)
	default boolean actionReturnMouseCenter1()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_ENABLED_2,
		name = "Action 2 enabled",
		description = "Enable or disable action slot 2 without clearing its hotkey.",
		section = actionSection
	)
	default boolean actionEnabled2()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_HOTKEY_2,
		name = "Action 2 hotkey",
		description = "Hotkey for action slot 2.",
		section = actionSection
	)
	default Keybind actionHotkey2()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = ACTION_SURFACE_2,
		name = "Action 2 surface",
		description = "Which exported target surface action slot 2 should click.",
		section = actionSection
	)
	default CvHelperActionSurface actionSurface2()
	{
		return CvHelperActionSurface.DISABLED;
	}

	@ConfigItem(
		keyName = ACTION_TARGET_2,
		name = "Action 2 target label",
		description = "Case-insensitive target label text to match.",
		section = actionSection
	)
	default String actionTarget2()
	{
		return "";
	}

	@ConfigItem(
		keyName = ACTION_CLICK_MOUSE_2,
		name = "Action 2 click mouse after",
		description = "After clicking the target widget, click the current mouse canvas position.",
		section = actionSection
	)
	default boolean actionClickMouse2()
	{
		return false;
	}

	@ConfigItem(
		keyName = ACTION_CLICK_AFTER_MODE_2,
		name = "Action 2 click-after mode",
		description = "Whether action slot 2 should click the current mouse target after selecting its target.",
		section = actionSection
	)
	default CvHelperClickAfterMode actionClickAfterMode2()
	{
		return CvHelperClickAfterMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_INVOCATION_MODE_2,
		name = "Action 2 invocation",
		description = "How action slot 2 selects its target: AUTO prefers RuneLite widget actions for spells/prayers; CLICK uses Java Robot.",
		section = actionSection
	)
	default CvHelperActionInvocationMode actionInvocationMode2()
	{
		return CvHelperActionInvocationMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_2,
		name = "Action 2 return panel",
		description = "After action slot 2 finishes, click back to the previously open side panel.",
		section = actionSection
	)
	default boolean actionReturnPanel2()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_MOUSE_CENTER_2,
		name = "Action 2 restore mouse",
		description = "After action slot 2 finishes, restore the mouse to its original screen position.",
		section = actionSection
	)
	default boolean actionReturnMouseCenter2()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_ENABLED_3,
		name = "Action 3 enabled",
		description = "Enable or disable action slot 3 without clearing its hotkey.",
		section = actionSection
	)
	default boolean actionEnabled3()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_HOTKEY_3,
		name = "Action 3 hotkey",
		description = "Hotkey for action slot 3.",
		section = actionSection
	)
	default Keybind actionHotkey3()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = ACTION_SURFACE_3,
		name = "Action 3 surface",
		description = "Which exported target surface action slot 3 should click.",
		section = actionSection
	)
	default CvHelperActionSurface actionSurface3()
	{
		return CvHelperActionSurface.DISABLED;
	}

	@ConfigItem(
		keyName = ACTION_TARGET_3,
		name = "Action 3 target label",
		description = "Case-insensitive target label text to match.",
		section = actionSection
	)
	default String actionTarget3()
	{
		return "";
	}

	@ConfigItem(
		keyName = ACTION_CLICK_MOUSE_3,
		name = "Action 3 click mouse after",
		description = "After clicking the target widget, click the current mouse canvas position.",
		section = actionSection
	)
	default boolean actionClickMouse3()
	{
		return false;
	}

	@ConfigItem(
		keyName = ACTION_CLICK_AFTER_MODE_3,
		name = "Action 3 click-after mode",
		description = "Whether action slot 3 should click the current mouse target after selecting its target.",
		section = actionSection
	)
	default CvHelperClickAfterMode actionClickAfterMode3()
	{
		return CvHelperClickAfterMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_INVOCATION_MODE_3,
		name = "Action 3 invocation",
		description = "How action slot 3 selects its target: AUTO prefers RuneLite widget actions for spells/prayers; CLICK uses Java Robot.",
		section = actionSection
	)
	default CvHelperActionInvocationMode actionInvocationMode3()
	{
		return CvHelperActionInvocationMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_3,
		name = "Action 3 return panel",
		description = "After action slot 3 finishes, click back to the previously open side panel.",
		section = actionSection
	)
	default boolean actionReturnPanel3()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_MOUSE_CENTER_3,
		name = "Action 3 restore mouse",
		description = "After action slot 3 finishes, restore the mouse to its original screen position.",
		section = actionSection
	)
	default boolean actionReturnMouseCenter3()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_ENABLED_4,
		name = "Action 4 enabled",
		description = "Enable or disable action slot 4 without clearing its hotkey.",
		section = actionSection
	)
	default boolean actionEnabled4()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_HOTKEY_4,
		name = "Action 4 hotkey",
		description = "Hotkey for action slot 4.",
		section = actionSection
	)
	default Keybind actionHotkey4()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = ACTION_SURFACE_4,
		name = "Action 4 surface",
		description = "Which exported target surface action slot 4 should click.",
		section = actionSection
	)
	default CvHelperActionSurface actionSurface4()
	{
		return CvHelperActionSurface.DISABLED;
	}

	@ConfigItem(
		keyName = ACTION_TARGET_4,
		name = "Action 4 target label",
		description = "Case-insensitive target label text to match.",
		section = actionSection
	)
	default String actionTarget4()
	{
		return "";
	}

	@ConfigItem(
		keyName = ACTION_CLICK_MOUSE_4,
		name = "Action 4 click mouse after",
		description = "After clicking the target widget, click the current mouse canvas position.",
		section = actionSection
	)
	default boolean actionClickMouse4()
	{
		return false;
	}

	@ConfigItem(
		keyName = ACTION_CLICK_AFTER_MODE_4,
		name = "Action 4 click-after mode",
		description = "Whether action slot 4 should click the current mouse target after selecting its target.",
		section = actionSection
	)
	default CvHelperClickAfterMode actionClickAfterMode4()
	{
		return CvHelperClickAfterMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_INVOCATION_MODE_4,
		name = "Action 4 invocation",
		description = "How action slot 4 selects its target: AUTO prefers RuneLite widget actions for spells/prayers; CLICK uses Java Robot.",
		section = actionSection
	)
	default CvHelperActionInvocationMode actionInvocationMode4()
	{
		return CvHelperActionInvocationMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_4,
		name = "Action 4 return panel",
		description = "After action slot 4 finishes, click back to the previously open side panel.",
		section = actionSection
	)
	default boolean actionReturnPanel4()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_MOUSE_CENTER_4,
		name = "Action 4 restore mouse",
		description = "After action slot 4 finishes, restore the mouse to its original screen position.",
		section = actionSection
	)
	default boolean actionReturnMouseCenter4()
	{
		return true;
	}
}
