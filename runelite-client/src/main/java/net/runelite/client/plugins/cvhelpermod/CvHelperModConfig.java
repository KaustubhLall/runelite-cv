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
package net.runelite.client.plugins.cvhelpermod;

import net.runelite.client.config.Keybind;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CvHelperModConfig.GROUP)
public interface CvHelperModConfig extends Config
{
	String GROUP = "cvhelper";
	String ACTION_SECTION = "actionSection";
	String MOB_FARMER_SECTION = "mobFarmerSection";
	String MINER_SECTION = "minerSection";
	String WOODCUTTER_SECTION = "woodcutterSection";
	String FIREMAKING_SECTION = "firemakingSection";
	String FIREMAKING_LOG_TYPE = "firemakingLogType";
	String FISHING_SECTION = "fishingSection";
	String FISHING_FARMER_TARGET = "fishingFarmerTarget";
	String FISHING_FARMER_ACTION = "fishingFarmerAction";
	String FISHING_SCAN_RADIUS = "fishingScanRadius";
	String FISHING_MAX_CANDIDATES = "fishingMaxCandidates";
	String FISHING_SPOT_MOVED_GRACE_TICKS = "fishingSpotMovedGraceTicks";
	String CHAT_RESPONDER_SECTION = "chatResponderSection";
	String CHAT_RESPONDER_ENABLED = "chatResponderEnabled";
	String CHAT_RESPONDER_MIN_MESSAGES = "chatResponderMinMessages";
	String CHAT_RESPONDER_WINDOW_SECONDS = "chatResponderWindowSeconds";
	String CHAT_RESPONDER_COOLDOWN_SECONDS = "chatResponderCooldownSeconds";
	String CHAT_RESPONDER_MODEL = "chatResponderModel";
	String CHAT_RESPONDER_PROMPT = "chatResponderPrompt";
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
	String SHOW_SKILL_FARMER_TARGETS = "showSkillFarmerTargets";
	String SHOW_TARGET_LABELS = "showTargetLabels";
	String ENABLE_LOCAL_EXPORT = "enableLocalExport";
	String LOCAL_PORT = "localPort";
	String WEBHOOK_URL = "webhookUrl";
	String DEBUG_HOTKEY = "debugHotkey";
	String PRINT_BOUNDS_HOTKEY = "printBoundsHotkey";
	String CAPTURE_SCREEN_HOTKEY = "captureScreenHotkey";
	String REFRESH_ENTITIES_HOTKEY = "refreshEntitiesHotkey";
	String NEAREST_ENTITY_HOTKEY = "nearestEntityHotkey";
	String PANIC_STOP_HOTKEY = "panicStopHotkey";
	String ACTION_ENABLED_1 = "actionEnabled1";
	String ACTION_HOTKEY_1 = "actionHotkey1";
	String ACTION_SURFACE_1 = "actionSurface1";
	String ACTION_TARGET_1 = "actionTarget1";
	String ACTION_CLICK_MOUSE_1 = "actionClickMouse1";
	String ACTION_CLICK_AFTER_MODE_1 = "actionClickAfterMode1";
	String ACTION_INVOCATION_MODE_1 = "actionInvocationMode1";
	String ACTION_PRAYER_MODE_1 = "actionPrayerMode1";
	String ACTION_SPELL_AVAILABILITY_MODE_1 = "actionSpellAvailabilityMode1";
	String ACTION_RETURN_PANEL_1 = "actionReturnPanel1";
	String ACTION_RETURN_MOUSE_CENTER_1 = "actionReturnMouseCenter1";
	String ACTION_ENABLED_2 = "actionEnabled2";
	String ACTION_HOTKEY_2 = "actionHotkey2";
	String ACTION_SURFACE_2 = "actionSurface2";
	String ACTION_TARGET_2 = "actionTarget2";
	String ACTION_CLICK_MOUSE_2 = "actionClickMouse2";
	String ACTION_CLICK_AFTER_MODE_2 = "actionClickAfterMode2";
	String ACTION_INVOCATION_MODE_2 = "actionInvocationMode2";
	String ACTION_PRAYER_MODE_2 = "actionPrayerMode2";
	String ACTION_SPELL_AVAILABILITY_MODE_2 = "actionSpellAvailabilityMode2";
	String ACTION_RETURN_PANEL_2 = "actionReturnPanel2";
	String ACTION_RETURN_MOUSE_CENTER_2 = "actionReturnMouseCenter2";
	String ACTION_ENABLED_3 = "actionEnabled3";
	String ACTION_HOTKEY_3 = "actionHotkey3";
	String ACTION_SURFACE_3 = "actionSurface3";
	String ACTION_TARGET_3 = "actionTarget3";
	String ACTION_CLICK_MOUSE_3 = "actionClickMouse3";
	String ACTION_CLICK_AFTER_MODE_3 = "actionClickAfterMode3";
	String ACTION_INVOCATION_MODE_3 = "actionInvocationMode3";
	String ACTION_PRAYER_MODE_3 = "actionPrayerMode3";
	String ACTION_SPELL_AVAILABILITY_MODE_3 = "actionSpellAvailabilityMode3";
	String ACTION_RETURN_PANEL_3 = "actionReturnPanel3";
	String ACTION_RETURN_MOUSE_CENTER_3 = "actionReturnMouseCenter3";
	String ACTION_ENABLED_4 = "actionEnabled4";
	String ACTION_HOTKEY_4 = "actionHotkey4";
	String ACTION_SURFACE_4 = "actionSurface4";
	String ACTION_TARGET_4 = "actionTarget4";
	String ACTION_CLICK_MOUSE_4 = "actionClickMouse4";
	String ACTION_CLICK_AFTER_MODE_4 = "actionClickAfterMode4";
	String ACTION_INVOCATION_MODE_4 = "actionInvocationMode4";
	String ACTION_PRAYER_MODE_4 = "actionPrayerMode4";
	String ACTION_SPELL_AVAILABILITY_MODE_4 = "actionSpellAvailabilityMode4";
	String ACTION_RETURN_PANEL_4 = "actionReturnPanel4";
	String ACTION_RETURN_MOUSE_CENTER_4 = "actionReturnMouseCenter4";
	String ACTION_PANEL_OPEN_DELAY_MS = "actionPanelOpenDelayMs";
	String ACTION_RESOLVE_DELAY_MS = "actionResolveDelayMs";
	String ACTION_MOUSE_SETTLE_DELAY_MS = "actionMouseSettleDelayMs";
	String ACTION_WIDGET_TARGET_DELAY_MS = "actionWidgetTargetDelayMs";
	String ACTION_SELECTED_WIDGET_TIMEOUT_MS = "actionSelectedWidgetTimeoutMs";
	String ACTION_RETURN_PANEL_DELAY_MS = "actionReturnPanelDelayMs";
	String ACTION_MOUSE_RESTORE_DELAY_MS = "actionMouseRestoreDelayMs";
	String ACTION_RETURN_COMBAT_HOTKEY = "actionReturnCombatHotkey";
	String ACTION_RETURN_INVENTORY_HOTKEY = "actionReturnInventoryHotkey";
	String ACTION_RETURN_EQUIPMENT_HOTKEY = "actionReturnEquipmentHotkey";
	String ACTION_RETURN_PRAYER_HOTKEY = "actionReturnPrayerHotkey";
	String ACTION_RETURN_SPELLBOOK_HOTKEY = "actionReturnSpellbookHotkey";

	String ANTI_IDLE_ENABLED = "antiIdleEnabled";
	String ANTI_IDLE_TIMEOUT_MINUTES = "antiIdleTimeoutMinutes";
	String ANTI_IDLE_INPUT_INTERVAL_MINUTES = "antiIdleInputIntervalMinutes";
	String ANTI_IDLE_MANUAL_OVERRIDE = "antiIdleManualOverride";
	String ANTI_IDLE_RESTORE_MOUSE = "antiIdleRestoreMouse";

