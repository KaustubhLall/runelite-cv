/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.AWTException;
import java.awt.Color;
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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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

@Slf4j
abstract class CvHelperModData extends Plugin
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
	protected static final String GROUND_ITEMS_CONFIG_GROUP = "grounditems";
	protected static final String GROUND_ITEMS_HIGHLIGHTED_ITEMS = "highlightedItems";
	protected static final String GROUND_ITEMS_HIDDEN_ITEMS = "hiddenItems";
	protected static final String GROUND_ITEMS_SHOW_HIGHLIGHTED_ONLY = "showHighlightedOnly";
	protected static final String GROUND_ITEMS_HIDE_UNDER_VALUE = "hideUnderValue";
	protected static final String GROUND_ITEMS_DONT_HIDE_UNTRADEABLES = "dontHideUntradeables";
	protected static final int GROUND_ITEMS_NONE = 0;
	protected static final int GROUND_ITEMS_WILDCARD = 1;
	protected static final int GROUND_ITEMS_EXACT = 2;
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
	protected volatile Map<String, Object> lastMobFarmerProgressStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerSchedulerStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerDeathLootStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerReattackStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerStabilizationStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerAutorunStatus = new LinkedHashMap<>();
	protected volatile Map<String, Object> lastMobFarmerFocusClickStatus = new LinkedHashMap<>();
	protected volatile List<Map<String, Object>> lastMobFarmerIntents = new ArrayList<>();
	protected volatile boolean lastMobFarmerMultiCombat;
	protected volatile String lastStableSidePanel = "inventory";
	protected volatile String mobFarmerTarget = "cow";
	protected volatile boolean mobFarmerLiveMode;
	protected volatile Thread mobFarmerThread;
	protected volatile long lastMobFarmerLoginClickMillis;
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
	protected volatile Map<String, Object> lastSelectedWoodcuttingTarget = null;
	protected volatile long lastWoodcuttingTargetClickTime = 0L;
	protected final Map<String, Integer> lastMobFarmerActionTickByKey = new LinkedHashMap<>();
	protected final Map<String, Integer> mobFarmerLootSkipUntilTickByKey = new LinkedHashMap<>();

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
}
