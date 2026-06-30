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

	// Quest-Helper-style navigation: a swappable body, a back stack of screen renderers, and the
	// metadata-driven page list from SettingsCatalog.
	private final java.util.List<SettingsPage> pages;
	private final JPanel body = new JPanel(new BorderLayout());
	private final java.util.Deque<Runnable> history = new java.util.ArrayDeque<>();
	private Runnable currentRender;

	CvHelperModPanel(CvHelperModPlugin plugin)
	{
		// wrap=false: we manage our own scroll around the body so the header/footer stay fixed
		// (and to avoid a scroll-pane nested inside PluginPanel's default scroll wrapper).
		super(false);
		this.plugin = plugin;
		this.pages = buildPages();

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(buildHeader(), BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
		add(buildFooter(), BorderLayout.SOUTH);

		navigate(this::renderHome);
		refreshStatus();
	}

	private java.util.List<SettingsPage> buildPages()
	{
		java.util.List<SettingsPage> list = new java.util.ArrayList<>(new SettingsCatalog(plugin, this).build());
		list.add(buildDebugToolsPage());
		return list;
	}

	private JComponent buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(0, 0, 6, 0));

		JLabel title = new JLabel("CV Helper");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		header.add(title, BorderLayout.WEST);

		JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		btns.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton home = new JButton("Home");
		home.setToolTipText("Back to the home screen");
		home.addActionListener(e ->
		{
			history.clear();
			currentRender = null;
			navigate(this::renderHome);
		});

		JButton copyPort = new JButton("Copy port");
		copyPort.setToolTipText("Copy this client's HTTP server port to the clipboard.");
		copyPort.addActionListener(e -> copyPort());

		JButton panic = new JButton("PANIC");
		panic.setForeground(Color.RED);
		panic.setToolTipText("Stop all automation immediately.");
		panic.addActionListener(e -> plugin.panicStop());

		btns.add(home);
		btns.add(copyPort);
		btns.add(panic);
		header.add(btns, BorderLayout.EAST);
		return header;
	}

	private JComponent buildFooter()
	{
		serverStatus.setForeground(Color.LIGHT_GRAY);
		serverStatus.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(Color.LIGHT_GRAY);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setToolTipText("Latest plugin status");

		JPanel footer = new JPanel(new GridLayout(0, 1, 0, 2));
		footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		footer.setBorder(new EmptyBorder(6, 0, 0, 0));
		footer.add(serverStatus);
		footer.add(status);
		return footer;
	}

	void copyPort()
	{
		int port = plugin.getLocalPort();
		String text = port > 0 ? String.valueOf(port) : "";
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
		updateStatus(port > 0 ? "Copied port " + port : "Server port not available yet");
	}

	// ---------------------------------------------------------------- navigation

	private void navigate(Runnable render)
	{
		if (currentRender != null)
		{
			history.push(currentRender);
		}
		currentRender = render;
		render.run();
	}

	private void back()
	{
		if (!history.isEmpty())
		{
			currentRender = history.pop();
			currentRender.run();
		}
	}

	/** Re-run the current screen renderer (e.g. after a value changes elsewhere). */
	void refreshCurrentPage()
	{
		if (currentRender != null)
		{
			currentRender.run();
		}
	}

	private void setBody(JComponent view)
	{
		body.removeAll();
		JPanel holder = new JPanel(new BorderLayout());
		holder.setBackground(ColorScheme.DARK_GRAY_COLOR);
		holder.add(view, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(holder,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.add(scroll, BorderLayout.CENTER);
		body.revalidate();
		body.repaint();
	}

	// ---------------------------------------------------------------- home screen

	private void renderHome()
	{
		JPanel home = vbox();

		JTextField search = new JTextField();
		search.setToolTipText("Search every setting, e.g. loot, prayer, alch, hotkey");
		stretch(search);
		home.add(labeledRow("Search settings", search, null));

		stretchAdd(home, createQuickStartSection());

		JPanel blocksHost = vbox();
		home.add(blocksHost);

		Runnable rebuild = () ->
		{
			blocksHost.removeAll();
			String q = search.getText().trim().toLowerCase();
			for (String category : SettingsPage.CATEGORIES)
			{
				java.util.List<SettingsPage> inCategory = new java.util.ArrayList<>();
				for (SettingsPage page : pages)
				{
					if (category.equals(page.category) && (q.isEmpty() || page.searchText().contains(q)))
					{
						inCategory.add(page);
					}
				}
				if (inCategory.isEmpty())
				{
					continue;
				}
				blocksHost.add(categoryHeader(category));
				for (SettingsPage page : inCategory)
				{
					blocksHost.add(buildBlock(page));
				}
			}
			blocksHost.revalidate();
			blocksHost.repaint();
		};
		search.getDocument().addDocumentListener(simpleDocListener(rebuild));
		rebuild.run();

		setBody(home);
	}

	private JComponent buildBlock(SettingsPage page)
	{
		JPanel block = new JPanel(new BorderLayout(6, 0));
		block.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		block.setBorder(new EmptyBorder(6, 8, 6, 8));

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		JLabel titleLabel = new JLabel(page.title);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		text.add(titleLabel);
		if (page.summary != null && !page.summary.isEmpty())
		{
			JLabel summaryLabel = new JLabel(page.summary);
			summaryLabel.setForeground(Color.LIGHT_GRAY);
			summaryLabel.setFont(FontManager.getRunescapeSmallFont());
			text.add(summaryLabel);
		}
		block.add(text, BorderLayout.CENTER);

		JLabel chevron = new JLabel("›");
		chevron.setForeground(Color.LIGHT_GRAY);
		block.add(chevron, BorderLayout.EAST);

		block.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		block.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mousePressed(java.awt.event.MouseEvent e)
			{
				navigate(() -> renderPage(page));
			}

			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				block.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				block.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
		stretch(block);
		return block;
	}

	private JComponent categoryHeader(String name)
	{
		JLabel label = new JLabel(name.toUpperCase());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setBorder(new EmptyBorder(8, 2, 2, 2));
		stretch(label);
		return label;
	}

	// ---------------------------------------------------------------- detail page

	private void renderPage(SettingsPage page)
	{
		JPanel view = vbox();
		view.add(backBar(page.title));
		if (page.help != null && !page.help.isEmpty())
		{
			view.add(helpLabel(page.help));
		}

		if (page.customBody != null)
		{
			JComponent custom = page.customBody.get();
			stretchAdd(view, custom);
		}
		else
		{
			String currentGroup = null;
			for (SettingField field : page.fields)
			{
				if (field.group != null && !field.group.equals(currentGroup))
				{
					currentGroup = field.group;
					view.add(groupHeader(currentGroup));
				}
				view.add(renderField(field));
			}
		}
		setBody(view);
	}

	private JComponent backBar(String title)
	{
		JPanel bar = new JPanel(new BorderLayout(6, 0));
		bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bar.setBorder(new EmptyBorder(0, 0, 6, 0));
		JButton back = new JButton("‹ Back");
		back.addActionListener(e -> back());
		bar.add(back, BorderLayout.WEST);
		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		bar.add(titleLabel, BorderLayout.CENTER);
		stretch(bar);
		return bar;
	}

	private JComponent groupHeader(String name)
	{
		JLabel label = new JLabel(name);
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setBorder(new EmptyBorder(10, 2, 2, 2));
		stretch(label);
		return label;
	}

	private JComponent helpLabel(String text)
	{
		JLabel label = new JLabel("<html><div style='width:210px'>" + escapeHtml(text) + "</div></html>");
		label.setForeground(Color.LIGHT_GRAY);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setBorder(new EmptyBorder(0, 2, 4, 2));
		stretch(label);
		return label;
	}

	// ---------------------------------------------------------------- field rendering

	private JComponent renderField(SettingField field)
	{
		switch (field.kind)
		{
			case TOGGLE:
				return renderToggle(field);
			case TEXT:
				return renderText(field);
			case INT:
				return renderInt(field);
			case CHOICE:
				return renderChoice(field);
			case LIST:
				return renderList(field);
			case HOTKEY:
				return renderHotkey(field);
			case ACTION:
				return renderAction(field);
			case INFO:
				return helpLabel(field.help != null ? field.help : field.label);
			case CUSTOM:
			{
				JComponent component = field.custom.get();
				JPanel wrap = vbox();
				stretchAdd(wrap, component);
				return wrap;
			}
			case PAGE_LINK:
				return buildBlock(field.linkTarget);
			default:
				return new JPanel();
		}
	}

	private JComponent renderToggle(SettingField field)
	{
		JCheckBox checkbox = new JCheckBox(field.label, field.getBool.getAsBoolean());
		styleCheckbox(checkbox);
		checkbox.addActionListener(e -> field.setBool.accept(checkbox.isSelected()));
		return fieldRow(checkbox, field.help);
	}

	private JComponent renderText(SettingField field)
	{
		JTextField input = new JTextField(field.getText.get());
		commitOnChange(input, () -> field.setText.accept(input.getText().trim()));
		return labeledRow(field.label, input, field.help);
	}

	private JComponent renderInt(SettingField field)
	{
		JTextField input = new JTextField(String.valueOf(field.getInt.getAsInt()));
		commitOnChange(input, () ->
		{
			int value = parseIntOr(input.getText().trim(), field.getInt.getAsInt());
			if (field.bounded)
			{
				value = Math.max(field.min, Math.min(field.max, value));
			}
			field.setInt.accept(value);
			input.setText(String.valueOf(value));
		});
		StringBuilder label = new StringBuilder(field.label);
		if (field.units != null && !field.units.isEmpty())
		{
			label.append(" (").append(field.units).append(')');
		}
		if (field.bounded)
		{
			label.append("  [").append(field.min).append('–').append(field.max).append(']');
		}
		return labeledRow(label.toString(), input, field.help);
	}

	private JComponent renderChoice(SettingField field)
	{
		String current = field.getChoice.get();
		if (field.choices != null && field.choices.size() <= 3)
		{
			JPanel segment = new JPanel(new GridLayout(1, field.choices.size(), 2, 0));
			segment.setBackground(ColorScheme.DARK_GRAY_COLOR);
			javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
			for (SettingField.Choice choice : field.choices)
			{
				JToggleButton button = new JToggleButton(choice.label);
				button.setSelected(choice.value.equals(current));
				button.addActionListener(e -> field.setChoice.accept(choice.value));
				group.add(button);
				segment.add(button);
			}
			return labeledRow(field.label, segment, field.help);
		}

		String currentLabel = current;
		if (field.choices != null)
		{
			for (SettingField.Choice choice : field.choices)
			{
				if (choice.value.equals(current))
				{
					currentLabel = choice.label;
				}
			}
		}
		JButton row = new JButton(field.label + ":  " + currentLabel + "    ›");
		row.setHorizontalAlignment(SwingConstants.LEFT);
		row.addActionListener(e -> navigate(() -> renderChoiceSelect(field)));
		return fieldRow(row, field.help);
	}

	private void renderChoiceSelect(SettingField field)
	{
		JPanel view = vbox();
		view.add(backBar(field.label));
		if (field.help != null && !field.help.isEmpty())
		{
			view.add(helpLabel(field.help));
		}
		String current = field.getChoice.get();
		for (SettingField.Choice choice : field.choices)
		{
			view.add(optionRow(choice.label, choice.help, choice.value.equals(current), () ->
			{
				field.setChoice.accept(choice.value);
				back();
			}));
		}
		setBody(view);
	}

	private JComponent optionRow(String label, String help, boolean selected, Runnable onPick)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(selected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(6, 8, 6, 8));

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		JLabel title = new JLabel((selected ? "✓ " : "") + label);
		title.setForeground(Color.WHITE);
		text.add(title);
		if (help != null && !help.isEmpty())
		{
			JLabel helpLabel = new JLabel("<html><div style='width:190px'>" + escapeHtml(help) + "</div></html>");
			helpLabel.setForeground(Color.LIGHT_GRAY);
			helpLabel.setFont(FontManager.getRunescapeSmallFont());
			text.add(helpLabel);
		}
		row.add(text, BorderLayout.CENTER);
		row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		row.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mousePressed(java.awt.event.MouseEvent e)
			{
				onPick.run();
			}
		});
		stretch(row);
		return row;
	}

	private JComponent renderList(SettingField field)
	{
		ListEditorField editor = new ListEditorField(field.getText.get());
		editor.setOnChange(() -> field.setText.accept(editor.getValue()));
		return labeledRow(field.label, editor, field.help);
	}

	private JComponent renderHotkey(SettingField field)
	{
		KeyCaptureButton button = new KeyCaptureButton(field.getKey.get(), field.setKey);
		return labeledRow(field.label, button, field.help);
	}

	private JComponent renderAction(SettingField field)
	{
		JButton button = new JButton(field.label);
		if (field.help != null && !field.help.isEmpty())
		{
			button.setToolTipText(field.help);
		}
		button.addActionListener(e -> field.action.run());
		return fieldRow(button, null);
	}

	// ---------------------------------------------------------------- field layout helpers

	private JComponent fieldRow(JComponent control, String help)
	{
		JPanel row = vbox();
		row.setBorder(new EmptyBorder(2, 0, 4, 0));
		stretchAdd(row, control);
		if (help != null && !help.isEmpty())
		{
			row.add(helpLabel(help));
		}
		stretch(row);
		return row;
	}

	private JComponent labeledRow(String labelText, JComponent control, String help)
	{
		JPanel row = vbox();
		row.setBorder(new EmptyBorder(2, 0, 4, 0));
		if (labelText != null && !labelText.isEmpty())
		{
			row.add(label(labelText));
		}
		stretchAdd(row, control);
		if (help != null && !help.isEmpty())
		{
			row.add(helpLabel(help));
		}
		stretch(row);
		return row;
	}

	private JPanel vbox()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return panel;
	}

	private void stretchAdd(JPanel parent, JComponent component)
	{
		stretch(component);
		parent.add(component);
	}

	private void commitOnChange(JTextField input, Runnable commit)
	{
		input.addActionListener(e -> commit.run());
		input.addFocusListener(new java.awt.event.FocusAdapter()
		{
			@Override
			public void focusLost(java.awt.event.FocusEvent e)
			{
				commit.run();
			}
		});
	}

	private int parseIntOr(String value, int fallback)
	{
		try
		{
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}

	private static String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private javax.swing.event.DocumentListener simpleDocListener(Runnable onChange)
	{
		return new javax.swing.event.DocumentListener()
		{
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e)
			{
				onChange.run();
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e)
			{
				onChange.run();
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e)
			{
				onChange.run();
			}
		};
	}

	private SettingsPage buildDebugToolsPage()
	{
		SettingsPage page = new SettingsPage("tools", "Status & tools", "Captures, debug, player/login status",
			"Manual screen captures, overlay debug helpers and player/login status readouts.", SettingsPage.CAT_SYSTEM);
		page.body(() ->
		{
			JPanel box = vbox();

			JLabel playerStatus = new JLabel("Player export: pending");
			playerStatus.setForeground(Color.LIGHT_GRAY);
			JButton refreshPlayer = new JButton("Refresh player export");
			refreshPlayer.addActionListener(e ->
			{
				playerStatus.setText(plugin.getPlayerStatus().toString());
				updateStatus("Player export refreshed");
			});
			loginRecoveryStatus.setForeground(Color.LIGHT_GRAY);
			JButton refreshLogin = new JButton("Refresh login status");
			refreshLogin.addActionListener(e ->
			{
				loginRecoveryStatus.setText(plugin.getLoginRecoveryStatusText());
				updateStatus("Login recovery status refreshed");
			});

			for (JComponent component : new JComponent[]{
				groupHeader("Status"),
				playerStatus,
				refreshPlayer,
				loginRecoveryStatus,
				refreshLogin,
				groupHeader("Captures"),
				toolButton("Capture screenshot", plugin::captureScreenshot),
				toolButton("Capture screen", plugin::captureScreen),
				toolButton("Capture minimap", plugin::captureMinimap),
				groupHeader("Debug"),
				toolButton("Click login", plugin::clickLoginScreen),
				toolButton("Prayer targets", plugin::refreshPrayerTargets),
				toolButton("Debug overlay", plugin::debugOverlayState),
				toolButton("Print overlay bounds", plugin::printOverlayCoordinates),
				toolButton("Refresh status", this::refreshStatus)
			})
			{
				stretchAdd(box, component);
			}
			return box;
		});
		return page;
	}

	private JButton toolButton(String label, Runnable action)
	{
		JButton button = new JButton(label);
		button.addActionListener(e -> action.run());
		return button;
	}

	/**
	 * Always-visible quick-start card: pick a farmer, set its target, Start/Stop, see live status —
	 * the common "just start farming" path without digging into the detailed config sections below.
	 */
	private JPanel createQuickStartSection()
	{
		JPanel section = new JPanel(new BorderLayout(0, 4));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.setBorder(BorderFactory.createTitledBorder("Quick start"));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JComboBox<String> farmer = new JComboBox<>(new String[]{"Mob farmer", "Mining", "Woodcutting"});
		JTextField target = new JTextField();
		JLabel status = new JLabel();
		status.setForeground(Color.LIGHT_GRAY);

		Runnable refresh = () ->
		{
			String f = (String) farmer.getSelectedItem();
			if ("Mining".equals(f))
			{
				target.setText(plugin.getMiningFarmerTarget());
				status.setText("Status: " + (plugin.getMiningFarmerRunning() ? "Running" : "Stopped"));
			}
			else if ("Woodcutting".equals(f))
			{
				target.setText(plugin.getWoodcuttingFarmerTarget());
				status.setText("Status: " + (plugin.getWoodcuttingFarmerRunning() ? "Running" : "Stopped"));
			}
			else
			{
				target.setText(plugin.getMobFarmerTarget());
				status.setText("Status: " + (plugin.getMobFarmerRunning() ? "Running" : "Stopped"));
			}
		};
		farmer.addActionListener(e -> refresh.run());

		Runnable applyTarget = () ->
		{
			String f = (String) farmer.getSelectedItem();
			if ("Mining".equals(f))
			{
				plugin.setMiningFarmerTarget(target.getText());
			}
			else if ("Woodcutting".equals(f))
			{
				plugin.setWoodcuttingFarmerTarget(target.getText());
			}
			else
			{
				plugin.setMobFarmerTarget(target.getText());
			}
		};

		JButton start = new JButton("Start");
		start.addActionListener(e ->
		{
			applyTarget.run();
			String f = (String) farmer.getSelectedItem();
			if ("Mining".equals(f))
			{
				plugin.startMiningFarmer(true);
			}
			else if ("Woodcutting".equals(f))
			{
				plugin.startWoodcuttingFarmer(true);
			}
			else
			{
				plugin.startMobFarmer(true);
			}
			refresh.run();
			updateStatus("Started " + f);
		});

		JButton stop = new JButton("Stop");
		stop.addActionListener(e ->
		{
			String f = (String) farmer.getSelectedItem();
			if ("Mining".equals(f))
			{
				plugin.stopMiningFarmer();
			}
			else if ("Woodcutting".equals(f))
			{
				plugin.stopWoodcuttingFarmer();
			}
			else
			{
				plugin.stopMobFarmer();
			}
			refresh.run();
			updateStatus("Stopped " + f);
		});

		JButton refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(e -> refresh.run());

		JPanel buttonsRow = new JPanel(new GridLayout(1, 3, 4, 0));
		buttonsRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttonsRow.add(start);
		buttonsRow.add(stop);
		buttonsRow.add(refreshBtn);

		refresh.run();

		for (JComponent component : new JComponent[]{
			label("Farmer"),
			farmer,
			label("Target"),
			target,
			buttonsRow,
			status
		})
		{
			stretch(component);
			body.add(component);
		}

		section.add(body, BorderLayout.CENTER);
		setCompact(section);
		return section;
	}

	JPanel createActionSlot(int slot)
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
		// Dynamic-height components (the list editor grows/shrinks as rows are added/removed) must
		// NOT have their height pinned, or new rows get clipped. Fill width, leave height free.
		if (component instanceof ListEditorField)
		{
			component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
			return;
		}
		Dimension preferred = component.getPreferredSize();
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(24, preferred.height)));
		component.setPreferredSize(new Dimension(Math.max(160, preferred.width), Math.max(24, preferred.height)));
	}

	/**
	 * Dynamic list editor: one text row per entry, each with a "−" remove button, plus an "Add"
	 * button. Splits the initial pipe/comma/semicolon/newline value into rows and joins back with
	 * "|" via {@link #getValue()}. Mirrors the per-row add/remove list pattern of plugins like NPC
	 * Indicators, so multi-entry config (target/loot/food lists) is editable item-by-item instead of
	 * one fiddly pipe-separated text field.
	 */
	private final class ListEditorField extends JPanel
	{
		private final JPanel rowsPanel = new JPanel();
		private Runnable onChange;

		void setOnChange(Runnable onChange)
		{
			this.onChange = onChange;
		}

		private void fireChange()
		{
			if (onChange != null)
			{
				onChange.run();
			}
		}

		private ListEditorField(String initial)
		{
			setLayout(new BorderLayout(0, 2));
			setBackground(ColorScheme.DARK_GRAY_COLOR);
			rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
			rowsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

			if (initial != null)
			{
				for (String entry : initial.split("\\s*(?:\\||,|;|\\r?\\n)\\s*"))
				{
					if (!entry.trim().isEmpty())
					{
						addRow(entry.trim());
					}
				}
			}

			JButton add = new JButton("+ Add");
			add.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			add.setForeground(Color.LIGHT_GRAY);
			add.addActionListener(e ->
			{
				addRow("");
				revalidate();
				repaint();
			});

			add(rowsPanel, BorderLayout.CENTER);
			add(add, BorderLayout.SOUTH);
		}

		private void addRow(String text)
		{
			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setBackground(ColorScheme.DARK_GRAY_COLOR);
			row.setAlignmentX(LEFT_ALIGNMENT);
			JTextField field = new JTextField(text);
			field.getDocument().addDocumentListener(simpleDocListener(this::fireChange));
			JButton remove = new JButton("−");
			remove.setForeground(Color.RED);
			remove.addActionListener(e ->
			{
				rowsPanel.remove(row);
				ListEditorField.this.revalidate();
				ListEditorField.this.repaint();
				fireChange();
			});
			row.add(field, BorderLayout.CENTER);
			row.add(remove, BorderLayout.EAST);
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(22, field.getPreferredSize().height)));
			rowsPanel.add(row);
		}

		String getValue()
		{
			StringBuilder sb = new StringBuilder();
			for (Component rowComp : rowsPanel.getComponents())
			{
				if (!(rowComp instanceof JPanel))
				{
					continue;
				}
				for (Component c : ((JPanel) rowComp).getComponents())
				{
					if (c instanceof JTextField)
					{
						String t = ((JTextField) c).getText().trim();
						if (!t.isEmpty())
						{
							if (sb.length() > 0)
							{
								sb.append("|");
							}
							sb.append(t);
						}
					}
				}
			}
			return sb.toString();
		}
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
