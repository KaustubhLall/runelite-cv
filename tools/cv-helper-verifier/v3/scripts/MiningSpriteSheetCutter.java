import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuts the mining rock sprite sheet into individual preset icons.
 * Source: asset-library/mining-sprites-source.png
 * Output: asset-library/mining-presets/*.png
 */
public class MiningSpriteSheetCutter
{
	private static final int COLS = 8;
	private static final int ROWS = 8;
	private static final int LABEL_HEIGHT = 20; // approximate label area at bottom of each cell
	private static final int TRANSPARENT_COLOR = 0xFF1A1A1A; // dark background color to make transparent

	public static void main(String[] args) throws IOException
	{
		File projectRoot = new File("c:\\Users\\kaust\\IdeaProjects\\runelite-cv\\tools\\cv-helper-verifier\\v3");
		File sourceFile = new File(projectRoot, "asset-library/mining-sprites-source.png");
		File outputDir = new File(projectRoot, "asset-library/mining-presets");
		outputDir.mkdirs();

		if (!sourceFile.exists())
		{
			System.err.println("Source sprite sheet not found: " + sourceFile.getAbsolutePath());
			System.exit(1);
		}

		BufferedImage sheet = ImageIO.read(sourceFile);
		int cellWidth = sheet.getWidth() / COLS;
		int cellHeight = sheet.getHeight() / ROWS;
		int spriteHeight = cellHeight - LABEL_HEIGHT;

		System.out.println("Sheet: " + sheet.getWidth() + "x" + sheet.getHeight());
		System.out.println("Cell: " + cellWidth + "x" + cellHeight + " Sprite height: " + spriteHeight);

		String[] names = {
			"Rockfall", "Rune essence", "Specimen tray", "Saltpetre", "Rock (Abyss)", "Guardian parts", "Guardian remains", "Large guardian remains",
			"Huge guardian remains", "Fallen guardian", "Rock Formation", "Copper rocks", "Tin rocks", "Clay rocks", "Pile of Rock", "Tin ore vein",
			"Copper ore vein", "Clay ore vein", "Rocks (The Tourist Trap)", "Easter egg rock", "Structural pillar", "Blurite rocks", "Limestone rock", "Pile of Rock (Big Cats & WWF)",
			"Barronite rocks", "Iron rocks", "Iron ore vein", "Daeyalt rocks", "Silver rocks", "Ash pile", "Ore vein", "Coal rocks",
			"Coal ore vein", "Sandstone rocks", "Dense runestone", "Gem rocks", "Gold rocks", "Gold vein", "Calcified rocks", "Volcanic sulphur",
			"Granite rocks", "Boulder (Volcanic Mine)", "Boulder (Shaman Caves)", "Mithril rocks", "Mithril ore vein", "Daeyalt Essence", "Stalagmites (Lunar Isle)", "Amalgamation",
			"Lovakite rocks", "Adamantite rocks", "Soft clay rocks", "Adamant ore vein", "Urt salt rocks", "Efh salt rocks", "Te salt rocks", "Basalt rocks",
			"Salt Deposit", "Ancient essence crystals", "Infernal shale deposit", "Infernal shale rocks", "Runite rocks", "Amethyst crystals", "Pickaxe", "Pickaxe alt"
		};

		Map<String, Integer> presetToIndex = new LinkedHashMap<>();
		presetToIndex.put("Clay", 13);
		presetToIndex.put("Copper/tin", 11);
		presetToIndex.put("Blurite", 21);
		presetToIndex.put("Iron", 25);
		presetToIndex.put("Silver", 28);
		presetToIndex.put("Coal", 31);
		presetToIndex.put("Gold", 36);
		presetToIndex.put("Mithril", 43);
		presetToIndex.put("Adamantite", 49);
		presetToIndex.put("Runite", 60);
		presetToIndex.put("Amethyst", 61);
		presetToIndex.put("Gem rocks", 35);
		presetToIndex.put("Granite", 40);
		presetToIndex.put("Sandstone", 33);
		presetToIndex.put("Lovakite", 48);
		presetToIndex.put("Daeyalt", 27);
		presetToIndex.put("Limestone", 22);
		presetToIndex.put("Volcanic sulphur", 39);
		presetToIndex.put("Rune essence", 1);
		presetToIndex.put("Pure essence", 1);
		presetToIndex.put("Ancient essence", 57);
		presetToIndex.put("Custom", 62);

		int exported = 0;
		for (int row = 0; row < ROWS; row++)
		{
			for (int col = 0; col < COLS; col++)
			{
				int index = row * COLS + col;
				int x = col * cellWidth;
				int y = row * cellHeight;
				BufferedImage sprite = sheet.getSubimage(x, y, cellWidth, spriteHeight);

				// Make background transparent
				BufferedImage transparentSprite = makeTransparent(sprite, TRANSPARENT_COLOR);

				String safeName = names[index].replaceAll("[^a-zA-Z0-9 ]", "").replace(" ", "-").toLowerCase();
				File outFile = new File(outputDir, "mining-rock-" + (index + 1) + "-" + safeName + ".png");
				ImageIO.write(transparentSprite, "png", outFile);
				exported++;
				System.out.println("Exported " + (index + 1) + ": " + outFile.getName());
			}
		}

		for (Map.Entry<String, Integer> entry : presetToIndex.entrySet())
		{
			int index = entry.getValue();
			String safeName = names[index].replaceAll("[^a-zA-Z0-9 ]", "").replace(" ", "-").toLowerCase();
			File source = new File(outputDir, "mining-rock-" + (index + 1) + "-" + safeName + ".png");
			File presetFile = new File(outputDir, "mining-preset-" + entry.getKey().toLowerCase().replace("/", "-") + ".png");
			copyFile(source, presetFile);
			System.out.println("Preset icon: " + presetFile.getName());
		}

		System.out.println("Done. Exported " + exported + " sprites plus " + presetToIndex.size() + " preset icons to " + outputDir.getAbsolutePath());
	}

	private static void copyFile(File source, File dest) throws IOException
	{
		BufferedImage img = ImageIO.read(source);
		ImageIO.write(img, "png", dest);
	}

	private static BufferedImage makeTransparent(BufferedImage image, int transparentColor)
	{
		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int rgb = image.getRGB(x, y);
				if (rgb == transparentColor)
				{
					result.setRGB(x, y, 0x00000000); // fully transparent
				}
				else
				{
					result.setRGB(x, y, rgb);
				}
			}
		}
		return result;
	}
}
