package com.gm910.sotdivine.util;

import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import net.minecraft.util.RandomSource;

public class RandomUtils {

	private RandomUtils() {
	}

	/**
	 * Chooses random element from stream
	 * 
	 * @param <T>
	 * @param random
	 * @param stream
	 * @return
	 */
	public static <T> Optional<T> choose(RandomSource random, Stream<? extends T> stream, int size) {
		if (size == 0)
			return Optional.empty();
		int index = size == 1 ? 1 : random.nextIntBetweenInclusive(1, size);
		return stream.map((s) -> (T) s).limit(index).findFirst();
	}

	/**
	 * Chooses random element from collection
	 * 
	 * @param <T>
	 * @param random
	 * @param collection
	 * @return
	 */
	public static <T> Optional<T> choose(RandomSource random, Collection<? extends T> collection) {
		return choose(random, collection.stream(), collection.size());
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
