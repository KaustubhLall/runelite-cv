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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.definitions.SpriteDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.definitions.loaders.SpriteLoader;
import net.runelite.cache.item.ItemSpriteFactory;
import net.runelite.cache.item.ObjectSpriteFactory;
import net.runelite.cache.definitions.providers.ModelProvider;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;
import net.runelite.cache.IndexType;

/**
 * Exports game assets (items, sprites) as organized PNG files for frontend consumption.
 * Leverages RuneLite's cache infrastructure to extract official game assets.
 */
@Slf4j
public class AssetLibraryExporter
{
	private final Store store;
	private final ItemManager itemManager;
	private final ObjectManager objectManager;
	private final SpriteManager spriteManager;
	private final TextureManager textureManager;
	private final ModelProvider modelProvider;
	private final SpriteDefinition[] mapSceneSprites;

	private final Map<String, AssetMetadata> metadata = new HashMap<>();

	public AssetLibraryExporter(Store store) throws IOException
	{
		this.store = store;
		this.itemManager = new ItemManager(store);
		this.objectManager = new ObjectManager(store);
		this.spriteManager = new SpriteManager(store);
		this.textureManager = new TextureManager(store);
		
		// Create ModelProvider that loads models on demand
		this.modelProvider = new ModelProvider()
		{
			@Override
			public ModelDefinition provide(int modelId) throws IOException
			{
				Index models = store.getIndex(IndexType.MODELS);
				Archive archive = models.getArchive(modelId);
				
				if (archive == null)
				{
					return null;
				}
				
				byte[] data = archive.decompress(store.getStorage().loadArchive(archive));
				return new ModelLoader().load(modelId, data);
			}
		};

		itemManager.load();
		itemManager.link();
		objectManager.load();
		spriteManager.load();
		textureManager.load();
		this.mapSceneSprites = loadMapSceneSprites();
	}

	private SpriteDefinition[] loadMapSceneSprites() throws IOException
	{
		try
		{
			Storage storage = store.getStorage();
			Index index = store.getIndex(IndexType.SPRITES);
			Archive archive = index.findArchiveByName("mapscene");
			if (archive == null)
			{
				log.warn("Could not find mapscene sprite archive");
				return new SpriteDefinition[0];
			}
			byte[] contents = archive.decompress(storage.loadArchive(archive));
			SpriteLoader loader = new SpriteLoader();
			return loader.load(archive.getArchiveId(), contents);
		}
		catch (Exception e)
		{
			log.warn("Failed to load map scene sprites: {}", e.getMessage());
			return new SpriteDefinition[0];
		}
	}

	/**
	 * Export all assets to the specified directory.
	 * Creates organized subdirectories:
	 * - items/ - Item sprites (ID.png)
	 * - sprites/ - Game sprites (ID-frame.png)
	 * - metadata.json - Asset metadata
	 */
	public void exportAll(File outputDir) throws IOException
	{
		log.info("Starting asset library export to {}", outputDir);
		outputDir.mkdirs();

		File itemsDir = new File(outputDir, "items");
		File spritesDir = new File(outputDir, "sprites");

		// Export sprites first (faster)
		exportSprites(spritesDir);
		
		// Export items (slower, 3D rendering) - optional for performance
		exportItems(itemsDir);
		
		exportMetadata(new File(outputDir, "metadata.json"));

		log.info("Asset library export complete");
	}

	/**
	 * Export only sprites (fastest option).
	 * Useful for quick exports when item sprites aren't needed.
	 */
	public void exportSpritesOnly(File outputDir) throws IOException
	{
		log.info("Starting sprite-only export to {}", outputDir);
		outputDir.mkdirs();

		File spritesDir = new File(outputDir, "sprites");
		exportSprites(spritesDir);

		log.info("Sprite export complete");
	}

