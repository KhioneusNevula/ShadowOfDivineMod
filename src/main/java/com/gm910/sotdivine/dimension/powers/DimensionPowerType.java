package com.gm910.sotdivine.dimension.powers;

import java.util.Optional;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.dimension.powers.types.GhostResurrectionPower;
import com.gm910.sotdivine.dimension.powers.types.RandomLevitationPower;
import com.gm910.sotdivine.dimension.powers.types.UndyingPower;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister.RegistryHolder;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

/**
 * A type of power that may exist in a dimension
 */
public class DimensionPowerType<T extends IDimensionPower> {

	private static final BiMap<ResourceLocation, DimensionPowerType<?>> BUILTIN_MAP = HashBiMap.create();

	public final static RegistryHolder<DimensionPowerType<?>> REGISTRY = SOTDMod.DIMENSION_POWER_TYPES
			.makeRegistry(() -> RegistryBuilder
					.<DimensionPowerType<?>>of(ModRegistries.DIMENSION_POWER_TYPES.location()).allowModification());

	private static Codec<IDimensionPower> D_CODEC = null;

	private static Optional<Codec<IDimensionPower>> O_CODEC = Optional.empty();

	/**
	 * Debug power; gives random levitation periodically
	 */
	public static final RegistryObject<DimensionPowerType<RandomLevitationPower>> RANDOM_LEVITATION = register(
			"random_levitation", RandomLevitationPower.CODEC);

	/**
	 * Stops all things from dying
	 */
	public static final RegistryObject<DimensionPowerType<UndyingPower>> UNDYING = register("undying",
			UndyingPower.CODEC);

	/**
	 * REsurrects ghosts periodically
	 */
	public static final RegistryObject<DimensionPowerType<GhostResurrectionPower>> GHOST_RESURRECTION = register(
			"ghost_resurrection", GhostResurrectionPower.CODEC);

	public static void init() {
		LogUtils.getLogger().debug("Initializing DimensionPower types");
	}

	public static Codec<DimensionPowerType<?>> typeCodec() {
		return REGISTRY.get().getCodec();
	}

	/**
	 * Get the codec for party resources
	 * 
	 * @return
	 */
	public static Codec<IDimensionPower> resourceCodec() {
		if (O_CODEC.isEmpty()) {
			if (REGISTRY.get() == null) {
				if (D_CODEC == null) {
					D_CODEC = ResourceLocation.CODEC.xmap(BUILTIN_MAP::get, BUILTIN_MAP.inverse()::get).dispatch("type",
							(e) -> (DimensionPowerType) e.getDimensionPowerType(), (x) -> x.codec());
				}
				return D_CODEC;
			} else {
				O_CODEC = Optional.of(REGISTRY.get()).map((f) -> f.getCodec())
						.map((f) -> f.dispatch("type", IDimensionPower::getDimensionPowerType, (x) -> x.codec()));
			}
		}
		return O_CODEC.get();
	}

	public static <T extends IDimensionPower> RegistryObject<DimensionPowerType<T>> register(String id,
			MapCodec<T> codec) {
		LogUtils.getLogger()
				.debug("Registering DimensionPower type " + id + " to registry " + SOTDMod.DIMENSION_POWER_TYPES);
		var emType = new DimensionPowerType<T>(codec);
		BUILTIN_MAP.put(ModUtils.path(id), emType);
		return SOTDMod.DIMENSION_POWER_TYPES.register(id, () -> emType);
	}

	private MapCodec<T> mcodec;

	private DimensionPowerType(MapCodec<T> codec) {
		this.mcodec = codec;
	}

	public MapCodec<T> codec() {
		return mcodec;
	}

}
