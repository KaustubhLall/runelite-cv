/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

	public CvHelperModPlugin.PathingResult pathDistanceToWorldArea(Player localPlayer, WorldArea target, int maxDistance)
	{
		if (localPlayer == null || target == null || localPlayer.getWorldArea() == null)
		{
			return CvHelperModPlugin.PathingResult.unreachable("missing-area", 0, 0);
		}
		WorldArea start = localPlayer.getWorldArea();
		if (start.getPlane() != target.getPlane())
		{
			return CvHelperModPlugin.PathingResult.unreachable("different-plane", 0, 0);
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		int straightDistance = start.distanceTo(target);
		int searchLimit = Math.max(1, Math.min(CvHelperModPlugin.MOB_FARMER_PATHING_MAX_SEARCH_TILES, (maxDistance > 0 ? maxDistance : straightDistance) + CvHelperModPlugin.MOB_FARMER_PATHING_SLACK_TILES));
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
				return CvHelperModPlugin.PathingResult.reachable(pathDistance, searchLimit, visited);
			}
			if (pathDistance >= searchLimit)
			{
				continue;
			}
			for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
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
		return CvHelperModPlugin.PathingResult.unreachable(failureReason, searchLimit, visited);
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
		for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
		{
			if (canTravelSafely(worldView, area, direction[0], direction[1]))
			{
				return true;
			}
		}
		return false;
	}

	public CvHelperModPlugin.InteractionPathingResult pathDistanceToInteractionArea(Player localPlayer, WorldArea footprint, int maxDistance)
	{
		if (localPlayer == null || footprint == null || localPlayer.getWorldArea() == null)
		{
			return CvHelperModPlugin.InteractionPathingResult.unreachable("missing-area", 0, 0, footprint,
				0, 0, 0, 0, 0);
		}
		WorldArea start = localPlayer.getWorldArea();
		if (start.getPlane() != footprint.getPlane())
		{
			return CvHelperModPlugin.InteractionPathingResult.unreachable("different-plane", 0, 0, footprint,
				0, 0, 0, 0, 0);
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null)
		{
			return CvHelperModPlugin.InteractionPathingResult.unreachable("world-view-unavailable", 0, 0, footprint,
				0, 0, 0, 0, 0);
		}

		WorldPoint startPoint = start.toWorldPoint();
		if (LocalPoint.fromWorld(worldView, startPoint) == null)
		{
			return CvHelperModPlugin.InteractionPathingResult.unreachable("start-outside-scene", 0, 0, footprint,
				0, 0, 0, 0, 0);
		}

		int straightDistance = start.distanceTo(footprint);
		int searchLimit = Math.max(1, Math.min(CvHelperModPlugin.MOB_FARMER_PATHING_MAX_SEARCH_TILES,
			(maxDistance > 0 ? maxDistance : straightDistance) + CvHelperModPlugin.MOB_FARMER_PATHING_SLACK_TILES));

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
			return CvHelperModPlugin.InteractionPathingResult.unreachable(failureReason, searchLimit, 0,
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
			for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
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
			return CvHelperModPlugin.InteractionPathingResult.reachable(reachedDistance, searchLimit, visited,
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
		return CvHelperModPlugin.InteractionPathingResult.unreachable(failureReason, searchLimit, visited,
			footprint, evaluatedInteractionTiles, walkableInteractionTiles,
			blockedByCollision + blockedByScene, blockedByCollision, blockedByScene);
	}

	public CvHelperModPlugin.PathingResult mobFarmerPathDistanceToMelee(Player localPlayer, NPC npc, int maxDistance)
	{
		if (localPlayer == null || npc == null)
		{
			return CvHelperModPlugin.PathingResult.unreachable("missing-actor", 0, 0);
		}
		WorldArea start = localPlayer.getWorldArea();
		WorldArea target = npc.getWorldArea();
		if (start == null || target == null)
		{
			return CvHelperModPlugin.PathingResult.unreachable("missing-world-area", 0, 0);
		}
		if (start.getPlane() != target.getPlane())
		{
			return CvHelperModPlugin.PathingResult.unreachable("different-plane", 0, 0);
		}

		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null || worldView.getCollisionMaps() == null || start.getPlane() < 0 || start.getPlane() >= worldView.getCollisionMaps().length || worldView.getCollisionMaps()[start.getPlane()] == null)
		{
			return CvHelperModPlugin.PathingResult.unreachable("collision-map-unavailable", 0, 0);
		}

		int straightDistance = start.distanceTo(target);
		int baseLimit = maxDistance > 0 ? maxDistance : straightDistance + CvHelperModPlugin.MOB_FARMER_PATHING_SLACK_TILES;
		int searchLimit = Math.max(1, Math.min(CvHelperModPlugin.MOB_FARMER_PATHING_MAX_SEARCH_TILES, baseLimit + CvHelperModPlugin.MOB_FARMER_PATHING_SLACK_TILES));
		WorldPoint startPoint = start.toWorldPoint();
		if (LocalPoint.fromWorld(worldView, startPoint) == null)
		{
			return CvHelperModPlugin.PathingResult.unreachable("start-outside-scene", searchLimit, 0);
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
				return CvHelperModPlugin.PathingResult.reachable(pathDistance, searchLimit, visited);
			}
			if (pathDistance >= searchLimit)
			{
				continue;
			}

			for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
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

		return CvHelperModPlugin.PathingResult.unreachable("no-route-within:" + searchLimit, searchLimit, visited);
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
			if (area.canTravelInDirection(worldView, dx, dy))
			{
				return true;
			}
			// The collision map can't distinguish a permanent wall from a closed door/gate -
			// both set the same BLOCK_MOVEMENT flag. The real client opens doors automatically
			// while walking through them, so treat a step blocked only by an openable door as
			// traversable; otherwise our BFS rejects perfectly attackable targets behind doors.
			return isDoorBlockingStep(worldView, area, dx, dy);
		}
		catch (RuntimeException e)
		{
			return false;
		}
	}

	private boolean isDoorBlockingStep(WorldView worldView, WorldArea area, int dx, int dy)
	{
		WorldPoint from = area.toWorldPoint();
		WorldPoint to = new WorldPoint(from.getX() + Integer.signum(dx), from.getY() + Integer.signum(dy), from.getPlane());
		return hasOpenableDoor(worldView, from) || hasOpenableDoor(worldView, to);
	}

	private boolean hasOpenableDoor(WorldView worldView, WorldPoint point)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(worldView, point);
		if (localPoint == null)
		{
			return false;
		}
		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return false;
		}
		Tile[][][] tiles = scene.getTiles();
		int plane = point.getPlane();
		if (tiles == null || plane < 0 || plane >= tiles.length)
		{
			return false;
		}
		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();
		if (sceneX < 0 || sceneX >= tiles[plane].length || sceneY < 0 || sceneY >= tiles[plane][sceneX].length)
		{
			return false;
		}
		Tile tile = tiles[plane][sceneX][sceneY];
		if (tile == null)
		{
			return false;
		}
		WallObject wall = tile.getWallObject();
		if (wall != null && isDoorObject(wall.getId()))
		{
			return true;
		}
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject gameObject : gameObjects)
			{
				if (gameObject != null && isDoorObject(gameObject.getId()))
				{
					return true;
				}
			}
		}
		return false;
	}

	private boolean isDoorObject(int objectId)
	{
		if (objectId < 0)
		{
			return false;
		}
		ObjectComposition composition = client.getObjectDefinition(objectId);
		String[] actions = composition == null ? null : composition.getActions();
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if (action != null && (action.equalsIgnoreCase("Open") || action.equalsIgnoreCase("Close")))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Same BFS as {@link #pathDistanceToWorldArea} but reconstructs the actual route and
	 * collapses it into a sparse waypoint chain (a point every {@code segmentTiles} tiles,
	 * plus the final tile) instead of a single straight-line destination. Intended for
	 * movement clicks toward far-away points where one click may not cover the whole
	 * distance: walk to waypoint 1, then waypoint 2, etc., ending at the target.
	 */
	public List<WorldPoint> findWaypoints(Player localPlayer, WorldPoint target, int maxDistance, int segmentTiles)
	{
		List<WorldPoint> empty = new ArrayList<>();
		if (localPlayer == null || target == null)
		{
			return empty;
		}
		WorldArea start = localPlayer.getWorldArea();
		if (start == null || start.getPlane() != target.getPlane())
		{
			return empty;
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null)
		{
			return empty;
		}
		WorldPoint startPoint = start.toWorldPoint();
		if (LocalPoint.fromWorld(worldView, startPoint) == null)
		{
			return empty;
		}

		int straightDistance = startPoint.distanceTo(target);
		int searchLimit = Math.max(1, Math.min(CvHelperModPlugin.MOB_FARMER_PATHING_MAX_SEARCH_TILES,
			(maxDistance > 0 ? maxDistance : straightDistance) + CvHelperModPlugin.MOB_FARMER_PATHING_SLACK_TILES));

		ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
		Map<WorldPoint, Integer> distances = new HashMap<>();
		Map<WorldPoint, WorldPoint> predecessors = new HashMap<>();
		queue.add(startPoint);
		distances.put(startPoint, 0);
		boolean reached = startPoint.equals(target);

		while (!queue.isEmpty() && !reached)
		{
			WorldPoint point = queue.remove();
			int pathDistance = distances.get(point);
			if (pathDistance >= searchLimit)
			{
				continue;
			}
			WorldArea area = new WorldArea(point, start.getWidth(), start.getHeight());
			for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
			{
				if (!canTravelSafely(worldView, area, direction[0], direction[1]))
				{
					continue;
				}
				WorldPoint next = new WorldPoint(point.getX() + direction[0], point.getY() + direction[1], point.getPlane());
				if (distances.containsKey(next) || LocalPoint.fromWorld(worldView, next) == null)
				{
					continue;
				}
				distances.put(next, pathDistance + 1);
				predecessors.put(next, point);
				if (next.equals(target))
				{
					reached = true;
					break;
				}
				queue.add(next);
			}
		}

		if (!reached)
		{
			return empty;
		}

		List<WorldPoint> route = new ArrayList<>();
		WorldPoint cursor = target;
		route.add(cursor);
		while (!cursor.equals(startPoint))
		{
			cursor = predecessors.get(cursor);
			if (cursor == null)
			{
				return empty;
			}
			route.add(cursor);
		}
		java.util.Collections.reverse(route);

		int step = Math.max(1, segmentTiles);
		List<WorldPoint> waypoints = new ArrayList<>();
		for (int i = step; i < route.size() - 1; i += step)
		{
			waypoints.add(route.get(i));
		}
		waypoints.add(route.get(route.size() - 1));
		return waypoints;
	}

	/**
	 * Reconstructs the door-aware walking route from the player to a melee tile
	 * next to {@code target}, and reports any openable doors along it with their
	 * current open/closed state. Shape: {@code {route:[{x,y}], doors:[{x,y,open}],
	 * reachable, length}}. Returns an empty map when no route exists. Intended for
	 * the WebHelper reachability grid so doors on the path are visible and trusted.
	 */
	public Map<String, Object> mobFarmerPathingDetail(Player localPlayer, WorldPoint target, int maxDistance)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		if (localPlayer == null || target == null)
		{
			return out;
		}
		WorldArea start = localPlayer.getWorldArea();
		if (start == null || start.getPlane() != target.getPlane())
		{
			return out;
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null)
		{
			return out;
		}
		WorldPoint startPoint = start.toWorldPoint();
		if (LocalPoint.fromWorld(worldView, startPoint) == null)
		{
			return out;
		}

		int straightDistance = startPoint.distanceTo(target);
		int searchLimit = Math.max(1, Math.min(CvHelperModPlugin.MOB_FARMER_PATHING_MAX_SEARCH_TILES,
			(maxDistance > 0 ? maxDistance : straightDistance) + CvHelperModPlugin.MOB_FARMER_PATHING_SLACK_TILES));

		ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
		Map<WorldPoint, Integer> distances = new HashMap<>();
		Map<WorldPoint, WorldPoint> predecessors = new HashMap<>();
		queue.add(startPoint);
		distances.put(startPoint, 0);
		WorldPoint goal = null;

		while (!queue.isEmpty())
		{
			WorldPoint point = queue.remove();
			int pathDistance = distances.get(point);
			int manhattan = Math.abs(point.getX() - target.getX()) + Math.abs(point.getY() - target.getY());
			if (point.equals(target) || manhattan == 1)
			{
				goal = point;
				break;
			}
			if (pathDistance >= searchLimit)
			{
				continue;
			}
			WorldArea area = new WorldArea(point, 1, 1);
			for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
			{
				if (!canTravelSafely(worldView, area, direction[0], direction[1]))
				{
					continue;
				}
				WorldPoint next = new WorldPoint(point.getX() + direction[0], point.getY() + direction[1], point.getPlane());
				if (distances.containsKey(next) || LocalPoint.fromWorld(worldView, next) == null)
				{
					continue;
				}
				distances.put(next, pathDistance + 1);
				predecessors.put(next, point);
				queue.add(next);
			}
		}

		if (goal == null)
		{
			return out;
		}

		List<WorldPoint> route = new ArrayList<>();
		WorldPoint cursor = goal;
		while (cursor != null && !cursor.equals(startPoint))
		{
			route.add(cursor);
			cursor = predecessors.get(cursor);
		}
		Collections.reverse(route);

		List<Map<String, Object>> routeOut = new ArrayList<>();
		for (WorldPoint wp : route)
		{
			Map<String, Object> tile = new LinkedHashMap<>();
			tile.put("x", wp.getX());
			tile.put("y", wp.getY());
			routeOut.add(tile);
		}

		List<Map<String, Object>> doorsOut = new ArrayList<>();
		Set<WorldPoint> seen = new HashSet<>();
		List<WorldPoint> scan = new ArrayList<>();
		scan.add(startPoint);
		scan.addAll(route);
		for (WorldPoint wp : scan)
		{
			Integer state = doorState(worldView, wp);
			if (state != null && seen.add(wp))
			{
				Map<String, Object> door = new LinkedHashMap<>();
				door.put("x", wp.getX());
				door.put("y", wp.getY());
				door.put("open", state == 1);
				doorsOut.add(door);
			}
		}

		out.put("route", routeOut);
		out.put("doors", doorsOut);
		out.put("reachable", true);
		out.put("length", route.size());
		return out;
	}

	/**
	 * Flood-fills every tile within a square radius of the player using the same
	 * collision-aware BFS as the rest of this service, and reports per-tile
	 * reachability/path distance/block reason for ALL of them -- not just tiles
	 * that happen to hold a candidate object. Intended as the base layer for the
	 * WebHelper minimap grid so every square has real diagnostic info, with
	 * candidates/footprints/selected-target/route/doors layered on top by the caller.
	 * Shape: {@code {player:{x,y,plane}, radius, visited, tiles:[{x,y,dx,dy,
	 * reachable,pathDistance,blockedReason}]}}.
	 */
	public Map<String, Object> buildReachabilityGrid(Player localPlayer, int radius)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		List<Map<String, Object>> tilesOut = new ArrayList<>();
		out.put("tiles", tilesOut);
		if (localPlayer == null)
		{
			return out;
		}
		WorldArea start = localPlayer.getWorldArea();
		if (start == null)
		{
			return out;
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null)
		{
			return out;
		}
		WorldPoint startPoint = start.toWorldPoint();
		if (LocalPoint.fromWorld(worldView, startPoint) == null)
		{
			return out;
		}

		int clampedRadius = Math.max(1, Math.min(30, radius));
		int hardCap = (clampedRadius * 2 + 1) * (clampedRadius * 2 + 1) + 50;

		ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
		Map<WorldPoint, Integer> distances = new HashMap<>();
		Map<WorldPoint, String> blockedReasons = new HashMap<>();
		queue.add(startPoint);
		distances.put(startPoint, 0);
		int visited = 0;

		while (!queue.isEmpty() && visited < hardCap)
		{
			WorldPoint point = queue.remove();
			int pathDistance = distances.get(point);
			visited++;
			int dxAbs = Math.abs(point.getX() - startPoint.getX());
			int dyAbs = Math.abs(point.getY() - startPoint.getY());
			if (Math.max(dxAbs, dyAbs) >= clampedRadius)
			{
				// Tile is on/past the requested edge; record it but don't expand further.
				continue;
			}
			WorldArea area = new WorldArea(point, 1, 1);
			for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
			{
				WorldPoint next = new WorldPoint(point.getX() + direction[0], point.getY() + direction[1], point.getPlane());
				if (distances.containsKey(next))
				{
					continue;
				}
				if (!canTravelSafely(worldView, area, direction[0], direction[1]))
				{
					blockedReasons.putIfAbsent(next, "collision-blocked");
					continue;
				}
				if (LocalPoint.fromWorld(worldView, next) == null)
				{
					blockedReasons.putIfAbsent(next, "scene-blocked");
					continue;
				}
				distances.put(next, pathDistance + 1);
				queue.add(next);
			}
		}

		for (int dx = -clampedRadius; dx <= clampedRadius; dx++)
		{
			for (int dy = -clampedRadius; dy <= clampedRadius; dy++)
			{
				WorldPoint point = new WorldPoint(startPoint.getX() + dx, startPoint.getY() + dy, startPoint.getPlane());
				Integer dist = distances.get(point);
				LocalPoint localPoint = LocalPoint.fromWorld(worldView, point);
				boolean inScene = localPoint != null;
				Map<String, Object> tile = new LinkedHashMap<>();
				tile.put("x", point.getX());
				tile.put("y", point.getY());
				tile.put("plane", point.getPlane());
				tile.put("dx", dx);
				tile.put("dy", dy);
				tile.put("sceneX", inScene ? localPoint.getSceneX() : null);
				tile.put("sceneY", inScene ? localPoint.getSceneY() : null);
				tile.put("inScene", inScene);
				tile.put("reachable", dist != null);
				tile.put("pathDistance", dist);
				if (dist == null)
				{
					tile.put("blockedReason", !inScene ? "scene-blocked" : blockedReasons.getOrDefault(point, "no-route"));
				}
				tilesOut.add(tile);
			}
		}

		Map<String, Object> playerOut = new LinkedHashMap<>();
		playerOut.put("x", startPoint.getX());
		playerOut.put("y", startPoint.getY());
		playerOut.put("plane", startPoint.getPlane());
		out.put("player", playerOut);
		out.put("radius", clampedRadius);
		out.put("visited", visited);
		return out;
	}

	/** null = no openable door on the tile; 1 = open (has Close action); 0 = closed (has Open action). */
	private Integer doorState(WorldView worldView, WorldPoint point)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(worldView, point);
		if (localPoint == null)
		{
			return null;
		}
		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return null;
		}
		Tile[][][] tiles = scene.getTiles();
		int plane = point.getPlane();
		if (tiles == null || plane < 0 || plane >= tiles.length)
		{
			return null;
		}
		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();
		if (sceneX < 0 || sceneX >= tiles[plane].length || sceneY < 0 || sceneY >= tiles[plane][sceneX].length)
		{
			return null;
		}
		Tile tile = tiles[plane][sceneX][sceneY];
		if (tile == null)
		{
			return null;
		}
		WallObject wall = tile.getWallObject();
		if (wall != null)
		{
			Integer state = doorStateFromId(wall.getId());
			if (state != null)
			{
				return state;
			}
		}
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject gameObject : gameObjects)
			{
				if (gameObject != null)
				{
					Integer state = doorStateFromId(gameObject.getId());
					if (state != null)
					{
						return state;
					}
				}
			}
		}
		return null;
	}

	private Integer doorStateFromId(int objectId)
	{
		if (objectId < 0)
		{
			return null;
		}
		ObjectComposition composition = client.getObjectDefinition(objectId);
		String[] actions = composition == null ? null : composition.getActions();
		if (actions == null)
		{
			return null;
		}
		boolean canOpen = false;
		boolean canClose = false;
		for (String action : actions)
		{
			if (action == null)
			{
				continue;
			}
			if (action.equalsIgnoreCase("Open"))
			{
				canOpen = true;
			}
			else if (action.equalsIgnoreCase("Close"))
			{
				canClose = true;
			}
		}
		if (canClose)
		{
			return 1;
		}
		if (canOpen)
		{
			return 0;
		}
		return null;
	}

}
