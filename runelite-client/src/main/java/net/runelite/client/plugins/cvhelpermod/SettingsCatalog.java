/*
 * Builds the CV Helper side-panel pages from RuneLite @ConfigItem/@ConfigSection metadata, augmented
 * with non-config controls. Single registry: adding a farmer's @ConfigItems makes them auto-appear.
 */
package net.runelite.client.plugins.cvhelpermod;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigSectionDescriptor;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

/**
 * Translates the {@link CvHelperModConfig} metadata into a list of navigable {@link SettingsPage}s.
 * Every {@code @ConfigItem} becomes a {@link SettingField} (type → control kind, description → help),
 * grouped by its {@code @ConfigSection} and mapped to a Home category. Reads go through the config
 * proxy (so interface defaults are honoured); writes go through {@link ConfigManager#setConfiguration}
 * which fires ConfigChanged (the plugin re-syncs caches there). Special pages — action slots,
 * prayer/spellbook toggles, farmer Start/Stop controls — are layered on top.
 */
final class SettingsCatalog
{
	private static final String GROUP = CvHelperModConfig.GROUP;

	/** String keys that hold a delimited list and should use the +/- list editor. */
	private static final Set<String> LIST_KEYS = new HashSet<>(Arrays.asList(
		"mobFarmerTarget",
		"mobFarmerTargetBlacklist",
		"mobFarmerFoodItems",
		"mobFarmerDoorAllowlist",
		"mobFarmerLootItems",
		"mobFarmerLootBlacklist",
		"mobFarmerNeverDropItems",
		"mobFarmerDropItems",
		"mobFarmerIntermediateItems",
		"mobFarmerCombatInterruptItems",
		"mobFarmerHighAlchItems",
		"mobFarmerHighAlchBlacklist",
		"dropPolicyItems",
		"dropPolicyProtectedItems",
		"miningDropItems"));

	private final CvHelperModPlugin plugin;
	private final CvHelperModPanel panel;
	private final ConfigManager cm;
	private final CvHelperModConfig config;

	private final Map<String, Method> keyToMethod = new LinkedHashMap<>();
	private final Map<String, ConfigSectionDescriptor> sections = new LinkedHashMap<>();
	private final Map<String, List<ConfigItemDescriptor>> bySection = new LinkedHashMap<>();
	private final List<ConfigItemDescriptor> ungrouped = new ArrayList<>();

	SettingsCatalog(CvHelperModPlugin plugin, CvHelperModPanel panel)
	{
		this.plugin = plugin;
		this.panel = panel;
		this.cm = plugin.getConfigManager();
		this.config = plugin.getConfig();
		index();
	}

	private void index()
	{
		for (Method m : CvHelperModConfig.class.getMethods())
		{
			ConfigItem ci = m.getAnnotation(ConfigItem.class);
			if (ci != null)
			{
				keyToMethod.put(ci.keyName(), m);
			}
		}

		ConfigDescriptor descriptor = cm.getConfigDescriptor(config);
		for (ConfigSectionDescriptor section : descriptor.getSections())
		{
			sections.put(section.getKey(), section);
		}
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			if (item.getItem().hidden())
			{
				continue;
			}
			String section = item.getItem().section();
			if (section == null || section.isEmpty())
			{
				ungrouped.add(item);
			}
			else
			{
				bySection.computeIfAbsent(section, k -> new ArrayList<>()).add(item);
			}
		}

