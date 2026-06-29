import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuts the woodcutting tree sprite sheet into individual preset icons.
 * Source: asset-library/woodcutting-sprites-source.png
 * Output: asset-library/woodcutting-presets/*.png
 */
public class WoodcuttingSpriteSheetCutter
{
	private static final int COLS = 8;
	private static final int ROWS = 6;
	private static final int LABEL_HEIGHT = 20; // approximate label area at bottom of each cell
	private static final int TRANSPARENT_COLOR = 0xFF1A1A1A; // dark background color to make transparent

	public static void main(String[] args) throws IOException
	{
		File projectRoot = new File("c:\\Users\\kaust\\IdeaProjects\\runelite-cv\\tools\\cv-helper-verifier\\v3");
		File sourceFile = new File(projectRoot, "asset-library/woodcutting-sprites-source.png");
		File outputDir = new File(projectRoot, "asset-library/woodcutting-presets");
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
			"Tree", "Oak", "Willow", "Maple", "Yew", "Magic", "Redwood", "Tree (Draynor)",
			"Tree (Lumbridge)", "Tree (Varrock)", "Tree (Falador)", "Tree (Edgeville)", "Tree (Catherby)", "Tree (Seers)", "Tree (Ardougne)", "Tree (Yanille)",
			"Oak (Draynor)", "Oak (Lumbridge)", "Oak (Varrock)", "Oak (Falador)", "Oak (Edgeville)", "Oak (Catherby)", "Oak (Seers)", "Oak (Ardougne)",
			"Willow (Draynor)", "Willow (Lumbridge)", "Willow (Varrock)", "Willow (Falador)", "Willow (Edgeville)", "Willow (Catherby)", "Willow (Seers)", "Willow (Ardougne)",
			"Maple (Seers)", "Maple (Ardougne)", "Maple (Yanille)", "Yew (Seers)", "Yew (Ardougne)", "Yew (Yanille)", "Magic (Seers)", "Magic (Ardougne)",
			"Redwood (Seers)", "Redwood (Ardougne)", "Redwood (Yanille)", "Dead tree", "Dying tree", "Evergreen", "Jungle tree", "Teak"
		};

		Map<String, Integer> presetToIndex = new LinkedHashMap<>();
		presetToIndex.put("Tree", 0);
		presetToIndex.put("Oak", 1);
		presetToIndex.put("Willow", 2);
		presetToIndex.put("Maple", 3);
		presetToIndex.put("Yew", 4);
		presetToIndex.put("Magic", 5);
		presetToIndex.put("Redwood", 6);
		presetToIndex.put("Custom", 7);

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
				File outFile = new File(outputDir, "woodcutting-tree-" + (index + 1) + "-" + safeName + ".png");
				ImageIO.write(transparentSprite, "png", outFile);
				exported++;
				System.out.println("Exported " + (index + 1) + ": " + outFile.getName());
			}
		}

		for (Map.Entry<String, Integer> entry : presetToIndex.entrySet())
		{
			int index = entry.getValue();
			String safeName = names[index].replaceAll("[^a-zA-Z0-9 ]", "").replace(" ", "-").toLowerCase();
			File source = new File(outputDir, "woodcutting-tree-" + (index + 1) + "-" + safeName + ".png");
			File presetFile = new File(outputDir, "woodcutting-preset-" + entry.getKey().toLowerCase().replace("/", "-") + ".png");
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
