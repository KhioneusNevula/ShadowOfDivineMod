package com.gm910.sotdivine.systems.deity.emanation;

import java.util.Optional;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.systems.deity.emanation.types.GiveEffectEmanation;
import com.gm910.sotdivine.systems.deity.emanation.types.ParticleEmanation;
import com.gm910.sotdivine.systems.deity.emanation.types.combined.CombinedEmanation;
import com.gm910.sotdivine.systems.deity.emanation.types.spawn.SpawnEmanation;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister.RegistryHolder;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

/**
 * Emanation types are different paradigms for how list might operate
 */
public class EmanationType<T extends IEmanation> {

	private static final BiMap<ResourceLocation, EmanationType<?>> BUILTIN_MAP = HashBiMap.create();

	private static final RegistryHolder<EmanationType<?>> REGISTRY_HOLDER = SOTDMod.EMANATION_TYPES
			.makeRegistry(() -> RegistryBuilder.of());

	private static Codec<IEmanation> D_CODEC = null;

	private static Optional<Codec<IEmanation>> O_CODEC = Optional.empty();

	public static final RegistryObject<EmanationType<GiveEffectEmanation>> GIVE_EFFECT = register("give_effect",
			GiveEffectEmanation.CODEC);
	public static final RegistryObject<EmanationType<ParticleEmanation>> PARTICLE = register("particle",
			ParticleEmanation.CODEC);
	public static final RegistryObject<EmanationType<SpawnEmanation>> SPAWN = register("spawn", SpawnEmanation.CODEC);
	public static final RegistryObject<EmanationType<CombinedEmanation>> COMBINED = register("combined",
			CombinedEmanation.CODEC);

	public static void init() {
	}

	public static Codec<EmanationType<?>> typeCodec() {
		return REGISTRY_HOLDER.get().getCodec();
	}

	/**
	 * Get the codec for party resources
	 * 
	 * @return
	 */
	public static Codec<IEmanation> resourceCodec() {
		if (O_CODEC.isEmpty()) {
			if (REGISTRY_HOLDER.get() == null) {
				if (D_CODEC == null) {
					D_CODEC = ResourceLocation.CODEC.xmap(BUILTIN_MAP::get, BUILTIN_MAP.inverse()::get).dispatch("type",
							(e) -> (EmanationType) e.getEmanationType(), (x) -> x.codec().fieldOf("data"));
				}
				return D_CODEC;
			} else {
				O_CODEC = Optional.of(REGISTRY_HOLDER.get()).map((f) -> f.getCodec())
						.map((f) -> f.dispatch("type", IEmanation::getEmanationType, (x) -> x.codec().fieldOf("data")));
			}
		}
		return O_CODEC.get();
	}

	public static <T extends IEmanation> RegistryObject<EmanationType<T>> register(String id, Codec<T> codec) {
		SOTDMod.LOGGER.debug("Registering emanation type " + id + " to registry " + SOTDMod.EMANATION_TYPES);
		var emType = new EmanationType<T>(codec);
		BUILTIN_MAP.put(ModUtils.path(id), emType);
		return SOTDMod.EMANATION_TYPES.register(id, () -> emType);
	}

	private Codec<T> mcodec;

	private EmanationType(Codec<T> codec) {
		this.mcodec = codec;
	}

	public Codec<T> codec() {
		return mcodec;
	}

}
