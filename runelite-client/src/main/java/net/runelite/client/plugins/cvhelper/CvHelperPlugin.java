/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelper;

import com.google.gson.Gson;
import com.google.inject.Provides;
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
import java.util.ArrayList;
import java.util.Arrays;
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
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.VarPlayer;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
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

@PluginDescriptor(
	name = "CV Helper",
	description = "Highlights hovered UI areas and prepares coordinate capture for CV extraction.",
	tags = {"overlay", "ui", "coordinates", "debug"},
	enabledByDefault = true
)
@Slf4j
public class CvHelperPlugin extends Plugin
{
	private static final String TOOLTIP = "CV Helper";
	private static final int DEFAULT_LOCAL_PORT = 11777;
	private static final int ACTION_SLOT_COUNT = 22;
	private static final int ACTION_RESOLVE_RETRIES = 4;
	private static final int MOB_FARMER_LOOP_DELAY_MS = 1200;
	private static final long MOB_FARMER_LOGIN_CLICK_COOLDOWN_MS = 5000L;
	private static final int MOB_FARMER_PROGRESS_WINDOW_TICKS = 8;
	private static final int MOB_FARMER_LOOT_SPAWN_GRACE_TICKS = 3;
	private static final String GROUND_ITEMS_CONFIG_GROUP = "grounditems";
	private static final String GROUND_ITEMS_HIGHLIGHTED_ITEMS = "highlightedItems";
	private static final String GROUND_ITEMS_HIDDEN_ITEMS = "hiddenItems";
	private static final String GROUND_ITEMS_SHOW_HIGHLIGHTED_ONLY = "showHighlightedOnly";
	private static final String GROUND_ITEMS_HIDE_UNDER_VALUE = "hideUnderValue";
	private static final String GROUND_ITEMS_DONT_HIDE_UNTRADEABLES = "dontHideUntradeables";
	private static final int GROUND_ITEMS_NONE = 0;
	private static final int GROUND_ITEMS_WILDCARD = 1;
	private static final int GROUND_ITEMS_EXACT = 2;
	private static final DateTimeFormatter CAPTURE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	private static final int[] PRAYER_COMPONENTS = {
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
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private CvHelperOverlay overlay;

	@Inject
	private CvHelperConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ItemManager itemManager;

	private NavigationButton navButton;
	private CvHelperPanel panel;
	private HttpServer server;
	private final AtomicReference<String> lastEvent = new AtomicReference<>("idle");
	private volatile List<Map<String, Object>> lastPrayerTargets = new ArrayList<>();
	private volatile List<Map<String, Object>> lastSpellTargets = new ArrayList<>();
	private volatile List<Map<String, Object>> lastMinimapTargets = new ArrayList<>();
	private volatile List<Map<String, Object>> lastInventoryTargets = new ArrayList<>();
	private volatile List<Map<String, Object>> lastEquipmentTargets = new ArrayList<>();
	private volatile List<Map<String, Object>> lastPanelTargets = new ArrayList<>();
	private volatile List<Map<String, Object>> lastCombatTargets = new ArrayList<>();
	private volatile List<Map<String, Object>> lastEntities = new ArrayList<>();
	private final Map<String, Map<String, Object>> targetSnapshots = new LinkedHashMap<>();
	private final Map<String, Map<String, Object>> lastCaptures = new LinkedHashMap<>();
	private final Set<String> enabledPrayers = new HashSet<>();
	private final Set<String> enabledSpellbooks = new HashSet<>();
	private final List<HotkeyListener> actionHotkeyListeners = new ArrayList<>();
	private final Set<String> preDispatcherPressedKeys = new HashSet<>();
	private final AtomicBoolean actionInProgress = new AtomicBoolean(false);
	private final Map<Integer, Integer> actionSequenceIndexes = new HashMap<>();
	private final AtomicBoolean mobFarmerRunning = new AtomicBoolean(false);
	private final AtomicInteger mobFarmerGeneration = new AtomicInteger();
	private final AtomicReference<String> mobFarmerStatus = new AtomicReference<>("idle");
	private volatile List<Map<String, Object>> lastMobFarmerCandidates = new ArrayList<>();
	private volatile Map<String, Object> lastMobFarmerDecision = new LinkedHashMap<>();
	private volatile List<Map<String, Object>> lastMobFarmerLootCandidates = new ArrayList<>();
	private volatile Map<String, Object> lastMobFarmerLootDecision = new LinkedHashMap<>();
	private volatile Map<String, Object> lastMobFarmerSurvivalDecision = new LinkedHashMap<>();
	private volatile Map<String, Object> lastMobFarmerIntermediateDecision = new LinkedHashMap<>();
	private volatile Map<String, Object> lastMobFarmerActionAttempt = new LinkedHashMap<>();
	private volatile List<Map<String, Object>> lastMobFarmerMenuEntries = new ArrayList<>();
	private volatile Map<String, Object> lastMobFarmerInventoryStatus = new LinkedHashMap<>();
	private volatile Map<String, Object> lastMobFarmerLoginRecovery = new LinkedHashMap<>();
	private volatile Map<String, Object> lastLoginClickAttempt = new LinkedHashMap<>();
	private volatile Map<String, Object> lastMobFarmerProgressStatus = new LinkedHashMap<>();
	private volatile Map<String, Object> lastMobFarmerSchedulerStatus = new LinkedHashMap<>();
	private volatile Map<String, Object> lastMobFarmerDeathLootStatus = new LinkedHashMap<>();
	private volatile List<Map<String, Object>> lastMobFarmerIntents = new ArrayList<>();
	private volatile boolean lastMobFarmerMultiCombat;
	private final KeyEventDispatcher cvHotkeyDispatcher = this::dispatchCvHotkey;
	private volatile String lastStableSidePanel = "inventory";
	private volatile String mobFarmerTarget = "cow";
	private volatile boolean mobFarmerLiveMode;
	private volatile Thread mobFarmerThread;
	private volatile long lastMobFarmerLoginClickMillis;
	private volatile String activeMobFarmerLootKey;
	private volatile int activeMobFarmerLootStartTick;
	private volatile int activeMobFarmerLootLastDistance = Integer.MAX_VALUE;
	private volatile boolean activeMobFarmerLootImproving;
	private volatile int mobFarmerMakeProgressUntilTick;
	private volatile int lastMobFarmerLoopStepTick = -1;
	private volatile String lastMobFarmerLoopStepSource = "none";
	private volatile String pendingMobFarmerDeathKey;
	private volatile int pendingMobFarmerDeathTick = -1;
	private volatile Map<String, Object> pendingMobFarmerDeathTarget = new LinkedHashMap<>();
	private final Map<String, Integer> lastMobFarmerActionTickByKey = new LinkedHashMap<>();

	private static final class PanelSwitchTarget
	{
		private final String panel;
		private final Keybind keybind;
		private final Point clickPoint;

		private PanelSwitchTarget(String panel, Keybind keybind, Point clickPoint)
		{
			this.panel = panel;
			this.keybind = keybind;
			this.clickPoint = clickPoint;
		}

		private boolean usesHotkey()
		{
			return keybind != null && !Keybind.NOT_SET.equals(keybind);
		}
	}

	private static final class MobFarmerCandidate
	{
		private final NPC npc;
		private final Map<String, Object> entity;
		private final List<String> reasons = new ArrayList<>();
		private int score;
		private boolean selectable = true;
		private boolean engagedByOther;
		private boolean engagedWithLocalPlayer;

		private MobFarmerCandidate(NPC npc, Map<String, Object> entity)
		{
			this.npc = npc;
			this.entity = entity;
		}

		private void reject(String reason)
		{
			selectable = false;
			reasons.add(reason);
		}

		private void note(String reason)
		{
			reasons.add(reason);
		}
	}

	private static final class MobFarmerSelection
	{
		private Map<String, Object> target;
		private List<Map<String, Object>> reports = new ArrayList<>();
		private String decision = "none";
		private boolean multiCombat;
	}

	private static final class MobFarmerLootCandidate
	{
		private final Map<String, Object> item;
		private final List<String> reasons = new ArrayList<>();
		private final List<String> priorityReasons = new ArrayList<>();
		private int score;
		private boolean selectable = true;
		private boolean highPriority;

		private MobFarmerLootCandidate(Map<String, Object> item)
		{
			this.item = item;
		}

		private void reject(String reason)
		{
			selectable = false;
			reasons.add(reason);
		}

		private void note(String reason)
		{
			reasons.add(reason);
		}

		private void priority(String reason)
		{
			highPriority = true;
			priorityReasons.add(reason);
			note("priority:" + reason);
		}
	}

	private static final class MobFarmerLootSelection
	{
		private Map<String, Object> target;
		private List<Map<String, Object>> reports = new ArrayList<>();
		private String decision = "none";
		private int selectableCount;
		private boolean priorityOnly;
	}

	private static final class InventoryMenuAction
	{
		private final int opIndex;
		private final int componentOpId;
		private final int param0;
		private final int param1;
		private final MenuAction menuAction;
		private final int identifier;
		private final int itemId;
		private final String option;

		private InventoryMenuAction(int opIndex, int componentOpId, int param0, int param1, MenuAction menuAction, int identifier, int itemId, String option)
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

		private Map<String, Object> asMap()
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

	private static final class IntermediateInventoryAction
	{
		private final Map<String, Object> target;
		private final String[] preferredActions;
		private final String failureReason;
		private final String matchedRule;

		private IntermediateInventoryAction(Map<String, Object> target, String[] preferredActions, String failureReason, String matchedRule)
		{
			this.target = target;
			this.preferredActions = preferredActions;
			this.failureReason = failureReason;
			this.matchedRule = matchedRule;
		}

		private boolean isActionable()
		{
			return failureReason == null;
		}
	}

	private static final class IntermediateActionRule
	{
		private final String itemTarget;
		private final String[] actions;
		private final String source;

		private IntermediateActionRule(String itemTarget, String[] actions, String source)
		{
			this.itemTarget = itemTarget;
			this.actions = actions;
			this.source = source;
		}
	}

	private enum GroundItemsClassification
	{
		NONE,
		HIGHLIGHTED,
		HIDDEN,
		HIDDEN_BY_VALUE,
		SUPPRESSED_BY_SHOW_HIGHLIGHTED_ONLY
	}

	private enum MobFarmerActionKind
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

	@Provides
	CvHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CvHelperConfig.class);
	}

