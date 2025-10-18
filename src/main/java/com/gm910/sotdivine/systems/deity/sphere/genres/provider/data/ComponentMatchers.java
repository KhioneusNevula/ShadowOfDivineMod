package com.gm910.sotdivine.systems.deity.sphere.genres.provider.data;

import java.util.HashMap;
import java.util.Map;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.data.components.BannerPatternComponentMatcher;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.data.components.EnchantmentsComponentMatcher;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.data.components.IComponentMatcherProvider;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;

import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.resources.ResourceLocation;

public class ComponentMatchers {

	private static final Map<ResourceLocation, Codec<? extends IComponentMatcherProvider<?>>> map = new HashMap<>();

	private ComponentMatchers() {
	}

	public static void registerInit() {
		registerCodec(EnchantmentsComponentMatcher.PATH, CodecUtils.listOrSingleCodec(EnchantmentPredicate.CODEC)
				.xmap(EnchantmentsComponentMatcher::new, EnchantmentsComponentMatcher::enchantments));
		registerCodec(BannerPatternComponentMatcher.PATH, BannerPatternComponentMatcher.codec());
	}

	public static <T, A extends IComponentMatcherProvider<T>> void registerCodec(ResourceLocation type, Codec<A> codec) {
		map.put(type, codec);
	}

	public static <T, A extends IComponentMatcherProvider<T>> Codec<A> getCodec(ResourceLocation type) {
		return (Codec<A>) map.get(type);
	}

}
