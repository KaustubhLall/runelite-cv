/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

import com.google.inject.Inject;
import net.runelite.client.game.ItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Singleton
public class ItemLookupUtil
{
	private static final Logger log = LoggerFactory.getLogger(ItemLookupUtil.class);

	@Inject
	private ItemManager itemManager;

	// Cache for item name to ID lookups to avoid repeated expensive searches
	private final Map<String, Integer> itemNameCache = new ConcurrentHashMap<>();

	/**
	 * Find an item ID by its name using multiple matching strategies.
	 * Results are cached to avoid repeated expensive searches.
	 *
	 * @param name The item name to search for
	 * @return The item ID, or -1 if not found
	 */
	public int findItemIdByName(String name)
	{
		try
		{
			String lowerName = name.toLowerCase().trim();
			if (itemNameCache.containsKey(lowerName))
			{
				return itemNameCache.get(lowerName);
			}

			// Normalize the search name by removing special characters and extra spaces
			String normalizedName = lowerName.replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();

			// Search through all item compositions to find a match
			// Item IDs go beyond 30000, so search a wider range
			for (int id = 0; id < 50000; id++)
			{
				try
				{
					net.runelite.api.ItemComposition composition = itemManager.getItemComposition(id);
					if (composition != null)
					{
						String itemName = composition.getName();
						if (itemName != null)
						{
							String normalizedItemName = itemName.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
							
							// Try exact match first
							if (normalizedItemName.equals(normalizedName))
							{
								itemNameCache.put(lowerName, id);
								return id;
							}
							
							// Try case-insensitive match without normalization
							if (itemName.equalsIgnoreCase(name))
							{
								itemNameCache.put(lowerName, id);
								return id;
							}
							
							// Try contains match for partial names
							if (normalizedItemName.contains(normalizedName) || normalizedName.contains(normalizedItemName))
							{
								itemNameCache.put(lowerName, id);
								return id;
							}
						}
					}
				}
				catch (Exception e)
				{
					// Skip invalid IDs
					continue;
				}
			}

			// Not found
			itemNameCache.put(lowerName, -1);
			return -1;
		}
		catch (Exception e)
		{
			log.debug("Failed to find item ID for name: {}", name, e);
			return -1;
		}
	}

	/**
	 * Clear the item name cache. Useful for testing or when item data changes.
	 */
	public void clearCache()
	{
		itemNameCache.clear();
	}
}
