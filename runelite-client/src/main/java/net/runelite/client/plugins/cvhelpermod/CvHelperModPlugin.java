/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;
import static net.runelite.client.plugins.cvhelpermod.CvJson.boundsMap;
import static net.runelite.client.plugins.cvhelpermod.CvJson.pointMap;
import static net.runelite.client.plugins.cvhelpermod.CvJson.longValue;
import static net.runelite.client.plugins.cvhelpermod.CvJson.intValue;
import static net.runelite.client.plugins.cvhelpermod.CvJson.mapValue;
import static net.runelite.client.plugins.cvhelpermod.CvJson.friendlyName;
import static net.runelite.client.plugins.cvhelpermod.CvJson.actionsText;
import static net.runelite.client.plugins.cvhelpermod.CvJson.normalize;
import static net.runelite.client.plugins.cvhelpermod.CvJson.safeValue;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.inject.Inject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.EnumID;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.VarPlayer;
import net.runelite.api.World;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.config.Keybind;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.WildcardMatcher;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
@PluginDescriptor(
	name = "CV Helper (Modular)",
	description = "Highlights hovered UI areas and prepares coordinate capture for CV extraction.",
	tags = {"overlay", "ui", "coordinates", "debug"},
	enabledByDefault = false
)
@Slf4j
public class CvHelperModPlugin extends Plugin
{
	protected static final String TOOLTIP = "CV Helper";
	protected static final String FORCE_LOCAL_EXPORT_PROPERTY = "cvhelper.forceLocalExport";
	protected static final int DEFAULT_LOCAL_PORT = 11777;
	protected static final int ACTION_SLOT_COUNT = 22;
	protected static final int ACTION_RESOLVE_RETRIES = 4;
	protected static final int DEFAULT_MOB_FARMER_RECOVERY_LOOP_DELAY_MS = 1200;
	protected static final int RUN_ENABLED_VARP = 173;
	protected static final long MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS = 5000L;
	protected static final int MOB_FARMER_PROGRESS_WINDOW_TICKS = 8;
	protected static final int MOB_FARMER_LOOT_SPAWN_GRACE_TICKS = 3;
	protected static final int MOB_FARMER_COMBAT_STABILIZE_TICKS = 4;
	protected static final int MOB_FARMER_ATTACK_REISSUE_MIN_TICKS = 6;
	protected static final int MOB_FARMER_LOOT_RESOLUTION_MAX_TICKS = 6;
	protected static final int MOB_FARMER_UNRESOLVED_LOOT_SKIP_TICKS = 30;
	protected static final int MOB_FARMER_PATHING_SLACK_TILES = 8;
	protected static final int MOB_FARMER_PATHING_MAX_SEARCH_TILES = 64;
	protected static final int SKILL_FARMER_SCAN_RADIUS_TILES = 24;
	protected static final int SKILL_FARMER_MAX_CANDIDATES = 80;
	protected static final int SKILL_FARMER_PATH_SEARCH_TILES = 30;
	// Hard safety ceiling on objects evaluated per scan, independent of the
	// user-configurable maxCandidates display cap (see collectSkillFarmerObjects).
	protected static final int SKILL_FARMER_HARD_CAP = 500;
	protected static final String MOB_FARMER_IMPLICIT_NEVER_DROP_ITEMS = String.join("|",
		"rune pouch",
		"coins",
		"clue scroll",
		"clue geode",
		"clue nest",
		"clue bottle",
		"casket",
		"reward casket",
		"clue box",
		"giant key",
		"mossy key",
		"brimstone key",
		"larran's key",
		"crystal key",
		"enhanced crystal key",
		"dark totem",
		"ancient shard",
		"champion's scroll",
		"long bone",
		"curved bone",
		"jar of");

	protected enum LoginRecoveryState
	{
		IN_GAME,
		LOGIN_SCREEN,
		DISCONNECTED,
		CLICK_TO_PLAY,
		PLAY_NOW,
		WORLD_SELECT_REQUIRED,
		WORLD_SWITCH_REQUIRED,
		AUTH_REQUIRED_MANUAL,
		LOADING,
		RECOVERY_BLOCKED,
		UNKNOWN_LOGIN_STATE
	}

	protected static final int DEFAULT_F2P_WORLD = 301;
	protected static final int DEFAULT_P2P_WORLD = 318;
	// Verified F2P worlds from RuneLite world list
	protected static final int[] FALLBACK_F2P_WORLDS = {301, 308, 316, 335, 381, 382, 383, 384, 393, 394, 395, 396, 498, 499};
	// Verified P2P worlds from RuneLite world list  
	protected static final int[] FALLBACK_P2P_WORLDS = {318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499, 500};

	protected static final int[][] MOB_FARMER_PATH_DIRECTIONS = {
		{1, 0},
		{-1, 0},
		{0, 1},
		{0, -1},
		{1, 1},
		{1, -1},
		{-1, 1},
		{-1, -1}
	};
	protected static final int[] WOODCUTTING_ANIMATION_IDS = {
		879,
		880,
		881,
		882,
		883,
		884,
		885,
		11965,
		11966,
		11967
	};
	protected static final int[] MINING_ANIMATION_IDS = {
		625,
		626,
		627,
		628,
		629,
		630,
		631,
		643,
		7139,
		7284,
		8313,
		8346,
		8347
	};
	protected static final String GROUND_ITEMS_CONFIG_GROUP = "grounditems";
	protected static final String GROUND_ITEMS_HIGHLIGHTED_ITEMS = "highlightedItems";
	protected static final String GROUND_ITEMS_HIDDEN_ITEMS = "hiddenItems";
	protected static final String GROUND_ITEMS_SHOW_HIGHLIGHTED_ONLY = "showHighlightedOnly";
	protected static final String GROUND_ITEMS_HIDE_UNDER_VALUE = "hideUnderValue";
	protected static final String GROUND_ITEMS_DONT_HIDE_UNTRADEABLES = "dontHideUntradeables";
	protected static final int GROUND_ITEMS_NONE = 0;
	protected static final int GROUND_ITEMS_WILDCARD = 1;
	protected static final int GROUND_ITEMS_EXACT = 2;
	protected static final int DEFAULT_IDLE_TIMEOUT_MINUTES = 5;
	protected static final int MAX_IDLE_TIMEOUT_MINUTES = 30;
	protected static final int MIN_IDLE_TIMEOUT_MINUTES = 5;
	protected static final int CLIENT_TICKS_PER_MINUTE = 60 * 1000 / Constants.CLIENT_TICK_LENGTH;
	protected static final int DEFAULT_ANTI_IDLE_INPUT_INTERVAL_MINUTES = 4;
	protected static final DateTimeFormatter CAPTURE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	protected static final int[] PRAYER_COMPONENTS = {
		InterfaceID.Prayerbook.PRAYER1,
		InterfaceID.Prayerbook.PRAYER2,
		InterfaceID.Prayerbook.PRAYER3,
		InterfaceID.Prayerbook.PRAYER4,
		InterfaceID.Prayerbook.PRAYER5,
		InterfaceID.Prayerbook.PRAYER6,
		InterfaceID.Prayerbook.PRAYER7,
		InterfaceID.Prayerbook.PRAYER8,
		InterfaceID.Prayerbook.PRAYER9,
		InterfaceID.Prayerbook.PRAYER10,
		InterfaceID.Prayerbook.PRAYER11,
		InterfaceID.Prayerbook.PRAYER12,
		InterfaceID.Prayerbook.PRAYER13,
		InterfaceID.Prayerbook.PRAYER14,
		InterfaceID.Prayerbook.PRAYER15,
		InterfaceID.Prayerbook.PRAYER16,
		InterfaceID.Prayerbook.PRAYER17,
		InterfaceID.Prayerbook.PRAYER18,
		InterfaceID.Prayerbook.PRAYER19,
		InterfaceID.Prayerbook.PRAYER20,
		InterfaceID.Prayerbook.PRAYER21,
		InterfaceID.Prayerbook.PRAYER22,
		InterfaceID.Prayerbook.PRAYER23,
		InterfaceID.Prayerbook.PRAYER24,
		InterfaceID.Prayerbook.PRAYER25,
		InterfaceID.Prayerbook.PRAYER26,
		InterfaceID.Prayerbook.PRAYER27,
		InterfaceID.Prayerbook.PRAYER28,
		InterfaceID.Prayerbook.PRAYER29,
		InterfaceID.Prayerbook.PRAYER30
	};

	@Inject
	protected OverlayManager overlayManager;

	@Inject
	protected Client client;

	@Inject
	protected CvHelperModOverlay overlay;

	@Inject
	protected CvHelperModConfig config;

	@Inject
	protected ConfigManager configManager;

	@Inject
	protected Gson gson;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected ClientToolbar clientToolbar;

	@Inject
	protected DrawManager drawManager;

	@Inject
	protected ClientThread clientThread;

	@Inject
	protected KeyManager keyManager;

	@Inject
	protected ItemManager itemManager;

	@Inject
	protected ItemSafetyService itemSafetyService;

	@Inject
	protected InventoryDropService inventoryDropService;

	@Inject
	protected PathfindingService pathfinding;

	@Inject
	protected ChatResponderService chatResponderService;

	protected NavigationButton navButton;
	protected CvHelperModPanel panel;
	protected HttpServer server;
	protected final AtomicReference<String> lastEvent = new AtomicReference<>("idle");
	protected final AtomicReference<Instant> lastLocalWebHelperRequest = new AtomicReference<>();
	protected volatile List<Map<String, Object>> lastPrayerTargets = new ArrayList<>();
	protected volatile List<Map<String, Object>> lastSpellTargets = new ArrayList<>();
	protected volatile List<Map<String, Object>> lastMinimapTargets = new ArrayList<>();
	protected volatile List<Map<String, Object>> lastInventoryTargets = new ArrayList<>();
	protected volatile List<Map<String, Object>> lastEquipmentTargets = new ArrayList<>();
	protected volatile List<Map<String, Object>> lastPanelTargets = new ArrayList<>();
	protected volatile List<Map<String, Object>> lastCombatTargets = new ArrayList<>();
	protected volatile List<Map<String, Object>> lastEntities = new ArrayList<>();
	protected final Map<String, Map<String, Object>> targetSnapshots = new LinkedHashMap<>();
	protected final Map<String, Map<String, Object>> lastCaptures = new LinkedHashMap<>();
	protected final Set<String> enabledPrayers = new HashSet<>();
	protected final Set<String> enabledSpellbooks = new HashSet<>();
	protected final List<HotkeyListener> actionHotkeyListeners = new ArrayList<>();
	protected final Set<String> preDispatcherPressedKeys = new HashSet<>();
	protected volatile boolean hotkeyChatDisplayVisible;
	protected volatile boolean hotkeyMesLayerAvailable;
	protected volatile boolean hotkeyPressEnterPromptVisible;
	protected volatile boolean hotkeyDefaultChatInputActive;
	protected volatile int hotkeyMesLayerMode = InputType.NONE.getType();
	protected volatile int hotkeyChatInputLength;
	protected volatile int hotkeyMesLayerInputLength;
	protected volatile Map<String, Object> lastHotkeyGuardDecision = new LinkedHashMap<>();
	protected final AtomicBoolean actionInProgress = new AtomicBoolean(false);
	protected final Map<Integer, Integer> actionSequenceIndexes = new HashMap<>();
	protected final AtomicBoolean mobFarmerRunning = new AtomicBoolean(false);
	protected final AtomicInteger mobFarmerGeneration = new AtomicInteger();
	protected final AtomicReference<String> mobFarmerStatus = new AtomicReference<>("idle");
	protected volatile List<Map<String, Object>> lastMobFarmerCandidates = new ArrayList<>();
	protected volatile Map<String, Object> lastMobFarmerDecision = new LinkedHashMap<>();
	protected volatile List<Map<String, Object>> lastMobFarmerLootCandidates = new ArrayList<>();
	protected volatile Map<String, Object> lastMobFarmerLootDecision = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerSurvivalDecision = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerIntermediateDecision = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerActionAttempt = new LinkedHashMap<>();
	protected volatile List<Map<String, Object>> lastMobFarmerMenuEntries = new ArrayList<>();
	protected volatile Map<String, Object> lastMobFarmerInventoryStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerLoginRecovery = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastLoginClickAttempt = new LinkedHashMap<>();
	// World-switch/login diagnostics: what we last tried, whether it failed and why,
	// and whether the real world-list lookup (not a hand-built World) actually resolved.
	protected volatile String lastWorldSwitchAction = null;
	protected volatile String lastWorldSwitchFailureReason = null;
	protected volatile Boolean lastWorldListAvailable = null;
	protected volatile Boolean lastWorldResolvedValid = null;
	protected volatile Map<String, Object> lastMobFarmerProgressStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerSchedulerStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerDeathLootStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerReattackStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerStabilizationStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerAutorunStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerFocusClickStatus = new LinkedHashMap<>();
	protected volatile List<Map<String, Object>> lastMobFarmerIntents = new ArrayList<>();
	protected volatile boolean lastMobFarmerMultiCombat;
	protected volatile Map<String, Object> lastMobFarmerDoorTransitionStatus = new LinkedHashMap<>();
	private volatile String mobFarmerDoorTransitionKey;
	private volatile long mobFarmerDoorTransitionLastClickMillis;
	private volatile int mobFarmerDoorTransitionAttempts;
	private static final long MOB_FARMER_DOOR_TRANSITION_CLICK_COOLDOWN_MS = 1200L;
	private static final int MOB_FARMER_DOOR_TRANSITION_MAX_ATTEMPTS = 5;
	protected volatile String lastStableSidePanel = "inventory";
	protected volatile String mobFarmerTarget = "cow";
	protected volatile boolean mobFarmerLiveMode;
	protected volatile Thread mobFarmerThread;
	protected volatile long lastMobFarmerLoginClickMillis;
	// Dedicated wall-clock recovery thread for Mining/Woodcutting, mirroring Mob
	// Farmer's own recovery-loop thread (see startMobFarmer). onGameTick early-returns
	// whenever the client isn't LOGGED_IN, so a farmer driven purely by game ticks can
	// never recover itself from a disconnect/logout -- it just sits frozen reporting
	// stale status forever. This thread is what actually drives recovery while logged
	// out; runSkillFarmerStep/onGameTick only cover the already-logged-in case.
	protected volatile Thread skillFarmerRecoveryThread;
	protected final AtomicInteger skillFarmerRecoveryGeneration = new AtomicInteger();
	protected volatile String activeMobFarmerLootKey;
	protected volatile int activeMobFarmerLootStartTick;
	protected volatile int activeMobFarmerLootLastDistance = Integer.MAX_VALUE;
	protected volatile boolean activeMobFarmerLootImproving;
	protected volatile int mobFarmerMakeProgressUntilTick;
	protected volatile String activeMobFarmerCombatKey;
	protected volatile int activeMobFarmerCombatUntilTick = -1;
	protected volatile Map<String, Object> activeMobFarmerCombatTarget = new LinkedHashMap<>();
	protected volatile int lastMobFarmerLoopStepTick = -1;
	protected volatile String lastMobFarmerLoopStepSource = "none";
	protected volatile String pendingMobFarmerDeathKey;
	protected volatile int pendingMobFarmerDeathTick = -1;
	protected volatile Map<String, Object> pendingMobFarmerDeathTarget = new LinkedHashMap<>();
	protected volatile boolean mobFarmerReattackAfterPickupPending;
	protected volatile String pendingMobFarmerReattackLootKey;
	protected volatile int pendingMobFarmerReattackLootTick = -1;
	protected volatile int pendingMobFarmerReattackLootWaitTicks = 0;
	protected volatile Map<String, Object> pendingMobFarmerReattackLootTarget = new LinkedHashMap<>();
	protected volatile boolean mobFarmerFocusClickNeeded;
	protected final AtomicBoolean miningFarmerRunning = new AtomicBoolean(false);
	protected final AtomicBoolean woodcuttingFarmerRunning = new AtomicBoolean(false);
	protected volatile boolean miningFarmerLiveMode;
	protected volatile boolean woodcuttingFarmerLiveMode;
	protected volatile String miningFarmerTarget = "iron rocks|iron ore rocks|rocks";
	protected volatile String woodcuttingFarmerTarget = "oak|tree|willow|maple";
	protected volatile int miningFarmerScanRadius = SKILL_FARMER_SCAN_RADIUS_TILES;
	protected volatile int woodcuttingFarmerScanRadius = SKILL_FARMER_SCAN_RADIUS_TILES;
	protected volatile int miningFarmerMaxCandidates = SKILL_FARMER_MAX_CANDIDATES;
	protected volatile int woodcuttingFarmerMaxCandidates = SKILL_FARMER_MAX_CANDIDATES;
	protected volatile Map<String, Object> lastMiningFarmerStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastWoodcuttingFarmerStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastSelectedMiningTarget = null;
	protected volatile Map<String, Object> lastSelectedWoodcuttingTarget = null;
	protected volatile long lastWoodcuttingTargetClickTime = 0L;
	protected volatile long lastMiningTargetClickTime = 0L;
	// XP-drop based mining completion + short-lived depleted-tile blacklist
	protected volatile int lastMiningXp = -1;
	protected volatile Map<String, Object> lastCompletedMiningTarget = null;
	protected volatile String miningCompletionReason = null;
	protected volatile int lastMiningInvalidationTick = -1;
	// Woodcutting target lifecycle diagnostics (object-missing / action-unavailable),
	// same shape as the mining fields above so Debug works identically for both skills.
	protected volatile Map<String, Object> lastCompletedWoodcuttingTarget = null;
	protected volatile String woodcuttingCompletionReason = null;
	protected volatile int lastWoodcuttingInvalidationTick = -1;
	// key "x,y,plane" -> game tick until which the tile is skipped after a successful mine
	protected final Map<String, Integer> miningDepletedTileUntilTick = new java.util.concurrent.ConcurrentHashMap<>();
	// key "skill:x,y,plane" -> game tick this tile was last selected; breaks pathDistance
	// ties round-robin across equally-close candidates (see selectSkillFarmerObject).
	protected final Map<String, Integer> skillFarmerTileLastUsedTick = new java.util.concurrent.ConcurrentHashMap<>();
	// key "skill:x,y,plane" -> last seen object id/tick at that tile, for any candidate
	// (not just our own current target) -- catches a 3rd party depleting a neighbor rock.
	protected final Map<String, Integer> skillFarmerTileLastSeenId = new java.util.concurrent.ConcurrentHashMap<>();
	protected final Map<String, Integer> skillFarmerTileLastSeenTick = new java.util.concurrent.ConcurrentHashMap<>();
	// number of ticks a just-mined tile stays on the cooldown blacklist
	protected static final int MINING_DEPLETED_TILE_COOLDOWN_TICKS = 3;
	protected final Map<String, Integer> lastMobFarmerActionTickByKey = new LinkedHashMap<>();
	protected final Map<String, Integer> mobFarmerLootSkipUntilTickByKey = new LinkedHashMap<>();
	protected volatile boolean antiIdleActive;
	protected volatile String antiIdleMode = "off";
	protected volatile Instant lastAntiIdleInputTime;
	protected volatile Instant antiIdleTimeoutAppliedAt;
	protected volatile int lastAppliedIdleTimeoutTicks = -1;
	protected Robot antiIdleRobot;

	protected static final class PanelSwitchTarget
	{
		protected final String panel;
		protected final Keybind keybind;
		protected final Point clickPoint;

		protected PanelSwitchTarget(String panel, Keybind keybind, Point clickPoint)
		{
			this.panel = panel;
			this.keybind = keybind;
			this.clickPoint = clickPoint;
		}

		protected boolean usesHotkey()
		{
			return keybind != null && !Keybind.NOT_SET.equals(keybind);
		}
	}

	protected static final class MobFarmerCandidate
	{
		protected final NPC npc;
		protected final Map<String, Object> entity;
		protected final List<String> reasons = new ArrayList<>();
		protected int score;
		protected boolean selectable = true;
		protected boolean engagedByOther;
		protected boolean engagedWithLocalPlayer;

		protected MobFarmerCandidate(NPC npc, Map<String, Object> entity)
		{
			this.npc = npc;
			this.entity = entity;
		}

		protected void reject(String reason)
		{
			selectable = false;
			reasons.add(reason);
		}

		protected void note(String reason)
		{
			reasons.add(reason);
		}
	}

	protected static final class MobFarmerSelection
	{
		protected Map<String, Object> target;
		protected List<Map<String, Object>> reports = new ArrayList<>();
		protected String decision = "none";
		protected boolean multiCombat;
	}

	protected static final class PathingResult
	{
		protected final boolean reachable;
		protected final int pathDistance;
		protected final int searchLimit;
		protected final int visited;
		protected final String failureReason;
		/**
		 * Non-null only when the winning route required crossing one closed door/gate CV
		 * Helper is permitted to act on (not denylisted, relevant auto-open/close flag on).
		 * The caller must still issue the actual Open/Close click and verify the door's real
		 * state before treating the route as walkable -- this is a contingent route, not a
		 * guarantee.
		 */
		protected Map<String, Object> doorTransition;
		/** True when the ONLY reason this is unreachable is a door/gate CV Helper can't act on. */
		protected boolean blockedByDoor;
		/** True when {@link #blockedByDoor} and the door is denylisted/unknown/auto-flag-disabled. */
		protected boolean manualActionRequired;
		/** Human-readable reason paired with {@link #manualActionRequired}, eg. "door-denylisted". */
		protected String manualActionReason;
		/** id/name/tile/actions/allowlistStatus of the blocking door when {@link #blockedByDoor}. */
		protected Map<String, Object> blockingDoor;

		protected PathingResult(boolean reachable, int pathDistance, int searchLimit, int visited, String failureReason)
		{
			this.reachable = reachable;
			this.pathDistance = pathDistance;
			this.searchLimit = searchLimit;
			this.visited = visited;
			this.failureReason = failureReason;
		}

		protected static PathingResult reachable(int pathDistance, int searchLimit, int visited)
		{
			return new PathingResult(true, pathDistance, searchLimit, visited, null);
		}

		protected static PathingResult unreachable(String failureReason, int searchLimit, int visited)
		{
			return new PathingResult(false, Integer.MAX_VALUE, searchLimit, visited, failureReason);
		}
	}

	protected static final class InteractionPathingResult
	{
		protected final boolean reachable;
		protected final int pathDistance;
		protected final int searchLimit;
		protected final int visited;
		protected final String failureReason;
		protected final WorldPoint interactionTile;
		protected final WorldArea objectFootprint;
		protected final int evaluatedInteractionTiles;
		protected final int walkableInteractionTiles;
		protected final int blockedInteractionTiles;
		protected final int blockedByCollision;
		protected final int blockedByScene;
		/** See {@link PathingResult#doorTransition}. */
		protected Map<String, Object> doorTransition;
		/** See {@link PathingResult#blockedByDoor}. */
		protected boolean blockedByDoor;
		/** See {@link PathingResult#manualActionRequired}. */
		protected boolean manualActionRequired;
		/** See {@link PathingResult#manualActionReason}. */
		protected String manualActionReason;
		/** See {@link PathingResult#blockingDoor}. */
		protected Map<String, Object> blockingDoor;

		protected InteractionPathingResult(boolean reachable, int pathDistance, int searchLimit, int visited,
			String failureReason, WorldPoint interactionTile, WorldArea objectFootprint,
			int evaluatedInteractionTiles, int walkableInteractionTiles, int blockedInteractionTiles,
			int blockedByCollision, int blockedByScene)
		{
			this.reachable = reachable;
			this.pathDistance = pathDistance;
			this.searchLimit = searchLimit;
			this.visited = visited;
			this.failureReason = failureReason;
			this.interactionTile = interactionTile;
			this.objectFootprint = objectFootprint;
			this.evaluatedInteractionTiles = evaluatedInteractionTiles;
			this.walkableInteractionTiles = walkableInteractionTiles;
			this.blockedInteractionTiles = blockedInteractionTiles;
			this.blockedByCollision = blockedByCollision;
			this.blockedByScene = blockedByScene;
		}

		protected static InteractionPathingResult reachable(int pathDistance, int searchLimit, int visited,
			WorldPoint interactionTile, WorldArea objectFootprint,
			int evaluatedInteractionTiles, int walkableInteractionTiles, int blockedInteractionTiles,
			int blockedByCollision, int blockedByScene)
		{
			return new InteractionPathingResult(true, pathDistance, searchLimit, visited, null,
				interactionTile, objectFootprint, evaluatedInteractionTiles, walkableInteractionTiles,
				blockedInteractionTiles, blockedByCollision, blockedByScene);
		}

		protected static InteractionPathingResult unreachable(String failureReason, int searchLimit, int visited,
			WorldArea objectFootprint, int evaluatedInteractionTiles, int walkableInteractionTiles,
			int blockedInteractionTiles, int blockedByCollision, int blockedByScene)
		{
			return new InteractionPathingResult(false, Integer.MAX_VALUE, searchLimit, visited, failureReason,
				null, objectFootprint, evaluatedInteractionTiles, walkableInteractionTiles,
				blockedInteractionTiles, blockedByCollision, blockedByScene);
		}

		protected Map<String, Object> toMap()
		{
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("reachable", reachable);
			map.put("pathDistance", reachable ? pathDistance : null);
			map.put("searchLimit", searchLimit);
			map.put("visited", visited);
			map.put("failureReason", failureReason);
			map.put("interactionTile", worldPointMap(interactionTile));
			map.put("objectFootprint", footprintMap(objectFootprint));
			map.put("evaluatedInteractionTiles", evaluatedInteractionTiles);
			map.put("walkableInteractionTiles", walkableInteractionTiles);
			map.put("blockedInteractionTiles", blockedInteractionTiles);
			map.put("blockedByCollision", blockedByCollision);
			map.put("blockedByScene", blockedByScene);
			map.put("doorTransition", doorTransition);
			map.put("blockedByDoor", blockedByDoor);
			map.put("manualActionRequired", manualActionRequired);
			map.put("manualActionReason", manualActionReason);
			map.put("blockingDoor", blockingDoor);
			return map;
		}
	}

	protected static Map<String, Object> footprintMap(WorldArea footprint)
	{
		if (footprint == null)
		{
			return null;
		}
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("x", footprint.getX());
		map.put("y", footprint.getY());
		map.put("plane", footprint.getPlane());
		map.put("width", footprint.getWidth());
		map.put("height", footprint.getHeight());
		return map;
	}

	/**
	 * Builds the world-space occupancy footprint of a tile object using its scene-min
	 * corner, not {@link TileObject#getWorldLocation()}. For multi-tile GameObjects,
	 * getWorldLocation() returns the "center most tile, rounded to the south-west", which
	 * is offset from the true SW occupied tile whenever size is even -- routing interaction
	 * tiles around that offset footprint skipped the actually-adjacent reachable tiles
	 * (the root cause of normal trees showing as unreachable).
	 */
	private WorldArea buildObjectFootprint(TileObject object, WorldView worldView, int sizeX, int sizeY)
	{
		if (object instanceof GameObject && worldView != null)
		{
			GameObject gameObject = (GameObject) object;
			net.runelite.api.Point sceneMin = gameObject.getSceneMinLocation();
			if (sceneMin != null)
			{
				WorldPoint origin = WorldPoint.fromScene(worldView, sceneMin.getX(), sceneMin.getY(), object.getPlane());
				if (origin != null)
				{
					return new WorldArea(origin, sizeX, sizeY);
				}
			}
		}
		return new WorldArea(object.getWorldLocation(), sizeX, sizeY);
	}

	protected static Map<String, Object> worldPointMap(WorldPoint point)
	{
		if (point == null)
		{
			return null;
		}
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("x", point.getX());
		map.put("y", point.getY());
		map.put("plane", point.getPlane());
		map.put("value", point.toString());
		return map;
	}

	protected static final class MobFarmerLootCandidate
	{
		protected final Map<String, Object> item;
		protected final List<String> reasons = new ArrayList<>();
		protected final List<String> priorityReasons = new ArrayList<>();
		protected int score;
		protected boolean selectable = true;
		protected boolean highPriority;

		protected MobFarmerLootCandidate(Map<String, Object> item)
		{
			this.item = item;
		}

		protected void reject(String reason)
		{
			selectable = false;
			reasons.add(reason);
		}

		protected void note(String reason)
		{
			reasons.add(reason);
		}

		protected void priority(String reason)
		{
			highPriority = true;
			priorityReasons.add(reason);
			note("priority:" + reason);
		}
	}

	protected static final class MobFarmerLootSelection
	{
		protected Map<String, Object> target;
		protected List<Map<String, Object>> reports = new ArrayList<>();
		protected String decision = "none";
		protected int selectableCount;
		protected boolean priorityOnly;
	}

	protected static final class InventoryMenuAction
	{
		protected final int opIndex;
		protected final int componentOpId;
		protected final int param0;
		protected final int param1;
		protected final MenuAction menuAction;
		protected final int identifier;
		protected final int itemId;
		protected final String option;

		protected InventoryMenuAction(int opIndex, int componentOpId, int param0, int param1, MenuAction menuAction, int identifier, int itemId, String option)
		{
			this.opIndex = opIndex;
			this.componentOpId = componentOpId;
			this.param0 = param0;
			this.param1 = param1;
			this.menuAction = menuAction;
			this.identifier = identifier;
			this.itemId = itemId;
			this.option = option;
		}

		protected Map<String, Object> asMap()
		{
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("opIndex", opIndex);
			out.put("componentOpId", componentOpId);
			out.put("param0", param0);
			out.put("param1", param1);
			out.put("menuAction", menuAction.name());
			out.put("identifier", identifier);
			out.put("itemId", itemId);
			out.put("option", option);
			return out;
		}
	}

	protected static final class IntermediateInventoryAction
	{
		protected final Map<String, Object> target;
		protected final String[] preferredActions;
		protected final String failureReason;
		protected final String matchedRule;

		protected IntermediateInventoryAction(Map<String, Object> target, String[] preferredActions, String failureReason, String matchedRule)
		{
			this.target = target;
			this.preferredActions = preferredActions;
			this.failureReason = failureReason;
			this.matchedRule = matchedRule;
		}

		protected boolean isActionable()
		{
			return failureReason == null;
		}
	}

	protected static final class IntermediateActionRule
	{
		protected final String itemTarget;
		protected final String[] actions;
		protected final String source;

		protected IntermediateActionRule(String itemTarget, String[] actions, String source)
		{
			this.itemTarget = itemTarget;
			this.actions = actions;
			this.source = source;
		}
	}

	protected enum GroundItemsClassification
	{
		NONE,
		HIGHLIGHTED,
		HIDDEN,
		HIDDEN_BY_VALUE,
		SUPPRESSED_BY_SHOW_HIGHLIGHTED_ONLY
	}

	protected enum MobFarmerActionKind
	{
		COMBAT,
		MOVEMENT,
		LOOT_PICKUP,
		INVENTORY,
		SURVIVAL,
		UI,
		LOGIN_RECOVERY,
		CONFIG
	}
	private final java.awt.KeyEventDispatcher cvHotkeyDispatcher = this::dispatchCvHotkey;

	@Provides
	CvHelperModConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CvHelperModConfig.class);
	}

	@Override
	protected void startUp()
	{
		log.info("CV Helper starting");
		mobFarmerTarget = normalizedMobFarmerTarget(config.mobFarmerTarget());
		miningFarmerTarget = config.miningFarmerTarget();
		woodcuttingFarmerTarget = config.woodcuttingFarmerTarget();
		miningFarmerScanRadius = config.miningScanRadius();
		miningFarmerMaxCandidates = config.miningMaxCandidates();
		woodcuttingFarmerScanRadius = config.woodcuttingScanRadius();
		woodcuttingFarmerMaxCandidates = config.woodcuttingMaxCandidates();
		enabledPrayers.addAll(getPrayerNames());
		enabledSpellbooks.addAll(getSpellbookNames());
		overlayManager.add(overlay);
		panel = new CvHelperModPanel(this);
		navButton = NavigationButton.builder()
			.tooltip(TOOLTIP)
			.icon(createIcon())
			.priority(9)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		registerHotkeys();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(cvHotkeyDispatcher);
		chatResponderService.start();
		startServer();
		try
		{
			antiIdleRobot = new Robot();
			antiIdleRobot.setAutoDelay(1);
		}
		catch (AWTException e)
		{
			log.warn("CV Helper anti-idle robot unavailable", e);
		}
		updateAntiIdleState();
	}

	@Override
	protected void shutDown()
	{
		log.info("CV Helper stopping");
		// Stop the HTTP server FIRST: its dispatcher thread is non-daemon, so if a later cleanup
		// step throws, an un-stopped server keeps the whole JVM (java process) alive on close.
		stopServer();
		chatResponderService.stop();
		stopMobFarmer();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(cvHotkeyDispatcher);
		preDispatcherPressedKeys.clear();
		unregisterHotkeys();
		overlayManager.remove(overlay);
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		navButton = null;
		panel = null;
		resetIdleTimeout();
		antiIdleActive = false;
		antiIdleMode = "off";
		lastAntiIdleInputTime = null;
		antiIdleTimeoutAppliedAt = null;
		lastAppliedIdleTimeoutTicks = -1;
	}

	private void updateAntiIdleState()
	{
		boolean anyFarmerRunning = mobFarmerRunning.get() || miningFarmerRunning.get() || woodcuttingFarmerRunning.get();
		boolean manualOverride = config.antiIdleManualOverride();
		boolean shouldBeActive = config.antiIdleEnabled() && (anyFarmerRunning || manualOverride);
		boolean wasActive = antiIdleActive;

		if (shouldBeActive && !wasActive)
		{
			antiIdleActive = true;
			antiIdleMode = manualOverride && !anyFarmerRunning ? "manual" : "auto";
			lastAntiIdleInputTime = Instant.now();
			applyAntiIdleTimeout();
			log.info("CV Helper anti-idle activated (mode={})", antiIdleMode);
		}
		else if (!shouldBeActive && wasActive)
		{
			antiIdleActive = false;
			antiIdleMode = "off";
			lastAntiIdleInputTime = null;
			antiIdleTimeoutAppliedAt = null;
			lastAppliedIdleTimeoutTicks = -1;
			resetIdleTimeout();
			log.info("CV Helper anti-idle deactivated");
		}
		else if (shouldBeActive && wasActive)
		{
			antiIdleMode = manualOverride && !anyFarmerRunning ? "manual" : "auto";
		}
	}

	private void applyAntiIdleTimeout()
	{
		int requestedMinutes = Math.max(MIN_IDLE_TIMEOUT_MINUTES, Math.min(MAX_IDLE_TIMEOUT_MINUTES, config.antiIdleTimeoutMinutes()));
		int requestedTicks = requestedMinutes * CLIENT_TICKS_PER_MINUTE;
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}
			client.setIdleTimeout(requestedTicks);
			lastAppliedIdleTimeoutTicks = client.getIdleTimeout();
			antiIdleTimeoutAppliedAt = Instant.now();
			if (lastAppliedIdleTimeoutTicks != requestedTicks)
			{
				log.warn("CV Helper anti-idle timeout was clamped by client: requested {} ticks, got {} ticks", requestedTicks, lastAppliedIdleTimeoutTicks);
			}
		});
	}

	private void resetIdleTimeout()
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}
			client.setIdleTimeout(DEFAULT_IDLE_TIMEOUT_MINUTES * CLIENT_TICKS_PER_MINUTE);
			lastAppliedIdleTimeoutTicks = -1;
			antiIdleTimeoutAppliedAt = null;
		});
	}

	private void sendAntiIdleInputIfNeeded()
	{
		if (!antiIdleActive || antiIdleRobot == null)
		{
			return;
		}
		Instant lastInput = lastAntiIdleInputTime;
		if (lastInput == null)
		{
			lastInput = Instant.now();
			lastAntiIdleInputTime = lastInput;
			return;
		}
		int intervalMinutes = Math.max(1, Math.min(config.antiIdleTimeoutMinutes() - 1, config.antiIdleInputIntervalMinutes()));
		Duration interval = Duration.ofMinutes(intervalMinutes);
		if (Duration.between(lastInput, Instant.now()).compareTo(interval) < 0)
		{
			return;
		}

		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}
		LocalPoint localPoint = local.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}
		net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getPlane());
		if (canvasPoint == null)
		{
			return;
		}
		Map<String, Object> pointMap = pointMap(canvasPoint.getX(), canvasPoint.getY());
		java.awt.Point screenPoint = canvasPointToScreen(pointMap);
		if (screenPoint == null)
		{
			return;
		}

		lastAntiIdleInputTime = Instant.now();
		Thread inputThread = new Thread(() -> sendAntiIdleInput(screenPoint.x, screenPoint.y, canvasPoint.getX(), canvasPoint.getY()), "cv-helper-anti-idle");
		inputThread.setDaemon(true);
		inputThread.start();
	}

	private void sendAntiIdleInput(int screenX, int screenY, int canvasX, int canvasY)
	{
		try
		{
			java.awt.Point originalMouse = MouseInfo.getPointerInfo().getLocation();
			antiIdleRobot.mouseMove(screenX, screenY);
			antiIdleRobot.mouseMove(screenX + 1, screenY);
			antiIdleRobot.mouseMove(screenX, screenY);
			antiIdleRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			antiIdleRobot.delay(5);
			antiIdleRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			if (config.antiIdleRestoreMouse())
			{
				antiIdleRobot.mouseMove(originalMouse.x, originalMouse.y);
			}
			log.debug("CV Helper anti-idle input sent at screen=({},{}) canvas=({},{}) mouseRestored={}", screenX, screenY, canvasX, canvasY, config.antiIdleRestoreMouse());
		}
		catch (Exception e)
		{
			log.warn("CV Helper anti-idle input failed", e);
		}
	}

	private Map<String, Object> getAntiIdleStatus()
	{
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("active", antiIdleActive);
		status.put("mode", antiIdleMode);
		status.put("timeoutMinutes", config.antiIdleEnabled() ? Math.max(MIN_IDLE_TIMEOUT_MINUTES, Math.min(MAX_IDLE_TIMEOUT_MINUTES, config.antiIdleTimeoutMinutes())) : DEFAULT_IDLE_TIMEOUT_MINUTES);
		status.put("inputIntervalMinutes", config.antiIdleEnabled() ? Math.max(1, Math.min(config.antiIdleTimeoutMinutes() - 1, config.antiIdleInputIntervalMinutes())) : DEFAULT_ANTI_IDLE_INPUT_INTERVAL_MINUTES);
		status.put("manualOverride", config.antiIdleManualOverride());
		status.put("restoreMouse", config.antiIdleRestoreMouse());
		status.put("lastAppliedIdleTimeoutTicks", lastAppliedIdleTimeoutTicks);
		status.put("timeoutAppliedAt", antiIdleTimeoutAppliedAt == null ? null : antiIdleTimeoutAppliedAt.toString());
		status.put("lastInputAt", lastAntiIdleInputTime == null ? null : lastAntiIdleInputTime.toString());
		Instant nextInput = lastAntiIdleInputTime;
		if (antiIdleActive && nextInput != null)
		{
			int intervalMinutes = Math.max(1, Math.min(config.antiIdleTimeoutMinutes() - 1, config.antiIdleInputIntervalMinutes()));
			nextInput = nextInput.plus(Duration.ofMinutes(intervalMinutes));
		}
		status.put("nextInputAt", nextInput == null ? null : nextInput.toString());
		status.put("robotAvailable", antiIdleRobot != null);
		return status;
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		refreshHotkeyInputState();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = safeValue(client::getTickCount, 0);
		Map<String, Object> scheduler = new LinkedHashMap<>(lastMobFarmerSchedulerStatus);
		scheduler.put("lastGameTick", tick);
		scheduler.put("gameTickAt", Instant.now().toString());
		lastMobFarmerSchedulerStatus = scheduler;
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		updateAntiIdleState();
		sendAntiIdleInputIfNeeded();
		if (mobFarmerRunning.get())
		{
			int generation = mobFarmerGeneration.get();
			if (isCurrentMobFarmerLoop(generation) && lastMobFarmerLoopStepTick != tick)
			{
				mobFarmerStep(mobFarmerLiveMode, generation, "game-tick");
			}
		}
		runSkillFarmerTick("mining");
		runSkillFarmerTick("woodcutting");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("gameState", gameState == null ? null : gameState.name());
		details.put("detectedScreen", detectedLoginScreen(gameState));
		details.put("at", Instant.now().toString());
		details.put("running", mobFarmerRunning.get());
		if (gameState == GameState.LOGGED_IN)
		{
			Map<String, Object> recovery = new LinkedHashMap<>(details);
			recovery.put("result", "logged-in");
			recovery.put("success", true);
			setMobFarmerLoginRecoveryDecision("succeeded:logged-in", recovery);
			clearPendingMobFarmerDeath("logged-in-state-change");
			if (mobFarmerRunning.get() && getMobFarmerFocusClickAfterLogin())
			{
				mobFarmerFocusClickNeeded = true;
				recordMobFarmerFocusClick("needed-after-login", "logged-in-state-change", false, null);
			}
		}
		else if (gameState == GameState.CONNECTION_LOST)
		{
			Map<String, Object> recovery = new LinkedHashMap<>(details);
			String pausedReason = mobFarmerLoginRecoveryPausedReason(gameState, mobFarmerRunning.get(), mobFarmerLiveMode);
			recovery.put("result", pausedReason == null ? "connection-lost-detected" : "paused");
			recovery.put("pausedReason", pausedReason == null ? "waiting-for-recovery-loop" : pausedReason);
			setMobFarmerLoginRecoveryDecision(pausedReason == null ? "connection-lost-detected" : "paused:" + pausedReason, recovery);
		}
		else if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR)
		{
			Map<String, Object> recovery = new LinkedHashMap<>(details);
			String pausedReason = mobFarmerLoginRecoveryPausedReason(gameState, mobFarmerRunning.get(), mobFarmerLiveMode);
			recovery.put("result", pausedReason == null ? "login-screen-detected" : "paused");
			recovery.put("pausedReason", pausedReason == null ? "waiting-for-recovery-loop" : pausedReason);
			setMobFarmerLoginRecoveryDecision(pausedReason == null ? "login-screen-detected" : "paused:" + pausedReason, recovery);
		}
		Map<String, Object> scheduler = new LinkedHashMap<>(lastMobFarmerSchedulerStatus);
		scheduler.put("lastGameState", details);
		lastMobFarmerSchedulerStatus = scheduler;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		rememberMobFarmerMenuEntry("added", event.getMenuEntry());
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		rememberMobFarmerMenuEntry("clicked", event.getMenuEntry());
		if ("Drop".equalsIgnoreCase(event.getMenuOption()))
		{
			log.info(
				"MANUAL DROP: option={} target={} action={} id={} itemId={} itemOp={} p0={} p1={} widget={}",
				event.getMenuOption(),
				event.getMenuTarget(),
				event.getMenuAction(),
				event.getId(),
				event.getItemId(),
				event.isItemOp() ? event.getItemOp() : -1,
				event.getParam0(),
				event.getParam1(),
				event.getWidget()
			);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		chatResponderService.onChatMessage(event);
	}

	/**
	 * A Mining XP gain is the most immediate, reliable signal that the active rock
	 * was successfully mined. When it fires we treat the current target as completed,
	 * blacklist its tile for a few ticks so the very next scan does not re-pick the
	 * (now depleting/depleted) rock, and clear the selection so the next step rescans
	 * for the next best reachable candidate.
	 */
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.MINING)
		{
			return;
		}
		int xp = event.getXp();
		int previous = lastMiningXp;
		lastMiningXp = xp;
		if (previous < 0 || xp <= previous)
		{
			return;
		}
		if (!miningFarmerRunning.get() || lastSelectedMiningTarget == null)
		{
			return;
		}
		markMiningTargetCompleted("xp-drop");
	}

	private String tileKey(Map<String, Object> world)
	{
		if (world == null)
		{
			return null;
		}
		int x = intValue(world.get("x"), Integer.MIN_VALUE);
		int y = intValue(world.get("y"), Integer.MIN_VALUE);
		int plane = intValue(world.get("plane"), 0);
		if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
		{
			return null;
		}
		return x + "," + y + "," + plane;
	}

	/**
	 * Record the active mining target as completed and put its tile on a short
	 * cooldown blacklist so it is skipped during candidate selection for a few ticks.
	 */
	private void markMiningTargetCompleted(String reason)
	{
		Map<String, Object> target = lastSelectedMiningTarget;
		if (target == null)
		{
			return;
		}
		String key = tileKey(mapValue(target.get("worldLocation")));
		if (key != null)
		{
			miningDepletedTileUntilTick.put(key, client.getTickCount() + MINING_DEPLETED_TILE_COOLDOWN_TICKS);
		}
		lastCompletedMiningTarget = target;
		miningCompletionReason = reason;
		lastMiningInvalidationTick = client.getTickCount();
		lastSelectedMiningTarget = null;
	}

	/** True while a tile is still on the post-mine cooldown blacklist. */
	private boolean isMiningTileOnCooldown(Map<String, Object> world)
	{
		String key = tileKey(world);
		if (key == null)
		{
			return false;
		}
		Integer until = miningDepletedTileUntilTick.get(key);
		if (until == null)
		{
			return false;
		}
		if (client.getTickCount() >= until)
		{
			miningDepletedTileUntilTick.remove(key);
			return false;
		}
		return true;
	}

	@Subscribe
	public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
	{
		if (!CvHelperModConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		String key = event.getKey();
		if (CvHelperModConfig.ANTI_IDLE_ENABLED.equals(key)
			|| CvHelperModConfig.ANTI_IDLE_TIMEOUT_MINUTES.equals(key)
			|| CvHelperModConfig.ANTI_IDLE_INPUT_INTERVAL_MINUTES.equals(key)
			|| CvHelperModConfig.ANTI_IDLE_MANUAL_OVERRIDE.equals(key)
			|| CvHelperModConfig.ANTI_IDLE_RESTORE_MOUSE.equals(key))
		{
			updateAntiIdleState();
			if (CvHelperModConfig.ANTI_IDLE_TIMEOUT_MINUTES.equals(key) && antiIdleActive)
			{
				applyAntiIdleTimeout();
			}
		}
	}

	private void rememberMobFarmerMenuEntry(String source, MenuEntry entry)
	{
		if (entry == null || !isMobFarmerRelevantMenuEntry(entry))
		{
			return;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("source", source);
		out.put("at", Instant.now().toString());
		out.put("option", cleanWidgetText(entry.getOption()));
		out.put("target", cleanWidgetText(entry.getTarget()));
		out.put("menuAction", entry.getType().name());
		out.put("identifier", entry.getIdentifier());
		out.put("param0", entry.getParam0());
		out.put("param1", entry.getParam1());
		out.put("itemId", entry.getItemId());
		out.put("deprioritized", entry.isDeprioritized());
		out.put("worldViewId", entry.getWorldViewId());
		if (entry.getWidget() != null)
		{
			out.put("widgetId", entry.getWidget().getId());
			out.put("widgetIndex", entry.getWidget().getIndex());
		}
		if (entry.getNpc() != null)
		{
			out.put("npcIndex", entry.getNpc().getIndex());
			out.put("npcName", entry.getNpc().getName());
		}
		List<Map<String, Object>> entries = new ArrayList<>(lastMobFarmerMenuEntries);
		entries.add(0, out);
		while (entries.size() > 30)
		{
			entries.remove(entries.size() - 1);
		}
		lastMobFarmerMenuEntries = entries;
	}

	private boolean isMobFarmerRelevantMenuEntry(MenuEntry entry)
	{
		MenuAction type = entry.getType();
		switch (type)
		{
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
			case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION:
			case GROUND_ITEM_THIRD_OPTION:
			case GROUND_ITEM_FOURTH_OPTION:
			case GROUND_ITEM_FIFTH_OPTION:
			case WIDGET_TARGET_ON_GROUND_ITEM:
			case CC_OP:
			case CC_OP_LOW_PRIORITY:
				break;
			default:
				return false;
		}
		String option = normalize(cleanWidgetText(entry.getOption()));
		return option.equals("take")
			|| option.equals("attack")
			|| option.equals("bury")
			|| option.equals("scatter")
			|| option.equals("use")
			|| option.equals("drop")
			|| option.equals("eat")
			|| option.equals("drink")
			|| option.equals("consume");
	}

	private void recordMobFarmerActionAttempt(String kind, Map<String, Object> attempt)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("kind", kind);
		out.put("at", Instant.now().toString());
		if (attempt != null)
		{
			out.putAll(attempt);
		}
		lastMobFarmerActionAttempt = out;
	}

	private final HotkeyListener debugHotkeyListener = new HotkeyListener(() -> config.debugHotkey())
	{
		@Override
		public void keyPressed(java.awt.event.KeyEvent e)
		{
			if (shouldSuppressMatchedHotkey(e, config.debugHotkey(), "debug"))
			{
				return;
			}
			super.keyPressed(e);
		}

		@Override
		public void hotkeyPressed()
		{
			debugOverlayState();
		}
	};

	private final HotkeyListener printBoundsHotkeyListener = new HotkeyListener(() -> config.printBoundsHotkey())
	{
		@Override
		public void keyPressed(java.awt.event.KeyEvent e)
		{
			if (shouldSuppressMatchedHotkey(e, config.printBoundsHotkey(), "print-bounds"))
			{
				return;
			}
			super.keyPressed(e);
		}

		@Override
		public void hotkeyPressed()
		{
			printOverlayCoordinates();
		}
	};

	private final HotkeyListener captureScreenHotkeyListener = new HotkeyListener(() -> config.captureScreenHotkey())
	{
		@Override
		public void keyPressed(java.awt.event.KeyEvent e)
		{
			if (shouldSuppressMatchedHotkey(e, config.captureScreenHotkey(), "capture-screen"))
			{
				return;
			}
			super.keyPressed(e);
		}

		@Override
		public void hotkeyPressed()
		{
			captureScreen();
		}
	};

	private final HotkeyListener refreshEntitiesHotkeyListener = new HotkeyListener(() -> config.refreshEntitiesHotkey())
	{
		@Override
		public void keyPressed(java.awt.event.KeyEvent e)
		{
			if (shouldSuppressMatchedHotkey(e, config.refreshEntitiesHotkey(), "refresh-entities"))
			{
				return;
			}
			super.keyPressed(e);
		}

		@Override
		public void hotkeyPressed()
		{
			refreshEntities();
		}
	};

	private final HotkeyListener nearestEntityHotkeyListener = new HotkeyListener(() -> config.nearestEntityHotkey())
	{
		@Override
		public void keyPressed(java.awt.event.KeyEvent e)
		{
			if (shouldSuppressMatchedHotkey(e, config.nearestEntityHotkey(), "nearest-entity"))
			{
				return;
			}
			super.keyPressed(e);
		}

		@Override
		public void hotkeyPressed()
		{
			printNearestEntityTarget();
		}
	};

	private final HotkeyListener panicStopHotkeyListener = new HotkeyListener(() -> config.panicStopHotkey())
	{
		@Override
		public void keyPressed(java.awt.event.KeyEvent e)
		{
			if (config.panicStopHotkey().matches(e))
			{
				recordPanicStopHotkey(e, "HotkeyListener");
			}
			super.keyPressed(e);
		}

		@Override
		public void hotkeyPressed()
		{
			panicStop();
		}
	};

	private void registerHotkeys()
	{
		keyManager.registerKeyListener(debugHotkeyListener);
		keyManager.registerKeyListener(printBoundsHotkeyListener);
		keyManager.registerKeyListener(captureScreenHotkeyListener);
		keyManager.registerKeyListener(refreshEntitiesHotkeyListener);
		keyManager.registerKeyListener(nearestEntityHotkeyListener);
		keyManager.registerKeyListener(panicStopHotkeyListener);
		actionHotkeyListeners.clear();
		for (int slot = 1; slot <= ACTION_SLOT_COUNT; slot++)
		{
			final int actionSlot = slot;
			HotkeyListener listener = new HotkeyListener(() -> getActionHotkey(actionSlot))
			{
				@Override
				public void keyPressed(java.awt.event.KeyEvent e)
				{
					if (shouldSuppressMatchedHotkey(e, getActionHotkey(actionSlot), "action-slot-" + actionSlot))
					{
						return;
					}
					super.keyPressed(e);
				}

				@Override
				public void hotkeyPressed()
				{
					performConfiguredAction(actionSlot);
				}
			};
			actionHotkeyListeners.add(listener);
			keyManager.registerKeyListener(listener);
		}
	}

	private void unregisterHotkeys()
	{
		keyManager.unregisterKeyListener(debugHotkeyListener);
		keyManager.unregisterKeyListener(printBoundsHotkeyListener);
		keyManager.unregisterKeyListener(captureScreenHotkeyListener);
		keyManager.unregisterKeyListener(refreshEntitiesHotkeyListener);
		keyManager.unregisterKeyListener(nearestEntityHotkeyListener);
		keyManager.unregisterKeyListener(panicStopHotkeyListener);
		for (HotkeyListener listener : actionHotkeyListeners)
		{
			keyManager.unregisterKeyListener(listener);
		}
		actionHotkeyListeners.clear();
	}

	private boolean dispatchCvHotkey(KeyEvent event)
	{
		if (event == null)
		{
			return false;
		}
		if (event.getID() == KeyEvent.KEY_RELEASED)
		{
			preDispatcherPressedKeys.remove(preDispatchKey(event));
			return false;
		}
		if (event.getID() != KeyEvent.KEY_PRESSED)
		{
			return false;
		}

		String key = preDispatchKey(event);
		if (config.panicStopHotkey().matches(event))
		{
			if (preDispatcherPressedKeys.contains(key))
			{
				event.consume();
				return true;
			}
			preDispatcherPressedKeys.add(key);
			recordPanicStopHotkey(event, "KeyEventDispatcher");
			panicStop();
			event.consume();
			return true;
		}

		String matchedHotkey = matchedNonPanicHotkey(event);
		if (matchedHotkey == null)
		{
			return false;
		}
		if (shouldSuppressHotkey("KeyEventDispatcher", event, matchedHotkey))
		{
			preDispatcherPressedKeys.remove(key);
			return false;
		}
		if (preDispatcherPressedKeys.contains(key))
		{
			event.consume();
			return true;
		}

		if (config.debugHotkey().matches(event))
		{
			preDispatcherPressedKeys.add(key);
			debugOverlayState();
			event.consume();
			return true;
		}
		if (config.printBoundsHotkey().matches(event))
		{
			preDispatcherPressedKeys.add(key);
			printOverlayCoordinates();
			event.consume();
			return true;
		}
		if (config.captureScreenHotkey().matches(event))
		{
			preDispatcherPressedKeys.add(key);
			captureScreen();
			event.consume();
			return true;
		}
		if (config.refreshEntitiesHotkey().matches(event))
		{
			preDispatcherPressedKeys.add(key);
			refreshEntities();
			event.consume();
			return true;
		}
		if (config.nearestEntityHotkey().matches(event))
		{
			preDispatcherPressedKeys.add(key);
			printNearestEntityTarget();
			event.consume();
			return true;
		}
		for (int slot = 1; slot <= ACTION_SLOT_COUNT; slot++)
		{
			if (getActionHotkey(slot).matches(event))
			{
				preDispatcherPressedKeys.add(key);
				performConfiguredAction(slot);
				event.consume();
				return true;
			}
		}
		return false;
	}

	private String matchedNonPanicHotkey(KeyEvent event)
	{
		if (config.debugHotkey().matches(event))
		{
			return "debug";
		}
		if (config.printBoundsHotkey().matches(event))
		{
			return "print-bounds";
		}
		if (config.captureScreenHotkey().matches(event))
		{
			return "capture-screen";
		}
		if (config.refreshEntitiesHotkey().matches(event))
		{
			return "refresh-entities";
		}
		if (config.nearestEntityHotkey().matches(event))
		{
			return "nearest-entity";
		}
		for (int slot = 1; slot <= ACTION_SLOT_COUNT; slot++)
		{
			if (getActionHotkey(slot).matches(event))
			{
				return "action-slot-" + slot;
			}
		}
		return null;
	}

	private String preDispatchKey(KeyEvent event)
	{
		int modifierMask = InputEvent.SHIFT_DOWN_MASK
			| InputEvent.CTRL_DOWN_MASK
			| InputEvent.ALT_DOWN_MASK
			| InputEvent.META_DOWN_MASK;
		return event.getExtendedKeyCode() + ":" + (event.getModifiersEx() & modifierMask);
	}

	private boolean shouldSuppressMatchedHotkey(KeyEvent event, Keybind keybind, String matchedHotkey)
	{
		if (event == null || keybind == null || !keybind.matches(event))
		{
			return false;
		}
		boolean suppressed = shouldSuppressHotkey("HotkeyListener", event, matchedHotkey);
		if (suppressed)
		{
			preDispatcherPressedKeys.remove(preDispatchKey(event));
		}
		return suppressed;
	}

	private boolean shouldSuppressHotkey(String path, KeyEvent event, String matchedHotkey)
	{
		Map<String, Object> diagnostics = hotkeyDiagnostics(path, event, matchedHotkey);
		String reason = null;
		if (client == null)
		{
			reason = "client-unavailable";
		}
		else if (Boolean.TRUE.equals(diagnostics.get("swingTextInputFocused")))
		{
			reason = "swing-text-input-focused";
		}
		else if (!Boolean.TRUE.equals(diagnostics.get("runeLiteWindowActive")))
		{
			reason = "runelite-window-unfocused";
		}
		else if (hotkeyMesLayerMode != InputType.NONE.getType())
		{
			reason = "meslayer-input-active";
		}
		else if (hotkeyDefaultChatInputActive)
		{
			reason = "chatbox-input-active";
		}
		else if (hotkeyChatInputLength > 0)
		{
			reason = "chat-input-present";
		}
		else if (hotkeyMesLayerInputLength > 0)
		{
			reason = "meslayer-input-present";
		}

		boolean suppressed = reason != null;
		diagnostics.put("decision", suppressed ? "suppressed" : "allowed");
		diagnostics.put("suppressionReason", reason);
		diagnostics.put("panicStopBypassesGuard", true);
		lastHotkeyGuardDecision = diagnostics;
		if (suppressed)
		{
			log.debug("CV Helper hotkey {} suppressed on {}: {}", matchedHotkey, path, reason);
		}
		return suppressed;
	}

	private void recordPanicStopHotkey(KeyEvent event, String path)
	{
		Map<String, Object> diagnostics = hotkeyDiagnostics(path, event, "panic-stop");
		diagnostics.put("decision", "allowed");
		diagnostics.put("suppressionReason", null);
		diagnostics.put("panicStopBypassesGuard", true);
		diagnostics.put("reason", "panic-stop-global-exception");
		lastHotkeyGuardDecision = diagnostics;
	}

	private Map<String, Object> hotkeyDiagnostics(String path, KeyEvent event, String matchedHotkey)
	{
		KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		Component focusOwner = focusManager.getFocusOwner();
		Window activeWindow = focusManager.getActiveWindow();
		Component canvas = client == null ? null : client.getCanvas();
		Window runeLiteWindow = canvas == null ? null : SwingUtilities.getWindowAncestor(canvas);
		boolean runeLiteWindowActive = runeLiteWindow != null
			&& (runeLiteWindow == activeWindow || runeLiteWindow.isActive());

		Map<String, Object> diagnostics = new LinkedHashMap<>();
		diagnostics.put("at", Instant.now().toString());
		diagnostics.put("path", path);
		diagnostics.put("matchedHotkey", matchedHotkey);
		diagnostics.put("keyCode", event == null ? null : event.getExtendedKeyCode());
		diagnostics.put("keyText", event == null ? null : KeyEvent.getKeyText(event.getExtendedKeyCode()));
		diagnostics.put("modifiers", event == null ? null : event.getModifiersEx());
		diagnostics.put("eventSourceClass", event == null || event.getSource() == null ? null : event.getSource().getClass().getName());
		diagnostics.put("runeLiteWindowActive", runeLiteWindowActive);
		diagnostics.put("canvasFocusOwner", canvas != null && canvas.isFocusOwner());
		diagnostics.put("focusOwnerClass", focusOwner == null ? null : focusOwner.getClass().getName());
		diagnostics.put("swingTextInputFocused", focusOwner instanceof javax.swing.text.JTextComponent);
		diagnostics.put("gameState", client == null ? null : safeValue(() -> client.getGameState().name(), null));
		diagnostics.put("mesLayerMode", hotkeyMesLayerMode);
		diagnostics.put("chatDisplayVisible", hotkeyChatDisplayVisible);
		diagnostics.put("mesLayerAvailable", hotkeyMesLayerAvailable);
		diagnostics.put("pressEnterPromptVisible", hotkeyPressEnterPromptVisible);
		diagnostics.put("defaultChatInputActive", hotkeyDefaultChatInputActive);
		diagnostics.put("chatInputPresent", hotkeyChatInputLength > 0);
		diagnostics.put("chatInputLength", hotkeyChatInputLength);
		diagnostics.put("mesLayerInputPresent", hotkeyMesLayerInputLength > 0);
		diagnostics.put("mesLayerInputLength", hotkeyMesLayerInputLength);
		return diagnostics;
	}

	private void refreshHotkeyInputState()
	{
		if (client == null)
		{
			return;
		}
		hotkeyMesLayerMode = safeValue(() -> client.getVarcIntValue(VarClientID.MESLAYERMODE), InputType.NONE.getType());
		String chatInput = safeValue(() -> client.getVarcStrValue(VarClientID.CHATINPUT), "");
		String mesLayerInput = safeValue(() -> client.getVarcStrValue(VarClientID.MESLAYERINPUT), "");
		hotkeyChatInputLength = chatInput == null ? 0 : chatInput.length();
		hotkeyMesLayerInputLength = mesLayerInput == null ? 0 : mesLayerInput.length();
		hotkeyChatDisplayVisible = safeValue(() -> widgetVisible(InterfaceID.Chatbox.CHATDISPLAY), false);
		hotkeyMesLayerAvailable = safeValue(() -> widgetVisible(InterfaceID.Chatbox.MES_LAYER_HIDE), false);
		Widget chatInputWidget = safeValue(() -> client.getWidget(InterfaceID.Chatbox.INPUT), null);
		String chatInputWidgetText = chatInputWidget == null ? "" : safeValue(chatInputWidget::getText, "");
		hotkeyPressEnterPromptVisible = normalize(chatInputWidgetText).contains("press enter to chat");
		hotkeyDefaultChatInputActive = client.getGameState() == GameState.LOGGED_IN
			&& hotkeyChatDisplayVisible
			&& hotkeyMesLayerAvailable
			&& !hotkeyPressEnterPromptVisible;
	}

	private Map<String, Object> hotkeyGuardStatus()
	{
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("panicStopBypassesGuard", true);
		status.put("panicStopReason", "panic-stop-global-exception");
		status.put("mesLayerMode", hotkeyMesLayerMode);
		status.put("chatDisplayVisible", hotkeyChatDisplayVisible);
		status.put("mesLayerAvailable", hotkeyMesLayerAvailable);
		status.put("pressEnterPromptVisible", hotkeyPressEnterPromptVisible);
		status.put("defaultChatInputActive", hotkeyDefaultChatInputActive);
		status.put("chatInputPresent", hotkeyChatInputLength > 0);
		status.put("chatInputLength", hotkeyChatInputLength);
		status.put("mesLayerInputPresent", hotkeyMesLayerInputLength > 0);
		status.put("mesLayerInputLength", hotkeyMesLayerInputLength);
		status.put("lastDecision", new LinkedHashMap<>(lastHotkeyGuardDecision));
		return status;
	}

	CvHelperModConfig getConfig()
	{
		return config;
	}

	String getServerStatusText()
	{
		int port = localPort();
		if (port <= 0)
		{
			return "Server: off";
		}
		Instant lastRequest = lastLocalWebHelperRequest.get();
		String webHelper = lastRequest == null
			? "WebHelper: waiting"
			: "WebHelper: seen " + Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(Instant.now().toEpochMilli() - lastRequest.toEpochMilli())) + "s ago";
		return "Server: http://127.0.0.1:" + port + " | " + webHelper;
	}

	Map<String, Object> getPlayerStatus()
	{
		Map<String, Object> status = new LinkedHashMap<>();
		Player localPlayer = client.getLocalPlayer();
		status.put("gameState", safeValue(() -> client.getGameState().name(), "unknown"));
		status.put("loggedIn", client.getGameState() == GameState.LOGGED_IN);
		status.put("accountType", safeValue(() -> client.getAccountType() == null ? null : client.getAccountType().name(), null));
		status.put("localPlayer", localPlayer != null ? localPlayer.getName() : null);
		status.put("combatLevel", localPlayer != null ? localPlayer.getCombatLevel() : -1);
		status.put("world", safeValue(() -> client.getWorld() > 0 ? client.getWorld() : -1, -1));
		status.put("worldHost", safeValue(client::getWorldHost, null));
		status.put("worldType", safeValue(() -> client.getWorldType().toString(), null));
		status.put("plane", safeValue(client::getPlane, -1));
		status.put("baseX", safeValue(client::getBaseX, 0));
		status.put("baseY", safeValue(client::getBaseY, 0));
		status.put("runEnergy", safeValue(client::getEnergy, -1));
		status.put("weight", safeValue(client::getWeight, 0));
		status.put("spellbook", getSpellbookStatus());
		status.put("mouseCanvasPosition", safeValue(() -> pointValue(client.getMouseCanvasPosition()), null));
		status.put("localDestination", safeValue(() -> pointValue(client.getLocalDestinationLocation()), null));
		status.put("localLocation", safeValue(() -> localPlayer != null ? pointValue(localPlayer.getLocalLocation()) : null, null));
		status.put("worldLocation", safeValue(() -> localPlayer != null ? pointValue(localPlayer.getWorldLocation()) : null, null));
		status.put("selfBounds", safeValue(() -> actorBounds(localPlayer), null));
		status.put("interfaces", safeValue(this::interfaceStatus, new LinkedHashMap<>()));
		status.put("captures", captureStatuses());
		status.put("skills", safeValue(this::allSkillSnapshots, new LinkedHashMap<>()));
		status.put("prayers", safeValue(this::prayerStatus, new LinkedHashMap<>()));
		status.put("vitals", safeValue(this::vitalStatus, new LinkedHashMap<>()));
		status.put("wealth", safeValue(this::wealthStatus, new LinkedHashMap<>()));
		status.put("selectedWidget", safeValue(this::selectedWidgetStatus, new LinkedHashMap<>()));
		status.put("automation", safeValue(this::automationStatus, new LinkedHashMap<>()));
		return status;
	}

	List<Rectangle> getPrayerTargetBounds()
	{
		List<Rectangle> bounds = new ArrayList<>();
		for (Map<String, Object> target : lastPrayerTargets)
		{
			Object boundsValue = target.get("bounds");
			if (!(boundsValue instanceof Map))
			{
				continue;
			}

			Map<?, ?> boundsMap = (Map<?, ?>) boundsValue;
			Number x = (Number) boundsMap.get("x");
			Number y = (Number) boundsMap.get("y");
			Number width = (Number) boundsMap.get("width");
			Number height = (Number) boundsMap.get("height");
			if (x != null && y != null && width != null && height != null)
			{
				bounds.add(new Rectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue()));
			}
		}
		return bounds;
	}

	List<Rectangle> getSpellTargetBounds()
	{
		return getTargetBounds(lastSpellTargets);
	}

	List<Rectangle> getMinimapTargetBounds()
	{
		return getTargetBounds(lastMinimapTargets);
	}

	List<Rectangle> getInventoryTargetBounds()
	{
		return getTargetBounds(lastInventoryTargets);
	}

	List<Rectangle> getEquipmentTargetBounds()
	{
		return getTargetBounds(lastEquipmentTargets);
	}

	List<Rectangle> getPanelTargetBounds()
	{
		return getTargetBounds(lastPanelTargets);
	}

	List<Rectangle> getCombatTargetBounds()
	{
		return getTargetBounds(lastCombatTargets);
	}

	List<Map<String, Object>> getLivePrayerTargets()
	{
		lastPrayerTargets = collectPrayerTargets();
		return lastPrayerTargets;
	}

	List<Map<String, Object>> getLiveSpellTargets()
	{
		lastSpellTargets = collectSpellTargets();
		return lastSpellTargets;
	}

	List<Map<String, Object>> getLiveMinimapTargets()
	{
		lastMinimapTargets = collectMinimapTargets();
		return lastMinimapTargets;
	}

	List<Map<String, Object>> getLiveInventoryTargets()
	{
		lastInventoryTargets = collectInventoryTargets();
		return lastInventoryTargets;
	}

	List<Map<String, Object>> getLiveEquipmentTargets()
	{
		lastEquipmentTargets = collectEquipmentTargets();
		return lastEquipmentTargets;
	}

	List<Map<String, Object>> getLivePanelTargets()
	{
		lastPanelTargets = collectPanelTargets();
		return lastPanelTargets;
	}

	List<Map<String, Object>> getLiveCombatTargets()
	{
		lastCombatTargets = collectCombatTargets();
		return lastCombatTargets;
	}

	List<Map<String, Object>> getLiveEntities()
	{
		lastEntities = collectEntities();
		return lastEntities;
	}

	List<Map<String, Object>> getLiveSkillFarmerTargets()
	{
		List<Map<String, Object>> targets = new ArrayList<>();
		addSkillFarmerOverlayTargets(targets, lastMiningFarmerStatus);
		addSkillFarmerOverlayTargets(targets, lastWoodcuttingFarmerStatus);
		return targets;
	}

	private void addSkillFarmerOverlayTargets(List<Map<String, Object>> targets, Map<String, Object> status)
	{
		if (status == null || status.isEmpty())
		{
			return;
		}
		Set<String> seen = new HashSet<>();
		Object selected = status.get("selected");
		if (selected instanceof Map)
		{
			addSkillFarmerOverlayTarget(targets, seen, (Map<String, Object>) selected);
		}
		Object candidates = status.get("candidates");
		if (!(candidates instanceof List))
		{
			return;
		}
		for (Object value : (List<?>) candidates)
		{
			if (value instanceof Map)
			{
				addSkillFarmerOverlayTarget(targets, seen, (Map<String, Object>) value);
			}
		}
	}

	private void addSkillFarmerOverlayTarget(List<Map<String, Object>> targets, Set<String> seen, Map<String, Object> target)
	{
		if (target == null || !(target.get("bounds") instanceof Map))
		{
			return;
		}
		String key = target.get("skill") + ":" + target.get("id") + ":" + String.valueOf(target.get("worldLocation"));
		if (seen.add(key))
		{
			targets.add(target);
		}
	}

	List<Rectangle> getLivePrayerTargetBounds()
	{
		getLivePrayerTargets();
		return getPrayerTargetBounds();
	}

	List<Rectangle> getLiveSpellTargetBounds()
	{
		getLiveSpellTargets();
		return getSpellTargetBounds();
	}

	private List<Rectangle> getTargetBounds(List<Map<String, Object>> targets)
	{
		List<Rectangle> bounds = new ArrayList<>();
		for (Map<String, Object> target : targets)
		{
			Object boundsValue = target.get("bounds");
			if (!(boundsValue instanceof Map))
			{
				continue;
			}

			Map<?, ?> boundsMap = (Map<?, ?>) boundsValue;
			Number x = (Number) boundsMap.get("x");
			Number y = (Number) boundsMap.get("y");
			Number width = (Number) boundsMap.get("width");
			Number height = (Number) boundsMap.get("height");
			if (x != null && y != null && width != null && height != null)
			{
				bounds.add(new Rectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue()));
			}
		}
		return bounds;
	}

	void setShowHoverOverlay(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_HOVER_OVERLAY, value);
	}

	void setShowWidgetInfo(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_WIDGET_INFO, value);
	}

	void setShowPrayerTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_PRAYER_TARGETS, value);
		if (value)
		{
			refreshPrayerTargets();
		}
	}

	void setShowSpellTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_SPELL_TARGETS, value);
		if (value)
		{
			refreshSpellTargets();
		}
	}

	void setShowMinimapTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_MINIMAP_TARGETS, value);
		if (value)
		{
			refreshMinimapTargets();
		}
	}

	void setShowInventoryTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_INVENTORY_TARGETS, value);
		if (value)
		{
			refreshInventoryTargets();
		}
	}

	void setShowEquipmentTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_EQUIPMENT_TARGETS, value);
		if (value)
		{
			refreshEquipmentTargets();
		}
	}

	void setShowPanelTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_PANEL_TARGETS, value);
		if (value)
		{
			refreshTargets("panels", this::collectPanelTargets);
		}
	}

	void setShowCombatTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_COMBAT_TARGETS, value);
		if (value)
		{
			refreshTargets("combat", this::collectCombatTargets);
		}
	}

	void setShowEntityTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_ENTITY_TARGETS, value);
		if (value)
		{
			refreshEntities();
		}
	}

	void setShowSkillFarmerTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_SKILL_FARMER_TARGETS, value);
	}

	void setShowTargetLabels(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_TARGET_LABELS, value);
	}

	void setPrayerEnabled(String prayer, boolean value)
	{
		setEnabled(enabledPrayers, prayer, value);
		refreshPrayerTargets();
	}

	void setSpellEnabled(String spell, boolean value)
	{
		setEnabled(enabledSpellbooks, spell, value);
		refreshSpellTargets();
	}

	void setLocalExportEnabled(boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.ENABLE_LOCAL_EXPORT, value);
		if (value)
		{
			startServer();
		}
		else
		{
			stopServer();
		}
		updatePanelServerStatus();
	}

	void setWebhookUrl(String value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.WEBHOOK_URL, value == null ? "" : value);
	}

	void setActionHotkey(int slot, Keybind keybind)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionHotkey" + slot, keybind == null ? Keybind.NOT_SET : keybind);
		updatePanelStatus("Action " + slot + " hotkey saved");
	}

	void setActionEnabled(int slot, boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionEnabled" + slot, value);
		updatePanelStatus("Action " + slot + (value ? " enabled" : " disabled"));
	}

	void setActionSurface(int slot, CvHelperActionSurface surface)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionSurface" + slot, surface == null ? CvHelperActionSurface.DISABLED : surface);
		updatePanelStatus("Action " + slot + " surface saved");
	}

	void setActionTarget(int slot, String target)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionTarget" + slot, target == null ? "" : target.trim());
		updatePanelStatus("Action " + slot + " target saved");
	}

	void setActionClickMouse(int slot, boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionClickMouse" + slot, value);
		updatePanelStatus("Action " + slot + " mouse-after setting saved");
	}

	void setActionClickAfterMode(int slot, CvHelperClickAfterMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionClickAfterMode" + slot, mode == null ? CvHelperClickAfterMode.AUTO : mode);
		updatePanelStatus("Action " + slot + " click-after mode saved");
	}

	void setActionInvocationMode(int slot, CvHelperActionInvocationMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionInvocationMode" + slot, mode == null ? CvHelperActionInvocationMode.AUTO : mode);
		updatePanelStatus("Action " + slot + " invocation mode saved");
	}

	void setActionPrayerMode(int slot, CvHelperPrayerActionMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionPrayerMode" + slot, mode == null ? CvHelperPrayerActionMode.TOGGLE : mode);
		updatePanelStatus("Action " + slot + " prayer mode saved");
	}

	void setActionSpellAvailabilityMode(int slot, CvHelperSpellAvailabilityMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionSpellAvailabilityMode" + slot, mode == null ? CvHelperSpellAvailabilityMode.GUARD_UNAVAILABLE : mode);
		updatePanelStatus("Action " + slot + " spell guard saved");
	}

	void setActionReturnPanel(int slot, boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionReturnPanel" + slot, value);
		updatePanelStatus("Action " + slot + " return-panel setting saved");
	}

	void setActionReturnMouseCenter(int slot, boolean value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, "actionReturnMouseCenter" + slot, value);
		updatePanelStatus("Action " + slot + " center-mouse setting saved");
	}

	int getActionSlotCount()
	{
		return ACTION_SLOT_COUNT;
	}

	void resetActionSequence(int slot)
	{
		actionSequenceIndexes.remove(slot);
		updatePanelStatus("Action " + slot + " sequence memory reset");
	}

	Keybind getActionHotkey(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionHotkey1();
			case 2:
				return config.actionHotkey2();
			case 3:
				return config.actionHotkey3();
			case 4:
				return config.actionHotkey4();
			default:
				Keybind keybind = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionHotkey" + slot, Keybind.class);
				return keybind == null ? defaultActionHotkey(slot) : keybind;
		}
	}

	private Keybind defaultActionHotkey(int slot)
	{
		switch (slot)
		{
			case 1:
				return new Keybind(KeyEvent.VK_1, 0);
			case 2:
				return new Keybind(KeyEvent.VK_2, 0);
			case 3:
				return new Keybind(KeyEvent.VK_3, 0);
			case 4:
				return new Keybind(KeyEvent.VK_4, 0);
			case 5:
				return new Keybind(KeyEvent.VK_5, 0);
			case 6:
				return new Keybind(KeyEvent.VK_Q, 0);
			case 7:
				return new Keybind(KeyEvent.VK_W, 0);
			case 8:
				return new Keybind(KeyEvent.VK_E, 0);
			case 9:
				return new Keybind(KeyEvent.VK_R, 0);
			case 10:
				return new Keybind(KeyEvent.VK_T, 0);
			case 11:
				return new Keybind(KeyEvent.VK_A, 0);
			case 12:
				return new Keybind(KeyEvent.VK_S, 0);
			case 13:
				return new Keybind(KeyEvent.VK_D, 0);
			case 14:
				return new Keybind(KeyEvent.VK_F, 0);
			case 15:
				return new Keybind(KeyEvent.VK_G, 0);
			case 16:
				return new Keybind(KeyEvent.VK_Z, 0);
			case 17:
				return new Keybind(KeyEvent.VK_X, 0);
			case 18:
				return new Keybind(KeyEvent.VK_C, 0);
			case 19:
				return new Keybind(KeyEvent.VK_V, 0);
			case 20:
				return new Keybind(KeyEvent.VK_BACK_QUOTE, 0);
			case 21:
				return new Keybind(KeyEvent.VK_CAPS_LOCK, 0);
			case 22:
				return new Keybind(KeyEvent.VK_TAB, 0);
			default:
				return Keybind.NOT_SET;
		}
	}

	boolean getActionEnabled(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionEnabled1();
			case 2:
				return config.actionEnabled2();
			case 3:
				return config.actionEnabled3();
			case 4:
				return config.actionEnabled4();
			default:
				Boolean enabled = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionEnabled" + slot, Boolean.class);
				return enabled == null || enabled;
		}
	}

	CvHelperActionSurface getActionSurface(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionSurface1();
			case 2:
				return config.actionSurface2();
			case 3:
				return config.actionSurface3();
			case 4:
				return config.actionSurface4();
			default:
				CvHelperActionSurface surface = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionSurface" + slot, CvHelperActionSurface.class);
				return surface == null ? CvHelperActionSurface.DISABLED : surface;
		}
	}

	String getActionTarget(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionTarget1();
			case 2:
				return config.actionTarget2();
			case 3:
				return config.actionTarget3();
			case 4:
				return config.actionTarget4();
			default:
				String target = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionTarget" + slot);
				return target == null ? "" : target;
		}
	}

	boolean getActionClickMouse(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionClickMouse1();
			case 2:
				return config.actionClickMouse2();
			case 3:
				return config.actionClickMouse3();
			case 4:
				return config.actionClickMouse4();
			default:
				return false;
		}
	}

	CvHelperClickAfterMode getActionClickAfterMode(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionClickAfterMode1();
			case 2:
				return config.actionClickAfterMode2();
			case 3:
				return config.actionClickAfterMode3();
			case 4:
				return config.actionClickAfterMode4();
			default:
				CvHelperClickAfterMode mode = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionClickAfterMode" + slot, CvHelperClickAfterMode.class);
				return mode == null ? CvHelperClickAfterMode.AUTO : mode;
		}
	}

	CvHelperActionInvocationMode getActionInvocationMode(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionInvocationMode1();
			case 2:
				return config.actionInvocationMode2();
			case 3:
				return config.actionInvocationMode3();
			case 4:
				return config.actionInvocationMode4();
			default:
				CvHelperActionInvocationMode mode = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionInvocationMode" + slot, CvHelperActionInvocationMode.class);
				return mode == null ? CvHelperActionInvocationMode.AUTO : mode;
		}
	}

	CvHelperPrayerActionMode getActionPrayerMode(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionPrayerMode1();
			case 2:
				return config.actionPrayerMode2();
			case 3:
				return config.actionPrayerMode3();
			case 4:
				return config.actionPrayerMode4();
			default:
				CvHelperPrayerActionMode mode = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionPrayerMode" + slot, CvHelperPrayerActionMode.class);
				return mode == null ? CvHelperPrayerActionMode.TOGGLE : mode;
		}
	}

	CvHelperSpellAvailabilityMode getActionSpellAvailabilityMode(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionSpellAvailabilityMode1();
			case 2:
				return config.actionSpellAvailabilityMode2();
			case 3:
				return config.actionSpellAvailabilityMode3();
			case 4:
				return config.actionSpellAvailabilityMode4();
			default:
				CvHelperSpellAvailabilityMode mode = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionSpellAvailabilityMode" + slot, CvHelperSpellAvailabilityMode.class);
				return mode == null ? CvHelperSpellAvailabilityMode.GUARD_UNAVAILABLE : mode;
		}
	}

	boolean getActionReturnPanel(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionReturnPanel1();
			case 2:
				return config.actionReturnPanel2();
			case 3:
				return config.actionReturnPanel3();
			case 4:
				return config.actionReturnPanel4();
			default:
				Boolean returnPanel = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionReturnPanel" + slot, Boolean.class);
				return returnPanel == null || returnPanel;
		}
	}

	boolean getActionReturnMouseCenter(int slot)
	{
		switch (slot)
		{
			case 1:
				return config.actionReturnMouseCenter1();
			case 2:
				return config.actionReturnMouseCenter2();
			case 3:
				return config.actionReturnMouseCenter3();
			case 4:
				return config.actionReturnMouseCenter4();
			default:
				Boolean returnMouseCenter = configManager.getConfiguration(CvHelperModConfig.GROUP, "actionReturnMouseCenter" + slot, Boolean.class);
				return returnMouseCenter == null || returnMouseCenter;
		}
	}

	void performConfiguredAction(int slot)
	{
		if (!getActionEnabled(slot))
		{
			updatePanelStatus("Action " + slot + " is disabled");
			return;
		}
		if (!actionInProgress.compareAndSet(false, true))
		{
			updatePanelStatus("Action already running; ignored action " + slot);
			return;
		}
		performConfiguredAction(slot, getActionSurface(slot), getActionTarget(slot), getActionClickAfterMode(slot), getActionInvocationMode(slot), getActionReturnPanel(slot), getActionReturnMouseCenter(slot));
	}

	void debugOverlayState()
	{
		clientThread.invokeLater(() ->
		{
			Map<String, Object> player = getPlayerStatus();
			List<Rectangle> prayerBounds = getPrayerTargetBounds();
			StringBuilder sb = new StringBuilder();
			sb.append("CV Helper debug | ");
			sb.append("state=").append(player.get("gameState"));
			sb.append(" | prayerTargets=").append(prayerBounds.size());
			sb.append(" | spellTargets=").append(lastSpellTargets.size());
			sb.append(" | minimapTargets=").append(lastMinimapTargets.size());
			sb.append(" | inventoryTargets=").append(lastInventoryTargets.size());
			sb.append(" | equipmentTargets=").append(lastEquipmentTargets.size());
			sb.append(" | spellbook=").append(getSpellbookStatus().get("name"));
			sb.append(" | mouse=").append(client.getMouseCanvasPosition());
			Player localPlayer = client.getLocalPlayer();
			if (localPlayer != null)
			{
				sb.append(" | player=").append(localPlayer.getName());
			}
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", sb.toString(), "");
		});
	}

	void printOverlayCoordinates()
	{
		clientThread.invokeLater(() ->
		{
			StringBuilder sb = new StringBuilder("CV Helper overlays | ");
			sb.append("mouse=").append(client.getMouseCanvasPosition());
			sb.append(" | prayerTargets=").append(getPrayerTargetBounds());
			sb.append(" | spellTargets=").append(getSpellTargetBounds());
			sb.append(" | minimapTargets=").append(getMinimapTargetBounds());
			sb.append(" | inventoryTargets=").append(getInventoryTargetBounds());
			sb.append(" | equipmentTargets=").append(getEquipmentTargetBounds());
			sb.append(" | prayerPanel=").append(widgetSummary(client.getWidget(ComponentID.PRAYER_PARENT)));
			sb.append(" | quickPrayerPanel=").append(widgetSummary(client.getWidget(ComponentID.QUICK_PRAYER_PRAYERS)));
			sb.append(" | spellbook=").append(widgetSummary(client.getWidget(ComponentID.SPELLBOOK_PARENT)));
			sb.append(" | spellbookState=").append(getSpellbookStatus().get("name"));
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", sb.toString(), "");
		});
	}

	void printNearestEntityTarget()
	{
		clientThread.invokeLater(() ->
		{
			lastEntities = collectEntities();
			Map<String, Object> nearest = nearestClickableEntity(lastEntities);
			if (nearest == null)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CV Helper nearest entity | none with canvas click point", "");
				return;
			}

			StringBuilder sb = new StringBuilder("CV Helper nearest entity | ");
			sb.append(nearest.get("type")).append("=").append(nearest.get("name"));
			sb.append(" | distance=").append(nearest.get("distance"));
			sb.append(" | clickPoint=").append(nearest.get("clickPoint"));
			sb.append(" | world=").append(nearest.get("worldLocation"));
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", sb.toString(), "");
			lastEvent.set("nearest-entity@" + nearest.get("name") + "@" + Instant.now());
		});
	}

	void performConfiguredAction(int slot, CvHelperActionSurface surface, String targetLabel, CvHelperClickAfterMode clickAfterMode, CvHelperActionInvocationMode invocationMode, boolean returnPanel, boolean returnMouseCenter)
	{
		if (surface == null || surface == CvHelperActionSurface.DISABLED)
		{
			finishAction(slot, "Action " + slot + " has no surface");
			return;
		}
		CvHelperActionInvocationMode effectiveInvocationMode = invocationMode == null ? CvHelperActionInvocationMode.AUTO : invocationMode;

		Point currentMouseScreenPoint = currentMouseScreenPoint();
		clientThread.invokeLater(() ->
		{
			String previousPanel = actionStartSidePanel();
			PanelSwitchTarget requiredPanelTarget = requiredPanelTarget(surface, previousPanel);
			if (requiredPanelTarget != null)
			{
				runPanelOpenThenAction(slot, surface, targetLabel, clickAfterMode, effectiveInvocationMode, returnPanel, returnMouseCenter, previousPanel, currentMouseScreenPoint, requiredPanelTarget);
				return;
			}
			performConfiguredActionResolved(slot, surface, targetLabel, clickAfterMode, effectiveInvocationMode, returnPanel, returnMouseCenter, previousPanel, currentMouseScreenPoint);
		});
	}

	private void performConfiguredActionResolved(int slot, CvHelperActionSurface surface, String targetLabel, CvHelperClickAfterMode clickAfterMode, CvHelperActionInvocationMode invocationMode, boolean returnPanel, boolean returnMouseCenter, String previousPanel, Point originalMouseScreenPoint)
	{
		performConfiguredActionResolved(slot, surface, targetLabel, clickAfterMode, invocationMode, returnPanel, returnMouseCenter, previousPanel, originalMouseScreenPoint, ACTION_RESOLVE_RETRIES);
	}

	private void performConfiguredActionResolved(int slot, CvHelperActionSurface surface, String targetLabel, CvHelperClickAfterMode clickAfterMode, CvHelperActionInvocationMode invocationMode, boolean returnPanel, boolean returnMouseCenter, String previousPanel, Point originalMouseScreenPoint, int retriesRemaining)
	{
		clientThread.invokeLater(() ->
		{
			Map<String, Object> target = resolveActionTarget(slot, surface, targetLabel);
			if (target == null)
			{
				if (retriesRemaining > 0)
				{
					scheduleActionResolve(slot, surface, targetLabel, clickAfterMode, invocationMode, returnPanel, returnMouseCenter, previousPanel, originalMouseScreenPoint, retriesRemaining - 1);
					return;
				}
				finishAction(slot, "CV Helper action " + slot + " | no target matched " + surface + " / " + targetLabel);
				return;
			}

			String guardMessage = actionGuardMessage(slot, surface, target);
			if (guardMessage != null)
			{
				finishAction(slot, guardMessage);
				return;
			}

			Map<String, Object> clickPoint = firstPoint(target, "clickPoint", "center", "canvasTileCenter");
			if (clickPoint == null)
			{
				finishAction(slot, "CV Helper action " + slot + " | matched target has no canvas click point");
				return;
			}

			Map<String, Object> randomizedClickPoint = randomizedClickPoint(target, clickPoint);
			Point targetScreenPoint = canvasPointToScreen(randomizedClickPoint);
			boolean targetedSpell = isTargetedSpell(surface, target);
			boolean clickMouseAfterTarget = shouldClickMouseAfter(surface, target, clickAfterMode);
			Point mouseScreenPoint = clickMouseAfterTarget ? originalMouseScreenPoint : null;
			PanelSwitchTarget returnPanelTarget = returnPanel ? panelReturnTarget(previousPanel) : null;
			Point restoreMousePoint = returnMouseCenter ? originalMouseScreenPoint : null;
			boolean shouldTryWidgetAction = invocationMode == CvHelperActionInvocationMode.WIDGET
				|| (invocationMode == CvHelperActionInvocationMode.AUTO && isWidgetActionSurface(surface) && !targetedSpell);
			if (shouldTryWidgetAction)
			{
				if (invokeWidgetAction(surface, target, targetedSpell))
				{
					runRobotAfterWidgetAction(slot, surface, target, targetScreenPoint, mouseScreenPoint, returnPanelTarget, restoreMousePoint, targetedSpell);
					lastEvent.set("action-hotkey-" + slot + "@" + surface + "@widget@" + Instant.now());
					return;
				}
				if (invocationMode == CvHelperActionInvocationMode.WIDGET)
				{
					finishAction(slot, "CV Helper action " + slot + " | target has no widget action for " + surface + " / " + targetLabel);
					return;
				}
			}
			if (targetScreenPoint == null)
			{
				finishAction(slot, "CV Helper action " + slot + " | target is off-canvas");
				return;
			}

			runRobotClick(slot, surface, target, randomizedClickPoint, targetScreenPoint, mouseScreenPoint, returnPanelTarget, restoreMousePoint, targetedSpell);
			lastEvent.set("action-hotkey-" + slot + "@" + surface + "@" + Instant.now());
		});
	}

	private void scheduleActionResolve(int slot, CvHelperActionSurface surface, String targetLabel, CvHelperClickAfterMode clickAfterMode, CvHelperActionInvocationMode invocationMode, boolean returnPanel, boolean returnMouseCenter, String previousPanel, Point originalMouseScreenPoint, int retriesRemaining)
	{
		Thread retryThread = new Thread(() ->
		{
			try
			{
				Thread.sleep(timing(config.actionResolveDelayMs(), 0, 3000));
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				return;
			}
			performConfiguredActionResolved(slot, surface, targetLabel, clickAfterMode, invocationMode, returnPanel, returnMouseCenter, previousPanel, originalMouseScreenPoint, retriesRemaining);
		}, "cv-helper-action-resolve-retry");
		retryThread.setDaemon(true);
		retryThread.start();
	}

	private void runPanelOpenThenAction(int slot, CvHelperActionSurface surface, String targetLabel, CvHelperClickAfterMode clickAfterMode, CvHelperActionInvocationMode invocationMode, boolean returnPanel, boolean returnMouseCenter, String previousPanel, Point originalMouseScreenPoint, PanelSwitchTarget panelTarget)
	{
		Thread openThread = new Thread(() ->
		{
			try
			{
				Robot robot = new Robot();
				activatePanel(robot, panelTarget, true);
				robot.delay(timing(config.actionPanelOpenDelayMs(), 0, 1500));
				performConfiguredActionResolved(slot, surface, targetLabel, clickAfterMode, invocationMode, returnPanel, returnMouseCenter, previousPanel, originalMouseScreenPoint);
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				log.warn("CV Helper action panel-open failed", e);
				finishAction(slot, "CV Helper action " + slot + " panel open failed: " + e.getMessage());
			}
		}, "cv-helper-action-open-panel");
		openThread.setDaemon(true);
		openThread.start();
	}

	private void runRobotClick(int slot, CvHelperActionSurface surface, Map<String, Object> target, Map<String, Object> randomizedClickPoint, Point targetScreenPoint, Point mouseScreenPoint, PanelSwitchTarget returnPanelTarget, Point centerPoint, boolean targetedSpell)
	{
		Thread clickThread = new Thread(() ->
		{
			try
			{
				Robot robot = new Robot();
				clickScreenPoint(robot, targetScreenPoint);
				boolean clickedMouseTarget = false;
				if (mouseScreenPoint != null)
				{
					robot.delay(timing(config.actionWidgetTargetDelayMs(), 0, 3000));
					clickScreenPoint(robot, mouseScreenPoint);
					clickedMouseTarget = true;
				}
				boolean safeToClickReturnPanel = !targetedSpell || clickedMouseTarget;
				maybeReturnPanel(robot, returnPanelTarget, safeToClickReturnPanel);
				if (centerPoint != null)
				{
					robot.delay(timing(config.actionMouseRestoreDelayMs(), 0, 3000));
					robot.mouseMove(centerPoint.x, centerPoint.y);
				}
				advanceActionSequence(slot, target);
				finishAction(slot, "CV Helper action " + slot + " | clicked " + surface + " " + targetLabelForMessage(target) + " at " + randomizedClickPoint + (mouseScreenPoint == null ? " | no mouse target click" : " | then mouse@" + mouseScreenPoint.x + "," + mouseScreenPoint.y));
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				log.warn("CV Helper action hotkey failed", e);
				finishAction(slot, "CV Helper action " + slot + " failed: " + e.getMessage());
			}
		}, "cv-helper-action-click");
		clickThread.setDaemon(true);
		clickThread.start();
	}

	private void runRobotAfterWidgetAction(int slot, CvHelperActionSurface surface, Map<String, Object> target, Point physicalFallbackPoint, Point mouseScreenPoint, PanelSwitchTarget returnPanelTarget, Point restoreMousePoint, boolean targetedSpell)
	{
		Thread clickThread = new Thread(() ->
		{
			try
			{
				Robot robot = new Robot();
				boolean clickedMouseTarget = false;
				boolean usedPhysicalFallback = false;
				if (targetedSpell && !waitForSelectedWidget())
				{
					if (physicalFallbackPoint == null)
					{
						finishAction(slot, "CV Helper action " + slot + " | invoked " + surface + " " + targetLabelForMessage(target) + " via widget | spell was not selected before timeout, skipped mouse target click | " + selectedWidgetDebug());
						return;
					}
					clickScreenPoint(robot, physicalFallbackPoint);
					usedPhysicalFallback = true;
				}
				if (mouseScreenPoint != null)
				{
					robot.delay(timing(config.actionWidgetTargetDelayMs(), 0, 3000));
					clickScreenPoint(robot, mouseScreenPoint);
					clickedMouseTarget = true;
				}
				boolean safeToClickReturnPanel = !targetedSpell || clickedMouseTarget;
				maybeReturnPanel(robot, returnPanelTarget, safeToClickReturnPanel);
				if (restoreMousePoint != null)
				{
					robot.delay(timing(config.actionMouseRestoreDelayMs(), 0, 3000));
					robot.mouseMove(restoreMousePoint.x, restoreMousePoint.y);
				}
				String resultMessage = "CV Helper action " + slot + " | invoked " + surface + " " + targetLabelForMessage(target) + " via widget"
					+ (usedPhysicalFallback ? " | physical fallback selected" : "")
					+ (mouseScreenPoint == null ? " | no mouse target click" : " | then mouse@" + mouseScreenPoint.x + "," + mouseScreenPoint.y);
				advanceActionSequence(slot, target);
				finishAction(slot, resultMessage);
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				log.warn("CV Helper action widget follow-up failed", e);
				finishAction(slot, "CV Helper action " + slot + " widget follow-up failed: " + e.getMessage());
			}
		}, "cv-helper-action-widget-follow-up");
		clickThread.setDaemon(true);
		clickThread.start();
	}

	private boolean waitForSelectedWidget()
	{
		long deadline = System.currentTimeMillis() + timing(config.actionSelectedWidgetTimeoutMs(), 0, 5000);
		while (System.currentTimeMillis() <= deadline)
		{
			if (isWidgetSelectedOnClientThread())
			{
				return true;
			}
			try
			{
				Thread.sleep(25);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	private boolean isWidgetSelectedOnClientThread()
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Boolean> selected = new AtomicReference<>(false);
		clientThread.invokeLater(() ->
		{
			selected.set(client.isWidgetSelected());
			latch.countDown();
		});
		try
		{
			if (!latch.await(250, TimeUnit.MILLISECONDS))
			{
				return false;
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return false;
		}
		return Boolean.TRUE.equals(selected.get());
	}

	private String selectedWidgetDebug()
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> debug = new AtomicReference<>("selected=false");
		clientThread.invokeLater(() ->
		{
			Widget selectedWidget = client.getSelectedWidget();
			debug.set("selected=" + client.isWidgetSelected()
				+ ", widget=" + (selectedWidget == null ? "null" : selectedWidget.getId())
				+ ", name=" + (selectedWidget == null ? "" : cleanWidgetText(selectedWidget.getName()))
				+ ", text=" + (selectedWidget == null ? "" : cleanWidgetText(selectedWidget.getText())));
			latch.countDown();
		});
		try
		{
			latch.await(250, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		return debug.get();
	}

	private boolean isWidgetActionSurface(CvHelperActionSurface surface)
	{
		return surface == CvHelperActionSurface.SPELL || surface == CvHelperActionSurface.PRAYER;
	}

	private String actionGuardMessage(int slot, CvHelperActionSurface surface, Map<String, Object> target)
	{
		if (surface == CvHelperActionSurface.PRAYER)
		{
			return prayerGuardMessage(slot, target);
		}
		if (surface == CvHelperActionSurface.SPELL)
		{
			return spellGuardMessage(slot, target);
		}
		return null;
	}

	private String prayerGuardMessage(int slot, Map<String, Object> target)
	{
		CvHelperPrayerActionMode mode = getActionPrayerMode(slot);
		if (mode == CvHelperPrayerActionMode.TOGGLE)
		{
			return null;
		}
		Prayer prayer = prayerForTarget(target);
		if (prayer == null)
		{
			return "CV Helper action " + slot + " | prayer guard could not resolve state for " + targetLabelForMessage(target);
		}
		boolean active = client.isPrayerActive(prayer);
		if (mode == CvHelperPrayerActionMode.ON_ONLY && active)
		{
			return "CV Helper action " + slot + " | skipped " + friendlyName(prayer.name()) + " because it is already on";
		}
		if (mode == CvHelperPrayerActionMode.OFF_ONLY && !active)
		{
			return "CV Helper action " + slot + " | skipped " + friendlyName(prayer.name()) + " because it is already off";
		}
		return null;
	}

	private String spellGuardMessage(int slot, Map<String, Object> target)
	{
		if (getActionSpellAvailabilityMode(slot) == CvHelperSpellAvailabilityMode.ALLOW_ATTEMPT)
		{
			return null;
		}
		if (Boolean.TRUE.equals(target.get("spellUnavailable")))
		{
			return "CV Helper action " + slot + " | skipped unavailable spell " + targetLabelForMessage(target) + " | opacity=" + target.get("opacity") + ", clickMask=" + target.get("clickMask") + ", textColor=" + target.get("textColor");
		}
		return null;
	}

	private Prayer prayerForTarget(Map<String, Object> target)
	{
		Object explicit = target.get("prayer");
		if (explicit instanceof String)
		{
			try
			{
				return Prayer.valueOf((String) explicit);
			}
			catch (IllegalArgumentException ignored)
			{
				// Fall back to normalized haystack matching below.
			}
		}
		String haystack = normalizedTargetHaystack(target);
		for (Prayer prayer : Prayer.values())
		{
			if (haystack.contains(normalize(prayer.name())) || haystack.contains(normalize(friendlyName(prayer.name()))))
			{
				return prayer;
			}
		}
		return null;
	}

	private boolean invokeWidgetAction(CvHelperActionSurface surface, Map<String, Object> target, boolean targetedSpell)
	{
		Object widgetIdValue = target.get("widgetId");
		if (!(widgetIdValue instanceof Number))
		{
			return false;
		}

		int widgetId = ((Number) widgetIdValue).intValue();
		if (widgetId <= 0)
		{
			return false;
		}

		Object itemIdValue = target.get("itemId");
		int itemId = itemIdValue instanceof Number ? ((Number) itemIdValue).intValue() : -1;
		String label = targetLabelForMessage(target);
		if (surface == CvHelperActionSurface.SPELL && targetedSpell)
		{
			client.menuAction(0, widgetId, MenuAction.WIDGET_TARGET, 0, itemId, "Cast", label);
			return true;
		}

		String option = surface == CvHelperActionSurface.PRAYER ? "Activate" : "Cast";
		client.menuAction(-1, widgetId, MenuAction.CC_OP, 1, itemId, option, label);
		return true;
	}

	private int timing(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}

	private boolean shouldClickMouseAfter(CvHelperActionSurface surface, Map<String, Object> target, CvHelperClickAfterMode mode)
	{
		if (mode == CvHelperClickAfterMode.ALWAYS)
		{
			return true;
		}
		if (mode == CvHelperClickAfterMode.NEVER)
		{
			return false;
		}
		if (surface != CvHelperActionSurface.SPELL)
		{
			return false;
		}

		String label = normalizedTargetHaystack(target);
		if (isCombatSpell(label))
		{
			return true;
		}
		return !isSelfResolvingSpell(label);
	}

	private boolean isTargetedSpell(CvHelperActionSurface surface, Map<String, Object> target)
	{
		if (surface != CvHelperActionSurface.SPELL)
		{
			return false;
		}
		String label = normalizedTargetHaystack(target);
		return !isSelfResolvingSpell(label);
	}

	private boolean isCombatSpell(String normalizedLabel)
	{
		return normalizedLabel.contains("strike")
			|| normalizedLabel.contains("bolt")
			|| normalizedLabel.contains("blast")
			|| normalizedLabel.contains("wave")
			|| normalizedLabel.contains("surge")
			|| normalizedLabel.contains("rush")
			|| normalizedLabel.contains("burst")
			|| normalizedLabel.contains("blitz")
			|| normalizedLabel.contains("barrage")
			|| normalizedLabel.contains("crumbleundead")
			|| normalizedLabel.contains("teleblock")
			|| normalizedLabel.contains("bind")
			|| normalizedLabel.contains("snare")
			|| normalizedLabel.contains("entangle");
	}

	private boolean isSelfResolvingSpell(String normalizedLabel)
	{
		return normalizedLabel.contains("teleport")
			|| normalizedLabel.contains("home")
			|| normalizedLabel.contains("varrock")
			|| normalizedLabel.contains("lumbridge")
			|| normalizedLabel.contains("falador")
			|| normalizedLabel.contains("camelot")
			|| normalizedLabel.contains("ardougne")
			|| normalizedLabel.contains("watchtower")
			|| normalizedLabel.contains("trollheim")
			|| normalizedLabel.contains("kourend")
			|| normalizedLabel.contains("barrows")
			|| normalizedLabel.contains("apeatoll")
			|| normalizedLabel.contains("ourania")
			|| normalizedLabel.contains("resurrect")
			|| normalizedLabel.contains("vengeance");
	}

	private String actionStartSidePanel()
	{
		String activePanel = String.valueOf(interfaceStatus().get("activeSidePanel"));
		if (isKnownSidePanel(activePanel))
		{
			return activePanel;
		}
		return isKnownSidePanel(lastStableSidePanel) ? lastStableSidePanel : "inventory";
	}

	private boolean isKnownSidePanel(String panelName)
	{
		return "combat".equals(panelName)
			|| "inventory".equals(panelName)
			|| "equipment".equals(panelName)
			|| "prayer".equals(panelName)
			|| "spellbook".equals(panelName);
	}

	private PanelSwitchTarget panelReturnTarget(String previousPanel)
	{
		String panelName = isKnownSidePanel(previousPanel) ? previousPanel : "inventory";
		return panelSwitchTarget(panelName, true);
	}

	private PanelSwitchTarget requiredPanelTarget(CvHelperActionSurface surface, String activePanel)
	{
		String requiredPanel = requiredPanelName(surface);
		if (requiredPanel == null || requiredPanel.equals(activePanel))
		{
			return null;
		}

		return panelSwitchTarget(requiredPanel, true);
	}

	private PanelSwitchTarget panelSwitchTarget(String panelName, boolean preferClick)
	{
		if (panelName == null || panelName.isEmpty() || "unknown".equals(panelName))
		{
			return null;
		}

		Keybind keybind = panelKeybind(panelName);
		Map<String, Object> panelTarget = findTargetByLabel(collectPanelTargets(), panelName, panelName.equals("spellbook") ? "magic" : panelName);
		Map<String, Object> clickPoint = panelTarget == null ? null : firstPoint(panelTarget, "clickPoint", "center");
		Point screenPoint = clickPoint == null ? null : canvasPointToScreen(clickPoint);
		if (preferClick && screenPoint != null)
		{
			return new PanelSwitchTarget(panelName, keybind, screenPoint);
		}
		if (keybind != null && !Keybind.NOT_SET.equals(keybind))
		{
			return new PanelSwitchTarget(panelName, keybind, screenPoint);
		}
		return screenPoint == null ? null : new PanelSwitchTarget(panelName, null, screenPoint);
	}

	private Keybind panelKeybind(String panelName)
	{
		switch (panelName)
		{
			case "combat":
				return config.actionReturnCombatHotkey();
			case "inventory":
				return config.actionReturnInventoryHotkey();
			case "equipment":
				return config.actionReturnEquipmentHotkey();
			case "prayer":
				return config.actionReturnPrayerHotkey();
			case "spellbook":
				return config.actionReturnSpellbookHotkey();
			default:
				return null;
		}
	}

	private String requiredPanelName(CvHelperActionSurface surface)
	{
		switch (surface)
		{
			case PRAYER:
				return "prayer";
			case SPELL:
				return "spellbook";
			case INVENTORY:
				return "inventory";
			case EQUIPMENT:
				return "equipment";
			case COMBAT:
				return "combat";
			default:
				return null;
		}
	}

	private void clickScreenPoint(Robot robot, Point point)
	{
		robot.mouseMove(point.x, point.y);
		robot.delay(timing(config.actionMouseSettleDelayMs(), 0, 1000));
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(18);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private void finishAction(int slot, String message)
	{
		actionInProgress.set(false);
		updatePanelStatus(message);
		clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, ""));
	}

	private boolean maybeReturnPanel(Robot robot, PanelSwitchTarget panelTarget, boolean allowMouseClick)
	{
		if (panelTarget == null)
		{
			return false;
		}
		if (!allowMouseClick && !panelTarget.usesHotkey())
		{
			return false;
		}
		robot.delay(timing(config.actionReturnPanelDelayMs(), 0, 3000));
		return activatePanel(robot, panelTarget, allowMouseClick);
	}

	private boolean activatePanel(Robot robot, PanelSwitchTarget panelTarget, boolean preferClick)
	{
		if (panelTarget == null)
		{
			return false;
		}
		if (preferClick && panelTarget.clickPoint != null)
		{
			clickScreenPoint(robot, panelTarget.clickPoint);
			return true;
		}
		if (panelTarget.usesHotkey())
		{
			pressKeybind(robot, panelTarget.keybind);
			return true;
		}
		if (panelTarget.clickPoint != null)
		{
			clickScreenPoint(robot, panelTarget.clickPoint);
			return true;
		}
		return false;
	}

	private void pressKeybind(Robot robot, Keybind keybind)
	{
		if (keybind == null || Keybind.NOT_SET.equals(keybind))
		{
			return;
		}
		int modifiers = keybind.getModifiers();
		pressModifier(robot, modifiers, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_CONTROL);
		pressModifier(robot, modifiers, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_ALT);
		pressModifier(robot, modifiers, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_SHIFT);
		pressModifier(robot, modifiers, InputEvent.META_DOWN_MASK, KeyEvent.VK_META);
		if (keybind.getKeyCode() != KeyEvent.VK_UNDEFINED)
		{
			robot.keyPress(keybind.getKeyCode());
			robot.delay(18);
			robot.keyRelease(keybind.getKeyCode());
		}
		releaseModifier(robot, modifiers, InputEvent.META_DOWN_MASK, KeyEvent.VK_META);
		releaseModifier(robot, modifiers, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_SHIFT);
		releaseModifier(robot, modifiers, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_ALT);
		releaseModifier(robot, modifiers, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_CONTROL);
	}

	private void pressModifier(Robot robot, int modifiers, int mask, int keyCode)
	{
		if ((modifiers & mask) != 0)
		{
			robot.keyPress(keyCode);
		}
	}

	private void releaseModifier(Robot robot, int modifiers, int mask, int keyCode)
	{
		if ((modifiers & mask) != 0)
		{
			robot.keyRelease(keyCode);
		}
	}

	private Point currentMouseScreenPoint()
	{
		return MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
	}

	private Map<String, Object> randomizedClickPoint(Map<String, Object> target, Map<String, Object> clickPoint)
	{
		Object boundsValue = target.get("bounds");
		if (!(boundsValue instanceof Map))
		{
			boundsValue = target.get("canvasBounds");
		}
		if (!(boundsValue instanceof Map))
		{
			return clickPoint;
		}

		Map<?, ?> bounds = (Map<?, ?>) boundsValue;
		Number width = (Number) bounds.get("width");
		Number height = (Number) bounds.get("height");
		Number x = (Number) clickPoint.get("x");
		Number y = (Number) clickPoint.get("y");
		if (width == null || height == null || x == null || y == null)
		{
			return clickPoint;
		}

		double radius = Math.max(1.0, Math.min(10.0, Math.min(width.doubleValue(), height.doubleValue()) / 2.0 - 2.0));
		double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
		double distance = Math.sqrt(ThreadLocalRandom.current().nextDouble()) * radius;
		int randomizedX = (int) Math.round(x.doubleValue() + Math.cos(angle) * distance);
		int randomizedY = (int) Math.round(y.doubleValue() + Math.sin(angle) * distance);
		return pointMap(randomizedX, randomizedY);
	}

	private Map<String, Object> resolveActionTarget(int slot, CvHelperActionSurface surface, String targetLabel)
	{
		List<String> candidates = actionTargetCandidates(targetLabel);
		boolean sequenced = isSequencedActionTarget(targetLabel);
		int candidateCount = Math.max(1, candidates.size());
		int startIndex = sequenced ? Math.floorMod(actionSequenceIndexes.getOrDefault(slot, 0), candidateCount) : 0;
		if (surface == CvHelperActionSurface.NEAREST_ENTITY)
		{
			lastEntities = collectEntities();
			for (int attempt = 0; attempt < candidateCount; attempt++)
			{
				int candidateIndex = sequenced ? (startIndex + attempt) % candidateCount : attempt;
				String candidate = candidates.get(candidateIndex);
				String needle = normalize(candidate);
				Map<String, Object> target = needle.isEmpty() || "nearestclickableentity".equals(needle)
					? nearestClickableEntity(lastEntities)
					: findEntityByNameOrId(lastEntities, candidate);
				if (target != null)
				{
					return actionResolvedTarget(target, candidate, candidateIndex, candidateCount, sequenced);
				}
			}
			return null;
		}

		List<Map<String, Object>> targets = collectActionSurfaceTargets(surface);
		List<Map<String, Object>> cachedTargets = cachedActionSurfaceTargets(surface);
		for (int attempt = 0; attempt < candidateCount; attempt++)
		{
			int candidateIndex = sequenced ? (startIndex + attempt) % candidateCount : attempt;
			String candidate = candidates.get(candidateIndex);
			String needle = normalize(candidate);
			Map<String, Object> target;
			if (needle.isEmpty())
			{
				target = !targets.isEmpty() ? targets.get(0) : (cachedTargets.isEmpty() ? null : cachedTargets.get(0));
			}
			else
			{
				target = findActionTargetByLabel(surface, targets, candidate);
				if (target == null)
				{
					target = findActionTargetByLabel(surface, cachedTargets, candidate);
				}
			}
			if (target != null)
			{
				return actionResolvedTarget(target, candidate, candidateIndex, candidateCount, sequenced);
			}
		}
		return null;
	}

	private List<String> actionTargetCandidates(String targetLabel)
	{
		List<String> candidates = new ArrayList<>();
		if (targetLabel == null || targetLabel.trim().isEmpty())
		{
			candidates.add("");
			return candidates;
		}

		String separator = isSequencedActionTarget(targetLabel) ? "\\s*->\\s*" : "\\s*(?:\\||;|,|\\r?\\n)\\s*";
		for (String part : targetLabel.trim().split(separator))
		{
			String candidate = part.trim();
			if (!candidate.isEmpty())
			{
				candidates.add(candidate);
			}
		}
		if (candidates.isEmpty())
		{
			candidates.add(targetLabel.trim());
		}
		return candidates;
	}

	private boolean isSequencedActionTarget(String targetLabel)
	{
		return targetLabel != null && targetLabel.contains("->");
	}

	private Map<String, Object> actionResolvedTarget(Map<String, Object> target, String candidate, int candidateIndex, int candidateCount, boolean sequenced)
	{
		Map<String, Object> copy = new LinkedHashMap<>(target);
		copy.put("actionCandidate", candidate);
		copy.put("actionCandidateIndex", candidateIndex);
		copy.put("actionCandidateCount", candidateCount);
		copy.put("actionSequence", sequenced);
		return copy;
	}

	private Map<String, Object> findActionTargetByLabel(CvHelperActionSurface surface, List<Map<String, Object>> targets, String needle)
	{
		if (surface == CvHelperActionSurface.INVENTORY || surface == CvHelperActionSurface.EQUIPMENT)
		{
			Map<String, Object> doseAware = findDoseAwareTarget(targets, needle);
			if (doseAware != null)
			{
				return doseAware;
			}
		}
		return findTargetByLabel(targets, needle);
	}

	private Map<String, Object> findDoseAwareTarget(List<Map<String, Object>> targets, String needle)
	{
		String normalizedNeedle = normalizeDoseAgnostic(needle);
		if (normalizedNeedle.isEmpty())
		{
			return null;
		}

		Map<String, Object> best = null;
		int bestDose = Integer.MAX_VALUE;
		for (Map<String, Object> target : targets)
		{
			String haystack = normalizedDoseAgnosticHaystack(target);
			if (!haystack.contains(normalizedNeedle))
			{
				continue;
			}
			int dose = itemDose(target);
			if (best == null || dose < bestDose)
			{
				best = target;
				bestDose = dose;
			}
		}
		return best;
	}

	private String normalizedDoseAgnosticHaystack(Map<String, Object> target)
	{
		return normalizeDoseAgnostic(targetLabelForMessage(target)
			+ " " + target.get("name")
			+ " " + target.get("text")
			+ " " + target.get("itemName")
			+ " " + target.get("itemId")
			+ " " + actionsText(target.get("actions")));
	}

	private String normalizeDoseAgnostic(String value)
	{
		return normalize(value == null ? "" : value.replaceAll("\\(\\d+\\)", ""));
	}

	private int itemDose(Map<String, Object> target)
	{
		String text = targetLabelForMessage(target)
			+ " " + target.get("name")
			+ " " + target.get("text")
			+ " " + target.get("itemName");
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\((\\d+)\\)").matcher(text);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
	}

	private void advanceActionSequence(int slot, Map<String, Object> target)
	{
		if (!Boolean.TRUE.equals(target.get("actionSequence")))
		{
			return;
		}
		Object candidateCountValue = target.get("actionCandidateCount");
		Object candidateIndexValue = target.get("actionCandidateIndex");
		if (!(candidateCountValue instanceof Number) || !(candidateIndexValue instanceof Number))
		{
			return;
		}
		int candidateCount = ((Number) candidateCountValue).intValue();
		if (candidateCount <= 1)
		{
			return;
		}
		int candidateIndex = ((Number) candidateIndexValue).intValue();
		actionSequenceIndexes.put(slot, Math.floorMod(candidateIndex + 1, candidateCount));
	}

	private Map<String, Object> findEntityByNameOrId(List<Map<String, Object>> entities, String targetLabel)
	{
		String needle = normalize(targetLabel);
		if (needle.isEmpty())
		{
			return nearestClickableEntity(entities);
		}

		String idNeedle = needle.startsWith("id") ? normalize(needle.substring(2)) : needle;
		Map<String, Object> best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (Map<String, Object> entity : entities)
		{
			if (!(entity.get("clickPoint") instanceof Map))
			{
				continue;
			}

			boolean idMatches = false;
			Object idValue = entity.get("id");
			if (idValue instanceof Number)
			{
				String id = String.valueOf(((Number) idValue).intValue());
				idMatches = id.equals(idNeedle) || id.equals(needle);
			}

			String haystack = normalize(entity.get("name") + " " + entity.get("type") + " " + entity.get("id"));
			if (!idMatches && !haystack.contains(needle))
			{
				continue;
			}

			int distance = entity.get("distance") instanceof Number ? ((Number) entity.get("distance")).intValue() : Integer.MAX_VALUE;
			if (best == null || distance < bestDistance)
			{
				best = entity;
				bestDistance = distance;
			}
		}
		return best;
	}

	private Map<String, Object> findTargetByLabel(List<Map<String, Object>> targets, String... needles)
	{
		for (Map<String, Object> target : targets)
		{
			String haystack = normalizedTargetHaystack(target);
			for (String needle : needles)
			{
				String normalizedNeedle = normalize(needle);
				if (!normalizedNeedle.isEmpty() && haystack.contains(normalizedNeedle))
				{
					return target;
				}
			}
		}
		return null;
	}

	private List<Map<String, Object>> collectActionSurfaceTargets(CvHelperActionSurface surface)
	{
		switch (surface)
		{
			case PRAYER:
				return collectPrayerTargets();
			case SPELL:
				return collectSpellTargets();
			case MINIMAP:
				return collectMinimapTargets();
			case INVENTORY:
				return collectInventoryTargets();
			case EQUIPMENT:
				return collectEquipmentTargets();
			case PANELS:
				return collectPanelTargets();
			case COMBAT:
				return collectCombatTargets();
			default:
				return new ArrayList<>();
		}
	}

	private List<Map<String, Object>> cachedActionSurfaceTargets(CvHelperActionSurface surface)
	{
		switch (surface)
		{
			case PRAYER:
				return lastPrayerTargets;
			case SPELL:
				return lastSpellTargets;
			case MINIMAP:
				return lastMinimapTargets;
			case INVENTORY:
				return lastInventoryTargets;
			case EQUIPMENT:
				return lastEquipmentTargets;
			case PANELS:
				return lastPanelTargets;
			case COMBAT:
				return lastCombatTargets;
			default:
				return new ArrayList<>();
		}
	}

	private Map<String, Object> firstPoint(Map<String, Object> target, String... keys)
	{
		for (String key : keys)
		{
			Object value = target.get(key);
			if (value instanceof Map)
			{
				return (Map<String, Object>) value;
			}
		}
		return null;
	}

	private Point canvasPointToScreen(Map<String, Object> canvasPoint)
	{
		if (canvasPoint == null)
		{
			return null;
		}
		Number x = (Number) canvasPoint.get("x");
		Number y = (Number) canvasPoint.get("y");
		if (x == null || y == null)
		{
			return null;
		}

		Point canvasLocation = client.getCanvas().getLocationOnScreen();
		Dimension realDimensions = client.getRealDimensions();
		Dimension displayedDimensions = client.isStretchedEnabled() ? client.getStretchedDimensions() : realDimensions;
		double scaleX = realDimensions == null || realDimensions.width <= 0 || displayedDimensions == null ? 1.0 : displayedDimensions.getWidth() / realDimensions.getWidth();
		double scaleY = realDimensions == null || realDimensions.height <= 0 || displayedDimensions == null ? 1.0 : displayedDimensions.getHeight() / realDimensions.getHeight();
		return new Point(
			canvasLocation.x + (int) Math.round(x.doubleValue() * scaleX),
			canvasLocation.y + (int) Math.round(y.doubleValue() * scaleY)
		);
	}

	private String targetLabelForMessage(Map<String, Object> target)
	{
		Object label = target.get("label");
		if (label == null || String.valueOf(label).trim().isEmpty() || "null".equals(label))
		{
			label = target.get("name");
		}
		return label == null ? "(unnamed)" : String.valueOf(label);
	}

	void refreshPrayerTargets()
	{
		clientThread.invokeLater(() ->
		{
			lastPrayerTargets = collectPrayerTargets();
			lastEvent.set("prayer-targets@" + lastPrayerTargets.size() + "@" + Instant.now());
			sendWebhook(eventPayload("prayer-targets", lastPrayerTargets));
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.updateStatus("Prayer targets: " + lastPrayerTargets.size());
				}
			});
		});
	}

	void refreshSpellTargets()
	{
		clientThread.invokeLater(() ->
		{
			lastSpellTargets = collectSpellTargets();
			lastEvent.set("spell-targets@" + lastSpellTargets.size() + "@" + Instant.now());
			sendWebhook(eventPayload("spell-targets", lastSpellTargets));
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.updateStatus("Spell targets: " + lastSpellTargets.size());
				}
			});
		});
	}

	void refreshMinimapTargets()
	{
		refreshTargets("minimap", this::collectMinimapTargets);
	}

	void refreshInventoryTargets()
	{
		refreshTargets("inventory", this::collectInventoryTargets);
	}

	void refreshEquipmentTargets()
	{
		refreshTargets("equipment", this::collectEquipmentTargets);
	}

	void refreshEntities()
	{
		clientThread.invokeLater(() ->
		{
			lastEntities = collectEntities();
			lastEvent.set("entities@" + lastEntities.size() + "@" + Instant.now());
			sendWebhook(eventPayload("entities", lastEntities));
			updatePanelStatus("Entities: " + lastEntities.size());
		});
	}

	String getMobFarmerTarget()
	{
		return mobFarmerTarget;
	}

	void setMobFarmerTarget(String target)
	{
		mobFarmerTarget = normalizedMobFarmerTarget(target);
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_TARGET, mobFarmerTarget);
		mobFarmerStatus.set("target@" + mobFarmerTarget);
		updatePanelStatus("Mob farmer target: " + mobFarmerTarget);
	}

	String getWoodcuttingFarmerTarget()
	{
		return woodcuttingFarmerTarget;
	}

	void setWoodcuttingFarmerTarget(String target)
	{
		woodcuttingFarmerTarget = target.trim();
	}

	boolean getWoodcuttingFarmerRunning()
	{
		return woodcuttingFarmerRunning.get();
	}

	void startWoodcuttingFarmer(boolean live)
	{
		startSkillFarmer("woodcutting", live);
	}

	void stopWoodcuttingFarmer()
	{
		stopSkillFarmer("woodcutting");
	}

	void runWoodcuttingFarmerStep(boolean live)
	{
		runSkillFarmerStep("woodcutting", live, "manual-step");
	}

	String getMiningFarmerTarget()
	{
		return miningFarmerTarget;
	}

	void setMiningFarmerTarget(String target)
	{
		miningFarmerTarget = target.trim();
	}

	boolean getMiningFarmerRunning()
	{
		return miningFarmerRunning.get();
	}

	void startMiningFarmer(boolean live)
	{
		startSkillFarmer("mining", live);
	}

	void stopMiningFarmer()
	{
		stopSkillFarmer("mining");
	}

	void runMiningFarmerStep(boolean live)
	{
		runSkillFarmerStep("mining", live, "manual-step");
	}

	int getMobFarmerRecoveryLoopDelayMs()
	{
		return Math.max(250, Math.min(10000, config.mobFarmerRecoveryLoopDelayMs()));
	}

	void setMobFarmerRecoveryLoopDelayMs(int delayMs)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_RECOVERY_LOOP_DELAY_MS, Math.max(250, Math.min(10000, delayMs)));
	}

	boolean getMobFarmerAutorunEnabled()
	{
		return config.mobFarmerAutorunEnabled();
	}

	void setMobFarmerAutorunEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_AUTORUN_ENABLED, enabled);
	}

	int getMobFarmerAutorunMinEnergy()
	{
		return Math.max(1, Math.min(100, config.mobFarmerAutorunMinEnergy()));
	}

	void setMobFarmerAutorunMinEnergy(int energy)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_AUTORUN_MIN_ENERGY, Math.max(1, Math.min(100, energy)));
	}

	boolean getMobFarmerFocusClickAfterLogin()
	{
		return config.mobFarmerFocusClickAfterLogin();
	}

	void setMobFarmerFocusClickAfterLogin(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_FOCUS_CLICK_AFTER_LOGIN, enabled);
		if (enabled && mobFarmerRunning.get())
		{
			mobFarmerFocusClickNeeded = true;
			recordMobFarmerFocusClick("needed-after-config-enable", "config-enable", false, null);
		}
	}

	CvHelperAfterLootCombatMode getMobFarmerAfterLootCombatMode()
	{
		CvHelperAfterLootCombatMode mode = config.mobFarmerAfterLootCombatMode();
		return mode == null ? CvHelperAfterLootCombatMode.STAY_ON_CURRENT_ATTACKER : mode;
	}

	void setMobFarmerAfterLootCombatMode(CvHelperAfterLootCombatMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_AFTER_LOOT_COMBAT_MODE, mode == null ? CvHelperAfterLootCombatMode.STAY_ON_CURRENT_ATTACKER : mode);
	}

	CvHelperMobEngagedMode getMobFarmerEngagedMode()
	{
		CvHelperMobEngagedMode mode = config.mobFarmerEngagedMode();
		return mode == null ? CvHelperMobEngagedMode.PREFER_FREE : mode;
	}

	void setMobFarmerEngagedMode(CvHelperMobEngagedMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_ENGAGED_MODE, mode == null ? CvHelperMobEngagedMode.PREFER_FREE : mode);
	}

	CvHelperMobAggroResponse getMobFarmerAggroResponse()
	{
		CvHelperMobAggroResponse response = config.mobFarmerAggroResponse();
		return response == null ? CvHelperMobAggroResponse.WAIT : response;
	}

	void setMobFarmerAggroResponse(CvHelperMobAggroResponse response)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_AGGRO_RESPONSE, response == null ? CvHelperMobAggroResponse.WAIT : response);
	}

	boolean getMobFarmerRequireLineOfSight()
	{
		return config.mobFarmerRequireLineOfSight();
	}

	void setMobFarmerRequireLineOfSight(boolean requireLineOfSight)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_REQUIRE_LINE_OF_SIGHT, requireLineOfSight);
	}

	int getMobFarmerMaxDistance()
	{
		return Math.max(0, config.mobFarmerMaxDistance());
	}

	void setMobFarmerMaxDistance(int maxDistance)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_MAX_DISTANCE, Math.max(0, maxDistance));
	}

	boolean getMobFarmerAutoEatEnabled()
	{
		return config.mobFarmerAutoEatEnabled();
	}

	void setMobFarmerAutoEatEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_AUTO_EAT_ENABLED, enabled);
	}

	int getMobFarmerEatHitpointPercent()
	{
		return Math.max(1, Math.min(99, config.mobFarmerEatHitpointPercent()));
	}

	void setMobFarmerEatHitpointPercent(int percent)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_EAT_HITPOINT_PERCENT, Math.max(1, Math.min(99, percent)));
	}

	String getMobFarmerFoodItems()
	{
		return config.mobFarmerFoodItems() == null ? "" : config.mobFarmerFoodItems();
	}

	void setMobFarmerFoodItems(String items)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_FOOD_ITEMS, items == null ? "" : items.trim());
	}

	boolean getMobFarmerStopIfNoFood()
	{
		return config.mobFarmerStopIfNoFood();
	}

	void setMobFarmerStopIfNoFood(boolean stop)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_STOP_IF_NO_FOOD, stop);
	}

	boolean getMobFarmerSurvivalPreemptsActions()
	{
		return config.mobFarmerSurvivalPreemptsActions();
	}

	void setMobFarmerSurvivalPreemptsActions(boolean preempts)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_SURVIVAL_PREEMPTS_ACTIONS, preempts);
	}

	boolean getMobFarmerLoginRecoveryEnabled()
	{
		return config.mobFarmerLoginRecoveryEnabled();
	}

	void setMobFarmerLoginRecoveryEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOGIN_RECOVERY_ENABLED, enabled);
	}

	boolean getMobFarmerLoginRecoveryF2pOnly()
	{
		return config.mobFarmerLoginRecoveryF2pOnly();
	}

	void setMobFarmerLoginRecoveryF2pOnly(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOGIN_RECOVERY_F2P_ONLY, enabled);
	}

	boolean getMobFarmerLoginClickToPlayEnabled()
	{
		return config.mobFarmerLoginClickToPlayEnabled();
	}

	void setMobFarmerLoginClickToPlayEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOGIN_CLICK_TO_PLAY_ENABLED, enabled);
	}

	boolean getMobFarmerLoginDisconnectRecoveryEnabled()
	{
		return config.mobFarmerLoginDisconnectRecoveryEnabled();
	}

	void setMobFarmerLoginDisconnectRecoveryEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOGIN_DISCONNECT_RECOVERY_ENABLED, enabled);
	}

	boolean getMobFarmerAutoResumeAfterLogin()
	{
		return config.mobFarmerAutoResumeAfterLogin();
	}

	void setMobFarmerAutoResumeAfterLogin(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_AUTO_RESUME_AFTER_LOGIN, enabled);
	}

	int getMobFarmerPreferredLoginWorld()
	{
		return Math.max(0, config.mobFarmerPreferredLoginWorld());
	}

	void setMobFarmerPreferredLoginWorld(int world)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_PREFERRED_LOGIN_WORLD, Math.max(0, world));
	}

	boolean getMobFarmerLootEnabled()
	{
		return config.mobFarmerLootEnabled();
	}

	void setMobFarmerLootEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_ENABLED, enabled);
	}

	boolean getMobFarmerLootDuringCombat()
	{
		return config.mobFarmerLootDuringCombat();
	}

	void setMobFarmerLootDuringCombat(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_DURING_COMBAT, enabled);
	}

	boolean getMobFarmerAttackBeforeLoot()
	{
		return config.mobFarmerAttackBeforeLoot();
	}

	void setMobFarmerAttackBeforeLoot(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_ATTACK_BEFORE_LOOT, enabled);
	}

	int getMobFarmerLootMinValueGe()
	{
		return Math.max(0, config.mobFarmerLootMinValueGe());
	}

	void setMobFarmerLootMinValueGe(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_MIN_VALUE_GE, Math.max(0, value));
	}

	int getMobFarmerLootMinSingleGe()
	{
		return Math.max(0, config.mobFarmerLootMinSingleGe());
	}

	void setMobFarmerLootMinSingleGe(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_MIN_SINGLE_GE, Math.max(0, value));
	}

	int getMobFarmerLootMinStackGe()
	{
		return Math.max(0, config.mobFarmerLootMinStackGe());
	}

	void setMobFarmerLootMinStackGe(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_MIN_STACK_GE, Math.max(0, value));
	}

	int getMobFarmerLootMinStackQuantity()
	{
		return Math.max(0, config.mobFarmerLootMinStackQuantity());
	}

	void setMobFarmerLootMinStackQuantity(int quantity)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_MIN_STACK_QUANTITY, Math.max(0, quantity));
	}

	int getMobFarmerLootAlwaysStackGe()
	{
		return Math.max(0, config.mobFarmerLootAlwaysStackGe());
	}

	void setMobFarmerLootAlwaysStackGe(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_ALWAYS_STACK_GE, Math.max(0, value));
	}

	int getMobFarmerLootNeverStackBelowGe()
	{
		return Math.max(0, config.mobFarmerLootNeverStackBelowGe());
	}

	void setMobFarmerLootNeverStackBelowGe(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_NEVER_STACK_BELOW_GE, Math.max(0, value));
	}

	int getMobFarmerHighPriorityLootValueGe()
	{
		return Math.max(0, config.mobFarmerHighPriorityLootValueGe());
	}

	void setMobFarmerHighPriorityLootValueGe(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_HIGH_PRIORITY_LOOT_VALUE_GE, Math.max(0, value));
	}

	int getMobFarmerLootUrgentDespawnTicks()
	{
		return Math.max(0, config.mobFarmerLootUrgentDespawnTicks());
	}

	void setMobFarmerLootUrgentDespawnTicks(int ticks)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_URGENT_DESPAWN_TICKS, Math.max(0, ticks));
	}

	int getMobFarmerLootCleanupPileCount()
	{
		return Math.max(0, config.mobFarmerLootCleanupPileCount());
	}

	void setMobFarmerLootCleanupPileCount(int count)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_CLEANUP_PILE_COUNT, Math.max(0, count));
	}

	int getMobFarmerLootRadius()
	{
		return Math.max(0, config.mobFarmerLootRadius());
	}

	void setMobFarmerLootRadius(int radius)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_RADIUS, Math.max(0, radius));
	}

	String getMobFarmerLootItems()
	{
		return config.mobFarmerLootItems() == null ? "" : config.mobFarmerLootItems();
	}

	void setMobFarmerLootItems(String items)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_ITEMS, items == null ? "" : items.trim());
	}

	String getMobFarmerLootBlacklist()
	{
		return config.mobFarmerLootBlacklist() == null ? "" : config.mobFarmerLootBlacklist();
	}

	void setMobFarmerLootBlacklist(String items)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_BLACKLIST, items == null ? "" : items.trim());
	}

	CvHelperLootOwnershipMode getMobFarmerLootOwnershipMode()
	{
		CvHelperLootOwnershipMode mode = config.mobFarmerLootOwnershipMode();
		return mode == null ? CvHelperLootOwnershipMode.OWN_OR_PUBLIC : mode;
	}

	void setMobFarmerLootOwnershipMode(CvHelperLootOwnershipMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_OWNERSHIP_MODE, mode == null ? CvHelperLootOwnershipMode.OWN_OR_PUBLIC : mode);
	}

	CvHelperMobInteractionMode getMobFarmerAttackInteractionMode()
	{
		CvHelperMobInteractionMode mode = config.mobFarmerAttackInteractionMode();
		return mode == null ? CvHelperMobInteractionMode.MENU_ACTION : mode;
	}

	void setMobFarmerAttackInteractionMode(CvHelperMobInteractionMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_ATTACK_INTERACTION_MODE, mode == null ? CvHelperMobInteractionMode.MENU_ACTION : mode);
	}

	CvHelperMobInteractionMode getMobFarmerLootInteractionMode()
	{
		CvHelperMobInteractionMode mode = config.mobFarmerLootInteractionMode();
		return mode == null ? CvHelperMobInteractionMode.MENU_ACTION : mode;
	}

	void setMobFarmerLootInteractionMode(CvHelperMobInteractionMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_LOOT_INTERACTION_MODE, mode == null ? CvHelperMobInteractionMode.MENU_ACTION : mode);
	}

	CvHelperGroundItemsMode getMobFarmerGroundItemsMode()
	{
		CvHelperGroundItemsMode mode = config.mobFarmerGroundItemsMode();
		return mode == null ? CvHelperGroundItemsMode.SUPPLEMENT : mode;
	}

	void setMobFarmerGroundItemsMode(CvHelperGroundItemsMode mode)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_GROUND_ITEMS_MODE, mode == null ? CvHelperGroundItemsMode.SUPPLEMENT : mode);
	}

	boolean getMobFarmerRespectGroundItemsHidden()
	{
		return config.mobFarmerRespectGroundItemsHidden();
	}

	void setMobFarmerRespectGroundItemsHidden(boolean respectHidden)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_RESPECT_GROUND_ITEMS_HIDDEN, respectHidden);
	}

	boolean getMobFarmerIntermediateActionsEnabled()
	{
		return config.mobFarmerIntermediateActionsEnabled();
	}

	void setMobFarmerIntermediateActionsEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_INTERMEDIATE_ACTIONS_ENABLED, enabled);
	}

	String getMobFarmerIntermediateItems()
	{
		return config.mobFarmerIntermediateItems() == null ? "" : config.mobFarmerIntermediateItems();
	}

	void setMobFarmerIntermediateItems(String items)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_INTERMEDIATE_ITEMS, items == null ? "" : items.trim());
	}

	String getMobFarmerIntermediateActionMappings()
	{
		return config.mobFarmerIntermediateActionMappings() == null ? "" : config.mobFarmerIntermediateActionMappings();
	}

	void setMobFarmerIntermediateActionMappings(String mappings)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_INTERMEDIATE_ACTION_MAPPINGS, mappings == null ? "" : mappings.trim());
	}

	String getMobFarmerNeverDropItems()
	{
		return config.mobFarmerNeverDropItems() == null ? "" : config.mobFarmerNeverDropItems();
	}

	void setMobFarmerNeverDropItems(String items)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_NEVER_DROP_ITEMS, items == null ? "" : items.trim());
	}

	String getMobFarmerDropItems()
	{
		return config.mobFarmerDropItems() == null ? "" : config.mobFarmerDropItems();
	}

	void setMobFarmerDropItems(String items)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_DROP_ITEMS, items == null ? "" : items.trim());
	}

	int getMobFarmerMaxDropValue()
	{
		return config.mobFarmerMaxDropValue();
	}

	void setMobFarmerMaxDropValue(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_MAX_DROP_VALUE, value);
	}

	boolean getMobFarmerHighAlchEnabled()
	{
		return config.mobFarmerHighAlchEnabled();
	}

	void setMobFarmerHighAlchEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_HIGH_ALCH_ENABLED, enabled);
	}

	int getMobFarmerHighAlchMinHa()
	{
		return Math.max(0, config.mobFarmerHighAlchMinHa());
	}

	void setMobFarmerHighAlchMinHa(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_HIGH_ALCH_MIN_HA, Math.max(0, value));
	}

	int getMobFarmerHighAlchMinDelta()
	{
		return config.mobFarmerHighAlchMinDelta();
	}

	void setMobFarmerHighAlchMinDelta(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_HIGH_ALCH_MIN_DELTA, value);
	}

	int getMobFarmerHighAlchMaxLoss()
	{
		return Math.max(0, config.mobFarmerHighAlchMaxLoss());
	}

	void setMobFarmerHighAlchMaxLoss(int value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_HIGH_ALCH_MAX_LOSS, Math.max(0, value));
	}

	String getMobFarmerHighAlchItems()
	{
		return config.mobFarmerHighAlchItems() == null ? "" : config.mobFarmerHighAlchItems();
	}

	void setMobFarmerHighAlchItems(String items)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_HIGH_ALCH_ITEMS, items == null ? "" : items.trim());
	}

	String getMobFarmerHighAlchBlacklist()
	{
		return config.mobFarmerHighAlchBlacklist() == null ? "" : config.mobFarmerHighAlchBlacklist();
	}

	void setMobFarmerHighAlchBlacklist(String items)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_HIGH_ALCH_BLACKLIST, items == null ? "" : items.trim());
	}

	String getMobFarmerDoorDenylist()
	{
		return config.mobFarmerDoorDenylist();
	}

	String getMobFarmerDoorAllowlist()
	{
		return config.mobFarmerDoorAllowlist();
	}

	void setMobFarmerDoorAllowlist(String value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_DOOR_ALLOWLIST, value);
	}

	void setMobFarmerDoorDenylist(String value)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_DOOR_DENYLIST, value);
	}

	boolean getMobFarmerDoorAutoOpenEnabled()
	{
		return config.mobFarmerDoorAutoOpenEnabled();
	}

	void setMobFarmerDoorAutoOpenEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_DOOR_AUTO_OPEN_ENABLED, enabled);
	}

	boolean getMobFarmerDoorAutoCloseEnabled()
	{
		return config.mobFarmerDoorAutoCloseEnabled();
	}

	void setMobFarmerDoorAutoCloseEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MOB_FARMER_DOOR_AUTO_CLOSE_ENABLED, enabled);
	}

	private String normalizedMobFarmerTarget(String target)
	{
		String cleaned = target == null ? "" : target.trim();
		return cleaned.isEmpty() ? "cow" : cleaned;
	}

	Map<String, Object> getMobFarmerStatus()
	{
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("target", mobFarmerTarget);
		status.put("targetCandidates", lastMobFarmerCandidates);
		status.put("running", mobFarmerRunning.get());
		status.put("live", mobFarmerLiveMode);
		status.put("status", mobFarmerStatus.get());
		status.put("loopDelayMs", getMobFarmerRecoveryLoopDelayMs());
		status.put("multiCombat", lastMobFarmerMultiCombat);
		status.put("afterLootCombatMode", getMobFarmerAfterLootCombatMode().name());
		status.put("engagedMode", getMobFarmerEngagedMode().name());
		status.put("aggroResponse", getMobFarmerAggroResponse().name());
		status.put("requireLineOfSight", getMobFarmerRequireLineOfSight());
		status.put("maxDistance", getMobFarmerMaxDistance());
		status.put("autorun", mobFarmerAutorunStatus());
		status.put("startupFocus", mobFarmerFocusClickStatus());
		status.put("autoEat", mobFarmerAutoEatConfigStatus());
		status.put("loginRecovery", mobFarmerLoginRecoveryStatus());
		status.put("loot", mobFarmerLootConfigStatus());
		status.put("decision", lastMobFarmerDecision);
		status.put("survivalDecision", lastMobFarmerSurvivalDecision);
		status.put("intermediateDecision", lastMobFarmerIntermediateDecision);
		status.put("lootDecision", lastMobFarmerLootDecision);
		status.put("lastActionAttempt", lastMobFarmerActionAttempt);
		status.put("scheduler", mobFarmerSchedulerStatus());
		status.put("deathLootTiming", lastMobFarmerDeathLootStatus);
		status.put("reattachAfterPickup", lastMobFarmerReattackStatus);
		status.put("doorTransition", lastMobFarmerDoorTransitionStatus);
		status.put("stabilization", lastMobFarmerStabilizationStatus);
		status.put("progress", lastMobFarmerProgressStatus);
		status.put("recentIntents", lastMobFarmerIntents);
		status.put("recentMenuEntries", lastMobFarmerMenuEntries);
		status.put("inventory", lastMobFarmerInventoryStatus);
		status.put("candidates", lastMobFarmerCandidates);
		status.put("lootCandidates", lastMobFarmerLootCandidates);
		status.put("pathing", safeValue(this::computeMobFarmerPathing, new LinkedHashMap<>()));
		return status;
	}

	/**
	 * Door-aware route + door states from the player to the currently-best
	 * selectable candidate, for the WebHelper reachability grid. Runs on the
	 * client thread (getMobFarmerStatus is invoked there).
	 */
	private Map<String, Object> computeMobFarmerPathing()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || localPlayer.getWorldLocation() == null)
		{
			return new LinkedHashMap<>();
		}
		Map<String, Object> best = null;
		int bestScore = Integer.MAX_VALUE;
		for (Map<String, Object> candidate : lastMobFarmerCandidates)
		{
			if (!Boolean.TRUE.equals(candidate.get("selectable")))
			{
				continue;
			}
			int score = intValue(candidate.get("score"), intValue(candidate.get("pathDistance"), intValue(candidate.get("distance"), Integer.MAX_VALUE)));
			if (best == null || score < bestScore)
			{
				best = candidate;
				bestScore = score;
			}
		}
		if (best == null || !(best.get("worldLocation") instanceof Map))
		{
			return new LinkedHashMap<>();
		}
		Map<?, ?> worldLocation = (Map<?, ?>) best.get("worldLocation");
		int x = intValue(worldLocation.get("x"), Integer.MIN_VALUE);
		int y = intValue(worldLocation.get("y"), Integer.MIN_VALUE);
		if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
		{
			return new LinkedHashMap<>();
		}
		int plane = intValue(worldLocation.get("plane"), localPlayer.getWorldLocation().getPlane());
		WorldPoint target = new WorldPoint(x, y, plane);
		return pathfinding.mobFarmerPathingDetail(localPlayer, target, getMobFarmerMaxDistance());
	}

	private Map<String, Object> mobFarmerAutorunStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>(lastMobFarmerAutorunStatus);
		out.put("enabled", getMobFarmerAutorunEnabled());
		out.put("minEnergyPercent", getMobFarmerAutorunMinEnergy());
		out.put("runEnergyPercent", safeValue(() -> client.getEnergy() / 100.0, -1.0));
		out.put("runEnabled", runEnabled());
		return out;
	}

	private Map<String, Object> mobFarmerFocusClickStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>(lastMobFarmerFocusClickStatus);
		out.put("enabled", getMobFarmerFocusClickAfterLogin());
		out.put("needed", mobFarmerFocusClickNeeded);
		return out;
	}

	private boolean runEnabled()
	{
		return safeValue(() -> client.getVarpValue(RUN_ENABLED_VARP) == 1, false);
	}

	private Map<String, Object> mobFarmerConfigPayload()
	{
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("version", 1);
		body.put("generatedAt", Instant.now().toString());
		body.put("settings", mobFarmerConfigSettings());
		body.put("schema", mobFarmerConfigSchema());
		body.put("actionSlots", actionSlotConfigStatus());
		return body;
	}

	private Map<String, Object> mobFarmerConfigSettings()
	{
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("target", getMobFarmerTarget());
		settings.put("recoveryLoopDelayMs", getMobFarmerRecoveryLoopDelayMs());
		settings.put("autorunEnabled", getMobFarmerAutorunEnabled());
		settings.put("autorunMinEnergy", getMobFarmerAutorunMinEnergy());
		settings.put("focusClickAfterLogin", getMobFarmerFocusClickAfterLogin());
		settings.put("afterLootCombatMode", getMobFarmerAfterLootCombatMode().name());
		settings.put("engagedMode", getMobFarmerEngagedMode().name());
		settings.put("aggroResponse", getMobFarmerAggroResponse().name());
		settings.put("requireLineOfSight", getMobFarmerRequireLineOfSight());
		settings.put("maxDistance", getMobFarmerMaxDistance());
		settings.put("autoEatEnabled", getMobFarmerAutoEatEnabled());
		settings.put("eatHitpointPercent", getMobFarmerEatHitpointPercent());
		settings.put("foodItems", getMobFarmerFoodItems());
		settings.put("stopIfNoFood", getMobFarmerStopIfNoFood());
		settings.put("survivalPreemptsActions", getMobFarmerSurvivalPreemptsActions());
		settings.put("loginRecoveryEnabled", getMobFarmerLoginRecoveryEnabled());
		settings.put("loginRecoveryF2pOnly", getMobFarmerLoginRecoveryF2pOnly());
		settings.put("loginClickToPlayEnabled", getMobFarmerLoginClickToPlayEnabled());
		settings.put("loginDisconnectRecoveryEnabled", getMobFarmerLoginDisconnectRecoveryEnabled());
		settings.put("doorAutoOpenEnabled", getMobFarmerDoorAutoOpenEnabled());
		settings.put("doorAutoCloseEnabled", getMobFarmerDoorAutoCloseEnabled());
		settings.put("doorAllowlist", getMobFarmerDoorAllowlist());
		settings.put("doorDenylist", getMobFarmerDoorDenylist());
		settings.put("autoResumeAfterLogin", getMobFarmerAutoResumeAfterLogin());
		settings.put("preferredLoginWorld", getMobFarmerPreferredLoginWorld());
		settings.put("lootEnabled", getMobFarmerLootEnabled());
		settings.put("lootDuringCombat", getMobFarmerLootDuringCombat());
		settings.put("attackBeforeLoot", getMobFarmerAttackBeforeLoot());
		settings.put("lootMinValueGe", getMobFarmerLootMinValueGe());
		settings.put("lootMinSingleGe", getMobFarmerLootMinSingleGe());
		settings.put("lootMinStackGe", getMobFarmerLootMinStackGe());
		settings.put("lootMinStackQuantity", getMobFarmerLootMinStackQuantity());
		settings.put("lootAlwaysStackGe", getMobFarmerLootAlwaysStackGe());
		settings.put("lootNeverStackBelowGe", getMobFarmerLootNeverStackBelowGe());
		settings.put("highPriorityLootValueGe", getMobFarmerHighPriorityLootValueGe());
		settings.put("lootUrgentDespawnTicks", getMobFarmerLootUrgentDespawnTicks());
		settings.put("lootCleanupPileCount", getMobFarmerLootCleanupPileCount());
		settings.put("lootRadius", getMobFarmerLootRadius());
		settings.put("lootItems", getMobFarmerLootItems());
		settings.put("lootBlacklist", getMobFarmerLootBlacklist());
		settings.put("lootOwnershipMode", getMobFarmerLootOwnershipMode().name());
		settings.put("attackInteractionMode", getMobFarmerAttackInteractionMode().name());
		settings.put("lootInteractionMode", getMobFarmerLootInteractionMode().name());
		settings.put("groundItemsMode", getMobFarmerGroundItemsMode().name());
		settings.put("respectGroundItemsHidden", getMobFarmerRespectGroundItemsHidden());
		settings.put("intermediateActionsEnabled", getMobFarmerIntermediateActionsEnabled());
		settings.put("intermediateItems", getMobFarmerIntermediateItems());
		settings.put("intermediateActionMappings", getMobFarmerIntermediateActionMappings());
		settings.put("neverDropItems", getMobFarmerNeverDropItems());
		settings.put("highAlchEnabled", getMobFarmerHighAlchEnabled());
		settings.put("highAlchMinHa", getMobFarmerHighAlchMinHa());
		settings.put("highAlchMinDelta", getMobFarmerHighAlchMinDelta());
		settings.put("highAlchMaxLoss", getMobFarmerHighAlchMaxLoss());
		settings.put("highAlchItems", getMobFarmerHighAlchItems());
		settings.put("highAlchBlacklist", getMobFarmerHighAlchBlacklist());
		settings.put("panicStopHotkey", keybindText(config.panicStopHotkey()));
		return settings;
	}

	private List<Map<String, Object>> mobFarmerConfigSchema()
	{
		List<Map<String, Object>> schema = new ArrayList<>();
		schema.add(settingSchema("target", "Mob targets", "text", "Partial NPC name, id:<npc id>, or a list separated by |, comma, semicolon, or newlines.", null));
		schema.add(settingSchema("recoveryLoopDelayMs", "Recovery delay ms", "number", "Wall-clock delay for logged-out recovery and manual loop sleeps. Logged-in farming is game-tick driven.", null));
		schema.add(settingSchema("autorunEnabled", "Auto-run on", "boolean", "Click the run orb when run is off and energy reaches the threshold. Never toggles run off.", null));
		schema.add(settingSchema("autorunMinEnergy", "Run energy %", "number", "Minimum run energy percent required before auto-run toggles on.", null));
		schema.add(settingSchema("focusClickAfterLogin", "Focus click after login", "boolean", "Require one guarded canvas-center click after login/start before farming actions.", null));
		schema.add(settingSchema("afterLootCombatMode", "After-loot combat", "enum", "After a pickup, hold or stop if something is already attacking/tagging you instead of targeting a new NPC.", enumOptions(CvHelperAfterLootCombatMode.values())));
		schema.add(settingSchema("engagedMode", "Engaged mobs", "enum", "Controls whether multi-combat may target mobs already being fought by someone else.", enumOptions(CvHelperMobEngagedMode.values())));
		schema.add(settingSchema("aggroResponse", "Undesired attacker", "enum", "What to do when an aggressive non-target mob is already attacking the player.", enumOptions(CvHelperMobAggroResponse.values())));
		schema.add(settingSchema("requireLineOfSight", "Require LOS", "boolean", "Skip mobs without RuneLite line of sight from the local player.", null));
		schema.add(settingSchema("maxDistance", "Max distance", "number", "Maximum path/target distance for auto-targeting. Use 0 to disable.", null));
		schema.add(settingSchema("autoEatEnabled", "Auto-eat", "boolean", "Eat configured food before combat/loot when HP is below threshold.", null));
		schema.add(settingSchema("eatHitpointPercent", "Eat below HP %", "number", "Auto-eat when current hitpoints are at or below this percent.", null));
		schema.add(settingSchema("foodItems", "Food items", "textarea", "Food item names or id:<item id>, separated by |, comma, semicolon, or newlines.", null));
		schema.add(settingSchema("stopIfNoFood", "Stop without food", "boolean", "Stop the farmer if HP is unsafe and no configured food is found.", null));
		schema.add(settingSchema("survivalPreemptsActions", "Survival wins", "boolean", "Let survival actions interrupt combat, loot, and utility helpers.", null));
		schema.add(settingSchema("loginRecoveryEnabled", "Login recovery", "boolean", "Allow the farmer to recover from login-screen/disconnect states while running live.", null));
		schema.add(settingSchema("loginRecoveryF2pOnly", "F2P recovery only", "boolean", "Skip automatic login recovery on members, PvP, seasonal, or special worlds.", null));
		schema.add(settingSchema("loginClickToPlayEnabled", "Click-to-play", "boolean", "Allow the guarded login-screen click/Enter helper.", null));
		schema.add(settingSchema("loginDisconnectRecoveryEnabled", "Disconnect recovery", "boolean", "Allow guarded Enter recovery from CONNECTION_LOST.", null));
		schema.add(settingSchema("doorAutoOpenEnabled", "Auto-open closed doors", "boolean", "Click Open on a closed, allowlisted and non-denylisted door blocking the path, then wait for native collision to confirm the transition is clear.", null));
		schema.add(settingSchema("doorAutoCloseEnabled", "Auto-close open doors", "boolean", "Click Close on an open, allowlisted and non-denylisted door/gate blocking the path (rare). Off by default.", null));
		schema.add(settingSchema("doorAllowlist", "Door allowlist (id or name)", "textarea", "Doors/gates the farmer may click, separated by |, comma, semicolon, or newlines. Empty means unknown/unallowlisted doors require manual action.", null));
		schema.add(settingSchema("doorDenylist", "Door denylist (id or name)", "textarea", "Doors/gates the farmer must never click or path through, separated by |, comma, semicolon, or newlines. Always reports blocked-by-door/manual-action-required regardless of the auto-open/auto-close flags.", null));
		schema.add(settingSchema("autoResumeAfterLogin", "Resume after login", "boolean", "Keep the farmer alive through login recovery so it resumes after login.", null));
		schema.add(settingSchema("preferredLoginWorld", "Preferred world", "number", "Preferred F2P world used in recovery diagnostics and safety checks.", null));
		schema.add(settingSchema("lootEnabled", "Loot pickup", "boolean", "Allow pickup of matching or valuable ground items.", null));
		schema.add(settingSchema("lootDuringCombat", "Loot in combat", "boolean", "Allow pickup attempts while already fighting.", null));
		schema.add(settingSchema("attackBeforeLoot", "Attack before loot", "boolean", "When idle and both target and loot exist, attack first unless priority loot overrides.", null));
		schema.add(settingSchema("lootMinValueGe", "Legacy min stack GE", "number", "Legacy total GE value guard for unlisted loot. Kept for compatibility; stack-aware rules below make the decision explicit.", null));
		schema.add(settingSchema("lootMinSingleGe", "Min each GE", "number", "Minimum GE value per individual item before unlisted loot is eligible. Use 0 to disable.", null));
		schema.add(settingSchema("lootMinStackGe", "Min stack GE", "number", "Minimum total stack GE value before unlisted loot is eligible. This skips tiny coin/rune/arrow stacks.", null));
		schema.add(settingSchema("lootMinStackQuantity", "Min stack qty", "number", "Minimum quantity for stackable unlisted items. Use 0 to disable.", null));
		schema.add(settingSchema("lootAlwaysStackGe", "Always stack GE", "number", "Treat stacks at or above this total GE value as high-priority loot. Use 0 to disable.", null));
		schema.add(settingSchema("lootNeverStackBelowGe", "Never below stack GE", "number", "Reject unlisted stacks below this total GE value even if broad value rules allow them. Explicit always-loot still wins.", null));
		schema.add(settingSchema("highPriorityLootValueGe", "Priority loot GE", "number", "Loot at or above this value can override Attack before loot.", null));
		schema.add(settingSchema("lootUrgentDespawnTicks", "Urgent despawn ticks", "number", "Loot at or below this despawn window can override Attack before loot. Use 0 to disable.", null));
		schema.add(settingSchema("lootCleanupPileCount", "Cleanup pile count", "number", "Visible selectable loot pile count that can trigger cleanup mode. Use 0 to disable.", null));
		schema.add(settingSchema("lootRadius", "Loot radius", "number", "Maximum tile distance for loot pickup. Use 0 to disable.", null));
		schema.add(settingSchema("lootItems", "Always loot", "textarea", "Items to loot even below value threshold.", null));
		schema.add(settingSchema("lootBlacklist", "Never loot", "textarea", "Items to never pick up.", null));
		schema.add(settingSchema("lootOwnershipMode", "Loot ownership", "enum", "Which ground-item ownership categories can be picked up.", enumOptions(CvHelperLootOwnershipMode.values())));
		schema.add(settingSchema("attackInteractionMode", "Attack click mode", "enum", "MENU_ACTION uses RuneLite menu entries; DIRECT_CLICK physically clicks canvas coordinates.", enumOptions(CvHelperMobInteractionMode.values())));
		schema.add(settingSchema("lootInteractionMode", "Loot click mode", "enum", "MENU_ACTION can take hidden/deprioritized entries without relying on left-click visibility.", enumOptions(CvHelperMobInteractionMode.values())));
		schema.add(settingSchema("groundItemsMode", "Ground Items", "enum", "OFF ignores Ground Items lists; SUPPLEMENT uses highlighted/hidden metadata in loot decisions.", enumOptions(CvHelperGroundItemsMode.values())));
		schema.add(settingSchema("respectGroundItemsHidden", "Respect hidden GI", "boolean", "Treat Ground Items hidden-list matches as never-loot unless CV Helper allowlists them.", null));
		schema.add(settingSchema("intermediateActionsEnabled", "Intermediate actions", "boolean", "Use configured inventory actions such as Bury/Scatter during safe loop windows.", null));
		schema.add(settingSchema("intermediateItems", "Intermediate items", "textarea", "Inventory items eligible for configured intermediate actions.", null));
		schema.add(settingSchema("intermediateActionMappings", "Intermediate map", "textarea", "Mappings like bones -> Bury; ashes -> Scatter|Bury. Drop is never allowed here.", null));
		schema.add(settingSchema("neverDropItems", "Protected items", "textarea", "Inventory items protected from future drop/replacement actions. Built-in safeguards always protect clue/rare unique items (for example clue scroll rewards, keys, totems, shards, champion scroll, long/curved bones).", null));
		schema.add(settingSchema("highAlchEnabled", "High Alch policy", "boolean", "Evaluate safe High Alchemy candidates while farming. Current pass reports safe candidates and availability instead of casting unsafely.", null));
		schema.add(settingSchema("highAlchMinHa", "Min HA value", "number", "Minimum single-item High Alchemy value for candidate reporting.", null));
		schema.add(settingSchema("highAlchMinDelta", "Min HA delta", "number", "Require HA value minus GE value to be at least this amount.", null));
		schema.add(settingSchema("highAlchMaxLoss", "Max HA loss", "number", "Maximum acceptable GE-to-HA loss per item when inventory space matters.", null));
		schema.add(settingSchema("highAlchItems", "Alch allowlist", "textarea", "If non-empty, only these item names or id:<item id> are eligible for High Alchemy.", null));
		schema.add(settingSchema("highAlchBlacklist", "Never alch", "textarea", "Items that must never be high-alched. Protected, food, teleport, rune, and useful items belong here.", null));
		return schema;
	}

	private Map<String, Object> settingSchema(String key, String label, String type, String description, List<String> options)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("key", key);
		out.put("label", label);
		out.put("type", type);
		out.put("description", description);
		if (options != null)
		{
			out.put("options", options);
		}
		return out;
	}

	private List<String> enumOptions(Enum<?>[] values)
	{
		List<String> options = new ArrayList<>();
		for (Enum<?> value : values)
		{
			options.add(value.name());
		}
		return options;
	}

	private List<Map<String, Object>> actionSlotConfigStatus()
	{
		List<Map<String, Object>> slots = new ArrayList<>();
		for (int slot = 1; slot <= getActionSlotCount(); slot++)
		{
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("slot", slot);
			out.put("enabled", getActionEnabled(slot));
			out.put("hotkey", keybindText(getActionHotkey(slot)));
			out.put("surface", getActionSurface(slot).name());
			out.put("target", getActionTarget(slot));
			out.put("clickAfterMode", getActionClickAfterMode(slot).name());
			out.put("invocationMode", getActionInvocationMode(slot).name());
			out.put("prayerMode", getActionPrayerMode(slot).name());
			out.put("spellAvailabilityMode", getActionSpellAvailabilityMode(slot).name());
			out.put("returnPanel", getActionReturnPanel(slot));
			out.put("returnMouseCenter", getActionReturnMouseCenter(slot));
			slots.add(out);
		}
		return slots;
	}

	private String keybindText(Keybind keybind)
	{
		return keybind == null || Keybind.NOT_SET.equals(keybind) ? "NOT_SET" : keybind.toString();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> applyMobFarmerConfigPayload(Map<String, Object> payload)
	{
		Map<String, Object> settings = mapValue(payload.get("settings"));
		if (settings.isEmpty())
		{
			settings = payload;
		}
		List<String> errors = new ArrayList<>();
		List<Runnable> updates = new ArrayList<>();
		applyStringSetting(settings, "target", updates, this::setMobFarmerTarget);
		applyIntSetting(settings, "recoveryLoopDelayMs", updates, this::setMobFarmerRecoveryLoopDelayMs, errors);
		applyBooleanSetting(settings, "autorunEnabled", updates, this::setMobFarmerAutorunEnabled, errors);
		applyIntSetting(settings, "autorunMinEnergy", updates, this::setMobFarmerAutorunMinEnergy, errors);
		applyBooleanSetting(settings, "focusClickAfterLogin", updates, this::setMobFarmerFocusClickAfterLogin, errors);
		applyEnumSetting(settings, "afterLootCombatMode", CvHelperAfterLootCombatMode.class, updates, this::setMobFarmerAfterLootCombatMode, errors);
		applyEnumSetting(settings, "engagedMode", CvHelperMobEngagedMode.class, updates, this::setMobFarmerEngagedMode, errors);
		applyEnumSetting(settings, "aggroResponse", CvHelperMobAggroResponse.class, updates, this::setMobFarmerAggroResponse, errors);
		applyBooleanSetting(settings, "requireLineOfSight", updates, this::setMobFarmerRequireLineOfSight, errors);
		applyIntSetting(settings, "maxDistance", updates, this::setMobFarmerMaxDistance, errors);
		applyBooleanSetting(settings, "autoEatEnabled", updates, this::setMobFarmerAutoEatEnabled, errors);
		applyIntSetting(settings, "eatHitpointPercent", updates, this::setMobFarmerEatHitpointPercent, errors);
		applyStringSetting(settings, "foodItems", updates, this::setMobFarmerFoodItems);
		applyBooleanSetting(settings, "stopIfNoFood", updates, this::setMobFarmerStopIfNoFood, errors);
		applyBooleanSetting(settings, "survivalPreemptsActions", updates, this::setMobFarmerSurvivalPreemptsActions, errors);
		applyBooleanSetting(settings, "loginRecoveryEnabled", updates, this::setMobFarmerLoginRecoveryEnabled, errors);
		applyBooleanSetting(settings, "loginRecoveryF2pOnly", updates, this::setMobFarmerLoginRecoveryF2pOnly, errors);
		applyBooleanSetting(settings, "loginClickToPlayEnabled", updates, this::setMobFarmerLoginClickToPlayEnabled, errors);
		applyBooleanSetting(settings, "loginDisconnectRecoveryEnabled", updates, this::setMobFarmerLoginDisconnectRecoveryEnabled, errors);
		applyBooleanSetting(settings, "doorAutoOpenEnabled", updates, this::setMobFarmerDoorAutoOpenEnabled, errors);
		applyBooleanSetting(settings, "doorAutoCloseEnabled", updates, this::setMobFarmerDoorAutoCloseEnabled, errors);
		applyStringSetting(settings, "doorAllowlist", updates, this::setMobFarmerDoorAllowlist);
		applyStringSetting(settings, "doorDenylist", updates, this::setMobFarmerDoorDenylist);
		applyBooleanSetting(settings, "autoResumeAfterLogin", updates, this::setMobFarmerAutoResumeAfterLogin, errors);
		applyIntSetting(settings, "preferredLoginWorld", updates, this::setMobFarmerPreferredLoginWorld, errors);
		applyBooleanSetting(settings, "lootEnabled", updates, this::setMobFarmerLootEnabled, errors);
		applyBooleanSetting(settings, "lootDuringCombat", updates, this::setMobFarmerLootDuringCombat, errors);
		applyBooleanSetting(settings, "attackBeforeLoot", updates, this::setMobFarmerAttackBeforeLoot, errors);
		applyIntSetting(settings, "lootMinValueGe", updates, this::setMobFarmerLootMinValueGe, errors);
		applyIntSetting(settings, "lootMinSingleGe", updates, this::setMobFarmerLootMinSingleGe, errors);
		applyIntSetting(settings, "lootMinStackGe", updates, this::setMobFarmerLootMinStackGe, errors);
		applyIntSetting(settings, "lootMinStackQuantity", updates, this::setMobFarmerLootMinStackQuantity, errors);
		applyIntSetting(settings, "lootAlwaysStackGe", updates, this::setMobFarmerLootAlwaysStackGe, errors);
		applyIntSetting(settings, "lootNeverStackBelowGe", updates, this::setMobFarmerLootNeverStackBelowGe, errors);
		applyIntSetting(settings, "highPriorityLootValueGe", updates, this::setMobFarmerHighPriorityLootValueGe, errors);
		applyIntSetting(settings, "lootUrgentDespawnTicks", updates, this::setMobFarmerLootUrgentDespawnTicks, errors);
		applyIntSetting(settings, "lootCleanupPileCount", updates, this::setMobFarmerLootCleanupPileCount, errors);
		applyIntSetting(settings, "lootRadius", updates, this::setMobFarmerLootRadius, errors);
		applyStringSetting(settings, "lootItems", updates, this::setMobFarmerLootItems);
		applyStringSetting(settings, "lootBlacklist", updates, this::setMobFarmerLootBlacklist);
		applyEnumSetting(settings, "lootOwnershipMode", CvHelperLootOwnershipMode.class, updates, this::setMobFarmerLootOwnershipMode, errors);
		applyEnumSetting(settings, "attackInteractionMode", CvHelperMobInteractionMode.class, updates, this::setMobFarmerAttackInteractionMode, errors);
		applyEnumSetting(settings, "lootInteractionMode", CvHelperMobInteractionMode.class, updates, this::setMobFarmerLootInteractionMode, errors);
		applyEnumSetting(settings, "groundItemsMode", CvHelperGroundItemsMode.class, updates, this::setMobFarmerGroundItemsMode, errors);
		applyBooleanSetting(settings, "respectGroundItemsHidden", updates, this::setMobFarmerRespectGroundItemsHidden, errors);
		applyBooleanSetting(settings, "intermediateActionsEnabled", updates, this::setMobFarmerIntermediateActionsEnabled, errors);
		applyStringSetting(settings, "intermediateItems", updates, this::setMobFarmerIntermediateItems);
		applyStringSetting(settings, "intermediateActionMappings", updates, this::setMobFarmerIntermediateActionMappings);
		applyStringSetting(settings, "neverDropItems", updates, this::setMobFarmerNeverDropItems);
		applyBooleanSetting(settings, "highAlchEnabled", updates, this::setMobFarmerHighAlchEnabled, errors);
		applyIntSetting(settings, "highAlchMinHa", updates, this::setMobFarmerHighAlchMinHa, errors);
		applyIntSetting(settings, "highAlchMinDelta", updates, this::setMobFarmerHighAlchMinDelta, errors);
		applyIntSetting(settings, "highAlchMaxLoss", updates, this::setMobFarmerHighAlchMaxLoss, errors);
		applyStringSetting(settings, "highAlchItems", updates, this::setMobFarmerHighAlchItems);
		applyStringSetting(settings, "highAlchBlacklist", updates, this::setMobFarmerHighAlchBlacklist);
		if (settings.containsKey("panicStopHotkey"))
		{
			Keybind hotkey = parseKeybind(String.valueOf(settings.get("panicStopHotkey")), errors, "panicStopHotkey");
			updates.add(() -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.PANIC_STOP_HOTKEY, hotkey));
		}

		Object actionSlots = payload.get("actionSlots");
		if (actionSlots instanceof List)
		{
			for (Object value : (List<?>) actionSlots)
			{
				Map<String, Object> slot = mapValue(value);
				int slotNumber = intSettingValue(slot.get("slot"), -1, "actionSlots.slot", errors);
				if (slotNumber < 1 || slotNumber > getActionSlotCount())
				{
					errors.add("actionSlots.slot must be between 1 and " + getActionSlotCount() + ": " + slotNumber);
					continue;
				}
				applyActionSlotConfig(slotNumber, slot, updates, errors);
			}
		}

		Map<String, Object> response = new LinkedHashMap<>();
		if (!errors.isEmpty())
		{
			response.put("ok", false);
			response.put("errors", errors);
			response.put("applied", false);
			return response;
		}
		for (Runnable update : updates)
		{
			update.run();
		}
		response.put("ok", true);
		response.put("applied", true);
		response.put("updatedSettings", updates.size());
		response.put("config", mobFarmerConfigPayload());
		return response;
	}

	private void applyActionSlotConfig(int slotNumber, Map<String, Object> slot, List<Runnable> updates, List<String> errors)
	{
		applyBooleanSetting(slot, "enabled", updates, value -> setActionEnabled(slotNumber, value), errors);
		applyStringSetting(slot, "target", updates, value -> setActionTarget(slotNumber, value));
		applyEnumSetting(slot, "surface", CvHelperActionSurface.class, updates, value -> setActionSurface(slotNumber, value), errors);
		applyEnumSetting(slot, "clickAfterMode", CvHelperClickAfterMode.class, updates, value -> setActionClickAfterMode(slotNumber, value), errors);
		applyEnumSetting(slot, "invocationMode", CvHelperActionInvocationMode.class, updates, value -> setActionInvocationMode(slotNumber, value), errors);
		applyEnumSetting(slot, "prayerMode", CvHelperPrayerActionMode.class, updates, value -> setActionPrayerMode(slotNumber, value), errors);
		applyEnumSetting(slot, "spellAvailabilityMode", CvHelperSpellAvailabilityMode.class, updates, value -> setActionSpellAvailabilityMode(slotNumber, value), errors);
		applyBooleanSetting(slot, "returnPanel", updates, value -> setActionReturnPanel(slotNumber, value), errors);
		applyBooleanSetting(slot, "returnMouseCenter", updates, value -> setActionReturnMouseCenter(slotNumber, value), errors);
		if (slot.containsKey("hotkey"))
		{
			Keybind hotkey = parseKeybind(String.valueOf(slot.get("hotkey")), errors, "actionSlots[" + slotNumber + "].hotkey");
			updates.add(() -> setActionHotkey(slotNumber, hotkey));
		}
	}

	private void applyStringSetting(Map<String, Object> settings, String key, List<Runnable> updates, java.util.function.Consumer<String> setter)
	{
		if (settings.containsKey(key))
		{
			String value = settings.get(key) == null ? "" : String.valueOf(settings.get(key));
			updates.add(() -> setter.accept(value));
		}
	}

	private void applyBooleanSetting(Map<String, Object> settings, String key, List<Runnable> updates, java.util.function.Consumer<Boolean> setter, List<String> errors)
	{
		if (settings.containsKey(key))
		{
			Boolean value = booleanSettingValue(settings.get(key), key, errors);
			updates.add(() -> setter.accept(Boolean.TRUE.equals(value)));
		}
	}

	private void applyIntSetting(Map<String, Object> settings, String key, List<Runnable> updates, java.util.function.IntConsumer setter, List<String> errors)
	{
		if (settings.containsKey(key))
		{
			int value = intSettingValue(settings.get(key), 0, key, errors);
			updates.add(() -> setter.accept(value));
		}
	}

	private <T extends Enum<T>> void applyEnumSetting(Map<String, Object> settings, String key, Class<T> enumClass, List<Runnable> updates, java.util.function.Consumer<T> setter, List<String> errors)
	{
		if (settings.containsKey(key))
		{
			T value = enumSettingValue(settings.get(key), enumClass, key, errors);
			if (value != null)
			{
				updates.add(() -> setter.accept(value));
			}
		}
	}

	private Boolean booleanSettingValue(Object value, String key, List<String> errors)
	{
		if (value instanceof Boolean)
		{
			return (Boolean) value;
		}
		if (value instanceof String)
		{
			String text = ((String) value).trim();
			if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text))
			{
				return Boolean.parseBoolean(text);
			}
		}
		errors.add(key + " must be true or false");
		return false;
	}

	private int intSettingValue(Object value, int fallback, String key, List<String> errors)
	{
		if (value instanceof Number)
		{
			return ((Number) value).intValue();
		}
		if (value instanceof String)
		{
			try
			{
				return Integer.parseInt(((String) value).trim());
			}
			catch (NumberFormatException e)
			{
				// Report below.
			}
		}
		errors.add(key + " must be a number");
		return fallback;
	}

	private <T extends Enum<T>> T enumSettingValue(Object value, Class<T> enumClass, String key, List<String> errors)
	{
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(value).trim().toUpperCase().replace('-', '_'));
		}
		catch (RuntimeException e)
		{
			errors.add(key + " must be one of " + enumOptions(enumClass.getEnumConstants()));
			return null;
		}
	}

	private Keybind parseKeybind(String raw, List<String> errors, String key)
	{
		String text = raw == null ? "" : raw.trim();
		if (text.isEmpty() || "NOT_SET".equalsIgnoreCase(text) || "Not set".equalsIgnoreCase(text))
		{
			return Keybind.NOT_SET;
		}
		int modifiers = 0;
		int keyCode = KeyEvent.VK_UNDEFINED;
		for (String token : text.split("\\+"))
		{
			String part = token.trim();
			if (part.isEmpty())
			{
				continue;
			}
			String normalized = part.toUpperCase().replace("CONTROL", "CTRL").replace("CMD", "META").replace("COMMAND", "META");
			if ("CTRL".equals(normalized))
			{
				modifiers |= InputEvent.CTRL_DOWN_MASK;
				continue;
			}
			if ("ALT".equals(normalized))
			{
				modifiers |= InputEvent.ALT_DOWN_MASK;
				continue;
			}
			if ("SHIFT".equals(normalized))
			{
				modifiers |= InputEvent.SHIFT_DOWN_MASK;
				continue;
			}
			if ("META".equals(normalized))
			{
				modifiers |= InputEvent.META_DOWN_MASK;
				continue;
			}
			keyCode = keyCodeForText(normalized);
		}
		if (keyCode == KeyEvent.VK_UNDEFINED && modifiers == 0)
		{
			errors.add(key + " could not parse hotkey '" + text + "'. Use examples like F12, CTRL+1, ALT+Q, or NOT_SET.");
			return Keybind.NOT_SET;
		}
		return new Keybind(keyCode, modifiers);
	}

	private int keyCodeForText(String text)
	{
		if (text.length() == 1)
		{
			return KeyEvent.getExtendedKeyCodeForChar(text.charAt(0));
		}
		try
		{
			return KeyEvent.class.getField("VK_" + text.replace(' ', '_')).getInt(null);
		}
		catch (ReflectiveOperationException e)
		{
			return KeyEvent.VK_UNDEFINED;
		}
	}

	private Map<String, Object> mobFarmerAutoEatConfigStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("enabled", getMobFarmerAutoEatEnabled());
		out.put("hitpointPercent", getMobFarmerEatHitpointPercent());
		out.put("foodItems", getMobFarmerFoodItems());
		out.put("stopIfNoFood", getMobFarmerStopIfNoFood());
		out.put("survivalPreemptsActions", getMobFarmerSurvivalPreemptsActions());
		return out;
	}

	private Map<String, Object> mobFarmerLoginRecoveryStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>();
		GameState gameState = safeValue(client::getGameState, GameState.UNKNOWN);
		int preferredWorld = getMobFarmerPreferredLoginWorld();
		int currentWorld = safeValue(() -> client.getWorld() > 0 ? client.getWorld() : -1, -1);
		boolean running = mobFarmerRunning.get();
		boolean live = mobFarmerLiveMode;
		String worldBlockReason = mobFarmerLoginWorldBlockReason();
		out.put("currentClientLoginState", gameState == null ? null : gameState.name());
		out.put("detectedScreen", detectedLoginScreen(gameState));
		out.put("macroRunning", running);
		out.put("macroLive", live);
		out.put("recoveryOnlyRunsWhileMacroRunning", true);
		out.put("recoveryWorkerActive", running && live && gameState != GameState.LOGGED_IN);
		out.put("pausedReason", mobFarmerLoginRecoveryPausedReason(gameState, running, live));
		out.put("willAttemptOnNextRecoveryTick", mobFarmerLoginRecoveryWillAttempt(gameState, running, live));
		out.put("enabled", getMobFarmerLoginRecoveryEnabled());
		out.put("loginRecoveryEnabled", getMobFarmerLoginRecoveryEnabled());
		out.put("clickToPlayEnabled", getMobFarmerLoginClickToPlayEnabled());
		out.put("disconnectRecoveryEnabled", getMobFarmerLoginDisconnectRecoveryEnabled());
		out.put("autoResumeAfterLogin", getMobFarmerAutoResumeAfterLogin());
		out.put("f2pWorldOnly", getMobFarmerLoginRecoveryF2pOnly());
		out.put("preferredWorld", preferredWorld);
		out.put("currentWorld", currentWorld);
		out.put("preferredWorldUsed", preferredWorld > 0 && currentWorld == preferredWorld);
		out.put("preferredWorldReady", preferredWorld <= 0 || currentWorld == preferredWorld);
		out.put("worldHost", safeValue(client::getWorldHost, null));
		out.put("worldType", currentWorldTypeText());
		out.put("currentWorldAllowed", worldBlockReason == null);
		out.put("worldBlockReason", worldBlockReason);
		out.put("cooldownMs", MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS);
		out.put("antiIdle", false);
		out.put("idleTimerGuidance", "Use RuneLite's Logout Timer plugin/settings to extend idle logout windows; CV Helper only recovers after a normal logout/login-screen state.");
		out.put("last", lastMobFarmerLoginRecovery);
		out.put("lastClickAttempt", lastLoginClickAttempt);
		return out;
	}

	private String mobFarmerLoginRecoveryPausedReason(GameState gameState, boolean running, boolean live)
	{
		if (gameState == GameState.LOGGED_IN)
		{
			return null;
		}
		if (!getMobFarmerLoginRecoveryEnabled())
		{
			return "login-recovery-disabled";
		}
		if (!running)
		{
			return "macro-stopped";
		}
		if (!live)
		{
			return "dry-run";
		}
		if (!getMobFarmerAutoResumeAfterLogin())
		{
			return "auto-resume-disabled";
		}
		if (gameState == GameState.LOGGING_IN || gameState == GameState.LOADING || gameState == GameState.HOPPING)
		{
			return "waiting:" + gameState;
		}
		if ((gameState == GameState.LOGIN_SCREEN || gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR) && !getMobFarmerLoginClickToPlayEnabled())
		{
			return "click-to-play-disabled";
		}
		if (gameState == GameState.CONNECTION_LOST && !getMobFarmerLoginDisconnectRecoveryEnabled())
		{
			return "disconnect-recovery-disabled";
		}
		boolean clickableState = gameState == GameState.LOGIN_SCREEN || gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR || gameState == GameState.CONNECTION_LOST;
		if (!clickableState)
		{
			return "not-clickable-login-state";
		}
		String blockReason = mobFarmerLoginWorldBlockReason();
		if (blockReason != null)
		{
			return blockReason;
		}
		long elapsed = System.currentTimeMillis() - lastMobFarmerLoginClickMillis;
		if (elapsed >= 0 && elapsed < MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS)
		{
			return "cooldown:" + (MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS - elapsed) + "ms";
		}
		return null;
	}

	private boolean mobFarmerLoginRecoveryWillAttempt(GameState gameState, boolean running, boolean live)
	{
		String reason = mobFarmerLoginRecoveryPausedReason(gameState, running, live);
		return reason == null && gameState != GameState.LOGGED_IN;
	}

	private Map<String, Object> mobFarmerLootConfigStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("enabled", getMobFarmerLootEnabled());
		out.put("duringCombat", getMobFarmerLootDuringCombat());
		out.put("attackBeforeLoot", getMobFarmerAttackBeforeLoot());
		out.put("minValueGe", getMobFarmerLootMinValueGe());
		out.put("minSingleGe", getMobFarmerLootMinSingleGe());
		out.put("minStackGe", getMobFarmerLootMinStackGe());
		out.put("minStackQuantity", getMobFarmerLootMinStackQuantity());
		out.put("alwaysStackGe", getMobFarmerLootAlwaysStackGe());
		out.put("neverStackBelowGe", getMobFarmerLootNeverStackBelowGe());
		out.put("highPriorityValueGe", getMobFarmerHighPriorityLootValueGe());
		out.put("urgentDespawnTicks", getMobFarmerLootUrgentDespawnTicks());
		out.put("cleanupPileCount", getMobFarmerLootCleanupPileCount());
		out.put("radius", getMobFarmerLootRadius());
		out.put("items", getMobFarmerLootItems());
		out.put("blacklist", getMobFarmerLootBlacklist());
		out.put("ownershipMode", getMobFarmerLootOwnershipMode().name());
		out.put("attackInteractionMode", getMobFarmerAttackInteractionMode().name());
		out.put("lootInteractionMode", getMobFarmerLootInteractionMode().name());
		out.put("groundItemsMode", getMobFarmerGroundItemsMode().name());
		out.put("respectGroundItemsHidden", getMobFarmerRespectGroundItemsHidden());
		out.put("groundItems", groundItemsConfigStatus());
		out.put("intermediateActionsEnabled", getMobFarmerIntermediateActionsEnabled());
		out.put("intermediateItems", getMobFarmerIntermediateItems());
		out.put("intermediateActionMappings", getMobFarmerIntermediateActionMappings());
		out.put("parsedIntermediateActionMappings", parsedIntermediateActionMappingsStatus());
		out.put("neverDropItems", getMobFarmerNeverDropItems());
		out.put("highAlch", highAlchPolicyStatus());
		out.put("temporaryLootSkips", new LinkedHashMap<>(mobFarmerLootSkipUntilTickByKey));
		return out;
	}

	private Map<String, Object> highAlchPolicyStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("enabled", getMobFarmerHighAlchEnabled());
		out.put("available", false);
		out.put("availabilityReason", "candidate-report-only");
		out.put("minHa", getMobFarmerHighAlchMinHa());
		out.put("minDelta", getMobFarmerHighAlchMinDelta());
		out.put("maxLoss", getMobFarmerHighAlchMaxLoss());
		out.put("allowlist", getMobFarmerHighAlchItems());
		out.put("blacklist", getMobFarmerHighAlchBlacklist());
		out.put("candidates", highAlchCandidates());
		return out;
	}

	private Map<String, Object> mobFarmerSchedulerStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>(lastMobFarmerSchedulerStatus);
		out.put("currentTick", safeValue(client::getTickCount, 0));
		out.put("lastStepTick", lastMobFarmerLoopStepTick);
		out.put("lastStepSource", lastMobFarmerLoopStepSource);
		out.put("tickDrivenWhenLoggedIn", true);
		out.put("recoveryLoopDelayMs", getMobFarmerRecoveryLoopDelayMs());
		out.put("actionTicks", new LinkedHashMap<>(lastMobFarmerActionTickByKey));
		out.put("kindMinimumTicks", mobFarmerKindMinimumTickStatus());
		return out;
	}

	private Map<String, Object> mobFarmerKindMinimumTickStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put(MobFarmerActionKind.SURVIVAL.name(), 1);
		out.put(MobFarmerActionKind.INVENTORY.name(), 1);
		out.put(MobFarmerActionKind.LOOT_PICKUP.name(), 1);
		out.put(MobFarmerActionKind.COMBAT.name(), 1);
		out.put(MobFarmerActionKind.MOVEMENT.name(), 1);
		out.put(MobFarmerActionKind.UI.name(), 0);
		out.put(MobFarmerActionKind.LOGIN_RECOVERY.name(), "wall-clock-cooldown:" + MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS + "ms");
		out.put(MobFarmerActionKind.CONFIG.name(), 0);
		return out;
	}

	private boolean mobFarmerActionAllowed(MobFarmerActionKind kind, String targetKey, int minTicks, String reason)
	{
		int tick = safeValue(client::getTickCount, 0);
		String key = mobFarmerSchedulerKey(kind, targetKey);
		Integer lastTick = lastMobFarmerActionTickByKey.get(key);
		boolean allowed = tick <= 0 || minTicks <= 0 || lastTick == null || tick - lastTick >= minTicks;
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("currentTick", tick);
		status.put("kind", kind.name());
		status.put("targetKey", targetKey);
		status.put("schedulerKey", key);
		status.put("minTicks", minTicks);
		status.put("lastIssuedTick", lastTick);
		status.put("allowed", allowed);
		status.put("reason", reason);
		if (!allowed)
		{
			status.put("waitTicks", minTicks - (tick - lastTick));
		}
		lastMobFarmerSchedulerStatus = status;
		return allowed;
	}

	private void recordMobFarmerScheduledAction(MobFarmerActionKind kind, String targetKey, String reason)
	{
		int tick = safeValue(client::getTickCount, 0);
		String key = mobFarmerSchedulerKey(kind, targetKey);
		lastMobFarmerActionTickByKey.put(key, tick);
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("currentTick", tick);
		status.put("kind", kind.name());
		status.put("targetKey", targetKey);
		status.put("schedulerKey", key);
		status.put("allowed", true);
		status.put("issued", true);
		status.put("reason", reason);
		lastMobFarmerSchedulerStatus = status;
	}

	private String mobFarmerSchedulerKey(MobFarmerActionKind kind, String targetKey)
	{
		return kind.name() + ":" + (targetKey == null ? "global" : targetKey);
	}

	private Integer lastMobFarmerActionTickForKind(MobFarmerActionKind kind)
	{
		String prefix = kind.name() + ":";
		Integer latest = null;
		for (Map.Entry<String, Integer> entry : lastMobFarmerActionTickByKey.entrySet())
		{
			if (entry.getKey() != null && entry.getKey().startsWith(prefix) && entry.getValue() != null && (latest == null || entry.getValue() > latest))
			{
				latest = entry.getValue();
			}
		}
		return latest;
	}

	private void recordMobFarmerStabilization(String decision, Map<String, Object> details)
	{
		Map<String, Object> status = new LinkedHashMap<>();
		int tick = safeValue(client::getTickCount, 0);
		status.put("decision", decision);
		status.put("at", Instant.now().toString());
		status.put("currentTick", tick);
		status.put("combatLeaseActive", mobFarmerCombatLeaseActive(tick));
		status.put("combatLeaseTargetKey", activeMobFarmerCombatKey);
		status.put("combatLeaseUntilTick", activeMobFarmerCombatUntilTick);
		status.put("combatLeaseTarget", activeMobFarmerCombatTarget);
		status.put("makeProgressActive", mobFarmerMakeProgressActive());
		status.put("makeProgressUntilTick", mobFarmerMakeProgressUntilTick);
		status.put("lootResolutionPending", mobFarmerReattackAfterPickupPending);
		status.put("pendingLootKey", pendingMobFarmerReattackLootKey);
		if (details != null)
		{
			status.put("details", details);
		}
		lastMobFarmerStabilizationStatus = status;
	}

	private void clearMobFarmerStabilization(String reason)
	{
		activeMobFarmerCombatKey = null;
		activeMobFarmerCombatUntilTick = -1;
		activeMobFarmerCombatTarget = new LinkedHashMap<>();
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("decision", "cleared");
		status.put("reason", reason);
		status.put("at", Instant.now().toString());
		status.put("currentTick", safeValue(client::getTickCount, 0));
		lastMobFarmerStabilizationStatus = status;
	}

	private void recordMobFarmerCombatLease(Map<String, Object> target, String reason)
	{
		int tick = safeValue(client::getTickCount, 0);
		activeMobFarmerCombatKey = mobFarmerTargetKey(target);
		activeMobFarmerCombatUntilTick = tick + MOB_FARMER_COMBAT_STABILIZE_TICKS;
		activeMobFarmerCombatTarget = target == null ? new LinkedHashMap<>() : new LinkedHashMap<>(target);
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("reason", reason);
		details.put("target", target == null ? null : targetLabelForMessage(target));
		details.put("targetKey", activeMobFarmerCombatKey);
		details.put("leaseTicks", MOB_FARMER_COMBAT_STABILIZE_TICKS);
		details.put("leaseUntilTick", activeMobFarmerCombatUntilTick);
		recordMobFarmerStabilization("combat-lease-started", details);
	}

	private boolean mobFarmerCombatLeaseActive(int tick)
	{
		return activeMobFarmerCombatKey != null && tick <= activeMobFarmerCombatUntilTick;
	}

	private boolean mobFarmerAttackResolutionHoldActive()
	{
		Integer lastAttackTick = lastMobFarmerActionTickForKind(MobFarmerActionKind.COMBAT);
		int tick = safeValue(client::getTickCount, 0);
		return lastAttackTick != null && tick - lastAttackTick >= 0 && tick - lastAttackTick < MOB_FARMER_ATTACK_REISSUE_MIN_TICKS;
	}

	private boolean holdMobFarmerAttackResolution(Player localPlayer)
	{
		if (!mobFarmerAttackResolutionHoldActive())
		{
			return false;
		}
		Actor interacting = localPlayer == null ? null : localPlayer.getInteracting();
		if (interacting != null && !isEffectivelyDead(interacting))
		{
			return false;
		}
		Integer lastAttackTick = lastMobFarmerActionTickForKind(MobFarmerActionKind.COMBAT);
		int tick = safeValue(client::getTickCount, 0);
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("lastAttackTick", lastAttackTick);
		details.put("ticksSinceLastAttack", lastAttackTick == null ? null : tick - lastAttackTick);
		details.put("minReissueTicks", MOB_FARMER_ATTACK_REISSUE_MIN_TICKS);
		details.put("activeCombatTargetKey", activeMobFarmerCombatKey);
		details.put("activeCombatTarget", activeMobFarmerCombatTarget);
		setMobFarmerDecision("waiting-for-attack-resolution", details);
		recordMobFarmerStabilization("holding-attack-reissue", details);
		mobFarmerStatus.set("waiting-attack-resolution");
		updatePanelStatus("Mob farmer waiting for attack command to resolve");
		return true;
	}

	private boolean mobFarmerShouldHoldCombat(Actor interacting)
	{
		if (!(interacting instanceof NPC) || isEffectivelyDead(interacting) || !matchesAnyMobTarget((NPC) interacting, mobFarmerTarget))
		{
			return false;
		}
		int tick = safeValue(client::getTickCount, 0);
		String interactingKey = mobFarmerTargetKey(actorSummary(interacting));
		boolean combatLease = mobFarmerCombatLeaseActive(tick) && (interactingKey == null || interactingKey.equals(activeMobFarmerCombatKey));
		boolean oscillationHold = activeMobFarmerLootKey != null && tick <= mobFarmerMakeProgressUntilTick;
		return combatLease || oscillationHold;
	}

	private boolean mobFarmerPriorityLootCanInterruptCombat(Map<String, Object> target)
	{
		if (target == null || !(target.get("mobFarmerPriorityReasons") instanceof List))
		{
			return false;
		}
		for (Object reasonValue : (List<?>) target.get("mobFarmerPriorityReasons"))
		{
			String reason = String.valueOf(reasonValue);
			if ("allowlist".equals(reason) || reason.startsWith("despawn:"))
			{
				return true;
			}
			if (reason.startsWith("value:") && getMobFarmerHighPriorityLootValueGe() > 0)
			{
				return true;
			}
		}
		return false;
	}

	private boolean tryMobFarmerPriorityLootInterrupt(Player localPlayer, Actor interacting, boolean live, int generation, String phase)
	{
		if (!getMobFarmerLootEnabled() || !getMobFarmerLootDuringCombat())
		{
			setMobFarmerLootDecision("combat-priority-loot-disabled:" + phase, null);
			return false;
		}

		MobFarmerLootSelection selection = selectMobFarmerLoot(localPlayer, true);
		lastMobFarmerLootCandidates = selection.reports;
		if (selection.target == null)
		{
			setMobFarmerLootDecision(selection.decision + ":" + phase, null);
			return false;
		}
		if (!mobFarmerPriorityLootCanInterruptCombat(selection.target))
		{
			Map<String, Object> details = new LinkedHashMap<>();
			details.put("phase", phase);
			details.put("currentTarget", actorSummary(interacting));
			details.put("deferredLoot", selection.target);
			details.put("priorityReasons", selection.target.get("mobFarmerPriorityReasons"));
			setMobFarmerLootDecision("priority-loot-deferred-by-combat-stabilizer:" + phase, selection.target);
			recordMobFarmerStabilization("deferred-priority-loot-during-combat", details);
			return false;
		}

		setMobFarmerLootDecision("selected-combat-interrupt-priority-loot:" + targetLabelForMessage(selection.target) + ":" + phase, selection.target);
		recordMobFarmerIntent("LOOT_ITEM", selection.target);
		return clickMobFarmerAutomationTarget("loot", selection.target, live, generation);
	}

	private void queueMobFarmerReattackAfterPickup(String lootLabel, Map<String, Object> lootTarget)
	{
		mobFarmerReattackAfterPickupPending = true;
		pendingMobFarmerReattackLootKey = mobFarmerTargetKey(lootTarget);
		pendingMobFarmerReattackLootTick = safeValue(client::getTickCount, 0);
		pendingMobFarmerReattackLootTarget = lootTarget == null ? new LinkedHashMap<>() : new LinkedHashMap<>(lootTarget);
		int distance = intValue(lootTarget == null ? null : lootTarget.get("distance"), 0);
		pendingMobFarmerReattackLootWaitTicks = Math.max(2, Math.min(MOB_FARMER_LOOT_RESOLUTION_MAX_TICKS, distance + 2));
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("pending", true);
		status.put("queuedAt", Instant.now().toString());
		status.put("queuedAtTick", pendingMobFarmerReattackLootTick);
		status.put("reason", "loot-pickup-invoked-waiting-for-resolution");
		status.put("lootTarget", lootLabel);
		status.put("lootTargetKey", pendingMobFarmerReattackLootKey);
		status.put("lootSnapshot", lootTarget);
		status.put("lootResolutionWaitTicks", pendingMobFarmerReattackLootWaitTicks);
		status.put("lastPickupTick", lastMobFarmerActionTickForKind(MobFarmerActionKind.LOOT_PICKUP));
		status.put("lastAttackTick", lastMobFarmerActionTickForKind(MobFarmerActionKind.COMBAT));
		status.put("estimatedAttackCooldownTicks", 1);
		lastMobFarmerReattackStatus = status;
		recordMobFarmerStabilization("waiting-for-loot-resolution", status);
	}

	private void clearMobFarmerReattackAfterPickup(String reason)
	{
		mobFarmerReattackAfterPickupPending = false;
		pendingMobFarmerReattackLootKey = null;
		pendingMobFarmerReattackLootTick = -1;
		pendingMobFarmerReattackLootWaitTicks = 0;
		pendingMobFarmerReattackLootTarget = new LinkedHashMap<>();
		Map<String, Object> status = new LinkedHashMap<>(lastMobFarmerReattackStatus);
		status.put("pending", false);
		status.put("clearedAt", Instant.now().toString());
		status.put("clearReason", reason);
		lastMobFarmerReattackStatus = status;
	}

	private void skipMobFarmerLootTemporarily(Map<String, Object> lootTarget, String reason)
	{
		String key = mobFarmerTargetKey(lootTarget);
		if (key == null)
		{
			return;
		}
		int tick = safeValue(client::getTickCount, 0);
		int untilTick = tick + MOB_FARMER_UNRESOLVED_LOOT_SKIP_TICKS;
		mobFarmerLootSkipUntilTickByKey.put(key, untilTick);
		pruneMobFarmerLootSkips(tick);
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("reason", reason);
		details.put("lootTarget", targetLabelForMessage(lootTarget));
		details.put("lootTargetKey", key);
		details.put("skipUntilTick", untilTick);
		details.put("skipTicks", MOB_FARMER_UNRESOLVED_LOOT_SKIP_TICKS);
		details.put("lootSnapshot", lootTarget);
		recordMobFarmerStabilization("temporary-loot-skip", details);
	}

	private String mobFarmerTemporaryLootSkipReason(Map<String, Object> lootTarget)
	{
		String key = mobFarmerTargetKey(lootTarget);
		if (key == null)
		{
			return null;
		}
		int tick = safeValue(client::getTickCount, 0);
		pruneMobFarmerLootSkips(tick);
		Integer untilTick = mobFarmerLootSkipUntilTickByKey.get(key);
		if (untilTick == null)
		{
			return null;
		}
		return "unresolved-loot-cooldown:" + Math.max(0, untilTick - tick) + "t";
	}

	private void pruneMobFarmerLootSkips(int tick)
	{
		List<String> expired = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : mobFarmerLootSkipUntilTickByKey.entrySet())
		{
			if (entry.getValue() == null || entry.getValue() < tick)
			{
				expired.add(entry.getKey());
			}
		}
		for (String key : expired)
		{
			mobFarmerLootSkipUntilTickByKey.remove(key);
		}
		while (mobFarmerLootSkipUntilTickByKey.size() > 32)
		{
			String firstKey = mobFarmerLootSkipUntilTickByKey.keySet().iterator().next();
			mobFarmerLootSkipUntilTickByKey.remove(firstKey);
		}
	}

	private boolean tryMobFarmerReattackAfterPickup(Player localPlayer, boolean live, int generation)
	{
		if (!mobFarmerReattackAfterPickupPending)
		{
			return false;
		}
		Map<String, Object> status = new LinkedHashMap<>(lastMobFarmerReattackStatus);
		int tick = safeValue(client::getTickCount, 0);
		status.put("pending", true);
		status.put("attemptedAt", Instant.now().toString());
		status.put("attemptedAtTick", tick);
		status.put("lastPickupTick", lastMobFarmerActionTickForKind(MobFarmerActionKind.LOOT_PICKUP));
		status.put("lastAttackTick", lastMobFarmerActionTickForKind(MobFarmerActionKind.COMBAT));
		status.put("estimatedAttackCooldownTicks", 1);
		status.put("lootTargetKey", pendingMobFarmerReattackLootKey);
		status.put("lootQueuedAtTick", pendingMobFarmerReattackLootTick);
		status.put("lootResolutionWaitTicks", pendingMobFarmerReattackLootWaitTicks);
		if (!live || isStaleMobFarmerLoop(generation))
		{
			status.put("result", live ? "stale-loop" : "dry-run");
			lastMobFarmerReattackStatus = status;
			clearMobFarmerReattackAfterPickup(String.valueOf(status.get("result")));
			return false;
		}
		if (pendingMobFarmerReattackLootTarget != null && !pendingMobFarmerReattackLootTarget.isEmpty())
		{
			Map<String, Object> freshLoot = freshGroundItemTarget(pendingMobFarmerReattackLootTarget);
			int ticksSinceLootCommand = pendingMobFarmerReattackLootTick < 0 ? 0 : tick - pendingMobFarmerReattackLootTick;
			status.put("ticksSinceLootCommand", ticksSinceLootCommand);
			status.put("pendingLootStillVisible", freshLoot != null);
			if (freshLoot != null && ticksSinceLootCommand <= pendingMobFarmerReattackLootWaitTicks)
			{
				status.put("result", "waiting-for-loot-resolution");
				status.put("freshLoot", freshLoot);
				lastMobFarmerReattackStatus = status;
				setMobFarmerDecision("waiting-for-loot-resolution", status);
				recordMobFarmerStabilization("holding-reattack-until-loot-resolves", status);
				mobFarmerStatus.set("waiting-loot-resolution:" + targetLabelForMessage(freshLoot));
				updatePanelStatus("Mob farmer waiting for loot pickup to resolve: " + targetLabelForMessage(freshLoot));
				return true;
			}
			if (freshLoot != null)
			{
				status.put("result", "loot-still-visible-after-wait");
				status.put("freshLoot", freshLoot);
				lastMobFarmerReattackStatus = status;
				skipMobFarmerLootTemporarily(freshLoot, "loot-still-visible-after-wait");
				clearMobFarmerReattackAfterPickup("loot-still-visible-after-wait");
				recordMobFarmerStabilization("loot-resolution-timeout", status);
				return false;
			}
			status.put("lootResolved", true);
		}
		Actor interacting = localPlayer.getInteracting();
		if (interacting != null && !isEffectivelyDead(interacting))
		{
			status.put("result", "already-in-combat");
			status.put("currentTarget", actorSummary(interacting));
			lastMobFarmerReattackStatus = status;
			clearMobFarmerReattackAfterPickup("already-in-combat");
			return false;
		}
		Map<String, Object> incomingAttacker = findNpcAttackingLocalPlayer(localPlayer);
		if (incomingAttacker != null && getMobFarmerAfterLootCombatMode() != CvHelperAfterLootCombatMode.RESUME_TARGETING)
		{
			status.put("result", "incoming-attacker-after-loot");
			status.put("mode", getMobFarmerAfterLootCombatMode().name());
			status.put("attacker", incomingAttacker);
			lastMobFarmerReattackStatus = status;
			clearMobFarmerReattackAfterPickup("incoming-attacker-after-loot");
			if (getMobFarmerAfterLootCombatMode() == CvHelperAfterLootCombatMode.STOP_WHEN_TAGGED)
			{
				setMobFarmerDecision("stopping-after-loot-tagged", status);
				stopMobFarmer();
			}
			else
			{
				setMobFarmerDecision("holding-after-loot-tagged", status);
				mobFarmerStatus.set("holding-after-loot-tagged:" + targetLabelForMessage(incomingAttacker));
				updatePanelStatus("Mob farmer holding after loot because attacker is already on player: " + targetLabelForMessage(incomingAttacker));
			}
			return true;
		}

		lastEntities = collectEntities();
		MobFarmerSelection selection = selectMobFarmerTarget(localPlayer);
		lastMobFarmerCandidates = selection.reports;
		lastMobFarmerMultiCombat = selection.multiCombat;
		status.put("selectionDecision", selection.decision);
		if (selection.target == null)
		{
			status.put("result", "no-valid-target");
			lastMobFarmerReattackStatus = status;
			clearMobFarmerReattackAfterPickup("no-valid-target");
			return false;
		}

		Map<String, Object> target = selection.target;
		if (tryHandleMobFarmerDoorTransition(localPlayer, target, true))
		{
			status.put("result", "door-transition");
			status.put("target", target);
			lastMobFarmerReattackStatus = status;
			return true;
		}
		Map<String, Object> clickPoint = firstPoint(target, "clickPoint", "center", "canvasTileCenter");
		if (getMobFarmerAttackInteractionMode() == CvHelperMobInteractionMode.DIRECT_CLICK && canvasPointToScreen(clickPoint) == null)
		{
			status.put("result", "target-off-canvas");
			status.put("target", target);
			lastMobFarmerReattackStatus = status;
			clearMobFarmerReattackAfterPickup("target-off-canvas");
			return false;
		}

		status.put("result", "attacking");
		status.put("target", target);
		status.put("attackReady", true);
		status.put("preemptedNormalFlow", true);
		lastMobFarmerReattackStatus = status;
		clearMobFarmerReattackAfterPickup("attack-issued");
		setMobFarmerDecision("reattack-after-pickup", target);
		invokeMobFarmerAttack(target, clickPoint, true, generation);
		return true;
	}

	void runMobFarmerStep(boolean live)
	{
		clientThread.invokeLater(() -> mobFarmerStep(live, 0, "manual-step"));
	}

	void startMobFarmer(boolean live)
	{
		if (!mobFarmerRunning.compareAndSet(false, true))
		{
			updatePanelStatus("Mob farmer already running");
			return;
		}
		mobFarmerLiveMode = live;
		clearMobFarmerReattackAfterPickup("start");
		clearMobFarmerStabilization("start");
		mobFarmerFocusClickNeeded = live && getMobFarmerFocusClickAfterLogin();
		recordMobFarmerFocusClick(mobFarmerFocusClickNeeded ? "needed-after-start" : "not-needed-after-start", "start", false, null);
		mobFarmerLootSkipUntilTickByKey.clear();
		int generation = mobFarmerGeneration.incrementAndGet();
		mobFarmerStatus.set((live ? "live" : "dry") + "-loop-started");
		Thread loopThread = new Thread(() ->
		{
			try
			{
				while (isCurrentMobFarmerLoop(generation))
				{
					clientThread.invokeLater(() ->
					{
						if (isCurrentMobFarmerLoop(generation) && client.getGameState() != GameState.LOGGED_IN)
						{
							mobFarmerStep(live, generation, "recovery-loop");
						}
					});
					Thread.sleep(getMobFarmerRecoveryLoopDelayMs());
				}
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			finally
			{
				if (Thread.currentThread() == mobFarmerThread)
				{
					mobFarmerThread = null;
				}
				if (mobFarmerGeneration.get() == generation)
				{
					mobFarmerRunning.set(false);
					mobFarmerLiveMode = false;
					mobFarmerStatus.set("stopped");
				}
			}
		}, "cv-helper-mob-farmer");
		loopThread.setDaemon(true);
		mobFarmerThread = loopThread;
		loopThread.start();
		updatePanelStatus("Mob farmer " + (live ? "live" : "dry-run") + " loop started");
		updateAntiIdleState();
	}

	void stopMobFarmer()
	{
		mobFarmerGeneration.incrementAndGet();
		mobFarmerRunning.set(false);
		mobFarmerLiveMode = false;
		clearMobFarmerReattackAfterPickup("stop");
		clearMobFarmerStabilization("stop");
		mobFarmerFocusClickNeeded = false;
		mobFarmerLootSkipUntilTickByKey.clear();
		interruptMobFarmerThread();
		mobFarmerStatus.set("stopped");
		updatePanelStatus("Mob farmer stopped");
		updateAntiIdleState();
	}

	void panicStop()
	{
		mobFarmerGeneration.incrementAndGet();
		mobFarmerRunning.set(false);
		mobFarmerLiveMode = false;
		miningFarmerRunning.set(false);
		miningFarmerLiveMode = false;
		woodcuttingFarmerRunning.set(false);
		woodcuttingFarmerLiveMode = false;
		clearMobFarmerReattackAfterPickup("panic-stop");
		clearMobFarmerStabilization("panic-stop");
		mobFarmerFocusClickNeeded = false;
		mobFarmerLootSkipUntilTickByKey.clear();
		interruptMobFarmerThread();
		actionInProgress.set(false);
		mobFarmerStatus.set("panic-stopped");
		lastEvent.set("panic-stop@" + Instant.now());
		String message = "CV Helper panic stop: all loops stopped and action guard cleared";
		updatePanelStatus(message);
		clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, ""));
		updateAntiIdleState();
	}

	private boolean isCurrentMobFarmerLoop(int generation)
	{
		return mobFarmerRunning.get() && mobFarmerGeneration.get() == generation;
	}

	private boolean isStaleMobFarmerLoop(int generation)
	{
		return generation > 0 && !isCurrentMobFarmerLoop(generation);
	}

	private void interruptMobFarmerThread()
	{
		Thread thread = mobFarmerThread;
		if (thread != null)
		{
			thread.interrupt();
		}
	}

	private boolean tryMobFarmerLoginRecovery(boolean live, int generation, GameState gameState)
	{
		Map<String, Object> details = new LinkedHashMap<>();
		LoginRecoveryState state = detectLoginRecoveryState();
		
		details.put("enabled", getMobFarmerLoginRecoveryEnabled());
		details.put("live", live);
		details.put("gameState", gameState == null ? null : gameState.name());
		details.put("recoveryState", state.name());
		details.put("clickToPlayEnabled", getMobFarmerLoginClickToPlayEnabled());
		details.put("disconnectRecoveryEnabled", getMobFarmerLoginDisconnectRecoveryEnabled());
		details.put("autoResumeAfterLogin", getMobFarmerAutoResumeAfterLogin());
		details.put("f2pWorldOnly", getMobFarmerLoginRecoveryF2pOnly());
		details.put("preferredWorld", getMobFarmerPreferredLoginWorld());
		details.put("currentWorld", safeValue(() -> client.getWorld() > 0 ? client.getWorld() : -1, -1));
		details.put("worldHost", safeValue(client::getWorldHost, null));
		details.put("worldType", currentWorldTypeText());
		details.put("worldSuitable", isWorldSuitableForAutomation());
		details.put("worldBlockReason", mobFarmerLoginWorldBlockReason());
		details.put("selectedFallbackWorld", isWorldSuitableForAutomation() ? null : selectFallbackWorld(getMobFarmerLoginRecoveryF2pOnly()));

		if (!live || !getMobFarmerLoginRecoveryEnabled())
		{
			details.put("result", live ? "disabled" : "dry-run-skip");
			setMobFarmerLoginRecoveryDecision(live ? "disabled" : "dry-run-skip", details);
			return false;
		}
		
		if (state == LoginRecoveryState.IN_GAME)
		{
			details.put("result", "already-logged-in");
			setMobFarmerLoginRecoveryDecision("already-logged-in", details);
			return false;
		}
		
		if (state == LoginRecoveryState.LOADING)
		{
			details.put("result", "waiting");
			setMobFarmerLoginRecoveryDecision("waiting:" + gameState, details);
			mobFarmerStatus.set("login-recovery-waiting:" + gameState);
			setMobFarmerDecision("login-recovery-waiting", details);
			return true;
		}
		
		if (!getMobFarmerAutoResumeAfterLogin())
		{
			details.put("result", "auto-resume-disabled");
			setMobFarmerLoginRecoveryDecision("auto-resume-disabled", details);
			mobFarmerStatus.set("login-recovery-disabled:auto-resume");
			setMobFarmerDecision("login-auto-resume-disabled", details);
			updatePanelStatus("Mob farmer stopped at login/disconnect because auto-resume is disabled");
			stopMobFarmer();
			return true;
		}
		
		if (state == LoginRecoveryState.WORLD_SWITCH_REQUIRED)
		{
			details.put("result", "world-switch-required");
			setMobFarmerLoginRecoveryDecision("world-switch-required", details);
			mobFarmerStatus.set("login-recovery-blocked:world-switch-required");
			setMobFarmerDecision("login-recovery-world-switch-required", details);
			updatePanelStatus("Mob farmer login recovery blocked: world switch required to " + details.get("selectedFallbackWorld"));
			return true;
		}
		
		if (state == LoginRecoveryState.LOGIN_SCREEN || state == LoginRecoveryState.AUTH_REQUIRED_MANUAL)
		{
			if (!getMobFarmerLoginClickToPlayEnabled())
			{
				details.put("result", "click-to-play-disabled");
				setMobFarmerLoginRecoveryDecision("click-to-play-disabled", details);
				mobFarmerStatus.set("login-recovery-disabled:click-to-play");
				setMobFarmerDecision("login-recovery-click-to-play-disabled", details);
				return true;
			}
		}
		
		if (state == LoginRecoveryState.DISCONNECTED)
		{
			if (!getMobFarmerLoginDisconnectRecoveryEnabled())
			{
				details.put("result", "disconnect-recovery-disabled");
				setMobFarmerLoginRecoveryDecision("disconnect-recovery-disabled", details);
				mobFarmerStatus.set("login-recovery-disabled:connection-lost");
				setMobFarmerDecision("login-recovery-disconnect-disabled", details);
				return true;
			}
		}
		
		if (isStaleMobFarmerLoop(generation))
		{
			details.put("result", "stale-loop");
			setMobFarmerLoginRecoveryDecision("stale-loop", details);
			return true;
		}

		long now = System.currentTimeMillis();
		long elapsed = now - lastMobFarmerLoginClickMillis;
		if (elapsed >= 0 && elapsed < MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS)
		{
			long remaining = MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS - elapsed;
			details.put("remainingCooldownMs", remaining);
			details.put("result", "cooldown");
			setMobFarmerLoginRecoveryDecision("cooldown", details);
			mobFarmerStatus.set("login-recovery-cooldown:" + remaining + "ms");
			setMobFarmerDecision("login-recovery-cooldown", details);
			return true;
		}

		lastMobFarmerLoginClickMillis = now;
		boolean isDisconnect = state == LoginRecoveryState.DISCONNECTED;
		details.put("intendedAction", isDisconnect ? "Recover connection lost" : "Click login");
		details.put("actualAction", isDisconnect ? "enter-key-disconnect-recovery" : "guarded-login-widget-click");
		details.put("result", "queued");
		recordMobFarmerScheduledAction(MobFarmerActionKind.LOGIN_RECOVERY, state.name(), details.get("actualAction").toString());
		setMobFarmerLoginRecoveryDecision("queued", details);
		mobFarmerStatus.set(isDisconnect ? "login-disconnect-recovery-queued" : "login-recovery-click-queued");
		setMobFarmerDecision(isDisconnect ? "login-disconnect-recovery-queued" : "login-recovery-click-queued", details);
		updatePanelStatus(isDisconnect ? "Mob farmer queued connection-lost recovery" : "Mob farmer queued guarded login recovery click");
		if (isDisconnect)
		{
			pressLoginEnterFallback("login-disconnect-enter-fallback", "Pressed Enter on connection-lost screen");
		}
		else
		{
			clickLoginScreen();
		}
		return true;
	}

	/**
	 * Skill-agnostic "enable and wait" login-recovery service for Mining/Woodcutting
	 * (or any future farmer): reuses the SAME detectLoginRecoveryState()/clickLoginScreen()/
	 * switchToWorld() machinery tryMobFarmerLoginRecovery already drives, and the SAME
	 * cooldown timer, since there is one client login state shared by every farmer, not
	 * one per farmer. Gated by the existing mob-farmer login-recovery config -- it's a
	 * shared client-level toggle in spirit even though the keys are mob-farmer-prefixed
	 * for historical reasons, so enabling it for Mob Farmer also covers skill farmers.
	 * Returns true while recovery is actively in progress (so the caller can report
	 * "waiting on login recovery" instead of a bare "needs-login" failure).
	 */
	private boolean tryGenericLoginRecovery()
	{
		if (!getMobFarmerLoginRecoveryEnabled() || !getMobFarmerAutoResumeAfterLogin())
		{
			return false;
		}
		LoginRecoveryState state = detectLoginRecoveryState();
		if (state == LoginRecoveryState.IN_GAME)
		{
			return false;
		}
		if (state == LoginRecoveryState.LOADING)
		{
			return true;
		}
		if (state == LoginRecoveryState.WORLD_SWITCH_REQUIRED)
		{
			switchToWorld(selectFallbackWorld(getMobFarmerLoginRecoveryF2pOnly()));
			return true;
		}
		if ((state == LoginRecoveryState.LOGIN_SCREEN || state == LoginRecoveryState.AUTH_REQUIRED_MANUAL)
			&& !getMobFarmerLoginClickToPlayEnabled())
		{
			return false;
		}
		if (state == LoginRecoveryState.DISCONNECTED && !getMobFarmerLoginDisconnectRecoveryEnabled())
		{
			return false;
		}
		long now = System.currentTimeMillis();
		long elapsed = now - lastMobFarmerLoginClickMillis;
		if (elapsed >= 0 && elapsed < MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS)
		{
			return true;
		}
		lastMobFarmerLoginClickMillis = now;
		if (state == LoginRecoveryState.DISCONNECTED)
		{
			pressLoginEnterFallback("login-disconnect-enter-fallback", "Skill farmer queued connection-lost recovery");
		}
		else
		{
			clickLoginScreen();
		}
		return true;
	}

	private void setMobFarmerLoginRecoveryDecision(String decision, Map<String, Object> details)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("decision", decision);
		payload.put("at", Instant.now().toString());
		if (details != null && !details.isEmpty())
		{
			payload.put("details", new LinkedHashMap<>(details));
		}
		lastMobFarmerLoginRecovery = payload;
	}

	private boolean mobFarmerLoginWorldAllowed()
	{
		return mobFarmerLoginWorldBlockReason() == null;
	}

	private String mobFarmerLoginWorldBlockReason()
	{
		if (!getMobFarmerLoginRecoveryF2pOnly())
		{
			return null;
		}
		int world = safeValue(() -> client.getWorld() > 0 ? client.getWorld() : -1, -1);
		if (world <= 0)
		{
			return "unknown-world";
		}
		// The configured preferred/default world (326 by default -- an LMS world) is an
		// explicit exception to the world-type blocklist below. Without this, recovery
		// would prefer 326 as its target world while simultaneously blocking it for being
		// an LMS world type, which made it bounce to a fallback world instead of the one
		// it was actually configured to use. The user has confirmed 326 is intentional and
		// safe for this project; only the *configured* preferred world gets the exception,
		// not LMS worlds in general.
		int preferredWorld = getMobFarmerPreferredLoginWorld();
		if (preferredWorld > 0 && world == preferredWorld)
		{
			return null;
		}

		String worldType = currentWorldTypeText().toLowerCase();
		for (String blocked : Arrays.asList("members", "pvp", "deadman", "seasonal", "beta", "last_man_standing", "bounty", "high_risk", "skill_total", "quest_speedrunning", "fresh_start", "tournament"))
		{
			if (worldType.contains(blocked))
			{
				return "unsafe-world-type:" + blocked + "@" + world;
			}
		}
		return null;
	}

	private LoginRecoveryState detectLoginRecoveryState()
	{
		GameState gameState = safeValue(client::getGameState, GameState.UNKNOWN);
		if (gameState == null)
		{
			return LoginRecoveryState.UNKNOWN_LOGIN_STATE;
		}

		switch (gameState)
		{
			case LOGGED_IN:
				return LoginRecoveryState.IN_GAME;
			case LOGIN_SCREEN:
			case LOGIN_SCREEN_AUTHENTICATOR:
				String worldBlockReason = mobFarmerLoginWorldBlockReason();
				if (worldBlockReason != null)
				{
					return LoginRecoveryState.WORLD_SWITCH_REQUIRED;
				}
				Widget loginWidget = findLoginClickWidget();
				if (isVisibleWidget(loginWidget))
				{
					return LoginRecoveryState.CLICK_TO_PLAY;
				}
				return LoginRecoveryState.LOGIN_SCREEN;
			case CONNECTION_LOST:
				return LoginRecoveryState.DISCONNECTED;
			case LOGGING_IN:
			case LOADING:
			case HOPPING:
				return LoginRecoveryState.LOADING;
			case STARTING:
				return LoginRecoveryState.LOADING;
			default:
				return LoginRecoveryState.UNKNOWN_LOGIN_STATE;
		}
	}

	private boolean isWorldSuitableForAutomation()
	{
		return mobFarmerLoginWorldBlockReason() == null;
	}

	private int selectFallbackWorld(boolean preferF2P)
	{
		int currentWorld = safeValue(() -> client.getWorld() > 0 ? client.getWorld() : -1, -1);
		int[] fallbacks = preferF2P ? FALLBACK_F2P_WORLDS : FALLBACK_P2P_WORLDS;
		
		for (int world : fallbacks)
		{
			if (world != currentWorld)
			{
				return world;
			}
		}
		return preferF2P ? DEFAULT_F2P_WORLD : DEFAULT_P2P_WORLD;
	}

	private Map<String, Object> getLoginRecoveryDiagnostics()
	{
		Map<String, Object> diagnostics = new LinkedHashMap<>();
		GameState gameState = safeValue(client::getGameState, GameState.UNKNOWN);
		LoginRecoveryState state = detectLoginRecoveryState();
		int currentWorld = safeValue(() -> client.getWorld() > 0 ? client.getWorld() : -1, -1);
		String worldType = currentWorldTypeText();
		boolean worldSuitable = isWorldSuitableForAutomation();
		String worldBlockReason = mobFarmerLoginWorldBlockReason();
		
		diagnostics.put("state", state.name());
		diagnostics.put("gameState", gameState == null ? null : gameState.name());
		diagnostics.put("currentWorld", currentWorld);
		diagnostics.put("worldType", worldType);
		diagnostics.put("worldSuitable", worldSuitable);
		diagnostics.put("worldBlockReason", worldBlockReason);
		diagnostics.put("loggedIn", gameState == GameState.LOGGED_IN);
		diagnostics.put("selectedFallbackWorld", worldSuitable ? null : selectFallbackWorld(getMobFarmerLoginRecoveryF2pOnly()));

		int preferredWorld = getMobFarmerPreferredLoginWorld();
		diagnostics.put("preferredWorld", preferredWorld);
		diagnostics.put("preferredWorldIsCurrent", preferredWorld > 0 && preferredWorld == currentWorld);
		diagnostics.put("lastLoginAction", lastWorldSwitchAction);
		diagnostics.put("lastLoginFailureReason", lastWorldSwitchFailureReason);
		diagnostics.put("recoveryBlocked", worldBlockReason != null && state != LoginRecoveryState.IN_GAME);
		diagnostics.put("manualActionRequired", state == LoginRecoveryState.LOGIN_SCREEN || state == LoginRecoveryState.UNKNOWN_LOGIN_STATE);
		diagnostics.put("worldListAvailable", lastWorldListAvailable);
		diagnostics.put("validWorldResolved", lastWorldResolvedValid);

		Widget loginWidget = findLoginClickWidget();
		diagnostics.put("loginWidgetVisible", isVisibleWidget(loginWidget));
		if (isVisibleWidget(loginWidget))
		{
			diagnostics.put("loginWidget", loginWidgetDiagnostics(loginWidget));
		}

		return diagnostics;
	}

	String getLoginRecoveryStatusText()
	{
		Map<String, Object> diagnostics = getLoginRecoveryDiagnostics();
		LoginRecoveryState state = (LoginRecoveryState) diagnostics.get("state");
		int currentWorld = (int) diagnostics.get("currentWorld");
		String worldType = (String) diagnostics.get("worldType");
		boolean worldSuitable = (boolean) diagnostics.get("worldSuitable");
		String worldBlockReason = (String) diagnostics.get("worldBlockReason");
		boolean loggedIn = (boolean) diagnostics.get("loggedIn");
		
		StringBuilder sb = new StringBuilder();
		sb.append("State: ").append(state.name());
		if (loggedIn)
		{
			sb.append(" (logged in)");
		}
		sb.append(" | World: ").append(currentWorld > 0 ? currentWorld : "unknown");
		sb.append(" | Type: ").append(worldType != null ? worldType : "unknown");
		sb.append(" | Suitable: ").append(worldSuitable ? "yes" : "no");
		if (!worldSuitable && worldBlockReason != null)
		{
			sb.append(" (").append(worldBlockReason).append(")");
		}
		return sb.toString();
	}

	/**
	 * Resolves the real world-list entry for {@code worldId} from RuneLite's own world
	 * service (the same one the official world-hopper/login flow uses), so the World
	 * object handed to {@code client.changeWorld()} has a real address/types/location --
	 * not just an id with everything else left blank. A hand-built World with no address
	 * has nothing for the login flow to actually connect to, which was the prior failure
	 * mode here. Runs on the calling (non-client) thread since it's a blocking HTTP call;
	 * callers must not invoke this from the client thread.
	 */
	private net.runelite.http.api.worlds.World lookupRealWorld(int worldId)
	{
		try
		{
			net.runelite.http.api.worlds.WorldResult result = new net.runelite.client.game.WorldClient(
				RuneLiteAPI.CLIENT, okhttp3.HttpUrl.get(net.runelite.client.RuneLiteProperties.getApiBase())
			).lookupWorlds();
			if (result == null || result.getWorlds() == null)
			{
				lastWorldListAvailable = false;
				return null;
			}
			lastWorldListAvailable = true;
			for (net.runelite.http.api.worlds.World candidate : result.getWorlds())
			{
				if (candidate.getId() == worldId)
				{
					return candidate;
				}
			}
			return null;
		}
		catch (IOException e)
		{
			log.warn("Unable to look up real world-list entry for world {}", worldId, e);
			lastWorldListAvailable = false;
			return null;
		}
	}

	private void switchToWorld(int worldId)
	{
		// Blocking HTTP call -- must happen before the clientThread.invokeLater below,
		// not inside it, so the client thread is never blocked on network I/O.
		lastWorldSwitchAction = "switch-world:" + worldId;
		net.runelite.http.api.worlds.World realWorld = lookupRealWorld(worldId);
		lastWorldResolvedValid = realWorld != null;
		clientThread.invokeLater(() ->
		{
			log.info("Attempting to switch to world {} (real world-list entry {})", worldId, realWorld != null ? "found" : "NOT found, using fallback fields");
			lastEvent.set("world-switch-attempt:" + worldId);

			World rsWorld = client.createWorld();
			rsWorld.setId(worldId);
			if (realWorld != null)
			{
				rsWorld.setActivity(realWorld.getActivity() == null ? "" : realWorld.getActivity());
				rsWorld.setAddress(realWorld.getAddress());
				rsWorld.setLocation(realWorld.getLocation());
				rsWorld.setPlayerCount(realWorld.getPlayers());
				EnumSet<WorldType> types = EnumSet.noneOf(WorldType.class);
				if (realWorld.getTypes() != null)
				{
					for (net.runelite.http.api.worlds.WorldType httpType : realWorld.getTypes())
					{
						try
						{
							types.add(WorldType.valueOf(httpType.name()));
						}
						catch (IllegalArgumentException ignored)
						{
							// http-api world type with no matching client-api WorldType; skip it
						}
					}
				}
				rsWorld.setTypes(types);
			}
			else
			{
				// World-list lookup failed (offline/blocked) -- best effort, matches prior
				// behavior, but this is now the degraded path, not the normal one.
				rsWorld.setActivity("Roleplaying");
			}

			try
			{
				client.changeWorld(rsWorld);
				log.info("Successfully requested world change to {}", worldId);
				lastEvent.set("world-change-requested:" + worldId);
				lastWorldSwitchFailureReason = null;
			}
			catch (Exception e)
			{
				log.error("Failed to change world using client API", e);
				lastEvent.set("world-change-api-failed:" + e.getMessage());
				lastWorldSwitchFailureReason = e.getMessage();
			}
		});
	}

	private String detectedLoginScreen(GameState gameState)
	{
		if (gameState == null)
		{
			return "unknown";
		}
		switch (gameState)
		{
			case LOGGED_IN:
				return "logged-in";
			case LOGIN_SCREEN:
				return "login-screen";
			case LOGIN_SCREEN_AUTHENTICATOR:
				return "authenticator";
			case LOGGING_IN:
				return "logging-in";
			case LOADING:
				return "loading";
			case CONNECTION_LOST:
				return "connection-lost";
			case HOPPING:
				return "world-hop";
			case STARTING:
				return "starting";
			default:
				return "unknown";
		}
	}

	private String currentWorldTypeText()
	{
		return safeValue(() -> client.getWorldType() == null ? "[]" : client.getWorldType().toString(), "unknown");
	}

	private void mobFarmerStep(boolean live, int generation, String source)
	{
		if (isStaleMobFarmerLoop(generation))
		{
			return;
		}
		int stepTick = safeValue(client::getTickCount, 0);
		if (generation > 0 && client.getGameState() == GameState.LOGGED_IN && "game-tick".equals(source) && lastMobFarmerLoopStepTick == stepTick)
		{
			return;
		}
		lastMobFarmerLoopStepTick = stepTick;
		lastMobFarmerLoopStepSource = source == null ? "unknown" : source;
		lastMobFarmerMultiCombat = isMultiCombat();
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			if (tryMobFarmerLoginRecovery(live, generation, client.getGameState()))
			{
				return;
			}
			mobFarmerStatus.set("needs-login:" + client.getGameState());
			setMobFarmerDecision("needs-login", null);
			updatePanelStatus("Mob farmer needs login: " + client.getGameState());
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			mobFarmerStatus.set("no-local-player");
			setMobFarmerDecision("no-local-player", null);
			updatePanelStatus("Mob farmer blocked: no local player");
			return;
		}

		// Always refresh NPC target diagnostics so /automation/mob-farmer/status.candidates
		// stays current even when this step returns early (e.g. while in combat with an
		// aggressive target). Read-only; does not affect the attack decision below. The
		// selection's pathfinding only runs for name-matching NPCs, so this is cheap.
		lastEntities = collectEntities();
		lastMobFarmerCandidates = selectMobFarmerTarget(localPlayer).reports;

		lastMobFarmerInventoryStatus = inventoryPolicyStatus();
		if (tryMobFarmerAutoEat(localPlayer, live, generation))
		{
			return;
		}
		if (tryMobFarmerStartupFocusClick(live, generation, "pre-actions"))
		{
			return;
		}
		if (tryMobFarmerAutorun(live, generation, "pre-actions"))
		{
			return;
		}
		if (tryMobFarmerReattackAfterPickup(localPlayer, live, generation))
		{
			return;
		}
		if (tryMobFarmerIntermediateAction(localPlayer, live, generation, "pre-combat"))
		{
			return;
		}

		Actor interacting = localPlayer.getInteracting();
		if (interacting != null && isEffectivelyDead(interacting))
		{
			recordPendingMobFarmerDeathLoot(interacting);
			if (tryMobFarmerLoot(localPlayer, live, generation, "dying-target-loot-window"))
			{
				return;
			}
			if (mobFarmerPendingDeathLootActive())
			{
				Map<String, Object> waitDetails = new LinkedHashMap<>(lastMobFarmerDeathLootStatus);
				waitDetails.put("reason", "waiting-for-loot-spawn");
				setMobFarmerDecision("waiting-for-loot-spawn", waitDetails);
				mobFarmerStatus.set("waiting-for-loot-spawn:" + interacting.getName());
				updatePanelStatus("Mob farmer waiting for loot spawn from dying target: " + interacting.getName());
				return;
			}
		}
		else if (mobFarmerPendingDeathLootActive())
		{
			if (tryMobFarmerLoot(localPlayer, live, generation, "pending-death-loot-window"))
			{
				clearPendingMobFarmerDeath("loot-window-attempted");
				return;
			}
		}
		else if (pendingMobFarmerDeathKey != null)
		{
			clearPendingMobFarmerDeath("death-loot-window-expired");
		}

		boolean holdCombatForStabilizer = mobFarmerShouldHoldCombat(interacting);
		if (holdCombatForStabilizer)
		{
			if (tryMobFarmerPriorityLootInterrupt(localPlayer, interacting, live, generation, "priority-before-combat"))
			{
				return;
			}
		}
		else if (tryMobFarmerPriorityLoot(localPlayer, live, generation, "priority-before-combat"))
		{
			return;
		}

		if (interacting != null && !isEffectivelyDead(interacting))
		{
			if (interacting instanceof NPC && matchesAnyMobTarget((NPC) interacting, mobFarmerTarget))
			{
				if (tryMobFarmerIntermediateAction(localPlayer, live, generation, "combat-window"))
				{
					return;
				}
				if (getMobFarmerLootDuringCombat() && !holdCombatForStabilizer && tryMobFarmerLoot(localPlayer, live, generation, "combat-window"))
				{
					return;
				}
				if (getMobFarmerLootDuringCombat() && holdCombatForStabilizer)
				{
					Map<String, Object> details = new LinkedHashMap<>();
					details.put("phase", "combat-window");
					details.put("currentTarget", actorSummary(interacting));
					details.put("suppressedAction", "normal-loot-during-combat");
					recordMobFarmerStabilization("held-combat-window-loot", details);
				}
				mobFarmerStatus.set("continuing-target:" + interacting.getName());
				setMobFarmerDecision("continuing-target", actorSummary(interacting));
				updatePanelStatus("Mob farmer continuing target: " + interacting.getName());
				return;
			}
			CvHelperMobAggroResponse aggroResponse = getMobFarmerAggroResponse();
			if (aggroResponse == CvHelperMobAggroResponse.CONTINUE_ANY_ATTACKER || (!lastMobFarmerMultiCombat && aggroResponse == CvHelperMobAggroResponse.IGNORE_IN_MULTI))
			{
				if (tryMobFarmerIntermediateAction(localPlayer, live, generation, "combat-window-undesired"))
				{
					return;
				}
				if (getMobFarmerLootDuringCombat() && tryMobFarmerLoot(localPlayer, live, generation, "combat-window-undesired"))
				{
					return;
				}
				mobFarmerStatus.set("continuing-undesired:" + interacting.getName());
				setMobFarmerDecision("continuing-undesired", actorSummary(interacting));
				updatePanelStatus("Mob farmer continuing undesired attacker: " + interacting.getName());
				return;
			}
			if (!lastMobFarmerMultiCombat || aggroResponse == CvHelperMobAggroResponse.WAIT)
			{
				mobFarmerStatus.set("blocked-undesired-combat:" + interacting.getName());
				setMobFarmerDecision("blocked-undesired-combat", actorSummary(interacting));
				updatePanelStatus("Mob farmer blocked by undesired combat: " + interacting.getName());
				return;
			}
		}

		if (holdMobFarmerAttackResolution(localPlayer))
		{
			return;
		}

		if (!getMobFarmerAttackBeforeLoot() && tryMobFarmerLoot(localPlayer, live, generation, "idle-before-attack"))
		{
			return;
		}
		if (mobFarmerMakeProgressActive() && tryMobFarmerLoot(localPlayer, live, generation, "make-progress-before-attack"))
		{
			return;
		}
		if (tryMobFarmerAfterLootCombatGuard(localPlayer, live))
		{
			return;
		}

		lastEntities = collectEntities();
		MobFarmerSelection selection = selectMobFarmerTarget(localPlayer);
		lastMobFarmerCandidates = selection.reports;
		lastMobFarmerMultiCombat = selection.multiCombat;
		setMobFarmerDecision(selection.decision, selection.target);
		Map<String, Object> target = selection.target;
		if (target == null)
		{
			if (tryMobFarmerLoot(localPlayer, live, generation, "idle-no-target"))
			{
				return;
			}
			mobFarmerStatus.set(selection.decision + ":" + mobFarmerTarget);
			updatePanelStatus("Mob farmer found no valid target: " + mobFarmerTarget + " | " + selection.decision);
			return;
		}

		if (tryHandleMobFarmerDoorTransition(localPlayer, target, live))
		{
			return;
		}

		Map<String, Object> clickPoint = firstPoint(target, "clickPoint", "center", "canvasTileCenter");
		if (getMobFarmerAttackInteractionMode() == CvHelperMobInteractionMode.DIRECT_CLICK && canvasPointToScreen(clickPoint) == null)
		{
			mobFarmerStatus.set("off-canvas:" + targetLabelForMessage(target));
			updatePanelStatus("Mob farmer target off-canvas: " + targetLabelForMessage(target));
			return;
		}

		if (!live)
		{
			String message = "Mob farmer dry target: " + targetLabelForMessage(target) + " @ " + clickPoint;
			mobFarmerStatus.set("dry-target:" + targetLabelForMessage(target) + "@" + clickPoint);
			updatePanelStatus(message);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
			return;
		}

		invokeMobFarmerAttack(target, clickPoint, live, generation);
	}

	private boolean tryMobFarmerStartupFocusClick(boolean live, int generation, String phase)
	{
		if (!getMobFarmerFocusClickAfterLogin())
		{
			recordMobFarmerFocusClick("disabled", phase, false, null);
			return false;
		}
		if (!mobFarmerFocusClickNeeded)
		{
			recordMobFarmerFocusClick("not-needed", phase, false, null);
			return false;
		}
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("phase", phase);
		details.put("live", live);
		details.put("generation", generation);
		Point focusPoint = loginCanvasFocusPoint();
		details.put("screenPoint", awtPointMap(focusPoint));
		if (focusPoint == null)
		{
			recordMobFarmerFocusClick("no-canvas-focus-point", phase, true, details);
			return false;
		}
		if (!live)
		{
			recordMobFarmerFocusClick("dry-run", phase, true, details);
			mobFarmerStatus.set("dry-focus-click-after-login");
			updatePanelStatus("Mob farmer dry startup focus click would click canvas center");
			mobFarmerFocusClickNeeded = false;
			return true;
		}
		if (!mobFarmerActionAllowed(MobFarmerActionKind.UI, "startup-focus", 0, "startup-focus-click"))
		{
			recordMobFarmerFocusClick("scheduler-wait", phase, true, details);
			return true;
		}
		if (!actionInProgress.compareAndSet(false, true))
		{
			recordMobFarmerFocusClick("action-running", phase, true, details);
			mobFarmerStatus.set("skipped:startup-focus:action-running");
			return true;
		}
		Thread clickThread = new Thread(() ->
		{
			Map<String, Object> result = new LinkedHashMap<>(details);
			try
			{
				if (isStaleMobFarmerLoop(generation))
				{
					result.put("stale", true);
					recordMobFarmerFocusClick("stale-loop", phase, true, result);
					return;
				}
				Robot robot = new Robot();
				clickScreenPoint(robot, focusPoint);
				result.put("clickedAt", Instant.now().toString());
				clientThread.invokeLater(() -> recordMobFarmerScheduledAction(MobFarmerActionKind.UI, "startup-focus", "startup-focus-click"));
				mobFarmerFocusClickNeeded = false;
				recordMobFarmerFocusClick("clicked", phase, true, result);
				mobFarmerStatus.set("startup-focus-clicked");
				updatePanelStatus("Mob farmer startup focus click sent");
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				result.put("failureReason", e.getMessage());
				recordMobFarmerFocusClick("failed", phase, true, result);
				log.warn("CV Helper mob farmer startup focus click failed", e);
				mobFarmerStatus.set("startup-focus-click-failed:" + e.getMessage());
				updatePanelStatus("Mob farmer startup focus click failed: " + e.getMessage());
			}
			finally
			{
				actionInProgress.set(false);
			}
		}, "cv-helper-mob-farmer-focus");
		clickThread.setDaemon(true);
		clickThread.start();
		return true;
	}

	void runMobFarmerStartupFocusClick()
	{
		clientThread.invokeLater(() ->
		{
			mobFarmerFocusClickNeeded = true;
			tryMobFarmerStartupFocusClick(true, mobFarmerGeneration.get(), "webhelper-manual");
		});
	}

	private void recordMobFarmerFocusClick(String result, String phase, boolean attemptedAction, Map<String, Object> details)
	{
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("result", result);
		status.put("phase", phase);
		status.put("attemptedAction", attemptedAction);
		status.put("enabled", getMobFarmerFocusClickAfterLogin());
		status.put("needed", mobFarmerFocusClickNeeded);
		status.put("at", Instant.now().toString());
		if (details != null)
		{
			status.put("details", details);
		}
		lastMobFarmerFocusClickStatus = status;
	}

	private boolean tryMobFarmerAutorun(boolean live, int generation, String phase)
	{
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("phase", phase);
		status.put("enabled", getMobFarmerAutorunEnabled());
		status.put("minEnergyPercent", getMobFarmerAutorunMinEnergy());
		status.put("runEnergyPercent", safeValue(() -> client.getEnergy() / 100.0, -1.0));
		status.put("runEnabled", runEnabled());
		if (!getMobFarmerAutorunEnabled())
		{
			status.put("result", "disabled");
			lastMobFarmerAutorunStatus = status;
			return false;
		}
		if (runEnabled())
		{
			status.put("result", "already-enabled");
			lastMobFarmerAutorunStatus = status;
			return false;
		}
		double energy = safeValue(() -> client.getEnergy() / 100.0, -1.0);
		if (energy < getMobFarmerAutorunMinEnergy())
		{
			status.put("result", "below-energy-threshold");
			lastMobFarmerAutorunStatus = status;
			return false;
		}
		List<Map<String, Object>> minimapTargets = collectMinimapTargets();
		Map<String, Object> runTarget = minimapTargets.stream()
			.filter(target -> String.valueOf(target.get("label")).toLowerCase().contains("run"))
			.findFirst()
			.orElse(null);
		status.put("target", runTarget);
		if (runTarget == null)
		{
			status.put("result", "run-target-missing");
			lastMobFarmerAutorunStatus = status;
			return false;
		}
		status.put("result", live ? "toggle-run" : "dry-run");
		lastMobFarmerAutorunStatus = status;
		mobFarmerStatus.set(live ? "autorun-toggle" : "dry-autorun-toggle");
		setMobFarmerDecision(live ? "autorun-toggle" : "dry-autorun-toggle", status);
		return clickMobFarmerAutomationTarget("autorun", runTarget, live, generation);
	}

	private boolean tryMobFarmerAfterLootCombatGuard(Player localPlayer, boolean live)
	{
		CvHelperAfterLootCombatMode mode = getMobFarmerAfterLootCombatMode();
		if (mode == CvHelperAfterLootCombatMode.RESUME_TARGETING)
		{
			return false;
		}
		Integer lastLootTick = lastMobFarmerActionTickForKind(MobFarmerActionKind.LOOT_PICKUP);
		if (lastLootTick == null)
		{
			return false;
		}
		int tick = safeValue(client::getTickCount, 0);
		int ticksSinceLoot = tick - lastLootTick;
		if (ticksSinceLoot < 0 || ticksSinceLoot > Math.max(1, MOB_FARMER_LOOT_RESOLUTION_MAX_TICKS))
		{
			return false;
		}
		Map<String, Object> attacker = localPlayer.getInteracting() == null ? findNpcAttackingLocalPlayer(localPlayer) : actorSummary(localPlayer.getInteracting());
		if (attacker == null)
		{
			return false;
		}
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("mode", mode.name());
		details.put("live", live);
		details.put("tick", tick);
		details.put("ticksSinceLoot", ticksSinceLoot);
		details.put("attacker", attacker);
		if (mode == CvHelperAfterLootCombatMode.STOP_WHEN_TAGGED)
		{
			setMobFarmerDecision("stopping-after-loot-tagged", details);
			mobFarmerStatus.set("stopping-after-loot-tagged:" + targetLabelForMessage(attacker));
			updatePanelStatus("Mob farmer stopping after loot because attacker is already on player: " + targetLabelForMessage(attacker));
			if (live)
			{
				stopMobFarmer();
			}
			return true;
		}
		setMobFarmerDecision("holding-after-loot-tagged", details);
		mobFarmerStatus.set("holding-after-loot-tagged:" + targetLabelForMessage(attacker));
		updatePanelStatus("Mob farmer holding after loot because attacker is already on player: " + targetLabelForMessage(attacker));
		return true;
	}

	private Map<String, Object> findNpcAttackingLocalPlayer(Player localPlayer)
	{
		if (localPlayer == null)
		{
			return null;
		}
		for (NPC npc : client.getNpcs())
		{
			if (npc == null || isEffectivelyDead(npc) || npc.getInteracting() != localPlayer)
			{
				continue;
			}
			return actorSummary(npc);
		}
		return null;
	}

	private boolean tryMobFarmerAutoEat(Player localPlayer, boolean live, int generation)
	{
		if (!getMobFarmerAutoEatEnabled())
		{
			setMobFarmerSurvivalDecision("auto-eat-disabled", null);
			return false;
		}

		int currentHp = Math.max(0, client.getBoostedSkillLevel(Skill.HITPOINTS));
		int realHp = Math.max(1, client.getRealSkillLevel(Skill.HITPOINTS));
		double percent = currentHp * 100.0 / realHp;
		Map<String, Object> hp = new LinkedHashMap<>();
		hp.put("current", currentHp);
		hp.put("real", realHp);
		hp.put("percent", percent);
		hp.put("thresholdPercent", getMobFarmerEatHitpointPercent());
		hp.put("survivalPreemptsActions", getMobFarmerSurvivalPreemptsActions());
		if (percent > getMobFarmerEatHitpointPercent())
		{
			setMobFarmerSurvivalDecision("hp-ok", hp);
			return false;
		}
		if (getMobFarmerSurvivalPreemptsActions())
		{
			hp.put("preemptedDecision", new LinkedHashMap<>(lastMobFarmerDecision));
			hp.put("preemptedIntents", new ArrayList<>(lastMobFarmerIntents));
		}

		lastInventoryTargets = collectInventoryTargets();
		lastMobFarmerInventoryStatus = inventoryPolicyStatus();
		Map<String, Object> food = findFoodInventoryTarget(lastInventoryTargets);
		if (food == null)
		{
			if (!isInventoryVisible())
			{
				setMobFarmerSurvivalDecision("opening-inventory-for-food", hp);
				mobFarmerStatus.set("opening-inventory-for-food");
				updatePanelStatus("Mob farmer auto-eat: opening inventory");
				if (live)
				{
					openAutomationPanel("inventory", "auto-eat", generation);
				}
				else
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Mob farmer dry auto-eat: would open inventory", "");
				}
				return true;
			}

			setMobFarmerSurvivalDecision("no-food", hp);
			if (getMobFarmerStopIfNoFood())
			{
				mobFarmerStatus.set("blocked:no-food");
				updatePanelStatus("Mob farmer auto-eat blocked: no matching food");
				stopMobFarmer();
				return true;
			}
			mobFarmerStatus.set("warning:no-food-continue");
			updatePanelStatus("Mob farmer auto-eat warning: no matching food, continuing because stop-if-no-food is off");
			return false;
		}

		Map<String, Object> decision = new LinkedHashMap<>(hp);
		decision.put("target", food);
		setMobFarmerSurvivalDecision("eat", decision);
		return invokeMobFarmerInventoryAction("auto-eat", food, live, generation, "Eat", "Drink", "Consume");
	}

	private Map<String, Object> findFoodInventoryTarget(List<Map<String, Object>> inventoryTargets)
	{
		for (Map<String, Object> target : inventoryTargets)
		{
			String name = String.valueOf(target.get("itemName"));
			int itemId = intValue(target.get("itemId"), -1);
			int quantity = intValue(target.get("quantity"), 1);
			if (name.trim().isEmpty() || "null".equals(name))
			{
				continue;
			}
			if (!matchesItemPolicy(name, itemId, quantity, getMobFarmerFoodItems()))
			{
				continue;
			}
			String[] actions = inventoryTargetActions(target);
			if (hasActionNamed(actions, "Eat") || hasActionNamed(actions, "Drink") || hasActionNamed(actions, "Consume"))
			{
				return target;
			}
		}
		return null;
	}

	private boolean tryMobFarmerIntermediateAction(Player localPlayer, boolean live, int generation, String phase)
	{
		if (!getMobFarmerIntermediateActionsEnabled())
		{
			setMobFarmerIntermediateDecision("disabled:" + phase, null);
			return false;
		}
		lastInventoryTargets = collectInventoryTargets();
		lastMobFarmerInventoryStatus = inventoryPolicyStatus();
		IntermediateInventoryAction intermediateAction = findIntermediateInventoryAction(lastInventoryTargets);
		if (intermediateAction == null)
		{
			if (!isInventoryVisible())
			{
				setMobFarmerIntermediateDecision("opening-inventory:" + phase, null);
				mobFarmerStatus.set("opening-inventory-for-intermediate");
				updatePanelStatus("Mob farmer intermediate action: opening inventory");
				if (live)
				{
					openAutomationPanel("inventory", "intermediate", generation);
				}
				else
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Mob farmer dry intermediate: would open inventory", "");
				}
				return true;
			}
			setMobFarmerIntermediateDecision("none:" + phase, null);
			return false;
		}
		if (!intermediateAction.isActionable())
		{
			recordSkippedIntermediateAction(intermediateAction, phase);
			return false;
		}

		Map<String, Object> decision = new LinkedHashMap<>();
		decision.put("phase", phase);
		decision.put("target", intermediateAction.target);
		decision.put("matchedRule", intermediateAction.matchedRule);
		decision.put("configuredAction", configuredActionText(intermediateAction.preferredActions));
		decision.put("configuredActions", Arrays.asList(intermediateAction.preferredActions));
		decision.put("availableItemActions", Arrays.asList(inventoryTargetActions(intermediateAction.target)));
		decision.put("preferredActions", Arrays.asList(intermediateAction.preferredActions));
		decision.put("intendedAction", firstMatchingActionName(inventoryTargetActions(intermediateAction.target), intermediateAction.preferredActions));
		setMobFarmerIntermediateDecision("inventory-action", decision);
		Map<String, Object> targetWithRule = new LinkedHashMap<>(intermediateAction.target);
		targetWithRule.put("intermediateMatchedRule", intermediateAction.matchedRule);
		targetWithRule.put("intermediateConfiguredActions", Arrays.asList(intermediateAction.preferredActions));
		return invokeMobFarmerInventoryAction("intermediate", targetWithRule, live, generation, intermediateAction.preferredActions);
	}

	private IntermediateInventoryAction findIntermediateInventoryAction(List<Map<String, Object>> inventoryTargets)
	{
		IntermediateInventoryAction firstSkipped = null;
		List<IntermediateActionRule> rules = intermediateActionRules();
		for (Map<String, Object> target : inventoryTargets)
		{
			String name = String.valueOf(target.get("itemName"));
			int itemId = intValue(target.get("itemId"), -1);
			int quantity = intValue(target.get("quantity"), 1);
			if (name.trim().isEmpty() || "null".equals(name))
			{
				continue;
			}
			IntermediateActionRule matchedRule = matchingIntermediateActionRule(name, itemId, quantity, rules);
			boolean legacyMatch = matchesItemPolicy(name, itemId, quantity, getMobFarmerIntermediateItems());
			if (matchedRule == null && !legacyMatch)
			{
				continue;
			}
			String[] preferredActions = matchedRule == null ? preferredIntermediateActions(target) : matchedRule.actions;
			String matchedRuleText = matchedRule == null ? "legacy-category:" + getMobFarmerIntermediateItems() : matchedRule.source;
			String[] actions = inventoryTargetActions(target);
			if (preferredActions.length == 0)
			{
				if (firstSkipped == null)
				{
					firstSkipped = new IntermediateInventoryAction(target, preferredActions, "unsupported-intermediate-item", matchedRuleText);
				}
				continue;
			}
			if (firstMatchingActionName(actions, preferredActions) != null)
			{
				return new IntermediateInventoryAction(target, preferredActions, null, matchedRuleText);
			}
			if (firstSkipped == null)
			{
				firstSkipped = new IntermediateInventoryAction(target, preferredActions, "required-action-unavailable", matchedRuleText);
			}
		}
		return firstSkipped;
	}

	private List<IntermediateActionRule> intermediateActionRules()
	{
		List<IntermediateActionRule> rules = new ArrayList<>();
		String mappings = getMobFarmerIntermediateActionMappings();
		if (mappings.trim().isEmpty())
		{
			return rules;
		}
		for (String rawRule : mappings.split("\\s*(?:\\r?\\n|;)\\s*"))
		{
			String rule = rawRule.trim();
			if (rule.isEmpty())
			{
				continue;
			}
			String[] parts = rule.split("\\s*(?:->|=>|=|:)\\s*", 2);
			if (parts.length != 2)
			{
				continue;
			}
			String itemTarget = parts[0].trim();
			List<String> actions = actionTargetCandidates(parts[1]);
			if (itemTarget.isEmpty() || actions.isEmpty())
			{
				continue;
			}
			rules.add(new IntermediateActionRule(itemTarget, actions.toArray(new String[0]), rule));
		}
		return rules;
	}

	private IntermediateActionRule matchingIntermediateActionRule(String name, int itemId, int quantity, List<IntermediateActionRule> rules)
	{
		for (IntermediateActionRule rule : rules)
		{
			if (matchesItemTarget(name, itemId, quantity, rule.itemTarget))
			{
				return rule;
			}
		}
		return null;
	}

	private List<Map<String, Object>> parsedIntermediateActionMappingsStatus()
	{
		List<Map<String, Object>> out = new ArrayList<>();
		for (IntermediateActionRule rule : intermediateActionRules())
		{
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("item", rule.itemTarget);
			entry.put("configuredAction", configuredActionText(rule.actions));
			entry.put("actions", Arrays.asList(rule.actions));
			entry.put("source", rule.source);
			out.add(entry);
		}
		return out;
	}

	private String configuredActionText(String[] actions)
	{
		return actions == null || actions.length == 0 ? "" : String.join("|", actions);
	}

	private String[] preferredIntermediateActions(Map<String, Object> target)
	{
		String itemName = String.valueOf(target.get("itemName"));
		String normalizedName = normalize(itemName);
		if (normalizedName.contains("ash"))
		{
			return new String[]{"Scatter", "Bury"};
		}
		if (normalizedName.contains("bone"))
		{
			return new String[]{"Bury"};
		}
		return new String[0];
	}

	private void recordSkippedIntermediateAction(IntermediateInventoryAction intermediateAction, String phase)
	{
		Map<String, Object> target = intermediateAction.target;
		String label = targetLabelForMessage(target);
		String itemName = String.valueOf(target.get("itemName"));
		int slot = intValue(target.get("index"), -1);
		String[] availableActions = inventoryTargetActions(target);
		String intendedAction = firstMatchingActionName(availableActions, intermediateAction.preferredActions);
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("phase", phase);
		details.put("target", target);
		details.put("itemName", itemName);
		details.put("slot", slot);
		details.put("availableActions", Arrays.asList(availableActions));
		details.put("availableItemActions", Arrays.asList(availableActions));
		details.put("matchedRule", intermediateAction.matchedRule);
		details.put("configuredAction", configuredActionText(intermediateAction.preferredActions));
		details.put("configuredActions", Arrays.asList(intermediateAction.preferredActions));
		details.put("preferredActions", Arrays.asList(intermediateAction.preferredActions));
		details.put("intendedAction", intendedAction);
		details.put("actualAction", null);
		details.put("result", "skipped");
		details.put("failureReason", intermediateAction.failureReason);
		setMobFarmerIntermediateDecision("skipped:" + intermediateAction.failureReason, details);
		recordMobFarmerActionAttempt("intermediate", details);
		mobFarmerStatus.set("intermediate-skipped:" + intermediateAction.failureReason + ":" + label);
		updatePanelStatus("Mob farmer intermediate skipped: " + label + " slot " + slot + " (" + intermediateAction.failureReason + ")");
	}

	private boolean tryMobFarmerLoot(Player localPlayer, boolean live, int generation, String phase)
	{
		return tryMobFarmerLoot(localPlayer, live, generation, phase, false);
	}

	private boolean tryMobFarmerPriorityLoot(Player localPlayer, boolean live, int generation, String phase)
	{
		return tryMobFarmerLoot(localPlayer, live, generation, phase, true);
	}

	private boolean tryMobFarmerLoot(Player localPlayer, boolean live, int generation, String phase, boolean priorityOnly)
	{
		if (!getMobFarmerLootEnabled())
		{
			setMobFarmerLootDecision("loot-disabled:" + phase, null);
			return false;
		}

		MobFarmerLootSelection selection = selectMobFarmerLoot(localPlayer, priorityOnly);
		lastMobFarmerLootCandidates = selection.reports;
		setMobFarmerLootDecision(selection.decision + ":" + phase, selection.target);
		if (selection.target == null)
		{
			if (phase.startsWith("make-progress"))
			{
				clearMobFarmerLootChase("no-valid-loot:" + phase);
			}
			return false;
		}
		recordMobFarmerIntent("LOOT_ITEM", selection.target);
		return clickMobFarmerAutomationTarget("loot", selection.target, live, generation);
	}

	private MobFarmerLootSelection selectMobFarmerLoot(Player localPlayer)
	{
		return selectMobFarmerLoot(localPlayer, false);
	}

	private MobFarmerLootSelection selectMobFarmerLoot(Player localPlayer, boolean priorityOnly)
	{
		MobFarmerLootSelection selection = new MobFarmerLootSelection();
		selection.priorityOnly = priorityOnly;
		MobFarmerLootCandidate best = null;
		List<MobFarmerLootCandidate> candidates = new ArrayList<>();
		List<MobFarmerLootCandidate> selectable = new ArrayList<>();
		for (Map<String, Object> item : collectGroundItemTargets(localPlayer))
		{
			MobFarmerLootCandidate candidate = evaluateMobFarmerLootCandidate(item);
			candidates.add(candidate);
			if (candidate.selectable)
			{
				selectable.add(candidate);
			}
		}
		selection.selectableCount = selectable.size();
		for (MobFarmerLootCandidate candidate : selectable)
		{
			applyMobFarmerLootPriority(candidate, selection.selectableCount);
		}
		for (MobFarmerLootCandidate candidate : candidates)
		{
			selection.reports.add(lootCandidateReport(candidate));
			if (!candidate.selectable)
			{
				continue;
			}
			if (priorityOnly && !candidate.highPriority)
			{
				continue;
			}
			if (best == null || candidate.score < best.score)
			{
				best = candidate;
			}
		}

		if (best == null)
		{
			selection.decision = selection.reports.isEmpty() ? "no-loot-candidates" : (priorityOnly ? "no-priority-loot" : "no-valid-loot");
			return selection;
		}

		selection.target = best.item;
		selection.decision = (best.highPriority ? "selected-priority-loot:" : "selected-loot:") + targetLabelForMessage(best.item);
		return selection;
	}

	private MobFarmerLootCandidate evaluateMobFarmerLootCandidate(Map<String, Object> item)
	{
		MobFarmerLootCandidate candidate = new MobFarmerLootCandidate(item);
		int distance = intValue(item.get("distance"), Integer.MAX_VALUE);
		long value = longValue(item.get("gePrice"));
		long haValue = longValue(item.get("haPrice"));
		long geEach = longValue(item.get("gePriceEach"));
		long haEach = longValue(item.get("haPriceEach"));
		candidate.score = distance * 1000 - (int) Math.min(5000, value / 10);

		String name = String.valueOf(item.get("name"));
		int itemId = intValue(item.get("itemId"), -1);
		int quantity = intValue(item.get("quantity"), 1);
		boolean stackable = Boolean.TRUE.equals(item.get("stackable"));
		boolean cvAllowlist = matchesItemPolicy(name, itemId, quantity, getMobFarmerLootItems());
		boolean cvBlacklist = matchesItemPolicy(name, itemId, quantity, getMobFarmerLootBlacklist());
		boolean groundItemsHighlighted = Boolean.TRUE.equals(item.get("groundItemsHighlighted"));
		boolean groundItemsHidden = Boolean.TRUE.equals(item.get("groundItemsHidden"));
		boolean groundItemsHiddenByValue = Boolean.TRUE.equals(item.get("groundItemsHiddenByValue"));
		boolean groundItemsSuppressedByShowHighlightedOnly = Boolean.TRUE.equals(item.get("groundItemsSuppressedByShowHighlightedOnly"));
		boolean groundItemsSupplements = getMobFarmerGroundItemsMode() == CvHelperGroundItemsMode.SUPPLEMENT;
		if (cvBlacklist)
		{
			candidate.reject("blacklisted");
		}
		if (groundItemsHidden || groundItemsHiddenByValue || groundItemsSuppressedByShowHighlightedOnly)
		{
			if (groundItemsHidden)
			{
				candidate.note("ground-items-hidden");
			}
			if (groundItemsHiddenByValue)
			{
				candidate.note("ground-items-hidden-by-value");
			}
			if (groundItemsSuppressedByShowHighlightedOnly)
			{
				candidate.note("ground-items-show-highlighted-only");
			}
			if (getMobFarmerRespectGroundItemsHidden() && !cvAllowlist)
			{
				candidate.reject("ground-items-hidden");
			}
		}
		boolean explicitlyAllowed = cvAllowlist || (groundItemsSupplements && groundItemsHighlighted);
		item.put("singleGeValue", geEach);
		item.put("totalStackGeValue", value);
		item.put("singleHaValue", haEach);
		item.put("totalStackHaValue", haValue);
		item.put("allowlistMatch", cvAllowlist);
		item.put("denylistMatch", cvBlacklist);
		if (!explicitlyAllowed && getMobFarmerLootNeverStackBelowGe() > 0 && value < getMobFarmerLootNeverStackBelowGe())
		{
			candidate.reject("stack-below-never-threshold:" + value + "<" + getMobFarmerLootNeverStackBelowGe());
		}
		if (!explicitlyAllowed && value < getMobFarmerLootMinValueGe())
		{
			candidate.reject("below-value:" + value + "<" + getMobFarmerLootMinValueGe());
		}
		if (!explicitlyAllowed && getMobFarmerLootMinSingleGe() > 0 && geEach < getMobFarmerLootMinSingleGe())
		{
			candidate.reject("below-single-ge:" + geEach + "<" + getMobFarmerLootMinSingleGe());
		}
		if (!explicitlyAllowed && getMobFarmerLootMinStackGe() > 0 && value < getMobFarmerLootMinStackGe())
		{
			candidate.reject("below-stack-ge:" + value + "<" + getMobFarmerLootMinStackGe());
		}
		if (!explicitlyAllowed && stackable && getMobFarmerLootMinStackQuantity() > 0 && quantity < getMobFarmerLootMinStackQuantity())
		{
			candidate.reject("below-stack-quantity:" + quantity + "<" + getMobFarmerLootMinStackQuantity());
		}
		if (cvAllowlist)
		{
			candidate.note("allowlist");
		}
		if (groundItemsSupplements && groundItemsHighlighted)
		{
			candidate.note("ground-items-highlighted");
		}
		String temporarySkipReason = mobFarmerTemporaryLootSkipReason(item);
		if (temporarySkipReason != null)
		{
			candidate.reject(temporarySkipReason);
		}
		if (!lootOwnershipAccepted(item))
		{
			candidate.reject("ownership:" + item.get("ownership"));
		}
		int radius = getMobFarmerLootRadius();
		if (radius > 0 && distance > radius)
		{
			candidate.reject("too-far:" + distance + ">" + radius);
		}
		if (getMobFarmerLootInteractionMode() == CvHelperMobInteractionMode.MENU_ACTION)
		{
			if (intValue(item.get("sceneX"), Integer.MIN_VALUE) == Integer.MIN_VALUE || intValue(item.get("sceneY"), Integer.MIN_VALUE) == Integer.MIN_VALUE)
			{
				candidate.reject("no-menu-scene-point");
			}
		}
		else if (!(item.get("clickPoint") instanceof Map))
		{
			candidate.reject("no-click-point");
		}
		if (!inventoryCanAcceptItem(itemId, Boolean.TRUE.equals(item.get("stackable"))))
		{
			candidate.reject("inventory-full");
		}

		item.put("mobFarmerSelectable", candidate.selectable);
		item.put("mobFarmerReasons", new ArrayList<>(candidate.reasons));
		item.put("mobFarmerScore", candidate.score);
		return candidate;
	}

	private void applyMobFarmerLootPriority(MobFarmerLootCandidate candidate, int selectableCount)
	{
		long value = longValue(candidate.item.get("gePrice"));
		int priorityValue = getMobFarmerHighPriorityLootValueGe();
		int alwaysStackValue = getMobFarmerLootAlwaysStackGe();
		if (alwaysStackValue > 0 && value >= alwaysStackValue)
		{
			candidate.priority("stack-value:" + value + ">=" + alwaysStackValue);
			candidate.score -= 22000;
		}
		if (priorityValue == 0 || value >= priorityValue)
		{
			candidate.priority("value:" + value + ">=" + priorityValue);
			candidate.score -= 20000;
		}
		if (candidate.reasons.contains("allowlist"))
		{
			candidate.priority("allowlist");
			candidate.score -= 15000;
		}
		if (candidate.reasons.contains("ground-items-highlighted"))
		{
			candidate.priority("ground-items-highlighted");
			candidate.score -= 12000;
		}
		int urgentTicks = getMobFarmerLootUrgentDespawnTicks();
		int despawnTicks = intValue(candidate.item.get("despawnInTicks"), Integer.MAX_VALUE);
		if (urgentTicks > 0 && despawnTicks >= 0 && despawnTicks <= urgentTicks)
		{
			candidate.priority("despawn:" + despawnTicks + "<=" + urgentTicks);
			candidate.score -= 18000;
		}
		int cleanupCount = getMobFarmerLootCleanupPileCount();
		if (cleanupCount > 0 && selectableCount >= cleanupCount)
		{
			candidate.priority("cleanup-pile-count:" + selectableCount + ">=" + cleanupCount);
			candidate.score -= 5000;
		}
		candidate.item.put("mobFarmerHighPriority", candidate.highPriority);
		candidate.item.put("mobFarmerPriorityReasons", new ArrayList<>(candidate.priorityReasons));
		candidate.item.put("mobFarmerScore", candidate.score);
	}

	private List<Map<String, Object>> collectGroundItemTargets(Player localPlayer)
	{
		List<Map<String, Object>> items = new ArrayList<>();
		WorldView worldView = localPlayer == null ? client.getTopLevelWorldView() : localPlayer.getWorldView();
		if (worldView == null || worldView.getScene() == null)
		{
			return items;
		}
		Scene scene = worldView.getScene();
		Tile[][][] tiles = scene.getTiles();
		int plane = Math.max(0, Math.min(worldView.getPlane(), tiles.length - 1));
		if (tiles.length == 0 || tiles[plane] == null)
		{
			return items;
		}
		int maxX = Math.min(worldView.getSizeX(), tiles[plane].length);
		for (int x = 0; x < maxX; x++)
		{
			if (tiles[plane][x] == null)
			{
				continue;
			}
			int maxY = Math.min(worldView.getSizeY(), tiles[plane][x].length);
			for (int y = 0; y < maxY; y++)
			{
				addTileGroundItems(items, tiles[plane][x][y], localPlayer);
			}
		}
		return items;
	}

	private void addTileGroundItems(List<Map<String, Object>> items, Tile tile, Player localPlayer)
	{
		if (tile == null || tile.getGroundItems() == null || tile.getGroundItems().isEmpty())
		{
			return;
		}
		for (TileItem item : tile.getGroundItems())
		{
			Map<String, Object> target = groundItemTarget(tile, item, localPlayer);
			if (target != null)
			{
				items.add(target);
			}
		}
	}

	private Map<String, Object> freshGroundItemTarget(Map<String, Object> target)
	{
		int itemId = intValue(target.get("itemId"), -1);
		int sceneX = intValue(target.get("sceneX"), Integer.MIN_VALUE);
		int sceneY = intValue(target.get("sceneY"), Integer.MIN_VALUE);
		if (itemId <= 0 || sceneX == Integer.MIN_VALUE || sceneY == Integer.MIN_VALUE)
		{
			return null;
		}
		Player localPlayer = client.getLocalPlayer();
		WorldView worldView = localPlayer == null ? client.getTopLevelWorldView() : localPlayer.getWorldView();
		if (worldView == null || worldView.getScene() == null)
		{
			return null;
		}
		Scene scene = worldView.getScene();
		Tile[][][] tiles = scene.getTiles();
		int plane = Math.max(0, Math.min(worldView.getPlane(), tiles.length - 1));
		if (tiles.length == 0 || tiles[plane] == null || sceneX < 0 || sceneX >= tiles[plane].length || tiles[plane][sceneX] == null || sceneY < 0 || sceneY >= tiles[plane][sceneX].length)
		{
			return null;
		}
		Tile tile = tiles[plane][sceneX][sceneY];
		if (tile == null || tile.getGroundItems() == null)
		{
			return null;
		}
		for (TileItem item : tile.getGroundItems())
		{
			if (item != null && item.getId() == itemId)
			{
				return groundItemTarget(tile, item, localPlayer);
			}
		}
		return null;
	}

	private Map<String, Object> groundItemTarget(Tile tile, TileItem item, Player localPlayer)
	{
		if (tile == null || item == null || item.getId() <= 0)
		{
			return null;
		}
		ItemComposition composition = itemManager.getItemComposition(item.getId());
		String name = composition == null ? "" : cleanWidgetText(composition.getName());
		if (name.isEmpty() || "null".equals(name))
		{
			return null;
		}
		int geEach = itemManager.getItemPrice(item.getId());
		int haEach = composition == null ? 0 : composition.getHaPrice();
		long ge = (long) geEach * Math.max(1, item.getQuantity());
		long ha = (long) haEach * Math.max(1, item.getQuantity());
		GroundItemsClassification groundItemsClassification = groundItemsClassification(name, item.getQuantity(), geEach, haEach, composition);
		net.runelite.api.Point scenePoint = tile.getSceneLocation();
		Map<String, Object> target = new LinkedHashMap<>();
		target.put("surface", "loot");
		target.put("label", name + (item.getQuantity() > 1 ? " x" + item.getQuantity() : ""));
		target.put("name", name);
		target.put("itemId", item.getId());
		target.put("quantity", item.getQuantity());
		target.put("gePriceEach", geEach);
		target.put("haPriceEach", haEach);
		target.put("gePrice", ge);
		target.put("haPrice", ha);
		target.put("stackable", composition != null && composition.isStackable());
		target.put("ownership", ownershipName(item.getOwnership()));
		target.put("ownershipId", item.getOwnership());
		target.put("private", item.isPrivate());
		target.put("visibleInTicks", item.getVisibleTime() - client.getTickCount());
		target.put("despawnInTicks", item.getDespawnTime() - client.getTickCount());
		target.put("groundItemsClassification", groundItemsClassification.name());
		target.put("groundItemsHighlighted", groundItemsClassification == GroundItemsClassification.HIGHLIGHTED);
		target.put("groundItemsHidden", groundItemsClassification == GroundItemsClassification.HIDDEN);
		target.put("groundItemsHiddenByValue", groundItemsClassification == GroundItemsClassification.HIDDEN_BY_VALUE);
		target.put("groundItemsSuppressedByShowHighlightedOnly", groundItemsClassification == GroundItemsClassification.SUPPRESSED_BY_SHOW_HIGHLIGHTED_ONLY);
		if (scenePoint != null)
		{
			target.put("sceneX", scenePoint.getX());
			target.put("sceneY", scenePoint.getY());
		}
		target.put("menuAction", MenuAction.GROUND_ITEM_THIRD_OPTION.name());
		target.put("menuOption", "Take");
		target.put("menuTarget", name);
		target.put("worldLocation", pointValue(tile.getWorldLocation()));
		target.put("localLocation", pointValue(tile.getLocalLocation()));
		target.put("sceneLocation", pointValue(tile.getSceneLocation()));
		target.put("clickPoint", pointValue(Perspective.localToCanvas(client, tile.getLocalLocation(), tile.getPlane())));
		Polygon tilePoly = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());
		if (tilePoly != null)
		{
			target.put("bounds", boundsMap(tilePoly.getBounds()));
			if (!(target.get("clickPoint") instanceof Map))
			{
				target.put("clickPoint", centerFromBounds(boundsMap(tilePoly.getBounds())));
			}
		}
		if (localPlayer != null && localPlayer.getWorldLocation() != null && tile.getWorldLocation() != null)
		{
			target.put("distance", localPlayer.getWorldLocation().distanceTo(tile.getWorldLocation()));
		}
		return target;
	}

	private Map<String, Object> lootCandidateReport(MobFarmerLootCandidate candidate)
	{
		Map<String, Object> report = new LinkedHashMap<>();
		report.put("name", candidate.item.get("name"));
		report.put("itemId", candidate.item.get("itemId"));
		report.put("quantity", candidate.item.get("quantity"));
		report.put("distance", candidate.item.get("distance"));
		report.put("gePriceEach", candidate.item.get("gePriceEach"));
		report.put("gePrice", candidate.item.get("gePrice"));
		report.put("totalStackGeValue", candidate.item.get("totalStackGeValue"));
		report.put("haPriceEach", candidate.item.get("haPriceEach"));
		report.put("haPrice", candidate.item.get("haPrice"));
		report.put("totalStackHaValue", candidate.item.get("totalStackHaValue"));
		report.put("stackable", candidate.item.get("stackable"));
		report.put("allowlistMatch", candidate.item.get("allowlistMatch"));
		report.put("denylistMatch", candidate.item.get("denylistMatch"));
		report.put("ownership", candidate.item.get("ownership"));
		report.put("selectable", candidate.selectable);
		report.put("score", candidate.score);
		report.put("reasons", new ArrayList<>(candidate.reasons));
		report.put("highPriority", candidate.highPriority);
		report.put("priorityReasons", new ArrayList<>(candidate.priorityReasons));
		report.put("groundItemsClassification", candidate.item.get("groundItemsClassification"));
		report.put("groundItemsHighlighted", candidate.item.get("groundItemsHighlighted"));
		report.put("groundItemsHidden", candidate.item.get("groundItemsHidden"));
		report.put("groundItemsHiddenByValue", candidate.item.get("groundItemsHiddenByValue"));
		report.put("groundItemsSuppressedByShowHighlightedOnly", candidate.item.get("groundItemsSuppressedByShowHighlightedOnly"));
		report.put("sceneX", candidate.item.get("sceneX"));
		report.put("sceneY", candidate.item.get("sceneY"));
		report.put("menuAction", candidate.item.get("menuAction"));
		report.put("menuOption", candidate.item.get("menuOption"));
		report.put("clickPoint", candidate.item.get("clickPoint"));
		report.put("worldLocation", candidate.item.get("worldLocation"));
		return report;
	}

	private boolean clickMobFarmerAutomationTarget(String action, Map<String, Object> target, boolean live, int generation)
	{
		Map<String, Object> baseClickPoint = firstPoint(target, "clickPoint", "center", "canvasTileCenter");
		Map<String, Object> clickPoint = randomizedClickPoint(target, baseClickPoint);
		if ("loot".equals(action) && getMobFarmerLootInteractionMode() == CvHelperMobInteractionMode.MENU_ACTION)
		{
			return invokeMobFarmerLootTake(target, clickPoint, live, generation);
		}
		Point screenPoint = canvasPointToScreen(clickPoint);
		if (screenPoint == null)
		{
			mobFarmerStatus.set(action + "-off-canvas:" + targetLabelForMessage(target));
			updatePanelStatus("Mob farmer " + action + " target off-canvas: " + targetLabelForMessage(target));
			return !"loot".equals(action);
		}

		String label = targetLabelForMessage(target);
		if (!live)
		{
			String message = "Mob farmer dry " + action + ": " + label + " @ " + clickPoint;
			mobFarmerStatus.set("dry-" + action + ":" + label + "@" + clickPoint);
			updatePanelStatus(message);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
			return true;
		}

		MobFarmerActionKind actionKind = mobFarmerClickActionKind(action);
		String schedulerTarget = mobFarmerTargetKey(target);
		if (!mobFarmerActionAllowed(actionKind, schedulerTarget, 1, action + "-click"))
		{
			mobFarmerStatus.set("skipped:" + action + ":scheduler-wait");
			updatePanelStatus("Mob farmer " + action + " skipped: waiting for next tick window");
			return true;
		}

		if (!actionInProgress.compareAndSet(false, true))
		{
			mobFarmerStatus.set("skipped:" + action + ":action-running");
			updatePanelStatus("Mob farmer " + action + " skipped: action already running");
			return true;
		}

		Thread clickThread = new Thread(() ->
		{
			try
			{
				if (isStaleMobFarmerLoop(generation))
				{
					return;
				}
				Robot robot = new Robot();
				clickScreenPoint(robot, screenPoint);
				clientThread.invokeLater(() ->
				{
					recordMobFarmerScheduledAction(actionKind, schedulerTarget, action + "-click");
					if ("attack".equals(action))
					{
						recordMobFarmerCombatLease(target, action + "-click");
					}
				});
				if (isStaleMobFarmerLoop(generation))
				{
					return;
				}
				String message = "Mob farmer " + action + " clicked " + label + " @ " + clickPoint;
				mobFarmerStatus.set(action + "-clicked:" + label);
				lastEvent.set("mob-farmer-" + action + "@" + label + "@" + Instant.now());
				updatePanelStatus(message);
				clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, ""));
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				log.warn("CV Helper mob farmer {} click failed", action, e);
				mobFarmerStatus.set(action + "-click-failed:" + e.getMessage());
				updatePanelStatus("Mob farmer " + action + " click failed: " + e.getMessage());
			}
			finally
			{
				actionInProgress.set(false);
			}
		}, "cv-helper-mob-farmer-" + action);
		clickThread.setDaemon(true);
		clickThread.start();
		return true;
	}

	private MobFarmerActionKind mobFarmerClickActionKind(String action)
	{
		if ("attack".equals(action))
		{
			return MobFarmerActionKind.COMBAT;
		}
		if ("loot".equals(action))
		{
			return MobFarmerActionKind.LOOT_PICKUP;
		}
		if ("autorun".equals(action))
		{
			return MobFarmerActionKind.UI;
		}
		return MobFarmerActionKind.MOVEMENT;
	}

	private boolean invokeMobFarmerInventoryAction(String actionName, Map<String, Object> target, boolean live, int generation, String... preferredActions)
	{
		String label = targetLabelForMessage(target);
		String itemName = String.valueOf(target.get("itemName"));
		int slot = intValue(target.get("index"), -1);
		String[] availableActions = inventoryTargetActions(target);
		String intendedAction = firstMatchingActionName(availableActions, preferredActions);
		InventoryMenuAction menu = inventoryMenuAction(target, availableActions, preferredActions);
		Map<String, Object> attempt = new LinkedHashMap<>();
		attempt.put("kind", actionName);
		attempt.put("target", label);
		attempt.put("itemName", itemName);
		attempt.put("slot", slot);
		attempt.put("availableActions", Arrays.asList(availableActions));
		attempt.put("availableItemActions", Arrays.asList(availableActions));
		attempt.put("intendedAction", intendedAction);
		attempt.put("targetSnapshot", target);
		attempt.put("preferredActions", Arrays.asList(preferredActions));
		if ("intermediate".equals(actionName))
		{
			attempt.put("matchedRule", target.get("intermediateMatchedRule"));
			attempt.put("configuredAction", configuredActionText(preferredActions));
			attempt.put("configuredActions", target.get("intermediateConfiguredActions") instanceof List ? target.get("intermediateConfiguredActions") : Arrays.asList(preferredActions));
		}
		if (menu != null)
		{
			attempt.put("actualAction", menu.option);
			attempt.put("menu", menu.asMap());
		}
		if ("intermediate".equals(actionName) && menu != null && normalize(menu.option).equals("drop"))
		{
			attempt.put("result", "skipped");
			attempt.put("failureReason", "drop-action-blocked");
			recordMobFarmerActionAttempt(actionName, attempt);
			setMobFarmerIntermediateDecision("skipped:drop-action-blocked", attempt);
			mobFarmerStatus.set("intermediate-skipped:drop-action-blocked:" + label);
			updatePanelStatus("Mob farmer intermediate skipped: blocked Drop on " + label + " slot " + slot);
			return false;
		}
		if (!live)
		{
			attempt.put("result", menu == null ? "dry-missing-menu-action" : "dry");
			if (menu == null)
			{
				attempt.put("failureReason", intendedAction == null ? "no-supported-inventory-action" : "menu-action-unresolved");
			}
			recordMobFarmerActionAttempt(actionName, attempt);
			if ("intermediate".equals(actionName))
			{
				setMobFarmerIntermediateDecision(menu == null ? "dry-skipped:menu-missing" : "dry:" + menu.option, attempt);
			}
			String message = menu == null
				? "Mob farmer dry " + actionName + ": skipped " + label + " slot " + slot + " (intended=" + intendedAction + ", available=" + Arrays.toString(availableActions) + ")"
				: "Mob farmer dry " + actionName + ": " + menu.option + " " + label + " slot " + slot + " via " + menu.menuAction;
			mobFarmerStatus.set("dry-" + actionName + ":" + label);
			updatePanelStatus(message);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
			return true;
		}
		if (menu == null)
		{
			attempt.put("result", "missing-menu-action");
			attempt.put("failureReason", intendedAction == null ? "no-supported-inventory-action" : "menu-action-unresolved");
			recordMobFarmerActionAttempt(actionName, attempt);
			mobFarmerStatus.set(actionName + "-skipped:menu-missing:" + label);
			String reason = intendedAction == null
				? "no supported action"
				: "unable to resolve menu action for " + intendedAction;
			updatePanelStatus("Mob farmer " + actionName + " skipped: " + label + " slot " + slot + " (" + reason + ")");
			if ("intermediate".equals(actionName))
			{
				Map<String, Object> details = new LinkedHashMap<>();
				details.put("target", target);
				details.put("itemName", itemName);
				details.put("slot", slot);
				details.put("intendedAction", intendedAction);
				details.put("actualAction", null);
				details.put("availableActions", Arrays.asList(availableActions));
				details.put("availableItemActions", Arrays.asList(availableActions));
				details.put("configuredAction", configuredActionText(preferredActions));
				details.put("configuredActions", Arrays.asList(preferredActions));
				details.put("matchedRule", target.get("intermediateMatchedRule"));
				details.put("reason", reason);
				setMobFarmerIntermediateDecision("skipped:menu-missing", details);
			}
			return false;
		}
		if (isStaleMobFarmerLoop(generation))
		{
			attempt.put("result", "stale-loop");
			recordMobFarmerActionAttempt(actionName, attempt);
			if ("intermediate".equals(actionName))
			{
				setMobFarmerIntermediateDecision("skipped:stale-loop", attempt);
			}
			return true;
		}
		MobFarmerActionKind schedulerKind = mobFarmerInventoryActionKind(actionName);
		String schedulerTarget = itemName + "@" + slot;
		if (!mobFarmerActionAllowed(schedulerKind, schedulerTarget, 1, actionName + ":" + menu.option))
		{
			attempt.put("result", "scheduler-wait");
			attempt.put("schedulerKind", schedulerKind.name());
			attempt.put("schedulerTarget", schedulerTarget);
			recordMobFarmerActionAttempt(actionName, attempt);
			if ("intermediate".equals(actionName))
			{
				setMobFarmerIntermediateDecision("skipped:scheduler-wait", attempt);
			}
			mobFarmerStatus.set("skipped:" + actionName + ":scheduler-wait");
			updatePanelStatus("Mob farmer " + actionName + " skipped: waiting for next tick window");
			return true;
		}
		if (!actionInProgress.compareAndSet(false, true))
		{
			attempt.put("result", "action-running");
			recordMobFarmerActionAttempt(actionName, attempt);
			if ("intermediate".equals(actionName))
			{
				setMobFarmerIntermediateDecision("skipped:action-running", attempt);
			}
			mobFarmerStatus.set("skipped:" + actionName + ":action-running");
			updatePanelStatus("Mob farmer " + actionName + " skipped: action already running");
			return true;
		}
		try
		{
			client.menuAction(menu.param0, menu.param1, menu.menuAction, menu.identifier, menu.itemId, menu.option, label);
			recordMobFarmerScheduledAction(schedulerKind, schedulerTarget, actionName + ":" + menu.option);
			attempt.put("result", "invoked");
			attempt.put("actualAction", menu.option);
			recordMobFarmerActionAttempt(actionName, attempt);
			if ("intermediate".equals(actionName))
			{
				setMobFarmerIntermediateDecision("invoked:" + menu.option, attempt);
			}
			String message = "Mob farmer " + actionName + " invoked " + menu.option + " on " + label + " slot " + slot + " via " + menu.menuAction;
			mobFarmerStatus.set(actionName + "-menu:" + label);
			lastEvent.set("mob-farmer-" + actionName + "@" + label + "@" + Instant.now());
			updatePanelStatus(message);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
			return true;
		}
		catch (RuntimeException e)
		{
			log.warn("CV Helper mob farmer {} inventory action failed", actionName, e);
			attempt.put("result", "failed");
			attempt.put("error", e.getMessage());
			recordMobFarmerActionAttempt(actionName, attempt);
			if ("intermediate".equals(actionName))
			{
				setMobFarmerIntermediateDecision("failed:" + menu.option, attempt);
			}
			mobFarmerStatus.set(actionName + "-menu-failed:" + e.getMessage());
			updatePanelStatus("Mob farmer " + actionName + " menu failed: " + e.getMessage());
			return false;
		}
		finally
		{
			actionInProgress.set(false);
		}
	}

	private MobFarmerActionKind mobFarmerInventoryActionKind(String actionName)
	{
		return "auto-eat".equals(actionName) ? MobFarmerActionKind.SURVIVAL : MobFarmerActionKind.INVENTORY;
	}

	private InventoryMenuAction inventoryMenuAction(Map<String, Object> target, String... preferredActions)
	{
		return inventoryMenuAction(target, inventoryTargetActions(target), preferredActions);
	}

	private InventoryMenuAction inventoryMenuAction(Map<String, Object> target, String[] actions, String... preferredActions)
	{
		int widgetId = intValue(target.get("widgetId"), -1);
		if (widgetId <= 0)
		{
			widgetId = intValue(target.get("parentId"), -1);
		}
		int itemId = intValue(target.get("itemId"), -1);
		int param0 = intValue(target.get("index"), -1);
		if (widgetId <= 0 || itemId <= 0)
		{
			return null;
		}
		// The component op id must come from the widget's full 10-slot action array
		// (target.actions, i.e. Widget#getActions()), where actions[k] is shown at op (k+1).
		// The composition-derived 5-slot inventoryActions array (used for the "actions"
		// param / diagnostics) does NOT line up with op ids 1:1 across items - e.g. for
		// Bones, inventoryActions=["Bury",null,null,null,"Drop"] but the real widget
		// actions array is [null,"Bury",null,null,null,null,"Drop",null,null,"Examine"],
		// so Bury is really op 2, not the i+3=3 the old offset heuristic guessed (verified
		// live: manual Bury logged identifier=2). Drop happened to still work under the old
		// heuristic by coincidence (its widget-array index is also 6 -> op 7 = i+3 with i=4).
		String[] widgetActions = rawWidgetActions(target);
		for (String preferred : preferredActions)
		{
			InventoryMenuAction menu = inventoryMenuActionForOption(widgetActions, preferred, param0, widgetId, itemId);
			if (menu != null)
			{
				return menu;
			}
		}
		return null;
	}

	private String[] rawWidgetActions(Map<String, Object> target)
	{
		Object actions = target.get("actions");
		if (actions instanceof String[])
		{
			return (String[]) actions;
		}
		if (actions instanceof List)
		{
			List<?> actionList = (List<?>) actions;
			String[] out = new String[actionList.size()];
			for (int i = 0; i < actionList.size(); i++)
			{
				out[i] = actionList.get(i) == null ? null : String.valueOf(actionList.get(i));
			}
			return out;
		}
		return new String[0];
	}

	private InventoryMenuAction inventoryMenuActionForOption(String[] widgetActions, String preferred, int param0, int widgetId, int itemId)
	{
		if (widgetActions == null || preferred == null)
		{
			return null;
		}
		for (int i = 0; i < widgetActions.length; i++)
		{
			String action = widgetActions[i];
			if (action == null || !action.equalsIgnoreCase(preferred))
			{
				continue;
			}
			int opId = i + 1;
			MenuAction menuAction = opId >= 6 ? MenuAction.CC_OP_LOW_PRIORITY : MenuAction.CC_OP;
			return new InventoryMenuAction(i, opId, param0, widgetId, menuAction, opId, itemId, action);
		}
		return null;
	}

	private String firstMatchingActionName(String[] availableActions, String... preferredActions)
	{
		if (availableActions == null || preferredActions == null)
		{
			return null;
		}
		for (String preferred : preferredActions)
		{
			if (preferred == null)
			{
				continue;
			}
			for (String available : availableActions)
			{
				if (available != null && available.equalsIgnoreCase(preferred))
				{
					return available;
				}
			}
		}
		return null;
	}

	private void invokeMobFarmerAttack(Map<String, Object> target, Map<String, Object> clickPoint, boolean live, int generation)
	{
		recordMobFarmerIntent("ATTACK_TARGET", target);
		if (getMobFarmerAttackInteractionMode() == CvHelperMobInteractionMode.MENU_ACTION)
		{
			invokeMobFarmerNpcMenuAction(target, clickPoint, live, generation);
			return;
		}
		clickMobFarmerAutomationTarget("attack", target, live, generation);
	}

	private boolean invokeMobFarmerNpcMenuAction(Map<String, Object> target, Map<String, Object> clickPoint, boolean live, int generation)
	{
		String label = targetLabelForMessage(target);
		Map<String, Object> attempt = new LinkedHashMap<>();
		attempt.put("kind", "attack");
		attempt.put("target", label);
		attempt.put("targetSnapshot", target);
		attempt.put("clickPoint", clickPoint);
		if (!live)
		{
			attempt.put("result", "dry");
			recordMobFarmerActionAttempt("attack", attempt);
			String message = "Mob farmer dry attack menu: " + label + " @ " + clickPoint;
			mobFarmerStatus.set("dry-attack-menu:" + label + "@" + clickPoint);
			updatePanelStatus(message);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
			return true;
		}
		if (isStaleMobFarmerLoop(generation))
		{
			attempt.put("result", "stale-loop");
			recordMobFarmerActionAttempt("attack", attempt);
			return true;
		}
		String schedulerTarget = mobFarmerTargetKey(target);
		if (!mobFarmerActionAllowed(MobFarmerActionKind.COMBAT, schedulerTarget, 1, "attack-menu-action"))
		{
			attempt.put("result", "scheduler-wait");
			attempt.put("schedulerKind", MobFarmerActionKind.COMBAT.name());
			attempt.put("schedulerTarget", schedulerTarget);
			recordMobFarmerActionAttempt("attack", attempt);
			mobFarmerStatus.set("skipped:attack:scheduler-wait");
			updatePanelStatus("Mob farmer attack skipped: waiting for next tick window");
			return true;
		}
		if (!actionInProgress.compareAndSet(false, true))
		{
			attempt.put("result", "action-running");
			recordMobFarmerActionAttempt("attack", attempt);
			mobFarmerStatus.set("skipped:attack:action-running");
			updatePanelStatus("Mob farmer attack skipped: action already running");
			return true;
		}
		try
		{
			int index = intValue(target.get("index"), -1);
			MenuAction menuAction = npcMenuActionForIndex(intValue(target.get("attackActionIndex"), -1));
			if (index < 0 || menuAction == null)
			{
				attempt.put("result", "missing-menu-action");
				recordMobFarmerActionAttempt("attack", attempt);
				mobFarmerStatus.set("attack-menu-missing:" + label);
				updatePanelStatus("Mob farmer attack menu missing: " + label);
				return true;
			}
			attempt.put("menuAction", menuAction.name());
			attempt.put("identifier", index);
			attempt.put("option", "Attack");
			client.menuAction(0, 0, menuAction, index, -1, "Attack", label);
			recordMobFarmerScheduledAction(MobFarmerActionKind.COMBAT, schedulerTarget, "attack-menu-action");
			recordMobFarmerCombatLease(target, "attack-menu-action");
			attempt.put("result", "invoked");
			recordMobFarmerActionAttempt("attack", attempt);
			String message = "Mob farmer menu-attacked " + label + " via " + menuAction;
			mobFarmerStatus.set("menu-attack:" + label);
			lastEvent.set("mob-farmer-menu-attack@" + label + "@" + Instant.now());
			updatePanelStatus(message);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
		}
		catch (RuntimeException e)
		{
			log.warn("CV Helper mob farmer menu attack failed", e);
			attempt.put("result", "failed");
			attempt.put("error", e.getMessage());
			recordMobFarmerActionAttempt("attack", attempt);
			mobFarmerStatus.set("attack-menu-failed:" + e.getMessage());
			updatePanelStatus("Mob farmer attack menu failed: " + e.getMessage());
		}
		finally
		{
			actionInProgress.set(false);
		}
		return true;
	}

	private boolean invokeMobFarmerLootTake(Map<String, Object> target, Map<String, Object> clickPoint, boolean live, int generation)
	{
		String label = targetLabelForMessage(target);
		Map<String, Object> attempt = new LinkedHashMap<>();
		attempt.put("kind", "loot");
		attempt.put("target", label);
		attempt.put("targetSnapshot", target);
		attempt.put("clickPoint", clickPoint);
		if (!live)
		{
			attempt.put("result", "dry");
			recordMobFarmerActionAttempt("loot", attempt);
			String message = "Mob farmer dry loot menu: " + label + " @ " + clickPoint;
			mobFarmerStatus.set("dry-loot-menu:" + label + "@" + clickPoint);
			updatePanelStatus(message);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
			return true;
		}
		if (isStaleMobFarmerLoop(generation))
		{
			attempt.put("result", "stale-loop");
			recordMobFarmerActionAttempt("loot", attempt);
			return true;
		}
		String schedulerTarget = mobFarmerTargetKey(target);
		if (!mobFarmerActionAllowed(MobFarmerActionKind.LOOT_PICKUP, schedulerTarget, 1, "loot-menu-action"))
		{
			attempt.put("result", "scheduler-wait");
			attempt.put("schedulerKind", MobFarmerActionKind.LOOT_PICKUP.name());
			attempt.put("schedulerTarget", schedulerTarget);
			recordMobFarmerActionAttempt("loot", attempt);
			mobFarmerStatus.set("skipped:loot:scheduler-wait");
			updatePanelStatus("Mob farmer loot skipped: waiting for next tick window");
			return true;
		}
		if (!actionInProgress.compareAndSet(false, true))
		{
			attempt.put("result", "action-running");
			recordMobFarmerActionAttempt("loot", attempt);
			mobFarmerStatus.set("skipped:loot:action-running");
			updatePanelStatus("Mob farmer loot skipped: action already running");
			return true;
		}
		try
		{
			int itemId = intValue(target.get("itemId"), -1);
			int sceneX = intValue(target.get("sceneX"), Integer.MIN_VALUE);
			int sceneY = intValue(target.get("sceneY"), Integer.MIN_VALUE);
			if (itemId <= 0 || sceneX == Integer.MIN_VALUE || sceneY == Integer.MIN_VALUE)
			{
				attempt.put("result", "missing-scene-point");
				recordMobFarmerActionAttempt("loot", attempt);
				mobFarmerStatus.set("loot-menu-missing:" + label);
				updatePanelStatus("Mob farmer loot menu missing: " + label);
				return false;
			}
			Map<String, Object> freshTarget = freshGroundItemTarget(target);
			if (freshTarget == null)
			{
				attempt.put("result", "stale-target");
				recordMobFarmerActionAttempt("loot", attempt);
				mobFarmerStatus.set("loot-stale:" + label);
				updatePanelStatus("Mob farmer loot stale before pickup: " + label);
				return false;
			}
			MobFarmerLootCandidate freshCandidate = evaluateMobFarmerLootCandidate(freshTarget);
			attempt.put("freshTarget", freshTarget);
			attempt.put("freshSelectable", freshCandidate.selectable);
			attempt.put("freshReasons", new ArrayList<>(freshCandidate.reasons));
			if (!freshCandidate.selectable)
			{
				attempt.put("result", "fresh-target-rejected");
				recordMobFarmerActionAttempt("loot", attempt);
				mobFarmerStatus.set("loot-rejected:" + label);
				updatePanelStatus("Mob farmer loot rejected before pickup: " + label + " | " + freshCandidate.reasons);
				return false;
			}
			sceneX = intValue(freshTarget.get("sceneX"), sceneX);
			sceneY = intValue(freshTarget.get("sceneY"), sceneY);
			itemId = intValue(freshTarget.get("itemId"), itemId);
			attempt.put("menuAction", MenuAction.GROUND_ITEM_THIRD_OPTION.name());
			attempt.put("param0", sceneX);
			attempt.put("param1", sceneY);
			attempt.put("identifier", itemId);
			attempt.put("itemId", itemId);
			attempt.put("option", "Take");
			client.menuAction(sceneX, sceneY, MenuAction.GROUND_ITEM_THIRD_OPTION, itemId, itemId, "Take", label);
			recordMobFarmerScheduledAction(MobFarmerActionKind.LOOT_PICKUP, schedulerTarget, "loot-menu-action");
			attempt.put("result", "invoked");
			attempt.put("reattackQueued", true);
			queueMobFarmerReattackAfterPickup(label, freshTarget);
			recordMobFarmerActionAttempt("loot", attempt);
			String message = "Mob farmer menu-took " + label + " @ scene " + sceneX + "," + sceneY;
			mobFarmerStatus.set("menu-loot:" + label);
			lastEvent.set("mob-farmer-menu-loot@" + label + "@" + Instant.now());
			updatePanelStatus(message);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
		}
		catch (RuntimeException e)
		{
			log.warn("CV Helper mob farmer menu loot failed", e);
			attempt.put("result", "failed");
			attempt.put("error", e.getMessage());
			recordMobFarmerActionAttempt("loot", attempt);
			mobFarmerStatus.set("loot-menu-failed:" + e.getMessage());
			updatePanelStatus("Mob farmer loot menu failed: " + e.getMessage());
			return false;
		}
		finally
		{
			actionInProgress.set(false);
		}
		return true;
	}

	private boolean openAutomationPanel(String panelName, String reason, int generation)
	{
		PanelSwitchTarget panelTarget = panelSwitchTarget(panelName, true);
		if (panelTarget == null)
		{
			return false;
		}
		if (!actionInProgress.compareAndSet(false, true))
		{
			return true;
		}
		Thread openThread = new Thread(() ->
		{
			try
			{
				if (isStaleMobFarmerLoop(generation))
				{
					return;
				}
				Robot robot = new Robot();
				activatePanel(robot, panelTarget, true);
				lastEvent.set("mob-farmer-open-panel@" + panelName + "@" + reason + "@" + Instant.now());
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				log.warn("CV Helper mob farmer panel open failed", e);
				mobFarmerStatus.set("panel-open-failed:" + panelName + ":" + e.getMessage());
			}
			finally
			{
				actionInProgress.set(false);
			}
		}, "cv-helper-mob-farmer-open-panel");
		openThread.setDaemon(true);
		openThread.start();
		return true;
	}

	private Map<String, Object> getSkillFarmerStatus(String skill)
	{
		boolean mining = "mining".equals(skill);
		Map<String, Object> status = new LinkedHashMap<>(mining ? lastMiningFarmerStatus : lastWoodcuttingFarmerStatus);
		String target = mining ? miningFarmerTarget : woodcuttingFarmerTarget;
		status.put("skill", skill);
		status.put("running", mining ? miningFarmerRunning.get() : woodcuttingFarmerRunning.get());
		status.put("live", mining ? miningFarmerLiveMode : woodcuttingFarmerLiveMode);
		status.put("target", target);
		status.put("runtimeTarget", target);
		status.put("configSource", "runtime-config");
		status.put("scanRadiusTiles", getSkillFarmerScanRadius(skill));
		status.put("maxCandidates", getSkillFarmerMaxCandidates(skill));
		status.put("inventory", inventoryPolicyStatus());
		return status;
	}

	private Map<String, Object> skillFarmerConfigPayload(String skill)
	{
		boolean mining = "mining".equals(skill);
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("target", mining ? miningFarmerTarget : woodcuttingFarmerTarget);
		settings.put("live", mining ? miningFarmerLiveMode : woodcuttingFarmerLiveMode);
		settings.put("scanRadiusTiles", getSkillFarmerScanRadius(skill));
		settings.put("maxCandidates", getSkillFarmerMaxCandidates(skill));
		settings.put("protectedItems", getMobFarmerNeverDropItems());
		settings.put("inventoryPolicy", "REPORT_ONLY");
		settings.put("dropPolicyEnabled", config.dropPolicyEnabled());
		settings.put("dropPolicyMode", config.dropPolicyMode().name());
		settings.put("dropPolicyThresholdSlots", config.dropPolicyThresholdSlots());
		settings.put("dropPolicyItems", mining ? config.miningDropItems() : config.dropPolicyItems());
		settings.put("dropPolicyProtectedItems", config.dropPolicyProtectedItems());
		settings.put("dropPolicyMaxValue", config.dropPolicyMaxValue());
		settings.put("woodcuttingStickToTarget", config.woodcuttingStickToTarget());
		settings.put("woodcuttingReclickWhenActivelyChopping", config.woodcuttingReclickWhenActivelyChopping());

		List<Map<String, Object>> schema = new ArrayList<>();
		schema.add(settingSchema("target", mining ? "Target rocks/ores" : "Target trees/logs", "text", "Pipe/comma/newline separated names or id:123 object ids. The farmer selects the nearest reachable matching object with the correct menu action.", null));
		schema.add(settingSchema("live", "Live mode default", "boolean", "When enabled, Start/Step sends real menu actions. Leave off for dry selection/debug.", null));
		schema.add(settingSchema("scanRadiusTiles", "Scan radius tiles", "number", "Scene search radius around the player. Increase this if RuneLite is rendering targets farther out; lower it if scans feel heavy.", null));
		schema.add(settingSchema("maxCandidates", "Max candidates", "number", "Maximum object candidates returned to WebHelper and overlay. Higher values show more boxes but can be heavier.", null));
		schema.add(settingSchema("protectedItems", "Protected inventory items", "textarea", "Shared never-drop/protected list from the mob farmer. Skill farmers expose it for drop/replacement decisions.", null));
		schema.add(settingSchema("inventoryPolicy", "Inventory policy", "select", "Shared inventory policy reference. Actual dropping behavior is controlled by the drop policy settings below.", Arrays.asList("REPORT_ONLY")));
		schema.add(settingSchema("woodcuttingStickToTarget", "Stick to current tree", "boolean", "While actively chopping a tree that still matches the target list, do not switch to a different tree. This avoids wasting ticks re-targeting and prevents re-clicking the same tree.", null));
		schema.add(settingSchema("woodcuttingReclickWhenActivelyChopping", "Re-click while chopping", "boolean", "When enabled, the farmer will continue sending Chop down clicks while the woodcutting animation is running. Most players want this OFF because the click interrupts are slower than letting the axe swing uninterrupted.", null));
		schema.add(settingSchema("dropPolicyEnabled", "Drop policy enabled", "boolean", "Enable the conditional drop policy for skill farmers. When disabled, farmers use their legacy inventory handling.", null));
		schema.add(settingSchema("dropPolicyMode", "Drop mode", "select", "When to drop items: NEVER disables dropping; WHEN_FULL drops only when inventory is full; WHEN_IDLE drops when not actively chopping (batch drop while moving between trees); AFTER_TARGET drops after each target cycle completes; AFTER_GATHER drops after each successful gather action; CLEANUP_ONLY drops only during explicit cleanup phases; MANUAL_ONLY requires manual invocation.", Arrays.asList("NEVER", "WHEN_FULL", "WHEN_IDLE", "AFTER_TARGET", "AFTER_GATHER", "CLEANUP_ONLY", "MANUAL_ONLY")));
		schema.add(settingSchema("dropPolicyThresholdSlots", "Drop threshold slots", "number", "Minimum occupied inventory slots before dropping is considered. For WHEN_FULL mode, this is typically 28. For other modes, lower values allow earlier cleanup.", null));
		schema.add(settingSchema("dropPolicyItems", mining ? "Droppable items (mining)" : "Droppable items (woodcutting)", "textarea", "Items this farmer may drop when conditions are met. Mining and woodcutting keep SEPARATE lists. If empty, any non-protected item below max value is a candidate. Separated by |, comma, semicolon, or newlines.", null));
		schema.add(settingSchema("dropPolicyProtectedItems", "Protected items", "textarea", "Items that must never be dropped. Tools, food, teleport items, runes, and valuable items should be listed here. Built-in safeguards always protect clue/rare unique items.", null));
		schema.add(settingSchema("dropPolicyMaxValue", "Max drop value", "number", "Maximum GE value of an item that can be dropped automatically. Items above this value are protected even if not in the protected list.", null));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("version", 1);
		body.put("skill", skill);
		body.put("settings", settings);
		body.put("schema", schema);
		body.put("presets", mining ? miningProfiles() : woodcuttingProfiles());
		return body;
	}

	private Map<String, Object> applySkillFarmerConfigPayload(String skill, Map<String, Object> payload)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		List<String> errors = new ArrayList<>();
		if (payload == null)
		{
			errors.add("Payload is empty.");
		}
		Object version = payload == null ? null : payload.get("version");
		if (!(version instanceof Number) || ((Number) version).intValue() != 1)
		{
			errors.add("Unsupported or missing version. Expected version 1.");
		}
		Map<String, Object> settings = payload == null ? new LinkedHashMap<>() : mapValue(payload.get("settings"));
		String target = settings.get("target") == null ? "" : String.valueOf(settings.get("target")).trim();
		if (target.isEmpty())
		{
			errors.add("target is required.");
		}
		Boolean live = null;
		if (settings.containsKey("live"))
		{
			live = booleanSettingValue(settings.get("live"), "live", errors);
		}
		int scanRadius = settings.containsKey("scanRadiusTiles")
			? Math.max(4, Math.min(64, intSettingValue(settings.get("scanRadiusTiles"), getSkillFarmerScanRadius(skill), "scanRadiusTiles", errors)))
			: getSkillFarmerScanRadius(skill);
		int maxCandidates = settings.containsKey("maxCandidates")
			? Math.max(1, Math.min(300, intSettingValue(settings.get("maxCandidates"), getSkillFarmerMaxCandidates(skill), "maxCandidates", errors)))
			: getSkillFarmerMaxCandidates(skill);
		if (!errors.isEmpty())
		{
			result.put("ok", false);
			result.put("applied", false);
			result.put("errors", errors);
			return result;
		}
		if ("mining".equals(skill))
		{
			miningFarmerTarget = target;
			if (live != null)
			{
				miningFarmerLiveMode = Boolean.TRUE.equals(live);
			}
			miningFarmerScanRadius = scanRadius;
			miningFarmerMaxCandidates = maxCandidates;
			configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MINING_FARMER_TARGET, target);
			configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MINING_SCAN_RADIUS, scanRadius);
			configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.MINING_MAX_CANDIDATES, maxCandidates);
		}
		else
		{
			woodcuttingFarmerTarget = target;
			if (live != null)
			{
				woodcuttingFarmerLiveMode = Boolean.TRUE.equals(live);
			}
			woodcuttingFarmerScanRadius = scanRadius;
			woodcuttingFarmerMaxCandidates = maxCandidates;
			configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.WOODCUTTING_FARMER_TARGET, target);
			configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.WOODCUTTING_SCAN_RADIUS, scanRadius);
			configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.WOODCUTTING_MAX_CANDIDATES, maxCandidates);
		}
		Object protectedItems = settings.get("protectedItems");
		if (protectedItems instanceof String)
		{
			setMobFarmerNeverDropItems((String) protectedItems);
		}
		Boolean dropPolicyEnabled = settings.containsKey("dropPolicyEnabled")
			? booleanSettingValue(settings.get("dropPolicyEnabled"), "dropPolicyEnabled", errors)
			: config.dropPolicyEnabled();
		String dropPolicyModeStr = settings.containsKey("dropPolicyMode")
			? String.valueOf(settings.get("dropPolicyMode")).trim()
			: config.dropPolicyMode().name();
		CvHelperDropMode dropPolicyMode;
		try
		{
			dropPolicyMode = CvHelperDropMode.valueOf(dropPolicyModeStr);
		}
		catch (IllegalArgumentException e)
		{
			errors.add("Invalid dropPolicyMode: " + dropPolicyModeStr);
			dropPolicyMode = config.dropPolicyMode();
		}
		int dropPolicyThresholdSlots = settings.containsKey("dropPolicyThresholdSlots")
			? Math.max(1, Math.min(28, intSettingValue(settings.get("dropPolicyThresholdSlots"), config.dropPolicyThresholdSlots(), "dropPolicyThresholdSlots", errors)))
			: config.dropPolicyThresholdSlots();
		String dropPolicyItems = settings.containsKey("dropPolicyItems")
			? String.valueOf(settings.get("dropPolicyItems")).trim()
			: ("mining".equals(skill) ? config.miningDropItems() : config.dropPolicyItems());
		String dropPolicyProtectedItems = settings.containsKey("dropPolicyProtectedItems")
			? String.valueOf(settings.get("dropPolicyProtectedItems")).trim()
			: config.dropPolicyProtectedItems();
		int dropPolicyMaxValue = settings.containsKey("dropPolicyMaxValue")
			? Math.max(0, intSettingValue(settings.get("dropPolicyMaxValue"), config.dropPolicyMaxValue(), "dropPolicyMaxValue", errors))
			: config.dropPolicyMaxValue();

		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.DROP_POLICY_ENABLED, dropPolicyEnabled);
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.DROP_POLICY_MODE, dropPolicyMode);
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.DROP_POLICY_THRESHOLD_SLOTS, dropPolicyThresholdSlots);
		configManager.setConfiguration(CvHelperModConfig.GROUP, "mining".equals(skill) ? CvHelperModConfig.MINING_DROP_ITEMS : CvHelperModConfig.DROP_POLICY_ITEMS, dropPolicyItems);
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.DROP_POLICY_PROTECTED_ITEMS, dropPolicyProtectedItems);
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.DROP_POLICY_MAX_VALUE, dropPolicyMaxValue);

		Boolean woodcuttingStickToTarget = settings.containsKey("woodcuttingStickToTarget")
			? booleanSettingValue(settings.get("woodcuttingStickToTarget"), "woodcuttingStickToTarget", errors)
			: config.woodcuttingStickToTarget();
		Boolean woodcuttingReclickWhenActivelyChopping = settings.containsKey("woodcuttingReclickWhenActivelyChopping")
			? booleanSettingValue(settings.get("woodcuttingReclickWhenActivelyChopping"), "woodcuttingReclickWhenActivelyChopping", errors)
			: config.woodcuttingReclickWhenActivelyChopping();
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.WOODCUTTING_STICK_TO_TARGET, woodcuttingStickToTarget);
		configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.WOODCUTTING_RECLICK_WHEN_ACTIVE, woodcuttingReclickWhenActivelyChopping);

		result.put("ok", true);
		result.put("applied", true);
		result.put("errors", errors);
		result.put("config", skillFarmerConfigPayload(skill));
		return result;
	}

	private List<Map<String, Object>> miningProfiles()
	{
		List<Map<String, Object>> profiles = new ArrayList<>();
		profiles.add(skillProfile("Clay", "exact:Clay rocks|clay"));
		profiles.add(skillProfile("Copper/tin", "exact:Copper rocks|exact:Tin rocks|copper|tin|id:11361|id:11360|id:11362|id:11363"));
		profiles.add(skillProfile("Blurite", "exact:Blurite rocks|blurite"));
		profiles.add(skillProfile("Iron", "exact:Iron rocks|iron ore rocks|iron|id:11364|id:11365"));
		profiles.add(skillProfile("Silver", "exact:Silver rocks|silver|id:11368|id:11369"));
		profiles.add(skillProfile("Coal", "exact:Coal rocks|coal ore rocks|coal|id:11366|id:11367"));
		profiles.add(skillProfile("Gold", "exact:Gold rocks|gold ore rocks|gold|id:11370|id:11371"));
		profiles.add(skillProfile("Mithril", "exact:Mithril rocks|mithril ore rocks|mithril|id:11372|id:11373"));
		profiles.add(skillProfile("Adamantite", "exact:Adamantite rocks|adamantite ore rocks|adamantite|id:11374|id:11375"));
		profiles.add(skillProfile("Runite", "exact:Runite rocks|runite ore rocks|runite|id:11376|id:11377"));
		profiles.add(skillProfile("Amethyst", "exact:Amethyst crystals|amethyst"));
		profiles.add(skillProfile("Gem rocks", "exact:Gem rocks|gem"));
		profiles.add(skillProfile("Granite", "exact:Granite rocks|granite"));
		profiles.add(skillProfile("Sandstone", "exact:Sandstone rocks|sandstone"));
		profiles.add(skillProfile("Lovakite", "exact:Lovakite rocks|lovakite"));
		profiles.add(skillProfile("Daeyalt", "exact:Daeyalt rocks|daeyalt"));
		profiles.add(skillProfile("Limestone", "exact:Limestone|limestone"));
		profiles.add(skillProfile("Volcanic sulphur", "exact:Volcanic sulphur|volcanic sulphur"));
		profiles.add(skillProfile("Rune essence", "exact:Rune essence|rune essence"));
		profiles.add(skillProfile("Pure essence", "exact:Pure essence|pure essence"));
		profiles.add(skillProfile("Lead", "exact:Lead rocks|lead"));
		profiles.add(skillProfile("Nickel", "exact:Nickel rocks|nickel"));
		profiles.add(skillProfile("Ancient essence", "exact:Ancient essence|ancient essence"));
		profiles.add(skillProfile("Custom", miningFarmerTarget));
		return profiles;
	}

	private List<Map<String, Object>> woodcuttingProfiles()
	{
		List<Map<String, Object>> profiles = new ArrayList<>();
		profiles.add(skillProfile("Normal trees", "exact:Tree|exact:Dead tree|exact:Dying tree|exact:Evergreen tree|exact:Jungle tree|tree"));
		profiles.add(skillProfile("Achey", "exact:Achey tree|achey"));
		profiles.add(skillProfile("Oak", "exact:Oak tree|oak"));
		profiles.add(skillProfile("Willow", "exact:Willow tree|willow"));
		profiles.add(skillProfile("Teak", "exact:Teak tree|teak"));
		profiles.add(skillProfile("Maple", "exact:Maple tree|maple"));
		profiles.add(skillProfile("Arctic pine", "exact:Arctic pine tree|arctic pine"));
		profiles.add(skillProfile("Hollow", "exact:Hollow tree|hollow"));
		profiles.add(skillProfile("Mahogany", "exact:Mahogany tree|mahogany"));
		profiles.add(skillProfile("Yew", "exact:Yew tree|yew"));
		profiles.add(skillProfile("Blisterwood", "exact:Blisterwood tree|blisterwood"));
		profiles.add(skillProfile("Camphor", "exact:Camphor tree|camphor"));
		profiles.add(skillProfile("Magic", "exact:Magic tree|magic"));
		profiles.add(skillProfile("Ironwood", "exact:Ironwood tree|ironwood"));
		profiles.add(skillProfile("Redwood", "exact:Redwood tree|redwood"));
		profiles.add(skillProfile("Rosewood", "exact:Rosewood tree|rosewood"));
		profiles.add(skillProfile("Custom", woodcuttingFarmerTarget));
		return profiles;
	}

	private Map<String, Object> skillProfile(String name, String target)
	{
		Map<String, Object> profile = new LinkedHashMap<>();
		profile.put("name", name);
		profile.put("target", target);
		return profile;
	}

	/**
	 * Both skills read their effective scan radius from the same volatile field the
	 * HTTP config endpoint writes to (and that startUp() seeds from persisted config).
	 * Mining previously read straight from {@code config.miningScanRadius()} here,
	 * which meant a WebHelper config POST updated the volatile field but this getter
	 * never saw it — applied values silently reverted on the very next status read.
	 */
	private int getSkillFarmerScanRadius(String skill)
	{
		return "mining".equals(skill) ? miningFarmerScanRadius : woodcuttingFarmerScanRadius;
	}

	private int getSkillFarmerMaxCandidates(String skill)
	{
		return "mining".equals(skill) ? miningFarmerMaxCandidates : woodcuttingFarmerMaxCandidates;
	}

	private void startSkillFarmer(String skill, boolean live)
	{
		if ("mining".equals(skill))
		{
			miningFarmerRunning.set(true);
			miningFarmerLiveMode = live;
		}
		else
		{
			woodcuttingFarmerRunning.set(true);
			woodcuttingFarmerLiveMode = live;
		}
		runSkillFarmerStep(skill, live, "start");
		updateAntiIdleState();
		if (live)
		{
			ensureSkillFarmerRecoveryLoop();
		}
	}

	/**
	 * Starts the dedicated wall-clock recovery thread for Mining/Woodcutting if one
	 * isn't already running. Mirrors startMobFarmer's recovery-loop thread exactly:
	 * sleeps getMobFarmerRecoveryLoopDelayMs() between checks, only acts while NOT
	 * logged in (logged-in stepping stays game-tick driven via onGameTick), and exits
	 * once neither skill farmer is live-running anymore.
	 */
	private void ensureSkillFarmerRecoveryLoop()
	{
		Thread existing = skillFarmerRecoveryThread;
		if (existing != null && existing.isAlive())
		{
			return;
		}
		int generation = skillFarmerRecoveryGeneration.incrementAndGet();
		Thread loopThread = new Thread(() ->
		{
			try
			{
				while (skillFarmerRecoveryGeneration.get() == generation
					&& ((miningFarmerRunning.get() && miningFarmerLiveMode) || (woodcuttingFarmerRunning.get() && woodcuttingFarmerLiveMode)))
				{
					clientThread.invokeLater(() ->
					{
						if (client.getGameState() == GameState.LOGGED_IN)
						{
							return;
						}
						if (miningFarmerRunning.get() && miningFarmerLiveMode)
						{
							reportSkillFarmerNeedsLogin("mining", true);
						}
						if (woodcuttingFarmerRunning.get() && woodcuttingFarmerLiveMode)
						{
							reportSkillFarmerNeedsLogin("woodcutting", true);
						}
					});
					Thread.sleep(getMobFarmerRecoveryLoopDelayMs());
				}
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}, "cv-helper-skill-farmer-recovery");
		loopThread.setDaemon(true);
		skillFarmerRecoveryThread = loopThread;
		loopThread.start();
	}

	private void stopSkillFarmer(String skill)
	{
		if ("mining".equals(skill))
		{
			miningFarmerRunning.set(false);
			miningFarmerLiveMode = false;
		}
		else
		{
			woodcuttingFarmerRunning.set(false);
			woodcuttingFarmerLiveMode = false;
		}
		Map<String, Object> status = getSkillFarmerStatus(skill);
		status.put("currentAction", "stopped");
		setSkillFarmerStatus(skill, status);
		updateAntiIdleState();
	}

	private void runSkillFarmerTick(String skill)
	{
		boolean running = "mining".equals(skill) ? miningFarmerRunning.get() : woodcuttingFarmerRunning.get();
		boolean live = "mining".equals(skill) ? miningFarmerLiveMode : woodcuttingFarmerLiveMode;
		if (running)
		{
			runSkillFarmerStep(skill, live, "game-tick");
		}
	}

	private boolean isActivelyChopping()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return false;
		}
		int animationId = localPlayer.getAnimation();
		for (int id : WOODCUTTING_ANIMATION_IDS)
		{
			if (animationId == id)
			{
				return true;
			}
		}
		return false;
	}

	private boolean isActivelyMining()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return false;
		}
		int animationId = localPlayer.getAnimation();
		for (int id : MINING_ANIMATION_IDS)
		{
			if (animationId == id)
			{
				return true;
			}
		}
		return false;
	}

	private boolean selectedMatchesLastTarget(Map<String, Object> selected, Map<String, Object> last)
	{
		if (selected == null || last == null)
		{
			return false;
		}
		int selectedId = intValue(selected.get("id"), -1);
		int lastId = intValue(last.get("id"), -1);
		if (selectedId < 0 || lastId < 0 || selectedId != lastId)
		{
			return false;
		}
		Map<String, Object> selectedWorld = mapValue(selected.get("worldLocation"));
		Map<String, Object> lastWorld = mapValue(last.get("worldLocation"));
		int selectedX = intValue(selectedWorld.get("x"), Integer.MIN_VALUE);
		int selectedY = intValue(selectedWorld.get("y"), Integer.MIN_VALUE);
		int selectedPlane = intValue(selectedWorld.get("plane"), Integer.MIN_VALUE);
		int lastX = intValue(lastWorld.get("x"), Integer.MIN_VALUE);
		int lastY = intValue(lastWorld.get("y"), Integer.MIN_VALUE);
		int lastPlane = intValue(lastWorld.get("plane"), Integer.MIN_VALUE);
		return selectedX == lastX && selectedY == lastY && selectedPlane == lastPlane;
	}

	/**
	 * Single-scan check for whether a previously selected woodcutting target is still
	 * a valid thing to stick to or click again. Returns {@code null} when valid, or a
	 * completion reason ("object-missing" / "action-unavailable") when not -- mirrors
	 * the mining XP-drop lifecycle (lastCompletedTarget/completionReason) so both
	 * skills expose the same shape of target-lifecycle diagnostics in Debug.
	 */
	private String woodcuttingTargetValidityReason(String skill, String targetText, String action, Map<String, Object> lastTarget)
	{
		if (lastTarget == null)
		{
			return "no-target";
		}
		Map<String, Object> lastWorld = mapValue(lastTarget.get("worldLocation"));
		int lastX = intValue(lastWorld.get("x"), Integer.MIN_VALUE);
		int lastY = intValue(lastWorld.get("y"), Integer.MIN_VALUE);
		int lastPlane = intValue(lastWorld.get("plane"), Integer.MIN_VALUE);
		if (lastX == Integer.MIN_VALUE || lastY == Integer.MIN_VALUE || lastPlane == Integer.MIN_VALUE)
		{
			return "no-target";
		}
		for (Map<String, Object> candidate : collectSkillFarmerObjects(skill, targetText, action, client.getLocalPlayer()))
		{
			if (selectedMatchesLastTarget(candidate, lastTarget))
			{
				return Boolean.TRUE.equals(candidate.get("selectable")) ? null : "action-unavailable";
			}
		}
		return "object-missing";
	}

	/** Record the active woodcutting target as completed/invalidated for diagnostics. */
	private void markWoodcuttingTargetCompleted(String reason)
	{
		Map<String, Object> target = lastSelectedWoodcuttingTarget;
		if (target == null)
		{
			return;
		}
		lastCompletedWoodcuttingTarget = target;
		woodcuttingCompletionReason = reason;
		lastWoodcuttingInvalidationTick = client.getTickCount();
		lastSelectedWoodcuttingTarget = null;
	}

	private boolean miningTargetDepleted(Map<String, Object> lastTarget)
	{
		if (lastTarget == null)
		{
			return false;
		}
		Map<String, Object> lastWorld = mapValue(lastTarget.get("worldLocation"));
		int lastX = intValue(lastWorld.get("x"), Integer.MIN_VALUE);
		int lastY = intValue(lastWorld.get("y"), Integer.MIN_VALUE);
		int lastPlane = intValue(lastWorld.get("plane"), Integer.MIN_VALUE);
		int lastId = intValue(lastTarget.get("id"), -1);
		if (lastX == Integer.MIN_VALUE || lastY == Integer.MIN_VALUE || lastPlane == Integer.MIN_VALUE || lastId < 0)
		{
			return false;
		}
		// Check if the object at the same location now has a different ID (indicating depletion)
		for (Map<String, Object> candidate : collectSkillFarmerObjects("mining", miningFarmerTarget, "Mine", client.getLocalPlayer()))
		{
			Map<String, Object> candidateWorld = mapValue(candidate.get("worldLocation"));
			int candidateX = intValue(candidateWorld.get("x"), Integer.MIN_VALUE);
			int candidateY = intValue(candidateWorld.get("y"), Integer.MIN_VALUE);
			int candidatePlane = intValue(candidateWorld.get("plane"), Integer.MIN_VALUE);
			int candidateId = intValue(candidate.get("id"), -1);
			if (candidateX == lastX && candidateY == lastY && candidatePlane == lastPlane)
			{
				// Same location but different ID means the rock depleted
				return candidateId != lastId;
			}
		}
		// No object found at the location - also considered depleted
		return true;
	}

	/**
	 * Reports needs-login status for a skill farmer and attempts recovery if live.
	 * Shared by runSkillFarmerStep's own not-logged-in branch (game-tick driven, only
	 * reachable while LOGGED_IN -- see onGameTick's early return) AND the dedicated
	 * wall-clock skill-farmer recovery thread (which is what actually drives this
	 * while logged out, mirroring Mob Farmer's recovery-loop thread).
	 */
	private void reportSkillFarmerNeedsLogin(String skill, boolean live)
	{
		Map<String, Object> status = getSkillFarmerStatus(skill);
		boolean recoveryActive = live && tryGenericLoginRecovery();
		status.put("currentAction", recoveryActive ? "login-recovery-active" : "needs-login");
		status.put("lastFailureReason", recoveryActive ? null : "not-logged-in");
		status.put("loginRecoveryActive", recoveryActive);
		setSkillFarmerStatus(skill, status);
	}

	private void runSkillFarmerStep(String skill, boolean live, String source)
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			reportSkillFarmerNeedsLogin(skill, live);
			return;
		}
		Map<String, Object> inventory = inventoryPolicyStatus();
		String target = "mining".equals(skill) ? miningFarmerTarget : woodcuttingFarmerTarget;
		boolean woodcutting = "woodcutting".equals(skill);
		boolean mining = "mining".equals(skill);
		boolean activelyChopping = woodcutting && isActivelyChopping();
		boolean activelyMining = mining && isActivelyMining();
		boolean rockDepleted = mining && lastSelectedMiningTarget != null && miningTargetDepleted(lastSelectedMiningTarget);
		if (rockDepleted)
		{
			// Catches depletion by ANYONE the instant the object's identity changes at
			// the tile -- not just our own mining. Without this, a rock someone else
			// empties keeps being treated as our live target (and, after the round-robin
			// fix, even preferred via tie-break continuity) until we happen to land an
			// unrelated XP drop on a different rock and the mismatch self-corrects.
			markMiningTargetCompleted("object-depleted");
		}
		boolean notActivelyGathering = (woodcutting && !activelyChopping) || (mining && !activelyMining);

		boolean dropPolicyEnabled = config.dropPolicyEnabled();
		CvHelperDropMode dropMode = config.dropPolicyMode();
		int dropThreshold = config.dropPolicyThresholdSlots();
		String dropItems = mining ? config.miningDropItems() : config.dropPolicyItems();
		String dropProtected = config.dropPolicyProtectedItems();
		int dropMaxValue = config.dropPolicyMaxValue();

		InventoryDropService.DropOpportunity opportunity = InventoryDropService.DropOpportunity.NONE;
		boolean inventoryFull = Boolean.TRUE.equals(inventory.get("full"));
		boolean aboveThreshold = intValue(inventory.get("occupiedSlots"), 0) >= dropThreshold;
		if (dropMode == CvHelperDropMode.WHEN_IDLE && woodcutting && !activelyChopping && aboveThreshold)
		{
			opportunity = InventoryDropService.DropOpportunity.NOT_CHOPPING;
		}
		else if (dropMode == CvHelperDropMode.WHEN_IDLE && mining && !activelyMining && aboveThreshold)
		{
			opportunity = InventoryDropService.DropOpportunity.NOT_MINING;
		}
		else if (dropMode == CvHelperDropMode.WHEN_IDLE && mining && rockDepleted && aboveThreshold)
		{
			opportunity = InventoryDropService.DropOpportunity.ROCK_DEPLETED;
		}
		else if (inventoryFull)
		{
			opportunity = InventoryDropService.DropOpportunity.INVENTORY_FULL;
		}
		else if (woodcutting && !activelyChopping && aboveThreshold)
		{
			opportunity = InventoryDropService.DropOpportunity.NOT_CHOPPING;
		}
		else if (mining && !activelyMining && aboveThreshold)
		{
			opportunity = InventoryDropService.DropOpportunity.NOT_MINING;
		}
		else if (mining && rockDepleted && aboveThreshold)
		{
			opportunity = InventoryDropService.DropOpportunity.ROCK_DEPLETED;
		}

		InventoryDropService.DropPolicyStatus dropStatus = inventoryDropService.evaluateDropOpportunity(
			dropPolicyEnabled,
			dropMode,
			dropThreshold,
			dropItems,
			dropProtected,
			dropMaxValue,
			opportunity
		);

		if (dropStatus.decision == InventoryDropService.DropDecision.DROP_ALLOWED && live && !dropStatus.candidates.isEmpty())
		{
			InventoryDropService.DropCandidate candidate = dropStatus.candidates.get(0);
			List<Integer> sameItemSlots = new ArrayList<>();
			for (InventoryDropService.DropCandidate c : dropStatus.candidates)
			{
				if (c.itemId == candidate.itemId)
				{
					sameItemSlots.add(c.slot);
				}
			}
			Map<String, Object> status = getSkillFarmerStatus(skill);
			status.put("currentAction", "dropping");
			status.put("lastFailureReason", null);
			status.put("inventory", inventory);
			status.put("dropPolicy", dropStatus.toMap());
			status.put("dropTarget", candidate.toMap());
			status.put("dropSlots", sameItemSlots.size());
			status.put("droppableRemaining", dropStatus.candidates.size());
			setSkillFarmerStatus(skill, status);
			dropInventorySlots(sameItemSlots, candidate.itemId);
			return;
		}

		if (Boolean.TRUE.equals(inventory.get("full")))
		{
			// For mining, if we have any droppable items, try dropping before stopping
			if (mining && !dropStatus.candidates.isEmpty())
			{
				InventoryDropService.DropCandidate candidate = dropStatus.candidates.get(0);
				List<Integer> sameItemSlots = new ArrayList<>();
				for (InventoryDropService.DropCandidate c : dropStatus.candidates)
				{
					if (c.itemId == candidate.itemId)
					{
						sameItemSlots.add(c.slot);
					}
				}
				Map<String, Object> status = getSkillFarmerStatus(skill);
				status.put("currentAction", "dropping");
				status.put("lastFailureReason", null);
				status.put("inventory", inventory);
				status.put("dropPolicy", dropStatus.toMap());
				status.put("dropTarget", candidate.toMap());
				status.put("dropSlots", sameItemSlots.size());
				status.put("droppableRemaining", dropStatus.candidates.size());
				status.put("dropReason", "inventory-full-forced-drop");
				setSkillFarmerStatus(skill, status);
				dropInventorySlots(sameItemSlots, candidate.itemId);
				return;
			}
			Map<String, Object> status = getSkillFarmerStatus(skill);
			status.put("currentAction", "inventory-full");
			status.put("lastFailureReason", dropStatus.lastFailureReason != null ? dropStatus.lastFailureReason : "inventory-full-no-safe-drops");
			status.put("inventory", inventory);
			status.put("dropPolicy", dropStatus.toMap());
			setSkillFarmerStatus(skill, status);
			if ("mining".equals(skill))
			{
				miningFarmerRunning.set(false);
			}
			else
			{
				woodcuttingFarmerRunning.set(false);
			}
			return;
		}

		String action = "mining".equals(skill) ? "Mine" : "Chop down";
		Map<String, Object> selection = selectSkillFarmerObject(skill, target, action);
		selection.put("source", source);
		selection.put("live", live);
		selection.put("inventory", inventory);
		selection.put("dropPolicy", dropStatus.toMap());
		Map<String, Object> selected = mapValue(selection.get("selected"));
		boolean isWoodcutting = "woodcutting".equals(skill);
		if (selected == null)
		{
			selection.put("currentAction", "no-target");
			setSkillFarmerStatus(skill, selection);
			return;
		}
		boolean isMining = "mining".equals(skill);
		Map<String, Object> previousWoodcuttingTarget = lastSelectedWoodcuttingTarget;
		boolean selectedMatchesLast = isWoodcutting && previousWoodcuttingTarget != null
			&& selectedMatchesLastTarget(selected, previousWoodcuttingTarget);
		// rockDepleted was computed up front (before markMiningTargetCompleted nulled
		// lastSelectedMiningTarget), so it still reflects whether THIS tick is the one
		// where the previous rock turned out to be gone -- recomputing here would
		// always read false since the field has already been cleared by now.
		String woodcuttingInvalidReason = isWoodcutting && previousWoodcuttingTarget != null
			? woodcuttingTargetValidityReason(skill, target, action, previousWoodcuttingTarget)
			: null;
		if (woodcuttingInvalidReason != null)
		{
			markWoodcuttingTargetCompleted(woodcuttingInvalidReason);
		}
		boolean shouldStickToLast = isWoodcutting && config.woodcuttingStickToTarget()
			&& activelyChopping && lastSelectedWoodcuttingTarget != null;
		if (shouldStickToLast && !rockDepleted)
		{
			selected = lastSelectedWoodcuttingTarget;
			selection.put("selected", selected);
			selection.put("decision", "sticking:" + targetLabelForMessage(selected));
			selection.put("stickingToTarget", true);
		}
		selection.put("activelyChopping", activelyChopping);
		selection.put("currentAction", live ? (activelyChopping ? "chopping" : "interacting") : "dry-selected");
		if (rockDepleted)
		{
			selection.put("rockDepleted", true);
			selection.put("decision", "rock-depleted-switching");
		}
		if (woodcuttingInvalidReason != null)
		{
			selection.put("treeInvalidated", true);
			selection.put("decision", "tree-" + woodcuttingInvalidReason + "-switching");
		}
		setSkillFarmerStatus(skill, selection);
		if (!live)
		{
			if (isWoodcutting) lastSelectedWoodcuttingTarget = selected;
			if (isMining) lastSelectedMiningTarget = selected;
			return;
		}
		boolean shouldReclick = !isWoodcutting || !activelyChopping || config.woodcuttingReclickWhenActivelyChopping();
		boolean shouldSkip = isWoodcutting && activelyChopping && !config.woodcuttingReclickWhenActivelyChopping() && selectedMatchesLast && !rockDepleted;
		if (shouldSkip)
		{
			return;
		}
		invokeSkillFarmerObject(skill, action, selected);
		if (isWoodcutting)
		{
			lastSelectedWoodcuttingTarget = selected;
			lastWoodcuttingTargetClickTime = System.currentTimeMillis();
		}
		if (isMining)
		{
			lastSelectedMiningTarget = selected;
			lastMiningTargetClickTime = System.currentTimeMillis();
		}
	}

	private Map<String, Object> selectSkillFarmerObject(String skill, String targetText, String action)
	{
		Player localPlayer = client.getLocalPlayer();
		Map<String, Object> out = new LinkedHashMap<>();
		List<Map<String, Object>> candidates = new ArrayList<>();
		Map<String, Object> best = null;
		int totalCandidates = 0;
		int targetMatches = 0;
		int targetMismatches = 0;
		int matchedReachable = 0;
		int matchedUnreachable = 0;
		int missingAction = 0;
		int noRoute = 0;
		int collisionBlocked = 0;
		int sceneBlocked = 0;
		boolean mining = "mining".equals(skill);
		List<String> rejectedStaleTiles = new ArrayList<>();
		// Tile of whatever we picked LAST tick (still unchanged here -- the lifecycle/
		// completion checks that null it out run after selection, later in
		// runSkillFarmerStep). Used below so equidistant ties prefer continuing the
		// SAME rock/tree we're already mining/chopping rather than hopping between
		// tied neighbors mid-action; rotation to a different tile only happens once
		// the previous one is actually gone (depleted/cooldown/invalidated).
		Map<String, Object> lastTarget = mining ? lastSelectedMiningTarget : lastSelectedWoodcuttingTarget;
		String lastTileKey = lastTarget == null ? null : tileKey(mapValue(lastTarget.get("worldLocation")));
		for (Map<String, Object> candidate : collectSkillFarmerObjects(skill, targetText, action, localPlayer))
		{
			totalCandidates++;
			candidates.add(candidate);
			// Skip tiles that were just mined (XP-drop / depletion cooldown) so we don't
			// immediately re-pick a rock that is depleting and won't yield this tick.
			if (mining && isMiningTileOnCooldown(mapValue(candidate.get("worldLocation"))))
			{
				candidate.put("selectable", false);
				List<String> r = listValue(candidate.get("reasons"));
				r.add("depleted-cooldown");
				candidate.put("reasons", r);
				String tk = tileKey(mapValue(candidate.get("worldLocation")));
				if (tk != null)
				{
					rejectedStaleTiles.add(tk);
				}
				continue;
			}
			// Ore-first guard: if THIS tile's object identity changed since we last saw it a
			// few ticks ago, something was just mined there -- by us or anyone else. This is
			// independent of lastSelectedMiningTarget (which only covers the one rock we were
			// personally clicking), so it catches a neighbor rock someone else just emptied,
			// not only our own. A short recency window avoids false positives from scene
			// reloads or object-array reordering with no real state change.
			if (mining && Boolean.TRUE.equals(candidate.get("selectable")))
			{
				String tk = tileKey(mapValue(candidate.get("worldLocation")));
				if (tk != null)
				{
					String idKey = skill + ":" + tk;
					int currentId = intValue(candidate.get("id"), -1);
					int nowTick = client.getTickCount();
					Integer lastSeenId = skillFarmerTileLastSeenId.get(idKey);
					Integer lastSeenTick = skillFarmerTileLastSeenTick.get(idKey);
					boolean recentHistory = lastSeenTick != null && (nowTick - lastSeenTick) <= MINING_DEPLETED_TILE_COOLDOWN_TICKS * 2;
					if (recentHistory && lastSeenId != null && lastSeenId != currentId)
					{
						candidate.put("selectable", false);
						List<String> r = listValue(candidate.get("reasons"));
						r.add("stale-tile-id-changed");
						candidate.put("reasons", r);
						miningDepletedTileUntilTick.put(tk, nowTick + MINING_DEPLETED_TILE_COOLDOWN_TICKS);
						rejectedStaleTiles.add(tk);
					}
					skillFarmerTileLastSeenId.put(idKey, currentId);
					skillFarmerTileLastSeenTick.put(idKey, nowTick);
				}
			}
			boolean targetMatched = Boolean.TRUE.equals(candidate.get("targetMatched"));
			boolean actionMatched = Boolean.TRUE.equals(candidate.get("actionMatched"));
			boolean visible = Boolean.TRUE.equals(candidate.get("visible"));
			boolean reachable = Boolean.TRUE.equals(candidate.get("reachable"));
			List<String> reasons = listValue(candidate.get("reasons"));
			if (targetMatched)
			{
				targetMatches++;
			}
			else
			{
				targetMismatches++;
			}
			if (targetMatched && actionMatched && visible)
			{
				if (reachable)
				{
					matchedReachable++;
				}
				else
				{
					matchedUnreachable++;
				}
			}
			if (!actionMatched)
			{
				missingAction++;
			}
			for (String reason : reasons)
			{
				if (reason.startsWith("unreachable:"))
				{
					noRoute++;
					if (reason.contains("collision-blocked"))
					{
						collisionBlocked++;
					}
					if (reason.contains("scene-blocked"))
					{
						sceneBlocked++;
					}
				}
			}
			if (!Boolean.TRUE.equals(candidate.get("selectable")))
			{
				continue;
			}
			int candidateDistance = intValue(candidate.get("pathDistance"), Integer.MAX_VALUE);
			int bestDistance = best == null ? Integer.MAX_VALUE : intValue(best.get("pathDistance"), Integer.MAX_VALUE);
			if (best == null || candidateDistance < bestDistance)
			{
				best = candidate;
			}
			else if (candidateDistance == bestDistance)
			{
				String bestTile = tileKey(mapValue(best.get("worldLocation")));
				String candidateTile = tileKey(mapValue(candidate.get("worldLocation")));
				boolean candidateIsLast = lastTileKey != null && lastTileKey.equals(candidateTile);
				boolean bestIsLast = lastTileKey != null && lastTileKey.equals(bestTile);
				if (candidateIsLast && !bestIsLast)
				{
					// Keep mining/chopping the same tile we were already on instead of
					// hopping to an equally-close neighbor mid-action.
					best = candidate;
				}
				else if (!candidateIsLast && !bestIsLast)
				{
					// Neither tied candidate is the one we were just on -- the previous
					// target is gone (depleted/cooldown/invalidated), so rotate to
					// whichever tile was used longest ago (or never) instead of always
					// resolving to the same scan-order winner.
					String bestKey = skill + ":" + bestTile;
					String candidateKey = skill + ":" + candidateTile;
					int bestLastUsed = skillFarmerTileLastUsedTick.getOrDefault(bestKey, -1);
					int candidateLastUsed = skillFarmerTileLastUsedTick.getOrDefault(candidateKey, -1);
					if (candidateLastUsed < bestLastUsed)
					{
						best = candidate;
					}
				}
				// else bestIsLast && !candidateIsLast: keep best, already continuing correctly.
			}
		}
		if (best != null)
		{
			String bestTileKey = tileKey(mapValue(best.get("worldLocation")));
			if (bestTileKey != null)
			{
				skillFarmerTileLastUsedTick.put(skill + ":" + bestTileKey, client.getTickCount());
			}
		}
		int maxCandidates = getSkillFarmerMaxCandidates(skill);
		// maxCandidates never hides objects from the response -- collectSkillFarmerObjects()
		// already evaluates every reachable object within scan radius (bounded only by the
		// hard safety cap), so a small maxCandidates can't hide the actual best target behind
		// nearer stale ones. It's exposed here purely as a per-candidate priority flag: the
		// frontend renders the nearest maxCandidates in full color on the minimap and table,
		// and the rest (still real, still shown) in a deemphasized style instead of omitting
		// them -- the grid stays exhaustive regardless of how low maxCandidates is set.
		for (int i = 0; i < candidates.size(); i++)
		{
			candidates.get(i).put("withinMaxCandidates", i < maxCandidates);
		}
		if (best != null)
		{
			best.put("withinMaxCandidates", true);
		}
		out.put("candidates", candidates);
		out.put("selected", best);
		out.put("scanRadiusTiles", getSkillFarmerScanRadius(skill));
		out.put("maxCandidates", maxCandidates);
		out.put("decision", best == null ? "no-valid-target" : "selected:" + targetLabelForMessage(best));
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("totalCandidates", totalCandidates);
		summary.put("targetMatches", targetMatches);
		summary.put("targetMismatches", targetMismatches);
		summary.put("matchedReachable", matchedReachable);
		summary.put("matchedUnreachable", matchedUnreachable);
		summary.put("missingAction", missingAction);
		summary.put("noRoute", noRoute);
		summary.put("collisionBlocked", collisionBlocked);
		summary.put("sceneBlocked", sceneBlocked);
		summary.put("staleRejected", rejectedStaleTiles.size());
		out.put("candidateSummary", summary);
		out.put("rejectedStaleTiles", rejectedStaleTiles);
		// Same shape for both skills: lastCompletedTarget/completionReason/lastInvalidationTick.
		// Mining's source is the XP-drop handler; Woodcutting's is the per-tick validity check.
		if (mining)
		{
			out.put("lastCompletedTarget", lastCompletedMiningTarget == null ? null : targetLabelForMessage(lastCompletedMiningTarget));
			out.put("completionReason", miningCompletionReason);
			out.put("lastInvalidationTick", lastMiningInvalidationTick);
		}
		else
		{
			out.put("lastCompletedTarget", lastCompletedWoodcuttingTarget == null ? null : targetLabelForMessage(lastCompletedWoodcuttingTarget));
			out.put("completionReason", woodcuttingCompletionReason);
			out.put("lastInvalidationTick", lastWoodcuttingInvalidationTick);
		}
		out.put("currentTick", client.getTickCount());
		return out;
	}

	private List<String> listValue(Object value)
	{
		if (value instanceof List)
		{
			List<?> list = (List<?>) value;
			List<String> result = new ArrayList<>(list.size());
			for (Object item : list)
			{
				result.add(String.valueOf(item));
			}
			return result;
		}
		return new ArrayList<>();
	}

	private List<Map<String, Object>> collectSkillFarmerObjects(String skill, String targetText, String action, Player localPlayer)
	{
		List<Map<String, Object>> objects = new ArrayList<>();
		WorldView worldView = localPlayer == null ? client.getTopLevelWorldView() : localPlayer.getWorldView();
		if (worldView == null || worldView.getScene() == null)
		{
			return objects;
		}
		WorldPoint origin = localPlayer == null ? null : localPlayer.getWorldLocation();
		int scanRadius = getSkillFarmerScanRadius(skill);
		Tile[][][] tiles = worldView.getScene().getTiles();
		int plane = Math.max(0, Math.min(worldView.getPlane(), tiles.length - 1));
		int minX = 0;
		int maxX = Math.min(worldView.getSizeX(), tiles[plane].length) - 1;
		int minY = 0;
		int maxY = worldView.getSizeY() - 1;
		if (origin != null)
		{
			LocalPoint localOrigin = LocalPoint.fromWorld(worldView, origin);
			if (localOrigin != null)
			{
				int sceneX = localOrigin.getSceneX();
				int sceneY = localOrigin.getSceneY();
				minX = Math.max(0, sceneX - scanRadius);
				maxX = Math.min(maxX, sceneX + scanRadius);
				minY = Math.max(0, sceneY - scanRadius);
				maxY = Math.min(maxY, sceneY + scanRadius);
			}
		}
		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; tiles[plane][x] != null && y <= Math.min(maxY, tiles[plane][x].length - 1); y++)
			{
				Tile tile = tiles[plane][x][y];
				if (tile == null)
				{
					continue;
				}
				if (origin != null && tile.getWorldLocation() != null && origin.distanceTo(tile.getWorldLocation()) > scanRadius)
				{
					continue;
				}
				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects != null)
				{
					for (GameObject object : gameObjects)
					{
						Map<String, Object> candidate = skillFarmerObjectTarget(skill, object, targetText, action, localPlayer);
						if (candidate != null)
						{
							objects.add(candidate);
						}
					}
				}
				addSkillFarmerCandidate(objects, skillFarmerObjectTarget(skill, tile.getWallObject(), targetText, action, localPlayer));
				addSkillFarmerCandidate(objects, skillFarmerObjectTarget(skill, tile.getDecorativeObject(), targetText, action, localPlayer));
				addSkillFarmerCandidate(objects, skillFarmerObjectTarget(skill, tile.getGroundObject(), targetText, action, localPlayer));
			}
		}
		objects = deduplicateSkillFarmerCandidates(objects);
		objects.sort((left, right) -> Integer.compare(intValue(left.get("distance"), Integer.MAX_VALUE), intValue(right.get("distance"), Integer.MAX_VALUE)));
		// Every reachable object within the scan radius must be evaluated so the best
		// valid target is never hidden behind nearer stale/depleted/unreachable ones;
		// maxCandidates is a display cap applied later in selectSkillFarmerObject, not
		// an evaluation cap here. SKILL_FARMER_HARD_CAP is just a CPU/memory safety
		// valve for pathological scan radii, well above any realistic maxCandidates.
		return objects.size() > SKILL_FARMER_HARD_CAP ? new ArrayList<>(objects.subList(0, SKILL_FARMER_HARD_CAP)) : objects;
	}

	/**
	 * Every named object within radius of the player, with no target/action filter --
	 * the general "scene" lens for the standalone minimap/grid view. Unlike the skill
	 * farmer scans this runs on demand from an HTTP request, independent of whether
	 * any farmer is running, and independent of any farmer's configured scan radius
	 * (the caller supplies it directly so the grid's own radius control works without
	 * needing a farmer active at all).
	 */
	private List<Map<String, Object>> collectSceneObjects(Player localPlayer, int radius)
	{
		List<Map<String, Object>> objects = new ArrayList<>();
		// No player (e.g. at the login screen) -- without an origin the bounding box
		// below would never get narrowed to the requested radius and would instead
		// scan the entire loaded scene, which is both meaningless (no "nearby" without
		// a player) and was hanging the request past its timeout. Bail out instead.
		if (localPlayer == null)
		{
			return objects;
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null || worldView.getScene() == null)
		{
			return objects;
		}
		WorldPoint origin = localPlayer.getWorldLocation();
		int clampedRadius = Math.max(1, Math.min(30, radius));
		Tile[][][] tiles = worldView.getScene().getTiles();
		int plane = Math.max(0, Math.min(worldView.getPlane(), tiles.length - 1));
		int minX = 0;
		int maxX = Math.min(worldView.getSizeX(), tiles[plane].length) - 1;
		int minY = 0;
		int maxY = worldView.getSizeY() - 1;
		if (origin != null)
		{
			LocalPoint localOrigin = LocalPoint.fromWorld(worldView, origin);
			if (localOrigin != null)
			{
				int sceneX = localOrigin.getSceneX();
				int sceneY = localOrigin.getSceneY();
				minX = Math.max(0, sceneX - clampedRadius);
				maxX = Math.min(maxX, sceneX + clampedRadius);
				minY = Math.max(0, sceneY - clampedRadius);
				maxY = Math.min(maxY, sceneY + clampedRadius);
			}
		}
		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; tiles[plane][x] != null && y <= Math.min(maxY, tiles[plane][x].length - 1); y++)
			{
				Tile tile = tiles[plane][x][y];
				if (tile == null)
				{
					continue;
				}
				if (origin != null && tile.getWorldLocation() != null && origin.distanceTo(tile.getWorldLocation()) > clampedRadius)
				{
					continue;
				}
				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects != null)
				{
					for (GameObject object : gameObjects)
					{
						addSkillFarmerCandidate(objects, sceneObjectInfo(object, localPlayer));
					}
				}
				addSkillFarmerCandidate(objects, sceneObjectInfo(tile.getWallObject(), localPlayer));
				addSkillFarmerCandidate(objects, sceneObjectInfo(tile.getDecorativeObject(), localPlayer));
				addSkillFarmerCandidate(objects, sceneObjectInfo(tile.getGroundObject(), localPlayer));
			}
		}
		objects = deduplicateSkillFarmerCandidates(objects);
		objects.sort((left, right) -> Integer.compare(intValue(left.get("distance"), Integer.MAX_VALUE), intValue(right.get("distance"), Integer.MAX_VALUE)));
		return objects.size() > SKILL_FARMER_HARD_CAP ? new ArrayList<>(objects.subList(0, SKILL_FARMER_HARD_CAP)) : objects;
	}

	/** Lightweight per-object info for the general scene scan -- no target/action matching. */
	private Map<String, Object> sceneObjectInfo(TileObject object, Player localPlayer)
	{
		if (object == null || object.getId() <= 0)
		{
			return null;
		}
		ObjectComposition composition = safeValue(() -> client.getObjectDefinition(object.getId()), null);
		ObjectComposition impostor = composition == null ? null : safeValue(composition::getImpostor, null);
		if (impostor != null)
		{
			composition = impostor;
		}
		String name = composition == null ? "" : cleanWidgetText(composition.getName());
		if (name.isEmpty() || "null".equals(name))
		{
			return null;
		}
		net.runelite.api.Point canvasLocation = safeValue(object::getCanvasLocation, null);
		Map<String, Object> canvasBounds = tileObjectCanvasBounds(object);
		boolean visible = canvasBounds != null || canvasPointVisible(canvasLocation);
		int sizeX = 1;
		int sizeY = 1;
		Map<String, Object> sceneMin = pointValue(object.getLocalLocation() == null ? null : new Point(object.getLocalLocation().getSceneX(), object.getLocalLocation().getSceneY()));
		Map<String, Object> sceneMax = sceneMin;
		if (object instanceof GameObject)
		{
			GameObject gameObject = (GameObject) object;
			sizeX = Math.max(1, gameObject.sizeX());
			sizeY = Math.max(1, gameObject.sizeY());
			sceneMin = pointValue(gameObject.getSceneMinLocation());
			sceneMax = pointValue(gameObject.getSceneMaxLocation());
		}
		else if (composition != null)
		{
			sizeX = Math.max(1, composition.getSizeX());
			sizeY = Math.max(1, composition.getSizeY());
		}
		int distance = localPlayer == null || object.getWorldLocation() == null ? Integer.MAX_VALUE : localPlayer.getWorldLocation().distanceTo(object.getWorldLocation());
		WorldArea footprint = buildObjectFootprint(object, localPlayer == null ? null : localPlayer.getWorldView(), sizeX, sizeY);
		boolean reachable = false;
		Integer pathDistance = null;
		if (visible && localPlayer != null)
		{
			InteractionPathingResult pathing = pathfinding.pathDistanceToInteractionArea(localPlayer, footprint, SKILL_FARMER_PATH_SEARCH_TILES);
			reachable = pathing.reachable;
			pathDistance = pathing.reachable ? pathing.pathDistance : null;
		}
		List<String> actionList = new ArrayList<>();
		String[] actions = composition == null ? null : composition.getActions();
		if (actions != null)
		{
			for (String a : actions)
			{
				if (a != null)
				{
					actionList.add(a);
				}
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("surface", "scene");
		out.put("objectType", object.getClass().getSimpleName());
		out.put("name", name);
		out.put("label", name);
		out.put("id", object.getId());
		out.put("distance", distance);
		out.put("worldLocation", pointValue(object.getWorldLocation()));
		out.put("sceneMinLocation", sceneMin);
		out.put("sceneMaxLocation", sceneMax);
		out.put("objectFootprint", footprintMap(footprint));
		out.put("objectSize", footprintMap(footprint));
		out.put("bounds", canvasBounds);
		out.put("visible", visible);
		out.put("actions", actionList);
		out.put("reachable", reachable);
		out.put("pathDistance", pathDistance);
		out.put("selectable", visible && reachable);
		out.put("targetMatched", true);
		out.put("reasons", new ArrayList<String>());
		return out;
	}

	private void addSkillFarmerCandidate(List<Map<String, Object>> objects, Map<String, Object> candidate)
	{
		if (candidate != null)
		{
			objects.add(candidate);
		}
	}

	private List<Map<String, Object>> deduplicateSkillFarmerCandidates(List<Map<String, Object>> objects)
	{
		Map<String, Map<String, Object>> seen = new LinkedHashMap<>();
		for (Map<String, Object> candidate : objects)
		{
			String skill = String.valueOf(candidate.get("skill"));
			int id = intValue(candidate.get("id"), -1);
			Map<String, Object> worldLocation = mapValue(candidate.get("worldLocation"));
			String objectType = String.valueOf(candidate.get("objectType"));
			String key = skill + ":" + id + ":" + (worldLocation == null ? "" : worldLocation.get("x") + "," + worldLocation.get("y") + "," + worldLocation.get("plane")) + ":" + objectType;
			if (!seen.containsKey(key))
			{
				seen.put(key, candidate);
			}
		}
		return new ArrayList<>(seen.values());
	}

	private Map<String, Object> skillFarmerObjectTarget(String skill, TileObject object, String targetText, String action, Player localPlayer)
	{
		if (object == null || object.getId() <= 0)
		{
			return null;
		}
		ObjectComposition composition = safeValue(() -> client.getObjectDefinition(object.getId()), null);
		ObjectComposition impostor = composition == null ? null : safeValue(composition::getImpostor, null);
		if (impostor != null)
		{
			composition = impostor;
		}
		String name = composition == null ? "" : cleanWidgetText(composition.getName());
		if (name.isEmpty() || "null".equals(name))
		{
			return null;
		}
		Map<String, String> matchInfo = matchesTargetTextWithInfo(name, object.getId(), targetText);
		boolean targetMatch = matchInfo != null;
		int actionIndex = objectActionIndex(composition, action);
		boolean actionMatch = actionIndex >= 0;
		if (!targetMatch && !actionMatch)
		{
			return null;
		}
		net.runelite.api.Point canvasLocation = safeValue(object::getCanvasLocation, null);
		Map<String, Object> canvasBounds = tileObjectCanvasBounds(object);
		boolean visible = canvasBounds != null || canvasPointVisible(canvasLocation);
		int sizeX = 1;
		int sizeY = 1;
		Map<String, Object> sceneMin = pointValue(object.getLocalLocation() == null ? null : new Point(object.getLocalLocation().getSceneX(), object.getLocalLocation().getSceneY()));
		Map<String, Object> sceneMax = sceneMin;
		if (object instanceof GameObject)
		{
			GameObject gameObject = (GameObject) object;
			sizeX = Math.max(1, gameObject.sizeX());
			sizeY = Math.max(1, gameObject.sizeY());
			sceneMin = pointValue(gameObject.getSceneMinLocation());
			sceneMax = pointValue(gameObject.getSceneMaxLocation());
		}
		else if (composition != null)
		{
			sizeX = Math.max(1, composition.getSizeX());
			sizeY = Math.max(1, composition.getSizeY());
		}
		int distance = localPlayer == null || object.getWorldLocation() == null ? Integer.MAX_VALUE : localPlayer.getWorldLocation().distanceTo(object.getWorldLocation());
		List<String> reasons = new ArrayList<>();
		if (!targetMatch)
		{
			reasons.add("target-mismatch");
		}
		if (!actionMatch)
		{
			reasons.add("missing-action:" + action);
		}
		if (!visible)
		{
			reasons.add("not-visible");
		}
		Map<String, Object> target = new LinkedHashMap<>();
		WorldArea objectFootprint = buildObjectFootprint(object, localPlayer == null ? null : localPlayer.getWorldView(), sizeX, sizeY);
		target.put("skill", skill);
		target.put("surface", skill);
		target.put("objectType", object.getClass().getSimpleName());
		target.put("label", name);
		target.put("name", name);
		target.put("id", object.getId());
		target.put("distance", distance);
		target.put("playerWorldLocation", localPlayer == null ? null : pointValue(localPlayer.getWorldLocation()));
		target.put("worldLocation", pointValue(object.getWorldLocation()));
		target.put("localLocation", pointValue(object.getLocalLocation()));
		target.put("sceneMinLocation", sceneMin);
		target.put("sceneMaxLocation", sceneMax);
		target.put("objectOrigin", pointValue(object.getWorldLocation()));
		target.put("objectSize", footprintMap(objectFootprint));
		target.put("objectFootprint", footprintMap(objectFootprint));
		target.put("bounds", canvasBounds);
		target.put("canvasBounds", canvasBounds);
		target.put("clickPoint", pointValue(canvasLocation));
		target.put("visible", visible);
		target.put("action", action);
		target.put("actions", composition == null || composition.getActions() == null ? new String[0] : composition.getActions());
		target.put("actionIndex", actionIndex);
		MenuAction menuAction = gameObjectMenuActionForIndex(actionIndex);
		target.put("menuAction", menuAction == null ? null : menuAction.name());
		target.put("targetMatched", targetMatch);
		target.put("actionMatched", actionMatch);
		if (matchInfo != null)
		{
			target.put("matchedToken", matchInfo.get("token"));
			target.put("matchType", matchInfo.get("type"));
		}
		target.put("targetText", targetText);
		if (targetMatch && actionMatch && visible)
		{
			InteractionPathingResult pathing = pathfinding.pathDistanceToInteractionArea(localPlayer, objectFootprint, Math.max(SKILL_FARMER_PATH_SEARCH_TILES, getSkillFarmerScanRadius(skill)));
			target.put("reachable", pathing.reachable);
			target.put("pathDistance", pathing.reachable ? pathing.pathDistance : null);
			target.put("interactionPathDistance", pathing.reachable ? pathing.pathDistance : null);
			target.put("interactionReachable", pathing.reachable);
			target.put("interactionTile", pointValue(pathing.interactionTile));
			target.put("evaluatedInteractionTiles", pathing.evaluatedInteractionTiles);
			target.put("walkableInteractionTiles", pathing.walkableInteractionTiles);
			target.put("blockedInteractionTiles", pathing.blockedInteractionTiles);
			target.put("blockedByCollision", pathing.blockedByCollision);
			target.put("blockedByScene", pathing.blockedByScene);
			target.put("pathSearchLimit", pathing.searchLimit);
			target.put("pathVisited", pathing.visited);
			target.put("pathFailureReason", pathing.failureReason);
			target.put("doorTransition", pathing.doorTransition);
			target.put("blockedByDoor", pathing.blockedByDoor);
			target.put("manualActionRequired", pathing.manualActionRequired);
			target.put("manualActionReason", pathing.manualActionReason);
			target.put("blockingDoor", pathing.blockingDoor);
			if (!pathing.reachable)
			{
				reasons.add(pathing.manualActionRequired ? "manual-action-required:" + pathing.manualActionReason : "unreachable:" + pathing.failureReason);
			}
		}
		else
		{
			target.put("reachable", false);
			target.put("pathDistance", null);
			target.put("interactionPathDistance", null);
			target.put("interactionReachable", false);
			target.put("interactionTile", null);
			target.put("evaluatedInteractionTiles", 0);
			target.put("walkableInteractionTiles", 0);
			target.put("blockedInteractionTiles", 0);
			target.put("blockedByCollision", 0);
			target.put("blockedByScene", 0);
			target.put("pathSearchLimit", 0);
			target.put("pathVisited", 0);
			target.put("pathFailureReason", reasons.isEmpty() ? null : "not-pathing:" + String.join(",", reasons));
		}
		target.put("selectable", reasons.isEmpty());
		target.put("reasons", reasons);
		return target;
	}

	private boolean matchesTargetText(String name, int id, String targetText)
	{
		return matchesTargetTextWithInfo(name, id, targetText) != null;
	}

	private Map<String, String> matchesTargetTextWithInfo(String name, int id, String targetText)
	{
		for (String token : actionTargetCandidates(targetText))
		{
			String trimmed = token.trim();
			if (trimmed.toLowerCase().startsWith("id:"))
			{
				if (String.valueOf(id).equals(trimmed.substring(3).trim()))
				{
					Map<String, String> info = new LinkedHashMap<>();
					info.put("token", trimmed);
					info.put("type", "id");
					return info;
				}
				continue;
			}
			if (trimmed.toLowerCase().startsWith("exact:"))
			{
				String exactPattern = trimmed.substring(6).trim();
				if (!exactPattern.isEmpty() && normalize(name).equals(normalize(exactPattern)))
				{
					Map<String, String> info = new LinkedHashMap<>();
					info.put("token", trimmed);
					info.put("type", "exact");
					return info;
				}
				continue;
			}
			if (!trimmed.isEmpty() && normalize(name).contains(normalize(trimmed)))
			{
				Map<String, String> info = new LinkedHashMap<>();
				info.put("token", trimmed);
				info.put("type", "contains");
				return info;
			}
		}
		return null;
	}

	private int objectActionIndex(ObjectComposition composition, String action)
	{
		if (composition == null || composition.getActions() == null)
		{
			return -1;
		}
		String[] actions = composition.getActions();
		for (int i = 0; i < actions.length; i++)
		{
			if (action.equalsIgnoreCase(cleanWidgetText(actions[i])))
			{
				return i;
			}
		}
		return -1;
	}

	private MenuAction gameObjectMenuActionForIndex(int actionIndex)
	{
		switch (actionIndex)
		{
			case 0:
				return MenuAction.GAME_OBJECT_FIRST_OPTION;
			case 1:
				return MenuAction.GAME_OBJECT_SECOND_OPTION;
			case 2:
				return MenuAction.GAME_OBJECT_THIRD_OPTION;
			case 3:
				return MenuAction.GAME_OBJECT_FOURTH_OPTION;
			case 4:
				return MenuAction.GAME_OBJECT_FIFTH_OPTION;
			default:
				return null;
		}
	}

	private Map<String, Object> tileObjectCanvasBounds(TileObject object)
	{
		if (object == null)
		{
			return null;
		}
		Shape clickbox = safeValue(object::getClickbox, null);
		if (clickbox != null)
		{
			Rectangle bounds = clickbox.getBounds();
			if (bounds.width > 0 && bounds.height > 0)
			{
				return boundsMap(bounds);
			}
		}
		Polygon tilePoly = safeValue(object::getCanvasTilePoly, null);
		if (tilePoly != null)
		{
			Rectangle bounds = tilePoly.getBounds();
			if (bounds.width > 0 && bounds.height > 0)
			{
				return boundsMap(bounds);
			}
		}
		return null;
	}

	private boolean canvasPointVisible(net.runelite.api.Point point)
	{
		if (point == null)
		{
			return false;
		}
		int width = safeValue(client::getCanvasWidth, 0);
		int height = safeValue(client::getCanvasHeight, 0);
		return point.getX() >= 0 && point.getY() >= 0 && (width <= 0 || point.getX() <= width) && (height <= 0 || point.getY() <= height);
	}

	private void invokeSkillFarmerObject(String skill, String action, Map<String, Object> target)
	{
		if (target == null || !actionInProgress.compareAndSet(false, true))
		{
			return;
		}
		try
		{
			Map<String, Object> scene = mapValue(target.get("sceneMinLocation"));
			int sceneX = intValue(scene.get("x"), Integer.MIN_VALUE);
			int sceneY = intValue(scene.get("y"), Integer.MIN_VALUE);
			int id = intValue(target.get("id"), -1);
			if (sceneX == Integer.MIN_VALUE || sceneY == Integer.MIN_VALUE || id < 0)
			{
				return;
			}
			MenuAction menuAction = menuActionFromName(String.valueOf(target.get("menuAction")), MenuAction.GAME_OBJECT_FIRST_OPTION);
			client.menuAction(sceneX, sceneY, menuAction, id, -1, action, targetLabelForMessage(target));
		}
		finally
		{
			actionInProgress.set(false);
		}
	}

	private boolean dropInventorySlot(int slotIndex, int itemId)
	{
		return dropInventorySlots(java.util.Collections.singletonList(slotIndex), itemId);
	}

	private boolean dropInventorySlots(List<Integer> slotIndices, int itemId)
	{
		if (slotIndices == null || slotIndices.isEmpty())
		{
			return false;
		}
		if (!actionInProgress.compareAndSet(false, true))
		{
			return false;
		}
		try
		{
			clientThread.invokeLater(() ->
			{
				try
				{
					ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
					if (inventory == null)
					{
						return;
					}
					int dropped = 0;
					for (int slotIndex : slotIndices)
					{
						if (slotIndex < 0 || slotIndex >= inventory.size())
						{
							continue;
						}
						Item item = inventory.getItem(slotIndex);
						if (item == null || item.getId() != itemId)
						{
							continue;
						}
						Map<String, Object> target = inventoryTargetForSlot(slotIndex, itemId);
						if (target == null)
						{
							continue;
						}
						InventoryMenuAction menu = inventoryMenuAction(target, "Drop");
						if (menu == null)
						{
							log.warn("CV Helper no Drop menu action for slot {} item {}", slotIndex, itemId);
							continue;
						}
						log.info(
							"CV DROP: slot={} itemId={} parentId={} widgetId={} opIndex={} componentOpId={} param0={} param1={} menuAction={} identifier={} option={}",
							slotIndex,
							itemId,
							target.get("parentId"),
							target.get("widgetId"),
							menu.opIndex,
							menu.componentOpId,
							menu.param0,
							menu.param1,
							menu.menuAction,
							menu.identifier,
							menu.option
						);
						client.menuAction(menu.param0, menu.param1, menu.menuAction, menu.componentOpId, menu.itemId, menu.option, "");
						dropped++;
					}
					log.info("CV DROP batch: dropped {} slots for itemId {}", dropped, itemId);
				}
				finally
				{
					actionInProgress.set(false);
				}
			});
			return true;
		}
		catch (Exception e)
		{
			actionInProgress.set(false);
			return false;
		}
	}

	private Map<String, Object> inventoryTargetForSlot(int slotIndex, int itemId)
	{
		List<Map<String, Object>> targets = getLiveInventoryTargets();
		for (Map<String, Object> t : targets)
		{
			if ("inventory".equals(t.get("surface")) && intValue(t.get("index"), -1) == slotIndex)
			{
				Object targetItemId = t.get("itemId");
				if (targetItemId instanceof Number && ((Number) targetItemId).intValue() == itemId)
				{
					return t;
				}
			}
		}
		return null;
	}

	private MenuAction menuActionFromName(String name, MenuAction fallback)
	{
		if (name == null || name.isEmpty() || "null".equals(name))
		{
			return fallback;
		}
		try
		{
			return MenuAction.valueOf(name);
		}
		catch (IllegalArgumentException e)
		{
			return fallback;
		}
	}

	private void setSkillFarmerStatus(String skill, Map<String, Object> status)
	{
		status.put("updatedAt", Instant.now().toString());
		if ("mining".equals(skill))
		{
			lastMiningFarmerStatus = status;
		}
		else
		{
			lastWoodcuttingFarmerStatus = status;
		}
	}

	private Map<String, Object> inventoryPolicyStatus()
	{
		Map<String, Object> inventory = containerValue("inventory", InventoryID.INVENTORY);
		int slotCount = intValue(inventory.get("slotCount"), 28);
		int occupied = intValue(inventory.get("occupiedSlots"), 0);
		inventory.put("freeSlots", Math.max(0, slotCount - occupied));
		inventory.put("full", occupied >= slotCount && slotCount > 0);
		inventory.put("neverDropItems", getMobFarmerNeverDropItems());
		inventory.put("dropCandidate", lowestSafeDropCandidate(inventory));
		inventory.put("safeDroppableSlots", safeDroppableSlots(inventory));
		inventory.put("protectedSlots", protectedSlots(inventory));
		inventory.put("rejectedSlots", rejectedSlots(inventory));
		return inventory;
	}

	private Map<String, Object> lowestSafeDropCandidate(Map<String, Object> inventory)
	{
		Object itemsValue = inventory.get("items");
		if (!(itemsValue instanceof List))
		{
			return null;
		}
		Map<String, Object> best = null;
		long bestValue = Long.MAX_VALUE;
		for (Object itemValue : (List<?>) itemsValue)
		{
			if (!(itemValue instanceof Map))
			{
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) itemValue;
			String name = String.valueOf(item.get("name"));
			int itemId = intValue(item.get("id"), -1);
			if (itemSafetyService.isProtectedItem(name, itemId, getMobFarmerNeverDropItems()))
			{
				continue;
			}
			long value = longValue(item.get("gePrice"));
			if (best == null || value < bestValue)
			{
				best = item;
				bestValue = value;
			}
		}
		return best;
	}

	private List<Map<String, Object>> highAlchCandidates()
	{
		List<Map<String, Object>> candidates = new ArrayList<>();
		Map<String, Object> inventory = containerValue("inventory", InventoryID.INVENTORY);
		Object itemsValue = inventory.get("items");
		if (!(itemsValue instanceof List))
		{
			return candidates;
		}
		for (Object itemValue : (List<?>) itemsValue)
		{
			if (!(itemValue instanceof Map))
			{
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) itemValue;
			Map<String, Object> candidate = highAlchCandidateStatus(item);
			if (candidate != null)
			{
				candidates.add(candidate);
			}
		}
		candidates.sort((left, right) -> Long.compare(longValue(right.get("deltaEach")), longValue(left.get("deltaEach"))));
		return candidates;
	}

	private Map<String, Object> highAlchCandidateStatus(Map<String, Object> item)
	{
		String name = String.valueOf(item.get("name"));
		int itemId = intValue(item.get("id"), -1);
		int quantity = intValue(item.get("quantity"), 1);
		long geEach = longValue(item.get("gePriceEach"));
		long haEach = longValue(item.get("haPriceEach"));
		long deltaEach = haEach - geEach;
		List<String> reasons = new ArrayList<>();
		boolean allowlistEmpty = getMobFarmerHighAlchItems().trim().isEmpty();
		if (!getMobFarmerHighAlchEnabled())
		{
			reasons.add("disabled");
		}
		if (!allowlistEmpty && !matchesItemPolicy(name, itemId, quantity, getMobFarmerHighAlchItems()))
		{
			reasons.add("not-allowlisted");
		}
		if (matchesItemPolicy(name, itemId, quantity, getMobFarmerHighAlchBlacklist()))
		{
			reasons.add("blacklisted");
		}
		if (isMobFarmerProtectedInventoryItem(name, itemId, quantity))
		{
			reasons.add("protected-item");
		}
		if (haEach <= 0)
		{
			reasons.add("no-ha-value");
		}
		if (haEach < getMobFarmerHighAlchMinHa())
		{
			reasons.add("below-min-ha:" + haEach + "<" + getMobFarmerHighAlchMinHa());
		}
		if (deltaEach < getMobFarmerHighAlchMinDelta())
		{
			reasons.add("below-min-delta:" + deltaEach + "<" + getMobFarmerHighAlchMinDelta());
		}
		if (deltaEach < 0 && Math.abs(deltaEach) > getMobFarmerHighAlchMaxLoss())
		{
			reasons.add("loss-too-high:" + Math.abs(deltaEach) + ">" + getMobFarmerHighAlchMaxLoss());
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("slot", item.get("slot"));
		out.put("id", itemId);
		out.put("name", name);
		out.put("quantity", quantity);
		out.put("geEach", geEach);
		out.put("haEach", haEach);
		out.put("deltaEach", deltaEach);
		out.put("totalGe", longValue(item.get("gePrice")));
		out.put("totalHa", longValue(item.get("haPrice")));
		out.put("eligible", reasons.isEmpty());
		out.put("reasons", reasons);
		out.put("invocation", "not-invoked-this-pass");
		return out;
	}

	private boolean inventoryCanAcceptItem(int itemId, boolean stackable)
	{
		ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);
		Item[] items = container == null ? new Item[0] : container.getItems();
		int occupied = 0;
		for (Item item : items)
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			occupied++;
			if (stackable && item.getId() == itemId)
			{
				return true;
			}
		}
		return occupied < inventorySlotCount(InventoryID.INVENTORY, items);
	}

	private boolean isInventoryVisible()
	{
		Widget inventory = client.getWidget(ComponentID.INVENTORY_CONTAINER);
		return inventory != null && !inventory.isHidden();
	}

	private boolean lootOwnershipAccepted(Map<String, Object> item)
	{
		CvHelperLootOwnershipMode mode = getMobFarmerLootOwnershipMode();
		if (mode == CvHelperLootOwnershipMode.ANY)
		{
			return true;
		}
		int ownership = intValue(item.get("ownershipId"), TileItem.OWNERSHIP_NONE);
		boolean own = ownership == TileItem.OWNERSHIP_SELF || ownership == TileItem.OWNERSHIP_GROUP;
		if (mode == CvHelperLootOwnershipMode.OWN_ONLY)
		{
			return own;
		}
		return own || ownership == TileItem.OWNERSHIP_NONE;
	}

	private String ownershipName(int ownership)
	{
		switch (ownership)
		{
			case TileItem.OWNERSHIP_SELF:
				return "self";
			case TileItem.OWNERSHIP_OTHER:
				return "other";
			case TileItem.OWNERSHIP_GROUP:
				return "group";
			default:
				return "public";
		}
	}

	private boolean matchesItemPolicy(String itemName, int itemId, String policy)
	{
		return matchesItemPolicy(itemName, itemId, 1, policy);
	}

	private boolean isMobFarmerProtectedInventoryItem(String itemName, int itemId, int quantity)
	{
		if (matchesItemPolicy(itemName, itemId, quantity, getMobFarmerNeverDropItems()))
		{
			return true;
		}
		return matchesItemPolicy(itemName, itemId, quantity, MOB_FARMER_IMPLICIT_NEVER_DROP_ITEMS);
	}

	private boolean matchesItemPolicy(String itemName, int itemId, int quantity, String policy)
	{
		if (policy == null || policy.trim().isEmpty())
		{
			return false;
		}
		for (String candidate : actionTargetCandidates(policy))
		{
			if (matchesItemTarget(itemName, itemId, quantity, candidate))
			{
				return true;
			}
		}
		return false;
	}

	private boolean matchesItemTarget(String itemName, int itemId, int quantity, String target)
	{
		if (!itemQuantityMatches(target, quantity))
		{
			return false;
		}
		String nameTarget = itemPolicyName(target);
		String needle = normalize(nameTarget);
		if (needle.isEmpty())
		{
			return false;
		}
		String idNeedle = needle.startsWith("id") ? normalize(needle.substring(2)) : needle;
		if (String.valueOf(itemId).equals(idNeedle) || String.valueOf(itemId).equals(needle))
		{
			return true;
		}
		if (nameTarget.contains("*"))
		{
			return WildcardMatcher.matches(nameTarget, itemName);
		}
		return normalize(itemName).contains(needle);
	}

	private GroundItemsClassification groundItemsClassification(String itemName, int quantity, int geEach, int haEach, ItemComposition composition)
	{
		int highlighted = groundItemsListMatchLevel(itemName, quantity, groundItemsHighlightedItems());
		if (highlighted == GROUND_ITEMS_EXACT)
		{
			return GroundItemsClassification.HIGHLIGHTED;
		}
		int hidden = groundItemsListMatchLevel(itemName, quantity, groundItemsHiddenItems());
		if (hidden == GROUND_ITEMS_EXACT)
		{
			return GroundItemsClassification.HIDDEN;
		}
		if (highlighted == GROUND_ITEMS_WILDCARD)
		{
			return GroundItemsClassification.HIGHLIGHTED;
		}
		if (hidden == GROUND_ITEMS_WILDCARD)
		{
			return GroundItemsClassification.HIDDEN;
		}
		if (groundItemsHiddenByValue(geEach, haEach, composition))
		{
			return GroundItemsClassification.HIDDEN_BY_VALUE;
		}
		if (groundItemsShowHighlightedOnly())
		{
			return GroundItemsClassification.SUPPRESSED_BY_SHOW_HIGHLIGHTED_ONLY;
		}
		return GroundItemsClassification.NONE;
	}

	private int groundItemsListMatchLevel(String itemName, int quantity, String policy)
	{
		if (policy == null || policy.trim().isEmpty())
		{
			return GROUND_ITEMS_NONE;
		}
		List<String> candidates = actionTargetCandidates(policy);
		for (String candidate : candidates)
		{
			if (matchesGroundItemsEntry(itemName, quantity, candidate, false))
			{
				return GROUND_ITEMS_EXACT;
			}
		}
		for (String candidate : candidates)
		{
			if (matchesGroundItemsEntry(itemName, quantity, candidate, true))
			{
				return GROUND_ITEMS_WILDCARD;
			}
		}
		return GROUND_ITEMS_NONE;
	}

	private boolean matchesGroundItemsEntry(String itemName, int quantity, String candidate, boolean wildcardOnly)
	{
		if (!itemQuantityMatches(candidate, quantity))
		{
			return false;
		}
		String nameTarget = itemPolicyName(candidate);
		if (nameTarget.trim().isEmpty() || nameTarget.contains("*") != wildcardOnly)
		{
			return false;
		}
		return wildcardOnly ? WildcardMatcher.matches(nameTarget, itemName) : normalize(itemName).equals(normalize(nameTarget));
	}

	private boolean groundItemsHiddenByValue(int geEach, int haEach, ItemComposition composition)
	{
		int hideUnderValue = groundItemsHideUnderValue();
		if (hideUnderValue <= 0)
		{
			return false;
		}
		boolean tradeable = composition != null && composition.isGeTradeable();
		boolean canBeHidden = geEach > 0 || tradeable || !groundItemsDontHideUntradeables();
		return canBeHidden && geEach < hideUnderValue && haEach < hideUnderValue;
	}

	private boolean itemQuantityMatches(String target, int quantity)
	{
		int operatorIndex = itemQuantityOperatorIndex(target);
		if (operatorIndex < 0 || target == null || operatorIndex + 1 >= target.length())
		{
			return true;
		}
		int threshold;
		try
		{
			threshold = Integer.parseInt(target.substring(operatorIndex + 1).trim());
		}
		catch (NumberFormatException e)
		{
			return true;
		}
		return target.charAt(operatorIndex) == '<' ? quantity < threshold : quantity > threshold;
	}

	private String itemPolicyName(String target)
	{
		if (target == null)
		{
			return "";
		}
		int operatorIndex = itemQuantityOperatorIndex(target);
		return (operatorIndex < 0 ? target : target.substring(0, operatorIndex)).trim();
	}

	private int itemQuantityOperatorIndex(String target)
	{
		if (target == null)
		{
			return -1;
		}
		for (int i = target.length() - 1; i >= 0; i--)
		{
			char c = target.charAt(i);
			if ((c >= '0' && c <= '9') || Character.isWhitespace(c))
			{
				continue;
			}
			return c == '<' || c == '>' ? i : -1;
		}
		return -1;
	}

	private String groundItemsHighlightedItems()
	{
		String value = configManager.getConfiguration(GROUND_ITEMS_CONFIG_GROUP, GROUND_ITEMS_HIGHLIGHTED_ITEMS);
		return value == null ? "" : value;
	}

	private String groundItemsHiddenItems()
	{
		String value = configManager.getConfiguration(GROUND_ITEMS_CONFIG_GROUP, GROUND_ITEMS_HIDDEN_ITEMS);
		return value == null ? "Vial, Ashes, Coins, Bones, Bucket, Jug, Seaweed" : value;
	}

	private boolean groundItemsShowHighlightedOnly()
	{
		Boolean value = configManager.getConfiguration(GROUND_ITEMS_CONFIG_GROUP, GROUND_ITEMS_SHOW_HIGHLIGHTED_ONLY, Boolean.class);
		return value != null && value;
	}

	private int groundItemsHideUnderValue()
	{
		Integer value = configManager.getConfiguration(GROUND_ITEMS_CONFIG_GROUP, GROUND_ITEMS_HIDE_UNDER_VALUE, Integer.class);
		return value == null ? 0 : Math.max(0, value);
	}

	private boolean groundItemsDontHideUntradeables()
	{
		Boolean value = configManager.getConfiguration(GROUND_ITEMS_CONFIG_GROUP, GROUND_ITEMS_DONT_HIDE_UNTRADEABLES, Boolean.class);
		return value == null || value;
	}

	private Map<String, Object> groundItemsConfigStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("highlightedItems", groundItemsHighlightedItems());
		out.put("hiddenItems", groundItemsHiddenItems());
		out.put("showHighlightedOnly", groundItemsShowHighlightedOnly());
		out.put("hideUnderValue", groundItemsHideUnderValue());
		out.put("dontHideUntradeables", groundItemsDontHideUntradeables());
		out.put("mode", getMobFarmerGroundItemsMode().name());
		out.put("respectHidden", getMobFarmerRespectGroundItemsHidden());
		return out;
	}

	private String[] targetActions(Map<String, Object> target)
	{
		Object actions = target.get("actions");
		if (actions instanceof String[])
		{
			return (String[]) actions;
		}
		if (actions instanceof List)
		{
			List<?> actionList = (List<?>) actions;
			String[] out = new String[actionList.size()];
			for (int i = 0; i < actionList.size(); i++)
			{
				out[i] = actionList.get(i) == null ? "" : String.valueOf(actionList.get(i));
			}
			return out;
		}
		return new String[0];
	}

	private String[] inventoryTargetActions(Map<String, Object> target)
	{
		Object actions = target.get("inventoryActions");
		if (actions instanceof String[])
		{
			return (String[]) actions;
		}
		if (actions instanceof List)
		{
			List<?> actionList = (List<?>) actions;
			String[] out = new String[actionList.size()];
			for (int i = 0; i < actionList.size(); i++)
			{
				out[i] = actionList.get(i) == null ? "" : String.valueOf(actionList.get(i));
			}
			return out;
		}
		return targetActions(target);
	}

	private String[] itemInventoryActions(int itemId)
	{
		if (itemId <= 0)
		{
			return new String[0];
		}
		ItemComposition composition = itemManager.getItemComposition(itemId);
		String[] actions = composition == null ? null : composition.getInventoryActions();
		return actions == null ? new String[0] : actions;
	}

	private void setMobFarmerLootDecision(String decision, Map<String, Object> target)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("decision", decision);
		payload.put("at", Instant.now().toString());
		if (target != null)
		{
			payload.put("target", target);
		}
		lastMobFarmerLootDecision = payload;
	}

	private void setMobFarmerSurvivalDecision(String decision, Map<String, Object> details)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("decision", decision);
		payload.put("at", Instant.now().toString());
		if (details != null)
		{
			payload.put("details", details);
		}
		lastMobFarmerSurvivalDecision = payload;
	}

	private void setMobFarmerIntermediateDecision(String decision, Map<String, Object> details)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("decision", decision);
		payload.put("at", Instant.now().toString());
		if (details != null)
		{
			payload.put("details", details);
		}
		lastMobFarmerIntermediateDecision = payload;
	}

	/**
	 * The "hijack": if the selected target's path required a door transition (see
	 * {@link PathingResult#doorTransition}), this takes over the tick instead of letting the
	 * caller attack/walk immediately. It verifies the door's REAL current state, and only
	 * once that state actually matches what's needed does it let normal target logic resume
	 * -- it does not trust the path being "theoretically" clear. Returns true if this tick
	 * was consumed by door handling (caller should return without attacking).
	 */
	private boolean tryHandleMobFarmerDoorTransition(Player localPlayer, Map<String, Object> target, boolean live)
	{
		Map<String, Object> doorTransition = mapValue(target.get("doorTransition"));
		if (doorTransition.isEmpty())
		{
			if (mobFarmerDoorTransitionKey != null)
			{
				mobFarmerDoorTransitionKey = null;
				mobFarmerDoorTransitionAttempts = 0;
			}
			return false;
		}

		int doorId = intValue(doorTransition.get("id"), -1);
		String requiredAction = String.valueOf(doorTransition.get("requiredAction"));
		String doorName = String.valueOf(doorTransition.get("name"));
		Map<String, Object> tileMap = mapValue(doorTransition.get("worldTile"));
		Map<String, Object> fromTileMap = mapValue(doorTransition.get("fromTile"));
		Map<String, Object> toTileMap = mapValue(doorTransition.get("toTile"));
		boolean knownAction = "Open".equals(requiredAction) || "Close".equals(requiredAction);
		if (doorId < 0 || tileMap == null || !knownAction)
		{
			// Unknown/ambiguous required action (eg. unknown-door-action / disabled / denylisted
			// status from the kernel) -- nothing CV Helper is allowed to click. Surface as
			// manual action required and let normal selection logic reject the target.
			recordMobFarmerDoorTransition("manual-action-required", doorTransition);
			return false;
		}
		int wx = intValue(tileMap.get("x"), Integer.MIN_VALUE);
		int wy = intValue(tileMap.get("y"), Integer.MIN_VALUE);
		if (wx == Integer.MIN_VALUE)
		{
			return false;
		}
		int plane = intValue(tileMap.get("plane"), localPlayer.getWorldLocation().getPlane());
		WorldPoint doorTile = new WorldPoint(wx, wy, plane);
		WorldPoint fromTile = worldPointValue(fromTileMap, plane);
		WorldPoint toTile = worldPointValue(toTileMap, plane);

		Integer state = pathfinding.currentDoorState(localPlayer, doorTile);
		boolean alreadyDone = pathfinding.isDoorTransitionSatisfied(localPlayer, fromTile, toTile, requiredAction)
			|| ("Open".equals(requiredAction) && state != null && state == 1)
			|| ("Close".equals(requiredAction) && state != null && state == 0);
		String doorKey = doorId + "@" + wx + "," + wy + "," + plane + ":" + requiredAction;

		if (alreadyDone)
		{
			if (doorKey.equals(mobFarmerDoorTransitionKey))
			{
				mobFarmerDoorTransitionKey = null;
				mobFarmerDoorTransitionAttempts = 0;
			}
			recordMobFarmerDoorTransition("door-now-clear", doorTransition);
			return false;
		}

		if (!live)
		{
			recordMobFarmerDoorTransition("dry-pending", doorTransition);
			mobFarmerStatus.set("dry-door-transition:" + requiredAction + ":" + doorName);
			updatePanelStatus("Mob farmer dry door transition: " + requiredAction + " " + doorName);
			return true;
		}

		if (!doorKey.equals(mobFarmerDoorTransitionKey))
		{
			mobFarmerDoorTransitionKey = doorKey;
			mobFarmerDoorTransitionAttempts = 0;
			mobFarmerDoorTransitionLastClickMillis = 0;
		}

		if (mobFarmerDoorTransitionAttempts >= MOB_FARMER_DOOR_TRANSITION_MAX_ATTEMPTS)
		{
			recordMobFarmerDoorTransition("timeout", doorTransition);
			mobFarmerStatus.set("door-transition-timeout:" + doorName);
			updatePanelStatus("Mob farmer gave up on door transition: " + doorName);
			return true;
		}

		long now = System.currentTimeMillis();
		if (now - mobFarmerDoorTransitionLastClickMillis < MOB_FARMER_DOOR_TRANSITION_CLICK_COOLDOWN_MS)
		{
			recordMobFarmerDoorTransition("waiting", doorTransition);
			mobFarmerStatus.set("waiting-door-transition:" + doorName);
			return true;
		}

		boolean clicked = invokeDoorTransitionAction(localPlayer, doorTile, doorId, requiredAction, doorName);
		mobFarmerDoorTransitionLastClickMillis = now;
		mobFarmerDoorTransitionAttempts++;
		recordMobFarmerDoorTransition(clicked ? "clicked" : "click-failed", doorTransition);
		mobFarmerStatus.set((clicked ? "door-transition-clicked:" : "door-transition-click-failed:") + doorName);
		updatePanelStatus("Mob farmer " + requiredAction.toLowerCase() + "ing door: " + doorName);
		return true;
	}

	private void recordMobFarmerDoorTransition(String result, Map<String, Object> doorTransition)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("result", result);
		payload.put("door", doorTransition);
		payload.put("attempts", mobFarmerDoorTransitionAttempts);
		payload.put("updatedAt", Instant.now().toString());
		lastMobFarmerDoorTransitionStatus = payload;
	}

	private WorldPoint worldPointValue(Map<String, Object> value, int defaultPlane)
	{
		if (value == null)
		{
			return null;
		}
		int x = intValue(value.get("x"), Integer.MIN_VALUE);
		int y = intValue(value.get("y"), Integer.MIN_VALUE);
		if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
		{
			return null;
		}
		return new WorldPoint(x, y, intValue(value.get("plane"), defaultPlane));
	}

	private boolean invokeDoorTransitionAction(Player localPlayer, WorldPoint doorTile, int doorId, String action, String name)
	{
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null)
		{
			return false;
		}
		LocalPoint localPoint = LocalPoint.fromWorld(worldView, doorTile);
		if (localPoint == null)
		{
			return false;
		}
		ObjectComposition composition = safeValue(() -> client.getObjectDefinition(doorId), null);
		int actionIndex = objectActionIndex(composition, action);
		MenuAction menuAction = gameObjectMenuActionForIndex(actionIndex);
		if (menuAction == null)
		{
			return false;
		}
		if (!actionInProgress.compareAndSet(false, true))
		{
			return false;
		}
		try
		{
			client.menuAction(localPoint.getSceneX(), localPoint.getSceneY(), menuAction, doorId, -1, action, name);
			return true;
		}
		finally
		{
			actionInProgress.set(false);
		}
	}

	private MobFarmerSelection selectMobFarmerTarget(Player localPlayer)
	{
		MobFarmerSelection selection = new MobFarmerSelection();
		selection.multiCombat = isMultiCombat();
		lastMobFarmerMultiCombat = selection.multiCombat;

		MobFarmerCandidate best = null;
		for (NPC npc : client.getNpcs())
		{
			MobFarmerCandidate candidate = evaluateMobFarmerCandidate(localPlayer, npc, selection.multiCombat);
			selection.reports.add(candidateReport(candidate));
			if (!candidate.selectable)
			{
				continue;
			}
			if (best == null || candidate.score < best.score)
			{
				best = candidate;
			}
		}

		if (best == null)
		{
			selection.decision = selection.reports.isEmpty() ? "no-candidates" : "no-valid-candidates";
			return selection;
		}

		selection.target = best.entity;
		selection.decision = "selected:" + targetLabelForMessage(best.entity);
		return selection;
	}

	private MobFarmerCandidate evaluateMobFarmerCandidate(Player localPlayer, NPC npc, boolean multiCombat)
	{
		Map<String, Object> entity = actorEntity("npc", npc, localPlayer);
		MobFarmerCandidate candidate = new MobFarmerCandidate(npc, entity);
		int distance = entity.get("distance") instanceof Number ? ((Number) entity.get("distance")).intValue() : Integer.MAX_VALUE;
		candidate.score = distance;

		if (!matchesAnyMobTarget(npc, mobFarmerTarget))
		{
			candidate.reject("target-mismatch");
			return candidate;
		}
		if (!isNpcAttackable(npc))
		{
			candidate.reject("not-attackable");
		}
		if (isEffectivelyDead(npc))
		{
			candidate.reject("dead-or-zero-hp");
		}
		if (getMobFarmerAttackInteractionMode() == CvHelperMobInteractionMode.MENU_ACTION)
		{
			if (intValue(entity.get("attackActionIndex"), -1) < 0)
			{
				candidate.reject("no-attack-menu-action");
			}
		}
		else if (!(entity.get("clickPoint") instanceof Map))
		{
			candidate.reject("no-click-point");
		}

		int maxDistance = getMobFarmerMaxDistance();
		if (maxDistance > 0 && distance > maxDistance)
		{
			candidate.reject("too-far:" + distance + ">" + maxDistance);
		}

		PathingResult pathing = pathfinding.mobFarmerPathDistanceToMelee(localPlayer, npc, maxDistance);
		entity.put("reachable", pathing.reachable);
		entity.put("pathDistance", pathing.reachable ? pathing.pathDistance : null);
		entity.put("pathSearchLimit", pathing.searchLimit);
		entity.put("pathVisited", pathing.visited);
		entity.put("pathFailureReason", pathing.failureReason);
		entity.put("doorTransition", pathing.doorTransition);
		entity.put("blockedByDoor", pathing.blockedByDoor);
		entity.put("manualActionRequired", pathing.manualActionRequired);
		entity.put("manualActionReason", pathing.manualActionReason);
		entity.put("blockingDoor", pathing.blockingDoor);
		if (pathing.reachable)
		{
			candidate.score = pathing.pathDistance;
			if (maxDistance > 0 && pathing.pathDistance > maxDistance)
			{
				candidate.reject("path-too-far:" + pathing.pathDistance + ">" + maxDistance);
			}
		}
		else
		{
			candidate.reject(pathing.manualActionRequired ? "manual-action-required:" + pathing.manualActionReason : "unreachable:" + pathing.failureReason);
		}

		boolean lineOfSight = hasLineOfSight(localPlayer, npc);
		entity.put("lineOfSightToLocalPlayer", lineOfSight);
		if (getMobFarmerRequireLineOfSight() && !lineOfSight)
		{
			candidate.reject("no-line-of-sight");
		}

		Actor npcInteracting = npc.getInteracting();
		candidate.engagedWithLocalPlayer = npcInteracting == localPlayer;
		candidate.engagedByOther = npcInteracting != null && npcInteracting != localPlayer;
		entity.put("engagedWithLocalPlayer", candidate.engagedWithLocalPlayer);
		entity.put("engagedByOther", candidate.engagedByOther);

		if (candidate.engagedWithLocalPlayer)
		{
			candidate.note("already-fighting-us");
			candidate.score -= 1000;
		}
		else if (candidate.engagedByOther)
		{
			applyEngagedMobPolicy(candidate, multiCombat);
		}

		entity.put("mobFarmerSelectable", candidate.selectable);
		entity.put("mobFarmerReasons", new ArrayList<>(candidate.reasons));
		entity.put("mobFarmerScore", candidate.score);
		return candidate;
	}

	private void applyEngagedMobPolicy(MobFarmerCandidate candidate, boolean multiCombat)
	{
		if (!multiCombat)
		{
			candidate.reject("engaged-by-other-single-combat");
			return;
		}

		CvHelperMobEngagedMode mode = getMobFarmerEngagedMode();
		if (mode == CvHelperMobEngagedMode.FREE_ONLY)
		{
			candidate.reject("engaged-by-other-free-only");
		}
		else if (mode == CvHelperMobEngagedMode.PREFER_FREE)
		{
			candidate.note("engaged-by-other-deprioritized");
			candidate.score += 1000;
		}
		else
		{
			candidate.note("engaged-by-other-allowed");
			candidate.score += 50;
		}
	}

	private Map<String, Object> candidateReport(MobFarmerCandidate candidate)
	{
		Map<String, Object> report = new LinkedHashMap<>();
		report.put("name", candidate.entity.get("name"));
		report.put("id", candidate.entity.get("id"));
		report.put("distance", candidate.entity.get("distance"));
		report.put("selectable", candidate.selectable);
		report.put("score", candidate.score);
		report.put("reasons", new ArrayList<>(candidate.reasons));
		report.put("healthRatio", candidate.entity.get("healthRatio"));
		report.put("healthScale", candidate.entity.get("healthScale"));
		report.put("dead", candidate.entity.get("dead"));
		report.put("attackable", candidate.entity.get("attackable"));
		report.put("interacting", candidate.entity.get("interacting"));
		report.put("engagedByOther", candidate.entity.get("engagedByOther"));
		report.put("engagedWithLocalPlayer", candidate.entity.get("engagedWithLocalPlayer"));
		report.put("lineOfSightToLocalPlayer", candidate.entity.get("lineOfSightToLocalPlayer"));
		report.put("reachable", candidate.entity.get("reachable"));
		report.put("pathDistance", candidate.entity.get("pathDistance"));
		report.put("pathSearchLimit", candidate.entity.get("pathSearchLimit"));
		report.put("pathVisited", candidate.entity.get("pathVisited"));
		report.put("pathFailureReason", candidate.entity.get("pathFailureReason"));
		report.put("playerWorldLocation", candidate.entity.get("playerWorldLocation"));
		report.put("doorTransition", candidate.entity.get("doorTransition"));
		report.put("blockedByDoor", candidate.entity.get("blockedByDoor"));
		report.put("manualActionRequired", candidate.entity.get("manualActionRequired"));
		report.put("manualActionReason", candidate.entity.get("manualActionReason"));
		report.put("blockingDoor", candidate.entity.get("blockingDoor"));
		report.put("attackActionIndex", candidate.entity.get("attackActionIndex"));
		report.put("attackMenuAction", candidate.entity.get("attackMenuAction"));
		report.put("clickPoint", candidate.entity.get("clickPoint"));
		report.put("worldLocation", candidate.entity.get("worldLocation"));
		return report;
	}

	private void setMobFarmerDecision(String decision, Map<String, Object> target)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("decision", decision);
		payload.put("at", Instant.now().toString());
		if (target != null)
		{
			payload.put("target", target);
		}
		lastMobFarmerDecision = payload;
	}

	private void recordMobFarmerIntent(String intent, Map<String, Object> target)
	{
		int tick = safeValue(client::getTickCount, 0);
		String key = mobFarmerTargetKey(target);
		int distance = target == null ? Integer.MAX_VALUE : intValue(target.get("distance"), Integer.MAX_VALUE);
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("intent", intent);
		entry.put("at", Instant.now().toString());
		entry.put("tick", tick);
		entry.put("target", target == null ? null : targetLabelForMessage(target));
		entry.put("targetKey", key);
		if (distance != Integer.MAX_VALUE)
		{
			entry.put("distance", distance);
		}

		if ("LOOT_ITEM".equals(intent) && key != null)
		{
			updateMobFarmerLootChase(key, distance, tick);
		}

		List<Map<String, Object>> intents = new ArrayList<>(lastMobFarmerIntents);
		intents.add(entry);
		while (intents.size() > 6)
		{
			intents.remove(0);
		}
		lastMobFarmerIntents = intents;

		boolean oscillation = mobFarmerOscillationDetected(intents);
		if (oscillation)
		{
			mobFarmerMakeProgressUntilTick = Math.max(mobFarmerMakeProgressUntilTick, tick + MOB_FARMER_PROGRESS_WINDOW_TICKS);
		}
		updateMobFarmerProgressStatus(intent, target, oscillation, tick);
	}

	private void updateMobFarmerLootChase(String key, int distance, int tick)
	{
		if (!key.equals(activeMobFarmerLootKey))
		{
			activeMobFarmerLootKey = key;
			activeMobFarmerLootStartTick = tick;
			activeMobFarmerLootLastDistance = distance;
			activeMobFarmerLootImproving = true;
			return;
		}
		activeMobFarmerLootImproving = distance <= activeMobFarmerLootLastDistance;
		activeMobFarmerLootLastDistance = distance;
	}

	private void recordPendingMobFarmerDeathLoot(Actor actor)
	{
		if (actor == null)
		{
			return;
		}
		String key = actor.getName() + "@" + actor.getWorldLocation();
		int tick = safeValue(client::getTickCount, 0);
		if (!key.equals(pendingMobFarmerDeathKey))
		{
			pendingMobFarmerDeathKey = key;
			pendingMobFarmerDeathTick = tick;
			pendingMobFarmerDeathTarget = actorSummary(actor);
		}
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("target", actor.getName());
		status.put("targetKey", pendingMobFarmerDeathKey);
		status.put("targetSummary", pendingMobFarmerDeathTarget);
		status.put("currentTick", tick);
		status.put("lastAttackedTick", lastMobFarmerActionTickByKey.get(mobFarmerSchedulerKey(MobFarmerActionKind.COMBAT, pendingMobFarmerDeathKey)));
		status.put("dyingDetectedTick", pendingMobFarmerDeathTick);
		status.put("ticksSinceDyingDetected", pendingMobFarmerDeathTick < 0 ? null : tick - pendingMobFarmerDeathTick);
		status.put("healthRatio", actor.getHealthRatio());
		status.put("healthScale", actor.getHealthScale());
		status.put("effectivelyDead", isEffectivelyDead(actor));
		status.put("expectedLootWorldLocation", pointValue(actor.getWorldLocation()));
		status.put("movedTowardExpectedLoot", false);
		status.put("moveReason", "movement-primitive-not-yet-implemented");
		status.put("waitingForLoot", mobFarmerPendingDeathLootActive());
		lastMobFarmerDeathLootStatus = status;
	}

	private boolean mobFarmerPendingDeathLootActive()
	{
		int tick = safeValue(client::getTickCount, 0);
		return pendingMobFarmerDeathKey != null
			&& pendingMobFarmerDeathTick >= 0
			&& tick - pendingMobFarmerDeathTick <= MOB_FARMER_LOOT_SPAWN_GRACE_TICKS;
	}

	private void clearPendingMobFarmerDeath(String reason)
	{
		if (pendingMobFarmerDeathKey == null)
		{
			return;
		}
		Map<String, Object> status = new LinkedHashMap<>(lastMobFarmerDeathLootStatus);
		status.put("clearedReason", reason);
		status.put("clearedAtTick", safeValue(client::getTickCount, 0));
		status.put("waitingForLoot", false);
		lastMobFarmerDeathLootStatus = status;
		pendingMobFarmerDeathKey = null;
		pendingMobFarmerDeathTick = -1;
		pendingMobFarmerDeathTarget = new LinkedHashMap<>();
	}

	private boolean mobFarmerOscillationDetected(List<Map<String, Object>> intents)
	{
		if (intents.size() < 4)
		{
			return false;
		}
		List<String> recent = new ArrayList<>();
		for (int i = intents.size() - 4; i < intents.size(); i++)
		{
			recent.add(String.valueOf(intents.get(i).get("intent")));
		}
		return ("ATTACK_TARGET".equals(recent.get(0)) && "LOOT_ITEM".equals(recent.get(1)) && "ATTACK_TARGET".equals(recent.get(2)) && "LOOT_ITEM".equals(recent.get(3)))
			|| ("LOOT_ITEM".equals(recent.get(0)) && "ATTACK_TARGET".equals(recent.get(1)) && "LOOT_ITEM".equals(recent.get(2)) && "ATTACK_TARGET".equals(recent.get(3)));
	}

	private void updateMobFarmerProgressStatus(String intent, Map<String, Object> target, boolean oscillationDetected, int tick)
	{
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("currentIntent", intent);
		status.put("currentTick", tick);
		status.put("target", target == null ? null : targetLabelForMessage(target));
		status.put("targetKey", mobFarmerTargetKey(target));
		if (target != null && target.get("distance") != null)
		{
			status.put("targetDistance", target.get("distance"));
		}
		status.put("oscillationDetected", oscillationDetected);
		status.put("makeProgressActive", activeMobFarmerLootKey != null && activeMobFarmerLootImproving && tick <= mobFarmerMakeProgressUntilTick);
		status.put("makeProgressUntilTick", mobFarmerMakeProgressUntilTick);
		status.put("activeLootKey", activeMobFarmerLootKey);
		status.put("activeLootStartTick", activeMobFarmerLootStartTick);
		status.put("activeLootLastDistance", activeMobFarmerLootLastDistance == Integer.MAX_VALUE ? null : activeMobFarmerLootLastDistance);
		status.put("activeLootDistanceImproving", activeMobFarmerLootImproving);
		status.put("recentIntentNames", recentMobFarmerIntentNames());
		lastMobFarmerProgressStatus = status;
	}

	private List<String> recentMobFarmerIntentNames()
	{
		List<String> names = new ArrayList<>();
		for (Map<String, Object> entry : lastMobFarmerIntents)
		{
			names.add(String.valueOf(entry.get("intent")));
		}
		return names;
	}

	private boolean mobFarmerMakeProgressActive()
	{
		int tick = safeValue(client::getTickCount, 0);
		return activeMobFarmerLootKey != null
			&& activeMobFarmerLootImproving
			&& tick <= mobFarmerMakeProgressUntilTick
			&& (activeMobFarmerLootStartTick <= 0 || tick - activeMobFarmerLootStartTick <= MOB_FARMER_PROGRESS_WINDOW_TICKS);
	}

	private void clearMobFarmerLootChase(String reason)
	{
		activeMobFarmerLootKey = null;
		activeMobFarmerLootStartTick = 0;
		activeMobFarmerLootLastDistance = Integer.MAX_VALUE;
		activeMobFarmerLootImproving = false;
		Map<String, Object> status = new LinkedHashMap<>(lastMobFarmerProgressStatus);
		status.put("activeLootClearedReason", reason);
		status.put("activeLootKey", null);
		status.put("activeLootDistanceImproving", false);
		lastMobFarmerProgressStatus = status;
	}

	private String mobFarmerTargetKey(Map<String, Object> target)
	{
		if (target == null)
		{
			return null;
		}
		Object surface = target.get("surface");
		if ("loot".equals(surface))
		{
			return "loot:" + target.get("itemId") + "@" + target.get("sceneX") + "," + target.get("sceneY");
		}
		Object type = target.get("type");
		if ("npc".equals(type))
		{
			return "npc:" + target.get("id") + "@" + target.get("index");
		}
		return String.valueOf(target.get("label"));
	}

	private boolean matchesAnyMobTarget(NPC npc, String targetLabel)
	{
		for (String candidate : actionTargetCandidates(targetLabel))
		{
			if (matchesNpcTarget(npc, candidate))
			{
				return true;
			}
		}
		return false;
	}

	private boolean matchesNpcTarget(NPC npc, String targetLabel)
	{
		String needle = normalize(targetLabel);
		if (needle.isEmpty())
		{
			return true;
		}
		String idNeedle = needle.startsWith("id") ? normalize(needle.substring(2)) : needle;
		String id = String.valueOf(npc.getId());
		if (id.equals(idNeedle) || id.equals(needle))
		{
			return true;
		}
		return normalize(npc.getName()).contains(needle);
	}

	private boolean isNpcAttackable(NPC npc)
	{
		return npcAttackActionIndex(npc) >= 0;
	}

	private int npcAttackActionIndex(NPC npc)
	{
		NPCComposition composition = npc == null ? null : npc.getTransformedComposition();
		if (composition == null && npc != null)
		{
			composition = npc.getComposition();
		}
		if (composition == null || !composition.isInteractible() || composition.getActions() == null)
		{
			return -1;
		}
		String[] actions = composition.getActions();
		for (int i = 0; i < actions.length; i++)
		{
			if ("Attack".equalsIgnoreCase(actions[i]))
			{
				return i;
			}
		}
		return -1;
	}

	private MenuAction npcMenuActionForIndex(int actionIndex)
	{
		switch (actionIndex)
		{
			case 0:
				return MenuAction.NPC_FIRST_OPTION;
			case 1:
				return MenuAction.NPC_SECOND_OPTION;
			case 2:
				return MenuAction.NPC_THIRD_OPTION;
			case 3:
				return MenuAction.NPC_FOURTH_OPTION;
			case 4:
				return MenuAction.NPC_FIFTH_OPTION;
			default:
				return null;
		}
	}

	private boolean isEffectivelyDead(Actor actor)
	{
		return actor == null || actor.isDead() || actor.getHealthRatio() == 0;
	}

	private boolean isMultiCombat()
	{
		return safeValue(() -> client.getVarbitValue(VarbitID.MULTIWAY_INDICATOR) == 1, false);
	}

	private boolean hasLineOfSight(Player localPlayer, Actor target)
	{
		if (localPlayer == null || target == null)
		{
			return false;
		}
		WorldArea localArea = localPlayer.getWorldArea();
		WorldArea targetArea = target.getWorldArea();
		if (localArea == null || targetArea == null)
		{
			return false;
		}
		try
		{
			WorldView worldView = localPlayer.getWorldView();
			return localArea.hasLineOfSightTo(worldView == null ? client.getTopLevelWorldView() : worldView, targetArea);
		}
		catch (RuntimeException e)
		{
			return false;
		}
	}

	private Map<String, Object> findMobFarmerEntity(List<Map<String, Object>> entities, String targetLabel)
	{
		List<Map<String, Object>> npcs = new ArrayList<>();
		for (Map<String, Object> entity : entities)
		{
			if ("npc".equals(entity.get("type")))
			{
				npcs.add(entity);
			}
		}
		return findEntityByNameOrId(npcs, targetLabel);
	}

	private void refreshTargets(String surface, java.util.function.Supplier<List<Map<String, Object>>> collector)
	{
		clientThread.invokeLater(() ->
		{
			List<Map<String, Object>> targets = collector.get();
			setLastTargets(surface, targets);
			lastEvent.set(surface + "-targets@" + targets.size() + "@" + Instant.now());
			sendWebhook(eventPayload(surface + "-targets", targets));
			updatePanelStatus(surface + " targets: " + targets.size());
		});
	}

	void refreshActionSurface(CvHelperActionSurface surface)
	{
		if (surface == null)
		{
			return;
		}
		switch (surface)
		{
			case PRAYER:
				refreshPrayerTargets();
				break;
			case SPELL:
				refreshSpellTargets();
				break;
			case MINIMAP:
				refreshMinimapTargets();
				break;
			case INVENTORY:
				refreshInventoryTargets();
				break;
			case EQUIPMENT:
				refreshEquipmentTargets();
				break;
			case PANELS:
				refreshTargets("panels", this::collectPanelTargets);
				break;
			case COMBAT:
				refreshTargets("combat", this::collectCombatTargets);
				break;
			case NEAREST_ENTITY:
				refreshEntities();
				break;
			default:
				updatePanelStatus("No target refresh for " + surface);
				break;
		}
	}

	void captureScreenshot()
	{
		captureImage("client-frame", true, null);
	}

	void captureScreen()
	{
		captureImage("screen", false, null);
	}

	void captureMinimap()
	{
		clientThread.invokeLater(() ->
		{
			Rectangle minimapBounds = findFirstWidgetBounds(
				ComponentID.MINIMAP_CONTAINER,
				ComponentID.FIXED_VIEWPORT_MINIMAP,
				ComponentID.RESIZABLE_VIEWPORT_MINIMAP,
				ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP,
				ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA,
				ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA,
				ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA
			);
			if (minimapBounds == null)
			{
				lastEvent.set("capture-blocked:minimap-not-visible@" + Instant.now());
				updatePanelStatus("Minimap capture blocked: not visible");
				return;
			}
			captureImage("minimap", false, minimapBounds);
		});
	}

	void clickLoginScreen()
	{
		clientThread.invokeLater(() ->
		{
			GameState gameState = client.getGameState();
			Map<String, Object> attempt = new LinkedHashMap<>();
			attempt.put("source", "clickLoginScreen");
			attempt.put("gameState", gameState == null ? null : gameState.name());
			attempt.put("detectedScreen", detectedLoginScreen(gameState));
			attempt.put("worldAllowed", mobFarmerLoginWorldAllowed());
			attempt.put("startedAt", Instant.now().toString());
			if (gameState != GameState.LOGIN_SCREEN && gameState != GameState.LOGIN_SCREEN_AUTHENTICATOR)
			{
				attempt.put("failureReason", "bad-game-state");
				setLastLoginClickAttempt("skipped", attempt);
				updatePanelStatus("Login click skipped: game state is " + gameState);
				lastEvent.set("login-click-skipped:" + gameState + "@" + Instant.now());
				return;
			}

			Widget loginWidget = findLoginClickWidget();
			attempt.put("widget", loginWidgetDiagnostics(loginWidget));
			Point screenPoint;
			boolean enterFallback = false;
			boolean enterFallbackAllowed = loginEnterFallbackAllowed();
			attempt.put("enterFallbackAllowed", enterFallbackAllowed);
			if (isVisibleWidget(loginWidget))
			{
				Rectangle bounds = loginWidget.getBounds();
				Map<String, Object> canvasPoint = pointMap(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
				screenPoint = canvasPointToScreen(canvasPoint);
				attempt.put("canvasPoint", canvasPoint);
				attempt.put("screenPoint", awtPointMap(screenPoint));
				if (screenPoint == null && enterFallbackAllowed)
				{
					enterFallback = true;
				}
			}
			else if (enterFallbackAllowed)
			{
				screenPoint = null;
				enterFallback = true;
			}
			else
			{
				screenPoint = null;
			}
			if (screenPoint == null && !enterFallback)
			{
				attempt.put("failureReason", "click-to-play-widget-not-visible");
				setLastLoginClickAttempt("skipped", attempt);
				updatePanelStatus("Login click skipped: click-to-play widget not visible");
				lastEvent.set("login-click-skipped:no-widget@" + Instant.now());
				return;
			}

			boolean usedEnterFallback = enterFallback;
			attempt.put("plannedAction", usedEnterFallback ? "enter-key" : "robot-click");
			attempt.put("usedEnterFallback", usedEnterFallback);
			if (usedEnterFallback)
			{
				pressLoginEnterFallback("login-enter-fallback", "Pressed Enter on login screen", attempt);
				return;
			}
			attempt.put("result", "queued");
			setLastLoginClickAttempt("queued", attempt);
			Map<String, Object> queuedAttempt = new LinkedHashMap<>(attempt);
			boolean postClickEnterFallback = enterFallbackAllowed;
			Thread loginClickThread = new Thread(() ->
			{
				Map<String, Object> result = new LinkedHashMap<>(queuedAttempt);
				try
				{
					Robot robot = new Robot();
					clickScreenPoint(robot, screenPoint);
					result.put("actualActionInvoked", "robot-click");
					result.put("clickedAt", Instant.now().toString());
					if (postClickEnterFallback)
					{
						robot.delay(160);
						pressEnter(robot);
						result.put("actualActionInvoked", "robot-click+enter-key");
						result.put("postClickEnterFallback", true);
						result.put("enterFallbackAt", Instant.now().toString());
					}
					setLastLoginClickAttempt("clicked", result);
					updatePanelStatus("Clicked login screen");
					lastEvent.set("login-click@" + Instant.now());
				}
				catch (RuntimeException | java.awt.AWTException e)
				{
					result.put("failureReason", e.getMessage());
					setLastLoginClickAttempt("failed", result);
					log.warn("CV Helper login click failed", e);
					updatePanelStatus("Login click failed: " + e.getMessage());
					lastEvent.set("login-click-failed:" + e.getMessage() + "@" + Instant.now());
				}
			}, "cv-helper-login-click");
			loginClickThread.setDaemon(true);
			loginClickThread.start();
		});
	}

	private void pressLoginEnterFallback(String eventName, String panelMessage)
	{
		pressLoginEnterFallback(eventName, panelMessage, null);
	}

	private void pressLoginEnterFallback(String eventName, String panelMessage, Map<String, Object> attemptDetails)
	{
		Map<String, Object> queuedAttempt = attemptDetails == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attemptDetails);
		Point focusPoint = loginCanvasFocusPoint();
		queuedAttempt.putIfAbsent("source", "pressLoginEnterFallback");
		queuedAttempt.put("eventName", eventName);
		queuedAttempt.put("plannedAction", "enter-key");
		queuedAttempt.put("usedEnterFallback", true);
		queuedAttempt.put("focusBeforeEnter", focusPoint != null);
		queuedAttempt.put("focusPoint", awtPointMap(focusPoint));
		queuedAttempt.put("result", "queued");
		setLastLoginClickAttempt("queued", queuedAttempt);
		Thread loginClickThread = new Thread(() ->
		{
			Map<String, Object> result = new LinkedHashMap<>(queuedAttempt);
			try
			{
				Robot robot = new Robot();
				if (focusPoint != null)
				{
					clickScreenPoint(robot, focusPoint);
					robot.delay(80);
					result.put("focusClickedAt", Instant.now().toString());
				}
				pressEnter(robot);
				result.put("actualActionInvoked", focusPoint == null ? "enter-key" : "focus-click+enter-key");
				result.put("pressedAt", Instant.now().toString());
				setLastLoginClickAttempt("pressed-enter", result);
				updatePanelStatus(panelMessage);
				lastEvent.set(eventName + "@" + Instant.now());
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				result.put("failureReason", e.getMessage());
				setLastLoginClickAttempt("failed", result);
				log.warn("CV Helper login Enter fallback failed", e);
				updatePanelStatus("Login Enter fallback failed: " + e.getMessage());
				lastEvent.set(eventName + "-failed:" + e.getMessage() + "@" + Instant.now());
			}
		}, "cv-helper-login-enter");
		loginClickThread.setDaemon(true);
		loginClickThread.start();
	}

	private Point loginCanvasFocusPoint()
	{
		try
		{
			Point canvasLocation = client.getCanvas().getLocationOnScreen();
			Dimension size = client.getCanvas().getSize();
			if (size == null || size.width <= 0 || size.height <= 0)
			{
				size = client.getRealDimensions();
			}
			if (canvasLocation == null || size == null || size.width <= 0 || size.height <= 0)
			{
				return null;
			}
			// Prefer the centre of the actual game viewport when one is rendered. In fixed
			// (classic) layout the play area sits to the LEFT of the sidebar, so the raw
			// canvas centre would land on/near the sidebar instead of the play area; the
			// viewport offsets account for that whether the sidebar is on or off. On the
			// login / "click here to play" screen there is no viewport (and no sidebar),
			// so this falls back to the canvas centre, which is the correct target there.
			int vpW = safeValue(client::getViewportWidth, 0);
			int vpH = safeValue(client::getViewportHeight, 0);
			if (vpW > 0 && vpH > 0)
			{
				int vpX = safeValue(client::getViewportXOffset, 0);
				int vpY = safeValue(client::getViewportYOffset, 0);
				return new Point(canvasLocation.x + vpX + vpW / 2, canvasLocation.y + vpY + vpH / 2);
			}
			return new Point(canvasLocation.x + size.width / 2, canvasLocation.y + size.height / 2);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private void pressEnter(Robot robot)
	{
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.delay(40);
		robot.keyRelease(KeyEvent.VK_ENTER);
	}

	private void setLastLoginClickAttempt(String result, Map<String, Object> details)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		if (details != null)
		{
			payload.putAll(details);
		}
		payload.put("result", result);
		payload.put("updatedAt", Instant.now().toString());
		lastLoginClickAttempt = payload;
	}

	private Map<String, Object> loginWidgetDiagnostics(Widget widget)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("present", widget != null);
		if (widget == null)
		{
			return out;
		}
		out.put("id", widget.getId());
		out.put("visible", isVisibleWidget(widget));
		out.put("hidden", widget.isHidden());
		out.put("text", cleanWidgetText(widget.getText()));
		out.put("name", cleanWidgetText(widget.getName()));
		String[] actions = widget.getActions();
		out.put("actions", actions == null ? new ArrayList<>() : Arrays.asList(actions));
		out.put("bounds", rectangleMap(widget.getBounds()));
		out.put("candidate", isLoginClickCandidate(widget));
		return out;
	}

	private Map<String, Object> rectangleMap(Rectangle rectangle)
	{
		if (rectangle == null)
		{
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("x", rectangle.x);
		out.put("y", rectangle.y);
		out.put("width", rectangle.width);
		out.put("height", rectangle.height);
		return out;
	}

	private Map<String, Object> awtPointMap(Point point)
	{
		if (point == null)
		{
			return null;
		}
		return pointMap(point.x, point.y);
	}

	private Widget findLoginClickWidget()
	{
		// A generic system message dialog ("You were disconnected from the server", Ok)
		// can render OVER the title screen after a disconnect. It carries no GameState
		// signal of its own -- the client still reports LOGIN_SCREEN the whole time -- and
		// the real click-to-play widget underneath is not visible/clickable until this is
		// dismissed first. This is a genuinely separate step, confirmed by watching a live
		// disconnect screen: dismiss "Ok" -> THEN the Play Now widget appears. Checked first
		// so detectLoginRecoveryState() classifies this as CLICK_TO_PLAY (not manual-required)
		// and the existing auto-click flow dismisses it, then dismisses the real widget on
		// the very next tick once it's revealed.
		Widget messageBoxContinue = client.getWidget(InterfaceID.Messagebox.CONTINUE);
		if (isVisibleWidget(messageBoxContinue))
		{
			return messageBoxContinue;
		}

		for (int componentId : new int[]{InterfaceID.WelcomeScreen.PLAY, InterfaceID.WelcomeScreen.CLICKHERE_TEXT})
		{
			Widget widget = client.getWidget(componentId);
			if (isVisibleWidget(widget))
			{
				return widget;
			}
		}

		Widget widget = client.getWidget(WidgetInfo.LOGIN_CLICK_TO_PLAY_SCREEN);
		if (isVisibleWidget(widget))
		{
			return widget;
		}

		return findLoginClickWidget(client.getWidget(InterfaceID.WelcomeScreen.UNIVERSE));
	}

	private boolean loginEnterFallbackAllowed()
	{
		return client.getGameState() == GameState.LOGIN_SCREEN && mobFarmerLoginWorldAllowed();
	}

	private Widget findLoginClickWidget(Widget widget)
	{
		if (widget == null)
		{
			return null;
		}
		if (isVisibleWidget(widget) && isLoginClickCandidate(widget))
		{
			return widget;
		}

		for (Widget[] children : new Widget[][]{widget.getDynamicChildren(), widget.getStaticChildren(), widget.getNestedChildren()})
		{
			if (children == null)
			{
				continue;
			}
			for (Widget child : children)
			{
				Widget found = findLoginClickWidget(child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private boolean isVisibleWidget(Widget widget)
	{
		if (widget == null || widget.isHidden())
		{
			return false;
		}
		Rectangle bounds = widget.getBounds();
		return bounds != null && bounds.width > 0 && bounds.height > 0;
	}

	private boolean isLoginClickCandidate(Widget widget)
	{
		StringBuilder haystack = new StringBuilder();
		haystack.append(cleanWidgetText(widget.getText())).append(' ');
		haystack.append(cleanWidgetText(widget.getName())).append(' ');
		String[] actions = widget.getActions();
		if (actions != null)
		{
			for (String action : actions)
			{
				haystack.append(cleanWidgetText(action)).append(' ');
			}
		}
		String normalized = normalize(haystack.toString());
		return normalized.contains("click here to play") || normalized.equals("play") || normalized.contains(" play ");
	}

	private void captureImage(String captureType, boolean addFrame, Rectangle crop)
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGIN_SCREEN)
			{
				lastEvent.set("capture-blocked:login-screen@" + Instant.now());
				log.info("CV Helper screenshot blocked because the client is on the login screen");
				SwingUtilities.invokeLater(() ->
				{
					if (panel != null)
					{
						panel.updateStatus("Screenshot blocked: login screen");
					}
				});
				return;
			}

			lastEvent.set("capturing:" + captureType + "@" + Instant.now());
			updateCaptureStatus(captureType, "queued", null, crop);
			drawManager.requestNextFrameListener(image ->
			{
				BufferedImage screenshot = toBufferedImage(image);
				if (crop != null)
				{
					screenshot = cropImage(screenshot, crop);
				}
				try
				{
					File savedFile = saveCapture(captureType, screenshot);
					lastEvent.set("saved:" + captureType + "@" + savedFile.getAbsolutePath());
					updateCaptureStatus(captureType, "saved", savedFile, crop);
					log.info("CV Helper {} capture saved to {}", captureType, savedFile.getAbsolutePath());
					sendWebhook(eventPayload("capture-saved", capturePayload(captureType, crop)));
					SwingUtilities.invokeLater(() ->
					{
						if (panel != null)
						{
							panel.updateStatus("Captured " + captureType + ": " + savedFile.getAbsolutePath());
						}
					});
				}
				catch (IOException e)
				{
					lastEvent.set("capture-error:" + captureType + ":" + e.getMessage());
					updateCaptureStatus(captureType, "error:" + e.getMessage(), null, crop);
					log.warn("CV Helper {} capture failed", captureType, e);
					SwingUtilities.invokeLater(() ->
					{
						if (panel != null)
						{
							panel.updateStatus("Capture failed: " + captureType);
						}
					});
				}
			});
		});
	}

	private BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(34, 139, 230));
		g.fillRoundRect(1, 1, 14, 14, 4, 4);
		g.setColor(Color.WHITE);
		g.drawString("CV", 2, 12);
		g.dispose();
		return image;
	}

	private void startServer()
	{
		if (!config.enableLocalExport() && !Boolean.getBoolean(FORCE_LOCAL_EXPORT_PROPERTY))
		{
			lastEvent.set("server-disabled");
			log.info("CV Helper local export disabled by config");
			updatePanelServerStatus();
			return;
		}
		if (!config.enableLocalExport())
		{
			log.info("CV Helper local export enabled by -D{}", FORCE_LOCAL_EXPORT_PROPERTY);
		}
		if (server != null)
		{
			return;
		}

		try
		{
			int preferredPort = config.localPort() > 0 ? config.localPort() : DEFAULT_LOCAL_PORT;
			try
			{
				server = HttpServer.create(new InetSocketAddress("127.0.0.1", preferredPort), 0);
			}
			catch (IOException preferredPortFailure)
			{
				log.warn("CV Helper preferred port {} unavailable, falling back to an open port", preferredPort, preferredPortFailure);
				server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			}
			server.createContext("/status", this::handleStatusRequest);
			server.createContext("/player/status", this::handlePlayerStatusRequest);
			server.createContext("/targets/prayer", this::handlePrayerTargetsRequest);
			server.createContext("/targets/spell", this::handleSpellTargetsRequest);
			server.createContext("/targets/minimap", exchange -> handleTargetsRequest(exchange, "minimap", this::collectMinimapTargets));
			server.createContext("/targets/inventory", exchange -> handleTargetsRequest(exchange, "inventory", this::collectInventoryTargets));
			server.createContext("/targets/equipment", exchange -> handleTargetsRequest(exchange, "equipment", this::collectEquipmentTargets));
			server.createContext("/targets/panels", exchange -> handleTargetsRequest(exchange, "panels", this::collectPanelTargets));
			server.createContext("/targets/combat", exchange -> handleTargetsRequest(exchange, "combat", this::collectCombatTargets));
			server.createContext("/targets", this::handleAllTargetsRequest);
			server.createContext("/entities", this::handleEntitiesRequest);
			server.createContext("/pathing/grid", this::handlePathingGridRequest);
			server.createContext("/scene/diagnostics", this::handleSceneDiagnosticsRequest);
			server.createContext("/automation/global/config", this::handleGlobalConfigRequest);
			server.createContext("/entities/nearest", this::handleNearestEntityRequest);
			server.createContext("/automation/mob-farmer/status", this::handleMobFarmerStatusRequest);
			server.createContext("/automation/mob-farmer/config", this::handleMobFarmerConfigRequest);
			server.createContext("/automation/mob-farmer/focus-click", this::handleMobFarmerFocusClickRequest);
			server.createContext("/automation/mob-farmer/step", this::handleMobFarmerStepRequest);
			server.createContext("/automation/mob-farmer/start", this::handleMobFarmerStartRequest);
			server.createContext("/automation/mob-farmer/stop", this::handleMobFarmerStopRequest);
			server.createContext("/automation/mining/status", exchange -> handleSkillFarmerRequest(exchange, "mining", "status"));
			server.createContext("/automation/mining/config", exchange -> handleSkillFarmerRequest(exchange, "mining", "config"));
			server.createContext("/automation/mining/step", exchange -> handleSkillFarmerRequest(exchange, "mining", "step"));
			server.createContext("/automation/mining/start", exchange -> handleSkillFarmerRequest(exchange, "mining", "start"));
			server.createContext("/automation/mining/stop", exchange -> handleSkillFarmerRequest(exchange, "mining", "stop"));
			server.createContext("/automation/woodcutting/status", exchange -> handleSkillFarmerRequest(exchange, "woodcutting", "status"));
			server.createContext("/automation/woodcutting/config", exchange -> handleSkillFarmerRequest(exchange, "woodcutting", "config"));
			server.createContext("/automation/woodcutting/step", exchange -> handleSkillFarmerRequest(exchange, "woodcutting", "step"));
			server.createContext("/automation/woodcutting/start", exchange -> handleSkillFarmerRequest(exchange, "woodcutting", "start"));
			server.createContext("/automation/woodcutting/stop", exchange -> handleSkillFarmerRequest(exchange, "woodcutting", "stop"));
			server.createContext("/automation/panic-stop", this::handlePanicStopRequest);
			server.createContext("/chat/responder", this::handleChatResponderRequest);
			server.createContext("/login/click", exchange ->
			{
				Map<String, Object> response = new LinkedHashMap<>();
				Map<String, Object> diagnostics = getLoginRecoveryDiagnostics();
				LoginRecoveryState state = detectLoginRecoveryState();
				
				response.put("ok", state == LoginRecoveryState.CLICK_TO_PLAY || state == LoginRecoveryState.DISCONNECTED);
				response.putAll(diagnostics);
				
				String reason = null;
				String nextAction = null;
				boolean manualRequired = false;
				
				switch (state)
				{
					case IN_GAME:
						reason = "already-logged-in";
						nextAction = "none";
						break;
					case WORLD_SWITCH_REQUIRED:
						reason = (String) diagnostics.get("worldBlockReason");
						int fallbackWorld = (int) diagnostics.get("selectedFallbackWorld");
						nextAction = "switching-to-world-" + fallbackWorld;
						manualRequired = false;
						switchToWorld(fallbackWorld);
						break;
					case CLICK_TO_PLAY:
						reason = "click-to-play-widget-visible";
						nextAction = "click-login-widget";
						clickLoginScreen();
						break;
					case DISCONNECTED:
						reason = "connection-lost-screen";
						nextAction = "press-enter-to-reconnect";
						clickLoginScreen();
						break;
					case LOGIN_SCREEN:
						// Saved-credential / "click here to play" login art is drawn by the
						// client and is NOT exposed as a clickable widget, so findLoginClickWidget
						// returns null and we used to give up here. But this is exactly the case
						// the user wants automated: when the world is allowed, click the play-area
						// centre (sidebar/viewport-aware) + Enter via clickLoginScreen's fallback.
						// Only genuinely manual states (authenticator) below stay manual-required.
						if (mobFarmerLoginWorldAllowed())
						{
							reason = "login-screen-click-to-play";
							nextAction = "click-canvas-center";
							manualRequired = false;
							clickLoginScreen();
						}
						else
						{
							reason = "login-widget-not-visible";
							nextAction = "manual-auth-required";
							manualRequired = true;
						}
						break;
					case LOADING:
						reason = "client-loading";
						nextAction = "wait";
						break;
					case AUTH_REQUIRED_MANUAL:
						reason = "authenticator-required";
						nextAction = "manual-auth-required";
						manualRequired = true;
						break;
					default:
						reason = "unknown-login-state";
						nextAction = "manual-intervention-required";
						manualRequired = true;
						break;
				}
				
				response.put("reason", reason);
				response.put("nextAction", nextAction);
				response.put("manualRequired", manualRequired);
				
				int statusCode = (state == LoginRecoveryState.CLICK_TO_PLAY || state == LoginRecoveryState.DISCONNECTED
					|| (state == LoginRecoveryState.LOGIN_SCREEN && !manualRequired)) ? 202 : 200;
				writeJson(exchange, statusCode, response);
			});
			server.createContext("/capture", exchange ->
			{
				captureScreenshot();
				writeResponse(exchange, 202, "{\"ok\":true,\"queued\":true,\"capture\":\"client-frame\"}");
			});
			server.createContext("/capture/screen", exchange ->
			{
				captureScreen();
				writeResponse(exchange, 202, "{\"ok\":true,\"queued\":true,\"capture\":\"screen\"}");
			});
			server.createContext("/capture/minimap", exchange ->
			{
				captureMinimap();
				writeResponse(exchange, 202, "{\"ok\":true,\"queued\":true,\"capture\":\"minimap\"}");
			});
			server.createContext("/capture/latest/client-frame", exchange -> handleLatestCaptureImageRequest(exchange, "client-frame"));
			server.createContext("/capture/latest/screen", exchange -> handleLatestCaptureImageRequest(exchange, "screen"));
			server.createContext("/capture/latest/minimap", exchange -> handleLatestCaptureImageRequest(exchange, "minimap"));
			// Daemon handler threads so in-flight requests can never keep the JVM alive on close.
			server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r ->
			{
				Thread t = new Thread(r, "cvhelper-http");
				t.setDaemon(true);
				return t;
			}));
			server.start();
			int port = server.getAddress().getPort();
			lastEvent.set("server@" + port);
			log.info("CV Helper local export listening on http://127.0.0.1:{}/status", port);
			sendWebhook(eventPayload("server-started", getServerStatusText()));
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.updateStatus("Export: http://127.0.0.1:" + port + "/status");
				}
			});
			updatePanelServerStatus();
		}
		catch (IOException e)
		{
			lastEvent.set("server-error:" + e.getMessage());
			log.warn("Unable to start CV Helper local export", e);
		}
	}

	private void stopServer()
	{
		if (server != null)
		{
			log.info("CV Helper local export stopped");
			server.stop(0);
			server = null;
		}
		updatePanelServerStatus();
	}

	private void handleStatusRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("plugin", "CV Helper");
			body.put("status", lastEvent.get());
			body.put("port", localPort());
			body.put("preferredPort", config.localPort() > 0 ? config.localPort() : DEFAULT_LOCAL_PORT);
			body.put("endpoints", new String[]{"/status", "/login/click", "/capture", "/capture/screen", "/capture/minimap", "/capture/latest/client-frame", "/capture/latest/screen", "/capture/latest/minimap", "/player/status", "/targets/prayer", "/targets/spell", "/targets/minimap", "/targets/inventory", "/targets/equipment", "/targets/panels", "/targets/combat", "/targets", "/entities", "/entities/nearest", "/automation/mob-farmer/status", "/automation/mob-farmer/config", "/automation/mob-farmer/focus-click", "/automation/mob-farmer/step", "/automation/mob-farmer/start", "/automation/mob-farmer/stop", "/automation/mining/status", "/automation/mining/config", "/automation/mining/step", "/automation/mining/start", "/automation/mining/stop", "/automation/woodcutting/status", "/automation/woodcutting/config", "/automation/woodcutting/step", "/automation/woodcutting/start", "/automation/woodcutting/stop", "/automation/panic-stop", "/chat/responder"});
			Map<String, Object> playerStatus = getPlayerStatusOnClientThread();
			body.put("player", playerStatus);
			body.put("spellbook", playerStatus.get("spellbook"));
			body.put("interfaces", playerStatus.get("interfaces"));
			body.put("vitals", playerStatus.get("vitals"));
			body.put("wealth", playerStatus.get("wealth"));
			body.put("automation", playerStatus.get("automation"));
			body.put("selectedWidget", playerStatus.get("selectedWidget"));
			body.put("captures", captureStatuses());
			body.put("prayerTargets", lastPrayerTargets.size());
			body.put("spellTargets", lastSpellTargets.size());
			body.put("minimapTargets", lastMinimapTargets.size());
			body.put("inventoryTargets", lastInventoryTargets.size());
			body.put("equipmentTargets", lastEquipmentTargets.size());
			body.put("panelTargets", lastPanelTargets.size());
			body.put("combatTargets", lastCombatTargets.size());
			body.put("entities", lastEntities.size());
			body.put("targetSnapshots", snapshotStatuses());
			body.put("loginRecovery", getLoginRecoveryDiagnosticsOnClientThread());
			body.put("chatResponder", chatResponderService.getStatus());
			body.put("antiIdle", getAntiIdleStatus());
			body.put("hotkeyGuard", hotkeyGuardStatus());
			writeJson(exchange, 200, body);
		}
		catch (RuntimeException e)
		{
			log.warn("CV Helper status request failed", e);
			writeResponse(exchange, 500, "{\"error\":\"status-failed\"}");
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handlePlayerStatusRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("surface", "player");
			body.put("generatedAt", Instant.now().toString());
			body.put("status", getPlayerStatusOnClientThread());
			writeJson(exchange, 200, body);
		}
		catch (RuntimeException e)
		{
			log.warn("CV Helper player status request failed", e);
			writeResponse(exchange, 500, "{\"error\":\"player-status-failed\"}");
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleChatResponderRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("surface", "chat-responder");
			body.put("generatedAt", Instant.now().toString());
			body.put("chatResponder", chatResponderService.getStatus());
			writeJson(exchange, 200, body);
		}
		catch (RuntimeException e)
		{
			log.warn("CV Helper chat responder request failed", e);
			writeResponse(exchange, 500, "{\"error\":\"chat-responder-failed\"}");
		}
	}

	private void handlePrayerTargetsRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			Map<String, Object> snapshot = collectPrayerTargetsOnClientThread();
			writeJson(exchange, 200, snapshot);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleSpellTargetsRequest(HttpExchange exchange) throws IOException
	{
		handleTargetsRequest(exchange, "spell", this::collectSpellTargets);
	}

	private void handleTargetsRequest(HttpExchange exchange, String surface, java.util.function.Supplier<List<Map<String, Object>>> collector) throws IOException
	{
		try
		{
			Map<String, Object> snapshot = collectTargetsOnClientThread(surface, collector);
			writeJson(exchange, 200, snapshot);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleAllTargetsRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			writeJson(exchange, 200, collectAllTargetsOnClientThread());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleEntitiesRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			writeJson(exchange, 200, collectEntitiesOnClientThread());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleNearestEntityRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			writeJson(exchange, 200, collectNearestEntityOnClientThread());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	/**
	 * Every tile within {@code radius} of the player, reachable or not, with its
	 * path distance and (when blocked) the reason -- the base layer for the
	 * WebHelper minimap grid. Candidates/footprints/selected target are layered on
	 * top by the frontend from the existing skill/mob status payloads.
	 */
	private void handlePathingGridRequest(HttpExchange exchange) throws IOException
	{
		int radius = 12;
		try
		{
			String raw = queryParam(exchange, "radius");
			if (!raw.isEmpty())
			{
				radius = Math.max(4, Math.min(30, Integer.parseInt(raw.trim())));
			}
		}
		catch (NumberFormatException ignored)
		{
			// keep default radius
		}
		try
		{
			writeJson(exchange, 200, getPathingGridOnClientThread(radius));
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private Map<String, Object> getPathingGridOnClientThread(int radius) throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			result.set(pathfinding.buildReachabilityGrid(client.getLocalPlayer(), radius));
			latch.countDown();
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("error", "client-thread-timeout");
			timeout.put("tiles", new ArrayList<>());
			return timeout;
		}
		return result.get();
	}

	/**
	 * General scene scan, independent of any farmer's running state or configured
	 * radius -- the "Scene" lens for the standalone minimap/grid view. Every named
	 * object within the requested radius is returned, not just ones matching a
	 * farmer's target string, so the grid is never artificially sparse when no
	 * farmer happens to be running or targeting the right thing.
	 */
	private void handleSceneDiagnosticsRequest(HttpExchange exchange) throws IOException
	{
		int radius = 15;
		try
		{
			String raw = queryParam(exchange, "radius");
			if (!raw.isEmpty())
			{
				radius = Math.max(1, Math.min(30, Integer.parseInt(raw.trim())));
			}
		}
		catch (NumberFormatException ignored)
		{
			// keep default radius
		}
		try
		{
			writeJson(exchange, 200, getSceneDiagnosticsOnClientThread(radius));
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private Map<String, Object> getSceneDiagnosticsOnClientThread(int radius) throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			Player localPlayer = client.getLocalPlayer();
			Map<String, Object> out = new LinkedHashMap<>();
			List<Map<String, Object>> objects = collectSceneObjects(localPlayer, radius);
			out.put("surface", "scene");
			out.put("radius", radius);
			out.put("candidates", objects);
			out.put("candidateSummary", Map.of(
				"totalCandidates", objects.size(),
				"matchedReachable", (int) objects.stream().filter(o -> Boolean.TRUE.equals(o.get("reachable"))).count()
			));
			if (localPlayer != null && localPlayer.getWorldLocation() != null)
			{
				out.put("player", pointValue(localPlayer.getWorldLocation()));
			}
			result.set(out);
			latch.countDown();
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("error", "client-thread-timeout");
			timeout.put("candidates", new ArrayList<>());
			return timeout;
		}
		return result.get();
	}

	/**
	 * Global/shared CV Helper settings (overlay toggles, local export, anti-idle) --
	 * deliberately separate from any farmer's own config so shared knobs are never
	 * confused with farmer-specific ones. Same draft-editor contract (version 1,
	 * settings/schema) as the farmer config endpoints.
	 */
	private void handleGlobalConfigRequest(HttpExchange exchange) throws IOException
	{
		if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeResponse(exchange, 204, "");
			return;
		}
		if ("GET".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeJson(exchange, 200, globalConfigPayload());
			return;
		}
		if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeResponse(exchange, 405, "{\"error\":\"method-not-allowed\"}");
			return;
		}
		String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		Map<String, Object> payload;
		try
		{
			payload = gson.fromJson(raw, Map.class);
		}
		catch (RuntimeException e)
		{
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("ok", false);
			error.put("applied", false);
			error.put("errors", new String[]{"Invalid JSON: " + e.getMessage()});
			writeJson(exchange, 400, error);
			return;
		}
		if (payload == null)
		{
			payload = new LinkedHashMap<>();
		}
		Map<String, Object> result = applyGlobalConfigPayload(payload);
		writeJson(exchange, Boolean.TRUE.equals(result.get("ok")) ? 200 : 400, result);
	}

	private Map<String, Object> globalConfigPayload()
	{
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("version", 1);
		body.put("generatedAt", Instant.now().toString());
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("showHoverOverlay", config.showHoverOverlay());
		settings.put("showWidgetInfo", config.showWidgetInfo());
		settings.put("enableLocalExport", config.enableLocalExport());
		settings.put("antiIdleEnabled", config.antiIdleEnabled());
		settings.put("antiIdleTimeoutMinutes", config.antiIdleTimeoutMinutes());
		settings.put("antiIdleInputIntervalMinutes", config.antiIdleInputIntervalMinutes());
		settings.put("antiIdleManualOverride", config.antiIdleManualOverride());
		settings.put("antiIdleRestoreMouse", config.antiIdleRestoreMouse());
		body.put("settings", settings);
		List<Map<String, Object>> schema = new ArrayList<>();
		schema.add(settingSchema("showHoverOverlay", "Show hover overlay", "boolean", "Draw the currently hovered widget bounds and coordinates.", null));
		schema.add(settingSchema("showWidgetInfo", "Show widget info", "boolean", "Display widget parent/group identifiers in the overlay.", null));
		schema.add(settingSchema("enableLocalExport", "Local export server", "boolean", "Run the local HTTP export server (WebHelper/v3 console) on this client. Disabling stops the server on next restart.", null));
		schema.add(settingSchema("antiIdleEnabled", "Anti-idle enabled", "boolean", "Send harmless periodic input to prevent the client idle-logout timer while a farmer is running.", null));
		schema.add(settingSchema("antiIdleTimeoutMinutes", "Idle timeout minutes", "number", "How long the client allows before its own idle logout would fire; anti-idle inputs before this.", null));
		schema.add(settingSchema("antiIdleInputIntervalMinutes", "Input interval minutes", "number", "How often anti-idle sends a harmless input while active.", null));
		schema.add(settingSchema("antiIdleManualOverride", "Manual override", "boolean", "Force anti-idle on/off regardless of farmer running state.", null));
		schema.add(settingSchema("antiIdleRestoreMouse", "Restore mouse position", "boolean", "Move the mouse back to its prior position after an anti-idle input.", null));
		body.put("schema", schema);
		return body;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> applyGlobalConfigPayload(Map<String, Object> payload)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		Map<String, Object> settings = payload == null ? new LinkedHashMap<>() : mapValue(payload.get("settings"));
		if (settings.isEmpty() && payload != null)
		{
			settings = payload;
		}
		List<String> errors = new ArrayList<>();
		List<Runnable> updates = new ArrayList<>();
		applyBooleanSetting(settings, "showHoverOverlay", updates, v -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_HOVER_OVERLAY, v), errors);
		applyBooleanSetting(settings, "showWidgetInfo", updates, v -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.SHOW_WIDGET_INFO, v), errors);
		applyBooleanSetting(settings, "enableLocalExport", updates, v -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.ENABLE_LOCAL_EXPORT, v), errors);
		applyBooleanSetting(settings, "antiIdleEnabled", updates, v -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.ANTI_IDLE_ENABLED, v), errors);
		applyIntSetting(settings, "antiIdleTimeoutMinutes", updates, v -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.ANTI_IDLE_TIMEOUT_MINUTES, v), errors);
		applyIntSetting(settings, "antiIdleInputIntervalMinutes", updates, v -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.ANTI_IDLE_INPUT_INTERVAL_MINUTES, v), errors);
		applyBooleanSetting(settings, "antiIdleManualOverride", updates, v -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.ANTI_IDLE_MANUAL_OVERRIDE, v), errors);
		applyBooleanSetting(settings, "antiIdleRestoreMouse", updates, v -> configManager.setConfiguration(CvHelperModConfig.GROUP, CvHelperModConfig.ANTI_IDLE_RESTORE_MOUSE, v), errors);
		if (!errors.isEmpty())
		{
			result.put("ok", false);
			result.put("applied", false);
			result.put("errors", errors);
			return result;
		}
		updates.forEach(Runnable::run);
		result.put("ok", true);
		result.put("applied", true);
		result.put("errors", errors);
		result.put("updatedSettings", updates.size());
		result.put("config", globalConfigPayload());
		return result;
	}

	private void handleMobFarmerStatusRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			writeJson(exchange, 200, getMobFarmerStatusOnClientThread());
		}
		catch (RuntimeException e)
		{
			log.warn("CV Helper mob-farmer status request failed", e);
			writeResponse(exchange, 500, "{\"error\":\"mob-farmer-status-failed\"}");
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleMobFarmerConfigRequest(HttpExchange exchange) throws IOException
	{
		if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeResponse(exchange, 204, "");
			return;
		}
		if ("GET".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeJson(exchange, 200, mobFarmerConfigPayload());
			return;
		}
		if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeResponse(exchange, 405, "{\"error\":\"method-not-allowed\"}");
			return;
		}
		String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		Map<String, Object> payload;
		try
		{
			payload = gson.fromJson(raw, Map.class);
		}
		catch (RuntimeException e)
		{
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("ok", false);
			error.put("applied", false);
			error.put("errors", new String[]{"Invalid JSON: " + e.getMessage()});
			writeJson(exchange, 400, error);
			return;
		}
		if (payload == null)
		{
			payload = new LinkedHashMap<>();
		}
		Map<String, Object> result = applyMobFarmerConfigPayload(payload);
		writeJson(exchange, Boolean.TRUE.equals(result.get("ok")) ? 200 : 400, result);
	}

	private void handleMobFarmerFocusClickRequest(HttpExchange exchange) throws IOException
	{
		if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeResponse(exchange, 204, "");
			return;
		}
		runMobFarmerStartupFocusClick();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("action", "mob-farmer-focus-click");
		try
		{
			body.put("mobFarmer", getMobFarmerStatusOnClientThread());
			writeJson(exchange, 202, body);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleMobFarmerStepRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			applyMobFarmerQuery(exchange);
			boolean live = Boolean.parseBoolean(queryParam(exchange, "live"));
			runMobFarmerStep(live);
			writeJson(exchange, 202, getMobFarmerStatusOnClientThread());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleMobFarmerStartRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			applyMobFarmerQuery(exchange);
			boolean live = Boolean.parseBoolean(queryParam(exchange, "live"));
			startMobFarmer(live);
			writeJson(exchange, 202, getMobFarmerStatusOnClientThread());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleMobFarmerStopRequest(HttpExchange exchange) throws IOException
	{
		try
		{
			stopMobFarmer();
			writeJson(exchange, 202, getMobFarmerStatusOnClientThread());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handlePanicStopRequest(HttpExchange exchange) throws IOException
	{
		panicStop();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("action", "panic-stop");
		try
		{
			body.put("mobFarmer", getMobFarmerStatusOnClientThread());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
			return;
		}
		writeJson(exchange, 202, body);
	}

	private void handleSkillFarmerRequest(HttpExchange exchange, String skill, String action) throws IOException
	{
		try
		{
			if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
			{
				writeResponse(exchange, 204, "");
				return;
			}
			if ("config".equals(action))
			{
				handleSkillFarmerConfigRequest(exchange, skill);
				return;
			}
			applySkillFarmerQuery(exchange, skill);
			if ("status".equals(action))
			{
				writeJson(exchange, 200, getSkillFarmerStatusOnClientThread(skill));
				return;
			}
			if ("step".equals(action))
			{
				writeJson(exchange, 202, runSkillFarmerActionOnClientThread(skill, "step", Boolean.parseBoolean(queryParam(exchange, "live"))));
				return;
			}
			if ("start".equals(action))
			{
				writeJson(exchange, 202, runSkillFarmerActionOnClientThread(skill, "start", Boolean.parseBoolean(queryParam(exchange, "live"))));
				return;
			}
			if ("stop".equals(action))
			{
				writeJson(exchange, 202, runSkillFarmerActionOnClientThread(skill, "stop", false));
				return;
			}
			writeResponse(exchange, 404, "{\"error\":\"unknown-skill-farmer-action\"}");
		}
		catch (RuntimeException e)
		{
			log.warn("CV Helper {} farmer {} request failed", skill, action, e);
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("error", "skill-farmer-request-failed");
			error.put("skill", skill);
			error.put("action", action);
			error.put("message", e.getMessage());
			writeJson(exchange, 500, error);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			writeResponse(exchange, 503, "{\"error\":\"interrupted\"}");
		}
	}

	private void handleSkillFarmerConfigRequest(HttpExchange exchange, String skill) throws IOException
	{
		if ("GET".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeJson(exchange, 200, skillFarmerConfigPayload(skill));
			return;
		}
		if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			writeResponse(exchange, 405, "{\"error\":\"method-not-allowed\"}");
			return;
		}
		String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		Map<String, Object> payload;
		try
		{
			payload = gson.fromJson(raw, Map.class);
		}
		catch (RuntimeException e)
		{
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("ok", false);
			error.put("applied", false);
			error.put("errors", new String[]{"Invalid JSON: " + e.getMessage()});
			writeJson(exchange, 400, error);
			return;
		}
		Map<String, Object> result = applySkillFarmerConfigPayload(skill, payload);
		writeJson(exchange, Boolean.TRUE.equals(result.get("ok")) ? 200 : 400, result);
	}

	private void applyMobFarmerQuery(HttpExchange exchange)
	{
		String target = queryParam(exchange, "target");
		if (target != null && !target.trim().isEmpty())
		{
			setMobFarmerTarget(target);
		}
	}

	private void applySkillFarmerQuery(HttpExchange exchange, String skill)
	{
		String target = queryParam(exchange, "target");
		if (target == null || target.trim().isEmpty())
		{
			return;
		}
		if ("mining".equals(skill))
		{
			miningFarmerTarget = target.trim();
		}
		else
		{
			woodcuttingFarmerTarget = target.trim();
		}
	}

	private void handleLatestCaptureImageRequest(HttpExchange exchange, String captureType) throws IOException
	{
		Map<String, Object> capture = lastCaptures.get(captureType);
		Object savedPath = capture == null ? null : capture.get("savedPath");
		if (!(savedPath instanceof String) || ((String) savedPath).trim().isEmpty())
		{
			writeResponse(exchange, 404, "{\"error\":\"capture-not-found\"}");
			return;
		}

		File file = new File((String) savedPath);
		if (!file.isFile())
		{
			writeResponse(exchange, 404, "{\"error\":\"capture-file-missing\"}");
			return;
		}

		writeBinary(exchange, 200, "image/png", Files.readAllBytes(file.toPath()));
	}

	private int localPort()
	{
		return server != null ? server.getAddress().getPort() : -1;
	}

	private Map<String, Object> collectPrayerTargetsOnClientThread() throws InterruptedException
	{
		return collectTargetsOnClientThread("prayer", this::collectPrayerTargets);
	}

	private Map<String, Object> getPlayerStatusOnClientThread() throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			result.set(getPlayerStatus());
			latch.countDown();
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}
		return result.get();
	}

	private Map<String, Object> getLoginRecoveryDiagnosticsOnClientThread() throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			result.set(getLoginRecoveryDiagnostics());
			latch.countDown();
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}
		return result.get();
	}

	private Map<String, Object> getSkillFarmerStatusOnClientThread(String skill) throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			try
			{
				result.set(getSkillFarmerStatus(skill));
			}
			catch (RuntimeException e)
			{
				log.warn("CV Helper skill-farmer status failed", e);
				Map<String, Object> error = new LinkedHashMap<>();
				error.put("skill", skill);
				error.put("error", "client-thread-error");
				error.put("message", e.getMessage());
				result.set(error);
			}
			finally
			{
				latch.countDown();
			}
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("skill", skill);
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}
		return result.get();
	}

	private Map<String, Object> getMobFarmerStatusOnClientThread() throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			result.set(getMobFarmerStatus());
			latch.countDown();
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}
		return result.get();
	}

	private Map<String, Object> runSkillFarmerActionOnClientThread(String skill, String action, boolean live) throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			try
			{
				if ("step".equals(action))
				{
					runSkillFarmerStep(skill, live, "http-step");
				}
				else if ("start".equals(action))
				{
					startSkillFarmer(skill, live);
				}
				else if ("stop".equals(action))
				{
					stopSkillFarmer(skill);
				}
				result.set(getSkillFarmerStatus(skill));
			}
			catch (RuntimeException e)
			{
				log.warn("CV Helper skill-farmer action failed: {} {}", skill, action, e);
				Map<String, Object> error = new LinkedHashMap<>();
				error.put("skill", skill);
				error.put("action", action);
				error.put("error", "client-thread-error");
				error.put("message", e.getMessage());
				result.set(error);
			}
			finally
			{
				latch.countDown();
			}
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("skill", skill);
			timeout.put("action", action);
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}
		return result.get();
	}

	private Map<String, Object> collectTargetsOnClientThread(String surface, java.util.function.Supplier<List<Map<String, Object>>> collector) throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();

		clientThread.invokeLater(() ->
		{
			List<Map<String, Object>> targets = collector.get();
			setLastTargets(surface, targets);
			lastEvent.set(surface + "-targets@" + targets.size() + "@" + Instant.now());
			sendWebhook(eventPayload(surface + "-targets", targets));

			Map<String, Object> snapshot = targetSnapshot(surface, targets);
			if ("spell".equals(surface))
			{
				snapshot.put("spellbook", getSpellbookStatus());
			}
			result.set(snapshot);
			latch.countDown();
		});

		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("surface", surface);
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}

		return result.get();
	}

	private Map<String, Object> collectAllTargetsOnClientThread() throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("surface", "all");
			body.put("generatedAt", Instant.now().toString());
			body.put("gameState", client.getGameState().name());
			Map<String, Object> surfaces = new LinkedHashMap<>();
			surfaces.put("prayer", targetSnapshot("prayer", collectPrayerTargets()));
			surfaces.put("spell", targetSnapshot("spell", collectSpellTargets()));
			surfaces.put("minimap", targetSnapshot("minimap", collectMinimapTargets()));
			surfaces.put("inventory", targetSnapshot("inventory", collectInventoryTargets()));
			surfaces.put("equipment", targetSnapshot("equipment", collectEquipmentTargets()));
			surfaces.put("panels", targetSnapshot("panels", collectPanelTargets()));
			surfaces.put("combat", targetSnapshot("combat", collectCombatTargets()));
			body.put("surfaces", surfaces);
			body.put("interfaces", interfaceStatus());
			body.put("player", getPlayerStatus());
			result.set(body);
			latch.countDown();
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}
		return result.get();
	}

	private Map<String, Object> collectEntitiesOnClientThread() throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			lastEntities = collectEntities();
			Map<String, Object> snapshot = new LinkedHashMap<>();
			snapshot.put("surface", "entities");
			snapshot.put("generatedAt", Instant.now().toString());
			snapshot.put("gameState", client.getGameState().name());
			snapshot.put("count", lastEntities.size());
			snapshot.put("entities", lastEntities);
			result.set(snapshot);
			latch.countDown();
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("surface", "entities");
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}
		return result.get();
	}

	private Map<String, Object> collectNearestEntityOnClientThread() throws InterruptedException
	{
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> result = new AtomicReference<>();
		clientThread.invokeLater(() ->
		{
			lastEntities = collectEntities();
			Map<String, Object> snapshot = new LinkedHashMap<>();
			snapshot.put("surface", "entities/nearest");
			snapshot.put("generatedAt", Instant.now().toString());
			snapshot.put("gameState", client.getGameState().name());
			snapshot.put("entity", nearestClickableEntity(lastEntities));
			snapshot.put("count", lastEntities.size());
			result.set(snapshot);
			latch.countDown();
		});
		if (!latch.await(1500, TimeUnit.MILLISECONDS))
		{
			Map<String, Object> timeout = new LinkedHashMap<>();
			timeout.put("surface", "entities/nearest");
			timeout.put("error", "client-thread-timeout");
			return timeout;
		}
		return result.get();
	}

	private List<Map<String, Object>> collectPrayerTargets()
	{
		List<Map<String, Object>> targets = new ArrayList<>();
		collectDirectPrayerTargets(targets);
		Widget prayerParent = client.getWidget(ComponentID.PRAYER_PARENT);
		if (prayerParent != null)
		{
			collectWidgetTargets("prayer", prayerParent, targets);
		}

		Widget quickPrayerParent = client.getWidget(ComponentID.QUICK_PRAYER_PRAYERS);
		if (quickPrayerParent != null)
		{
			collectWidgetTargets("quickPrayer", quickPrayerParent, targets);
		}

		return dedupeTargets(targets);
	}

	private void collectDirectPrayerTargets(List<Map<String, Object>> targets)
	{
		Prayer[] prayers = Prayer.values();
		for (int i = 0; i < PRAYER_COMPONENTS.length && i < prayers.length; i++)
		{
			int before = targets.size();
			collectComponentTarget("prayer", prayers[i].name(), PRAYER_COMPONENTS[i], targets);
			if (targets.size() > before)
			{
				targets.get(targets.size() - 1).put("prayer", prayers[i].name());
			}
		}
	}

	private List<Map<String, Object>> collectSpellTargets()
	{
		Map<String, Object> spellbook = getSpellbookStatus();
		if (!enabledSpellbooks.contains(String.valueOf(spellbook.get("name"))))
		{
			return new ArrayList<>();
		}
		List<Map<String, Object>> targets = new ArrayList<>();
		Widget spellbookParent = client.getWidget(ComponentID.SPELLBOOK_PARENT);
		if (spellbookParent != null)
		{
			collectWidgetTargets("spell", spellbookParent, targets);
		}
		collectComponentTarget("spell", "Kourend Home Teleport", ComponentID.SPELLBOOK_KOUREND_HOME_TELEPORT, targets);
		collectComponentTarget("spell", "Catherby Home Teleport", ComponentID.SPELLBOOK_CATHERBY_HOME_TELEPORT, targets);
		collectComponentTarget("spell", "Lumbridge Home Teleport", ComponentID.SPELLBOOK_LUMBRIDGE_HOME_TELEPORT, targets);
		collectComponentTarget("spell", "Edgeville Home Teleport", ComponentID.SPELLBOOK_EDGEVILLE_HOME_TELEPORT, targets);
		collectComponentTarget("spell", "Lunar Home Teleport", ComponentID.SPELLBOOK_LUNAR_HOME_TELEPORT, targets);
		collectComponentTarget("spell", "Arceuus Home Teleport", ComponentID.SPELLBOOK_ARCEUUS_HOME_TELEPORT, targets);
		collectComponentTarget("spell", "Fertile Soil", ComponentID.SPELLBOOK_FERTILE_SOIL, targets);
		return targets;
	}

	private List<Map<String, Object>> collectMinimapTargets()
	{
		List<Map<String, Object>> targets = new ArrayList<>();
		collectComponentTarget("minimap", "Minimap", ComponentID.MINIMAP_CONTAINER, targets);
		collectWidgetInfoTarget("minimap", "Compass / look north", WidgetInfo.FIXED_VIEWPORT_MINIMAP, targets);
		collectWidgetInfoTarget("minimap", "Compass / look north", WidgetInfo.RESIZABLE_MINIMAP_STONES_WIDGET, targets);
		collectWidgetInfoTarget("minimap", "Compass / look north", WidgetInfo.RESIZABLE_MINIMAP_WIDGET, targets);
		collectComponentTarget("minimap", "Minimap draw area", ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA, targets);
		collectComponentTarget("minimap", "Minimap draw area", ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA, targets);
		collectComponentTarget("minimap", "Minimap draw area", ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA, targets);
		collectComponentTarget("minimap", "HP orb", ComponentID.MINIMAP_HEALTH_ORB, targets);
		collectComponentTarget("minimap", "Prayer orb", ComponentID.MINIMAP_PRAYER_ORB, targets);
		collectComponentTarget("minimap", "Quick prayer", ComponentID.MINIMAP_QUICK_PRAYER_ORB, targets);
		collectComponentTarget("minimap", "Run orb", ComponentID.MINIMAP_RUN_ORB, targets);
		collectComponentTarget("minimap", "Run toggle", ComponentID.MINIMAP_TOGGLE_RUN_ORB, targets);
		collectComponentTarget("minimap", "Special attack orb", ComponentID.MINIMAP_SPEC_ORB, targets);
		collectComponentTarget("minimap", "World map orb", ComponentID.MINIMAP_WORLDMAP_ORB, targets);
		return dedupeTargets(targets);
	}

	private List<Map<String, Object>> collectInventoryTargets()
	{
		List<Map<String, Object>> targets = new ArrayList<>();
		collectContainerTargets("inventory", client.getWidget(ComponentID.INVENTORY_CONTAINER), targets);
		collectContainerTargets("inventory", client.getWidget(ComponentID.BANK_INVENTORY_ITEM_CONTAINER), targets);
		collectContainerTargets("inventory", client.getWidget(ComponentID.EQUIPMENT_INVENTORY_ITEM_CONTAINER), targets);
		return dedupeTargets(targets);
	}

	private List<Map<String, Object>> collectEquipmentTargets()
	{
		List<Map<String, Object>> targets = new ArrayList<>();
		collectContainerTargets("equipment", client.getWidget(ComponentID.EQUIPMENT_INVENTORY_ITEM_CONTAINER), targets);
		collectContainerTargets("equipment", client.getWidget(ComponentID.BANK_EQUIPMENT_PARENT), targets);
		collectContainerTargets("equipment", client.getWidget(ComponentID.BANK_INVENTORY_EQUIPMENT_ITEM_CONTAINER), targets);
		collectComponentTarget("equipment", "Cape slot", ComponentID.EQUIPMENT_CAPE, targets);
		collectComponentTarget("equipment", "Quiver slot", ComponentID.EQUIPMENT_DIZANAS_QUIVER_ITEM_CONTAINER, targets);
		return dedupeTargets(targets);
	}

	private List<Map<String, Object>> collectPanelTargets()
	{
		List<Map<String, Object>> targets = new ArrayList<>();
		collectPanelTarget("combat options tab", WidgetInfo.FIXED_VIEWPORT_COMBAT_TAB, WidgetInfo.RESIZABLE_VIEWPORT_COMBAT_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_COMBAT_ICON, targets);
		collectPanelTarget("stats tab", WidgetInfo.FIXED_VIEWPORT_STATS_TAB, WidgetInfo.RESIZABLE_VIEWPORT_STATS_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_STATS_ICON, targets);
		collectPanelTarget("quests tab", WidgetInfo.FIXED_VIEWPORT_QUESTS_TAB, WidgetInfo.RESIZABLE_VIEWPORT_QUESTS_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_QUESTS_ICON, targets);
		collectPanelTarget("inventory tab", WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB, WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB, targets);
		collectPanelTarget("equipment tab", WidgetInfo.FIXED_VIEWPORT_EQUIPMENT_TAB, WidgetInfo.RESIZABLE_VIEWPORT_EQUIPMENT_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_EQUIPMENT_ICON, targets);
		collectPanelTarget("prayer tab", WidgetInfo.FIXED_VIEWPORT_PRAYER_TAB, WidgetInfo.RESIZABLE_VIEWPORT_PRAYER_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_PRAYER_TAB, targets);
		collectPanelTarget("magic tab", WidgetInfo.FIXED_VIEWPORT_MAGIC_TAB, WidgetInfo.RESIZABLE_VIEWPORT_MAGIC_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_MAGIC_ICON, targets);
		collectPanelTarget("friends chat tab", WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_TAB, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_CHAT_ICON, targets);
		collectPanelTarget("friends tab", WidgetInfo.FIXED_VIEWPORT_FRIENDS_TAB, WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_ICON, targets);
		collectPanelTarget("ignore tab", WidgetInfo.FIXED_VIEWPORT_IGNORES_TAB, WidgetInfo.RESIZABLE_VIEWPORT_IGNORES_TAB, targets);
		collectPanelTarget("logout tab", WidgetInfo.FIXED_VIEWPORT_LOGOUT_TAB, WidgetInfo.RESIZABLE_VIEWPORT_LOGOUT_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_LOGOUT_BUTTON, targets);
		collectPanelTarget("options tab", WidgetInfo.FIXED_VIEWPORT_OPTIONS_TAB, WidgetInfo.RESIZABLE_VIEWPORT_OPTIONS_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_OPTIONS_ICON, targets);
		collectPanelTarget("emotes tab", WidgetInfo.FIXED_VIEWPORT_EMOTES_TAB, WidgetInfo.RESIZABLE_VIEWPORT_EMOTES_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_EMOTES_ICON, targets);
		collectPanelTarget("music tab", WidgetInfo.FIXED_VIEWPORT_MUSIC_TAB, WidgetInfo.RESIZABLE_VIEWPORT_MUSIC_TAB, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_MUSIC_ICON, targets);
		collectPanelIconTargets(targets);
		if (targets.isEmpty())
		{
			collectRightRailCandidates(targets);
		}
		return dedupeTargets(targets);
	}

	private void collectRightRailCandidates(List<Map<String, Object>> targets)
	{
		Widget[] roots = client.getWidgetRoots();
		if (roots == null)
		{
			return;
		}
		for (Widget root : roots)
		{
			collectRightRailCandidates(root, targets);
		}
	}

	private void collectRightRailCandidates(Widget widget, List<Map<String, Object>> targets)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		int canvasWidth = client.getCanvasWidth();
		if (bounds != null
			&& bounds.width >= 16
			&& bounds.height >= 16
			&& bounds.width <= 80
			&& bounds.height <= 80
			&& bounds.x >= Math.max(0, canvasWidth - 320)
			&& bounds.y >= 140)
		{
			addWidgetTarget("panels", "right rail candidate", widget, targets);
		}

		for (Widget child : widget.getDynamicChildren())
		{
			collectRightRailCandidates(child, targets);
		}
		for (Widget child : widget.getStaticChildren())
		{
			collectRightRailCandidates(child, targets);
		}
		for (Widget child : widget.getNestedChildren())
		{
			collectRightRailCandidates(child, targets);
		}
	}

	private void collectPanelIconTargets(List<Map<String, Object>> targets)
	{
		collectWidgetInfoTarget("panels", "combat options tab", WidgetInfo.FIXED_VIEWPORT_COMBAT_ICON, targets);
		collectWidgetInfoTarget("panels", "combat options tab", WidgetInfo.RESIZABLE_VIEWPORT_COMBAT_ICON, targets);
		collectWidgetInfoTarget("panels", "stats tab", WidgetInfo.FIXED_VIEWPORT_STATS_ICON, targets);
		collectWidgetInfoTarget("panels", "stats tab", WidgetInfo.RESIZABLE_VIEWPORT_STATS_ICON, targets);
		collectWidgetInfoTarget("panels", "quests tab", WidgetInfo.FIXED_VIEWPORT_QUESTS_ICON, targets);
		collectWidgetInfoTarget("panels", "quests tab", WidgetInfo.RESIZABLE_VIEWPORT_QUESTS_ICON, targets);
		collectWidgetInfoTarget("panels", "inventory tab", WidgetInfo.FIXED_VIEWPORT_INVENTORY_ICON, targets);
		collectWidgetInfoTarget("panels", "inventory tab", WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_ICON, targets);
		collectWidgetInfoTarget("panels", "equipment tab", WidgetInfo.FIXED_VIEWPORT_EQUIPMENT_ICON, targets);
		collectWidgetInfoTarget("panels", "equipment tab", WidgetInfo.RESIZABLE_VIEWPORT_EQUIPMENT_ICON, targets);
		collectWidgetInfoTarget("panels", "prayer tab", WidgetInfo.FIXED_VIEWPORT_PRAYER_ICON, targets);
		collectWidgetInfoTarget("panels", "prayer tab", WidgetInfo.RESIZABLE_VIEWPORT_PRAYER_ICON, targets);
		collectWidgetInfoTarget("panels", "magic tab", WidgetInfo.FIXED_VIEWPORT_MAGIC_ICON, targets);
		collectWidgetInfoTarget("panels", "magic tab", WidgetInfo.RESIZABLE_VIEWPORT_MAGIC_ICON, targets);
		collectWidgetInfoTarget("panels", "friends chat tab", WidgetInfo.FIXED_VIEWPORT_FRIENDS_CHAT_ICON, targets);
		collectWidgetInfoTarget("panels", "friends chat tab", WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_CHAT_ICON, targets);
		collectWidgetInfoTarget("panels", "friends tab", WidgetInfo.FIXED_VIEWPORT_FRIENDS_ICON, targets);
		collectWidgetInfoTarget("panels", "friends tab", WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_ICON, targets);
		collectWidgetInfoTarget("panels", "ignore tab", WidgetInfo.FIXED_VIEWPORT_IGNORES_ICON, targets);
		collectWidgetInfoTarget("panels", "ignore tab", WidgetInfo.RESIZABLE_VIEWPORT_IGNORES_ICON, targets);
		collectWidgetInfoTarget("panels", "logout tab", WidgetInfo.FIXED_VIEWPORT_LOGOUT_ICON, targets);
		collectWidgetInfoTarget("panels", "logout tab", WidgetInfo.RESIZABLE_VIEWPORT_LOGOUT_ICON, targets);
		collectWidgetInfoTarget("panels", "options tab", WidgetInfo.FIXED_VIEWPORT_OPTIONS_ICON, targets);
		collectWidgetInfoTarget("panels", "options tab", WidgetInfo.RESIZABLE_VIEWPORT_OPTIONS_ICON, targets);
		collectWidgetInfoTarget("panels", "emotes tab", WidgetInfo.FIXED_VIEWPORT_EMOTES_ICON, targets);
		collectWidgetInfoTarget("panels", "emotes tab", WidgetInfo.RESIZABLE_VIEWPORT_EMOTES_ICON, targets);
		collectWidgetInfoTarget("panels", "music tab", WidgetInfo.FIXED_VIEWPORT_MUSIC_ICON, targets);
		collectWidgetInfoTarget("panels", "music tab", WidgetInfo.RESIZABLE_VIEWPORT_MUSIC_ICON, targets);
	}

	private List<Map<String, Object>> collectCombatTargets()
	{
		List<Map<String, Object>> targets = new ArrayList<>();
		collectWidgetInfoTarget("combat", "combat level", WidgetInfo.COMBAT_LEVEL, targets);
		collectWidgetInfoTarget("combat", "attack style 1", WidgetInfo.COMBAT_STYLE_ONE, targets);
		collectWidgetInfoTarget("combat", "attack style 2", WidgetInfo.COMBAT_STYLE_TWO, targets);
		collectWidgetInfoTarget("combat", "attack style 3", WidgetInfo.COMBAT_STYLE_THREE, targets);
		collectWidgetInfoTarget("combat", "attack style 4", WidgetInfo.COMBAT_STYLE_FOUR, targets);
		collectWidgetInfoTarget("combat", "auto retaliate", WidgetInfo.COMBAT_AUTO_RETALIATE, targets);
		collectWidgetInfoTarget("combat", "autocast spells", WidgetInfo.COMBAT_SPELLS, targets);
		collectWidgetInfoTarget("combat", "defensive autocast", WidgetInfo.COMBAT_DEFENSIVE_SPELL_BOX, targets);
		collectWidgetInfoTarget("combat", "normal autocast", WidgetInfo.COMBAT_SPELL_BOX, targets);
		return dedupeTargets(targets);
	}

	private void collectPanelTarget(String label, WidgetInfo primary, WidgetInfo secondary, List<Map<String, Object>> targets)
	{
		collectPanelTarget(label, primary, secondary, null, targets);
	}

	private void collectPanelTarget(String label, WidgetInfo primary, WidgetInfo secondary, WidgetInfo tertiary, List<Map<String, Object>> targets)
	{
		collectWidgetInfoTarget("panels", label, primary, targets);
		collectWidgetInfoTarget("panels", label, secondary, targets);
		if (tertiary != null)
		{
			collectWidgetInfoTarget("panels", label, tertiary, targets);
		}
	}

	private void collectWidgetInfoTarget(String surface, String label, WidgetInfo widgetInfo, List<Map<String, Object>> targets)
	{
		Widget widget = client.getWidget(widgetInfo);
		addWidgetTarget(surface, label, widget, targets);
	}

	private void collectContainerTargets(String surface, Widget widget, List<Map<String, Object>> targets)
	{
		if (widget == null || (!surface.contains("minimap") && widget.isHidden()))
		{
			return;
		}

		Widget[] children = widget.getDynamicChildren();
		if (children.length == 0)
		{
			children = widget.getStaticChildren();
		}
		if (children.length == 0)
		{
			children = widget.getNestedChildren();
		}
		if (children.length == 0)
		{
			if (!"inventory".equals(surface) && !"equipment".equals(surface))
			{
				addWidgetTarget(surface, surface + " container", widget, targets);
			}
			return;
		}

		for (Widget child : children)
		{
			if (isSlotSized(child))
			{
				addWidgetTarget(surface, slotLabel(surface, targets.size(), child), child, targets);
			}
		}
	}

	private String slotLabel(String surface, int slotIndex, Widget child)
	{
		if ("equipment".equals(surface))
		{
			String[] labels = {
				"head slot",
				"cape slot",
				"neck slot",
				"weapon slot",
				"body slot",
				"shield slot",
				"legs slot",
				"hands slot",
				"feet slot",
				"ring slot",
				"ammo slot",
				"equipment utility " + slotIndex
			};
			return slotIndex < labels.length ? labels[slotIndex] : "equipment slot " + slotIndex;
		}
		int widgetIndex = child.getIndex() >= 0 ? child.getIndex() : slotIndex;
		return surface + " slot " + widgetIndex;
	}

	private boolean isSlotSized(Widget widget)
	{
		Rectangle bounds = widget.getBounds();
		if (bounds == null)
		{
			return false;
		}
		return bounds.width >= 16 && bounds.height >= 16 && bounds.width <= 64 && bounds.height <= 64;
	}

	private void collectComponentTarget(String surface, String label, int componentId, List<Map<String, Object>> targets)
	{
		Widget widget = client.getWidget(componentId);
		addWidgetTarget(surface, label, widget, targets);
	}

	private void addWidgetTarget(String surface, String fallbackLabel, Widget widget, List<Map<String, Object>> targets)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}
		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}
		String[] actions = widget.getActions();
		String name = cleanWidgetText(widget.getName());
		String text = cleanWidgetText(widget.getText());
		String itemName = itemName(widget.getItemId());
		Map<String, Object> target = new LinkedHashMap<>();
		target.put("surface", surface);
		target.put("widgetId", widget.getId());
		target.put("parentId", widget.getParentId());
		target.put("index", widget.getIndex());
		target.put("type", widget.getType());
		target.put("name", name);
		target.put("text", text);
		target.put("itemName", itemName);
		target.put("actions", actions == null ? new String[0] : actions);
		target.put("inventoryActions", itemInventoryActions(widget.getItemId()));
		target.put("itemId", widget.getItemId());
		target.put("itemQuantity", widget.getItemQuantity());
		target.put("spriteId", widget.getSpriteId());
		target.put("opacity", widget.getOpacity());
		target.put("textColor", widget.getTextColor());
		target.put("clickMask", widget.getClickMask());
		if (surface.contains("spell"))
		{
			target.put("spellUnavailable", isUnavailableSpellWidget(widget, actions));
		}
		target.put("bounds", boundsMap(bounds));
		target.put("center", pointMap(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
		target.put("label", targetLabel(target, fallbackLabel));
		target.put("enabledForExport", true);
		targets.add(target);
	}

	private void collectWidgetTargets(String surface, Widget widget, List<Map<String, Object>> targets)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds != null && bounds.width > 0 && bounds.height > 0)
		{
			String[] actions = widget.getActions();
			String name = cleanWidgetText(widget.getName());
			String text = cleanWidgetText(widget.getText());
			String itemName = itemName(widget.getItemId());

			boolean hasUsefulMetadata = surface.contains("spell") || !name.isEmpty() || !text.isEmpty() || !itemName.isEmpty() || hasActions(actions);
			boolean clickableShape = bounds.width >= 8 && bounds.height >= 8;
			if (clickableShape && hasUsefulMetadata)
			{
				Map<String, Object> target = new LinkedHashMap<>();
				target.put("surface", surface);
				if (surface.contains("spell"))
				{
					target.put("spellbook", getSpellbookStatus());
				}
				target.put("widgetId", widget.getId());
				target.put("parentId", widget.getParentId());
				target.put("index", widget.getIndex());
				target.put("type", widget.getType());
				target.put("name", name);
				target.put("text", text);
				target.put("itemName", itemName);
				target.put("actions", actions == null ? new String[0] : actions);
				target.put("itemId", widget.getItemId());
				target.put("itemQuantity", widget.getItemQuantity());
				target.put("spriteId", widget.getSpriteId());
				target.put("opacity", widget.getOpacity());
				target.put("textColor", widget.getTextColor());
				target.put("clickMask", widget.getClickMask());
				if (surface.contains("spell"))
				{
					target.put("spellUnavailable", isUnavailableSpellWidget(widget, actions));
				}
				target.put("bounds", boundsMap(bounds));
				target.put("center", pointMap(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
				target.put("label", targetLabel(target, null));
				target.put("enabledForExport", isTargetEnabled(surface, target));
				if (Boolean.TRUE.equals(target.get("enabledForExport")))
				{
					targets.add(target);
				}
			}
		}

		for (Widget child : widget.getDynamicChildren())
		{
			collectWidgetTargets(surface, child, targets);
		}
		for (Widget child : widget.getStaticChildren())
		{
			collectWidgetTargets(surface, child, targets);
		}
		for (Widget child : widget.getNestedChildren())
		{
			collectWidgetTargets(surface, child, targets);
		}
	}

	private boolean hasActions(String[] actions)
	{
		if (actions == null)
		{
			return false;
		}

		for (String action : actions)
		{
			if (action != null && !action.trim().isEmpty())
			{
				return true;
			}
		}
		return false;
	}

	private boolean hasActionNamed(String[] actions, String name)
	{
		if (actions == null)
		{
			return false;
		}
		String needle = normalize(name);
		for (String action : actions)
		{
			if (normalize(action).contains(needle))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isUnavailableSpellWidget(Widget widget, String[] actions)
	{
		if (widget == null)
		{
			return false;
		}
		if (widget.getOpacity() >= 200)
		{
			return true;
		}
		return widget.getClickMask() == 0 && !hasActionNamed(actions, "Cast");
	}



	private Map<String, Object> targetSnapshot(String surface, List<Map<String, Object>> freshTargets)
	{
		boolean fresh = !freshTargets.isEmpty();
		Map<String, Object> snapshot;
		if (fresh)
		{
			snapshot = new LinkedHashMap<>();
			snapshot.put("surface", surface);
			snapshot.put("generatedAt", Instant.now().toString());
			snapshot.put("lastSeenAt", Instant.now().toString());
			snapshot.put("gameState", client.getGameState().name());
			snapshot.put("fresh", true);
			snapshot.put("cached", false);
			snapshot.put("count", freshTargets.size());
			snapshot.put("targets", freshTargets);
			targetSnapshots.put(surface, snapshot);
			setLastTargets(surface, freshTargets);
			return snapshot;
		}

		snapshot = targetSnapshots.get(surface);
		if (snapshot == null)
		{
			snapshot = new LinkedHashMap<>();
			snapshot.put("surface", surface);
			snapshot.put("lastSeenAt", null);
			snapshot.put("targets", new ArrayList<>());
		}
		Map<String, Object> cached = new LinkedHashMap<>(snapshot);
		cached.put("generatedAt", Instant.now().toString());
		cached.put("gameState", client.getGameState().name());
		cached.put("fresh", false);
		cached.put("cached", snapshot.get("targets") != null);
		Object targets = cached.get("targets");
		cached.put("count", targets instanceof List ? ((List<?>) targets).size() : 0);
		return cached;
	}

	private Map<String, Object> snapshotStatuses()
	{
		Map<String, Object> statuses = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, Object>> entry : targetSnapshots.entrySet())
		{
			Map<String, Object> status = new LinkedHashMap<>();
			status.put("lastSeenAt", entry.getValue().get("lastSeenAt"));
			status.put("count", entry.getValue().get("count"));
			statuses.put(entry.getKey(), status);
		}
		return statuses;
	}

	private Map<String, Object> captureStatuses()
	{
		return new LinkedHashMap<>(lastCaptures);
	}

	private void updateCaptureStatus(String captureType, String status, File file, Rectangle crop)
	{
		Map<String, Object> capture = new LinkedHashMap<>();
		capture.put("type", captureType);
		capture.put("status", status);
		capture.put("updatedAt", Instant.now().toString());
		capture.put("savedPath", file == null ? null : file.getAbsolutePath());
		capture.put("crop", crop == null ? null : boundsMap(crop));
		lastCaptures.put(captureType, capture);
	}

	private File saveCapture(String captureType, BufferedImage screenshot) throws IOException
	{
		File directory = captureDirectory();
		if (!directory.exists() && !directory.mkdirs())
		{
			throw new IOException("Unable to create " + directory.getAbsolutePath());
		}
		String stamp = LocalDateTime.now().format(CAPTURE_TIMESTAMP);
		File file = new File(directory, "cv-helper-" + captureType + " " + stamp + ".png");
		int suffix = 1;
		while (file.exists())
		{
			file = new File(directory, "cv-helper-" + captureType + " " + stamp + "-" + suffix + ".png");
			suffix++;
		}
		ImageIO.write(screenshot, "PNG", file);
		return file;
	}

	private File captureDirectory()
	{
		String playerName = "unknown";
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && localPlayer.getName() != null && !localPlayer.getName().trim().isEmpty())
		{
			playerName = localPlayer.getName();
		}
		return new File(new File(RuneLite.SCREENSHOT_DIR, playerName), "manual");
	}

	private Map<String, Object> skillSnapshot(Skill skill)
	{
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("real", client.getRealSkillLevel(skill));
		snapshot.put("boosted", client.getBoostedSkillLevel(skill));
		snapshot.put("experience", client.getSkillExperience(skill));
		return snapshot;
	}

	private Map<String, Object> allSkillSnapshots()
	{
		Map<String, Object> skills = new LinkedHashMap<>();
		for (Skill skill : Skill.values())
		{
			skills.put(skill.name().toLowerCase(), skillSnapshot(skill));
		}
		return skills;
	}

	private Map<String, Object> prayerStatus()
	{
		Map<String, Object> prayers = new LinkedHashMap<>();
		for (Prayer prayer : Prayer.values())
		{
			Map<String, Object> state = new LinkedHashMap<>();
			state.put("active", client.isPrayerActive(prayer));
			state.put("enabledForExport", enabledPrayers.contains(prayer.name()));
			state.put("varbit", prayer.getVarbit());
			prayers.put(prayer.name(), state);
		}
		return prayers;
	}

	private Map<String, Object> interfaceStatus()
	{
		Map<String, Object> interfaces = new LinkedHashMap<>();
		interfaces.put("combatVisible", widgetVisible(WidgetInfo.COMBAT_STYLE_ONE));
		interfaces.put("inventoryVisible", widgetVisible(ComponentID.INVENTORY_CONTAINER));
		interfaces.put("equipmentVisible", widgetVisible(ComponentID.EQUIPMENT_INVENTORY_ITEM_CONTAINER) || widgetVisible(ComponentID.BANK_EQUIPMENT_PARENT));
		interfaces.put("prayerVisible", widgetVisible(ComponentID.PRAYER_PARENT) || widgetVisible(ComponentID.QUICK_PRAYER_PRAYERS));
		interfaces.put("spellbookVisible", widgetVisible(ComponentID.SPELLBOOK_PARENT));
		interfaces.put("minimapVisible", widgetVisible(ComponentID.MINIMAP_CONTAINER));
		String activeSidePanel = activeSidePanelName(interfaces);
		if (isKnownSidePanel(activeSidePanel))
		{
			lastStableSidePanel = activeSidePanel;
		}
		interfaces.put("activeSidePanel", activeSidePanel);
		interfaces.put("lastStableSidePanel", lastStableSidePanel);
		return interfaces;
	}

	private Map<String, Object> vitalStatus()
	{
		Map<String, Object> vitals = new LinkedHashMap<>();
		vitals.put("hitpoints", skillSnapshot(Skill.HITPOINTS));
		vitals.put("prayer", skillSnapshot(Skill.PRAYER));
		vitals.put("runEnergyRaw", client.getEnergy());
		vitals.put("runEnergyPercent", client.getEnergy() / 100.0);
		vitals.put("runEnabled", runEnabled());
		int specialRaw = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);
		vitals.put("specialAttackRaw", specialRaw);
		vitals.put("specialAttackPercent", specialRaw / 10.0);
		vitals.put("specialAttackEnabled", client.getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED) == 1);
		vitals.put("weight", client.getWeight());
		List<String> activePrayers = new ArrayList<>();
		for (Prayer prayer : Prayer.values())
		{
			if (client.isPrayerActive(prayer))
			{
				activePrayers.add(friendlyName(prayer.name()));
			}
		}
		vitals.put("activePrayers", activePrayers);
		vitals.put("prayerActive", !activePrayers.isEmpty());
		return vitals;
	}

	private Map<String, Object> selectedWidgetStatus()
	{
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("selected", client.isWidgetSelected());
		Widget selectedWidget = client.getSelectedWidget();
		if (selectedWidget != null)
		{
			status.put("widgetId", selectedWidget.getId());
			status.put("name", selectedWidget.getName());
			status.put("text", selectedWidget.getText());
			status.put("actions", selectedWidget.getActions());
		}
		return status;
	}

	private Map<String, Object> wealthStatus()
	{
		Map<String, Object> inventory = containerValue("inventory", InventoryID.INVENTORY);
		Map<String, Object> equipment = containerValue("equipment", InventoryID.EQUIPMENT);
		long inventoryGe = longValue(inventory.get("gePrice"));
		long equipmentGe = longValue(equipment.get("gePrice"));
		long inventoryHa = longValue(inventory.get("haPrice"));
		long equipmentHa = longValue(equipment.get("haPrice"));

		Map<String, Object> wealth = new LinkedHashMap<>();
		wealth.put("inventory", inventory);
		wealth.put("equipment", equipment);
		wealth.put("currentLootValueGe", inventoryGe);
		wealth.put("currentLootValueHa", inventoryHa);
		wealth.put("totalCarriedValueGe", inventoryGe + equipmentGe);
		wealth.put("totalCarriedValueHa", inventoryHa + equipmentHa);
		wealth.put("riskedValueGeApprox", inventoryGe + equipmentGe);
		wealth.put("riskedValueHaApprox", inventoryHa + equipmentHa);
		wealth.put("riskModel", "coarse-total-carried");
		return wealth;
	}

	private Map<String, Object> automationStatus()
	{
		Map<String, Object> automation = new LinkedHashMap<>();
		automation.put("mobFarmer", getMobFarmerStatus());
		automation.put("mining", getSkillFarmerStatus("mining"));
		automation.put("woodcutting", getSkillFarmerStatus("woodcutting"));
		return automation;
	}

	private Map<String, Object> containerValue(String name, InventoryID inventoryId)
	{
		Map<String, Object> summary = new LinkedHashMap<>();
		List<Map<String, Object>> items = new ArrayList<>();
		long gePrice = 0;
		long haPrice = 0;
		int occupiedSlots = 0;
		ItemContainer container = client.getItemContainer(inventoryId);
		Item[] containerItems = container == null ? new Item[0] : container.getItems();
		for (int slot = 0; slot < containerItems.length; slot++)
		{
			Item item = containerItems[slot];
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			occupiedSlots++;
			ItemComposition composition = itemManager.getItemComposition(item.getId());
			int itemGe = itemManager.getItemPrice(item.getId());
			int itemHa = composition == null ? 0 : composition.getHaPrice();
			long stackGe = (long) itemGe * item.getQuantity();
			long stackHa = (long) itemHa * item.getQuantity();
			gePrice += stackGe;
			haPrice += stackHa;

			Map<String, Object> itemStatus = new LinkedHashMap<>();
			itemStatus.put("slot", slot);
			itemStatus.put("id", item.getId());
			itemStatus.put("name", composition == null ? "" : composition.getName());
			itemStatus.put("quantity", item.getQuantity());
			itemStatus.put("gePriceEach", itemGe);
			itemStatus.put("haPriceEach", itemHa);
			itemStatus.put("gePrice", stackGe);
			itemStatus.put("haPrice", stackHa);
			items.add(itemStatus);
		}

		summary.put("name", name);
		summary.put("containerId", inventoryId.getId());
		summary.put("slotCount", inventorySlotCount(inventoryId, containerItems));
		summary.put("occupiedSlots", occupiedSlots);
		summary.put("gePrice", gePrice);
		summary.put("haPrice", haPrice);
		summary.put("items", items);
		return summary;
	}

	private int inventorySlotCount(InventoryID inventoryId, Item[] containerItems)
	{
		if (inventoryId == InventoryID.INVENTORY)
		{
			return 28;
		}
		if (inventoryId == InventoryID.EQUIPMENT)
		{
			return 14;
		}
		return containerItems.length;
	}




	private String activeSidePanelName(Map<String, Object> interfaces)
	{
		if (Boolean.TRUE.equals(interfaces.get("inventoryVisible")))
		{
			return "inventory";
		}
		if (Boolean.TRUE.equals(interfaces.get("equipmentVisible")))
		{
			return "equipment";
		}
		if (Boolean.TRUE.equals(interfaces.get("prayerVisible")))
		{
			return "prayer";
		}
		if (Boolean.TRUE.equals(interfaces.get("spellbookVisible")))
		{
			return "spellbook";
		}
		if (Boolean.TRUE.equals(interfaces.get("combatVisible")))
		{
			return "combat";
		}
		return "unknown";
	}

	private boolean widgetVisible(int componentId)
	{
		Widget widget = client.getWidget(componentId);
		return widget != null && !widget.isHidden();
	}

	private boolean widgetVisible(WidgetInfo widgetInfo)
	{
		Widget widget = client.getWidget(widgetInfo);
		return widget != null && !widget.isHidden();
	}

	private List<Map<String, Object>> collectEntities()
	{
		List<Map<String, Object>> entities = new ArrayList<>();
		Player localPlayer = client.getLocalPlayer();
		for (Player player : client.getPlayers())
		{
			addActorEntity(entities, "player", player, localPlayer);
		}
		for (NPC npc : client.getNpcs())
		{
			addActorEntity(entities, "npc", npc, localPlayer);
		}
		return entities;
	}

	private void addActorEntity(List<Map<String, Object>> entities, String type, Actor actor, Player localPlayer)
	{
		Map<String, Object> entity = actorEntity(type, actor, localPlayer);
		if (entity != null)
		{
			entities.add(entity);
		}
	}

	private Map<String, Object> actorEntity(String type, Actor actor, Player localPlayer)
	{
		if (actor == null || actor.getName() == null)
		{
			return null;
		}
		WorldPoint origin = localPlayer == null ? null : localPlayer.getWorldLocation();
		Map<String, Object> canvasBounds = actorBounds(actor);
		Map<String, Object> boundsCenter = centerFromBounds(canvasBounds);
		Map<String, Object> tileCenter = canvasTileCenter(actor);
		Map<String, Object> entity = new LinkedHashMap<>();
		entity.put("type", type);
		entity.put("name", actor.getName());
		entity.put("combatLevel", actor.getCombatLevel());
		entity.put("animation", actor.getAnimation());
		entity.put("poseAnimation", actor.getPoseAnimation());
		entity.put("healthRatio", actor.getHealthRatio());
		entity.put("healthScale", actor.getHealthScale());
		entity.put("dead", actor.isDead());
		entity.put("effectivelyDead", isEffectivelyDead(actor));
		entity.put("inCombat", actor.getInteracting() != null || actor.isInteracting());
		entity.put("interacting", actorSummary(actor.getInteracting()));
		entity.put("worldLocation", pointValue(actor.getWorldLocation()));
		entity.put("playerWorldLocation", origin == null ? null : pointValue(origin));
		entity.put("localLocation", pointValue(actor.getLocalLocation()));
		entity.put("worldArea", worldAreaMap(actor.getWorldArea()));
		entity.put("canvasBounds", canvasBounds);
		entity.put("center", boundsCenter);
		entity.put("canvasTileCenter", tileCenter);
		entity.put("clickPoint", boundsCenter != null ? boundsCenter : tileCenter);
		if (origin != null && actor.getWorldLocation() != null)
		{
			entity.put("distance", origin.distanceTo(actor.getWorldLocation()));
		}
		if (actor instanceof NPC)
		{
			NPC npc = (NPC) actor;
			int attackActionIndex = npcAttackActionIndex(npc);
			entity.put("id", npc.getId());
			entity.put("index", npc.getIndex());
			entity.put("attackable", attackActionIndex >= 0);
			entity.put("attackActionIndex", attackActionIndex);
			MenuAction attackMenuAction = npcMenuActionForIndex(attackActionIndex);
			entity.put("attackMenuAction", attackMenuAction == null ? null : attackMenuAction.name());
			NPCComposition composition = npc.getTransformedComposition();
			if (composition == null)
			{
				composition = npc.getComposition();
			}
			entity.put("actions", composition == null || composition.getActions() == null ? new String[0] : composition.getActions());
			entity.put("interactible", composition != null && composition.isInteractible());
			entity.put("compositionId", composition == null ? npc.getId() : composition.getId());
		}
		return entity;
	}

	private Map<String, Object> actorBounds(Actor actor)
	{
		if (actor == null)
		{
			return null;
		}
		Shape hull = actor.getConvexHull();
		if (hull == null)
		{
			return null;
		}
		return boundsMap(hull.getBounds());
	}

	private Map<String, Object> actorSummary(Actor actor)
	{
		if (actor == null)
		{
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("type", actor instanceof NPC ? "npc" : actor instanceof Player ? "player" : "actor");
		out.put("name", actor.getName());
		out.put("combatLevel", actor.getCombatLevel());
		out.put("healthRatio", actor.getHealthRatio());
		out.put("healthScale", actor.getHealthScale());
		out.put("dead", actor.isDead());
		out.put("worldLocation", pointValue(actor.getWorldLocation()));
		if (actor instanceof NPC)
		{
			NPC npc = (NPC) actor;
			out.put("id", npc.getId());
			out.put("index", npc.getIndex());
		}
		return out;
	}

	private Map<String, Object> worldAreaMap(WorldArea area)
	{
		if (area == null)
		{
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("x", area.getX());
		out.put("y", area.getY());
		out.put("width", area.getWidth());
		out.put("height", area.getHeight());
		out.put("plane", area.getPlane());
		return out;
	}

	private Map<String, Object> canvasTileCenter(Actor actor)
	{
		if (actor == null || actor.getLocalLocation() == null)
		{
			return null;
		}
		return pointValue(Perspective.localToCanvas(client, actor.getLocalLocation(), client.getPlane()));
	}

	private Map<String, Object> centerFromBounds(Map<String, Object> bounds)
	{
		if (bounds == null)
		{
			return null;
		}
		Number x = (Number) bounds.get("x");
		Number y = (Number) bounds.get("y");
		Number width = (Number) bounds.get("width");
		Number height = (Number) bounds.get("height");
		if (x == null || y == null || width == null || height == null)
		{
			return null;
		}
		return pointMap(x.intValue() + width.intValue() / 2, y.intValue() + height.intValue() / 2);
	}

	private Map<String, Object> nearestClickableEntity(List<Map<String, Object>> entities)
	{
		Map<String, Object> nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (Map<String, Object> entity : entities)
		{
			if (!(entity.get("clickPoint") instanceof Map))
			{
				continue;
			}
			Object distanceValue = entity.get("distance");
			int distance = distanceValue instanceof Number ? ((Number) distanceValue).intValue() : Integer.MAX_VALUE;
			if (nearest == null || distance < nearestDistance)
			{
				nearest = entity;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	Map<String, Object> getSpellbookStatus()
	{
		Map<String, Object> status = new LinkedHashMap<>();
		int spellbookVarbit = safeValue(() -> client.getVarbitValue(VarbitID.SPELLBOOK), -1);
		int spellbookSublist = safeValue(() -> client.getVarbitValue(VarbitID.SPELLBOOK_SUBLIST), -1);
		int subSpellbookEnum = safeValue(() -> client.getEnum(EnumID.SPELLBOOKS_SUB).getIntValue(spellbookVarbit), -1);
		int spellbookId = safeValue(() -> subSpellbookEnum < 0 ? -1 : client.getEnum(subSpellbookEnum).getIntValue(spellbookSublist), -1);
		status.put("name", spellbookName(spellbookVarbit, spellbookId));
		status.put("varbit", spellbookVarbit);
		status.put("sublist", spellbookSublist);
		status.put("subSpellbookEnum", subSpellbookEnum);
		status.put("spellbookId", spellbookId);
		status.put("visible", client.getWidget(ComponentID.SPELLBOOK_PARENT) != null);
		return status;
	}

	List<String> getPrayerNames()
	{
		List<String> prayers = new ArrayList<>();
		for (Prayer prayer : Prayer.values())
		{
			prayers.add(prayer.name());
		}
		return prayers;
	}

	List<String> getSpellbookNames()
	{
		return Arrays.asList(
			"Standard",
			"Ancient",
			"Lunar",
			"Arceuus"
		);
	}

	List<String> getSuggestedActionTargets(CvHelperActionSurface surface)
	{
		Set<String> labels = new java.util.TreeSet<>();
		if (surface == CvHelperActionSurface.PRAYER)
		{
			for (String prayer : getPrayerNames())
			{
				labels.add(friendlyName(prayer));
			}
		}
		else if (surface == CvHelperActionSurface.SPELL)
		{
			labels.addAll(Arrays.asList(
				"Lumbridge Home Teleport",
				"Home Teleport",
				"Varrock Teleport",
				"Lumbridge Teleport",
				"Falador Teleport",
				"Camelot Teleport",
				"Ardougne Teleport",
				"Watchtower Teleport",
				"Trollheim Teleport",
				"Teleport to Kourend",
				"Ape Atoll Teleport",
				"Wind Strike",
				"Water Strike",
				"Earth Strike",
				"Fire Strike",
				"Wind Bolt",
				"Water Bolt",
				"Earth Bolt",
				"Fire Bolt",
				"Wind Blast",
				"Water Blast",
				"Earth Blast",
				"Fire Blast",
				"Wind Wave",
				"Water Wave",
				"Earth Wave",
				"Fire Wave",
				"Wind Surge",
				"Water Surge",
				"Earth Surge",
				"Fire Surge",
				"Confuse",
				"Weaken",
				"Curse",
				"Vulnerability",
				"Enfeeble",
				"Stun",
				"Bind",
				"Snare",
				"Entangle",
				"Crumble Undead",
				"Tele Block",
				"Monster Examine",
				"High Level Alchemy",
				"Low Level Alchemy",
				"Superheat Item",
				"Telekinetic Grab"
			));
			addTargetLabels(labels, lastSpellTargets);
		}
		else if (surface == CvHelperActionSurface.NEAREST_ENTITY)
		{
			labels.add("Nearest clickable entity");
			for (Map<String, Object> entity : lastEntities)
			{
				String entityLabel = entityActionLabel(entity);
				if (!entityLabel.isEmpty())
				{
					labels.add(entityLabel);
				}
			}
		}
		else
		{
			addTargetLabels(labels, lastTargetsForSurface(surface));
		}
		return new ArrayList<>(labels);
	}

	private String entityActionLabel(Map<String, Object> entity)
	{
		if (entity == null)
		{
			return "";
		}
		String name = entity.get("name") == null ? "" : String.valueOf(entity.get("name")).trim();
		String type = entity.get("type") == null ? "" : String.valueOf(entity.get("type")).trim();
		Object id = entity.get("id");
		if (id instanceof Number)
		{
			return name + " id:" + ((Number) id).intValue();
		}
		return (name + " " + type).trim();
	}

	private List<Map<String, Object>> lastTargetsForSurface(CvHelperActionSurface surface)
	{
		switch (surface)
		{
			case MINIMAP:
				return lastMinimapTargets;
			case INVENTORY:
				return lastInventoryTargets;
			case EQUIPMENT:
				return lastEquipmentTargets;
			case PANELS:
				return lastPanelTargets;
			case COMBAT:
				return lastCombatTargets;
			default:
				return new ArrayList<>();
		}
	}

	private void addTargetLabels(Set<String> labels, List<Map<String, Object>> targets)
	{
		for (Map<String, Object> target : targets)
		{
			String label = targetLabelForMessage(target);
			if (label != null && !label.trim().isEmpty() && !"(unnamed)".equals(label))
			{
				labels.add(label);
			}
		}
	}


	private void setEnabled(Set<String> set, String key, boolean value)
	{
		if (value)
		{
			set.add(key);
		}
		else
		{
			set.remove(key);
		}
	}

	private boolean isTargetEnabled(String surface, Map<String, Object> target)
	{
		String haystack = normalizedTargetHaystack(target);
		if ("prayer".equals(surface))
		{
			return enabledPrayers.stream().anyMatch(key -> haystack.contains(normalize(key)));
		}
		if ("quickPrayer".equals(surface))
		{
			boolean matchedKnownPrayer = getPrayerNames().stream().anyMatch(key -> haystack.contains(normalize(key)));
			return !matchedKnownPrayer || enabledPrayers.stream().anyMatch(key -> haystack.contains(normalize(key)));
		}
		if (surface.contains("spell"))
		{
			return enabledSpellbooks.contains(String.valueOf(getSpellbookStatus().get("name")));
		}
		return true;
	}

	private String normalizedTargetHaystack(Map<String, Object> target)
	{
		if (target == null)
		{
			return "";
		}
		return normalize(targetLabelForMessage(target)
			+ " " + target.get("name")
			+ " " + target.get("text")
			+ " " + target.get("itemName")
			+ " " + target.get("itemId")
			+ " " + actionsText(target.get("actions")));
	}



	private String spellbookName(int spellbookVarbit, int spellbookId)
	{
		if (client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGGING_IN)
		{
			return "Unavailable";
		}
		switch (spellbookVarbit)
		{
			case 0:
				return "Standard";
			case 1:
				return "Ancient";
			case 2:
				return "Lunar";
			case 3:
				return "Arceuus";
			default:
				return "Unknown(" + spellbookVarbit + "/" + spellbookId + ")";
		}
	}

	private String targetLabel(Map<String, Object> target, String fallback)
	{
		String itemName = String.valueOf(target.get("itemName"));
		if (itemName != null && !itemName.isEmpty() && !"null".equals(itemName)
			&& fallback != null && !fallback.isEmpty()
			&& (fallback.startsWith("inventory") || fallback.contains("slot")))
		{
			return itemName + " (" + fallback + ")";
		}
		if (fallback != null && !fallback.isEmpty())
		{
			return fallback;
		}
		String name = String.valueOf(target.get("name"));
		String text = String.valueOf(target.get("text"));
		if (name != null && !name.isEmpty() && !"null".equals(name))
		{
			return name;
		}
		if (text != null && !text.isEmpty() && !"null".equals(text))
		{
			return text;
		}
		if (itemName != null && !itemName.isEmpty() && !"null".equals(itemName))
		{
			return itemName;
		}
		return target.get("surface") + "#" + target.get("index");
	}

	private String itemName(int itemId)
	{
		if (itemId <= 0)
		{
			return "";
		}
		ItemComposition composition = itemManager.getItemComposition(itemId);
		String name = composition == null ? "" : cleanWidgetText(composition.getName());
		return "null".equals(name) ? "" : name;
	}

	private Map<String, Object> capturePayload(String captureType, Rectangle crop)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", captureType);
		payload.put("crop", crop == null ? null : boundsMap(crop));
		payload.put("captureStatus", lastCaptures.get(captureType));
		payload.put("player", getPlayerStatus());
		payload.put("spellbook", getSpellbookStatus());
		payload.put("prayerTargets", lastPrayerTargets.size());
		payload.put("spellTargets", lastSpellTargets.size());
		payload.put("minimapTargets", lastMinimapTargets.size());
		payload.put("inventoryTargets", lastInventoryTargets.size());
		payload.put("equipmentTargets", lastEquipmentTargets.size());
		payload.put("panelTargets", lastPanelTargets.size());
		payload.put("combatTargets", lastCombatTargets.size());
		return payload;
	}

	private void setLastTargets(String surface, List<Map<String, Object>> targets)
	{
		switch (surface)
		{
			case "prayer":
				lastPrayerTargets = targets;
				break;
			case "spell":
				lastSpellTargets = targets;
				break;
			case "minimap":
				lastMinimapTargets = targets;
				break;
			case "inventory":
				lastInventoryTargets = targets;
				break;
			case "equipment":
				lastEquipmentTargets = targets;
				break;
			case "panels":
				lastPanelTargets = targets;
				break;
			case "combat":
				lastCombatTargets = targets;
				break;
			default:
				break;
		}
	}

	private List<Map<String, Object>> dedupeTargets(List<Map<String, Object>> targets)
	{
		Map<String, Map<String, Object>> byBounds = new LinkedHashMap<>();
		for (Map<String, Object> target : targets)
		{
			Object boundsValue = target.get("bounds");
			if (!(boundsValue instanceof Map))
			{
				continue;
			}
			Map<?, ?> bounds = (Map<?, ?>) boundsValue;
			String key = bounds.get("x") + ":" + bounds.get("y") + ":" + bounds.get("width") + ":" + bounds.get("height");
			byBounds.putIfAbsent(key, target);
		}
		return new ArrayList<>(byBounds.values());
	}

	private BufferedImage toBufferedImage(java.awt.Image image)
	{
		if (image instanceof BufferedImage)
		{
			return (BufferedImage) image;
		}
		BufferedImage out = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = out.createGraphics();
		graphics.drawImage(image, 0, 0, null);
		graphics.dispose();
		return out;
	}

	private BufferedImage cropImage(BufferedImage image, Rectangle crop)
	{
		int x = Math.max(0, crop.x);
		int y = Math.max(0, crop.y);
		int width = Math.min(crop.width, image.getWidth() - x);
		int height = Math.min(crop.height, image.getHeight() - y);
		if (width <= 0 || height <= 0)
		{
			return image;
		}
		BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = copy.createGraphics();
		graphics.drawImage(image, 0, 0, width, height, x, y, x + width, y + height, null);
		graphics.dispose();
		return copy;
	}

	private Rectangle findFirstWidgetBounds(int... componentIds)
	{
		for (int componentId : componentIds)
		{
			Widget widget = client.getWidget(componentId);
			if (widget == null || widget.isHidden())
			{
				continue;
			}
			Rectangle bounds = widget.getBounds();
			if (bounds != null && bounds.width > 0 && bounds.height > 0)
			{
				return bounds;
			}
		}
		return null;
	}

	private void updatePanelStatus(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.updateStatus(message);
			}
		});
	}


	private Map<String, Object> pointValue(Object point)
	{
		if (point == null)
		{
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		if (point instanceof net.runelite.api.Point)
		{
			net.runelite.api.Point canvasPoint = (net.runelite.api.Point) point;
			out.put("x", canvasPoint.getX());
			out.put("y", canvasPoint.getY());
		}
		else if (point instanceof LocalPoint)
		{
			LocalPoint localPoint = (LocalPoint) point;
			out.put("x", localPoint.getX());
			out.put("y", localPoint.getY());
		}
		else if (point instanceof WorldPoint)
		{
			WorldPoint worldPoint = (WorldPoint) point;
			out.put("x", worldPoint.getX());
			out.put("y", worldPoint.getY());
			out.put("plane", worldPoint.getPlane());
		}
		out.put("value", point.toString());
		return out;
	}

	private String widgetSummary(Widget widget)
	{
		if (widget == null)
		{
			return "none";
		}
		Rectangle bounds = widget.getBounds();
		return widget.getId() + "@" + (bounds == null ? "null" : bounds.x + "," + bounds.y + "," + bounds.width + "x" + bounds.height);
	}

	private String cleanWidgetText(String value)
	{
		if (value == null)
		{
			return "";
		}
		return value.replaceAll("<[^>]*>", "").trim();
	}

	private void updatePanelServerStatus()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.updateServerStatus(getServerStatusText());
			}
		});
	}

	private void writeJson(HttpExchange exchange, int code, Object body) throws IOException
	{
		writeResponse(exchange, code, gson.toJson(body));
	}

	private String queryParam(HttpExchange exchange, String key)
	{
		if (exchange == null || exchange.getRequestURI() == null || exchange.getRequestURI().getRawQuery() == null)
		{
			return "";
		}
		String rawQuery = exchange.getRequestURI().getRawQuery();
		for (String pair : rawQuery.split("&"))
		{
			String[] parts = pair.split("=", 2);
			String rawKey = decodeQuery(parts[0]);
			if (key.equals(rawKey))
			{
				return parts.length > 1 ? decodeQuery(parts[1]) : "";
			}
		}
		return "";
	}

	private String decodeQuery(String value)
	{
		return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	private void writeResponse(HttpExchange exchange, int code, String body) throws IOException
	{
		noteLocalWebHelperRequest();
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
		exchange.sendResponseHeaders(code, bytes.length);
		try (OutputStream os = exchange.getResponseBody())
		{
			os.write(bytes);
		}
	}

	private void writeBinary(HttpExchange exchange, int code, String contentType, byte[] bytes) throws IOException
	{
		noteLocalWebHelperRequest();
		exchange.getResponseHeaders().add("Content-Type", contentType);
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
		exchange.sendResponseHeaders(code, bytes.length);
		try (OutputStream os = exchange.getResponseBody())
		{
			os.write(bytes);
		}
	}

	private void noteLocalWebHelperRequest()
	{
		lastLocalWebHelperRequest.set(Instant.now());
		updatePanelServerStatus();
	}

	private Map<String, Object> eventPayload(String eventType, Object payload)
	{
		Map<String, Object> event = new LinkedHashMap<>();
		event.put("plugin", "CV Helper");
		event.put("event", eventType);
		event.put("generatedAt", Instant.now().toString());
		event.put("payload", payload);
		return event;
	}

	private void sendWebhook(Map<String, Object> event)
	{
		String webhookUrl = config.webhookUrl();
		if (webhookUrl == null || webhookUrl.trim().isEmpty())
		{
			return;
		}

		Request request = new Request.Builder()
			.url(webhookUrl.trim())
			.post(RequestBody.create(RuneLiteAPI.JSON, gson.toJson(event)))
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("CV Helper webhook failed", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	private List<Map<String, Object>> safeDroppableSlots(Map<String, Object> inventory)
	{
		List<Map<String, Object>> safe = new ArrayList<>();
		Object itemsValue = inventory.get("items");
		if (!(itemsValue instanceof List))
		{
			return safe;
		}
		for (Object itemValue : (List<?>) itemsValue)
		{
			if (!(itemValue instanceof Map))
			{
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) itemValue;
			String name = String.valueOf(item.get("name"));
			int itemId = intValue(item.get("id"), -1);
			if (itemSafetyService.isAllowedToDrop(name, itemId, getMobFarmerDropItems(), getMobFarmerNeverDropItems(), getMobFarmerMaxDropValue()))
			{
				safe.add(item);
			}
		}
		return safe;
	}

	private List<Map<String, Object>> protectedSlots(Map<String, Object> inventory)
	{
		List<Map<String, Object>> protectedItems = new ArrayList<>();
		Object itemsValue = inventory.get("items");
		if (!(itemsValue instanceof List))
		{
			return protectedItems;
		}
		for (Object itemValue : (List<?>) itemsValue)
		{
			if (!(itemValue instanceof Map))
			{
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) itemValue;
			String name = String.valueOf(item.get("name"));
			int itemId = intValue(item.get("id"), -1);
			if (itemSafetyService.isProtectedItem(name, itemId, getMobFarmerNeverDropItems()))
			{
				protectedItems.add(item);
			}
		}
		return protectedItems;
	}

	private List<Map<String, Object>> rejectedSlots(Map<String, Object> inventory)
	{
		List<Map<String, Object>> rejected = new ArrayList<>();
		Object itemsValue = inventory.get("items");
		if (!(itemsValue instanceof List))
		{
			return rejected;
		}
		for (Object itemValue : (List<?>) itemsValue)
		{
			if (!(itemValue instanceof Map))
			{
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) itemValue;
			String name = String.valueOf(item.get("name"));
			int itemId = intValue(item.get("id"), -1);
			Map<String, Object> rejection = new LinkedHashMap<>();
			rejection.putAll(item);
			String reason = null;
			if (itemSafetyService.isProtectedItem(name, itemId, getMobFarmerNeverDropItems()))
			{
				reason = "PROTECTED_ITEM";
			}
			else if (itemSafetyService.isValuable(itemId, getMobFarmerMaxDropValue()))
			{
				reason = "TOO_VALUABLE";
			}
			else if (!itemSafetyService.isAllowedToDrop(name, itemId, getMobFarmerDropItems(), getMobFarmerNeverDropItems(), getMobFarmerMaxDropValue()))
			{
				reason = "NOT_ALLOWLISTED";
			}
			if (reason != null)
			{
				rejection.put("reason", reason);
				rejected.add(rejection);
			}
		}
		return rejected;
	}

	private List<Map<String, Object>> droppableLogSlots(Map<String, Object> inventory, String woodcuttingTarget)
	{
		List<Map<String, Object>> droppable = new ArrayList<>();
		Object itemsValue = inventory.get("items");
		if (!(itemsValue instanceof List))
		{
			return droppable;
		}
		String[] targetWords = woodcuttingTarget == null ? new String[0] : woodcuttingTarget.split("\\s*(?:\\||,|;|\\r?\\n)\\s*");
		for (Object itemValue : (List<?>) itemsValue)
		{
			if (!(itemValue instanceof Map))
			{
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) itemValue;
			String name = String.valueOf(item.get("name")).toLowerCase();
			int itemId = intValue(item.get("id"), -1);
			if (itemSafetyService.isProtectedItem(name, itemId, getMobFarmerNeverDropItems()))
			{
				continue;
			}
			boolean isLog = name.contains("logs") || name.equals("log");
			if (!isLog)
			{
				for (String word : targetWords)
				{
					String w = word.trim().toLowerCase();
					if (!w.isEmpty() && name.contains(w) && (name.contains("log") || name.contains("logs")))
					{
						isLog = true;
						break;
					}
				}
			}
			if (isLog)
			{
				droppable.add(item);
			}
		}
		return droppable;
	}
}
