/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bounded collision-aware pathfinding / reachability (BFS over WorldArea.canTravelInDirection).
 * Verbatim port of the reference path-distance helpers; shared by the mob and skill farmers.
 */
@Singleton
public class PathfindingService
{
	@Inject
	private Client client;

	public CvHelperModData.PathingResult pathDistanceToWorldArea(Player localPlayer, WorldArea target, int maxDistance)
	{
		if (localPlayer == null || target == null || localPlayer.getWorldArea() == null)
		{
			return CvHelperModData.PathingResult.unreachable("missing-area", 0, 0);
		}
		WorldArea start = localPlayer.getWorldArea();
		if (start.getPlane() != target.getPlane())
		{
			return CvHelperModData.PathingResult.unreachable("different-plane", 0, 0);
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		int straightDistance = start.distanceTo(target);
		int searchLimit = Math.max(1, Math.min(CvHelperModData.MOB_FARMER_PATHING_MAX_SEARCH_TILES, (maxDistance > 0 ? maxDistance : straightDistance) + CvHelperModData.MOB_FARMER_PATHING_SLACK_TILES));
		ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
		Map<WorldPoint, Integer> distances = new HashMap<>();
		WorldPoint startPoint = start.toWorldPoint();
		queue.add(startPoint);
		distances.put(startPoint, 0);
		int visited = 0;
		int blockedByCollision = 0;
		int blockedByScene = 0;
		while (!queue.isEmpty())
		{
			WorldPoint point = queue.remove();
			int pathDistance = distances.get(point);
			visited++;
			WorldArea area = new WorldArea(point, start.getWidth(), start.getHeight());
			if (canReachMelee(worldView, area, target))
			{
				return CvHelperModData.PathingResult.reachable(pathDistance, searchLimit, visited);
			}
			if (pathDistance >= searchLimit)
			{
				continue;
			}
			for (int[] direction : CvHelperModData.MOB_FARMER_PATH_DIRECTIONS)
			{
				if (!canTravelSafely(worldView, area, direction[0], direction[1]))
				{
					blockedByCollision++;
					continue;
				}
				WorldPoint next = new WorldPoint(point.getX() + direction[0], point.getY() + direction[1], point.getPlane());
				if (distances.containsKey(next))
				{
					continue;
				}
				if (LocalPoint.fromWorld(worldView, next) == null)
				{
					blockedByScene++;
					continue;
				}
				distances.put(next, pathDistance + 1);
				queue.add(next);
			}
		}
		String failureReason = "no-route-within:" + searchLimit;
		if (blockedByScene > 0)
		{
			failureReason += ",scene-blocked:" + blockedByScene;
		}
		if (blockedByCollision > 0)
		{
			failureReason += ",collision-blocked:" + blockedByCollision;
		}
		return CvHelperModData.PathingResult.unreachable(failureReason, searchLimit, visited);
	}

	public boolean isInsideFootprint(WorldArea footprint, WorldPoint point)
	{
		return point.getPlane() == footprint.getPlane()
			&& point.getX() >= footprint.getX()
			&& point.getX() < footprint.getX() + footprint.getWidth()
			&& point.getY() >= footprint.getY()
			&& point.getY() < footprint.getY() + footprint.getHeight();
	}

	public boolean canStandOnTile(WorldView worldView, WorldPoint point)
	{
		if (worldView == null)
		{
			return false;
		}
		WorldArea area = new WorldArea(point, 1, 1);
		for (int[] direction : CvHelperModData.MOB_FARMER_PATH_DIRECTIONS)
		{
			if (canTravelSafely(worldView, area, direction[0], direction[1]))
			{
				return true;
			}
		}
		return false;
	}

	public CvHelperModData.InteractionPathingResult pathDistanceToInteractionArea(Player localPlayer, WorldArea footprint, int maxDistance)
	{
		if (localPlayer == null || footprint == null || localPlayer.getWorldArea() == null)
		{
			return CvHelperModData.InteractionPathingResult.unreachable("missing-area", 0, 0, footprint,
				0, 0, 0, 0, 0);
		}
		WorldArea start = localPlayer.getWorldArea();
		if (start.getPlane() != footprint.getPlane())
		{
			return CvHelperModData.InteractionPathingResult.unreachable("different-plane", 0, 0, footprint,
				0, 0, 0, 0, 0);
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null)
		{
			return CvHelperModData.InteractionPathingResult.unreachable("world-view-unavailable", 0, 0, footprint,
				0, 0, 0, 0, 0);
		}

		WorldPoint startPoint = start.toWorldPoint();
		if (LocalPoint.fromWorld(worldView, startPoint) == null)
		{
			return CvHelperModData.InteractionPathingResult.unreachable("start-outside-scene", 0, 0, footprint,
				0, 0, 0, 0, 0);
		}

		int straightDistance = start.distanceTo(footprint);
		int searchLimit = Math.max(1, Math.min(CvHelperModData.MOB_FARMER_PATHING_MAX_SEARCH_TILES,
			(maxDistance > 0 ? maxDistance : straightDistance) + CvHelperModData.MOB_FARMER_PATHING_SLACK_TILES));

		int minX = footprint.getX() - 1;
		int maxX = footprint.getX() + footprint.getWidth();
		int minY = footprint.getY() - 1;
		int maxY = footprint.getY() + footprint.getHeight();

		int evaluatedInteractionTiles = 0;
		int blockedByScene = 0;
		int blockedByCollision = 0;
		int walkableInteractionTiles = 0;
		Set<WorldPoint> interactionCandidates = new HashSet<>();

		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; y <= maxY; y++)
			{
				WorldPoint point = new WorldPoint(x, y, footprint.getPlane());
				if (isInsideFootprint(footprint, point))
				{
					continue;
				}
				evaluatedInteractionTiles++;
				if (LocalPoint.fromWorld(worldView, point) == null)
				{
					blockedByScene++;
					continue;
				}
				if (!canStandOnTile(worldView, point))
				{
					blockedByCollision++;
					continue;
				}
				walkableInteractionTiles++;
				interactionCandidates.add(point);
			}
		}