	@Override
	protected void startUp()
	{
		log.info("CV Helper starting");
		mobFarmerTarget = normalizedMobFarmerTarget(config.mobFarmerTarget());
		enabledPrayers.addAll(getPrayerNames());
		enabledSpellbooks.addAll(getSpellbookNames());
		overlayManager.add(overlay);
		panel = new CvHelperPanel(this);
		navButton = NavigationButton.builder()
			.tooltip(TOOLTIP)
			.icon(createIcon())
			.priority(9)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		registerHotkeys();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(cvHotkeyDispatcher);
		startServer();
	}

	@Override
	protected void shutDown()
	{
		log.info("CV Helper stopping");
		stopMobFarmer();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(cvHotkeyDispatcher);
		preDispatcherPressedKeys.clear();
		unregisterHotkeys();
		stopServer();
		overlayManager.remove(overlay);
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		navButton = null;
		panel = null;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = safeValue(client::getTickCount, 0);
		Map<String, Object> scheduler = new LinkedHashMap<>(lastMobFarmerSchedulerStatus);
		scheduler.put("lastGameTick", tick);
		scheduler.put("gameTickAt", Instant.now().toString());
		lastMobFarmerSchedulerStatus = scheduler;
		if (!mobFarmerRunning.get() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		int generation = mobFarmerGeneration.get();
		if (!isCurrentMobFarmerLoop(generation) || lastMobFarmerLoopStepTick == tick)
		{
			return;
		}
		mobFarmerStep(mobFarmerLiveMode, generation, "game-tick");
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
			if (shouldSuppressHotkey())
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
			if (shouldSuppressHotkey())
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
			if (shouldSuppressHotkey())
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
			if (shouldSuppressHotkey())
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
			if (shouldSuppressHotkey())
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
					if (shouldSuppressHotkey())
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
		if (preDispatcherPressedKeys.contains(key))
		{
			event.consume();
			return true;
		}

		if (config.panicStopHotkey().matches(event))
		{
			preDispatcherPressedKeys.add(key);
			panicStop();
			event.consume();
			return true;
		}
		if (shouldSuppressHotkey())
		{
			return false;
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

	private String preDispatchKey(KeyEvent event)
	{
		int modifierMask = InputEvent.SHIFT_DOWN_MASK
			| InputEvent.CTRL_DOWN_MASK
			| InputEvent.ALT_DOWN_MASK
			| InputEvent.META_DOWN_MASK;
		return event.getExtendedKeyCode() + ":" + (event.getModifiersEx() & modifierMask);
	}

	private boolean shouldSuppressHotkey()
	{
		if (client == null)
		{
			return false;
		}

		java.awt.Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focusOwner instanceof javax.swing.text.JTextComponent)
		{
			return true;
		}

		if (client.getVarcIntValue(VarClientID.MESLAYERMODE) != InputType.NONE.getType())
		{
			return true;
		}

		String chatInput = client.getVarcStrValue(VarClientID.CHATINPUT);
		String mesLayerInput = client.getVarcStrValue(VarClientID.MESLAYERINPUT);
		return (chatInput != null && !chatInput.isEmpty()) || (mesLayerInput != null && !mesLayerInput.isEmpty());
	}

	CvHelperConfig getConfig()
	{
		return config;
	}

	String getServerStatusText()
	{
		int port = localPort();
		return port > 0 ? "Server: http://127.0.0.1:" + port : "Server: off";
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
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_HOVER_OVERLAY, value);
	}

	void setShowWidgetInfo(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_WIDGET_INFO, value);
	}