	/**
	 * Export only specific item IDs as PNG files.
	 * Much faster than exportAll() when you only need certain items.
	 */
	public void exportSpecificItems(File outputDir, int[] itemIds) throws IOException
	{
		log.info("Exporting {} specific item sprites to {}", itemIds.length, outputDir);
		outputDir.mkdirs();

		File itemsDir = new File(outputDir, "items");
		itemsDir.mkdirs();

		int exported = 0;
		int skipped = 0;

		for (int itemId : itemIds)
		{
			ItemDefinition item = itemManager.getItem(itemId);
			if (item == null)
			{
				log.warn("Item ID {} not found in cache", itemId);
				skipped++;
				continue;
			}

			try
			{
				BufferedImage sprite = ItemSpriteFactory.createSprite(
					itemManager,
					modelProvider,
					spriteManager,
					textureManager,
					item.id,
					1, // quantity
					1, // border
					0, // shadowColor
					false // noted
				);

				if (sprite != null)
				{
					File outputFile = new File(itemsDir, item.id + ".png");
					ImageIO.write(sprite, "png", outputFile);
					log.info("Exported item {}: {}", item.id, item.name);
					exported++;
				}
				else
				{
					log.warn("Could not create sprite for item {}: {}", item.id, item.name);
					skipped++;
				}
			}
			catch (Exception e)
			{
				log.warn("Failed to export item {}: {}", item.id, e.getMessage());
				skipped++;
			}
		}

		log.info("Exported {} specific item sprites, skipped {}", exported, skipped);
	}

	/**
	 * Export only specific object IDs as PNG files.
	 * Renders 3D object models to 2D sprites.
	 */
	public void exportSpecificObjects(File outputDir, int[] objectIds) throws IOException
	{
		log.info("Exporting {} specific object sprites to {}", objectIds.length, outputDir);
		outputDir.mkdirs();

		File objectsDir = new File(outputDir, "objects");
		objectsDir.mkdirs();

		int exported = 0;
		int skipped = 0;
		int notFound = 0;

		for (int objectId : objectIds)
		{
			log.info("Processing object ID: {}", objectId);
			ObjectDefinition object = objectManager.getObject(objectId);
			if (object == null)
			{
				log.warn("Object ID {} not found in cache", objectId);
				notFound++;
				skipped++;
				continue;
			}

			log.info("Found object {}: {} with {} models", object.getId(), object.getName(),
				object.getObjectModels() != null ? object.getObjectModels().length : 0);

			try
			{
				BufferedImage sprite = null;

				// Prefer the official map scene sprite if available
				int mapSceneId = object.getMapSceneID();
				if (mapSceneId >= 0 && mapSceneId < mapSceneSprites.length && mapSceneSprites[mapSceneId] != null)
				{
					SpriteDefinition mapSceneSprite = mapSceneSprites[mapSceneId];
					if (mapSceneSprite.getWidth() > 0 && mapSceneSprite.getHeight() > 0)
					{
						sprite = new BufferedImage(mapSceneSprite.getWidth(), mapSceneSprite.getHeight(), BufferedImage.TYPE_INT_ARGB);
						sprite.setRGB(0, 0, mapSceneSprite.getWidth(), mapSceneSprite.getHeight(), mapSceneSprite.getPixels(), 0, mapSceneSprite.getWidth());
						log.info("Using map scene sprite {} for object {}: {}", mapSceneId, object.getId(), object.getName());
					}
				}
				else
				{
					// Fall back to 3D model rendering for objects without map scene sprites
					sprite = ObjectSpriteFactory.createSprite(
						modelProvider,
						spriteManager,
						textureManager,
						object
					);
				}

				if (sprite != null)
				{
					File outputFile = new File(objectsDir, object.getId() + ".png");
					ImageIO.write(sprite, "png", outputFile);
					log.info("Exported object {}: {}", object.getId(), object.getName());
					exported++;
				}
				else
				{
					log.warn("Could not create sprite for object {}: {}", object.getId(), object.getName());
					skipped++;
				}
			}
			catch (Exception e)
			{
				log.warn("Failed to export object {}: {}", object.getId(), e.getMessage());
				e.printStackTrace();
				skipped++;
			}
		}

		log.info("Exported {} specific object sprites, skipped {} ({} not found)", exported, skipped, notFound);
	}