		Comparator<ConfigItemDescriptor> byPosition = Comparator.comparingInt(i -> i.getItem().position());
		bySection.values().forEach(list -> list.sort(byPosition));
		ungrouped.sort(byPosition);
	}

	List<SettingsPage> build()
	{
		List<SettingsPage> pages = new ArrayList<>();

		// Farmers
		pages.add(mobFarmerPage());
		pages.add(skillFarmerPage("mining", "Mining", "minerSection",
			live -> plugin.startMiningFarmer(live), plugin::stopMiningFarmer, live -> plugin.runMiningFarmerStep(live)));
		pages.add(skillFarmerPage("woodcutting", "Woodcutting", "woodcutterSection",
			live -> plugin.startWoodcuttingFarmer(live), plugin::stopWoodcuttingFarmer, live -> plugin.runWoodcuttingFarmerStep(live)));

		// Combat
		pages.add(togglesPage("prayer", "Prayer toggles", "Highlighted prayers", plugin.getPrayerNames(), true));
		pages.add(togglesPage("spell", "Spellbook toggles", "Highlighted spells", plugin.getSpellbookNames(), false));
		pages.add(actionHotkeysPage());

		// Display
		pages.add(overlaysPage());

		// System
		pages.add(sectionPage("chatResponder", "Chat responder", "Auto-reply to chat", "chatResponderSection", SettingsPage.CAT_SYSTEM));
		pages.add(systemPage());

		return pages;
	}

	// ------------------------------------------------------------------ pages

	private SettingsPage mobFarmerPage()
	{
		SettingsPage page = new SettingsPage("mobFarmer", "Mob farmer",
			"Combat, looting, burying and high-alch farming", sectionHelp("mobFarmerSection"), SettingsPage.CAT_FARMERS);
		addFarmerControls(page, "mobFarmer", live -> plugin.startMobFarmer(live), plugin::stopMobFarmer, live -> plugin.runMobFarmerStep(live));
		for (ConfigItemDescriptor item : section("mobFarmerSection"))
		{
			page.add(fieldFor(item).group(mobGroup(item.getItem().keyName())));
		}
		return page;
	}

	private SettingsPage skillFarmerPage(String id, String title, String sectionKey,
		java.util.function.Consumer<Boolean> start, Runnable stop, java.util.function.Consumer<Boolean> step)
	{
		SettingsPage page = new SettingsPage(id, title, title + " farmer", sectionHelp(sectionKey), SettingsPage.CAT_FARMERS);
		addFarmerControls(page, id, start, stop, step);
		for (ConfigItemDescriptor item : section(sectionKey))
		{
			page.add(fieldFor(item));
		}
		return page;
	}

	private void addFarmerControls(SettingsPage page, String id,
		java.util.function.Consumer<Boolean> start, Runnable stop, java.util.function.Consumer<Boolean> step)
	{
		page.add(SettingField.action(id + ".start", "Start (live)", "Start the live farming loop.", () -> start.accept(true)).group("Controls"));
		page.add(SettingField.action(id + ".stop", "Stop", "Stop the farming loop.", stop).group("Controls"));
		page.add(SettingField.action(id + ".dry", "Dry step", "Run one dry (no-click) step for debugging.", () -> step.accept(false)).group("Controls"));
		page.add(SettingField.action(id + ".live", "Live step", "Run one live step.", () -> step.accept(true)).group("Controls"));
	}

	private SettingsPage togglesPage(String id, String title, String summary, List<String> names, boolean prayer)
	{
		SettingsPage page = new SettingsPage(id, title, summary,
			"Toggle which " + (prayer ? "prayers" : "spells") + " the overlay highlights and the action system may use.",
			SettingsPage.CAT_COMBAT);
		for (String name : names)
		{
			page.add(SettingField.toggle(id + "." + name, name, null,
				() -> prayer ? plugin.isPrayerEnabled(name) : plugin.isSpellEnabled(name),
				value ->
				{
					if (prayer)
					{
						plugin.setPrayerEnabled(name, value);
					}
					else
					{
						plugin.setSpellEnabled(name, value);
					}
				}));
		}
		return page;
	}

	private SettingsPage actionHotkeysPage()
	{
		SettingsPage parent = new SettingsPage("actions", "Action hotkeys", "Hotkey-triggered actions",
			"Each slot binds a hotkey to an action (cast a spell, click an item, toggle a prayer). Open a slot to configure it; timing and return-tab hotkeys are shared below.",
			SettingsPage.CAT_COMBAT);

		int slots = plugin.getActionSlotCount();
		for (int i = 1; i <= slots; i++)
		{
			final int slot = i;
			SettingsPage slotPage = new SettingsPage("action." + slot, "Action " + slot, "Slot " + slot + " binding", null, null);
			slotPage.body(() -> panel.createActionSlot(slot));
			parent.add(SettingField.pageLink(slotPage));
		}

		for (ConfigItemDescriptor item : section("actionSection"))
		{
			String key = item.getItem().keyName();
			if (key.matches(".*[1-9]$"))
			{
				continue; // per-slot, configured on the slot page
			}
			parent.add(fieldFor(item).group("Timing & return tabs"));
		}
		return parent;
	}

	private SettingsPage overlaysPage()
	{
		SettingsPage page = new SettingsPage("overlays", "Overlays", "On-screen highlight boxes",
			"Toggle the debug/automation overlays drawn on the game canvas, and the localhost export server.",
			SettingsPage.CAT_DISPLAY);
		for (ConfigItemDescriptor item : ungrouped)
		{
			if (isOverlayKey(item.getItem().keyName()))
			{
				page.add(fieldFor(item));
			}
		}
		return page;
	}

	private SettingsPage systemPage()
	{
		SettingsPage page = new SettingsPage("system", "Server & system", "Webhook, port, hotkeys, anti-idle",
			"Local export server, optional webhook, global hotkeys and anti-idle behaviour.", SettingsPage.CAT_SYSTEM);
		for (ConfigItemDescriptor item : ungrouped)
		{
			String key = item.getItem().keyName();
			if (isOverlayKey(key))
			{
				continue; // shown on the Overlays page
			}
			page.add(fieldFor(item).group(systemGroup(key)));
		}
		return page;
	}

	private SettingsPage sectionPage(String id, String title, String summary, String sectionKey, String category)
	{
		SettingsPage page = new SettingsPage(id, title, summary, sectionHelp(sectionKey), category);
		for (ConfigItemDescriptor item : section(sectionKey))
		{
			page.add(fieldFor(item));
		}
		return page;
	}

	// ------------------------------------------------------------------ field mapping

	@SuppressWarnings({"unchecked", "rawtypes"})
	private SettingField fieldFor(ConfigItemDescriptor descriptor)
	{
		ConfigItem item = descriptor.getItem();
		String key = item.keyName();
		String label = item.name();
		String help = item.description();
		Type type = descriptor.getType();
		Method method = keyToMethod.get(key);

		if (type instanceof Class)
		{
			Class<?> clazz = (Class<?>) type;
			if (clazz == boolean.class || clazz == Boolean.class)
			{
				return SettingField.toggle(key, label, help,
					() -> Boolean.TRUE.equals(read(method)),
					value -> cm.setConfiguration(GROUP, key, value));
			}
			if (clazz == int.class || clazz == Integer.class)
			{
				Range range = descriptor.getRange();
				Units units = descriptor.getUnits();
				return SettingField.intField(key, label, help,
					() ->
					{
						Object value = read(method);
						return value instanceof Number ? ((Number) value).intValue() : 0;
					},
					value -> cm.setConfiguration(GROUP, key, value),
					range != null ? range.min() : Integer.MIN_VALUE,
					range != null ? range.max() : Integer.MAX_VALUE,
					range != null,
					units != null ? units.value() : null);
			}
			if (clazz.isEnum())
			{
				List<SettingField.Choice> choices = new ArrayList<>();
				for (Object constant : clazz.getEnumConstants())
				{
					choices.add(new SettingField.Choice(((Enum<?>) constant).name(), prettyEnum(((Enum<?>) constant).name()), null));
				}
				final Class enumClass = clazz;
				return SettingField.choice(key, label, help, choices,
					() ->
					{
						Object value = read(method);
						return value == null ? null : ((Enum<?>) value).name();
					},
					value -> cm.setConfiguration(GROUP, key, Enum.valueOf(enumClass, value)));
			}
			if (clazz == Keybind.class)
			{
				return SettingField.hotkey(key, label, help,
					() ->
					{
						Object value = read(method);
						return value instanceof Keybind ? (Keybind) value : Keybind.NOT_SET;
					},
					value -> cm.setConfiguration(GROUP, key, value));
			}
		}

		// Strings (and anything else) — list editor for known list keys, plain text otherwise.
		if (LIST_KEYS.contains(key))
		{
			return SettingField.list(key, label, help, () -> str(read(method)), value -> cm.setConfiguration(GROUP, key, value));
		}
		return SettingField.text(key, label, help, () -> str(read(method)), value -> cm.setConfiguration(GROUP, key, value));
	}

	// ------------------------------------------------------------------ helpers

	private List<ConfigItemDescriptor> section(String key)
	{
		return bySection.getOrDefault(key, java.util.Collections.emptyList());
	}

	private String sectionHelp(String key)
	{
		ConfigSectionDescriptor section = sections.get(key);
		return section != null ? section.getSection().description() : null;
	}

	private Object read(Method method)
	{
		if (method == null)
		{
			return null;
		}
		try
		{
			return method.invoke(config);
		}
		catch (ReflectiveOperationException e)
		{
			return null;
		}
	}

	private static String str(Object value)
	{
		return value == null ? "" : value.toString();
	}

	private static boolean isOverlayKey(String key)
	{
		return key.startsWith("show") || key.equals("enableLocalExport");
	}

	private static String prettyEnum(String name)
	{
		String s = name.replace('_', ' ').trim().toLowerCase();
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static String mobGroup(String key)
	{
		String k = key.toLowerCase();
		if (k.contains("loot") || k.contains("grounditems") || k.contains("drop") || k.contains("cleanup") || k.contains("pile"))
		{
			return "Loot & drop";
		}
		if (k.contains("alch"))
		{
			return "High alchemy";
		}
		if (k.contains("login") || k.contains("recovery") || k.contains("world") || k.contains("disconnect") || k.contains("resume"))
		{
			return "Login & recovery";
		}
		if (k.contains("eat") || k.contains("food") || k.contains("survi") || k.contains("brew") || k.contains("health"))
		{
			return "Survival";
		}
		if (k.contains("door") || k.contains("path") || k.contains("stutter") || k.contains("reach"))
		{
			return "Pathing";
		}
		if (k.contains("intermediate") || k.contains("interrupt"))
		{
			return "Intermediate actions";
		}
		return "Targeting & combat";
	}

	private static String systemGroup(String key)
	{
		String k = key.toLowerCase();
		if (k.endsWith("hotkey"))
		{
			return "Hotkeys";
		}
		if (k.contains("antiidle"))
		{
			return "Anti-idle";
		}
		if (k.contains("webhook") || k.contains("port") || k.contains("export"))
		{
			return "Server";
		}
		return "Misc";
	}
}
