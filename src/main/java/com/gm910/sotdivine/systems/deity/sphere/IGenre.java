package com.gm910.sotdivine.systems.deity.sphere;

import java.util.Collection;

import com.google.common.collect.Range;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

/**
 * A genre for a sphere
 */
public sealed interface IGenre<T> permits Genres.Genre {

	public ResourceLocation resourceLocation();

	public Class<? super T> genreClass();

	public Codec<T> classCodec();

	public Codec<Collection<T>> genreSetCodec();

	/**
	 * Range of how many items this genre permits specifying, e.g. a genre may
	 * permit a max of 1 item, or require exactly 1 item
	 * 
	 * @return
	 */
	public Range<Integer> amountPermitted();

	public static void init() {

	}

}