	/**
	 * Export item sprites as PNG files.
	 * Each item is exported as items/{itemId}.png
	 */
	private void exportItems(File itemsDir) throws IOException
	{
		log.info("Exporting item sprites...");
		itemsDir.mkdirs();

		Collection<ItemDefinition> items = itemManager.getItems();
		int exported = 0;
		int skipped = 0;

		for (ItemDefinition item : items)
		{
			try
			{
				BufferedImage sprite = ItemSpriteFactory.createSprite(
					itemManager,
					modelProvider,
					spriteManager,
					textureManager,
					item.id,
					1, // quantity
					1, // border
					0, // shadowColor
					false // noted
				);

				if (sprite != null)
				{
					File outputFile = new File(itemsDir, item.id + ".png");
					ImageIO.write(sprite, "png", outputFile);

					metadata.put("item:" + item.id, new AssetMetadata(
						"item",
						item.id,
						item.name,
						"items/" + item.id + ".png",
						sprite.getWidth(),
						sprite.getHeight()
					));

					exported++;
				}
				else
				{
					skipped++;
				}
			}
			catch (Exception e)
			{
				log.warn("Failed to export item {}: {}", item.id, e.getMessage());
				skipped++;
			}
		}

		log.info("Exported {} item sprites, skipped {}", exported, skipped);
	}

	/**
	 * Export game sprites as PNG files.
	 * Each sprite frame is exported as sprites/{spriteId}-{frame}.png
	 */
	private void exportSprites(File spritesDir) throws IOException
	{
		log.info("Exporting game sprites...");
		spritesDir.mkdirs();

		Collection<SpriteDefinition> sprites = spriteManager.getSprites();
		int exported = 0;
		int skipped = 0;

		for (SpriteDefinition sprite : sprites)
		{
			// Skip sprites with invalid dimensions
			if (sprite.getHeight() <= 0 || sprite.getWidth() <= 0)
			{
				skipped++;
				continue;
			}

			try
			{
				BufferedImage image = spriteManager.getSpriteImage(sprite);
				File outputFile = new File(spritesDir, sprite.getId() + "-" + sprite.getFrame() + ".png");
				ImageIO.write(image, "png", outputFile);

				metadata.put("sprite:" + sprite.getId() + ":" + sprite.getFrame(), new AssetMetadata(
					"sprite",
					sprite.getId(),
					"sprite_" + sprite.getId() + "_frame_" + sprite.getFrame(),
					"sprites/" + sprite.getId() + "-" + sprite.getFrame() + ".png",
					sprite.getWidth(),
					sprite.getHeight()
				));

				exported++;
			}
			catch (Exception e)
			{
				log.warn("Failed to export sprite {}-{}: {}", sprite.getId(), sprite.getFrame(), e.getMessage());
				skipped++;
			}
		}

		log.info("Exported {} sprite frames, skipped {}", exported, skipped);
	}

	/**
	 * Export asset metadata as JSON.
	 */
	private void exportMetadata(File metadataFile) throws IOException
	{
		log.info("Exporting asset metadata...");
		// For now, just log the count. In a full implementation, this would
		// serialize the metadata map to JSON using a JSON library.
		log.info("Total assets in metadata: {}", metadata.size());
		log.info("Metadata would be written to: {}", metadataFile);
	}

	/**
	 * Metadata for a single asset.
	 */
	private static class AssetMetadata
	{
		String type;
		int id;
		String name;
		String path;
		int width;
		int height;

		AssetMetadata(String type, int id, String name, String path, int width, int height)
		{
			this.type = type;
			this.id = id;
			this.name = name;
			this.path = path;
			this.width = width;
			this.height = height;
		}
	}
}
