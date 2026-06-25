/*
 * Copyright (c) 2026
 * All rights reserved.
 */
package net.runelite.client.plugins.cvhelpermod;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Pure JSON/value helpers shared across cvhelpermod. Static so concern services can
 * `import static ...CvJson.*` and keep verbatim-ported call sites byte-identical.
 */
final class CvJson
{
	private CvJson() {}

	public static Map<String, Object> boundsMap(Rectangle bounds)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("x", bounds.x);
		out.put("y", bounds.y);
		out.put("width", bounds.width);
		out.put("height", bounds.height);
		return out;
	}

	public static Map<String, Object> pointMap(int x, int y)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("x", x);
		out.put("y", y);
		return out;
	}

	public static long longValue(Object value)
	{
		return value instanceof Number ? ((Number) value).longValue() : 0L;
	}

	public static int intValue(Object value, int fallback)
	{
		return value instanceof Number ? ((Number) value).intValue() : fallback;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> mapValue(Object value)
	{
		return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
	}

	public static String friendlyName(String value)
	{
		String[] words = value.toLowerCase().split("_");
		StringBuilder out = new StringBuilder();
		for (String word : words)
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (out.length() > 0)
			{
				out.append(' ');
			}
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return out.toString();
	}

	public static String actionsText(Object actions)
	{
		if (actions instanceof String[])
		{
			return Arrays.toString((String[]) actions);
		}
		return actions == null ? "" : String.valueOf(actions);
	}

	public static String normalize(String value)
	{
		return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	public static <T> T safeValue(java.util.function.Supplier<T> supplier, T fallback)
	{
		try
		{
			return supplier.get();
		}
		catch (RuntimeException e)
		{
			return fallback;
		}
	}

}
