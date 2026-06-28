/*
 * Copyright (c) 2026
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.cache;

import java.io.File;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.cache.fs.Store;

/**
 * Test for AssetLibraryExporter.
 * This test exports game assets (items, sprites) to a directory for frontend consumption.
 * Updated to export specific items from user's inventory.
 */
public class AssetLibraryExporterTest
{
	private static final Logger logger = LoggerFactory.getLogger(AssetLibraryExporterTest.class);

	@Rule
	public TemporaryFolder folder = StoreLocation.getTemporaryFolder();

	@Test
	public void testExportAll() throws IOException
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
			
			// Export only specific items from user's inventory (much faster)
			int[] itemIds = {1275, 440, 6739, 12779, 24361, 1135, 1725, 1359, 1113, 1185, 1099,
				// Food items for survival section
				315, 333, 329, 361, 379, 373, 7946, 385, 391, 13441, 1891, 1993, 3142, 2142, 2140, 2309, 2293, 2323, 532, 592, 526,
				// Coin item for GP icon
				995};
			exporter.exportSpecificItems(outputDir, itemIds);
		}

		logger.info("Asset library exported to {}", outputDir);
	}

	@Test
	public void testExportCoinSprite() throws IOException
	{
		File outputDir = new File("tools/asset-library");
		outputDir.mkdirs();

		// Use temp cache directory where RuneLite stores game data (same as testExportAll)
		File cacheLocation = new File(System.getProperty("java.io.tmpdir"), "cache-165");

		logger.info("Using cache from: {}", cacheLocation);

		try (Store store = new Store(cacheLocation))
		{
			store.load();

			AssetLibraryExporter exporter = new AssetLibraryExporter(store);
			
			// Export all coin stack visual variants for GP icons (single, 2, 3, 4, 5, 25, 100, 250, 1000, 10000)
			int[] itemIds = {995, 996, 997, 998, 999, 1000, 1001, 1002, 1003, 1004};
			exporter.exportSpecificItems(outputDir, itemIds);
		}

		logger.info("Coin item exported to {}", outputDir);
	}

	@Test
	public void testExportObjects() throws IOException
	{
		File outputDir = new File("tools/asset-library");
		outputDir.mkdirs();

		// Write output to a file for debugging
		File debugFile = new File("tools/asset-library/debug-object-export.txt");
		debugFile.createNewFile();
		java.io.PrintWriter writer = new java.io.PrintWriter(debugFile);

		// Use temp cache directory where RuneLite stores game data
		File cacheLocation = new File(System.getProperty("java.io.tmpdir"), "cache-165");

		writer.println("Using cache from: " + cacheLocation);
		logger.info("Using cache from: {}", cacheLocation);

		try (Store store = new Store(cacheLocation))
		{
			store.load();
			writer.println("Store loaded successfully");

			// Check if ObjectManager can load objects
			ObjectManager objectManager = new ObjectManager(store);
			objectManager.load();
			writer.println("ObjectManager loaded");
			
			// Try to find a valid object ID
			int foundCount = 0;
			for (int i = 0; i < 10000; i++)
			{
				net.runelite.cache.definitions.ObjectDefinition obj = objectManager.getObject(i);
				if (obj != null && obj.getObjectModels() != null && obj.getObjectModels().length > 0)
				{
					writer.println("Found valid object ID: " + i + " - " + obj.getName() + " with " + obj.getObjectModels().length + " models");
					foundCount++;
					
					if (foundCount == 1)
					{
						// Export just this one object
						AssetLibraryExporter exporter = new AssetLibraryExporter(store);
						int[] objectIds = {i};
						exporter.exportSpecificObjects(outputDir, objectIds);
						writer.println("Export completed for object ID: " + i);
						break;
					}
				}
			}
			writer.println("Total valid objects found in range 0-10000: " + foundCount);
		}
		catch (Exception e)
		{
			writer.println("Error: " + e.getMessage());
			e.printStackTrace(writer);
		}

		writer.println("Object sprites exported to " + outputDir);
		logger.info("Object sprites exported to {}", outputDir);
		writer.close();
	}
}
