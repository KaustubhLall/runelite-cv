/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

public enum CvHelperWoodcutterState
{
	IDLE,
	CHECK_LOGIN_READY,
	FIND_TREE,
	WALK_TO_TREE,
	CHOPPING,
	WAITING_FOR_CHOP,
	INVENTORY_CHECK,
	DROP_ITEMS,
	BANK_REQUIRED,
	BANKING,
	BLOCKED,
	ERROR
}
