/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelper;

import net.runelite.client.game.ItemManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ItemSafetyService
{
	private static final String IMPLICIT_NEVER_DROP_ITEMS = String.join("|",
		"rune pouch",
		"coins",
		"clue scroll",
		"clue geode",
		"clue nest",
		"clue bottle",
		"casket",
		"reward casket",
		"clue box",
		"scroll box",
		"giant key",
		"mossy key",
		"brimstone key",
		"larran's key",
		"crystal key",
		"enhanced crystal key",
		"dark totem",
		"ancient shard",
		"champion's scroll",
		"long bone",
		"curved bone",
		"jar of");

	@Inject
	private ItemManager itemManager;

	private List<String> splitPolicy(String policy)
	{
		if (policy == null || policy.trim().isEmpty())
		{
			return Collections.emptyList();
		}
		return Arrays.stream(policy.split("\\s*(?:\\||,|;|\\r?\\n)\\s*"))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toList());
	}

	public boolean isClueScroll(String itemName, int itemId)
	{
		String normalized = itemName == null ? "" : itemName.toLowerCase();
		return normalized.contains("clue scroll") ||
		       normalized.contains("clue geode") ||
		       normalized.contains("clue nest") ||
		       normalized.contains("clue bottle") ||
		       normalized.contains("casket") ||
		       normalized.contains("clue box") ||
		       normalized.contains("scroll box");
	}

	public boolean isConservativelyProtectedByName(String itemName, int itemId)
	{
		String normalized = itemName == null ? "" : itemName.toLowerCase();
		return normalized.contains("key") ||
		       normalized.contains("totem") ||
		       normalized.contains("shard") ||
		       normalized.contains("champion's scroll") ||
		       normalized.contains("long bone") ||
		       normalized.contains("curved bone") ||
		       normalized.contains("jar of");
	}

	public boolean isProtectedItem(String itemName, int itemId, String userProtectedItems)
	{
		if (isClueScroll(itemName, itemId) || isConservativelyProtectedByName(itemName, itemId))
		{
			return true;
		}
		String normalized = itemName == null ? "" : itemName.toLowerCase();
		for (String target : splitPolicy(userProtectedItems))
		{
			if (normalized.contains(target.toLowerCase()))
			{
				return true;
			}
		}
		for (String target : splitPolicy(IMPLICIT_NEVER_DROP_ITEMS))
		{
			if (normalized.contains(target.toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isValuable(int itemId, int maxDropValue)
	{
		if (maxDropValue <= 0)
		{
			return false;
		}
		try
		{
			int gePrice = itemManager.getItemPrice(itemId);
			if (gePrice > maxDropValue)
			{
				return true;
			}
			int haPrice = itemManager.getItemComposition(itemId).getHaPrice();
			if (haPrice > maxDropValue)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return true;
		}
		return false;
	}

	public boolean isAllowedToDrop(String itemName, int itemId, String dropAllowlist, String userProtectedItems, int maxDropValue)
	{
		if (isProtectedItem(itemName, itemId, userProtectedItems))
		{
			return false;
		}
		if (isValuable(itemId, maxDropValue))
		{
			return false;
		}
		String normalized = itemName == null ? "" : itemName.toLowerCase();
		for (String target : splitPolicy(dropAllowlist))
		{
			if (normalized.contains(target.toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isAllowedToBank(String itemName, int itemId, String bankAllowlist)
	{
		String normalized = itemName == null ? "" : itemName.toLowerCase();
		for (String target : splitPolicy(bankAllowlist))
		{
			if (normalized.contains(target.toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}
}
