package com.gm910.sotdivine.util;

import java.util.Random;

import net.minecraft.util.RandomSource;

public class RandomUtils {

	private RandomUtils() {
	}

	/**
	 * Return a random int in the given range, biased to pick lower numbers more
	 * 
	 * @param source
	 * @param lower
	 * @param upper
	 * @return
	 */
	public static int lowBiasedRandomInt(Random source, int lower, int upper) {
		int o1 = source.nextInt(lower, upper);
		int o2 = source.nextInt(lower, upper);
		return Math.min(o1, o2);
	}

	/**
	 * Return a random int in the given range, biased to pick lower numbers more
	 * 
	 * @param source
	 * @param lower
	 * @param upper
	 * @return
	 */
	public static int lowBiasedRandomInt(RandomSource source, int lower, int upper) {
		int o1 = source.nextInt(lower, upper);
		int o2 = source.nextInt(lower, upper);
		return Math.min(o1, o2);
	}

	/**
	 * Return a random int in the given range, biased to pick higher numbers more
	 * 
	 * @param source
	 * @param lower
	 * @param upper
	 * @return
	 */
	public static int highBiasedRandomInt(Random source, int lower, int upper) {
		int o1 = source.nextInt(lower, upper);
		int o2 = source.nextInt(lower, upper);
		return Math.max(o1, o2);
	}

	/**
	 * Return a random int in the given range, biased to pick higher numbers more
	 * 
	 * @param source
	 * @param lower
	 * @param upper
	 * @return
	 */
	public static int highBiasedRandomInt(RandomSource source, int lower, int upper) {
		int o1 = source.nextInt(lower, upper);
		int o2 = source.nextInt(lower, upper);
		return Math.max(o1, o2);
	}

}
