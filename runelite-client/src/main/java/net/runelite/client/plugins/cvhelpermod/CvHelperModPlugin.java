/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Modular rebuild of the CV Helper plugin.
 *
 * <p>This is a verbatim decomposition of the reference {@code cvhelper} plugin into maintainable
 * modules (see {@code ai-docs/modular-architecture.md}). It serves the identical localhost HTTP
 * contract so the existing verifier dashboard and saved settings keep working unchanged. It shares
 * the {@code "cvhelper"} config group so settings transfer seamlessly at cutover.
 *
 * <p>Disabled by default while it is built brick-by-brick alongside the still-working reference
 * plugin; enable it in the plugin manager to test. It will be flipped on (and the reference retired)
 * once full parity is verified in-game.
 */
@PluginDescriptor(
	name = "CV Helper (Modular)",
	description = "Modular rebuild of CV Helper: UI geometry export, captures, and guarded automation over localhost.",
	tags = {"overlay", "ui", "coordinates", "debug", "automation"},
	enabledByDefault = false
)
@Slf4j
public class CvHelperModPlugin extends Plugin
{
	@Provides
	CvHelperModConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CvHelperModConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.debug("CV Helper (Modular) starting up");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("CV Helper (Modular) shutting down");
	}
}
