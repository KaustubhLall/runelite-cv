/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

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

class CvHelperModPanel extends PluginPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final CvHelperModPlugin plugin;
	private final JLabel status = new JLabel("Ready");
	private final JLabel serverStatus = new JLabel("Server: starting");
	private final JLabel loginRecoveryStatus = new JLabel("Login: unknown");

	CvHelperModPanel(CvHelperModPlugin plugin)
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
		JPanel woodcuttingSection = createWoodcuttingSection();
		JPanel miningSection = createMiningSection();

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
		lower.add(woodcuttingSection);
		lower.add(miningSection);
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
		JTextField targetBlacklist = new JTextField(plugin.getMobFarmerTargetBlacklist());
		targetBlacklist.setToolTipText("NPCs to never attack even if they match the target (wins over the target). Same format, e.g. deadly red spider|id:1234");

		JLabel statusLabel = new JLabel("Status: " + (plugin.getMobFarmerRunning() ? "Running" : "Stopped"));
		statusLabel.setForeground(Color.LIGHT_GRAY);

		Runnable saveQuick = () ->
		{
			plugin.setMobFarmerTarget(target.getText());
			plugin.setMobFarmerTargetBlacklist(targetBlacklist.getText());
		};

		JButton dryStep = new JButton("Dry step");
		dryStep.addActionListener(e ->
		{
			saveQuick.run();
			plugin.runMobFarmerStep(false);
		});
		JButton liveStep = new JButton("Live attack step");
		liveStep.addActionListener(e ->
		{
			saveQuick.run();
			plugin.runMobFarmerStep(true);
		});
		JButton startDry = new JButton("Start dry loop");
		startDry.addActionListener(e ->
		{
			saveQuick.run();
			plugin.startMobFarmer(false);
		});
		JButton startLive = new JButton("Start live loop");
		startLive.addActionListener(e ->
		{
			saveQuick.run();
			plugin.startMobFarmer(true);
		});
		JButton stop = new JButton("Stop loop");
		stop.addActionListener(e -> plugin.stopMobFarmer());

		JButton refreshStatus = new JButton("Refresh status");
		refreshStatus.addActionListener(e ->
		{
			statusLabel.setText("Status: " + (plugin.getMobFarmerRunning() ? "Running" : "Stopped"));
			updateStatus("Mob farmer status refreshed");
		});

		JLabel help = new JLabel("<html>Mob farmer: guarded attack selection, loot, bury and survival guards. Detailed config (combat cadence, engaged/aggro modes, loot &amp; drop policy, high alch, login recovery, intermediate actions) lives in RuneLite settings &rarr; CV Helper (Modular), or the WebHelper console.</html>");
		help.setForeground(Color.LIGHT_GRAY);

		for (JComponent component : new JComponent[]{
			help,
			label("Mob target"),
			target,
			label("Never-attack mobs"),
			targetBlacklist,
			statusLabel,
			dryStep,
			liveStep,
			startDry,
			startLive,
			stop,
			refreshStatus
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

	private JPanel createWoodcuttingSection()
	{
		JPanel section = new JPanel(new BorderLayout(0, 4));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.setBorder(BorderFactory.createTitledBorder("Woodcutting farmer"));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField target = new JTextField(plugin.getWoodcuttingFarmerTarget());
		target.setToolTipText("Partial name or id:<object id>, for example oak|tree|willow|maple");

		JLabel statusLabel = new JLabel("Status: " + (plugin.getWoodcuttingFarmerRunning() ? "Running" : "Stopped"));
		statusLabel.setForeground(Color.LIGHT_GRAY);

		JButton dryStep = new JButton("Dry step");
		dryStep.addActionListener(e ->
		{
			plugin.setWoodcuttingFarmerTarget(target.getText());
			plugin.runWoodcuttingFarmerStep(false);
		});
		JButton liveStep = new JButton("Live chop step");
		liveStep.addActionListener(e ->
		{
			plugin.setWoodcuttingFarmerTarget(target.getText());
			plugin.runWoodcuttingFarmerStep(true);
		});
		JButton startDry = new JButton("Start dry loop");
		startDry.addActionListener(e ->
		{
			plugin.setWoodcuttingFarmerTarget(target.getText());
			plugin.startWoodcuttingFarmer(false);
		});
		JButton startLive = new JButton("Start live loop");
		startLive.addActionListener(e ->
		{
			plugin.setWoodcuttingFarmerTarget(target.getText());
			plugin.startWoodcuttingFarmer(true);
		});
		JButton stop = new JButton("Stop loop");
		stop.addActionListener(e -> plugin.stopWoodcuttingFarmer());

		JButton refreshStatus = new JButton("Refresh status");
		refreshStatus.addActionListener(e ->
		{
			statusLabel.setText("Status: " + (plugin.getWoodcuttingFarmerRunning() ? "Running" : "Stopped"));
			updateStatus("Woodcutting status refreshed");
		});

		JLabel help = new JLabel("<html>Woodcutting farmer: selects nearest reachable tree, chops until inventory full or tree depleted. Drop policy is enabled by default - configure in RuneLite plugin config (woodcutter section) or WebHelper.</html>");
		help.setForeground(Color.LIGHT_GRAY);

		for (JComponent component : new JComponent[]{
			help,
			label("Target trees"),
			target,
			statusLabel,
			dryStep,
			liveStep,
			startDry,
			startLive,
			stop,
			refreshStatus
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

	private JPanel createMiningSection()
	{
		JPanel section = new JPanel(new BorderLayout(0, 4));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.setBorder(BorderFactory.createTitledBorder("Mining farmer"));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTextField target = new JTextField(plugin.getMiningFarmerTarget());
		target.setToolTipText("Partial name or id:<object id>, for example iron rocks|iron ore rocks|rocks");

		JLabel statusLabel = new JLabel("Status: " + (plugin.getMiningFarmerRunning() ? "Running" : "Stopped"));
		statusLabel.setForeground(Color.LIGHT_GRAY);

		JButton dryStep = new JButton("Dry step");
		dryStep.addActionListener(e ->
		{
			plugin.setMiningFarmerTarget(target.getText());
			plugin.runMiningFarmerStep(false);
		});
		JButton liveStep = new JButton("Live mine step");
		liveStep.addActionListener(e ->
		{
			plugin.setMiningFarmerTarget(target.getText());
			plugin.runMiningFarmerStep(true);
		});
		JButton startDry = new JButton("Start dry loop");
		startDry.addActionListener(e ->
		{
			plugin.setMiningFarmerTarget(target.getText());
			plugin.startMiningFarmer(false);
		});
		JButton startLive = new JButton("Start live loop");
		startLive.addActionListener(e ->
		{
			plugin.setMiningFarmerTarget(target.getText());
			plugin.startMiningFarmer(true);
		});
		JButton stop = new JButton("Stop loop");
		stop.addActionListener(e -> plugin.stopMiningFarmer());

		JButton refreshStatus = new JButton("Refresh status");
		refreshStatus.addActionListener(e ->
		{
			statusLabel.setText("Status: " + (plugin.getMiningFarmerRunning() ? "Running" : "Stopped"));
			updateStatus("Mining status refreshed");
		});

		JLabel help = new JLabel("<html>Mining farmer: selects nearest reachable rock, mines until inventory full or rock depleted. Drop policy is enabled by default - configure in RuneLite plugin config (woodcutter section) or WebHelper.</html>");
		help.setForeground(Color.LIGHT_GRAY);

		for (JComponent component : new JComponent[]{
			help,
			label("Target rocks"),
			target,
			statusLabel,
			dryStep,
			liveStep,
			startDry,
			startLive,
			stop,
			refreshStatus
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
