/*
 * Declarative model for one CV Helper side-panel setting.
 */
package net.runelite.client.plugins.cvhelpermod;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.swing.JComponent;
import net.runelite.client.config.Keybind;

/**
 * Declarative description of one setting in the CV Helper side panel. Pure data plus typed accessors;
 * {@link CvHelperModPanel} renders it. Most fields are produced generically from RuneLite
 * {@code @ConfigItem} metadata by {@link SettingsCatalog}, but the ACTION / CUSTOM / PAGE_LINK kinds
 * let non-config controls (Start/Stop buttons, the dynamic action-target combo, sub-page links)
 * participate in the same navigation system.
 */
final class SettingField
{
	enum Kind
	{
		TOGGLE, TEXT, INT, CHOICE, LIST, HOTKEY, ACTION, INFO, CUSTOM, PAGE_LINK
	}

	/** One option of a {@link Kind#CHOICE} field. */
	static final class Choice
	{
		final String value;
		final String label;
		final String help;

		Choice(String value, String label, String help)
		{
			this.value = value;
			this.label = label;
			this.help = help;
		}
	}

	final Kind kind;
	/** Config key or synthetic id; also used for search matching. */
	final String key;
	final String label;
	final String help;
	/** Optional sub-header this field sits under within a page. */
	String group;

	BooleanSupplier getBool;
	Consumer<Boolean> setBool;
	Supplier<String> getText;
	Consumer<String> setText;
	IntSupplier getInt;
	IntConsumer setInt;
	int min = Integer.MIN_VALUE;
	int max = Integer.MAX_VALUE;
	boolean bounded;
	String units;
	List<Choice> choices;
	Supplier<String> getChoice;
	Consumer<String> setChoice;
	Supplier<Keybind> getKey;
	Consumer<Keybind> setKey;
	Runnable action;
	Supplier<JComponent> custom;
	SettingsPage linkTarget;

	private SettingField(Kind kind, String key, String label, String help)
	{
		this.kind = kind;
		this.key = key;
		this.label = label;
		this.help = help;
	}

	static SettingField toggle(String key, String label, String help, BooleanSupplier get, Consumer<Boolean> set)
	{
		SettingField f = new SettingField(Kind.TOGGLE, key, label, help);
		f.getBool = get;
		f.setBool = set;
		return f;
	}

	static SettingField text(String key, String label, String help, Supplier<String> get, Consumer<String> set)
	{
		SettingField f = new SettingField(Kind.TEXT, key, label, help);
		f.getText = get;
		f.setText = set;
		return f;
	}

	static SettingField intField(String key, String label, String help, IntSupplier get, IntConsumer set,
		int min, int max, boolean bounded, String units)
	{
		SettingField f = new SettingField(Kind.INT, key, label, help);
		f.getInt = get;
		f.setInt = set;
		f.min = min;
		f.max = max;
		f.bounded = bounded;
		f.units = units;
		return f;
	}

	static SettingField choice(String key, String label, String help, List<Choice> choices,
		Supplier<String> get, Consumer<String> set)
	{
		SettingField f = new SettingField(Kind.CHOICE, key, label, help);
		f.choices = choices;
		f.getChoice = get;
		f.setChoice = set;
		return f;
	}

	static SettingField list(String key, String label, String help, Supplier<String> get, Consumer<String> set)
	{
		SettingField f = new SettingField(Kind.LIST, key, label, help);
		f.getText = get;
		f.setText = set;
		return f;
	}

	static SettingField hotkey(String key, String label, String help, Supplier<Keybind> get, Consumer<Keybind> set)
	{
		SettingField f = new SettingField(Kind.HOTKEY, key, label, help);
		f.getKey = get;
		f.setKey = set;
		return f;
	}

	static SettingField action(String key, String label, String help, Runnable run)
	{
		SettingField f = new SettingField(Kind.ACTION, key, label, help);
		f.action = run;
		return f;
	}

	static SettingField info(String label, String help)
	{
		return new SettingField(Kind.INFO, null, label, help);
	}

	static SettingField custom(String key, String label, Supplier<JComponent> component)
	{
		SettingField f = new SettingField(Kind.CUSTOM, key, label, null);
		f.custom = component;
		return f;
	}

	static SettingField pageLink(SettingsPage target)
	{
		SettingField f = new SettingField(Kind.PAGE_LINK, target.id, target.title, target.summary);
		f.linkTarget = target;
		return f;
	}

	SettingField group(String group)
	{
		this.group = group;
		return this;
	}

	/** Lower-cased haystack for the home search box. */
	String searchText()
	{
		String l = label == null ? "" : label;
		String k = key == null ? "" : key;
		String h = help == null ? "" : help;
		return (l + " " + k + " " + h).toLowerCase();
	}
}