		if (interactionCandidates.isEmpty())
		{
			String failureReason = "matched-but-no-interaction-tile";
			if (blockedByScene > 0)
			{
				failureReason += ",scene-blocked:" + blockedByScene;
			}
			if (blockedByCollision > 0)
			{
				failureReason += ",collision-blocked:" + blockedByCollision;
			}
			return CvHelperModData.InteractionPathingResult.unreachable(failureReason, searchLimit, 0,
				footprint, evaluatedInteractionTiles, walkableInteractionTiles,
				blockedByCollision + blockedByScene, blockedByCollision, blockedByScene);
		}

		ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
		Map<WorldPoint, Integer> distances = new HashMap<>();
		queue.add(startPoint);
		distances.put(startPoint, 0);
		int visited = 0;
		int pathBlockedByCollision = 0;
		int pathBlockedByScene = 0;
		WorldPoint reachedTile = null;
		int reachedDistance = Integer.MAX_VALUE;

		while (!queue.isEmpty())
		{
			WorldPoint point = queue.remove();
			int pathDistance = distances.get(point);
			visited++;

			if (interactionCandidates.contains(point) && pathDistance < reachedDistance)
			{
				reachedTile = point;
				reachedDistance = pathDistance;
				break;
			}

			if (pathDistance >= searchLimit)
			{
				continue;
			}

			WorldArea area = new WorldArea(point, 1, 1);
			for (int[] direction : CvHelperModData.MOB_FARMER_PATH_DIRECTIONS)
			{
				if (!canTravelSafely(worldView, area, direction[0], direction[1]))
				{
					pathBlockedByCollision++;
					continue;
				}
				WorldPoint next = new WorldPoint(point.getX() + direction[0], point.getY() + direction[1], point.getPlane());
				if (distances.containsKey(next))
				{
					continue;
				}
				if (LocalPoint.fromWorld(worldView, next) == null)
				{
					pathBlockedByScene++;
					continue;
				}
				distances.put(next, pathDistance + 1);
				queue.add(next);
			}
		}

		if (reachedTile != null)
		{
			return CvHelperModData.InteractionPathingResult.reachable(reachedDistance, searchLimit, visited,
				reachedTile, footprint, evaluatedInteractionTiles, walkableInteractionTiles,
				blockedByCollision + blockedByScene + (walkableInteractionTiles - 1),
				blockedByCollision, blockedByScene);
		}