	void setShowPrayerTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_PRAYER_TARGETS, value);
		if (value)
		{
			refreshPrayerTargets();
		}
	}

	void setShowSpellTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_SPELL_TARGETS, value);
		if (value)
		{
			refreshSpellTargets();
		}
	}

	void setShowMinimapTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_MINIMAP_TARGETS, value);
		if (value)
		{
			refreshMinimapTargets();
		}
	}

	void setShowInventoryTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_INVENTORY_TARGETS, value);
		if (value)
		{
			refreshInventoryTargets();
		}
	}

	void setShowEquipmentTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_EQUIPMENT_TARGETS, value);
		if (value)
		{
			refreshEquipmentTargets();
		}
	}

	void setShowPanelTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_PANEL_TARGETS, value);
		if (value)
		{
			refreshTargets("panels", this::collectPanelTargets);
		}
	}

	void setShowCombatTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_COMBAT_TARGETS, value);
		if (value)
		{
			refreshTargets("combat", this::collectCombatTargets);
		}
	}

	void setShowEntityTargets(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_ENTITY_TARGETS, value);
		if (value)
		{
			refreshEntities();
		}
	}

	void setShowTargetLabels(boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.SHOW_TARGET_LABELS, value);
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
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.ENABLE_LOCAL_EXPORT, value);
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
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.WEBHOOK_URL, value == null ? "" : value);
	}

	void setActionHotkey(int slot, Keybind keybind)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionHotkey" + slot, keybind == null ? Keybind.NOT_SET : keybind);
		updatePanelStatus("Action " + slot + " hotkey saved");
	}

	void setActionEnabled(int slot, boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionEnabled" + slot, value);
		updatePanelStatus("Action " + slot + (value ? " enabled" : " disabled"));
	}

	void setActionSurface(int slot, CvHelperActionSurface surface)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionSurface" + slot, surface == null ? CvHelperActionSurface.DISABLED : surface);
		updatePanelStatus("Action " + slot + " surface saved");
	}

	void setActionTarget(int slot, String target)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionTarget" + slot, target == null ? "" : target.trim());
		updatePanelStatus("Action " + slot + " target saved");
	}

	void setActionClickMouse(int slot, boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionClickMouse" + slot, value);
		updatePanelStatus("Action " + slot + " mouse-after setting saved");
	}

	void setActionClickAfterMode(int slot, CvHelperClickAfterMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionClickAfterMode" + slot, mode == null ? CvHelperClickAfterMode.AUTO : mode);
		updatePanelStatus("Action " + slot + " click-after mode saved");
	}

	void setActionInvocationMode(int slot, CvHelperActionInvocationMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionInvocationMode" + slot, mode == null ? CvHelperActionInvocationMode.AUTO : mode);
		updatePanelStatus("Action " + slot + " invocation mode saved");
	}

	void setActionPrayerMode(int slot, CvHelperPrayerActionMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionPrayerMode" + slot, mode == null ? CvHelperPrayerActionMode.TOGGLE : mode);
		updatePanelStatus("Action " + slot + " prayer mode saved");
	}

	void setActionSpellAvailabilityMode(int slot, CvHelperSpellAvailabilityMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionSpellAvailabilityMode" + slot, mode == null ? CvHelperSpellAvailabilityMode.GUARD_UNAVAILABLE : mode);
		updatePanelStatus("Action " + slot + " spell guard saved");
	}

	void setActionReturnPanel(int slot, boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionReturnPanel" + slot, value);
		updatePanelStatus("Action " + slot + " return-panel setting saved");
	}

	void setActionReturnMouseCenter(int slot, boolean value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, "actionReturnMouseCenter" + slot, value);
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
				Keybind keybind = configManager.getConfiguration(CvHelperConfig.GROUP, "actionHotkey" + slot, Keybind.class);
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
				Boolean enabled = configManager.getConfiguration(CvHelperConfig.GROUP, "actionEnabled" + slot, Boolean.class);
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
				CvHelperActionSurface surface = configManager.getConfiguration(CvHelperConfig.GROUP, "actionSurface" + slot, CvHelperActionSurface.class);
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
				String target = configManager.getConfiguration(CvHelperConfig.GROUP, "actionTarget" + slot);
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
				CvHelperClickAfterMode mode = configManager.getConfiguration(CvHelperConfig.GROUP, "actionClickAfterMode" + slot, CvHelperClickAfterMode.class);
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
				CvHelperActionInvocationMode mode = configManager.getConfiguration(CvHelperConfig.GROUP, "actionInvocationMode" + slot, CvHelperActionInvocationMode.class);
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
				CvHelperPrayerActionMode mode = configManager.getConfiguration(CvHelperConfig.GROUP, "actionPrayerMode" + slot, CvHelperPrayerActionMode.class);
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
				CvHelperSpellAvailabilityMode mode = configManager.getConfiguration(CvHelperConfig.GROUP, "actionSpellAvailabilityMode" + slot, CvHelperSpellAvailabilityMode.class);
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
				Boolean returnPanel = configManager.getConfiguration(CvHelperConfig.GROUP, "actionReturnPanel" + slot, Boolean.class);
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
				Boolean returnMouseCenter = configManager.getConfiguration(CvHelperConfig.GROUP, "actionReturnMouseCenter" + slot, Boolean.class);
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
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_TARGET, mobFarmerTarget);
		mobFarmerStatus.set("target@" + mobFarmerTarget);
		updatePanelStatus("Mob farmer target: " + mobFarmerTarget);
	}

	CvHelperMobEngagedMode getMobFarmerEngagedMode()
	{
		CvHelperMobEngagedMode mode = config.mobFarmerEngagedMode();
		return mode == null ? CvHelperMobEngagedMode.PREFER_FREE : mode;
	}

	void setMobFarmerEngagedMode(CvHelperMobEngagedMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_ENGAGED_MODE, mode == null ? CvHelperMobEngagedMode.PREFER_FREE : mode);
	}

	CvHelperMobAggroResponse getMobFarmerAggroResponse()
	{
		CvHelperMobAggroResponse response = config.mobFarmerAggroResponse();
		return response == null ? CvHelperMobAggroResponse.WAIT : response;
	}

	void setMobFarmerAggroResponse(CvHelperMobAggroResponse response)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_AGGRO_RESPONSE, response == null ? CvHelperMobAggroResponse.WAIT : response);
	}

	boolean getMobFarmerRequireLineOfSight()
	{
		return config.mobFarmerRequireLineOfSight();
	}

	void setMobFarmerRequireLineOfSight(boolean requireLineOfSight)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_REQUIRE_LINE_OF_SIGHT, requireLineOfSight);
	}

	int getMobFarmerMaxDistance()
	{
		return Math.max(0, config.mobFarmerMaxDistance());
	}

	void setMobFarmerMaxDistance(int maxDistance)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_MAX_DISTANCE, Math.max(0, maxDistance));
	}

	boolean getMobFarmerAutoEatEnabled()
	{
		return config.mobFarmerAutoEatEnabled();
	}

	void setMobFarmerAutoEatEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_AUTO_EAT_ENABLED, enabled);
	}

	int getMobFarmerEatHitpointPercent()
	{
		return Math.max(1, Math.min(99, config.mobFarmerEatHitpointPercent()));
	}

	void setMobFarmerEatHitpointPercent(int percent)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_EAT_HITPOINT_PERCENT, Math.max(1, Math.min(99, percent)));
	}

	String getMobFarmerFoodItems()
	{
		return config.mobFarmerFoodItems() == null ? "" : config.mobFarmerFoodItems();
	}

	void setMobFarmerFoodItems(String items)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_FOOD_ITEMS, items == null ? "" : items.trim());
	}

	boolean getMobFarmerStopIfNoFood()
	{
		return config.mobFarmerStopIfNoFood();
	}

	void setMobFarmerStopIfNoFood(boolean stop)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_STOP_IF_NO_FOOD, stop);
	}

	boolean getMobFarmerSurvivalPreemptsActions()
	{
		return config.mobFarmerSurvivalPreemptsActions();
	}

	void setMobFarmerSurvivalPreemptsActions(boolean preempts)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_SURVIVAL_PREEMPTS_ACTIONS, preempts);
	}

	boolean getMobFarmerLoginRecoveryEnabled()
	{
		return config.mobFarmerLoginRecoveryEnabled();
	}

	void setMobFarmerLoginRecoveryEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOGIN_RECOVERY_ENABLED, enabled);
	}

	boolean getMobFarmerLoginRecoveryF2pOnly()
	{
		return config.mobFarmerLoginRecoveryF2pOnly();
	}

	void setMobFarmerLoginRecoveryF2pOnly(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOGIN_RECOVERY_F2P_ONLY, enabled);
	}

	boolean getMobFarmerLoginClickToPlayEnabled()
	{
		return config.mobFarmerLoginClickToPlayEnabled();
	}

	void setMobFarmerLoginClickToPlayEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOGIN_CLICK_TO_PLAY_ENABLED, enabled);
	}

	boolean getMobFarmerLoginDisconnectRecoveryEnabled()
	{
		return config.mobFarmerLoginDisconnectRecoveryEnabled();
	}

	void setMobFarmerLoginDisconnectRecoveryEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOGIN_DISCONNECT_RECOVERY_ENABLED, enabled);
	}

	boolean getMobFarmerAutoResumeAfterLogin()
	{
		return config.mobFarmerAutoResumeAfterLogin();
	}

	void setMobFarmerAutoResumeAfterLogin(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_AUTO_RESUME_AFTER_LOGIN, enabled);
	}

	int getMobFarmerPreferredLoginWorld()
	{
		return Math.max(0, config.mobFarmerPreferredLoginWorld());
	}

	void setMobFarmerPreferredLoginWorld(int world)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_PREFERRED_LOGIN_WORLD, Math.max(0, world));
	}

	boolean getMobFarmerLootEnabled()
	{
		return config.mobFarmerLootEnabled();
	}

	void setMobFarmerLootEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_ENABLED, enabled);
	}

	boolean getMobFarmerLootDuringCombat()
	{
		return config.mobFarmerLootDuringCombat();
	}

	void setMobFarmerLootDuringCombat(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_DURING_COMBAT, enabled);
	}

	boolean getMobFarmerAttackBeforeLoot()
	{
		return config.mobFarmerAttackBeforeLoot();
	}

	void setMobFarmerAttackBeforeLoot(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_ATTACK_BEFORE_LOOT, enabled);
	}

	int getMobFarmerLootMinValueGe()
	{
		return Math.max(0, config.mobFarmerLootMinValueGe());
	}

	void setMobFarmerLootMinValueGe(int value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_MIN_VALUE_GE, Math.max(0, value));
	}

	int getMobFarmerHighPriorityLootValueGe()
	{
		return Math.max(0, config.mobFarmerHighPriorityLootValueGe());
	}

	void setMobFarmerHighPriorityLootValueGe(int value)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_HIGH_PRIORITY_LOOT_VALUE_GE, Math.max(0, value));
	}

	int getMobFarmerLootUrgentDespawnTicks()
	{
		return Math.max(0, config.mobFarmerLootUrgentDespawnTicks());
	}

	void setMobFarmerLootUrgentDespawnTicks(int ticks)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_URGENT_DESPAWN_TICKS, Math.max(0, ticks));
	}

	int getMobFarmerLootCleanupPileCount()
	{
		return Math.max(0, config.mobFarmerLootCleanupPileCount());
	}

	void setMobFarmerLootCleanupPileCount(int count)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_CLEANUP_PILE_COUNT, Math.max(0, count));
	}

	int getMobFarmerLootRadius()
	{
		return Math.max(0, config.mobFarmerLootRadius());
	}

	void setMobFarmerLootRadius(int radius)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_RADIUS, Math.max(0, radius));
	}

	String getMobFarmerLootItems()
	{
		return config.mobFarmerLootItems() == null ? "" : config.mobFarmerLootItems();
	}

	void setMobFarmerLootItems(String items)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_ITEMS, items == null ? "" : items.trim());
	}

	String getMobFarmerLootBlacklist()
	{
		return config.mobFarmerLootBlacklist() == null ? "" : config.mobFarmerLootBlacklist();
	}

	void setMobFarmerLootBlacklist(String items)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_BLACKLIST, items == null ? "" : items.trim());
	}

	CvHelperLootOwnershipMode getMobFarmerLootOwnershipMode()
	{
		CvHelperLootOwnershipMode mode = config.mobFarmerLootOwnershipMode();
		return mode == null ? CvHelperLootOwnershipMode.OWN_OR_PUBLIC : mode;
	}

	void setMobFarmerLootOwnershipMode(CvHelperLootOwnershipMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_OWNERSHIP_MODE, mode == null ? CvHelperLootOwnershipMode.OWN_OR_PUBLIC : mode);
	}

	CvHelperMobInteractionMode getMobFarmerAttackInteractionMode()
	{
		CvHelperMobInteractionMode mode = config.mobFarmerAttackInteractionMode();
		return mode == null ? CvHelperMobInteractionMode.MENU_ACTION : mode;
	}

	void setMobFarmerAttackInteractionMode(CvHelperMobInteractionMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_ATTACK_INTERACTION_MODE, mode == null ? CvHelperMobInteractionMode.MENU_ACTION : mode);
	}

	CvHelperMobInteractionMode getMobFarmerLootInteractionMode()
	{
		CvHelperMobInteractionMode mode = config.mobFarmerLootInteractionMode();
		return mode == null ? CvHelperMobInteractionMode.MENU_ACTION : mode;
	}

	void setMobFarmerLootInteractionMode(CvHelperMobInteractionMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_LOOT_INTERACTION_MODE, mode == null ? CvHelperMobInteractionMode.MENU_ACTION : mode);
	}

	CvHelperGroundItemsMode getMobFarmerGroundItemsMode()
	{
		CvHelperGroundItemsMode mode = config.mobFarmerGroundItemsMode();
		return mode == null ? CvHelperGroundItemsMode.SUPPLEMENT : mode;
	}

	void setMobFarmerGroundItemsMode(CvHelperGroundItemsMode mode)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_GROUND_ITEMS_MODE, mode == null ? CvHelperGroundItemsMode.SUPPLEMENT : mode);
	}

	boolean getMobFarmerRespectGroundItemsHidden()
	{
		return config.mobFarmerRespectGroundItemsHidden();
	}

	void setMobFarmerRespectGroundItemsHidden(boolean respectHidden)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_RESPECT_GROUND_ITEMS_HIDDEN, respectHidden);
	}

	boolean getMobFarmerIntermediateActionsEnabled()
	{
		return config.mobFarmerIntermediateActionsEnabled();
	}

	void setMobFarmerIntermediateActionsEnabled(boolean enabled)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_INTERMEDIATE_ACTIONS_ENABLED, enabled);
	}

	String getMobFarmerIntermediateItems()
	{
		return config.mobFarmerIntermediateItems() == null ? "" : config.mobFarmerIntermediateItems();
	}

	void setMobFarmerIntermediateItems(String items)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_INTERMEDIATE_ITEMS, items == null ? "" : items.trim());
	}

	String getMobFarmerIntermediateActionMappings()
	{
		return config.mobFarmerIntermediateActionMappings() == null ? "" : config.mobFarmerIntermediateActionMappings();
	}

	void setMobFarmerIntermediateActionMappings(String mappings)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_INTERMEDIATE_ACTION_MAPPINGS, mappings == null ? "" : mappings.trim());
	}

	String getMobFarmerNeverDropItems()
	{
		return config.mobFarmerNeverDropItems() == null ? "" : config.mobFarmerNeverDropItems();
	}

	void setMobFarmerNeverDropItems(String items)
	{
		configManager.setConfiguration(CvHelperConfig.GROUP, CvHelperConfig.MOB_FARMER_NEVER_DROP_ITEMS, items == null ? "" : items.trim());
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
		status.put("targetCandidates", actionTargetCandidates(mobFarmerTarget));
		status.put("running", mobFarmerRunning.get());
		status.put("live", mobFarmerLiveMode);
		status.put("status", mobFarmerStatus.get());
		status.put("loopDelayMs", MOB_FARMER_LOOP_DELAY_MS);
		status.put("multiCombat", lastMobFarmerMultiCombat);
		status.put("engagedMode", getMobFarmerEngagedMode().name());
		status.put("aggroResponse", getMobFarmerAggroResponse().name());
		status.put("requireLineOfSight", getMobFarmerRequireLineOfSight());
		status.put("maxDistance", getMobFarmerMaxDistance());
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
		status.put("progress", lastMobFarmerProgressStatus);
		status.put("recentIntents", lastMobFarmerIntents);
		status.put("recentMenuEntries", lastMobFarmerMenuEntries);
		status.put("inventory", lastMobFarmerInventoryStatus);
		status.put("candidates", lastMobFarmerCandidates);
		status.put("lootCandidates", lastMobFarmerLootCandidates);
		return status;
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
		return out;
	}

	private Map<String, Object> mobFarmerSchedulerStatus()
	{
		Map<String, Object> out = new LinkedHashMap<>(lastMobFarmerSchedulerStatus);
		out.put("currentTick", safeValue(client::getTickCount, 0));
		out.put("lastStepTick", lastMobFarmerLoopStepTick);
		out.put("lastStepSource", lastMobFarmerLoopStepSource);
		out.put("tickDrivenWhenLoggedIn", true);
		out.put("recoveryLoopDelayMs", MOB_FARMER_LOOP_DELAY_MS);
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
					Thread.sleep(MOB_FARMER_LOOP_DELAY_MS);
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
	}

	void stopMobFarmer()
	{
		mobFarmerGeneration.incrementAndGet();
		mobFarmerRunning.set(false);
		mobFarmerLiveMode = false;
		interruptMobFarmerThread();
		mobFarmerStatus.set("stopped");
		updatePanelStatus("Mob farmer stopped");
	}

	void panicStop()
	{
		mobFarmerGeneration.incrementAndGet();
		mobFarmerRunning.set(false);
		mobFarmerLiveMode = false;
		interruptMobFarmerThread();
		actionInProgress.set(false);
		mobFarmerStatus.set("panic-stopped");
		lastEvent.set("panic-stop@" + Instant.now());
		String message = "CV Helper panic stop: loops stopped and action guard cleared";
		updatePanelStatus(message);
		clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, ""));
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
		details.put("enabled", getMobFarmerLoginRecoveryEnabled());
		details.put("loginRecoveryEnabled", getMobFarmerLoginRecoveryEnabled());
		details.put("live", live);
		details.put("gameState", gameState == null ? null : gameState.name());
		details.put("detectedScreen", detectedLoginScreen(gameState));
		details.put("clickToPlayEnabled", getMobFarmerLoginClickToPlayEnabled());
		details.put("disconnectRecoveryEnabled", getMobFarmerLoginDisconnectRecoveryEnabled());
		details.put("autoResumeAfterLogin", getMobFarmerAutoResumeAfterLogin());
		details.put("f2pWorldOnly", getMobFarmerLoginRecoveryF2pOnly());
		details.put("preferredWorld", getMobFarmerPreferredLoginWorld());
		details.put("currentWorld", safeValue(() -> client.getWorld() > 0 ? client.getWorld() : -1, -1));
		details.put("worldHost", safeValue(client::getWorldHost, null));
		details.put("worldType", currentWorldTypeText());
		details.put("antiIdle", false);
		details.put("idleTimerGuidance", "Use RuneLite Logout Timer for longer idle windows; this only recovers after logout.");

		if (!live || !getMobFarmerLoginRecoveryEnabled())
		{
			details.put("result", live ? "disabled" : "dry-run-skip");
			setMobFarmerLoginRecoveryDecision(live ? "disabled" : "dry-run-skip", details);
			return false;
		}
		if (gameState == GameState.LOGGING_IN || gameState == GameState.LOADING || gameState == GameState.HOPPING)
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
		boolean clickToPlayState = gameState == GameState.LOGIN_SCREEN || gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR;
		boolean disconnectState = gameState == GameState.CONNECTION_LOST;
		if (!clickToPlayState && !disconnectState)
		{
			details.put("result", "not-clickable-login-state");
			setMobFarmerLoginRecoveryDecision("not-clickable-login-state", details);
			return false;
		}
		if (clickToPlayState && !getMobFarmerLoginClickToPlayEnabled())
		{
			details.put("result", "click-to-play-disabled");
			setMobFarmerLoginRecoveryDecision("click-to-play-disabled", details);
			mobFarmerStatus.set("login-recovery-disabled:click-to-play");
			setMobFarmerDecision("login-recovery-click-to-play-disabled", details);
			return true;
		}
		if (disconnectState && !getMobFarmerLoginDisconnectRecoveryEnabled())
		{
			details.put("result", "disconnect-recovery-disabled");
			setMobFarmerLoginRecoveryDecision("disconnect-recovery-disabled", details);
			mobFarmerStatus.set("login-recovery-disabled:connection-lost");
			setMobFarmerDecision("login-recovery-disconnect-disabled", details);
			return true;
		}
		if (isStaleMobFarmerLoop(generation))
		{
			details.put("result", "stale-loop");
			setMobFarmerLoginRecoveryDecision("stale-loop", details);
			return true;
		}

		String blockReason = mobFarmerLoginWorldBlockReason();
		details.put("currentWorldAllowed", blockReason == null);
		if (blockReason != null)
		{
			details.put("failureReason", blockReason);
			setMobFarmerLoginRecoveryDecision("blocked:" + blockReason, details);
			mobFarmerStatus.set("login-recovery-blocked:" + blockReason);
			setMobFarmerDecision("login-recovery-blocked", details);
			updatePanelStatus("Mob farmer login recovery blocked: " + blockReason);
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
		details.put("intendedAction", disconnectState ? "Recover connection lost" : "Click login");
		details.put("actualAction", disconnectState ? "enter-key-disconnect-recovery" : "guarded-login-widget-click");
		details.put("result", "queued");
		recordMobFarmerScheduledAction(MobFarmerActionKind.LOGIN_RECOVERY, details.get("detectedScreen") == null ? String.valueOf(details.get("gameState")) : String.valueOf(details.get("detectedScreen")), details.get("actualAction").toString());
		setMobFarmerLoginRecoveryDecision("queued", details);
		mobFarmerStatus.set(disconnectState ? "login-disconnect-recovery-queued" : "login-recovery-click-queued");
		setMobFarmerDecision(disconnectState ? "login-disconnect-recovery-queued" : "login-recovery-click-queued", details);
		updatePanelStatus(disconnectState ? "Mob farmer queued connection-lost recovery" : "Mob farmer queued guarded login recovery click");
		if (disconnectState)
		{
			pressLoginEnterFallback("login-disconnect-enter-fallback", "Pressed Enter on connection-lost screen");
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

		lastMobFarmerInventoryStatus = inventoryPolicyStatus();
		if (tryMobFarmerAutoEat(localPlayer, live, generation))
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

		if (tryMobFarmerPriorityLoot(localPlayer, live, generation, "priority-before-combat"))
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
				if (getMobFarmerLootDuringCombat() && tryMobFarmerLoot(localPlayer, live, generation, "combat-window"))
				{
					return;
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

		if (!getMobFarmerAttackBeforeLoot() && tryMobFarmerLoot(localPlayer, live, generation, "idle-before-attack"))
		{
			return;
		}
		if (mobFarmerMakeProgressActive() && tryMobFarmerLoot(localPlayer, live, generation, "make-progress-before-attack"))
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
		candidate.score = distance * 1000 - (int) Math.min(5000, value / 10);

		String name = String.valueOf(item.get("name"));
		int itemId = intValue(item.get("itemId"), -1);
		int quantity = intValue(item.get("quantity"), 1);
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
		if (!explicitlyAllowed && value < getMobFarmerLootMinValueGe())
		{
			candidate.reject("below-value:" + value + "<" + getMobFarmerLootMinValueGe());
		}
		if (cvAllowlist)
		{
			candidate.note("allowlist");
		}
		if (groundItemsSupplements && groundItemsHighlighted)
		{
			candidate.note("ground-items-highlighted");
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
		report.put("gePrice", candidate.item.get("gePrice"));
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
				clientThread.invokeLater(() -> recordMobFarmerScheduledAction(actionKind, schedulerTarget, action + "-click"));
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
		int widgetId = intValue(target.get("parentId"), -1);
		if (widgetId <= 0)
		{
			widgetId = intValue(target.get("widgetId"), -1);
		}
		int itemId = intValue(target.get("itemId"), -1);
		int param0 = intValue(target.get("index"), -1);
		if (widgetId <= 0 || itemId <= 0)
		{
			return null;
		}
		for (String preferred : preferredActions)
		{
			InventoryMenuAction menu = inventoryMenuActionForOption(actions, preferred, param0, widgetId, itemId);
			if (menu != null)
			{
				return menu;
			}
		}
		return null;
	}

	private InventoryMenuAction inventoryMenuActionForOption(String[] actions, String preferred, int param0, int widgetId, int itemId)
	{
		if (actions == null || preferred == null)
		{
			return null;
		}
		for (int i = 0; i < actions.length; i++)
		{
			String action = actions[i];
			if (action == null || !action.equalsIgnoreCase(preferred))
			{
				continue;
			}
			int componentOpId = i + 2;
			MenuAction menuAction = componentOpId >= 6 ? MenuAction.CC_OP_LOW_PRIORITY : MenuAction.CC_OP;
			return new InventoryMenuAction(i, componentOpId, param0, widgetId, menuAction, componentOpId, itemId, action);
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

	private Map<String, Object> inventoryPolicyStatus()
	{
		Map<String, Object> inventory = containerValue("inventory", InventoryID.INVENTORY);
		int slotCount = intValue(inventory.get("slotCount"), 28);
		int occupied = intValue(inventory.get("occupiedSlots"), 0);
		inventory.put("freeSlots", Math.max(0, slotCount - occupied));
		inventory.put("full", occupied >= slotCount && slotCount > 0);
		inventory.put("neverDropItems", getMobFarmerNeverDropItems());
		inventory.put("dropCandidate", lowestSafeDropCandidate(inventory));
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
			int quantity = intValue(item.get("quantity"), 1);
			if (matchesItemPolicy(name, itemId, quantity, getMobFarmerNeverDropItems()))
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
		queuedAttempt.putIfAbsent("source", "pressLoginEnterFallback");
		queuedAttempt.put("eventName", eventName);
		queuedAttempt.put("plannedAction", "enter-key");
		queuedAttempt.put("usedEnterFallback", true);
		queuedAttempt.put("result", "queued");
		setLastLoginClickAttempt("queued", queuedAttempt);
		Thread loginClickThread = new Thread(() ->
		{
			Map<String, Object> result = new LinkedHashMap<>(queuedAttempt);
			try
			{
				Robot robot = new Robot();
				pressEnter(robot);
				result.put("actualActionInvoked", "enter-key");
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
		if (!config.enableLocalExport())
		{
			lastEvent.set("server-disabled");
			log.info("CV Helper local export disabled by config");
			updatePanelServerStatus();
			return;
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
			server.createContext("/entities/nearest", this::handleNearestEntityRequest);
			server.createContext("/automation/mob-farmer/status", this::handleMobFarmerStatusRequest);
			server.createContext("/automation/mob-farmer/step", this::handleMobFarmerStepRequest);
			server.createContext("/automation/mob-farmer/start", this::handleMobFarmerStartRequest);
			server.createContext("/automation/mob-farmer/stop", this::handleMobFarmerStopRequest);
			server.createContext("/automation/panic-stop", this::handlePanicStopRequest);
			server.createContext("/login/click", exchange ->
			{
				clickLoginScreen();
				writeResponse(exchange, 202, "{\"ok\":true,\"queued\":true,\"action\":\"login-click\"}");
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
			body.put("endpoints", new String[]{"/status", "/login/click", "/capture", "/capture/screen", "/capture/minimap", "/capture/latest/client-frame", "/capture/latest/screen", "/capture/latest/minimap", "/player/status", "/targets/prayer", "/targets/spell", "/targets/minimap", "/targets/inventory", "/targets/equipment", "/targets/panels", "/targets/combat", "/targets", "/entities", "/entities/nearest", "/automation/mob-farmer/status", "/automation/mob-farmer/step", "/automation/mob-farmer/start", "/automation/mob-farmer/stop", "/automation/panic-stop"});
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

	private void handleMobFarmerStatusRequest(HttpExchange exchange) throws IOException
	{
		writeJson(exchange, 200, getMobFarmerStatus());
	}

	private void handleMobFarmerStepRequest(HttpExchange exchange) throws IOException
	{
		applyMobFarmerQuery(exchange);
		boolean live = Boolean.parseBoolean(queryParam(exchange, "live"));
		runMobFarmerStep(live);
		writeJson(exchange, 202, getMobFarmerStatus());
	}

	private void handleMobFarmerStartRequest(HttpExchange exchange) throws IOException
	{
		applyMobFarmerQuery(exchange);
		boolean live = Boolean.parseBoolean(queryParam(exchange, "live"));
		startMobFarmer(live);
		writeJson(exchange, 202, getMobFarmerStatus());
	}

	private void handleMobFarmerStopRequest(HttpExchange exchange) throws IOException
	{
		stopMobFarmer();
		writeJson(exchange, 202, getMobFarmerStatus());
	}

	private void handlePanicStopRequest(HttpExchange exchange) throws IOException
	{
		panicStop();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("action", "panic-stop");
		body.put("mobFarmer", getMobFarmerStatus());
		writeJson(exchange, 202, body);
	}

	private void applyMobFarmerQuery(HttpExchange exchange)
	{
		String target = queryParam(exchange, "target");
		if (target != null && !target.trim().isEmpty())
		{
			setMobFarmerTarget(target);
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

	private Map<String, Object> boundsMap(Rectangle bounds)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("x", bounds.x);
		out.put("y", bounds.y);
		out.put("width", bounds.width);
		out.put("height", bounds.height);
		return out;
	}

	private Map<String, Object> pointMap(int x, int y)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("x", x);
		out.put("y", y);
		return out;
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

	private long longValue(Object value)
	{
		return value instanceof Number ? ((Number) value).longValue() : 0L;
	}

	private int intValue(Object value, int fallback)
	{
		return value instanceof Number ? ((Number) value).intValue() : fallback;
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

	private String friendlyName(String value)
	{
		String[] words = value.toLowerCase().split("_");
		StringBuilder out = new StringBuilder();
		for (String word : words)
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (out.length() > 0)
			{
				out.append(' ');
			}
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return out.toString();
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

	private String actionsText(Object actions)
	{
		if (actions instanceof String[])
		{
			return Arrays.toString((String[]) actions);
		}
		return actions == null ? "" : String.valueOf(actions);
	}

	private String normalize(String value)
	{
		return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
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

	private <T> T safeValue(java.util.function.Supplier<T> supplier, T fallback)
	{
		try
		{
			return supplier.get();
		}
		catch (RuntimeException e)
		{
			return fallback;
		}
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
}
