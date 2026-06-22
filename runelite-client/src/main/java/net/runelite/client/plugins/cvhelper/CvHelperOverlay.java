/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

class CvHelperOverlay extends Overlay
{
	private final Client client;
	private final CvHelperPlugin plugin;

	@Inject
	CvHelperOverlay(Client client, CvHelperPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
		setMovable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean rendered = false;
		if (plugin.getConfig().showHoverOverlay())
		{
			net.runelite.api.Point mouse = client.getMouseCanvasPosition();
			if (mouse != null)
			{
				int x = mouse.getX();
				int y = mouse.getY();
				graphics.setColor(new Color(0, 180, 255));
				graphics.drawLine(x - 8, y, x + 8, y);
				graphics.drawLine(x, y - 8, x, y + 8);

				String label = "mouse@" + x + "," + y;
				if (plugin.getConfig().showWidgetInfo())
				{
					label += " canvas";
				}

				Point textPoint = new Point(x + 12, y - 12);
				graphics.setColor(Color.BLACK);
				graphics.drawString(Text.removeTags(label), textPoint.x + 1, textPoint.y + 1);
				graphics.setColor(Color.WHITE);
				graphics.drawString(Text.removeTags(label), textPoint.x, textPoint.y);
				rendered = true;
			}
		}

		if (plugin.getConfig().showPrayerTargets())
		{
			List<Map<String, Object>> prayerTargets = plugin.getLivePrayerTargets();
			graphics.setColor(new Color(72, 219, 116));
			for (Map<String, Object> target : prayerTargets)
			{
				drawTarget(graphics, target, new Color(72, 219, 116));
				rendered = true;
			}
		}

		if (plugin.getConfig().showSpellTargets())
		{
			List<Map<String, Object>> spellTargets = plugin.getLiveSpellTargets();
			graphics.setColor(new Color(255, 184, 77));
			for (Map<String, Object> target : spellTargets)
			{
				drawTarget(graphics, target, new Color(255, 184, 77));
				rendered = true;
			}
		}

		if (plugin.getConfig().showMinimapTargets())
		{
			for (Map<String, Object> target : plugin.getLiveMinimapTargets())
			{
				drawTarget(graphics, target, new Color(80, 220, 255));
				rendered = true;
			}
		}

		if (plugin.getConfig().showInventoryTargets())
		{
			for (Map<String, Object> target : plugin.getLiveInventoryTargets())
			{
				drawTarget(graphics, target, new Color(255, 214, 92));
				rendered = true;
			}
		}

		if (plugin.getConfig().showEquipmentTargets())
		{
			for (Map<String, Object> target : plugin.getLiveEquipmentTargets())
			{
				drawTarget(graphics, target, new Color(196, 140, 255));
				rendered = true;
			}
		}

		if (plugin.getConfig().showPanelTargets())
		{
			for (Map<String, Object> target : plugin.getLivePanelTargets())
			{
				drawTarget(graphics, target, new Color(255, 105, 180));
				rendered = true;
			}
		}

		if (plugin.getConfig().showCombatTargets())
		{
			for (Map<String, Object> target : plugin.getLiveCombatTargets())
			{
				drawTarget(graphics, target, new Color(255, 80, 80));
				rendered = true;
			}
		}

		if (plugin.getConfig().showEntityTargets())
		{
			for (Map<String, Object> entity : plugin.getLiveEntities())
			{
				drawEntity(graphics, entity, new Color(64, 160, 255));
				rendered = true;
			}
		}

		if (plugin.getConfig().showSkillFarmerTargets())
		{
			for (Map<String, Object> target : plugin.getLiveSkillFarmerTargets())
			{
				drawTarget(graphics, target, new Color(72, 219, 116));
				rendered = true;
			}
		}

		return rendered ? new Dimension(1, 1) : null;
	}

	private void drawEntity(Graphics2D graphics, Map<String, Object> entity, Color color)
	{
		Rectangle bounds = rectangleFromMap(entity.get("canvasBounds"));
		if (bounds == null)
		{
			return;
		}
		graphics.setColor(color);
		graphics.draw(bounds);
		if (!plugin.getConfig().showTargetLabels())
		{
			return;
		}
		String label = String.valueOf(entity.get("name"));
		if (label == null || label.isEmpty() || "null".equals(label))
		{
			return;
		}
		int tx = bounds.x + 2;
		int ty = Math.max(12, bounds.y - 3);
		graphics.setColor(Color.BLACK);
		graphics.drawString(Text.removeTags(label), tx + 1, ty + 1);
		graphics.setColor(Color.WHITE);
		graphics.drawString(Text.removeTags(label), tx, ty);
	}

	private void drawTarget(Graphics2D graphics, Map<String, Object> target, Color color)
	{
		Rectangle bounds = boundsFromTarget(target);
		if (bounds == null)
		{
			return;
		}
		graphics.setColor(color);
		graphics.draw(bounds);
		if (!plugin.getConfig().showTargetLabels())
		{
			return;
		}
		String label = String.valueOf(target.get("label"));
		String surface = String.valueOf(target.get("surface"));
		if (!shouldDrawLabel(surface, label))
		{
			return;
		}
		int tx = bounds.x + 2;
		int ty = Math.max(12, bounds.y - 3);
		graphics.setColor(Color.BLACK);
		graphics.drawString(Text.removeTags(label), tx + 1, ty + 1);
		graphics.setColor(Color.WHITE);
		graphics.drawString(Text.removeTags(label), tx, ty);
	}

	private boolean shouldDrawLabel(String surface, String label)
	{
		if (label == null || label.isEmpty() || "null".equals(label))
		{
			return false;
		}
		if ("inventory".equals(surface) || "equipment".equals(surface) || "panels".equals(surface))
		{
			return false;
		}
		return true;
	}

	private Rectangle boundsFromTarget(Map<String, Object> target)
	{
		return rectangleFromMap(target.get("bounds"));
	}

	private Rectangle rectangleFromMap(Object value)
	{
		if (!(value instanceof Map))
		{
			return null;
		}
		Map<?, ?> bounds = (Map<?, ?>) value;
		Number x = (Number) bounds.get("x");
		Number y = (Number) bounds.get("y");
		Number width = (Number) bounds.get("width");
		Number height = (Number) bounds.get("height");
		if (x == null || y == null || width == null || height == null)
		{
			return null;
		}
		return new Rectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue());
	}
}
