package com.gm910.sotdivine.concepts.genres.provider.entity_preds;

import java.util.HashMap;
import java.util.Map;

import com.gm910.sotdivine.concepts.genres.provider.independent.ItemGenreProvider;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class CodecsTypeSpecificProviders {

	private static final Map<ResourceLocation, Codec<? extends ITypeSpecificProvider<?>>> map = new HashMap<>();

	private static Codec<ITypeSpecificProvider<?>> CODEC = null;

	public static Codec<ITypeSpecificProvider<?>> codec() {
		if (CODEC == null) {
			CODEC = ResourceLocation.CODEC.dispatch(ITypeSpecificProvider::path, (s) -> map.get(s).fieldOf("data"));
		}
		return CODEC;
	}

	private CodecsTypeSpecificProviders() {
	}

	public static void registerInit() {
		registerCodec(ItemFrameStack.PATH, ItemGenreProvider.codec().xmap(ItemFrameStack::new, ItemFrameStack::item));
		registerCodec(IsWorshiper.PATH, Codec.STRING.xmap(IsWorshiper::new, IsWorshiper::name));
	}

	public static <E extends Entity, A extends ITypeSpecificProvider<E>> void registerCodec(ResourceLocation type,
			Codec<A> codec) {
		map.put(type, codec);
	}

	public static <T extends Entity, E extends ITypeSpecificProvider<T>> Codec<E> getCodec(ResourceLocation type) {
		return (Codec<E>) map.get(type);
	}

}
