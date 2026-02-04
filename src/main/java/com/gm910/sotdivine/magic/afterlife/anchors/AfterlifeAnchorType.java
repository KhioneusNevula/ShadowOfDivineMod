package com.gm910.sotdivine.magic.afterlife.anchors;

import java.util.Optional;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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
public class AfterlifeAnchorType<T extends IAfterlifeAnchor<?, ?>> {

	private static final BiMap<ResourceLocation, AfterlifeAnchorType<?>> BUILTIN_MAP = HashBiMap.create();

	public final static RegistryHolder<AfterlifeAnchorType<?>> REGISTRY = SOTDMod.ANCHOR_TYPES
			.makeRegistry(() -> RegistryBuilder.<AfterlifeAnchorType<?>>of(ModRegistries.ANCHOR_TYPES.location())
					.allowModification());

	private static Codec<IAfterlifeAnchor<?, ?>> D_CODEC = null;

	private static Optional<Codec<IAfterlifeAnchor<?, ?>>> O_CODEC = Optional.empty();

	/**
	 * Anchor which is a soul
	 */
	public static final RegistryObject<AfterlifeAnchorType<SoulAnchor>> SOUL = register("soul", SoulAnchor::getCodec);
	/**
	 * Anchor which is a blockMatcher
	 */
	public static final RegistryObject<AfterlifeAnchorType<BlockAnchor>> BLOCK = register("block",
			BlockAnchor::createCodec);

	/**
	 * Anchor which is a blockMatcher
	 */
	public static final RegistryObject<AfterlifeAnchorType<PositionAnchor>> POSITION = register("position",
			PositionAnchor::createCodec);

	/**
	 * Default kind of anchor
	 */
	public static final RegistryObject<AfterlifeAnchorType<IAfterlifeAnchor<Object, Object>>> DEFAULT = register(
			"default", () -> MapCodec.unit(IAfterlifeAnchor.DEFAULT));

	public static void init() {
		LogUtils.getLogger().debug("Initializing afterlife anchor types");
	}

	public static Codec<AfterlifeAnchorType<?>> typeCodec() {
		return REGISTRY.get().getCodec();
	}

	/**
	 * Get the codec for party resources
	 * 
	 * @return
	 */
	public static Codec<IAfterlifeAnchor<?, ?>> resourceCodec() {
		if (O_CODEC.isEmpty()) {
			if (REGISTRY.get() == null) {
				if (D_CODEC == null) {
					D_CODEC = ResourceLocation.CODEC.xmap(BUILTIN_MAP::get, BUILTIN_MAP.inverse()::get).dispatch("type",
							(e) -> (AfterlifeAnchorType) e.getAnchorType(), (x) -> x.codec());
				}
				return D_CODEC;
			} else {
				O_CODEC = Optional.of(REGISTRY.get()).map((f) -> f.getCodec())
						.map((f) -> f.dispatch("type", IAfterlifeAnchor<?, ?>::getAnchorType, (x) -> x.codec()));
			}
		}
		return O_CODEC.get();
	}

	public static <T extends IAfterlifeAnchor<?, ?>> RegistryObject<AfterlifeAnchorType<T>> register(String id,
			Supplier<MapCodec<T>> codec) {
		LogUtils.getLogger().debug("Registering anchor type " + id + " to registry " + SOTDMod.ANCHOR_TYPES);
		var emType = new AfterlifeAnchorType<T>(codec);
		BUILTIN_MAP.put(ModUtils.path(id), emType);
		return SOTDMod.ANCHOR_TYPES.register(id, () -> emType);
	}

	private Supplier<MapCodec<T>> mcodec;

	private AfterlifeAnchorType(Supplier<MapCodec<T>> codec) {
		this.mcodec = Suppliers.memoize(codec);
	}

	public MapCodec<T> codec() {
		return mcodec.get();
	}

}
