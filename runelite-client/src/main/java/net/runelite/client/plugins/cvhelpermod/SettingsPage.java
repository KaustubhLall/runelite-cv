/*
 * Declarative model for one navigable CV Helper side-panel page.
 */
package net.runelite.client.plugins.cvhelpermod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JComponent;

/**
 * A navigable page ("block") in the CV Helper panel. It either renders a list of {@link SettingField}s
 * or supplies a custom body (escape hatch for complex controls). Pages with a non-null {@link #category}
 * appear as blocks on the Home screen; sub-pages (e.g. a single action slot) have a null category and
 * are reached only by drilling in from a parent page.
 */
final class SettingsPage
{
	static final String CAT_FARMERS = "Farmers";
	static final String CAT_COMBAT = "Combat";
	static final String CAT_DISPLAY = "Display";
	static final String CAT_SYSTEM = "System";

	/** Home category order. */
	static final String[] CATEGORIES = {CAT_FARMERS, CAT_COMBAT, CAT_DISPLAY, CAT_SYSTEM};

	final String id;
	final String title;
	final String summary;
	final String help;
	final String category;
	final List<SettingField> fields = new ArrayList<>();
	Supplier<JComponent> customBody;

	SettingsPage(String id, String title, String summary, String help, String category)
	{
		this.id = id;
		this.title = title;
		this.summary = summary;
		this.help = help;
		this.category = category;
	}

	SettingsPage add(SettingField field)
	{
		if (field != null)
		{
			fields.add(field);
		}
		return this;
	}

	SettingsPage body(Supplier<JComponent> customBody)
	{
		this.customBody = customBody;
		return this;
	}

	/** Lower-cased haystack for the home search box (title, summary, help and every field). */
	String searchText()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(title == null ? "" : title).append(' ');
		sb.append(summary == null ? "" : summary).append(' ');
		sb.append(help == null ? "" : help).append(' ');
		for (SettingField f : fields)
		{
			sb.append(f.searchText()).append(' ');
		}
		return sb.toString().toLowerCase();
	}
}