		String failureReason = "matched-but-no-route-to-interaction-tile";
		if (pathBlockedByScene > 0)
		{
			failureReason += ",scene-blocked:" + pathBlockedByScene;
		}
		if (pathBlockedByCollision > 0)
		{
			failureReason += ",collision-blocked:" + pathBlockedByCollision;
		}
		return CvHelperModData.InteractionPathingResult.unreachable(failureReason, searchLimit, visited,
			footprint, evaluatedInteractionTiles, walkableInteractionTiles,
			blockedByCollision + blockedByScene, blockedByCollision, blockedByScene);
	}

	public CvHelperModData.PathingResult mobFarmerPathDistanceToMelee(Player localPlayer, NPC npc, int maxDistance)
	{
		if (localPlayer == null || npc == null)
		{
			return CvHelperModData.PathingResult.unreachable("missing-actor", 0, 0);
		}
		WorldArea start = localPlayer.getWorldArea();
		WorldArea target = npc.getWorldArea();
		if (start == null || target == null)
		{
			return CvHelperModData.PathingResult.unreachable("missing-world-area", 0, 0);
		}
		if (start.getPlane() != target.getPlane())
		{
			return CvHelperModData.PathingResult.unreachable("different-plane", 0, 0);
		}

		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null || worldView.getCollisionMaps() == null || start.getPlane() < 0 || start.getPlane() >= worldView.getCollisionMaps().length || worldView.getCollisionMaps()[start.getPlane()] == null)
		{
			return CvHelperModData.PathingResult.unreachable("collision-map-unavailable", 0, 0);
		}

		int straightDistance = start.distanceTo(target);
		int baseLimit = maxDistance > 0 ? maxDistance : straightDistance + CvHelperModData.MOB_FARMER_PATHING_SLACK_TILES;
		int searchLimit = Math.max(1, Math.min(CvHelperModData.MOB_FARMER_PATHING_MAX_SEARCH_TILES, baseLimit + CvHelperModData.MOB_FARMER_PATHING_SLACK_TILES));
		WorldPoint startPoint = start.toWorldPoint();
		if (LocalPoint.fromWorld(worldView, startPoint) == null)
		{
			return CvHelperModData.PathingResult.unreachable("start-outside-scene", searchLimit, 0);
		}

		ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
		Map<WorldPoint, Integer> distances = new HashMap<>();
		queue.add(startPoint);
		distances.put(startPoint, 0);
		int visited = 0;
		while (!queue.isEmpty())
		{
			WorldPoint point = queue.remove();
			Integer pathDistance = distances.get(point);
			if (pathDistance == null)
			{
				continue;
			}
			visited++;
			WorldArea area = new WorldArea(point, start.getWidth(), start.getHeight());
			if (canReachMelee(worldView, area, target))
			{
				return CvHelperModData.PathingResult.reachable(pathDistance, searchLimit, visited);
			}
			if (pathDistance >= searchLimit)
			{
				continue;
			}

			for (int[] direction : CvHelperModData.MOB_FARMER_PATH_DIRECTIONS)
			{
				int dx = direction[0];
				int dy = direction[1];
				if (!canTravelSafely(worldView, area, dx, dy))
				{
					continue;
				}
				WorldPoint next = new WorldPoint(point.getX() + dx, point.getY() + dy, point.getPlane());
				if (distances.containsKey(next) || LocalPoint.fromWorld(worldView, next) == null)
				{
					continue;
				}
				distances.put(next, pathDistance + 1);
				queue.add(next);
			}
		}

		return CvHelperModData.PathingResult.unreachable("no-route-within:" + searchLimit, searchLimit, visited);
	}

	public boolean canReachMelee(WorldView worldView, WorldArea from, WorldArea target)
	{
		if (from.intersectsWith(target))
		{
			return true;
		}
		if (!from.isInMeleeDistance(target))
		{
			return false;
		}
		int dx = directionToRange(from.getX(), from.getX() + from.getWidth() - 1, target.getX(), target.getX() + target.getWidth() - 1);
		int dy = directionToRange(from.getY(), from.getY() + from.getHeight() - 1, target.getY(), target.getY() + target.getHeight() - 1);
		return canTravelSafely(worldView, from, dx, dy);
	}

	public int directionToRange(int fromMin, int fromMax, int targetMin, int targetMax)
	{
		if (targetMin > fromMax)
		{
			return 1;
		}
		if (targetMax < fromMin)
		{
			return -1;
		}
		return 0;
	}

	public boolean canTravelSafely(WorldView worldView, WorldArea area, int dx, int dy)
	{
		try
		{
			return area.canTravelInDirection(worldView, dx, dy);
		}
		catch (RuntimeException e)
		{
			return false;
		}
	}

}
