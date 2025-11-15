package com.gm910.sotdivine.concepts.genres.provider.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.gm910.sotdivine.concepts.genres.provider.data.components.BannerPatternComponentMatcher;
import com.gm910.sotdivine.concepts.genres.provider.data.components.EnchantmentsComponentMatcher;
import com.gm910.sotdivine.concepts.genres.provider.data.components.IComponentMatcherProvider;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.resources.ResourceLocation;

public class CodecsComponentMatchers {

	private static final Map<ResourceLocation, Codec<? extends IComponentMatcherProvider<?>>> map = new HashMap<>();

	private CodecsComponentMatchers() {
	}

	public static void registerInit() {
		registerCodec(EnchantmentsComponentMatcher.PATH,
				CodecUtils.multiCodecEither(
						CodecUtils.listOrSingleCodec(EnchantmentPredicate.CODEC).xmap(EnchantmentsComponentMatcher::new,
								EnchantmentsComponentMatcher::enchantments),
						Codec.STRING.flatXmap(
								(s) -> s.equalsIgnoreCase("any") || s.equalsIgnoreCase("exists")
										? DataResult.success(new EnchantmentsComponentMatcher())
										: DataResult.error(
												() -> "Invalid keyword; should by 'any' or proper specification"),
								(s) -> s.enchantments().isEmpty() ? DataResult.success("any")
										: DataResult.error(() -> "Non-empty list cannot be stringed"))));
		registerCodec(BannerPatternComponentMatcher.PATH, BannerPatternComponentMatcher.codec());
	}

	public static <T, A extends IComponentMatcherProvider<T>> void registerCodec(ResourceLocation type,
			Codec<A> codec) {
		map.put(type, codec);
	}

	public static <A extends IComponentMatcherProvider<?>> Codec<A> getCodec(ResourceLocation type) {
		return (Codec<A>) map.get(type);
	}

}
