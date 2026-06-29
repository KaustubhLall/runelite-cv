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
import net.runelite.cache.fs.Store;

/**
 * Standalone runner for AssetLibraryExporter.
 * Usage: java -cp cache.jar net.runelite.cache.AssetLibraryExporterRunner <cache-path> <output-path>
 */
public class AssetLibraryExporterRunner
{
	public static void main(String[] args) throws IOException
	{
		if (args.length < 2)
		{
			System.err.println("Usage: java AssetLibraryExporterRunner <cache-path> <output-path>");
			System.err.println("Example: java AssetLibraryExporterRunner ~/.runelite/cache ./assets");
			System.exit(1);
		}

		File cachePath = new File(args[0]);
		File outputPath = new File(args[1]);

		if (!cachePath.exists())
		{
			System.err.println("Cache path does not exist: " + cachePath);
			System.exit(1);
		}

		System.out.println("Loading cache from: " + cachePath);
		System.out.println("Exporting assets to: " + outputPath);

		try (Store store = new Store(cachePath))
		{
			store.load();

			AssetLibraryExporter exporter = new AssetLibraryExporter(store);
			exporter.exportAll(outputPath);
		}

		System.out.println("Asset export complete!");
	}
}
