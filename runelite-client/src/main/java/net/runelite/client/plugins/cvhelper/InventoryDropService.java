/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelper;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class InventoryDropService
{
	private static final Logger log = LoggerFactory.getLogger(InventoryDropService.class);

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ItemSafetyService itemSafetyService;

	public enum DropOpportunity
	{
		NONE,
		INVENTORY_FULL,
		TARGET_COMPLETED,
		GATHER_COMPLETED,
		CLEANUP_PHASE,
		MANUAL_INVOCATION
	}

	public enum DropDecision
	{
		SKIP_DISABLED,
		SKIP_MODE_NEVER,
		SKIP_NOT_AN_OPPORTUNITY,
		SKIP_BELOW_THRESHOLD,
		SKIP_NO_CANDIDATES,
		SKIP_PROTECTED_ALL,
		DROP_ALLOWED
	}

	public static class DropPolicyStatus
	{
		public boolean enabled;
		public CvHelperDropMode mode;
		public int thresholdSlots;
		public int currentSlots;
		public DropOpportunity opportunity;
		public DropDecision decision;
		public List<DropCandidate> candidates;
		public List<DropCandidate> protectedSkipped;
		public String lastFailureReason;
		public String lastActionAttempt;

		public DropPolicyStatus()
		{
			this.enabled = false;
			this.mode = CvHelperDropMode.NEVER;
			this.thresholdSlots = 28;
			this.currentSlots = 0;
			this.opportunity = DropOpportunity.NONE;
			this.decision = DropDecision.SKIP_DISABLED;
			this.candidates = new ArrayList<>();
			this.protectedSkipped = new ArrayList<>();
			this.lastFailureReason = null;
			this.lastActionAttempt = null;
		}

		public Map<String, Object> toMap()
		{
			Map<String, Object> map = new java.util.HashMap<>();
			map.put("enabled", enabled);
			map.put("mode", mode.name());
			map.put("thresholdSlots", thresholdSlots);
			map.put("currentSlots", currentSlots);
			map.put("opportunity", opportunity.name());
			map.put("decision", decision.name());
			map.put("candidates", candidates.stream().map(DropCandidate::toMap).toList());
			map.put("protectedSkipped", protectedSkipped.stream().map(DropCandidate::toMap).toList());
			map.put("lastFailureReason", lastFailureReason);
			map.put("lastActionAttempt", lastActionAttempt);
			return map;
		}
	}

	public static class DropCandidate
	{
		public int slot;
		public int itemId;
		public String itemName;
		public int quantity;
		public int geValue;
		public boolean protected;

		public DropCandidate(int slot, int itemId, String itemName, int quantity, int geValue, boolean protectedItem)
		{
			this.slot = slot;
			this.itemId = itemId;
			this.itemName = itemName;
			this.quantity = quantity;
			this.geValue = geValue;
			this.protected = protectedItem;
		}

		public Map<String, Object> toMap()
		{
			Map<String, Object> map = new java.util.HashMap<>();
			map.put("slot", slot);
			map.put("itemId", itemId);
			map.put("itemName", itemName);
			map.put("quantity", quantity);
			map.put("geValue", geValue);
			map.put("protected", protected);
			return map;
		}
	}

	public DropPolicyStatus evaluateDropOpportunity(
		boolean policyEnabled,
		CvHelperDropMode mode,
		int thresholdSlots,
		String dropAllowlist,
		String protectedItems,
		int maxDropValue,
		DropOpportunity opportunity
	)
	{
		DropPolicyStatus status = new DropPolicyStatus();
		status.enabled = policyEnabled;
		status.mode = mode;
		status.thresholdSlots = thresholdSlots;
		status.opportunity = opportunity;

		if (!policyEnabled)
		{
			status.decision = DropDecision.SKIP_DISABLED;
			status.lastFailureReason = "Drop policy is disabled";
			return status;
		}

		if (mode == CvHelperDropMode.NEVER)
		{
			status.decision = DropDecision.SKIP_MODE_NEVER;
			status.lastFailureReason = "Drop mode is NEVER";
			return status;
		}

		if (mode == CvHelperDropMode.MANUAL_ONLY && opportunity != DropOpportunity.MANUAL_INVOCATION)
		{
			status.decision = DropDecision.SKIP_NOT_AN_OPPORTUNITY;
			status.lastFailureReason = "Manual-only mode requires manual invocation";
			return status;
		}

		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			status.decision = DropDecision.SKIP_NOT_AN_OPPORTUNITY;
			status.lastFailureReason = "Inventory not available";
			return status;
		}

		int occupiedSlots = countOccupiedSlots(inventory);
		status.currentSlots = occupiedSlots;

		if (occupiedSlots < thresholdSlots)
		{
			status.decision = DropDecision.SKIP_BELOW_THRESHOLD;
			status.lastFailureReason = "Inventory slots (" + occupiedSlots + ") below threshold (" + thresholdSlots + ")";
			return status;
		}

		boolean opportunityMatches = false;
		switch (mode)
		{
			case WHEN_FULL:
				opportunityMatches = (opportunity == DropOpportunity.INVENTORY_FULL);
				break;
			case AFTER_TARGET:
				opportunityMatches = (opportunity == DropOpportunity.TARGET_COMPLETED);
				break;
			case AFTER_GATHER:
				opportunityMatches = (opportunity == DropOpportunity.GATHER_COMPLETED);
				break;
			case CLEANUP_ONLY:
				opportunityMatches = (opportunity == DropOpportunity.CLEANUP_PHASE);
				break;
			case MANUAL_ONLY:
				opportunityMatches = (opportunity == DropOpportunity.MANUAL_INVOCATION);
				break;
			case NEVER:
			default:
				opportunityMatches = false;
				break;
		}

		if (!opportunityMatches)
		{
			status.decision = DropDecision.SKIP_NOT_AN_OPPORTUNITY;
			status.lastFailureReason = "Current opportunity (" + opportunity.name() + ") does not match mode (" + mode.name() + ")";
			return status;
		}

		List<DropCandidate> candidates = new ArrayList<>();
		List<DropCandidate> protectedSkipped = new ArrayList<>();

		Item[] items = inventory.getItems();
		for (int slot = 0; slot < items.length; slot++)
		{
			Item item = items[slot];
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}

			String itemName = getItemName(item.getId());
			int geValue = getGeValue(item.getId());
			boolean isProtected = itemSafetyService.isProtectedItem(itemName, item.getId(), protectedItems);

			if (isProtected)
			{
				protectedSkipped.add(new DropCandidate(slot, item.getId(), itemName, item.getQuantity(), geValue, true));
				continue;
			}

			if (geValue > maxDropValue)
			{
				protectedSkipped.add(new DropCandidate(slot, item.getId(), itemName, item.getQuantity(), geValue, true));
				continue;
			}

			boolean isInAllowlist = itemSafetyService.isAllowedToDrop(itemName, item.getId(), dropAllowlist, protectedItems, maxDropValue);
			if (isInAllowlist || dropAllowlist == null || dropAllowlist.trim().isEmpty())
			{
				candidates.add(new DropCandidate(slot, item.getId(), itemName, item.getQuantity(), geValue, false));
			}
			else
			{
				protectedSkipped.add(new DropCandidate(slot, item.getId(), itemName, item.getQuantity(), geValue, true));
			}
		}

		status.candidates = candidates;
		status.protectedSkipped = protectedSkipped;

		if (candidates.isEmpty())
		{
			status.decision = DropDecision.SKIP_NO_CANDIDATES;
			status.lastFailureReason = "No droppable candidates found (all items protected or above max value)";
			return status;
		}

		status.decision = DropDecision.DROP_ALLOWED;
		return status;
	}

	private int countOccupiedSlots(ItemContainer inventory)
	{
		int count = 0;
		Item[] items = inventory.getItems();
		if (items == null)
		{
			return 0;
		}
		for (Item item : items)
		{
			if (item != null && item.getId() > 0 && item.getQuantity() > 0)
			{
				count++;
			}
		}
		return count;
	}

	private String getItemName(int itemId)
	{
		try
		{
			net.runelite.api.ItemComposition composition = itemManager.getItemComposition(itemId);
			if (composition != null)
			{
				return composition.getName();
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to get item name for id {}", itemId, e);
		}
		return "";
	}

	private int getGeValue(int itemId)
	{
		try
		{
			return itemManager.getItemPrice(itemId);
		}
		catch (Exception e)
		{
			log.warn("Failed to get GE value for item {}", itemId, e);
			return Integer.MAX_VALUE;
		}
	}
}
