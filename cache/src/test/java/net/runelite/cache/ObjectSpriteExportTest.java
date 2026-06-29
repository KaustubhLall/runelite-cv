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
 * Standalone test to export object sprites with console output.
 */
public class ObjectSpriteExportTest
{
	private static final Logger logger = LoggerFactory.getLogger(ObjectSpriteExportTest.class);

	public static void main(String[] args) throws IOException
	{
		File outputDir = new File("tools/asset-library");
		outputDir.mkdirs();

		// Use temp cache directory where RuneLite stores game data
		File cacheLocation = new File(System.getProperty("java.io.tmpdir"), "cache-165");

		logger.info("Using cache from: {}", cacheLocation);

		try (Store store = new Store(cacheLocation))
		{
			store.load();

			AssetLibraryExporter exporter = new AssetLibraryExporter(store);
			
			// Try just one object ID first to debug
			int[] objectIds = {10000};
			exporter.exportSpecificObjects(outputDir, objectIds);
		}

		logger.info("Object sprites exported to {}", outputDir);
	}
}
