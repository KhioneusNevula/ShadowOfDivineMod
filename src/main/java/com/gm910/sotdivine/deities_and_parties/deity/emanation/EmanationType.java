package com.gm910.sotdivine.deities_and_parties.deity.emanation;

import java.util.Optional;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.types.GiveEffectEmanation;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.types.ParticleEmanation;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.types.combined.CombinedEmanation;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.types.spawn.SpawnEmanation;
import com.gm910.sotdivine.registries.ModRegistries;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.logging.LogUtils;
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

	public final static RegistryHolder<EmanationType<?>> REGISTRY = SOTDMod.EMANATION_TYPES.makeRegistry(
			() -> RegistryBuilder.<EmanationType<?>>of(ModRegistries.EMANATION_TYPES.location()).allowModification());

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
		LogUtils.getLogger().debug("Initializing emanation types");
	}

	public static Codec<EmanationType<?>> typeCodec() {
		return REGISTRY.get().getCodec();
	}

	/**
	 * Get the codec for party resources
	 * 
	 * @return
	 */
	public static Codec<IEmanation> resourceCodec() {
		if (O_CODEC.isEmpty()) {
			if (REGISTRY.get() == null) {
				if (D_CODEC == null) {
					D_CODEC = ResourceLocation.CODEC.xmap(BUILTIN_MAP::get, BUILTIN_MAP.inverse()::get).dispatch("type",
							(e) -> (EmanationType) e.getEmanationType(), (x) -> x.codec().fieldOf("data"));
				}
				return D_CODEC;
			} else {
				O_CODEC = Optional.of(REGISTRY.get()).map((f) -> f.getCodec())
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
