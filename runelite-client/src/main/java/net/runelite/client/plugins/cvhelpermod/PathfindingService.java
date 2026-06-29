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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Bounded collision-aware pathfinding / reachability (BFS over WorldArea.canTravelInDirection).
 * Verbatim port of the reference path-distance helpers; shared by the mob and skill farmers.
 * <p>
 * Closed doors/gates are real collision blocks here -- there is no silent "the client opens
 * doors while walking" assumption baked into the base kernel any more. A bounded transition
 * chain is allowed through {@link #classifyDoorBlock} when each object and action is explicit,
 * door isn't denylisted and the relevant auto-open/auto-close config flag is enabled; the
 * winning path's {@code doorTransition} is returned to the caller so it can perform the actual
 * menu-action click and wait for the door to really open/close before treating the route as
 * walkable, instead of assuming it already is.
 */
@Singleton
public class PathfindingService
{
	private static final int MAX_DOOR_HOPS = 4;
	private static final Set<String> SAFE_TRANSITION_ACTIONS = new HashSet<>(Arrays.asList(
		"Open", "Close", "Enter", "Pass", "Climb-over"));

	@Inject
	private Client client;

	@Inject
	private CvHelperModConfig config;

	/** One node of the door-hop-aware BFS: a tile plus how many door transitions were used to reach it. */
	private static final class BfsNode
	{
		final WorldPoint point;
		final int hops;

		BfsNode(WorldPoint point, int hops)
		{
			this.point = point;
			this.hops = hops;
		}
	}

	/** Classification of why a step is blocked by a door, and whether CV Helper is allowed to act on it. */
	private static final class DoorBlock
	{
		final int id;
		final String name;
		final WorldPoint tile;
		final String requiredAction;
		final boolean allowed;
		final String status;
		final List<String> actions;
		final WorldPoint from;
		final WorldPoint to;

		DoorBlock(int id, String name, WorldPoint tile, String requiredAction, boolean allowed, String status,
			List<String> actions, WorldPoint from, WorldPoint to)
		{
			this.id = id;
			this.name = name;
			this.tile = tile;
			this.requiredAction = requiredAction;
			this.allowed = allowed;
			this.status = status;
			this.actions = actions;
			this.from = from;
			this.to = to;
		}

		Map<String, Object> toMap()
		{
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("id", id);
			out.put("name", name);
			Map<String, Object> tileMap = new LinkedHashMap<>();
			tileMap.put("x", tile.getX());
			tileMap.put("y", tile.getY());
			tileMap.put("plane", tile.getPlane());
			out.put("worldTile", tileMap);
			out.put("fromTile", worldPointMap(from));
			out.put("toTile", worldPointMap(to));
			out.put("requiredAction", requiredAction);
			out.put("allowlistStatus", status);
			out.put("actions", actions == null ? new ArrayList<>() : actions);
			return out;
		}
	}

	/** Outcome of the shared door-hop-aware BFS core used by all goal-directed pathing methods. */
	private static final class CoreBfsResult
	{
		boolean reachable;
		int pathDistance;
		int searchLimit;
		int visited;
		int blockedByCollision;
		int blockedByScene;
		WorldPoint reachedPoint;
		DoorBlock doorTransition;
		List<DoorBlock> transitionRoute = new ArrayList<>();
		Integer failedTransitionIndex;
		/**
		 * First door encountered along the search frontier that blocked a step AND wasn't
		 * {@code allowed} (denylisted, unknown action, or the relevant auto flag disabled).
		 * Only meaningful when {@code reachable} is false -- lets the caller report
		 * "manual action required: <door>" instead of a generic collision-blocked count
		 * when a door is genuinely the reason the target couldn't be reached.
		 */
		DoorBlock unresolvedDoorBlock;
	}

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
		WorldPoint startPoint = start.toWorldPoint();

		final WorldView fv1 = worldView;
		CoreBfsResult core = doorAwareBfs(worldView, startPoint, start.getWidth(), start.getHeight(), searchLimit,
			area -> canReachMelee(fv1, area, target));

		if (core.reachable)
		{
			CvHelperModPlugin.PathingResult result = CvHelperModPlugin.PathingResult.reachable(core.pathDistance, searchLimit, core.visited);
			result.doorTransition = core.doorTransition == null ? null : core.doorTransition.toMap();
			applyTransitionRoute(result, core);
			return result;
		}
		CvHelperModPlugin.PathingResult unreachable = CvHelperModPlugin.PathingResult.unreachable(
			failureReason(searchLimit, core.blockedByScene, core.blockedByCollision), searchLimit, core.visited);
		applyDoorDiagnostics(unreachable, core.unresolvedDoorBlock);
		return unreachable;
	}

	/**
	 * Surfaces explicit {@code blockedByDoor}/{@code manualActionRequired}/{@code
	 * manualActionReason}/{@code blockingDoor} fields on an unreachable result when a door
	 * CV Helper can't act on was the (or a) reason -- so callers don't have to parse the
	 * free-text failure reason to tell "blocked by an obstacle" apart from "blocked by a
	 * door that needs a human or a config change", per OSR-14 Part E's diagnostics contract.
	 */
	private void applyDoorDiagnostics(CvHelperModPlugin.PathingResult result, DoorBlock block)
	{
		if (block == null)
		{
			return;
		}
		result.blockedByDoor = true;
		result.manualActionRequired = true;
		result.manualActionReason = "door-" + block.status + ":" + block.id;
		result.blockingDoor = block.toMap();
	}

	private void applyDoorDiagnostics(CvHelperModPlugin.InteractionPathingResult result, DoorBlock block)
	{
		if (block == null)
		{
			return;
		}
		result.blockedByDoor = true;
		result.manualActionRequired = true;
		result.manualActionReason = "door-" + block.status + ":" + block.id;
		result.blockingDoor = block.toMap();
	}

	private List<Map<String, Object>> transitionMaps(List<DoorBlock> route)
	{
		List<Map<String, Object>> out = new ArrayList<>();
		for (int index = 0; route != null && index < route.size(); index++)
		{
			Map<String, Object> step = route.get(index).toMap();
			step.put("index", index);
			step.put("remainingTransitions", route.size() - index - 1);
			out.add(step);
		}
		return out;
	}

	private void applyTransitionRoute(CvHelperModPlugin.PathingResult result, CoreBfsResult core)
	{
		result.transitionRoute = transitionMaps(core.transitionRoute);
		result.transitionCount = result.transitionRoute.size();
		result.routeDepth = core.pathDistance;
		result.failedTransitionIndex = core.failedTransitionIndex;
	}

	private void applyTransitionRoute(CvHelperModPlugin.InteractionPathingResult result, CoreBfsResult core)
	{
		result.transitionRoute = transitionMaps(core.transitionRoute);
		result.transitionCount = result.transitionRoute.size();
		result.routeDepth = core.pathDistance;
		result.failedTransitionIndex = core.failedTransitionIndex;
	}

	public boolean isInsideFootprint(WorldArea footprint, WorldPoint point)
	{
		return point.getPlane() == footprint.getPlane()
			&& point.getX() >= footprint.getX()
			&& point.getX() < footprint.getX() + footprint.getWidth()
			&& point.getY() >= footprint.getY()
			&& point.getY() < footprint.getY() + footprint.getHeight();
	}

	/**
	 * Real "is this tile occupiable" check, using the collision flags of the tile itself
	 * rather than whether you can step away from it. {@link #canStandOnTile} used to check
	 * the destination flags of neighboring tiles as a proxy, which let object tiles (eg. a
	 * tree's own tile) pass the filter as long as one neighbor happened to be open.
	 */
	public boolean isTileWalkable(WorldView worldView, WorldPoint point)
	{
		if (worldView == null)
		{
			return false;
		}
		LocalPoint localPoint = LocalPoint.fromWorld(worldView, point);
		if (localPoint == null)
		{
			return false;
		}
		int[][] collisionFlags = collisionFlagsForPlane(worldView, point.getPlane());
		if (collisionFlags == null)
		{
			return false;
		}
		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();
		if (sceneX < 0 || sceneX >= collisionFlags.length || sceneY < 0 || sceneY >= collisionFlags[sceneX].length)
		{
			return false;
		}
		return (collisionFlags[sceneX][sceneY] & net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
	}

	private int[][] collisionFlagsForPlane(WorldView worldView, int plane)
	{
		net.runelite.api.CollisionData[] collisionMaps = worldView.getCollisionMaps();
		if (collisionMaps == null || plane < 0 || plane >= collisionMaps.length || collisionMaps[plane] == null)
		{
			return null;
		}
		return collisionMaps[plane].getFlags();
	}

	/**
	 * Retained for the "can leave this tile in some direction" check used by the local
	 * movement BFS; this is NOT a walkability check on its own. Use {@link #isTileWalkable}
	 * to test whether a tile itself can be stood/interacted on.
	 */
	public boolean canStandOnTile(WorldView worldView, WorldPoint point)
	{
		if (worldView == null)
		{
			return false;
		}
		if (!isTileWalkable(worldView, point))
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

		CoreBfsResult core = doorAwareBfs(worldView, startPoint, 1, 1, searchLimit,
			area -> interactionCandidates.contains(area.toWorldPoint()));

		if (core.reachable)
		{
			CvHelperModPlugin.InteractionPathingResult result = CvHelperModPlugin.InteractionPathingResult.reachable(core.pathDistance, searchLimit, core.visited,
				core.reachedPoint, footprint, evaluatedInteractionTiles, walkableInteractionTiles,
				blockedByCollision + blockedByScene + (walkableInteractionTiles - 1),
				blockedByCollision, blockedByScene);
			result.doorTransition = core.doorTransition == null ? null : core.doorTransition.toMap();
			applyTransitionRoute(result, core);
			return result;
		}

		String failureReason = "matched-but-no-route-to-interaction-tile";
		if (core.blockedByScene > 0)
		{
			failureReason += ",scene-blocked:" + core.blockedByScene;
		}
		if (core.blockedByCollision > 0)
		{
			failureReason += ",collision-blocked:" + core.blockedByCollision;
		}
		CvHelperModPlugin.InteractionPathingResult unreachable = CvHelperModPlugin.InteractionPathingResult.unreachable(failureReason, searchLimit, core.visited,
			footprint, evaluatedInteractionTiles, walkableInteractionTiles,
			blockedByCollision + blockedByScene, blockedByCollision, blockedByScene);
		applyDoorDiagnostics(unreachable, core.unresolvedDoorBlock);
		return unreachable;
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

		final WorldView fv2 = worldView;
		CoreBfsResult core = doorAwareBfs(worldView, startPoint, start.getWidth(), start.getHeight(), searchLimit,
			area -> canReachMelee(fv2, area, target));

		if (core.reachable)
		{
			CvHelperModPlugin.PathingResult result = CvHelperModPlugin.PathingResult.reachable(core.pathDistance, searchLimit, core.visited);
			result.doorTransition = core.doorTransition == null ? null : core.doorTransition.toMap();
			applyTransitionRoute(result, core);
			return result;
		}
		CvHelperModPlugin.PathingResult unreachable = CvHelperModPlugin.PathingResult.unreachable("no-route-within:" + searchLimit, searchLimit, core.visited);
		applyDoorDiagnostics(unreachable, core.unresolvedDoorBlock);
		return unreachable;
	}

	private String failureReason(int searchLimit, int blockedByScene, int blockedByCollision)
	{
		String failureReason = "no-route-within:" + searchLimit;
		if (blockedByScene > 0)
		{
			failureReason += ",scene-blocked:" + blockedByScene;
		}
		if (blockedByCollision > 0)
		{
			failureReason += ",collision-blocked:" + blockedByCollision;
		}
		return failureReason;
	}

	/**
	 * Shared door-hop-aware BFS core. Identical traversal to the old per-method BFS loops,
	 * except each visited state is keyed by (tile, door-hops-used-so-far) instead of just the
	 * tile: a step blocked only by a door that {@link #classifyDoorBlock} marks {@code allowed}
	 * may be taken once per path (capped at {@link #MAX_DOOR_HOPS}), and the winning path's
	 * transition (if any) is returned so the caller can actually open/close it and verify
	 * before treating the route as walkable -- not just assume the client will handle it.
	 */
	private CoreBfsResult doorAwareBfs(WorldView worldView, WorldPoint startPoint, int width, int height, int searchLimit, Predicate<WorldArea> goalCheck)
	{
		CoreBfsResult result = new CoreBfsResult();
		result.searchLimit = searchLimit;
		if (worldView == null || startPoint == null || LocalPoint.fromWorld(worldView, startPoint) == null)
		{
			return result;
		}

		ArrayDeque<BfsNode> queue = new ArrayDeque<>();
		Map<String, Integer> distances = new HashMap<>();
		Map<String, List<DoorBlock>> transitionsAtState = new HashMap<>();
		queue.add(new BfsNode(startPoint, 0));
		distances.put(stateKey(startPoint, 0), 0);
		int visited = 0;
		int blockedByCollision = 0;
		int blockedByScene = 0;
		DoorBlock unresolvedDoorBlock = null;

		while (!queue.isEmpty())
		{
			BfsNode current = queue.remove();
			int pathDistance = distances.get(stateKey(current.point, current.hops));
			visited++;
			WorldArea area = new WorldArea(current.point, width, height);
			if (goalCheck.test(area))
			{
				result.reachable = true;
				result.pathDistance = pathDistance;
				result.visited = visited;
				result.reachedPoint = current.point;
				List<DoorBlock> route = transitionsAtState.getOrDefault(stateKey(current.point, current.hops), new ArrayList<>());
				result.transitionRoute = new ArrayList<>(route);
				result.doorTransition = route.isEmpty() ? null : route.get(0);
				return result;
			}
			if (pathDistance >= searchLimit)
			{
				continue;
			}
			for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
			{
				WorldPoint next = new WorldPoint(current.point.getX() + direction[0], current.point.getY() + direction[1], current.point.getPlane());
				boolean canTravel = canTravelSafely(worldView, area, direction[0], direction[1]);
				int nextHops = current.hops;
				DoorBlock usedTransition = null;
				if (!canTravel)
				{
					DoorBlock block = classifyDoorBlock(worldView, current.point, next);
					if (block != null && block.allowed && current.hops < MAX_DOOR_HOPS)
					{
						nextHops = current.hops + 1;
						usedTransition = block;
					}
					else
					{
						blockedByCollision++;
						if (block != null && unresolvedDoorBlock == null)
						{
							unresolvedDoorBlock = block;
						}
						continue;
					}
				}
				String key = stateKey(next, nextHops);
				if (distances.containsKey(key))
				{
					continue;
				}
				if (LocalPoint.fromWorld(worldView, next) == null)
				{
					blockedByScene++;
					continue;
				}
				distances.put(key, pathDistance + 1);
				List<DoorBlock> route = new ArrayList<>(transitionsAtState.getOrDefault(stateKey(current.point, current.hops), new ArrayList<>()));
				if (usedTransition != null)
				{
					route.add(usedTransition);
				}
				transitionsAtState.put(key, route);
				queue.add(new BfsNode(next, nextHops));
			}
		}

		result.reachable = false;
		result.visited = visited;
		result.blockedByCollision = blockedByCollision;
		result.blockedByScene = blockedByScene;
		result.unresolvedDoorBlock = unresolvedDoorBlock;
		result.failedTransitionIndex = unresolvedDoorBlock == null ? null : 0;
		return result;
	}

	private String stateKey(WorldPoint point, int hops)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane() + ":" + hops;
	}

	private static Map<String, Object> worldPointMap(WorldPoint point)
	{
		if (point == null)
		{
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("x", point.getX());
		out.put("y", point.getY());
		out.put("plane", point.getPlane());
		return out;
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

	/**
	 * Pure native collision check -- no door bypass. A closed door is a real block here; see
	 * {@link #classifyDoorBlock} and {@link #doorAwareBfs} for the one-hop door transition
	 * model that replaces the old "assume the client opens it" shortcut.
	 */
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

	/**
	 * Classifies a blocked step as a door transition (or returns null if it's a plain
	 * obstacle/wall). {@code allowed} is true only when the door isn't denylisted AND the
	 * config flag for its required action (auto-open for a closed door, auto-close for an
	 * open-but-blocking gate) is enabled -- callers must still perform the actual click and
	 * verify before treating the path as walkable; this only says whether CV Helper is
	 * permitted to try.
	 */
	private DoorBlock classifyDoorBlock(WorldView worldView, WorldPoint from, WorldPoint to)
	{
		if (Math.abs(to.getX() - from.getX()) + Math.abs(to.getY() - from.getY()) != 1)
		{
			return null;
		}
		WorldPoint doorTile = from;
		Integer doorId = doorObjectIdAt(worldView, from);
		if (doorId == null)
		{
			doorId = doorObjectIdAt(worldView, to);
			doorTile = to;
		}
		if (doorId == null)
		{
			return null;
		}
		ObjectComposition composition = client.getObjectDefinition(doorId);
		String name = composition == null ? null : composition.getName();
		List<String> actions = composition == null || composition.getActions() == null
			? new ArrayList<>() : Arrays.asList(composition.getActions());
		Integer state = doorStateFromId(doorId);
		String requiredAction = state != null && state == 0 ? "Open" : state != null && state == 1 ? "Close" : null;
		if (requiredAction == null)
		{
			for (String candidate : actions)
			{
				if (candidate != null && SAFE_TRANSITION_ACTIONS.contains(candidate))
				{
					requiredAction = candidate;
					break;
				}
			}
		}
		boolean denylisted = isDoorDenylisted(doorId);
		boolean allowlisted = isDoorAllowlisted(doorId);
		boolean autoFlagEnabled = config != null && (
			"Open".equals(requiredAction) ? config.mobFarmerDoorAutoOpenEnabled()
				: "Close".equals(requiredAction) ? config.mobFarmerDoorAutoCloseEnabled()
				: SAFE_TRANSITION_ACTIONS.contains(requiredAction) && config.mobFarmerDoorAutoOpenEnabled());
		String status;
		if (denylisted)
		{
			status = "denylisted";
		}
		else if (requiredAction == null)
		{
			status = "unknown-transition-action";
		}
		else if (!allowlisted)
		{
			status = "unallowlisted";
		}
		else if (!autoFlagEnabled)
		{
			status = "Close".equals(requiredAction) ? "auto-close-disabled" : "auto-transition-disabled";
		}
		else
		{
			status = "allowed";
		}
		boolean allowed = !denylisted && allowlisted && requiredAction != null && autoFlagEnabled;
		return new DoorBlock(doorId, name, doorTile, requiredAction, allowed, status, actions, from, to);
	}

	private boolean isDoorAllowlisted(int objectId)
	{
		return doorListContains(config == null ? null : config.mobFarmerDoorAllowlist(), objectId);
	}

	private boolean isDoorDenylisted(int objectId)
	{
		return doorListContains(config == null ? null : config.mobFarmerDoorDenylist(), objectId);
	}

	private boolean doorListContains(String raw, int objectId)
	{
		if (raw == null || raw.trim().isEmpty())
		{
			return false;
		}
		ObjectComposition composition = client.getObjectDefinition(objectId);
		String name = composition == null ? null : composition.getName();
		for (String token : raw.split("[|,;\\n]+"))
		{
			String trimmed = token.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			if (trimmed.equals(String.valueOf(objectId)))
			{
				return true;
			}
			if (name != null && trimmed.equalsIgnoreCase(name))
			{
				return true;
			}
		}
		return false;
	}

	private Integer doorObjectIdAt(WorldView worldView, WorldPoint point)
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
		if (wall != null && isDoorObject(wall.getId()))
		{
			return wall.getId();
		}
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject gameObject : gameObjects)
			{
				if (gameObject != null && isDoorObject(gameObject.getId()))
				{
					return gameObject.getId();
				}
			}
		}
		return null;
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
			if (action != null && SAFE_TRANSITION_ACTIONS.stream().anyMatch(action::equalsIgnoreCase))
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
	 * distance: walk to waypoint 1, then waypoint 2, etc., ending at the target. Does not
	 * use the door-hop model -- a manual movement click target should not silently walk
	 * through a closed door without a deliberate decision.
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
		Collections.reverse(route);

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
	 * Flood-fills every tile within a square radius of the player using the same door-hop-
	 * aware BFS as the rest of this service, and reports per-tile reachability/path
	 * distance/block reason for ALL of them -- not just tiles that happen to hold a candidate
	 * object. Each tile gets one of: {@code reachable} (walkable right now), {@code
	 * reachable-via-door} (walkable only after a permitted door transition -- the actual click
	 * still needs to happen and be verified, this just means the route exists), {@code
	 * blocked-by-door} (a door blocks it but CV Helper isn't allowed to act on it --
	 * denylisted, or the relevant auto-open/auto-close flag is off), {@code collision-blocked}
	 * (a plain obstacle), or {@code scene-blocked}/{@code no-route}. Intended as the base layer
	 * for the WebHelper minimap grid so every square has real diagnostic info, with
	 * candidates/footprints/selected-target/route layered on top by the caller.
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

		int clampedRadius = Math.max(1, Math.min(100, radius));
		int hardCap = ((clampedRadius * 2 + 1) * (clampedRadius * 2 + 1) + 50) * (MAX_DOOR_HOPS + 1);

		ArrayDeque<BfsNode> queue = new ArrayDeque<>();
		Map<String, Integer> distances = new HashMap<>();
		Map<String, List<DoorBlock>> transitionsAtState = new HashMap<>();
		Map<WorldPoint, String> blockedReasons = new HashMap<>();
		Map<WorldPoint, Map<String, Object>> blockingDoors = new HashMap<>();
		queue.add(new BfsNode(startPoint, 0));
		distances.put(stateKey(startPoint, 0), 0);
		int visited = 0;

		while (!queue.isEmpty() && visited < hardCap)
		{
			BfsNode current = queue.remove();
			int pathDistance = distances.get(stateKey(current.point, current.hops));
			visited++;
			int dxAbs = Math.abs(current.point.getX() - startPoint.getX());
			int dyAbs = Math.abs(current.point.getY() - startPoint.getY());
			if (Math.max(dxAbs, dyAbs) >= clampedRadius)
			{
				// Tile is on/past the requested edge; record it but don't expand further.
				continue;
			}
			WorldArea area = new WorldArea(current.point, 1, 1);
			for (int[] direction : CvHelperModPlugin.MOB_FARMER_PATH_DIRECTIONS)
			{
				WorldPoint next = new WorldPoint(current.point.getX() + direction[0], current.point.getY() + direction[1], current.point.getPlane());
				boolean canTravel = canTravelSafely(worldView, area, direction[0], direction[1]);
				int nextHops = current.hops;
				DoorBlock usedTransition = null;
				if (!canTravel)
				{
					DoorBlock block = classifyDoorBlock(worldView, current.point, next);
					if (block != null)
					{
						// Door classification is more specific than a collision recorded for the
						// same tile from another frontier edge, so it must take precedence.
						blockedReasons.put(next, block.allowed ? "reachable-via-door" : "blocked-by-door:" + block.id);
						blockingDoors.putIfAbsent(next, block.toMap());
						if (block.allowed && current.hops < MAX_DOOR_HOPS)
						{
							nextHops = current.hops + 1;
							usedTransition = block;
						}
						else
						{
							continue;
						}
					}
					else
					{
						blockedReasons.putIfAbsent(next, "collision-blocked");
						continue;
					}
				}
				String key = stateKey(next, nextHops);
				if (distances.containsKey(key))
				{
					continue;
				}
				if (LocalPoint.fromWorld(worldView, next) == null)
				{
					blockedReasons.putIfAbsent(next, "scene-blocked");
					continue;
				}
				distances.put(key, pathDistance + 1);
				List<DoorBlock> route = new ArrayList<>(transitionsAtState.getOrDefault(stateKey(current.point, current.hops), new ArrayList<>()));
				if (usedTransition != null)
				{
					route.add(usedTransition);
				}
				transitionsAtState.put(key, route);
				queue.add(new BfsNode(next, nextHops));
			}
		}

		for (int dx = -clampedRadius; dx <= clampedRadius; dx++)
		{
			for (int dy = -clampedRadius; dy <= clampedRadius; dy++)
			{
				WorldPoint point = new WorldPoint(startPoint.getX() + dx, startPoint.getY() + dy, startPoint.getPlane());
				Integer distDirect = distances.get(stateKey(point, 0));
				Integer distViaDoor = null;
				int transitionCount = 0;
				for (int hops = 1; hops <= MAX_DOOR_HOPS; hops++)
				{
					Integer distance = distances.get(stateKey(point, hops));
					if (distance != null && (distViaDoor == null || distance < distViaDoor))
					{
						distViaDoor = distance;
						transitionCount = hops;
					}
				}
				boolean reachableDirect = distDirect != null;
				boolean reachableViaDoor = !reachableDirect && distViaDoor != null;
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
				// "reachable" intentionally means walkable RIGHT NOW with no action needed --
				// a tile only reachable via a pending door transition is NOT folded into this,
				// so the UI can render it as its own distinct state instead of plain "clear".
				tile.put("reachable", reachableDirect);
				tile.put("reachableViaDoor", reachableViaDoor);
				tile.put("transitionCount", transitionCount);
				tile.put("multiTransitionRoute", transitionCount > 1);
				tile.put("pathDistance", reachableDirect ? distDirect : reachableViaDoor ? distViaDoor : null);
				if (reachableViaDoor)
				{
					List<DoorBlock> route = transitionsAtState.getOrDefault(stateKey(point, transitionCount), new ArrayList<>());
					tile.put("blockedReason", transitionCount > 1 ? "reachable-via-multiple-transitions" : "reachable-via-transition");
					tile.put("transitionRoute", transitionMaps(route));
					if (!route.isEmpty())
					{
						tile.put("blockingDoor", route.get(0).toMap());
					}
				}
				else if (!reachableDirect)
				{
					tile.put("blockedReason", !inScene ? "scene-blocked" : blockedReasons.getOrDefault(point, "no-route"));
					Map<String, Object> doorMeta = blockingDoors.get(point);
					if (doorMeta != null)
					{
						tile.put("blockingDoor", doorMeta);
					}
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

	/**
	 * Public, player-relative variant of {@link #doorState} for live execution code that
	 * needs to verify a door's CURRENT state (eg. after clicking "Open", before trusting the
	 * path is actually clear) rather than re-running a full reachability search.
	 */
	public Integer currentDoorState(Player localPlayer, WorldPoint point)
	{
		if (localPlayer == null || point == null)
		{
			return null;
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null)
		{
			return null;
		}
		return doorState(worldView, point);
	}

	public boolean isDoorTransitionSatisfied(Player localPlayer, WorldPoint from, WorldPoint to, String requiredAction)
	{
		if (localPlayer == null || from == null || to == null || !"Open".equals(requiredAction))
		{
			return false;
		}
		WorldView worldView = localPlayer.getWorldView();
		if (worldView == null)
		{
			worldView = client.getTopLevelWorldView();
		}
		if (worldView == null)
		{
			return false;
		}
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		return Math.abs(dx) + Math.abs(dy) == 1 && canTravelSafely(worldView, new WorldArea(from, 1, 1), dx, dy);
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
