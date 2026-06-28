/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.cache;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.cache.fs.Store;

/**
 * Standalone main class to export object sprites with console output.
 */
public class ObjectExportMain
{
	private static final Logger logger = LoggerFactory.getLogger(ObjectExportMain.class);

	public static void main(String[] args) throws IOException
	{
		// Use absolute path to resolve from project root
		File projectRoot = new File(System.getProperty("user.dir")).getParentFile();
		File outputDir = new File(projectRoot, "tools/asset-library");
		outputDir.mkdirs();

		System.out.println("Output directory: " + outputDir.getAbsolutePath());

		// Use temp cache directory where RuneLite stores game data
		File cacheLocation = new File(System.getProperty("java.io.tmpdir"), "cache-165");

		System.out.println("Using cache from: " + cacheLocation);
		logger.info("Using cache from: {}", cacheLocation);

		try (Store store = new Store(cacheLocation))
		{
			store.load();
			System.out.println("Store loaded successfully");

			// Check if ObjectManager can load objects
			ObjectManager objectManager = new ObjectManager(store);
			objectManager.load();
			System.out.println("ObjectManager loaded");
			
			// Keywords for skilling objects
			String[] miningKeywords = {"rock", "ore", "essence", "coal", "iron", "copper", "tin", "clay", "silver", "gold", "mithril", "adamant", "runite", "bluerite", "barronite", "daeyalt", "granite", "sandstone", "gem", "amethyst", "volcanic", "sulphur", "salt", "basalt", "lova", "infernal", "calcified", "guardian", "specimen", "saltpetre", "abyss", "pillar", "boulder"};
			String[] treeKeywords = {"tree", "oak", "willow", "maple", "yew", "magic", "teak", "mahogany", "pine", "arctic", "hollow", "blisterwood", "camphor", "ironwood", "redwood", "rosewood", "jungle", "achey", "evergreen", "dying", "dead"};
			String[] otherKeywords = {"fishing", "herb", "farming", "crafting", "firemaking", "agility", "thieving", "hunter"};
			
			// Combine all keywords
			String[] allKeywords = new String[miningKeywords.length + treeKeywords.length + otherKeywords.length];
			System.arraycopy(miningKeywords, 0, allKeywords, 0, miningKeywords.length);
			System.arraycopy(treeKeywords, 0, allKeywords, miningKeywords.length, treeKeywords.length);
			System.arraycopy(otherKeywords, 0, allKeywords, miningKeywords.length + treeKeywords.length, otherKeywords.length);
			
			// Find objects with map scene IDs and skilling objects without them
			java.util.Set<Integer> exportIdsSet = new java.util.LinkedHashSet<>();
			java.util.List<Integer> skillingObjectIds = new java.util.ArrayList<>();
			int mapSceneCount = 0;
			for (int id = 0; id < 30000; id++)
			{
				net.runelite.cache.definitions.ObjectDefinition obj = objectManager.getObject(id);
				if (obj != null && obj.getObjectModels() != null && obj.getObjectModels().length > 0)
				{
					boolean skilling = false;
					String name = obj.getName();
					if (name != null && !name.isEmpty())
					{
						String lowerName = name.toLowerCase();
						for (String keyword : allKeywords)
						{
							if (lowerName.contains(keyword))
							{
								skilling = true;
								break;
							}
						}
					}

					if (obj.getMapSceneID() >= 0)
					{
						exportIdsSet.add(id);
						mapSceneCount++;
					}
					else if (skilling)
					{
						exportIdsSet.add(id);
						skillingObjectIds.add(id);
					}
				}
			}
			System.out.println("Found " + mapSceneCount + " objects with map scene IDs");
			System.out.println("Found " + skillingObjectIds.size() + " skilling objects without map scene IDs");
			
			// Debug specific mining rock IDs from frontend and search for actual rock names
			int[] debugIds = {11364, 11391, 11390, 18943};
			for (int debugId : debugIds)
			{
				net.runelite.cache.definitions.ObjectDefinition obj = objectManager.getObject(debugId);
				if (obj == null)
				{
					System.out.println("Debug ID " + debugId + ": NOT FOUND");
				}
				else
				{
					String impostors = obj.getConfigChangeDest() == null ? "none" : java.util.Arrays.toString(obj.getConfigChangeDest());
					System.out.println("Debug ID " + debugId + ": name=" + obj.getName() + " mapScene=" + obj.getMapSceneID() + " models=" + (obj.getObjectModels() != null ? obj.getObjectModels().length : 0) + " impostors=" + impostors + " exported=" + exportIdsSet.contains(debugId));
				}
			}
			
			// Search for objects whose names actually match rock/ore names in the cache
			String[] exactRockNames = {"Iron rocks", "Rocks", "Copper rocks", "Tin rocks", "Coal rocks", "Mithril rocks", "Adamantite rocks", "Runite rocks", "Gold rocks", "Silver rocks", "Clay rocks", "Blurite rocks"};
			for (int id = 0; id < 30000; id++)
			{
				net.runelite.cache.definitions.ObjectDefinition obj = objectManager.getObject(id);
				if (obj != null && obj.getName() != null)
				{
					for (String rockName : exactRockNames)
					{
						if (rockName.equalsIgnoreCase(obj.getName()))
						{
							System.out.println("Exact rock name match: ID " + id + " = " + obj.getName() + " mapScene=" + obj.getMapSceneID());
							exportIdsSet.add(id);
							break;
						}
					}
				}
			}
			
			int[] exportIds = exportIdsSet.stream().mapToInt(i -> i).toArray();
			
			AssetLibraryExporter exporter = new AssetLibraryExporter(store);
			System.out.println("Exporting " + exportIds.length + " object sprites (map scene + skilling fallback)");
			exporter.exportSpecificObjects(outputDir, exportIds);
			System.out.println("Export completed");
		}

		System.out.println("Object sprites exported to " + outputDir);
		logger.info("Object sprites exported to {}", outputDir);
	}
}
