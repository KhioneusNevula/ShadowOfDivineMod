package com.gm910.sotdivine.concepts.genres.provider;

import java.util.HashMap;
import java.util.Map;

import com.gm910.sotdivine.concepts.genres.provider.entity.EntityFlagsGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.entity.EntityTypeProvider;
import com.gm910.sotdivine.concepts.genres.provider.entity.EquipmentGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.entity.MobEffectGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.BlockGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.EntityGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.ItemGenreProvider;
import com.google.common.base.Supplier;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

/**
 * A provider type (this class exists for encoding/decoding purposes)
 * 
 * @param <T>
 */
public record ProviderType<T extends IGenreProvider<?, ?>>(ResourceLocation path, Class<T> providerClass,
		Supplier<Codec<T>> codec) {

	private static final Map<ResourceLocation, ProviderType<?>> TYPES = new HashMap<>();

	/**
	 * Provider type for items
	 */
	public static final ProviderType<ItemGenreProvider> ITEM = ProviderType.register(
			ResourceLocation.withDefaultNamespace("item"), ItemGenreProvider.class, () -> ItemGenreProvider.codec());
	/**
	 * Provider type for entities
	 */
	public static final ProviderType<EntityGenreProvider> ENTITY = ProviderType.register(
			ResourceLocation.withDefaultNamespace("entity"), EntityGenreProvider.class,
			() -> EntityGenreProvider.codec());
	/**
	 * Provider type for blocks
	 */
	public static final ProviderType<BlockGenreProvider> BLOCK = ProviderType.register(
			ResourceLocation.withDefaultNamespace("block"), BlockGenreProvider.class, () -> BlockGenreProvider.codec());
	/**
	 * Provider type that selects for entities based on flags
	 */
	public static final ProviderType<EntityFlagsGenreProvider> ENTITY_FLAGS = ProviderType.register(
			ResourceLocation.withDefaultNamespace("entity_flags"), EntityFlagsGenreProvider.class,
			() -> EntityFlagsGenreProvider.CODEC);
	/**
	 * Provider type that selects for entities based on equipment
	 */
	public static final ProviderType<EquipmentGenreProvider> EQUIPMENT = ProviderType.register(
			ResourceLocation.withDefaultNamespace("equipment"), EquipmentGenreProvider.class,
			() -> EquipmentGenreProvider.codec());
	/**
	 * Provider type that selects for entities based on status effects
	 */
	public static final ProviderType<MobEffectGenreProvider> MOB_EFFECT_TYPE = ProviderType.register(
			ResourceLocation.withDefaultNamespace("mob_effect"), MobEffectGenreProvider.class,
			() -> MobEffectGenreProvider.codec());

	public static final ProviderType<EntityTypeProvider> ENTITY_TYPE = ProviderType.register(
			ResourceLocation.withDefaultNamespace("entity_type"), EntityTypeProvider.class,
			() -> EntityTypeProvider.codec());

	private static final Codec<ProviderType<?>> CODEC = ResourceLocation.CODEC.xmap(TYPES::get, ProviderType::path);
	static final Codec<IGenreProvider<?, ?>> DISPATCH_CODEC = CODEC.dispatch(IGenreProvider::providerType,
			(pt) -> pt.codec.get().fieldOf("provider_type"));

	public static Codec<ProviderType<?>> typeCodec() {
		return CODEC;
	}

	/**
	 * Register a new provider type
	 * 
	 * @param <A>
	 * @param <B>
	 * @param <T>
	 * @param pathType
	 * @param codec
	 * @return
	 */
	public static <A, B, T extends IGenreProvider<A, B>> ProviderType<T> register(ResourceLocation path, Class<T> clazz,
			Supplier<Codec<T>> codec) {
		var type = new ProviderType<>(path, clazz, codec);
		if (TYPES.containsKey(path)) {
			throw new IllegalArgumentException(
					"Duplicate entry for " + path + ": " + type + " vs existing entry " + TYPES.get(path));
		}
		TYPES.put(path, type);
		return type;
	}

}
