# Asset Library Exporter

Exports official RuneScape game assets (items, sprites) as organized PNG files for frontend consumption.

## Overview

The `AssetLibraryExporter` leverages RuneLite's cache infrastructure to extract official game assets without web scraping. This provides a clean, legal way to access game assets for frontend applications.

## Features

- **Item Sprites**: Exports all item sprites as 36x32 PNG files with proper lighting and shadows
- **Game Sprites**: Exports all game sprite frames (UI elements, icons, etc.) as PNG files
- **Skill Icons**: Copies skill icons from RuneLite client resources
- **Organized Structure**: Creates organized directory structure for easy frontend consumption
- **Metadata**: Tracks asset metadata (ID, name, dimensions, path) for programmatic access

## Output Structure

```
tools/asset-library/
├── items/
│   ├── 1.png       # Item sprite for item ID 1
│   ├── 2.png       # Item sprite for item ID 2
│   └── ...
├── sprites/
│   ├── 1-0.png     # Sprite ID 1, frame 0
│   ├── 1-1.png     # Sprite ID 1, frame 1
│   └── ...
├── skill-icons/
│   ├── attack.png
│   ├── strength.png
│   └── ... (26 skill icons)
└── metadata.json   # Asset metadata (future implementation)
```

## Usage

### Quick Export (Test Cache - Fast, Limited)

Exports skill icons and basic sprites using the bundled test cache:

```bash
# From the runelite-cv directory
.\gradlew.bat :cache:test --tests AssetLibraryExporterTest
```

This is fast (~20 seconds) but only exports a limited set of assets from the test cache.

### Full Export (Game Cache - Complete Assets)

For complete asset export including all items and sprites, use your actual game cache:

```bash
# Modify AssetLibraryExporterTest to use your game cache
# Change: File cacheLocation = StoreLocation.LOCATION;
# To: File cacheLocation = new File(System.getProperty("user.home"), ".runelite/cache");

# Then run
.\gradlew.bat :cache:test --tests AssetLibraryExporterTest
```

This will export:
- All item sprites (computationally intensive, may take several minutes)
- All game sprites
- Skill icons

### Programmatic Usage

```java
import net.runelite.cache.AssetLibraryExporter;
import net.runelite.cache.fs.Store;
import java.io.File;

// Open the game cache store
try (Store store = new Store(new File("/path/to/game/cache"))) {
    store.load();
    
    // Create exporter
    AssetLibraryExporter exporter = new AssetLibraryExporter(store);
    
    // Export all assets (items + sprites)
    exporter.exportAll(new File("/path/to/output/directory"));
    
    // OR export only sprites (faster)
    exporter.exportSpritesOnly(new File("/path/to/output/directory"));
}
```

## Requirements

- **For Quick Export**: RuneLite source code (test cache is bundled)
- **For Full Export**: RuneLite game cache at `~/.runelite/cache/` (populated by running RuneLite)
- Java 8+
- Gradle (for running tests)

## Performance Considerations

- **Sprite-only export**: Fast (~20 seconds), works with test cache
- **Full export (items + sprites)**: Slower (several minutes), requires full game cache
- **Item sprite generation**: Computationally intensive due to 3D model rendering
- **Recommendation**: Run full export once, then serve assets statically to frontend

## Current Status

**Exported to `tools/asset-library/`:**
- ✅ Skill icons (26 PNG files from client resources)
- ⏳ Game sprites (requires full game cache for complete export)
- ⏳ Item sprites (requires full game cache, computationally intensive)

## Implementation Details

### Item Sprite Generation

Item sprites are generated using `ItemSpriteFactory` which:
1. Loads the 3D model from the game cache
2. Applies item-specific transformations (resize, recolor, retexture)
3. Renders the model with proper lighting and shadows
4. Outputs a 36x32 PNG with transparent background

### Sprite Export

Game sprites are extracted directly from the cache using `SpriteManager`:
1. Loads sprite definitions from the cache
2. Converts sprite pixel data to BufferedImage
3. Exports each frame as a separate PNG file

### Skill Icons

Skill icons are copied from RuneLite client resources:
- Located at `runelite-client/src/main/resources/skill_icons/`
- Already in PNG format, no conversion needed
- 26 skill icons included

## Frontend Integration

### Static File Serving

The exported assets can be served as static files:

```javascript
// Example: Fetch item sprite
const itemSpriteUrl = `/assets/items/${itemId}.png`;
const img = document.createElement('img');
img.src = itemSpriteUrl;

// Example: Fetch skill icon
const skillIconUrl = `/assets/skill-icons/${skillName}.png`;
```

### HTTP Endpoint (Future)

A future enhancement could add an HTTP endpoint to the CV Helper plugin:

```
GET /assets/items/{itemId}.png
GET /assets/sprites/{spriteId}/{frame}.png
GET /assets/skill-icons/{skillName}.png
GET /assets/metadata.json
```

## Legal Considerations

- These assets are extracted from the official RuneScape game cache
- Use of these assets should comply with Jagex's terms of service
- This tool is for educational and development purposes
- Always verify asset usage rights before using in production applications

## Limitations

- Requires access to the game cache (must have RuneLite installed and cache populated)
- Item sprite generation is computationally intensive (may take several minutes for all items)
- Some items may fail to export if their models are missing or corrupted
- Sprite export includes all frames, including some that may not be useful
- Test cache has limited assets; full export requires actual game cache

## Future Enhancements

- [ ] JSON metadata export with asset names, categories, and relationships
- [ ] HTTP endpoint integration with CV Helper plugin
- [ ] Incremental export (only export changed assets)
- [ ] Asset optimization (compression, format conversion)
- [ ] Asset categorization and tagging
- [ ] Frontend viewer/browser for exported assets
