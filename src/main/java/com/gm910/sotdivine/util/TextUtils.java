package com.gm910.sotdivine.util;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;

/**
 * For text components and other things
 */
public class TextUtils {

	public static final char INFINITY = '\u221e';

	private TextUtils() {
	}

	public static String printMinMaxBounds(MinMaxBounds<?> bounds) {
		return bounds.min().map((x) -> "[" + x).orElse("(-" + INFINITY) + ","
				+ bounds.max().map((x) -> x + "]").orElse(INFINITY + ")");
	}

	/**
	 * So we can easily obtain this and replace with translate later
	 * 
	 * @param string
	 * @return
	 */
	public static Component literal(String string) {
		return Component.literal(string);
	}

	/**
	 * Just for simplicity idk; prefixes "sotd" to the beginning if it is not there
	 * already
	 * 
	 * @param string
	 * @return
	 */
	public static Component transPrefix(String string, Object... list) {
		if (!string.startsWith("sotd.")) {
			string = "sotd." + string;
		}
		return Component.translatableEscape(string, list);
	}

}
