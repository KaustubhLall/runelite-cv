/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class CvHelperPanel extends PluginPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final CvHelperPlugin plugin;
	private final JLabel status = new JLabel("Ready");
	private final JLabel serverStatus = new JLabel("Server: starting");

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

		JButton prayerButton = new JButton("Prayer targets");
		prayerButton.addActionListener(e -> plugin.refreshPrayerTargets());

		JButton debugButton = new JButton("Debug overlay");
		debugButton.addActionListener(e -> plugin.debugOverlayState());

		JButton printBoundsButton = new JButton("Print overlay bounds");
		printBoundsButton.addActionListener(e -> plugin.printOverlayCoordinates());

		buttons.add(captureButton);
		buttons.add(screenButton);
		buttons.add(minimapButton);
		buttons.add(refreshButton);
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
		targetLabels.addActionListener(e -> plugin.setShowTargetLabels(targetLabels.isSelected()));
		localExport.addActionListener(e -> plugin.setLocalExportEnabled(localExport.isSelected()));

		for (JCheckBox checkbox : new JCheckBox[]{hoverOverlay, widgetInfo, prayerTargets, spellTargets, minimapTargets, inventoryTargets, equipmentTargets, panelTargets, combatTargets, entityTargets, targetLabels, localExport})
		{
			styleCheckbox(checkbox);
			toggles.add(checkbox);
		}

		JPanel prayerSection = createNestedSection("Prayer toggles", plugin.getPrayerNames(), true, true);
		JPanel spellSection = createNestedSection("Spellbook toggles", plugin.getSpellbookNames(), false, true);

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

		JPanel center = new JPanel(new BorderLayout(0, 4));
		center.setBackground(ColorScheme.DARK_GRAY_COLOR);
		center.add(toggles, BorderLayout.NORTH);
		JPanel lower = new JPanel();
		lower.setLayout(new BoxLayout(lower, BoxLayout.Y_AXIS));
		lower.setBackground(ColorScheme.DARK_GRAY_COLOR);
		lower.add(prayerSection);
		lower.add(spellSection);
		lower.add(serverSettings);
		lower.add(playerPanel);
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

	private void styleCheckbox(JCheckBox checkbox)
	{
		checkbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		checkbox.setForeground(Color.LIGHT_GRAY);
		checkbox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	}

	private void setCompact(JComponent component)
	{
		component.setAlignmentX(LEFT_ALIGNMENT);
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
}
