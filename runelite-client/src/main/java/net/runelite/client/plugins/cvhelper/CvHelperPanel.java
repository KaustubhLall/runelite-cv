/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class CvHelperPanel extends PluginPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final CvHelperPlugin plugin;
	private final JLabel status = new JLabel("Ready");
	private final JLabel serverStatus = new JLabel("Server: starting");
	private final JLabel loginRecoveryStatus = new JLabel("Login: unknown");

	CvHelperPanel(CvHelperPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("CV Helper");
		title.setForeground(Color.WHITE);

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.add(title, BorderLayout.WEST);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton captureButton = new JButton("Capture screenshot");
		captureButton.addActionListener(e -> plugin.captureScreenshot());

		JButton screenButton = new JButton("Capture screen");
		screenButton.addActionListener(e -> plugin.captureScreen());

		JButton minimapButton = new JButton("Capture minimap");
		minimapButton.addActionListener(e -> plugin.captureMinimap());

		JButton refreshButton = new JButton("Refresh status");
		refreshButton.addActionListener(e -> refreshStatus());

		JButton loginButton = new JButton("Click login");
		loginButton.addActionListener(e -> plugin.clickLoginScreen());

		JButton prayerButton = new JButton("Prayer targets");
		prayerButton.addActionListener(e -> plugin.refreshPrayerTargets());

		JButton debugButton = new JButton("Debug overlay");
		debugButton.addActionListener(e -> plugin.debugOverlayState());

		JButton printBoundsButton = new JButton("Print overlay bounds");
		printBoundsButton.addActionListener(e -> plugin.printOverlayCoordinates());

		JButton panicButton = new JButton("PANIC STOP");
		panicButton.setForeground(Color.RED);
		panicButton.addActionListener(e -> plugin.panicStop());

		buttons.add(panicButton);
		buttons.add(captureButton);
		buttons.add(screenButton);
		buttons.add(minimapButton);
		buttons.add(refreshButton);
		buttons.add(loginButton);
		buttons.add(prayerButton);
		buttons.add(debugButton);
		buttons.add(printBoundsButton);

		JPanel toggles = new JPanel(new GridLayout(0, 1, 0, 4));
		toggles.setBackground(ColorScheme.DARK_GRAY_COLOR);
		toggles.setBorder(BorderFactory.createTitledBorder("Overlays"));

		JCheckBox hoverOverlay = new JCheckBox("Mouse coordinates", plugin.getConfig().showHoverOverlay());
		JCheckBox widgetInfo = new JCheckBox("Widget info labels", plugin.getConfig().showWidgetInfo());
		JCheckBox prayerTargets = new JCheckBox("Prayer target boxes", plugin.getConfig().showPrayerTargets());
		JCheckBox spellTargets = new JCheckBox("Spell target boxes", plugin.getConfig().showSpellTargets());
		JCheckBox minimapTargets = new JCheckBox("Minimap/orb boxes", plugin.getConfig().showMinimapTargets());
		JCheckBox inventoryTargets = new JCheckBox("Inventory slot boxes", plugin.getConfig().showInventoryTargets());
		JCheckBox equipmentTargets = new JCheckBox("Equipment slot boxes", plugin.getConfig().showEquipmentTargets());
		JCheckBox panelTargets = new JCheckBox("Panel tab boxes", plugin.getConfig().showPanelTargets());
		JCheckBox combatTargets = new JCheckBox("Combat option boxes", plugin.getConfig().showCombatTargets());
		JCheckBox entityTargets = new JCheckBox("Nearby entity boxes", plugin.getConfig().showEntityTargets());
		JCheckBox skillFarmerTargets = new JCheckBox("Skilling target boxes", plugin.getConfig().showSkillFarmerTargets());
		JCheckBox targetLabels = new JCheckBox("Target labels", plugin.getConfig().showTargetLabels());
		JCheckBox localExport = new JCheckBox("Localhost export", plugin.getConfig().enableLocalExport());

		hoverOverlay.addActionListener(e -> plugin.setShowHoverOverlay(hoverOverlay.isSelected()));
		widgetInfo.addActionListener(e -> plugin.setShowWidgetInfo(widgetInfo.isSelected()));
		prayerTargets.addActionListener(e -> plugin.setShowPrayerTargets(prayerTargets.isSelected()));
		spellTargets.addActionListener(e -> plugin.setShowSpellTargets(spellTargets.isSelected()));
		minimapTargets.addActionListener(e -> plugin.setShowMinimapTargets(minimapTargets.isSelected()));
		inventoryTargets.addActionListener(e -> plugin.setShowInventoryTargets(inventoryTargets.isSelected()));
		equipmentTargets.addActionListener(e -> plugin.setShowEquipmentTargets(equipmentTargets.isSelected()));
		panelTargets.addActionListener(e -> plugin.setShowPanelTargets(panelTargets.isSelected()));
		combatTargets.addActionListener(e -> plugin.setShowCombatTargets(combatTargets.isSelected()));
		entityTargets.addActionListener(e -> plugin.setShowEntityTargets(entityTargets.isSelected()));
		skillFarmerTargets.addActionListener(e -> plugin.setShowSkillFarmerTargets(skillFarmerTargets.isSelected()));
		targetLabels.addActionListener(e -> plugin.setShowTargetLabels(targetLabels.isSelected()));
		localExport.addActionListener(e -> plugin.setLocalExportEnabled(localExport.isSelected()));

		for (JCheckBox checkbox : new JCheckBox[]{hoverOverlay, widgetInfo, prayerTargets, spellTargets, minimapTargets, inventoryTargets, equipmentTargets, panelTargets, combatTargets, entityTargets, skillFarmerTargets, targetLabels, localExport})
		{
			styleCheckbox(checkbox);
			toggles.add(checkbox);
		}

		JPanel prayerSection = createNestedSection("Prayer toggles", plugin.getPrayerNames(), true, true);
		JPanel spellSection = createNestedSection("Spellbook toggles", plugin.getSpellbookNames(), false, true);
		JPanel actionSection = createActionSection();
		JPanel mobFarmerSection = createMobFarmerSection();

		JPanel serverSettings = new JPanel(new BorderLayout(0, 4));
		serverSettings.setBackground(ColorScheme.DARK_GRAY_COLOR);
		serverSettings.setBorder(BorderFactory.createTitledBorder("Server"));

		JTextField webhookUrl = new JTextField(plugin.getConfig().webhookUrl());
		webhookUrl.setToolTipText("Optional Python webhook URL, for example http://127.0.0.1:8000/runelite-events");

		JButton saveWebhook = new JButton("Save webhook");
		saveWebhook.addActionListener(e ->
		{
			plugin.setWebhookUrl(webhookUrl.getText().trim());
			updateStatus("Webhook saved");
		});

		serverSettings.add(webhookUrl, BorderLayout.CENTER);
		serverSettings.add(saveWebhook, BorderLayout.SOUTH);

		JLabel playerStatus = new JLabel("Player export: pending");
		playerStatus.setForeground(Color.LIGHT_GRAY);
		playerStatus.setBorder(new EmptyBorder(0, 0, 0, 0));
		JButton refreshPlayer = new JButton("Refresh player export");
		refreshPlayer.addActionListener(e ->
		{
			playerStatus.setText(plugin.getPlayerStatus().toString());
			updateStatus("Player export refreshed");
		});

		JPanel playerPanel = new JPanel(new BorderLayout(0, 4));
		playerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		playerPanel.setBorder(BorderFactory.createTitledBorder("Player Status"));
		playerPanel.add(playerStatus, BorderLayout.CENTER);
		playerPanel.add(refreshPlayer, BorderLayout.SOUTH);

		JPanel loginRecoveryPanel = new JPanel(new BorderLayout(0, 4));
		loginRecoveryPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		loginRecoveryPanel.setBorder(BorderFactory.createTitledBorder("Login Recovery"));
		loginRecoveryStatus.setForeground(Color.LIGHT_GRAY);
		JButton refreshLoginRecovery = new JButton("Refresh login status");
		refreshLoginRecovery.addActionListener(e ->
		{
			loginRecoveryStatus.setText(plugin.getLoginRecoveryStatusText());
			updateStatus("Login recovery status refreshed");
		});
		loginRecoveryPanel.add(loginRecoveryStatus, BorderLayout.CENTER);
		loginRecoveryPanel.add(refreshLoginRecovery, BorderLayout.SOUTH);

		JPanel center = new JPanel(new BorderLayout(0, 4));
		center.setBackground(ColorScheme.DARK_GRAY_COLOR);
		center.add(toggles, BorderLayout.NORTH);
		JPanel lower = new JPanel();
		lower.setLayout(new BoxLayout(lower, BoxLayout.Y_AXIS));
		lower.setBackground(ColorScheme.DARK_GRAY_COLOR);
		lower.add(prayerSection);
		lower.add(spellSection);
		lower.add(actionSection);
		lower.add(mobFarmerSection);
		lower.add(serverSettings);
		lower.add(playerPanel);
		lower.add(loginRecoveryPanel);
		center.add(lower, BorderLayout.SOUTH);

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.add(buttons, BorderLayout.NORTH);
		content.add(center, BorderLayout.CENTER);

		JPanel footer = new JPanel(new GridLayout(0, 1, 0, 4));
		footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		footer.add(serverStatus);
		footer.add(status);
		content.add(footer, BorderLayout.SOUTH);

		status.setForeground(Color.LIGHT_GRAY);
		status.setBorder(new EmptyBorder(8, 0, 0, 0));
		status.setToolTipText("Latest plugin status");
		status.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		serverStatus.setForeground(Color.LIGHT_GRAY);

		add(header, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
		refreshStatus();
	}

	private JPanel createNestedSection(String title, List<String> entries, boolean prayers, boolean collapsed)
	{
		JPanel section = new JPanel(new BorderLayout(0, 2));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.setBorder(BorderFactory.createTitledBorder(title));

		JPanel list = new JPanel(new GridLayout(0, 1, 0, 0));
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (String entry : entries)
		{
			JCheckBox box = new JCheckBox(entry, true);
			styleCheckbox(box);
			box.addActionListener(e ->
			{
				if (prayers)
				{
					plugin.setPrayerEnabled(entry, box.isSelected());
				}
				else
				{
					plugin.setSpellEnabled(entry, box.isSelected());
				}
			});
			list.add(box);
		}

		JToggleButton expand = new JToggleButton(collapsed ? "Expand" : "Collapse");
		list.setVisible(!collapsed);
		expand.setSelected(collapsed);
		expand.addActionListener(e ->
		{
			boolean isCollapsed = expand.isSelected();
			expand.setText(isCollapsed ? "Expand" : "Collapse");
			list.setVisible(!isCollapsed);
			section.revalidate();
		});
		expand.setBackground(ColorScheme.DARK_GRAY_COLOR);
		expand.setForeground(Color.LIGHT_GRAY);

		section.add(expand, BorderLayout.NORTH);
		section.add(list, BorderLayout.CENTER);
		setCompact(section);
		return section;
	}

	private JPanel createActionSection()
	{
		JPanel section = new JPanel(new BorderLayout(0, 4));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.setBorder(BorderFactory.createTitledBorder("Action hotkeys"));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel help = new JLabel("<html>Compact cards use the same targets as the verifier. Fallback list: <b>Bind | Ice Barrage</b>. Memory sequence: <b>food -> brew</b>.</html>");
		help.setForeground(Color.LIGHT_GRAY);
		help.setBorder(new EmptyBorder(0, 0, 6, 0));
		stretch(help);
		body.add(help);

		for (int slot = 1; slot <= plugin.getActionSlotCount(); slot++)
		{
			body.add(createActionSlot(slot));
			if (slot < plugin.getActionSlotCount())
			{
				JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
				stretch(separator);
				body.add(separator);
			}
		}

		JToggleButton expand = new JToggleButton("Expand");
		body.setVisible(false);
		expand.setSelected(true);
		expand.addActionListener(e ->
		{
			boolean isCollapsed = expand.isSelected();
			expand.setText(isCollapsed ? "Expand" : "Collapse");
			body.setVisible(!isCollapsed);
			section.revalidate();
		});
		expand.setBackground(ColorScheme.DARK_GRAY_COLOR);
		expand.setForeground(Color.LIGHT_GRAY);

		section.add(expand, BorderLayout.NORTH);
		section.add(body, BorderLayout.CENTER);
		setCompact(section);
		return section;
	}

	private JPanel createActionSlot(int slot)
	{
		JPanel panel = new JPanel(new BorderLayout(0, 4));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(BorderFactory.createTitledBorder("Action " + slot));

		KeyCaptureButton hotkey = new KeyCaptureButton(plugin.getActionHotkey(slot), value -> plugin.setActionHotkey(slot, value));
		JCheckBox enabled = new JCheckBox("Enabled", plugin.getActionEnabled(slot));
		styleCheckbox(enabled);
		enabled.addActionListener(e -> plugin.setActionEnabled(slot, enabled.isSelected()));

		JComboBox<CvHelperActionSurface> surface = new JComboBox<>(CvHelperActionSurface.values());
		surface.setSelectedItem(plugin.getActionSurface(slot));

		JComboBox<String> target = new JComboBox<>();
		target.setEditable(true);
		target.setToolTipText("Examples: Protect from Magic, High Level Alchemy, inventory slot 1");
		populateTargetChoices(target, plugin.getActionSurface(slot), plugin.getActionTarget(slot));
		target.getEditor().getEditorComponent().addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				String typed = selectedTarget(target);
				Timer timer = new Timer(80, ignored -> populateTargetChoices(target, (CvHelperActionSurface) surface.getSelectedItem(), typed, true));
				timer.setRepeats(false);
				timer.start();
			}
		});
		JButton saveTarget = new JButton("Save target");
		saveTarget.addActionListener(e -> plugin.setActionTarget(slot, selectedTarget(target)));

		surface.addActionListener(e ->
		{
			CvHelperActionSurface selected = (CvHelperActionSurface) surface.getSelectedItem();
			plugin.setActionSurface(slot, selected);
			populateTargetChoices(target, selected, selectedTarget(target));
		});

		JComboBox<CvHelperClickAfterMode> clickAfterMode = new JComboBox<>(CvHelperClickAfterMode.values());
		clickAfterMode.setSelectedItem(plugin.getActionClickAfterMode(slot));
		clickAfterMode.addActionListener(e -> plugin.setActionClickAfterMode(slot, (CvHelperClickAfterMode) clickAfterMode.getSelectedItem()));

		JComboBox<CvHelperActionInvocationMode> invocationMode = new JComboBox<>(CvHelperActionInvocationMode.values());
		invocationMode.setSelectedItem(plugin.getActionInvocationMode(slot));
		invocationMode.addActionListener(e -> plugin.setActionInvocationMode(slot, (CvHelperActionInvocationMode) invocationMode.getSelectedItem()));

		JComboBox<CvHelperPrayerActionMode> prayerMode = new JComboBox<>(CvHelperPrayerActionMode.values());
		prayerMode.setSelectedItem(plugin.getActionPrayerMode(slot));
		prayerMode.addActionListener(e -> plugin.setActionPrayerMode(slot, (CvHelperPrayerActionMode) prayerMode.getSelectedItem()));

		JComboBox<CvHelperSpellAvailabilityMode> spellGuard = new JComboBox<>(CvHelperSpellAvailabilityMode.values());
		spellGuard.setSelectedItem(plugin.getActionSpellAvailabilityMode(slot));
		spellGuard.addActionListener(e -> plugin.setActionSpellAvailabilityMode(slot, (CvHelperSpellAvailabilityMode) spellGuard.getSelectedItem()));

		JButton refreshChoices = new JButton("Refresh choices");
		refreshChoices.addActionListener(e ->
		{
			CvHelperActionSurface selected = (CvHelperActionSurface) surface.getSelectedItem();
			plugin.refreshActionSurface(selected);
			Timer timer = new Timer(250, ignored -> populateTargetChoices(target, selected, selectedTarget(target)));
			timer.setRepeats(false);
			timer.start();
		});

		JCheckBox returnPanel = new JCheckBox("Return previous tab", plugin.getActionReturnPanel(slot));
		returnPanel.setToolTipText("Switch back to the previously open side tab. Function keys are used when possible so selected spells are not consumed.");
		styleCheckbox(returnPanel);
		returnPanel.addActionListener(e -> plugin.setActionReturnPanel(slot, returnPanel.isSelected()));

		JCheckBox returnMouseCenter = new JCheckBox("Restore mouse position", plugin.getActionReturnMouseCenter(slot));
		returnMouseCenter.setToolTipText("Move the OS mouse back to the position captured when the action started.");
		styleCheckbox(returnMouseCenter);
		returnMouseCenter.addActionListener(e -> plugin.setActionReturnMouseCenter(slot, returnMouseCenter.isSelected()));

		JButton run = new JButton("Run action " + slot);
		run.addActionListener(e -> plugin.performConfiguredAction(slot));

		JButton resetMemory = new JButton("Reset memory");
		resetMemory.addActionListener(e -> plugin.resetActionSequence(slot));

		JPanel top = new JPanel(new GridLayout(1, 3, 4, 0));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);
		top.add(enabled);
		top.add(hotkey);
		top.add(run);

		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		main.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (JComponent component : new JComponent[]{
			label("Surface"),
			surface,
			label("Target or list"),
			target
		})
		{
			stretch(component);
			main.add(component);
		}

		JPanel advancedBody = new JPanel();
		advancedBody.setLayout(new BoxLayout(advancedBody, BoxLayout.Y_AXIS));
		advancedBody.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (JComponent component : new JComponent[]{
			saveTarget,
			refreshChoices,
			resetMemory,
			label("Invocation"),
			invocationMode,
			label("Prayer mode"),
			prayerMode,
			label("Spell guard"),
			spellGuard,
			label("Click-after"),
			clickAfterMode,
			returnPanel,
			returnMouseCenter
		})
		{
			stretch(component);
			advancedBody.add(component);
		}
		advancedBody.setVisible(false);

		JToggleButton advanced = new JToggleButton("Advanced");
		advanced.setSelected(true);
		advanced.setBackground(ColorScheme.DARK_GRAY_COLOR);
		advanced.setForeground(Color.LIGHT_GRAY);
		advanced.addActionListener(e ->
		{
			boolean isCollapsed = advanced.isSelected();
			advanced.setText(isCollapsed ? "Advanced" : "Hide advanced");
			advancedBody.setVisible(!isCollapsed);
			panel.revalidate();
		});

		JPanel bottom = new JPanel(new BorderLayout(0, 2));
		bottom.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bottom.add(advanced, BorderLayout.NORTH);
		bottom.add(advancedBody, BorderLayout.CENTER);

		panel.add(top, BorderLayout.NORTH);
		panel.add(main, BorderLayout.CENTER);
		panel.add(bottom, BorderLayout.SOUTH);
		setCompact(panel);
		return panel;
	}

	private JPanel createMobFarmerSection()
	{
		JPanel section = new JPanel(new BorderLayout(0, 4));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.setBorder(BorderFactory.createTitledBorder("Mob farmer"));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField target = new JTextField(plugin.getMobFarmerTarget());
		target.setToolTipText("Partial name or id:<npc id>, for example cow or id:2790");
		JComboBox<CvHelperMobEngagedMode> engagedMode = new JComboBox<>(CvHelperMobEngagedMode.values());
		engagedMode.setSelectedItem(plugin.getMobFarmerEngagedMode());
		engagedMode.addActionListener(e -> plugin.setMobFarmerEngagedMode((CvHelperMobEngagedMode) engagedMode.getSelectedItem()));
		JComboBox<CvHelperMobAggroResponse> aggroResponse = new JComboBox<>(CvHelperMobAggroResponse.values());
		aggroResponse.setSelectedItem(plugin.getMobFarmerAggroResponse());
		aggroResponse.addActionListener(e -> plugin.setMobFarmerAggroResponse((CvHelperMobAggroResponse) aggroResponse.getSelectedItem()));
		JCheckBox requireLineOfSight = new JCheckBox("Require line of sight", plugin.getMobFarmerRequireLineOfSight());
		styleCheckbox(requireLineOfSight);
		requireLineOfSight.addActionListener(e -> plugin.setMobFarmerRequireLineOfSight(requireLineOfSight.isSelected()));
		JTextField maxDistance = new JTextField(String.valueOf(plugin.getMobFarmerMaxDistance()));
		maxDistance.setToolTipText("0 disables the distance guard.");
		JComboBox<CvHelperMobInteractionMode> attackInteraction = new JComboBox<>(CvHelperMobInteractionMode.values());
		attackInteraction.setSelectedItem(plugin.getMobFarmerAttackInteractionMode());
		attackInteraction.addActionListener(e -> plugin.setMobFarmerAttackInteractionMode((CvHelperMobInteractionMode) attackInteraction.getSelectedItem()));

		JCheckBox autoEatEnabled = new JCheckBox("Auto-eat", plugin.getMobFarmerAutoEatEnabled());
		styleCheckbox(autoEatEnabled);
		autoEatEnabled.addActionListener(e -> plugin.setMobFarmerAutoEatEnabled(autoEatEnabled.isSelected()));
		JTextField eatThreshold = new JTextField(String.valueOf(plugin.getMobFarmerEatHitpointPercent()));
		eatThreshold.setToolTipText("Eat when HP percent is at or below this value.");
		JTextField foodItems = new JTextField(plugin.getMobFarmerFoodItems());
		foodItems.setToolTipText("Food names or id:<item id>, separated by |, comma, semicolon, or newlines.");
		JCheckBox stopIfNoFood = new JCheckBox("Stop if no food", plugin.getMobFarmerStopIfNoFood());
		styleCheckbox(stopIfNoFood);
		stopIfNoFood.addActionListener(e -> plugin.setMobFarmerStopIfNoFood(stopIfNoFood.isSelected()));
		JCheckBox survivalPreempts = new JCheckBox("Survival preempts actions", plugin.getMobFarmerSurvivalPreemptsActions());
		styleCheckbox(survivalPreempts);
		survivalPreempts.setToolTipText("When HP is low, auto-eat takes priority over loot and combat.");
		survivalPreempts.addActionListener(e -> plugin.setMobFarmerSurvivalPreemptsActions(survivalPreempts.isSelected()));
		JCheckBox loginRecovery = new JCheckBox("Recover after logout", plugin.getMobFarmerLoginRecoveryEnabled());
		styleCheckbox(loginRecovery);
		loginRecovery.setToolTipText("Clicks RuneLite's visible login widget after a normal logout; it does not generate anti-idle input.");
		loginRecovery.addActionListener(e -> plugin.setMobFarmerLoginRecoveryEnabled(loginRecovery.isSelected()));
		JCheckBox loginRecoveryF2p = new JCheckBox("F2P-world recovery only", plugin.getMobFarmerLoginRecoveryF2pOnly());
		styleCheckbox(loginRecoveryF2p);
		loginRecoveryF2p.setToolTipText("Blocks autonomous login recovery on member, PvP, Deadman, seasonal, or minigame/special worlds.");
		loginRecoveryF2p.addActionListener(e -> plugin.setMobFarmerLoginRecoveryF2pOnly(loginRecoveryF2p.isSelected()));
		JCheckBox clickToPlayRecovery = new JCheckBox("Click-to-play recovery", plugin.getMobFarmerLoginClickToPlayEnabled());
		styleCheckbox(clickToPlayRecovery);
		clickToPlayRecovery.setToolTipText("Clicks the visible login widget or presses Enter on the click-to-play screen.");
		clickToPlayRecovery.addActionListener(e -> plugin.setMobFarmerLoginClickToPlayEnabled(clickToPlayRecovery.isSelected()));
		JCheckBox disconnectRecovery = new JCheckBox("Inactivity disconnect recovery", plugin.getMobFarmerLoginDisconnectRecoveryEnabled());
		styleCheckbox(disconnectRecovery);
		disconnectRecovery.setToolTipText("Handles RuneLite CONNECTION_LOST with a guarded Enter press. This is not anti-idle input.");
		disconnectRecovery.addActionListener(e -> plugin.setMobFarmerLoginDisconnectRecoveryEnabled(disconnectRecovery.isSelected()));
		JCheckBox autoResumeAfterLogin = new JCheckBox("Auto-resume after login", plugin.getMobFarmerAutoResumeAfterLogin());
		styleCheckbox(autoResumeAfterLogin);
		autoResumeAfterLogin.setToolTipText("Keep the farmer loop alive through logout/disconnect so it resumes after login.");
		autoResumeAfterLogin.addActionListener(e -> plugin.setMobFarmerAutoResumeAfterLogin(autoResumeAfterLogin.isSelected()));
		JTextField preferredLoginWorld = new JTextField(String.valueOf(plugin.getMobFarmerPreferredLoginWorld()));
		preferredLoginWorld.setToolTipText("Preferred local dev login world for status/reporting; current world is still validated before clicking.");

		JCheckBox lootEnabled = new JCheckBox("Loot pickup", plugin.getMobFarmerLootEnabled());
		styleCheckbox(lootEnabled);
		lootEnabled.addActionListener(e -> plugin.setMobFarmerLootEnabled(lootEnabled.isSelected()));
		JCheckBox lootDuringCombat = new JCheckBox("Loot during combat", plugin.getMobFarmerLootDuringCombat());
		styleCheckbox(lootDuringCombat);
		lootDuringCombat.addActionListener(e -> plugin.setMobFarmerLootDuringCombat(lootDuringCombat.isSelected()));
		JCheckBox attackBeforeLoot = new JCheckBox("Attack before loot", plugin.getMobFarmerAttackBeforeLoot());
		styleCheckbox(attackBeforeLoot);
		attackBeforeLoot.addActionListener(e -> plugin.setMobFarmerAttackBeforeLoot(attackBeforeLoot.isSelected()));
		JTextField lootMinValue = new JTextField(String.valueOf(plugin.getMobFarmerLootMinValueGe()));
		lootMinValue.setToolTipText("Minimum total GE value for items not in the always-loot list.");
		JTextField highPriorityLootValue = new JTextField(String.valueOf(plugin.getMobFarmerHighPriorityLootValueGe()));
		highPriorityLootValue.setToolTipText("Loot at or above this GE value can override attack-before-loot.");
		JTextField urgentLootTicks = new JTextField(String.valueOf(plugin.getMobFarmerLootUrgentDespawnTicks()));
		urgentLootTicks.setToolTipText("Loot with this many ticks left becomes high priority; 0 disables despawn urgency.");
		JTextField cleanupPileCount = new JTextField(String.valueOf(plugin.getMobFarmerLootCleanupPileCount()));
		cleanupPileCount.setToolTipText("If this many selectable loot piles are present, cleanup can override combat; 0 disables pile pressure.");
		JTextField lootRadius = new JTextField(String.valueOf(plugin.getMobFarmerLootRadius()));
		lootRadius.setToolTipText("0 disables the loot radius guard.");
		JTextField lootItems = new JTextField(plugin.getMobFarmerLootItems());
		lootItems.setToolTipText("Items to always loot even below the value threshold.");
		JTextField lootBlacklist = new JTextField(plugin.getMobFarmerLootBlacklist());
		lootBlacklist.setToolTipText("Items to never loot.");
		JTextField lootMinSingleGe = new JTextField(String.valueOf(plugin.getMobFarmerLootMinSingleGe()));
		lootMinSingleGe.setToolTipText("Minimum GE value per individual item before unlisted loot is eligible.");
		JTextField lootMinStackGe = new JTextField(String.valueOf(plugin.getMobFarmerLootMinStackGe()));
		lootMinStackGe.setToolTipText("Minimum total stack GE value before unlisted loot is eligible.");
		JTextField lootMinStackQuantity = new JTextField(String.valueOf(plugin.getMobFarmerLootMinStackQuantity()));
		lootMinStackQuantity.setToolTipText("Minimum quantity for stackable unlisted items.");
		JTextField lootAlwaysStackGe = new JTextField(String.valueOf(plugin.getMobFarmerLootAlwaysStackGe()));
		lootAlwaysStackGe.setToolTipText("Treat stacks at or above this GE value as high-priority loot.");
		JTextField lootNeverStackBelowGe = new JTextField(String.valueOf(plugin.getMobFarmerLootNeverStackBelowGe()));
		lootNeverStackBelowGe.setToolTipText("Reject unlisted stacks below this GE value even if broad rules allow them.");
		JCheckBox highAlchEnabled = new JCheckBox("High Alch policy", plugin.getMobFarmerHighAlchEnabled());
		styleCheckbox(highAlchEnabled);
		highAlchEnabled.setToolTipText("Evaluate safe High Alchemy candidates while farming.");
		highAlchEnabled.addActionListener(e -> plugin.setMobFarmerHighAlchEnabled(highAlchEnabled.isSelected()));
		JTextField highAlchMinHa = new JTextField(String.valueOf(plugin.getMobFarmerHighAlchMinHa()));
		highAlchMinHa.setToolTipText("Minimum single-item HA value for candidate reporting.");
		JTextField highAlchMinDelta = new JTextField(String.valueOf(plugin.getMobFarmerHighAlchMinDelta()));
		highAlchMinDelta.setToolTipText("Require HA value minus GE value to be at least this amount.");
		JTextField highAlchMaxLoss = new JTextField(String.valueOf(plugin.getMobFarmerHighAlchMaxLoss()));
		highAlchMaxLoss.setToolTipText("Maximum acceptable GE-to-HA loss per item when inventory space matters.");
		JTextField highAlchItems = new JTextField(plugin.getMobFarmerHighAlchItems());
		highAlchItems.setToolTipText("If non-empty, only these item names or id:<item id> are eligible for High Alchemy.");
		JTextField highAlchBlacklist = new JTextField(plugin.getMobFarmerHighAlchBlacklist());
		highAlchBlacklist.setToolTipText("Items that must never be high-alched.");
		JComboBox<CvHelperLootOwnershipMode> lootOwnership = new JComboBox<>(CvHelperLootOwnershipMode.values());
		lootOwnership.setSelectedItem(plugin.getMobFarmerLootOwnershipMode());
		lootOwnership.addActionListener(e -> plugin.setMobFarmerLootOwnershipMode((CvHelperLootOwnershipMode) lootOwnership.getSelectedItem()));
		JComboBox<CvHelperMobInteractionMode> lootInteraction = new JComboBox<>(CvHelperMobInteractionMode.values());
		lootInteraction.setSelectedItem(plugin.getMobFarmerLootInteractionMode());
		lootInteraction.addActionListener(e -> plugin.setMobFarmerLootInteractionMode((CvHelperMobInteractionMode) lootInteraction.getSelectedItem()));
		JComboBox<CvHelperGroundItemsMode> groundItemsMode = new JComboBox<>(CvHelperGroundItemsMode.values());
		groundItemsMode.setSelectedItem(plugin.getMobFarmerGroundItemsMode());
		groundItemsMode.addActionListener(e -> plugin.setMobFarmerGroundItemsMode((CvHelperGroundItemsMode) groundItemsMode.getSelectedItem()));
		JCheckBox respectGroundItemsHidden = new JCheckBox("Respect hidden Ground Items", plugin.getMobFarmerRespectGroundItemsHidden());
		styleCheckbox(respectGroundItemsHidden);
		respectGroundItemsHidden.addActionListener(e -> plugin.setMobFarmerRespectGroundItemsHidden(respectGroundItemsHidden.isSelected()));
		JCheckBox intermediateActions = new JCheckBox("Use intermediate actions", plugin.getMobFarmerIntermediateActionsEnabled());
		styleCheckbox(intermediateActions);
		intermediateActions.addActionListener(e -> plugin.setMobFarmerIntermediateActionsEnabled(intermediateActions.isSelected()));
		JTextField intermediateItems = new JTextField(plugin.getMobFarmerIntermediateItems());
		intermediateItems.setToolTipText("Items to use during farming, such as bones or ashes.");
		JTextArea intermediateMappings = new JTextArea(plugin.getMobFarmerIntermediateActionMappings(), 3, 18);
		intermediateMappings.setLineWrap(true);
		intermediateMappings.setWrapStyleWord(true);
		intermediateMappings.setToolTipText("Examples: bones -> Bury; big bones -> Bury; ashes -> Scatter|Bury");
		JScrollPane intermediateMappingsPane = new JScrollPane(intermediateMappings);
		intermediateMappingsPane.setPreferredSize(new Dimension(0, 76));
		JTextField neverDrop = new JTextField(plugin.getMobFarmerNeverDropItems());
		neverDrop.setToolTipText("Inventory items that future drop processing must never drop.");

		JButton saveGuards = new JButton("Save farmer guards");
		saveGuards.addActionListener(e ->
		{
			plugin.setMobFarmerTarget(target.getText());
			plugin.setMobFarmerEngagedMode((CvHelperMobEngagedMode) engagedMode.getSelectedItem());
			plugin.setMobFarmerAggroResponse((CvHelperMobAggroResponse) aggroResponse.getSelectedItem());
			plugin.setMobFarmerRequireLineOfSight(requireLineOfSight.isSelected());
			plugin.setMobFarmerMaxDistance(parseNonNegativeInt(maxDistance.getText(), plugin.getMobFarmerMaxDistance()));
			plugin.setMobFarmerAttackInteractionMode((CvHelperMobInteractionMode) attackInteraction.getSelectedItem());
			plugin.setMobFarmerAutoEatEnabled(autoEatEnabled.isSelected());
			plugin.setMobFarmerEatHitpointPercent(parseNonNegativeInt(eatThreshold.getText(), plugin.getMobFarmerEatHitpointPercent()));
			plugin.setMobFarmerFoodItems(foodItems.getText());
			plugin.setMobFarmerStopIfNoFood(stopIfNoFood.isSelected());
			plugin.setMobFarmerSurvivalPreemptsActions(survivalPreempts.isSelected());
			plugin.setMobFarmerLoginRecoveryEnabled(loginRecovery.isSelected());
			plugin.setMobFarmerLoginRecoveryF2pOnly(loginRecoveryF2p.isSelected());
			plugin.setMobFarmerLoginClickToPlayEnabled(clickToPlayRecovery.isSelected());
			plugin.setMobFarmerLoginDisconnectRecoveryEnabled(disconnectRecovery.isSelected());
			plugin.setMobFarmerAutoResumeAfterLogin(autoResumeAfterLogin.isSelected());
			plugin.setMobFarmerPreferredLoginWorld(parseNonNegativeInt(preferredLoginWorld.getText(), plugin.getMobFarmerPreferredLoginWorld()));
			plugin.setMobFarmerLootEnabled(lootEnabled.isSelected());
			plugin.setMobFarmerLootDuringCombat(lootDuringCombat.isSelected());
			plugin.setMobFarmerAttackBeforeLoot(attackBeforeLoot.isSelected());
			plugin.setMobFarmerLootMinValueGe(parseNonNegativeInt(lootMinValue.getText(), plugin.getMobFarmerLootMinValueGe()));
			plugin.setMobFarmerHighPriorityLootValueGe(parseNonNegativeInt(highPriorityLootValue.getText(), plugin.getMobFarmerHighPriorityLootValueGe()));
			plugin.setMobFarmerLootUrgentDespawnTicks(parseNonNegativeInt(urgentLootTicks.getText(), plugin.getMobFarmerLootUrgentDespawnTicks()));
			plugin.setMobFarmerLootCleanupPileCount(parseNonNegativeInt(cleanupPileCount.getText(), plugin.getMobFarmerLootCleanupPileCount()));
			plugin.setMobFarmerLootRadius(parseNonNegativeInt(lootRadius.getText(), plugin.getMobFarmerLootRadius()));
			plugin.setMobFarmerLootItems(lootItems.getText());
			plugin.setMobFarmerLootBlacklist(lootBlacklist.getText());
			plugin.setMobFarmerLootMinSingleGe(parseNonNegativeInt(lootMinSingleGe.getText(), plugin.getMobFarmerLootMinSingleGe()));
			plugin.setMobFarmerLootMinStackGe(parseNonNegativeInt(lootMinStackGe.getText(), plugin.getMobFarmerLootMinStackGe()));
			plugin.setMobFarmerLootMinStackQuantity(parseNonNegativeInt(lootMinStackQuantity.getText(), plugin.getMobFarmerLootMinStackQuantity()));
			plugin.setMobFarmerLootAlwaysStackGe(parseNonNegativeInt(lootAlwaysStackGe.getText(), plugin.getMobFarmerLootAlwaysStackGe()));
			plugin.setMobFarmerLootNeverStackBelowGe(parseNonNegativeInt(lootNeverStackBelowGe.getText(), plugin.getMobFarmerLootNeverStackBelowGe()));
			plugin.setMobFarmerHighAlchEnabled(highAlchEnabled.isSelected());
			plugin.setMobFarmerHighAlchMinHa(parseNonNegativeInt(highAlchMinHa.getText(), plugin.getMobFarmerHighAlchMinHa()));
			plugin.setMobFarmerHighAlchMinDelta(parseNonNegativeInt(highAlchMinDelta.getText(), plugin.getMobFarmerHighAlchMinDelta()));
			plugin.setMobFarmerHighAlchMaxLoss(parseNonNegativeInt(highAlchMaxLoss.getText(), plugin.getMobFarmerHighAlchMaxLoss()));
			plugin.setMobFarmerHighAlchItems(highAlchItems.getText());
			plugin.setMobFarmerHighAlchBlacklist(highAlchBlacklist.getText());
			plugin.setMobFarmerLootOwnershipMode((CvHelperLootOwnershipMode) lootOwnership.getSelectedItem());
			plugin.setMobFarmerLootInteractionMode((CvHelperMobInteractionMode) lootInteraction.getSelectedItem());
			plugin.setMobFarmerGroundItemsMode((CvHelperGroundItemsMode) groundItemsMode.getSelectedItem());
			plugin.setMobFarmerRespectGroundItemsHidden(respectGroundItemsHidden.isSelected());
			plugin.setMobFarmerIntermediateActionsEnabled(intermediateActions.isSelected());
			plugin.setMobFarmerIntermediateItems(intermediateItems.getText());
			plugin.setMobFarmerIntermediateActionMappings(intermediateMappings.getText());
			plugin.setMobFarmerNeverDropItems(neverDrop.getText());
			updateStatus("Mob farmer guards saved");
		});
		JButton saveTarget = new JButton("Save mob target");
		saveTarget.addActionListener(e -> plugin.setMobFarmerTarget(target.getText()));
		JButton dryStep = new JButton("Dry step");
		dryStep.addActionListener(e ->
		{
			plugin.setMobFarmerTarget(target.getText());
			plugin.runMobFarmerStep(false);
		});
		JButton liveStep = new JButton("Live attack step");
		liveStep.addActionListener(e ->
		{
			plugin.setMobFarmerTarget(target.getText());
			plugin.runMobFarmerStep(true);
		});
		JButton startDry = new JButton("Start dry loop");
		startDry.addActionListener(e ->
		{
			plugin.setMobFarmerTarget(target.getText());
			plugin.startMobFarmer(false);
		});
		JButton startLive = new JButton("Start live loop");
		startLive.addActionListener(e ->
		{
			plugin.setMobFarmerTarget(target.getText());
			plugin.startMobFarmer(true);
		});
		JButton stop = new JButton("Stop loop");
		stop.addActionListener(e -> plugin.stopMobFarmer());

		JLabel help = new JLabel("<html>Farmer loop: survival guard, optional intermediate inventory actions, loot processing, then guarded attack selection. Use mob targets like goblin|spider or id:1234. Loot failures are diagnosed in /automation/mob-farmer/status.</html>");
		help.setForeground(Color.LIGHT_GRAY);

		for (JComponent component : new JComponent[]{
			help,
			label("Mob target"),
			target,
			label("Already-engaged mobs"),
			engagedMode,
			label("Undesired attacker"),
			aggroResponse,
			requireLineOfSight,
			label("Max target distance"),
			maxDistance,
			label("Attack interaction"),
			attackInteraction,
			label("Survival"),
			autoEatEnabled,
			label("Eat below HP %"),
			eatThreshold,
			label("Food items"),
			foodItems,
			stopIfNoFood,
			survivalPreempts,
			label("Login recovery"),
			loginRecovery,
			loginRecoveryF2p,
			clickToPlayRecovery,
			disconnectRecovery,
			autoResumeAfterLogin,
			label("Preferred login world"),
			preferredLoginWorld,
			label("<html>Idle handling: use RuneLite's Logout Timer plugin/settings for longer idle windows. CV Helper only recovers after logout.</html>"),
			label("Loot"),
			lootEnabled,
			lootDuringCombat,
			attackBeforeLoot,
			label("Loot min GE value"),
			lootMinValue,
			label("Loot per-item GE"),
			lootMinSingleGe,
			label("Loot stack GE"),
			lootMinStackGe,
			label("Loot stack qty"),
			lootMinStackQuantity,
			label("Always loot stack GE"),
			lootAlwaysStackGe,
			label("Never loot below GE"),
			lootNeverStackBelowGe,
			label("High-priority GE value"),
			highPriorityLootValue,
			label("Urgent loot ticks"),
			urgentLootTicks,
			label("Cleanup pile count"),
			cleanupPileCount,
			label("Loot radius"),
			lootRadius,
			label("Always-loot items"),
			lootItems,
			label("Never-loot items"),
			lootBlacklist,
			label("Loot ownership"),
			lootOwnership,
			label("Loot interaction"),
			lootInteraction,
			label("Ground Items lists"),
			groundItemsMode,
			respectGroundItemsHidden,
			label("High Alchemy"),
			highAlchEnabled,
			label("Min HA value"),
			highAlchMinHa,
			label("Min HA delta"),
			highAlchMinDelta,
			label("Max HA loss"),
			highAlchMaxLoss,
			label("Alch allowlist"),
			highAlchItems,
			label("Never alch"),
			highAlchBlacklist,
			label("Intermediate actions"),
			intermediateActions,
			label("Intermediate items"),
			intermediateItems,
			label("Item -> action mappings"),
			intermediateMappingsPane,
			label("Protected inventory"),
			neverDrop,
			saveGuards,
			saveTarget,
			dryStep,
			liveStep,
			startDry,
			startLive,
			stop
		})
		{
			stretch(component);
			body.add(component);
		}

		JToggleButton expand = new JToggleButton("Expand");
		body.setVisible(false);
		expand.setSelected(true);
		expand.addActionListener(e ->
		{
			boolean isCollapsed = expand.isSelected();
			expand.setText(isCollapsed ? "Expand" : "Collapse");
			body.setVisible(!isCollapsed);
			section.revalidate();
		});
		expand.setBackground(ColorScheme.DARK_GRAY_COLOR);
		expand.setForeground(Color.LIGHT_GRAY);

		section.add(expand, BorderLayout.NORTH);
		section.add(body, BorderLayout.CENTER);
		setCompact(section);
		return section;
	}

	private int parseNonNegativeInt(String value, int fallback)
	{
		try
		{
			return Math.max(0, Integer.parseInt(value.trim()));
		}
		catch (RuntimeException e)
		{
			return fallback;
		}
	}

	private void populateTargetChoices(JComboBox<String> target, CvHelperActionSurface surface, String selected)
	{
		populateTargetChoices(target, surface, selected, false);
	}

	private void populateTargetChoices(JComboBox<String> target, CvHelperActionSurface surface, String selected, boolean filter)
	{
		target.removeAllItems();
		if (selected != null && !selected.trim().isEmpty())
		{
			target.addItem(selected);
		}
		String needle = normalize(selected);
		for (String label : plugin.getSuggestedActionTargets(surface))
		{
			if ((selected == null || !label.equals(selected)) && (!filter || needle.isEmpty() || normalize(label).contains(needle)))
			{
				target.addItem(label);
			}
		}
		target.setSelectedItem(selected == null ? "" : selected);
	}

	private String normalize(String value)
	{
		return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	private String selectedTarget(JComboBox<String> target)
	{
		Object selected = target.getEditor().getItem();
		return selected == null ? "" : String.valueOf(selected).trim();
	}

	private JLabel label(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.LIGHT_GRAY);
		label.setBorder(new EmptyBorder(4, 0, 1, 0));
		return label;
	}

	private void styleCheckbox(JCheckBox checkbox)
	{
		checkbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		checkbox.setForeground(Color.LIGHT_GRAY);
		checkbox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	}

	private void setCompact(JComponent component)
	{
		component.setAlignmentX(LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
	}

	private void stretch(JComponent component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		Dimension preferred = component.getPreferredSize();
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(24, preferred.height)));
		component.setPreferredSize(new Dimension(Math.max(160, preferred.width), Math.max(24, preferred.height)));
	}

	void updateStatus(String message)
	{
		status.setText(message);
	}

	void updateServerStatus(String message)
	{
		serverStatus.setText(message);
	}

	void refreshStatus()
	{
		updateServerStatus(plugin.getServerStatusText());
		updateStatus("Refreshed at " + LocalDateTime.now().format(TIME_FORMAT));
	}

	private static final class KeyCaptureButton extends JButton
	{
		private Keybind value;
		private final java.util.function.Consumer<Keybind> onChange;

		private KeyCaptureButton(Keybind value, java.util.function.Consumer<Keybind> onChange)
		{
			this.onChange = onChange;
			setFocusTraversalKeysEnabled(false);
			setFont(FontManager.getDefaultFont().deriveFont(12.f));
			setValue(value);
			addActionListener(e ->
			{
				setValue(Keybind.NOT_SET);
				onChange.accept(KeyCaptureButton.this.value);
			});
			addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					setValue(new Keybind(e));
					onChange.accept(KeyCaptureButton.this.value);
				}
			});
		}

		private void setValue(Keybind value)
		{
			this.value = value == null ? Keybind.NOT_SET : value;
			setText(this.value.toString());
		}
	}
}