	String MOB_FARMER_TARGET = "mobFarmerTarget";
	String MOB_FARMER_TARGET_BLACKLIST = "mobFarmerTargetBlacklist";
	String MOB_FARMER_RECOVERY_LOOP_DELAY_MS = "mobFarmerRecoveryLoopDelayMs";
	String MOB_FARMER_AUTORUN_ENABLED = "mobFarmerAutorunEnabled";
	String MOB_FARMER_AUTORUN_MIN_ENERGY = "mobFarmerAutorunMinEnergy";
	String MOB_FARMER_FOCUS_CLICK_AFTER_LOGIN = "mobFarmerFocusClickAfterLogin";
	String MOB_FARMER_AFTER_LOOT_COMBAT_MODE = "mobFarmerAfterLootCombatMode";
	String MOB_FARMER_ENGAGED_MODE = "mobFarmerEngagedMode";
	String MOB_FARMER_AGGRO_RESPONSE = "mobFarmerAggroResponse";
	String MOB_FARMER_REQUIRE_LINE_OF_SIGHT = "mobFarmerRequireLineOfSight";
	String MOB_FARMER_MAX_DISTANCE = "mobFarmerMaxDistance";
	String MOB_FARMER_ATTACK_INTERVAL_TICKS = "mobFarmerAttackIntervalTicks";
	String MOB_FARMER_CONFIRM_ATTACK_BEFORE_INTERRUPT = "mobFarmerConfirmAttackBeforeInterrupt";
	String MOB_FARMER_ATTACK_CONFIRM_TIMEOUT_TICKS = "mobFarmerAttackConfirmTimeoutTicks";
	String MOB_FARMER_COMBAT_STALL_TICKS = "mobFarmerCombatStallTicks";
	String MOB_FARMER_ATTACK_STYLE_SLOT = "mobFarmerAttackStyleSlot";
	String MOB_FARMER_AUTO_EAT_ENABLED = "mobFarmerAutoEatEnabled";
	String MOB_FARMER_EAT_HITPOINT_PERCENT = "mobFarmerEatHitpointPercent";
	String MOB_FARMER_FOOD_ITEMS = "mobFarmerFoodItems";
	String MOB_FARMER_STOP_IF_NO_FOOD = "mobFarmerStopIfNoFood";
	String MOB_FARMER_SURVIVAL_PREEMPTS_ACTIONS = "mobFarmerSurvivalPreemptsActions";
	String MOB_FARMER_LOGIN_RECOVERY_ENABLED = "mobFarmerLoginRecoveryEnabled";
	String MOB_FARMER_LOGIN_RECOVERY_F2P_ONLY = "mobFarmerLoginRecoveryF2pOnly";
	String MOB_FARMER_LOGIN_CLICK_TO_PLAY_ENABLED = "mobFarmerLoginClickToPlayEnabled";
	String MOB_FARMER_LOGIN_DISCONNECT_RECOVERY_ENABLED = "mobFarmerLoginDisconnectRecoveryEnabled";
	String MOB_FARMER_LOGIN_CLICK_OFFSET_X = "mobFarmerLoginClickOffsetX";
	String MOB_FARMER_LOGIN_CLICK_OFFSET_Y = "mobFarmerLoginClickOffsetY";
	String MOB_FARMER_DOOR_AUTO_OPEN_ENABLED = "mobFarmerDoorAutoOpenEnabled";
	String MOB_FARMER_DOOR_AUTO_CLOSE_ENABLED = "mobFarmerDoorAutoCloseEnabled";
	String MOB_FARMER_DOOR_ALLOWLIST = "mobFarmerDoorAllowlist";
	String MOB_FARMER_DOOR_DENYLIST = "mobFarmerDoorDenylist";
	String MOB_FARMER_AUTO_RESUME_AFTER_LOGIN = "mobFarmerAutoResumeAfterLogin";
	String MOB_FARMER_PREFERRED_LOGIN_WORLD = "mobFarmerPreferredLoginWorld";
	String AUTO_LOGIN_ON_LAUNCH = "autoLoginOnLaunch";
	String MOB_FARMER_LOOT_ENABLED = "mobFarmerLootEnabled";
	String MOB_FARMER_LOOT_DURING_COMBAT = "mobFarmerLootDuringCombat";
	String MOB_FARMER_ATTACK_BEFORE_LOOT = "mobFarmerAttackBeforeLoot";
	String MOB_FARMER_LOOT_MIN_VALUE_GE = "mobFarmerLootMinValueGe";
	String MOB_FARMER_LOOT_MIN_SINGLE_GE = "mobFarmerLootMinSingleGe";
	String MOB_FARMER_LOOT_MIN_STACK_GE = "mobFarmerLootMinStackGe";
	String MOB_FARMER_LOOT_MIN_STACK_QUANTITY = "mobFarmerLootMinStackQuantity";
	String MOB_FARMER_LOOT_ALWAYS_STACK_GE = "mobFarmerLootAlwaysStackGe";
	String MOB_FARMER_LOOT_NEVER_STACK_BELOW_GE = "mobFarmerLootNeverStackBelowGe";
	String MOB_FARMER_HIGH_PRIORITY_LOOT_VALUE_GE = "mobFarmerHighPriorityLootValueGe";
	String MOB_FARMER_LOOT_URGENT_DESPAWN_TICKS = "mobFarmerLootUrgentDespawnTicks";
	String MOB_FARMER_LOOT_CLEANUP_PILE_COUNT = "mobFarmerLootCleanupPileCount";
	String MOB_FARMER_LOOT_RADIUS = "mobFarmerLootRadius";
	String MOB_FARMER_HIGH_PRIORITY_LOOT_RADIUS = "mobFarmerHighPriorityLootRadius";
	String MOB_FARMER_NORMAL_LOOT_MAX_MISSED_ATTACKS = "mobFarmerNormalLootMaxMissedAttacks";
	String MOB_FARMER_LOOT_ITEMS = "mobFarmerLootItems";
	String MOB_FARMER_LOOT_BLACKLIST = "mobFarmerLootBlacklist";
	String MOB_FARMER_LOOT_OWNERSHIP_MODE = "mobFarmerLootOwnershipMode";
	String MOB_FARMER_NEVER_DROP_ITEMS = "mobFarmerNeverDropItems";
	String MOB_FARMER_DROP_ITEMS = "mobFarmerDropItems";
	String MOB_FARMER_MAX_DROP_VALUE = "mobFarmerMaxDropValue";
	String MOB_FARMER_ATTACK_INTERACTION_MODE = "mobFarmerAttackInteractionMode";
	String MOB_FARMER_LOOT_INTERACTION_MODE = "mobFarmerLootInteractionMode";
	String MOB_FARMER_GROUND_ITEMS_MODE = "mobFarmerGroundItemsMode";
	String MOB_FARMER_RESPECT_GROUND_ITEMS_HIDDEN = "mobFarmerRespectGroundItemsHidden";
	String MOB_FARMER_INTERMEDIATE_ACTIONS_ENABLED = "mobFarmerIntermediateActionsEnabled";
	String MOB_FARMER_INTERMEDIATE_ITEMS = "mobFarmerIntermediateItems";
	String MOB_FARMER_INTERMEDIATE_ACTION_MAPPINGS = "mobFarmerIntermediateActionMappings";
	String MOB_FARMER_MIN_BURY_BATCH = "mobFarmerMinBuryBatch";
	String MOB_FARMER_COMBAT_INTERRUPT_ITEMS = "mobFarmerCombatInterruptItems";
	String MOB_FARMER_HIGH_ALCH_ENABLED = "mobFarmerHighAlchEnabled";
	String MOB_FARMER_HIGH_ALCH_MIN_HA = "mobFarmerHighAlchMinHa";
	String MOB_FARMER_HIGH_ALCH_MIN_DELTA = "mobFarmerHighAlchMinDelta";
	String MOB_FARMER_HIGH_ALCH_MAX_LOSS = "mobFarmerHighAlchMaxLoss";
	String MOB_FARMER_HIGH_ALCH_ITEMS = "mobFarmerHighAlchItems";
	String MOB_FARMER_HIGH_ALCH_BLACKLIST = "mobFarmerHighAlchBlacklist";
	String DROP_POLICY_ENABLED = "dropPolicyEnabled";
	String DROP_POLICY_MODE = "dropPolicyMode";
	String DROP_POLICY_THRESHOLD_SLOTS = "dropPolicyThresholdSlots";
	String DROP_POLICY_ITEMS = "dropPolicyItems";
	String DROP_POLICY_PROTECTED_ITEMS = "dropPolicyProtectedItems";
	String DROP_POLICY_MAX_VALUE = "dropPolicyMaxValue";
	String WOODCUTTING_STICK_TO_TARGET = "woodcuttingStickToTarget";
	String WOODCUTTING_RECLICK_WHEN_ACTIVE = "woodcuttingReclickWhenActivelyChopping";
	String MINING_SCAN_RADIUS = "miningScanRadius";
	String MINING_MAX_CANDIDATES = "miningMaxCandidates";
	String MINING_DROP_ITEMS = "miningDropItems";
	String MINING_FARMER_TARGET = "miningFarmerTarget";
	String WOODCUTTING_FARMER_TARGET = "woodcuttingFarmerTarget";
	String WOODCUTTING_SCAN_RADIUS = "woodcuttingScanRadius";
	String WOODCUTTING_MAX_CANDIDATES = "woodcuttingMaxCandidates";

