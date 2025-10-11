package com.gm910.sotdivine.systems.deity.sphere;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.Range;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import net.minecraft.advancements.critereon.DataComponentMatchers;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.dimension.DimensionType;

public class Genres {

	static final Map<ResourceLocation, IGenre<?>> GENRES = new HashMap<>();
	private static Codec<IGenre<?>> CODEC = null;

	/**
	 * Deserialized as either an item or a predicate
	 */
	public static final Genre<ItemPredicate> OFFERING = register(ModUtils.path("offering"), ItemPredicate.class,
			Codec.either(ItemStack.CODEC,
					ItemPredicate.CODEC).xmap(
							(a) -> a.left().isPresent()
									? ItemPredicate.Builder.item().of(null, a.left().get().getItem())
											.withCount(MinMaxBounds.Ints.exactly(a.left().get().getCount()))
											.withComponents(DataComponentMatchers.Builder.components()
													.exact(DataComponentExactPredicate
															.allOf(a.left().get().getComponents()))
													.build())
											.build()
									: a.right().get(),
							(b) -> Either.right(b)),
			Range.atLeast(0));

	public static final Genre<ItemPredicate> DRUG = registerCopy(OFFERING, ModUtils.path("drug"));

	public static final Genre<ResourceKey<DimensionType>> DIMENSION = register(ModUtils.path("dimension"),
			ResourceKey.class, ResourceKey.codec(Registries.DIMENSION_TYPE), Range.atLeast(0));

	private Genres() {
	}

	public static <T> Genre<T> registerCopy(Genre<T> offering2, ResourceLocation id) {
		return register(id, offering2.clazz, offering2.codec, offering2.amountPermitted());
	}

	/**
	 * Register a genre
	 * 
	 * @param <T>
	 * @param id
	 * @param sphere
	 * @return
	 */

	public static <T> Genre<T> register(ResourceLocation id, Class<? super T> clazz, Codec<T> codec,
			Range<Integer> amountPermitted) {
		var gen = new Genre<T>(id, clazz, codec, amountPermitted);
		GENRES.put(id, gen);
		return gen;
	}

	public static void init() {

	}

	/**
	 * Return genre codec
	 * 
	 * @return
	 */
	public static Codec<IGenre<?>> genreCodec() {
		if (CODEC == null) {
			CODEC = ResourceLocation.CODEC.xmap(GENRES::get, IGenre::resourceLocation);
		}
		return CODEC;
	}

	static final class Genre<T> implements IGenre<T> {

		private ResourceLocation name;
		private Class<? super T> clazz;
		private Codec<T> codec;
		private Codec<Collection<T>> setCodec;
		private Range<Integer> ap;

		Genre(ResourceLocation name, Class<? super T> clazz, Codec<T> codec, Range<Integer> amountPermitted) {
			this.name = name;
			this.clazz = clazz;
			this.codec = codec;
			this.ap = amountPermitted;
			this.setCodec = Codec.list(codec).xmap((l) -> new HashSet<>(l), ArrayList::new);
		}

		@Override
		public ResourceLocation resourceLocation() {
			return name;
		}

		@Override
		public Class<? super T> genreClass() {
			return clazz;
		}

		@Override
		public Codec<T> classCodec() {
			return this.codec;
		}

		@Override
		public Codec<Collection<T>> genreSetCodec() {
			return this.setCodec;
		}

		@Override
		public Range<Integer> amountPermitted() {
			return ap;
		}

		@Override
		public String toString() {
			return this.name + "(" + this.clazz.getSimpleName() + ")";
		}
	}

}
