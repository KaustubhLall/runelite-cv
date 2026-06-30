/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * All burnable log types for the firemaking farmer, with their item IDs,
 * level requirements, and XP values (sourced from FiremakingAction.java).
 */
@AllArgsConstructor
@Getter
public enum CvHelperFiremakingLogType
{
	LOGS(ItemID.LOGS, "Logs", 1, 40),
	ACHEY_TREE_LOGS(ItemID.ACHEY_TREE_LOGS, "Achey tree logs", 1, 40),
	OAK_LOGS(ItemID.OAK_LOGS, "Oak logs", 15, 60),
	WILLOW_LOGS(ItemID.WILLOW_LOGS, "Willow logs", 30, 90),
	TEAK_LOGS(ItemID.TEAK_LOGS, "Teak logs", 35, 105),
	JATOBA_LOGS(ItemID.JATOBA_LOGS, "Jatoba logs", 40, 120),
	ARCTIC_PINE_LOGS(ItemID.ARCTIC_PINE_LOG, "Arctic pine logs", 42, 125),
	MAPLE_LOGS(ItemID.MAPLE_LOGS, "Maple logs", 45, 135),
	MAHOGANY_LOGS(ItemID.MAHOGANY_LOGS, "Mahogany logs", 50, 157),
	YEW_LOGS(ItemID.YEW_LOGS, "Yew logs", 60, 202),
	BLISTERWOOD_LOGS(ItemID.BLISTERWOOD_LOGS, "Blisterwood logs", 62, 96),
	CAMPHOR_LOGS(ItemID.CAMPHOR_LOGS, "Camphor logs", 66, 180),
	MAGIC_LOGS(ItemID.MAGIC_LOGS, "Magic logs", 75, 303),
	IRONWOOD_LOGS(ItemID.IRONWOOD_LOGS, "Ironwood logs", 80, 220),
	REDWOOD_LOGS(ItemID.REDWOOD_LOGS, "Redwood logs", 90, 350),
	ROSEWOOD_LOGS(ItemID.ROSEWOOD_LOGS, "Rosewood logs", 92, 268);

	private final int itemId;
	private final String displayName;
	private final int levelRequired;
	private final int xpPerLog;
}