	@ConfigSection(
		name = "Action hotkeys",
		description = "Native config exposes the stable global controls and slots 1-4. Use the CV Helper sidebar or WebHelper Actions page for the full 22-slot editor; those surfaces persist slots 5-22 through the same ConfigManager keys.",
		position = 100
	)
	String actionSection = ACTION_SECTION;

	@ConfigSection(
		name = "Mob farmer",
		description = "Combat automation guardrails, target selection, and farmer safety options.",
		position = 110
	)
	String mobFarmerSection = MOB_FARMER_SECTION;

	@ConfigSection(
		name = "Miner",
		description = "Mining automation and inventory management options.",
		position = 112
	)
	String minerSection = MINER_SECTION;

	@ConfigSection(
		name = "Woodcutter",
		description = "Woodcutting automation and inventory management options.",
		position = 115
	)
	String woodcutterSection = WOODCUTTER_SECTION;

	@ConfigSection(
		name = "Firemaker",
		description = "Firemaking automation: burns logs from inventory using a tinderbox. Stops automatically when inventory is empty or tinderbox is missing.",
		position = 117
	)
	String firemakingSection = FIREMAKING_SECTION;

	@ConfigItem(
		keyName = FIREMAKING_LOG_TYPE,
		name = "Log type",
		description = "Log type to burn. Must match the logs in your inventory.",
		section = firemakingSection
	)
	default CvHelperFiremakingLogType firemakingLogType()
	{
		return CvHelperFiremakingLogType.LOGS;
	}

	@ConfigSection(
		name = "Fisher",
		description = "Fishing automation (scaffold — not yet implemented).",
		position = 118
	)
	String fishingSection = FISHING_SECTION;

	@ConfigItem(
		keyName = FISHING_FARMER_TARGET,
		name = "Fishing target",
		description = "Fishing spot NPC name, e.g. 'fishing spot' or 'rod fishing spot'.",
		section = fishingSection
	)
	default String fishingFarmerTarget()
	{
		return "fishing spot";
	}

	@ConfigItem(
		keyName = FISHING_FARMER_ACTION,
		name = "Fishing action",
		description = "Menu action to use on the fishing spot NPC, e.g. 'Lure', 'Bait', 'Small Net', 'Cage', 'Harpoon', 'Net'.",
		section = fishingSection
	)
	default String fishingFarmerAction()
	{
		return "Lure";
	}

	@ConfigItem(
		keyName = FISHING_SCAN_RADIUS,
		name = "Fishing scan radius (tiles)",
		description = "Maximum distance in tiles to search for fishing spots.",
		section = fishingSection
	)
	@Range(min = 1, max = 64)
	default int fishingScanRadius()
	{
		return 5;
	}

	@ConfigItem(
		keyName = FISHING_MAX_CANDIDATES,
		name = "Fishing max candidates",
		description = "Maximum number of fishing spot candidates to evaluate.",
		section = fishingSection
	)
	@Range(min = 1, max = 300)
	default int fishingMaxCandidates()
	{
		return 10;
	}

	@ConfigItem(
		keyName = FISHING_SPOT_MOVED_GRACE_TICKS,
		name = "Spot-moved grace ticks",
		description = "Ticks to wait after a spot moves before re-targeting. Prevents immediate re-targeting when the spot briefly relocates.",
		section = fishingSection
	)
	@Range(min = 0, max = 10)
	default int fishingSpotMovedGraceTicks()
	{
		return 3;
	}

