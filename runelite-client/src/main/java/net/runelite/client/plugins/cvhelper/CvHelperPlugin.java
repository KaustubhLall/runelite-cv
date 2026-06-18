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
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
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
import net.runelite.client.util.ImageUtil;
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
	private static final int ACTION_SLOT_COUNT = 8;
	private static final int ACTION_RESOLVE_RETRIES = 4;
	private static final int ACTION_RESOLVE_DELAY_MS = 80;
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

	private static final class PanelSwitchTarget
	{
		private final String panel;
		private final Integer keyCode;
		private final Point clickPoint;

		private PanelSwitchTarget(String panel, Integer keyCode, Point clickPoint)
		{
			this.panel = panel;
			this.keyCode = keyCode;
			this.clickPoint = clickPoint;
		}

		private boolean usesHotkey()
		{
			return keyCode != null;
		}
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
		startServer();
	}

	@Override
	protected void shutDown()
	{
		log.info("CV Helper stopping");
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

	private void registerHotkeys()
	{
		keyManager.registerKeyListener(debugHotkeyListener);
		keyManager.registerKeyListener(printBoundsHotkeyListener);
		keyManager.registerKeyListener(captureScreenHotkeyListener);
		keyManager.registerKeyListener(refreshEntitiesHotkeyListener);
		keyManager.registerKeyListener(nearestEntityHotkeyListener);
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
		for (HotkeyListener listener : actionHotkeyListeners)
		{
			keyManager.unregisterKeyListener(listener);
		}
		actionHotkeyListeners.clear();
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
				return keybind == null ? Keybind.NOT_SET : keybind;
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
			return;
		}
		CvHelperActionInvocationMode effectiveInvocationMode = invocationMode == null ? CvHelperActionInvocationMode.AUTO : invocationMode;

		Point currentMouseScreenPoint = currentMouseScreenPoint();
		clientThread.invokeLater(() ->
		{
			String previousPanel = String.valueOf(interfaceStatus().get("activeSidePanel"));
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
			Map<String, Object> target = resolveActionTarget(surface, targetLabel);
			if (target == null)
			{
				if (retriesRemaining > 0)
				{
					scheduleActionResolve(slot, surface, targetLabel, clickAfterMode, invocationMode, returnPanel, returnMouseCenter, previousPanel, originalMouseScreenPoint, retriesRemaining - 1);
					return;
				}
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CV Helper action " + slot + " | no target matched " + surface + " / " + targetLabel, "");
				return;
			}

			Map<String, Object> clickPoint = firstPoint(target, "clickPoint", "center", "canvasTileCenter");
			if (clickPoint == null)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CV Helper action " + slot + " | matched target has no canvas click point", "");
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
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CV Helper action " + slot + " | target has no widget action for " + surface + " / " + targetLabel, "");
					return;
				}
			}
			if (targetScreenPoint == null)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CV Helper action " + slot + " | target is off-canvas", "");
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
				Thread.sleep(ACTION_RESOLVE_DELAY_MS);
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
				activatePanel(robot, panelTarget);
				robot.delay(timing(config.actionPanelOpenDelayMs(), 0, 1500));
				performConfiguredActionResolved(slot, surface, targetLabel, clickAfterMode, invocationMode, returnPanel, returnMouseCenter, previousPanel, originalMouseScreenPoint);
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				log.warn("CV Helper action panel-open failed", e);
				clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CV Helper action " + slot + " panel open failed: " + e.getMessage(), ""));
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
				boolean returnedPanel = maybeReturnPanelWithHotkey(robot, returnPanelTarget);
				if (mouseScreenPoint != null)
				{
					robot.delay(timing(config.actionWidgetTargetDelayMs(), 0, 3000));
					clickScreenPoint(robot, mouseScreenPoint);
					clickedMouseTarget = true;
				}
				if (returnPanelTarget != null && !returnedPanel && (!targetedSpell || clickedMouseTarget))
				{
					robot.delay(timing(config.actionReturnPanelDelayMs(), 0, 3000));
					activatePanel(robot, returnPanelTarget);
				}
				if (centerPoint != null)
				{
					robot.delay(timing(config.actionMouseRestoreDelayMs(), 0, 3000));
					robot.mouseMove(centerPoint.x, centerPoint.y);
				}
				clientThread.invokeLater(() -> client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"CV Helper action " + slot + " | clicked " + surface + " " + targetLabelForMessage(target) + " at " + randomizedClickPoint + (mouseScreenPoint == null ? " | no mouse target click" : " | then mouse@" + mouseScreenPoint.x + "," + mouseScreenPoint.y),
					""
				));
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				log.warn("CV Helper action hotkey failed", e);
				clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CV Helper action " + slot + " failed: " + e.getMessage(), ""));
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
				boolean returnedPanel = false;
				boolean usedPhysicalFallback = false;
				if (targetedSpell && !waitForSelectedWidget())
				{
					if (physicalFallbackPoint == null)
					{
						clientThread.invokeLater(() -> client.addChatMessage(
							ChatMessageType.GAMEMESSAGE,
							"",
							"CV Helper action " + slot + " | invoked " + surface + " " + targetLabelForMessage(target) + " via widget | spell was not selected before timeout, skipped mouse target click | " + selectedWidgetDebug(),
							""
						));
						return;
					}
					clickScreenPoint(robot, physicalFallbackPoint);
					usedPhysicalFallback = true;
				}
				if (mouseScreenPoint != null)
				{
					returnedPanel = maybeReturnPanelWithHotkey(robot, returnPanelTarget);
					robot.delay(timing(config.actionWidgetTargetDelayMs(), 0, 3000));
					clickScreenPoint(robot, mouseScreenPoint);
					clickedMouseTarget = true;
				}
				else
				{
					returnedPanel = maybeReturnPanelWithHotkey(robot, returnPanelTarget);
				}
				if (returnPanelTarget != null && !returnedPanel && (!targetedSpell || clickedMouseTarget))
				{
					robot.delay(timing(config.actionReturnPanelDelayMs(), 0, 3000));
					activatePanel(robot, returnPanelTarget);
				}
				if (restoreMousePoint != null)
				{
					robot.delay(timing(config.actionMouseRestoreDelayMs(), 0, 3000));
					robot.mouseMove(restoreMousePoint.x, restoreMousePoint.y);
				}
				String resultMessage = "CV Helper action " + slot + " | invoked " + surface + " " + targetLabelForMessage(target) + " via widget"
					+ (usedPhysicalFallback ? " | physical fallback selected" : "")
					+ (mouseScreenPoint == null ? " | no mouse target click" : " | then mouse@" + mouseScreenPoint.x + "," + mouseScreenPoint.y);
				clientThread.invokeLater(() -> client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					resultMessage,
					""
				));
			}
			catch (RuntimeException | java.awt.AWTException e)
			{
				log.warn("CV Helper action widget follow-up failed", e);
				clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CV Helper action " + slot + " widget follow-up failed: " + e.getMessage(), ""));
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

		String label = normalize(targetLabelForMessage(target) + " " + target.get("name") + " " + target.get("text"));
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
		String label = normalize(targetLabelForMessage(target) + " " + target.get("name") + " " + target.get("text"));
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

	private PanelSwitchTarget panelReturnTarget(String previousPanel)
	{
		return panelSwitchTarget(previousPanel);
	}

	private PanelSwitchTarget requiredPanelTarget(CvHelperActionSurface surface, String activePanel)
	{
		String requiredPanel = requiredPanelName(surface);
		if (requiredPanel == null || requiredPanel.equals(activePanel))
		{
			return null;
		}

		return panelSwitchTarget(requiredPanel);
	}

	private PanelSwitchTarget panelSwitchTarget(String panelName)
	{
		if (panelName == null || panelName.isEmpty() || "unknown".equals(panelName))
		{
			return null;
		}

		Integer keyCode = panelKeyCode(panelName);
		if (keyCode != null)
		{
			return new PanelSwitchTarget(panelName, keyCode, null);
		}

		Map<String, Object> panelTarget = findTargetByLabel(collectPanelTargets(), panelName, panelName.equals("spellbook") ? "magic" : panelName);
		Map<String, Object> clickPoint = panelTarget == null ? null : firstPoint(panelTarget, "clickPoint", "center");
		Point screenPoint = clickPoint == null ? null : canvasPointToScreen(clickPoint);
		return screenPoint == null ? null : new PanelSwitchTarget(panelName, null, screenPoint);
	}

	private Integer panelKeyCode(String panelName)
	{
		switch (panelName)
		{
			case "combat":
				return KeyEvent.VK_F1;
			case "inventory":
				return KeyEvent.VK_F4;
			case "equipment":
				return KeyEvent.VK_F5;
			case "prayer":
				return KeyEvent.VK_F6;
			case "spellbook":
				return KeyEvent.VK_F7;
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
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(18);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private boolean maybeReturnPanelWithHotkey(Robot robot, PanelSwitchTarget panelTarget)
	{
		if (panelTarget == null || !panelTarget.usesHotkey())
		{
			return false;
		}
		robot.delay(timing(config.actionReturnPanelDelayMs(), 0, 3000));
		activatePanel(robot, panelTarget);
		return true;
	}

	private void activatePanel(Robot robot, PanelSwitchTarget panelTarget)
	{
		if (panelTarget == null)
		{
			return;
		}
		if (panelTarget.keyCode != null)
		{
			robot.keyPress(panelTarget.keyCode);
			robot.delay(18);
			robot.keyRelease(panelTarget.keyCode);
			return;
		}
		if (panelTarget.clickPoint != null)
		{
			clickScreenPoint(robot, panelTarget.clickPoint);
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

	private Map<String, Object> resolveActionTarget(CvHelperActionSurface surface, String targetLabel)
	{
		if (surface == CvHelperActionSurface.NEAREST_ENTITY)
		{
			lastEntities = collectEntities();
			return nearestClickableEntity(lastEntities);
		}

		List<Map<String, Object>> targets = collectActionSurfaceTargets(surface);
		String needle = normalize(targetLabel);
		if (needle.isEmpty())
		{
			return targets.isEmpty() ? null : targets.get(0);
		}

		return findTargetByLabel(targets, needle);
	}

	private Map<String, Object> findTargetByLabel(List<Map<String, Object>> targets, String... needles)
	{
		for (Map<String, Object> target : targets)
		{
			String haystack = normalize(targetLabelForMessage(target) + " " + target.get("name") + " " + target.get("text") + " " + Arrays.toString((String[]) target.get("actions")));
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
			body.put("endpoints", new String[]{"/status", "/capture", "/capture/screen", "/capture/minimap", "/capture/latest/client-frame", "/capture/latest/screen", "/capture/latest/minimap", "/player/status", "/targets/prayer", "/targets/spell", "/targets/minimap", "/targets/inventory", "/targets/equipment", "/targets/panels", "/targets/combat", "/targets", "/entities", "/entities/nearest"});
			Map<String, Object> playerStatus = getPlayerStatusOnClientThread();
			body.put("player", playerStatus);
			body.put("spellbook", playerStatus.get("spellbook"));
			body.put("interfaces", playerStatus.get("interfaces"));
			body.put("vitals", playerStatus.get("vitals"));
			body.put("wealth", playerStatus.get("wealth"));
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
			collectComponentTarget("prayer", prayers[i].name(), PRAYER_COMPONENTS[i], targets);
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
		Map<String, Object> target = new LinkedHashMap<>();
		target.put("surface", surface);
		target.put("widgetId", widget.getId());
		target.put("parentId", widget.getParentId());
		target.put("index", widget.getIndex());
		target.put("type", widget.getType());
		target.put("name", name);
		target.put("text", text);
		target.put("actions", actions == null ? new String[0] : actions);
		target.put("itemId", widget.getItemId());
		target.put("itemQuantity", widget.getItemQuantity());
		target.put("spriteId", widget.getSpriteId());
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

			boolean hasUsefulMetadata = surface.contains("spell") || !name.isEmpty() || !text.isEmpty() || hasActions(actions);
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
				target.put("actions", actions == null ? new String[0] : actions);
				target.put("spriteId", widget.getSpriteId());
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
		interfaces.put("activeSidePanel", activeSidePanelName(interfaces));
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
		summary.put("slotCount", containerItems.length);
		summary.put("occupiedSlots", occupiedSlots);
		summary.put("gePrice", gePrice);
		summary.put("haPrice", haPrice);
		summary.put("items", items);
		return summary;
	}

	private long longValue(Object value)
	{
		return value instanceof Number ? ((Number) value).longValue() : 0L;
	}

	private String activeSidePanelName(Map<String, Object> interfaces)
	{
		if (Boolean.TRUE.equals(interfaces.get("combatVisible")))
		{
			return "combat";
		}
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
		WorldPoint origin = localPlayer == null ? null : localPlayer.getWorldLocation();
		for (Player player : client.getPlayers())
		{
			addActorEntity(entities, "player", player, origin);
		}
		for (NPC npc : client.getNpcs())
		{
			addActorEntity(entities, "npc", npc, origin);
		}
		return entities;
	}

	private void addActorEntity(List<Map<String, Object>> entities, String type, Actor actor, WorldPoint origin)
	{
		if (actor == null || actor.getName() == null)
		{
			return;
		}
		Map<String, Object> canvasBounds = actorBounds(actor);
		Map<String, Object> boundsCenter = centerFromBounds(canvasBounds);
		Map<String, Object> tileCenter = canvasTileCenter(actor);
		Map<String, Object> entity = new LinkedHashMap<>();
		entity.put("type", type);
		entity.put("name", actor.getName());
		entity.put("combatLevel", actor.getCombatLevel());
		entity.put("animation", actor.getAnimation());
		entity.put("worldLocation", pointValue(actor.getWorldLocation()));
		entity.put("localLocation", pointValue(actor.getLocalLocation()));
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
			entity.put("id", npc.getId());
		}
		entities.add(entity);
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
		}
		else
		{
			addTargetLabels(labels, lastTargetsForSurface(surface));
		}
		return new ArrayList<>(labels);
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
		String haystack = normalize(String.valueOf(target.get("name")) + " " + target.get("text") + " " + Arrays.toString((String[]) target.get("actions")));
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
		if (fallback != null && !fallback.isEmpty())
		{
			return fallback;
		}
		return target.get("surface") + "#" + target.get("index");
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
