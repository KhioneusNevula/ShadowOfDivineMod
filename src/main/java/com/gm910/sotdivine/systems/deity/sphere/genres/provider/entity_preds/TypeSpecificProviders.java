package com.gm910.sotdivine.systems.deity.sphere.genres.provider.entity_preds;

import java.util.HashMap;
import java.util.Map;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.ItemGenreProvider;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class TypeSpecificProviders {

	private static final Map<ResourceLocation, Codec<? extends ITypeSpecificProvider<?>>> map = new HashMap<>();

	private TypeSpecificProviders() {
	}

	public static void registerInit() {
		registerCodec(ResourceLocation.withDefaultNamespace("item_frame_stack"),
				ItemGenreProvider.codec().xmap(ItemFrameStack::new, ItemFrameStack::item));
	}

	public static <E extends Entity, A extends ITypeSpecificProvider<E>> void registerCodec(ResourceLocation type,
			Codec<A> codec) {
		map.put(type, codec);
	}

	public static <T extends Entity, E extends ITypeSpecificProvider<T>> Codec<E> getCodec(ResourceLocation type) {
		return (Codec<E>) map.get(type);
	}

}