	@ConfigSection(
		name = "Chat responder",
		description = "Experimental AI chat responder that reads public chat and replies using OpenAI. Toggle off to disable completely.",
		position = 120
	)
	String chatResponderSection = CHAT_RESPONDER_SECTION;

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
		keyName = SHOW_SKILL_FARMER_TARGETS,
		name = "Show skilling target boxes",
		description = "Draw latest mining and woodcutting candidate object boxes when the farmer scans them."
	)
	default boolean showSkillFarmerTargets()
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
		keyName = PANIC_STOP_HOTKEY,
		name = "Panic stop hotkey",
		description = "Immediately stop CV Helper loops and clear the in-progress action guard.",
		section = actionSection
	)
	default Keybind panicStopHotkey()
	{
		return new Keybind(java.awt.event.KeyEvent.VK_F12, 0);
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
		keyName = ACTION_RESOLVE_DELAY_MS,
		name = "Target-resolve retry delay ms",
		description = "Delay between retries when a target is not immediately available after opening a panel.",
		section = actionSection
	)
	default int actionResolveDelayMs()
	{
		return 80;
	}

	@ConfigItem(
		keyName = ACTION_MOUSE_SETTLE_DELAY_MS,
		name = "Mouse-settle delay ms",
		description = "Delay after moving the OS mouse before pressing, giving the client hover target time to update.",
		section = actionSection
	)
	default int actionMouseSettleDelayMs()
	{
		return 35;
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
		description = "Maximum time to wait for RuneLite to report a selected spell/widget before falling back to a physical spell click.",
		section = actionSection
	)
	default int actionSelectedWidgetTimeoutMs()
	{
		return 250;
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
		keyName = ACTION_RETURN_COMBAT_HOTKEY,
		name = "Panel key: combat",
		description = "Keyboard key CV Helper presses to open or return to the combat tab without clicking UI.",
		section = actionSection
	)
	default Keybind actionReturnCombatHotkey()
	{
		return new Keybind(java.awt.event.KeyEvent.VK_F1, 0);
	}

	@ConfigItem(
		keyName = ACTION_RETURN_INVENTORY_HOTKEY,
		name = "Panel key: inventory",
		description = "Keyboard key CV Helper presses to open or return to inventory without clicking UI. Set this to your remapped inventory key if needed.",
		section = actionSection
	)
	default Keybind actionReturnInventoryHotkey()
	{
		return new Keybind(java.awt.event.KeyEvent.VK_F4, 0);
	}

	@ConfigItem(
		keyName = ACTION_RETURN_EQUIPMENT_HOTKEY,
		name = "Panel key: equipment",
		description = "Keyboard key CV Helper presses to open or return to equipment without clicking UI.",
		section = actionSection
	)
	default Keybind actionReturnEquipmentHotkey()
	{
		return new Keybind(java.awt.event.KeyEvent.VK_F5, 0);
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PRAYER_HOTKEY,
		name = "Panel key: prayer",
		description = "Keyboard key CV Helper presses to open or return to prayer without clicking UI.",
		section = actionSection
	)
	default Keybind actionReturnPrayerHotkey()
	{
		return new Keybind(java.awt.event.KeyEvent.VK_F6, 0);
	}

	@ConfigItem(
		keyName = ACTION_RETURN_SPELLBOOK_HOTKEY,
		name = "Panel key: magic",
		description = "Keyboard key CV Helper presses to open or return to the magic tab without clicking UI.",
		section = actionSection
	)
	default Keybind actionReturnSpellbookHotkey()
	{
		return new Keybind(java.awt.event.KeyEvent.VK_F7, 0);
	}

	@ConfigItem(
		keyName = MOB_FARMER_TARGET,
		name = "Mob target",
		description = "Partial NPC name, id:<npc id>, or a list separated by |, comma, semicolon, or newlines.",
		section = mobFarmerSection
	)
	default String mobFarmerTarget()
	{
		return "cow";
	}

	@ConfigItem(
		keyName = MOB_FARMER_TARGET_BLACKLIST,
		name = "Never-attack mobs",
		description = "NPCs to never attack even if they match the target. Same format (name, id:<npc id>, separated by |, comma, semicolon, or newlines). Wins over the target list.",
		section = mobFarmerSection
	)
	default String mobFarmerTargetBlacklist()
	{
		return "";
	}

	@ConfigItem(
		keyName = MOB_FARMER_RECOVERY_LOOP_DELAY_MS,
		name = "Recovery loop delay",
		description = "Wall-clock delay, in milliseconds, for the background recovery loop when logged out or manually stepping. Logged-in farming remains game-tick driven.",
		section = mobFarmerSection
	)
	default int mobFarmerRecoveryLoopDelayMs()
	{
		return 1200;
	}

	@ConfigItem(
		keyName = MOB_FARMER_AUTORUN_ENABLED,
		name = "Auto-run on",
		description = "When logged in and safe, click the run orb if run is off and run energy is at or above the configured threshold.",
		section = mobFarmerSection
	)
	default boolean mobFarmerAutorunEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = MOB_FARMER_AUTORUN_MIN_ENERGY,
		name = "Auto-run energy %",
		description = "Minimum run energy percent required before the farmer toggles run on. Uses the minimap run toggle target and does not toggle run off.",
		section = mobFarmerSection
	)
	default int mobFarmerAutorunMinEnergy()
	{
		return 30;
	}

	@ConfigItem(
		keyName = MOB_FARMER_FOCUS_CLICK_AFTER_LOGIN,
		name = "Focus click after login",
		description = "After login/startup, require one guarded canvas-center focus click before farming actions. This works around RuneLite/client focus states where movement or menu actions do not register until one click.",
		section = mobFarmerSection
	)
	default boolean mobFarmerFocusClickAfterLogin()
	{
		return false;
	}

	@ConfigItem(
		keyName = MOB_FARMER_AFTER_LOOT_COMBAT_MODE,
		name = "After-loot combat",
		description = "Controls what happens after a pickup if the player is already in combat or an NPC is tagging the player. STAY_ON_CURRENT_ATTACKER prevents tagging another mob; STOP_WHEN_TAGGED stops the farmer.",
		section = mobFarmerSection
	)
	default CvHelperAfterLootCombatMode mobFarmerAfterLootCombatMode()
	{
		return CvHelperAfterLootCombatMode.STAY_ON_CURRENT_ATTACKER;
	}

	@ConfigItem(
		keyName = MOB_FARMER_ENGAGED_MODE,
		name = "Already-engaged mobs",
		description = "Controls whether multi-combat can target mobs already being fought by someone else. Single-combat always skips them.",
		section = mobFarmerSection
	)
	default CvHelperMobEngagedMode mobFarmerEngagedMode()
	{
		return CvHelperMobEngagedMode.PREFER_FREE;
	}

	@ConfigItem(
		keyName = MOB_FARMER_AGGRO_RESPONSE,
		name = "Undesired attacker",
		description = "What to do when an aggressive non-target mob is already attacking the player.",
		section = mobFarmerSection
	)
	default CvHelperMobAggroResponse mobFarmerAggroResponse()
	{
		return CvHelperMobAggroResponse.WAIT;
	}

	@ConfigItem(
		keyName = MOB_FARMER_REQUIRE_LINE_OF_SIGHT,
		name = "Require line of sight",
		description = "Skip mobs without a RuneLite line-of-sight path from the local player. This is a conservative first reachability guard, not full pathfinding.",
		section = mobFarmerSection
	)
	default boolean mobFarmerRequireLineOfSight()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_MAX_DISTANCE,
		name = "Max target distance",
		description = "Maximum tile distance for auto-targeting. Use 0 to disable the distance guard.",
		section = mobFarmerSection
	)
	default int mobFarmerMaxDistance()
	{
		return 20;
	}

	@ConfigItem(
		keyName = MOB_FARMER_ATTACK_INTERVAL_TICKS,
		name = "Attack interval ticks",
		description = "Conservative fallback weapon attack interval used to schedule re-attacks after loot, food, and intermediate actions. Most standard weapons are 4 ticks.",
		section = mobFarmerSection
	)
	default int mobFarmerAttackIntervalTicks()
	{
		return 4;
	}

	@ConfigItem(
		keyName = MOB_FARMER_CONFIRM_ATTACK_BEFORE_INTERRUPT,
		name = "Confirm attack before interrupt",
		description = "Wait for a combat XP drop (proof the swing landed) before burying/scattering or looting mid-combat. Melee XP lands on the swing tick, so the bury+move on that tick no longer cancels the attack.",
		section = mobFarmerSection
	)
	default boolean mobFarmerConfirmAttackBeforeInterrupt()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_ATTACK_CONFIRM_TIMEOUT_TICKS,
		name = "Attack confirm timeout ticks",
		description = "If no combat XP drop is seen for this many ticks while in combat (e.g. 0-damage splashes grant no XP), allow the interrupting action anyway so the bot never stalls. 0 = auto (2x attack interval).",
		section = mobFarmerSection
	)
	default int mobFarmerAttackConfirmTimeoutTicks()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_COMBAT_STALL_TICKS,
		name = "Combat stall recovery ticks",
		description = "While engaged and in range, if no combat XP is gained for this many ticks the farmer treats combat as stuck and takes over the mouse with a ground/target click to break the freeze. Default 100 (~1 min) so the takeover stays rare and non-disruptive. 0 = auto (3x measured attack interval, min 8).",
		section = mobFarmerSection
	)
	default int mobFarmerCombatStallTicks()
	{
		return 100;
	}

	@ConfigItem(
		keyName = MOB_FARMER_ATTACK_STYLE_SLOT,
		name = "Attack style slot",
		description = "Combat attack-style slot to keep selected (1-4, top to bottom in the Combat Options tab) to control which stat is trained. 0 = leave whatever is set. Which stat each slot trains depends on your weapon. Requires the combat tab to be opened once.",
		section = mobFarmerSection
	)
	default int mobFarmerAttackStyleSlot()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_AUTO_EAT_ENABLED,
		name = "Auto-eat enabled",
		description = "Before combat or loot actions, eat a matching inventory item when HP drops below the configured threshold.",
		section = mobFarmerSection
	)
	default boolean mobFarmerAutoEatEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_EAT_HITPOINT_PERCENT,
		name = "Eat below HP %",
		description = "Auto-eat when current hitpoints are at or below this percent of real hitpoints.",
		section = mobFarmerSection
	)
	default int mobFarmerEatHitpointPercent()
	{
		return 50;
	}

	@ConfigItem(
		keyName = MOB_FARMER_FOOD_ITEMS,
		name = "Food items",
		description = "Food item names or id:<item id>, separated by |, comma, semicolon, or newlines.",
		section = mobFarmerSection
	)
	default String mobFarmerFoodItems()
	{
		return "shrimp|trout|salmon|tuna|lobster|swordfish|monkfish|shark|manta ray|anglerfish|cake|jug of wine|karambwan|meat|chicken|bread|pizza|pie";
	}

	@ConfigItem(
		keyName = MOB_FARMER_STOP_IF_NO_FOOD,
		name = "Stop if no food",
		description = "Stop the farmer loop when HP is below the auto-eat threshold and no matching food is available.",
		section = mobFarmerSection
	)
	default boolean mobFarmerStopIfNoFood()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_SURVIVAL_PREEMPTS_ACTIONS,
		name = "Survival preempts actions",
		description = "When HP is below the auto-eat threshold, survival actions interrupt combat/loot scheduling. Survival still wins over high-priority loot.",
		section = mobFarmerSection
	)
	default boolean mobFarmerSurvivalPreemptsActions()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOGIN_RECOVERY_ENABLED,
		name = "Recover after logout",
		description = "When the live farmer reaches RuneLite's login screen, queue the same guarded click-to-play helper used by the panel. This is login recovery only, not anti-idle input.",
		section = mobFarmerSection
	)
	default boolean mobFarmerLoginRecoveryEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOGIN_RECOVERY_F2P_ONLY,
		name = "Recover only on F2P worlds",
		description = "Skip automatic login recovery on member, PvP, Deadman, seasonal, or minigame/special worlds.",
		section = mobFarmerSection
	)
	default boolean mobFarmerLoginRecoveryF2pOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOGIN_CLICK_TO_PLAY_ENABLED,
		name = "Click-to-play on login screen",
		description = "When RuneLite is on the login screen, queue a guarded click-to-play action. This is recovery only, not anti-idle input.",
		section = mobFarmerSection
	)
	default boolean mobFarmerLoginClickToPlayEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOGIN_DISCONNECT_RECOVERY_ENABLED,
		name = "Recover connection-lost",
		description = "When RuneLite reports CONNECTION_LOST, allow a guarded Enter press to advance the disconnect/login flow. This is recovery only, not anti-idle input.",
		section = mobFarmerSection
	)
	default boolean mobFarmerLoginDisconnectRecoveryEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOGIN_CLICK_OFFSET_X,
		name = "Login click offset X (px)",
		description = "Pixel nudge applied to every login/disconnect-screen click (Play Now, the disconnect dialog's Ok, the canvas-centre Enter-fallback) on top of the computed point. Positive = right. Tune this without recompiling if the computed point is consistently off due to stretching/letterboxing.",
		section = mobFarmerSection
	)
	default int mobFarmerLoginClickOffsetX()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOGIN_CLICK_OFFSET_Y,
		name = "Login click offset Y (px)",
		description = "Pixel nudge applied to every login/disconnect-screen click on top of the computed point. Positive = down.",
		section = mobFarmerSection
	)
	default int mobFarmerLoginClickOffsetY()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_DOOR_AUTO_OPEN_ENABLED,
		name = "Auto-open closed doors to path",
		description = "Allow the farmer to click Open on a closed, allowlisted and non-denylisted door/gate that blocks its path, then wait for collision to confirm the transition is clear.",
		section = mobFarmerSection
	)
	default boolean mobFarmerDoorAutoOpenEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_DOOR_AUTO_CLOSE_ENABLED,
		name = "Auto-close open doors to path",
		description = "Allow the farmer to click Close on an open, allowlisted and non-denylisted door/gate that blocks its path (rare). Off by default since this is less common than opening a closed door.",
		section = mobFarmerSection
	)
	default boolean mobFarmerDoorAutoCloseEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = MOB_FARMER_DOOR_ALLOWLIST,
		name = "Door allowlist (id or name)",
		description = "Doors/gates the farmer may click Open/Close on, separated by |, comma, semicolon, or newlines (object id or exact name). Empty means no automatic door interaction; unknown doors report manual-action-required.",
		section = mobFarmerSection
	)
	default String mobFarmerDoorAllowlist()
	{
		return "";
	}

	@ConfigItem(
		keyName = MOB_FARMER_DOOR_DENYLIST,
		name = "Door denylist (id or name)",
		description = "Doors/gates the farmer must never click Open/Close on or path through, separated by |, comma, semicolon, or newlines (object id or exact name). A denylisted door always reports blocked-by-door/manual-action-required, even if auto-open/auto-close is enabled. Use this for locked, dangerous, quest, or wilderness doors.",
		section = mobFarmerSection
	)
	default String mobFarmerDoorDenylist()
	{
		return "";
	}

	@ConfigItem(
		keyName = MOB_FARMER_AUTO_RESUME_AFTER_LOGIN,
		name = "Auto-resume after login",
		description = "Keep the farmer loop alive through login recovery so it resumes after successful login. Disable to stop the farmer when logout/disconnect is detected.",
		section = mobFarmerSection
	)
	default boolean mobFarmerAutoResumeAfterLogin()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_PREFERRED_LOGIN_WORLD,
		name = "Preferred login world",
		description = "Preferred safe F2P world for local development status/reporting. The recovery guard still validates the actual current world before clicking.",
		section = mobFarmerSection
	)
	default int mobFarmerPreferredLoginWorld()
	{
		return 326;
	}

	@ConfigItem(
		keyName = AUTO_LOGIN_ON_LAUNCH,
		name = "Auto-login on launch",
		description = "Automatically attempt login when RuneLite launches and detects the login screen. Works independently of macro state.",
		section = mobFarmerSection
	)
	default boolean autoLoginOnLaunch()
	{
		return false;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_ENABLED,
		name = "Loot pickup enabled",
		description = "Allow the farmer loop to pick up matching or valuable ground items.",
		section = mobFarmerSection
	)
	default boolean mobFarmerLootEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_DURING_COMBAT,
		name = "Loot during combat",
		description = "Allow pickup attempts while already fighting. This supports attacking the next mob first, then collecting drops during combat windows.",
		section = mobFarmerSection
	)
	default boolean mobFarmerLootDuringCombat()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_ATTACK_BEFORE_LOOT,
		name = "Attack before loot",
		description = "When not in combat and both a target and loot are available, attack first and leave loot for later loop steps.",
		section = mobFarmerSection
	)
	default boolean mobFarmerAttackBeforeLoot()
	{
		return true;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_MIN_VALUE_GE,
		name = "Loot min GE value",
		description = "Minimum total GE value to pick up when an item is not explicitly listed. Use 0 to allow all non-blacklisted items.",
		section = mobFarmerSection
	)
	default int mobFarmerLootMinValueGe()
	{
		return 100;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_MIN_SINGLE_GE,
		name = "Min single-item GE",
		description = "Minimum GE value per individual item before unlisted loot is eligible. Use 0 to disable the per-item guard.",
		section = mobFarmerSection
	)
	default int mobFarmerLootMinSingleGe()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_MIN_STACK_GE,
		name = "Min stack GE",
		description = "Minimum total GE stack value before unlisted loot is eligible. This is the guard that skips tiny coin/rune/arrow stacks.",
		section = mobFarmerSection
	)
	default int mobFarmerLootMinStackGe()
	{
		return 100;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_MIN_STACK_QUANTITY,
		name = "Min stack quantity",
		description = "Minimum quantity for stackable unlisted items. Use 0 to disable quantity filtering.",
		section = mobFarmerSection
	)
	default int mobFarmerLootMinStackQuantity()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_ALWAYS_STACK_GE,
		name = "Always loot stack GE",
		description = "Treat any stack at or above this total GE value as high-priority loot even if Attack before loot is enabled. Use 0 to disable.",
		section = mobFarmerSection
	)
	default int mobFarmerLootAlwaysStackGe()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_NEVER_STACK_BELOW_GE,
		name = "Never stack below GE",
		description = "Reject unlisted stacks below this total GE value even when other broad value rules would allow them. Explicit always-loot items still pass.",
		section = mobFarmerSection
	)
	default int mobFarmerLootNeverStackBelowGe()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_HIGH_PRIORITY_LOOT_VALUE_GE,
		name = "Priority loot GE",
		description = "Loot at or above this GE value can override Attack before loot. Use 0 to treat all selectable loot as priority.",
		section = mobFarmerSection
	)
	default int mobFarmerHighPriorityLootValueGe()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_URGENT_DESPAWN_TICKS,
		name = "Urgent loot despawn ticks",
		description = "Loot with this many or fewer ticks before despawn can override Attack before loot. Use 0 to disable age urgency.",
		section = mobFarmerSection
	)
	default int mobFarmerLootUrgentDespawnTicks()
	{
		return 30;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_CLEANUP_PILE_COUNT,
		name = "Loot cleanup pile count",
		description = "When at least this many selectable loot piles are visible, cleanup mode can override Attack before loot. Use 0 to disable cleanup override.",
		section = mobFarmerSection
	)
	default int mobFarmerLootCleanupPileCount()
	{
		return 5;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_RADIUS,
		name = "Loot radius",
		description = "Maximum tile distance for loot pickup. Use 0 to disable the radius guard.",
		section = mobFarmerSection
	)
	default int mobFarmerLootRadius()
	{
		return 6;
	}

	@ConfigItem(
		keyName = MOB_FARMER_HIGH_PRIORITY_LOOT_RADIUS,
		name = "Priority loot radius",
		description = "Maximum local path distance for explicit/high-value priority loot. May be larger than normal loot radius because the combat interruption is intentional.",
		section = mobFarmerSection
	)
	default int mobFarmerHighPriorityLootRadius()
	{
		return 12;
	}

	@ConfigItem(
		keyName = MOB_FARMER_NORMAL_LOOT_MAX_MISSED_ATTACKS,
		name = "Normal loot missed attacks",
		description = "Maximum estimated attack opportunities normal loot may cost while a combat target is active. Priority loot is exempt. Use 0 to preserve every expected attack window.",
		section = mobFarmerSection
	)
	default int mobFarmerNormalLootMaxMissedAttacks()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_ITEMS,
		name = "Always-loot items",
		description = "Items to loot even below the value threshold, separated by |, comma, semicolon, or newlines.",
		section = mobFarmerSection
	)
	default String mobFarmerLootItems()
	{
		return "bones|cowhide|coins|goblin mail";
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_BLACKLIST,
		name = "Never-loot items",
		description = "Items to never pick up, separated by |, comma, semicolon, or newlines.",
		section = mobFarmerSection
	)
	default String mobFarmerLootBlacklist()
	{
		return "";
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_OWNERSHIP_MODE,
		name = "Loot ownership",
		description = "Which ground-item ownership categories the farmer may pick up.",
		section = mobFarmerSection
	)
	default CvHelperLootOwnershipMode mobFarmerLootOwnershipMode()
	{
		return CvHelperLootOwnershipMode.OWN_OR_PUBLIC;
	}

	@ConfigItem(
		keyName = MOB_FARMER_ATTACK_INTERACTION_MODE,
		name = "Attack interaction",
		description = "How the farmer invokes NPC attacks. MENU_ACTION uses the RuneLite menu-entry path; DIRECT_CLICK physically clicks canvas coordinates.",
		section = mobFarmerSection
	)
	default CvHelperMobInteractionMode mobFarmerAttackInteractionMode()
	{
		return CvHelperMobInteractionMode.MENU_ACTION;
	}

	@ConfigItem(
		keyName = MOB_FARMER_LOOT_INTERACTION_MODE,
		name = "Loot interaction",
		description = "How the farmer takes ground items. MENU_ACTION can take hidden or deprioritized Ground Items entries without relying on left-click visibility.",
		section = mobFarmerSection
	)
	default CvHelperMobInteractionMode mobFarmerLootInteractionMode()
	{
		return CvHelperMobInteractionMode.MENU_ACTION;
	}

	@ConfigItem(
		keyName = MOB_FARMER_GROUND_ITEMS_MODE,
		name = "Ground Items lists",
		description = "OFF ignores Ground Items plugin lists. SUPPLEMENT treats highlighted Ground Items as always-loot candidates and reports hidden/show-highlighted-only/value-hide metadata.",
		section = mobFarmerSection
	)
	default CvHelperGroundItemsMode mobFarmerGroundItemsMode()
	{
		return CvHelperGroundItemsMode.SUPPLEMENT;
	}

	@ConfigItem(
		keyName = MOB_FARMER_RESPECT_GROUND_ITEMS_HIDDEN,
		name = "Respect hidden Ground Items",
		description = "When enabled, Ground Items hidden-list matches are treated as never-loot unless also explicitly listed in CV Helper's always-loot list.",
		section = mobFarmerSection
	)
	default boolean mobFarmerRespectGroundItemsHidden()
	{
		return false;
	}

	@ConfigItem(
		keyName = MOB_FARMER_INTERMEDIATE_ACTIONS_ENABLED,
		name = "Use intermediate actions",
		description = "During safe loop windows, open inventory and invoke configured item actions. Missing required actions are skipped rather than using or dropping the item.",
		section = mobFarmerSection
	)
	default boolean mobFarmerIntermediateActionsEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = MOB_FARMER_INTERMEDIATE_ITEMS,
		name = "Intermediate items",
		description = "Items to use from inventory during farming, such as bones or ashes. Supports names or id:<item id>, separated by |, comma, semicolon, or newlines.",
		section = mobFarmerSection
	)
	default String mobFarmerIntermediateItems()
	{
		return "bones|big bones|ashes";
	}

	@ConfigItem(
		keyName = MOB_FARMER_INTERMEDIATE_ACTION_MAPPINGS,
		name = "Intermediate mappings",
		description = "Inventory action mappings, one per line or separated by semicolon. Example: bones -> Bury; ashes -> Scatter|Bury. Drop is never allowed for intermediate actions.",
		section = mobFarmerSection
	)
	default String mobFarmerIntermediateActionMappings()
	{
		return "bones -> Bury; big bones -> Bury; ashes -> Scatter|Bury";
	}

	@ConfigItem(
		keyName = MOB_FARMER_MIN_BURY_BATCH,
		name = "Min bury batch",
		description = "Only start burying/scattering once this many matching items have accumulated, then clear the whole batch in one downtime session (fewer, faster interruptions). Always overridden when the inventory is full. 1 = bury every downtime.",
		section = mobFarmerSection
	)
	default int mobFarmerMinBuryBatch()
	{
		return 4;
	}

	@ConfigItem(
		keyName = MOB_FARMER_COMBAT_INTERRUPT_ITEMS,
		name = "Extreme-value interrupt loot",
		description = "The ONLY loot allowed to interrupt an in-progress attack (everything else waits for downtime). Names or id:<item id>, separated by |, comma, semicolon, or newlines. Leave empty so attacks are never interrupted for loot.",
		section = mobFarmerSection
	)
	default String mobFarmerCombatInterruptItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = MOB_FARMER_NEVER_DROP_ITEMS,
		name = "Protected inventory",
		description = "Inventory items that future drop-processing must never drop. Built-in rare/unique safeguards are always protected (for example clue scroll rewards, keys, totems, shards, champion scroll, long/curved bones). Current implementation reports protected/drop candidates but does not drop items yet.",
		section = mobFarmerSection
	)
	default String mobFarmerNeverDropItems()
	{
		return "rune pouch|coins";
	}

	@ConfigItem(
		keyName = MOB_FARMER_DROP_ITEMS,
		name = "Drop allowlist",
		description = "Items that are safe to drop when inventory space is needed. If empty, any non-protected item below the max drop value is a candidate.",
		section = mobFarmerSection
	)
	default String mobFarmerDropItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = MOB_FARMER_MAX_DROP_VALUE,
		name = "Max drop value",
		description = "Maximum GE value of an item that can be dropped automatically. Items above this value are protected even if not in the protected list.",
		section = mobFarmerSection
	)
	default int mobFarmerMaxDropValue()
	{
		return 100;
	}

	@ConfigItem(
		keyName = MOB_FARMER_HIGH_ALCH_ENABLED,
		name = "High Alch policy",
		description = "Evaluate safe High Alchemy candidates while farming. Current pass reports candidates and safety/availability; invocation stays disabled unless all guards pass in a future live-cast pass.",
		section = mobFarmerSection
	)
	default boolean mobFarmerHighAlchEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = MOB_FARMER_HIGH_ALCH_MIN_HA,
		name = "Min HA value",
		description = "Minimum single-item High Alchemy value for candidate reporting.",
		section = mobFarmerSection
	)
	default int mobFarmerHighAlchMinHa()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_HIGH_ALCH_MIN_DELTA,
		name = "Min HA delta",
		description = "Require HA value minus GE value to be at least this amount. Use 0 to allow break-even, negative values through max-loss, and positive values for profit-only alching.",
		section = mobFarmerSection
	)
	default int mobFarmerHighAlchMinDelta()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_HIGH_ALCH_MAX_LOSS,
		name = "Max HA loss",
		description = "Maximum acceptable GE-to-HA loss per item when inventory space matters. Use 0 for no loss.",
		section = mobFarmerSection
	)
	default int mobFarmerHighAlchMaxLoss()
	{
		return 0;
	}

	@ConfigItem(
		keyName = MOB_FARMER_HIGH_ALCH_ITEMS,
		name = "Alch allowlist",
		description = "If non-empty, only these item names or id:<item id> are eligible for High Alchemy.",
		section = mobFarmerSection
	)
	default String mobFarmerHighAlchItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = MOB_FARMER_HIGH_ALCH_BLACKLIST,
		name = "Never alch",
		description = "Items that must never be high-alched. Food, protected inventory items, and useful items should be listed here.",
		section = mobFarmerSection
	)
	default String mobFarmerHighAlchBlacklist()
	{
		return "coins|rune pouch|law rune|nature rune|air rune|fire rune|water rune|earth rune|mind rune|body rune|chaos rune|death rune|blood rune|soul rune|astral rune|wrath rune|teleport|tab|shark|lobster|swordfish|monkfish|karambwan";
	}

	@ConfigItem(
		keyName = ANTI_IDLE_ENABLED,
		name = "Anti-idle timeout prevention",
		description = "Extend the idle logout timer and periodically move/click the mouse on the character to prevent idle logout while farmers are running.",
		section = mobFarmerSection
	)
	default boolean antiIdleEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = ANTI_IDLE_TIMEOUT_MINUTES,
		name = "Anti-idle timeout",
		description = "Idle logout timeout duration when anti-idle is active. RuneLite hard-caps this at 30 minutes.",
		section = mobFarmerSection
	)
	@Units(Units.MINUTES)
	@Range(min = 5, max = 30)
	default int antiIdleTimeoutMinutes()
	{
		return 30;
	}

	@ConfigItem(
		keyName = ANTI_IDLE_INPUT_INTERVAL_MINUTES,
		name = "Anti-idle input interval",
		description = "How often to move and click the mouse on the character to reset idle ticks. Must be less than the timeout.",
		section = mobFarmerSection
	)
	@Units(Units.MINUTES)
	@Range(min = 1, max = 28)
	default int antiIdleInputIntervalMinutes()
	{
		return 4;
	}

	@ConfigItem(
		keyName = ANTI_IDLE_MANUAL_OVERRIDE,
		name = "Manual anti-idle override",
		description = "Keep anti-idle active even when no farmers are running.",
		section = mobFarmerSection
	)
	default boolean antiIdleManualOverride()
	{
		return false;
	}

	@ConfigItem(
		keyName = ANTI_IDLE_RESTORE_MOUSE,
		name = "Restore mouse position",
		description = "Return the OS mouse to its original position after each anti-idle input.",
		section = mobFarmerSection
	)
	default boolean antiIdleRestoreMouse()
	{
		return true;
	}

	@ConfigItem(
		keyName = "woodcutterInventoryMode",
		name = "Inventory Handling",
		description = "What to do when the inventory is full: do nothing, drop logs, or bank.",
		section = woodcutterSection
	)
	default CvHelperWoodcutterInventoryMode woodcutterInventoryMode()
	{
		return CvHelperWoodcutterInventoryMode.DROP;
	}

	@ConfigItem(
		keyName = "woodcutterDropItemNames",
		name = "Drop Items",
		description = "Comma-separated item names that the woodcutter is allowed to drop.",
		section = woodcutterSection
	)
	default String woodcutterDropItemNames()
	{
		return "Logs, Oak logs, Willow logs, Teak logs, Maple logs, Mahogany logs, Yew logs, Magic logs, Redwood logs";
	}

	@ConfigItem(
		keyName = "woodcutterMaxDropValue",
		name = "Max Drop Value",
		description = "Do not drop items worth more than this many GP.",
		section = woodcutterSection
	)
	default int woodcutterMaxDropValue()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = "woodcutterInventoryTriggerSlots",
		name = "Inventory Trigger Slots",
		description = "Start dropping or banking when this many inventory slots are occupied.",
		section = woodcutterSection
	)
	default int woodcutterInventoryTriggerSlots()
	{
		return 28;
	}

	@ConfigItem(
		keyName = "woodcutterBankItemNames",
		name = "Bank Items",
		description = "Comma-separated item names that the woodcutter should deposit when banking.",
		section = woodcutterSection
	)
	default String woodcutterBankItemNames()
	{
		return "Logs, Oak logs, Willow logs, Teak logs, Maple logs, Mahogany logs, Yew logs, Magic logs, Redwood logs";
	}

	@ConfigItem(
		keyName = WOODCUTTING_FARMER_TARGET,
		name = "Woodcutting target",
		description = "Tree/log names, id:<object id>, or a list separated by |, comma, semicolon, or newlines. Persists across restarts; the WebHelper config tab and presets write here.",
		section = woodcutterSection
	)
	default String woodcuttingFarmerTarget()
	{
		return "oak|tree|willow|maple";
	}

	@ConfigItem(
		keyName = WOODCUTTING_SCAN_RADIUS,
		name = "Woodcutting scan radius (tiles)",
		description = "Maximum distance in tiles to search for trees.",
		section = woodcutterSection
	)
	@Range(
		min = 1,
		max = 64
	)
	default int woodcuttingScanRadius()
	{
		return 5;
	}

	@ConfigItem(
		keyName = WOODCUTTING_MAX_CANDIDATES,
		name = "Woodcutting max candidates",
		description = "Maximum number of tree candidates to display/evaluate in detail. All reachable trees within the scan radius are still considered when picking a target; this only caps how many are shown.",
		section = woodcutterSection
	)
	@Range(
		min = 1,
		max = 300
	)
	default int woodcuttingMaxCandidates()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "woodcuttingStickToTarget",
		name = "Stick to current tree",
		description = "While actively chopping a tree that still matches the target list, do not switch to a different tree. This avoids wasting ticks re-targeting and prevents re-clicking the same tree.",
		section = woodcutterSection
	)
	default boolean woodcuttingStickToTarget()
	{
		return true;
	}

	@ConfigItem(
		keyName = "woodcuttingReclickWhenActivelyChopping",
		name = "Re-click while chopping",
		description = "When enabled, the farmer will continue sending Chop down clicks while the woodcutting animation is running. Most players want this OFF because the click interrupts are slower than letting the axe swing uninterrupted.",
		section = woodcutterSection
	)
	default boolean woodcuttingReclickWhenActivelyChopping()
	{
		return false;
	}

	@ConfigItem(
		keyName = MINING_FARMER_TARGET,
		name = "Mining target",
		description = "Rock/ore names, id:<object id>, or a list separated by |, comma, semicolon, or newlines. Persists across restarts; the WebHelper config tab and presets write here.",
		section = minerSection
	)
	default String miningFarmerTarget()
	{
		return "iron rocks|iron ore rocks|rocks";
	}

	@ConfigItem(
		keyName = MINING_SCAN_RADIUS,
		name = "Mining scan radius (tiles)",
		description = "Maximum distance in tiles to search for mining rocks. Set to a smaller value (e.g., 3-5) to stay within a rock clump and avoid running to distant rocks during respawn delays.",
		section = minerSection
	)
	@Range(
		min = 1,
		max = 64
	)
	default int miningScanRadius()
	{
		return 5;
	}

	@ConfigItem(
		keyName = MINING_MAX_CANDIDATES,
		name = "Mining max candidates",
		description = "Maximum number of mining rock candidates to display/evaluate in detail. All reachable rocks within the scan radius are still considered when picking a target; this only caps how many are shown.",
		section = minerSection
	)
	@Range(
		min = 1,
		max = 300
	)
	default int miningMaxCandidates()
	{
		return 10;
	}

	@ConfigItem(
		keyName = MINING_DROP_ITEMS,
		name = "Mining droppable items",
		description = "Items the mining farmer may drop when conditions are met (separate from woodcutting's list). If empty, any non-protected item below max value is a candidate. Separated by |, comma, semicolon, or newlines.",
		section = minerSection
	)
	default String miningDropItems()
	{
		return "ore|coal|clay|essence|sandstone|granite|pay-dirt";
	}

	@ConfigItem(
		keyName = DROP_POLICY_ENABLED,
		name = "Drop policy enabled",
		description = "Enable the conditional drop policy for skill farmers. When disabled, farmers use their legacy inventory handling.",
		section = woodcutterSection
	)
	default boolean dropPolicyEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = DROP_POLICY_MODE,
		name = "Drop mode",
		description = "When to drop items: NEVER disables dropping; WHEN_FULL drops only when inventory is full; WHEN_IDLE drops when not actively chopping (batch drop while moving between trees); AFTER_TARGET drops after each target cycle completes; AFTER_GATHER drops after each successful gather action; CLEANUP_ONLY drops only during explicit cleanup phases; MANUAL_ONLY requires manual invocation.",
		section = woodcutterSection
	)
	default CvHelperDropMode dropPolicyMode()
	{
		return CvHelperDropMode.WHEN_IDLE;
	}

	@ConfigItem(
		keyName = DROP_POLICY_THRESHOLD_SLOTS,
		name = "Drop threshold slots",
		description = "Minimum occupied inventory slots before dropping is considered. For WHEN_FULL mode, this is typically 28. For other modes, lower values allow earlier cleanup.",
		section = woodcutterSection
	)
	default int dropPolicyThresholdSlots()
	{
		return 28;
	}

	@ConfigItem(
		keyName = DROP_POLICY_ITEMS,
		name = "Droppable items",
		description = "Items that are safe to drop when conditions are met. If empty, any non-protected item below max value is a candidate. Separated by |, comma, semicolon, or newlines.",
		section = woodcutterSection
	)
	default String dropPolicyItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = DROP_POLICY_PROTECTED_ITEMS,
		name = "Protected items",
		description = "Items that must never be dropped. Tools, food, teleport items, runes, and valuable items should be listed here. Built-in safeguards always protect clue/rare unique items.",
		section = woodcutterSection
	)
	default String dropPolicyProtectedItems()
	{
		return "bronze pickaxe|iron pickaxe|steel pickaxe|mithril pickaxe|adamant pickaxe|rune pickaxe|bronze axe|iron axe|steel axe|mithril axe|adamant axe|rune axe|bronze hatchet|iron hatchet|steel hatchet|mithril hatchet|adamant hatchet|rune hatchet|shrimp|trout|salmon|tuna|lobster|swordfish|monkfish|shark|manta ray|anglerfish|cake|jug of wine|karambwan|meat|chicken|bread|pizza|pie|teleport|tab|rune pouch|coins";
	}

	@ConfigItem(
		keyName = DROP_POLICY_MAX_VALUE,
		name = "Max drop value",
		description = "Maximum GE value of an item that can be dropped automatically. Items above this value are protected even if not in the protected list.",
		section = woodcutterSection
	)
	default int dropPolicyMaxValue()
	{
		return 1000;
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
		return new Keybind(java.awt.event.KeyEvent.VK_1, 0);
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
		section = actionSection,
		hidden = true
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
		description = "How action slot 1 selects its target: AUTO uses widget actions for prayers/teleports and physical selection for targeted spells; CLICK forces Java Robot.",
		section = actionSection
	)
	default CvHelperActionInvocationMode actionInvocationMode1()
	{
		return CvHelperActionInvocationMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_PRAYER_MODE_1,
		name = "Action 1 prayer mode",
		description = "TOGGLE clicks normally; ON_ONLY skips if the prayer is already active; OFF_ONLY skips if it is already inactive.",
		section = actionSection
	)
	default CvHelperPrayerActionMode actionPrayerMode1()
	{
		return CvHelperPrayerActionMode.TOGGLE;
	}

	@ConfigItem(
		keyName = ACTION_SPELL_AVAILABILITY_MODE_1,
		name = "Action 1 spell guard",
		description = "Guard against clearly unavailable/shaded spells before attempting the action.",
		section = actionSection
	)
	default CvHelperSpellAvailabilityMode actionSpellAvailabilityMode1()
	{
		return CvHelperSpellAvailabilityMode.GUARD_UNAVAILABLE;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_1,
		name = "Action 1 return previous tab",
		description = "After action slot 1 finishes, switch back to the previously open side panel. Function keys are used when possible.",
		section = actionSection
	)
	default boolean actionReturnPanel1()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_MOUSE_CENTER_1,
		name = "Action 1 restore mouse position",
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
		return new Keybind(java.awt.event.KeyEvent.VK_2, 0);
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
		section = actionSection,
		hidden = true
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
		description = "How action slot 2 selects its target: AUTO uses widget actions for prayers/teleports and physical selection for targeted spells; CLICK forces Java Robot.",
		section = actionSection
	)
	default CvHelperActionInvocationMode actionInvocationMode2()
	{
		return CvHelperActionInvocationMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_PRAYER_MODE_2,
		name = "Action 2 prayer mode",
		description = "TOGGLE clicks normally; ON_ONLY skips if the prayer is already active; OFF_ONLY skips if it is already inactive.",
		section = actionSection
	)
	default CvHelperPrayerActionMode actionPrayerMode2()
	{
		return CvHelperPrayerActionMode.TOGGLE;
	}

	@ConfigItem(
		keyName = ACTION_SPELL_AVAILABILITY_MODE_2,
		name = "Action 2 spell guard",
		description = "Guard against clearly unavailable/shaded spells before attempting the action.",
		section = actionSection
	)
	default CvHelperSpellAvailabilityMode actionSpellAvailabilityMode2()
	{
		return CvHelperSpellAvailabilityMode.GUARD_UNAVAILABLE;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_2,
		name = "Action 2 return previous tab",
		description = "After action slot 2 finishes, switch back to the previously open side panel. Function keys are used when possible.",
		section = actionSection
	)
	default boolean actionReturnPanel2()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_MOUSE_CENTER_2,
		name = "Action 2 restore mouse position",
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
		return new Keybind(java.awt.event.KeyEvent.VK_3, 0);
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
		section = actionSection,
		hidden = true
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
		description = "How action slot 3 selects its target: AUTO uses widget actions for prayers/teleports and physical selection for targeted spells; CLICK forces Java Robot.",
		section = actionSection
	)
	default CvHelperActionInvocationMode actionInvocationMode3()
	{
		return CvHelperActionInvocationMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_PRAYER_MODE_3,
		name = "Action 3 prayer mode",
		description = "TOGGLE clicks normally; ON_ONLY skips if the prayer is already active; OFF_ONLY skips if it is already inactive.",
		section = actionSection
	)
	default CvHelperPrayerActionMode actionPrayerMode3()
	{
		return CvHelperPrayerActionMode.TOGGLE;
	}

	@ConfigItem(
		keyName = ACTION_SPELL_AVAILABILITY_MODE_3,
		name = "Action 3 spell guard",
		description = "Guard against clearly unavailable/shaded spells before attempting the action.",
		section = actionSection
	)
	default CvHelperSpellAvailabilityMode actionSpellAvailabilityMode3()
	{
		return CvHelperSpellAvailabilityMode.GUARD_UNAVAILABLE;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_3,
		name = "Action 3 return previous tab",
		description = "After action slot 3 finishes, switch back to the previously open side panel. Function keys are used when possible.",
		section = actionSection
	)
	default boolean actionReturnPanel3()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_MOUSE_CENTER_3,
		name = "Action 3 restore mouse position",
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
		return new Keybind(java.awt.event.KeyEvent.VK_4, 0);
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
		section = actionSection,
		hidden = true
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
		description = "How action slot 4 selects its target: AUTO uses widget actions for prayers/teleports and physical selection for targeted spells; CLICK forces Java Robot.",
		section = actionSection
	)
	default CvHelperActionInvocationMode actionInvocationMode4()
	{
		return CvHelperActionInvocationMode.AUTO;
	}

	@ConfigItem(
		keyName = ACTION_PRAYER_MODE_4,
		name = "Action 4 prayer mode",
		description = "TOGGLE clicks normally; ON_ONLY skips if the prayer is already active; OFF_ONLY skips if it is already inactive.",
		section = actionSection
	)
	default CvHelperPrayerActionMode actionPrayerMode4()
	{
		return CvHelperPrayerActionMode.TOGGLE;
	}

	@ConfigItem(
		keyName = ACTION_SPELL_AVAILABILITY_MODE_4,
		name = "Action 4 spell guard",
		description = "Guard against clearly unavailable/shaded spells before attempting the action.",
		section = actionSection
	)
	default CvHelperSpellAvailabilityMode actionSpellAvailabilityMode4()
	{
		return CvHelperSpellAvailabilityMode.GUARD_UNAVAILABLE;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_PANEL_4,
		name = "Action 4 return previous tab",
		description = "After action slot 4 finishes, switch back to the previously open side panel. Function keys are used when possible.",
		section = actionSection
	)
	default boolean actionReturnPanel4()
	{
		return true;
	}

	@ConfigItem(
		keyName = ACTION_RETURN_MOUSE_CENTER_4,
		name = "Action 4 restore mouse position",
		description = "After action slot 4 finishes, restore the mouse to its original screen position.",
		section = actionSection
	)
	default boolean actionReturnMouseCenter4()
	{
		return true;
	}

	@ConfigItem(
		keyName = CHAT_RESPONDER_ENABLED,
		name = "Enable chat responder",
		description = "Enable the experimental AI chat responder. Requires OPENAI_API_KEY environment variable.",
		section = chatResponderSection
	)
	default boolean chatResponderEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = CHAT_RESPONDER_MIN_MESSAGES,
		name = "Minimum messages",
		description = "Wait until this many non-self messages are seen in the window before generating a response.",
		section = chatResponderSection
	)
	default int chatResponderMinMessages()
	{
		return 3;
	}

	@ConfigItem(
		keyName = CHAT_RESPONDER_WINDOW_SECONDS,
		name = "Message window seconds",
		description = "Only consider messages from the last N seconds when deciding whether to respond.",
		section = chatResponderSection
	)
	default int chatResponderWindowSeconds()
	{
		return 20;
	}

	@ConfigItem(
		keyName = CHAT_RESPONDER_COOLDOWN_SECONDS,
		name = "Cooldown seconds",
		description = "Minimum time between AI responses.",
		section = chatResponderSection
	)
	default int chatResponderCooldownSeconds()
	{
		return 15;
	}

	@ConfigItem(
		keyName = CHAT_RESPONDER_MODEL,
		name = "OpenAI model",
		description = "Chat model to use. Leave empty for the default smallest model (gpt-4o-mini).",
		section = chatResponderSection
	)
	default String chatResponderModel()
	{
		return "gpt-4o-mini";
	}

	@ConfigItem(
		keyName = CHAT_RESPONDER_PROMPT,
		name = "Custom prompt",
		description = "Optional override for the system prompt. Leave empty for the built-in OSRS personality prompt.",
		section = chatResponderSection
	)
	default String chatResponderPrompt()
	{
		return "";
	}
}
