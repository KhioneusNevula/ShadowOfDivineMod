package com.gm910.sotdivine.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiPredicate;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Either;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderSet.Direct;

/**
 * Because the holder class is so weirdly designed
 */
public class HolderUtils {

	private HolderUtils() {
	}

	/**
	 * Check if two optionals are equal using the given function
	 * 
	 * @param <T>
	 * @param one
	 * @param two
	 * @param equality
	 * @return
	 */
	public static <T> boolean optionalEquals(Optional<T> one, Optional<T> two, BiPredicate<T, T> equality) {
		if (one.isEmpty() && two.isEmpty()) {
			return true;
		} else if (one.isPresent() && two.isPresent()) {
			return equality.test(one.get(), two.get());
		}
		return false;
	}

	/**
	 * Return if this holder set contains this item
	 * 
	 * @param <T>
	 * @param set
	 * @param item
	 * @return
	 */
	public static <T> boolean holderSetContains(HolderSet<T> set, Holder<T> item) {
		if (set instanceof HolderSet.Direct || item instanceof Holder.Direct) {
			return Streams.stream(set).anyMatch((r) -> r.is(item));
		}
		return set.contains(item);
	}

	/**
	 * Hash code for a holder
	 * 
	 * @param <T>
	 * @param holder
	 * @return
	 */
	public static <T> int holderHashCode(Holder<T> holder) {
		return holder.unwrap().map((x) -> x.location().hashCode(), (y) -> y.hashCode());
	}

	/**
	 * Hash code of a holder set
	 * 
	 * @param <T>
	 * @param set1
	 * @return
	 */
	public static <T> int holderSetHashCode(HolderSet<T> set1) {
		return Either.unwrap(set1.unwrap().mapLeft((tk) -> tk.location().hashCode())
				.mapRight((ls) -> Arrays.hashCode(ls.stream().map(HolderUtils::holderHashCode).toArray())));
	}

	/**
	 * Since Mojang's useless holder class is so badly designed, I'm adding this...
	 * 
	 * @param <T>
	 * @param set1
	 * @param set2
	 */
	public static <T> boolean holderSetEquals(HolderSet<T> set1, HolderSet<T> set2) {
		return Either.unwrap(set1.unwrap().mapLeft((l) -> {
			return Either.unwrap(set2.unwrap().mapLeft((l2) -> l.equals(l2)).mapRight((r2) -> false));
		}).mapRight((r) -> {
			return Either.unwrap(set2.unwrap().mapLeft((l2) -> false).mapRight((r2) -> {
				if (r.size() != r2.size())
					return false;
				for (int i = 0; i < r.size(); i++) {
					if (!r.get(i).is(r2.get(i))) {
						return false;
					}
				}
				return true;
			}));
		}));
	}

}
