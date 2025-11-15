package com.gm910.sotdivine.concepts.genres;

import java.util.Collection;

import com.google.common.collect.Range;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

/**
 * A genre for a sphere
 */
public sealed interface IGenreType<T> permits GenreTypes.GenreType {

	public ResourceLocation resourceLocation();

	public Class<? super T> genreClass();

	public Codec<T> classCodec();

	public Codec<Collection<?>> typelessGenreSetCodec();

	public Codec<Collection<T>> genreSetCodec();

	/**
	 * Range of how many items this genre permits specifying, e.g. a genre may
	 * permit a max of 1 item, or require exactly 1 item
	 * 
	 * @return
	 */
	public Range<Integer> amountPermitted();

	/**
	 * Whether an emanation of this genre can be given to something with inventory
	 * 
	 * @return
	 */
	public boolean isGiveable();

	/**
	 * Whether an emanation of this genre can be created in the world
	 * 
	 * @return
	 */
	public boolean isPlaceable();

	public static void init() {

	}

}
