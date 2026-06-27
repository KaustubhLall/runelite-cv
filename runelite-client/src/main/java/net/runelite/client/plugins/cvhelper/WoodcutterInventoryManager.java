/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelper;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class WoodcutterInventoryManager
{
	@Inject
	private Client client;

	@Inject
	private ItemSafetyService itemSafetyService;

	public enum InventoryDisposalDecision
	{
		NONE_NEEDED,
		DROP_REQUIRED,
		BANK_REQUIRED,
		BLOCKED_FULL_NO_SAFE_ACTION,
		INVENTORY_UNKNOWN
	}

	public boolean isInventoryAvailable()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		return inventory != null;
	}

	private List<ItemWithSlot> inventoryItems()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null || inventory.getItems() == null)
		{
			return new ArrayList<>();
		}

		List<ItemWithSlot> out = new ArrayList<>();
		Item[] items = inventory.getItems();
		for (int slot = 0; slot < items.length; slot++)
		{
			Item item = items[slot];
			if (item != null && item.getId() > 0 && item.getQuantity() > 0)
			{
				out.add(new ItemWithSlot(slot, item));
			}
		}
		return out;
	}

	public int getOccupiedSlots()
	{
		return inventoryItems().size();
	}

	public boolean isInventoryFull()
	{
		return getOccupiedSlots() >= 28;
	}

	public boolean shouldHandleInventory(int triggerSlots, CvHelperWoodcutterInventoryMode mode)
	{
		if (mode == CvHelperWoodcutterInventoryMode.NONE)
		{
			return false;
		}
		int occupied = getOccupiedSlots();
		return occupied >= triggerSlots;
	}

	public InventoryDisposalDecision getDisposalDecision(int triggerSlots, CvHelperWoodcutterInventoryMode mode, 
		String dropAllowlist, String userProtectedItems, int maxDropValue)
	{
		if (!isInventoryAvailable())
		{
			return InventoryDisposalDecision.INVENTORY_UNKNOWN;
		}

		int occupied = getOccupiedSlots();
		if (occupied < triggerSlots)
		{
			return InventoryDisposalDecision.NONE_NEEDED;
		}

		if (mode == CvHelperWoodcutterInventoryMode.NONE)
		{
			return InventoryDisposalDecision.BLOCKED_FULL_NO_SAFE_ACTION;
		}

		if (mode == CvHelperWoodcutterInventoryMode.BANK)
		{
			return InventoryDisposalDecision.BANK_REQUIRED;
		}

		if (mode == CvHelperWoodcutterInventoryMode.DROP)
		{
			List<InventorySlot> droppable = getSafeDroppableSlots(dropAllowlist, userProtectedItems, maxDropValue);
			if (droppable.isEmpty())
			{
				return InventoryDisposalDecision.BLOCKED_FULL_NO_SAFE_ACTION;
			}
			return InventoryDisposalDecision.DROP_REQUIRED;
		}

		return InventoryDisposalDecision.BLOCKED_FULL_NO_SAFE_ACTION;
	}

	public List<InventorySlot> getSafeDroppableSlots(String dropAllowlist, String userProtectedItems, int maxDropValue)
	{
		List<InventorySlot> droppable = new ArrayList<>();
		for (ItemWithSlot itemWithSlot : inventoryItems())
		{
			Item item = itemWithSlot.item();
			String itemName = getItemName(item.getId());
			if (itemSafetyService.isAllowedToDrop(itemName, item.getId(), dropAllowlist, userProtectedItems, maxDropValue))
			{
				droppable.add(new InventorySlot(itemWithSlot.slot(), item.getId(), itemName, item.getQuantity()));
			}
		}
		return droppable;
	}

	public List<InventorySlot> getProtectedSlots(String userProtectedItems)
	{
		List<InventorySlot> protectedSlots = new ArrayList<>();
		for (ItemWithSlot itemWithSlot : inventoryItems())
		{
			Item item = itemWithSlot.item();
			String itemName = getItemName(item.getId());
			if (itemSafetyService.isProtectedItem(itemName, item.getId(), userProtectedItems))
			{
				protectedSlots.add(new InventorySlot(itemWithSlot.slot(), item.getId(), itemName, item.getQuantity()));
			}
		}
		return protectedSlots;
	}

	public List<ItemWithSlot> getInventoryItems()
	{
		return inventoryItems();
	}

	public boolean canDrop(Item item, String dropAllowlist, String userProtectedItems, int maxDropValue)
	{
		if (item == null)
		{
			return false;
		}
		String itemName = getItemName(item.getId());
		return itemSafetyService.isAllowedToDrop(itemName, item.getId(), dropAllowlist, userProtectedItems, maxDropValue);
	}

	public boolean canBank(Item item, String bankAllowlist)
	{
		if (item == null)
		{
			return false;
		}
		String itemName = getItemName(item.getId());
		return itemSafetyService.isAllowedToBank(itemName, item.getId(), bankAllowlist);
	}

	private String getItemName(int itemId)
	{
		try
		{
			net.runelite.api.ItemComposition composition = client.getItemDefinition(itemId);
			if (composition != null)
			{
				return composition.getName();
			}
		}
		catch (Exception e)
		{
			return "";
		}
		return "";
	}

	public static class InventorySlot
	{
		private final int index;
		private final int itemId;
		private final String itemName;
		private final int quantity;

		public InventorySlot(int index, int itemId, String itemName, int quantity)
		{
			this.index = index;
			this.itemId = itemId;
			this.itemName = itemName;
			this.quantity = quantity;
		}

		public int index()
		{
			return index;
		}

		public int itemId()
		{
			return itemId;
		}

		public String itemName()
		{
			return itemName;
		}

		public int quantity()
		{
			return quantity;
		}
	}

	public static class ItemWithSlot
	{
		private final int slot;
		private final Item item;

		ItemWithSlot(int slot, Item item)
		{
			this.slot = slot;
			this.item = item;
		}

		public int slot()
		{
			return slot;
		}

		public Item item()
		{
			return item;
		}
	}
}
