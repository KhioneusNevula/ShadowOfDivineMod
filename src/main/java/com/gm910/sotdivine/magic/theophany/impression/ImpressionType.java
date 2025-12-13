package com.gm910.sotdivine.magic.theophany.impression;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.concepts.parties.IPartyLister.IDeityInfo;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.magic.theophany.impression.IImpression.Usage;
import com.gm910.sotdivine.magic.theophany.impression.types.DebugPrintImpression;
import com.gm910.sotdivine.magic.theophany.impression.types.DeityImpression;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.RandomUtils;
import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.DeferredRegister.RegistryHolder;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

/**
 * A specific kind of impression that a being can witness
 * 
 * @param <T>
 */
public class ImpressionType<T extends IImpression> {

	private static final BiMap<ResourceLocation, ImpressionType<?>> BUILTIN_MAP = HashBiMap.create();

	public final static RegistryHolder<ImpressionType<?>> REGISTRY = SOTDMod.IMPRESSION_TYPES.makeRegistry(
			() -> RegistryBuilder.<ImpressionType<?>>of(ModRegistries.IMPRESSION_TYPES.location()).allowModification());

	/**
	 * Placeholder for {@link IImpression#requireInputs()}
	 */
	public static final ImpressionType<IImpression> ANY = new ImpressionType<>(null, null, null, null, null);

	public static final RegistryObject<ImpressionType<DebugPrintImpression>> DEBUG = register("debug",
			DebugPrintImpression::codec, DebugPrintImpression::streamCodec, Usage.POWER,
			(sp, dei) -> new DebugPrintImpression(Component.literal(dei.uniqueName()),
					sp.getRandom().nextIntBetweenInclusive(1, DebugPrintImpression.MAX_ARGS)));

	public static final RegistryObject<ImpressionType<DeityImpression>> DEITY = register("deity",
			() -> DeityImpression.codec(), () -> ByteBufCodecs.fromCodecWithRegistries(DeityImpression.codec()),
			Usage.KNOWLEDGE,
			(sp, dei) -> new DeityImpression(dei != null ? dei.uniqueName()
					: RandomUtils.choose(sp.getRandom(), IPartySystem.get(sp.level()).allDeities())
							.orElseThrow(() -> new IllegalStateException("No deities")).uniqueName()));

	public static <T extends IImpression> RegistryObject<ImpressionType<T>> register(String id,
			Supplier<Codec<T>> codec, Supplier<StreamCodec<? super RegistryFriendlyByteBuf, T>> scodec,
			IImpression.Usage usage, BiFunction<ServerPlayer, IDeityInfo, T> func) {
		LogUtils.getLogger().debug("Registering impression type " + id + " to registry");
		var emType = new ImpressionType<T>(codec, scodec, ModUtils.path(id), usage, func);
		BUILTIN_MAP.put(ModUtils.path(id), emType);
		return SOTDMod.IMPRESSION_TYPES.register(id, () -> emType);
	}

	public static void init() {
		LogUtils.getLogger().debug("Initializing impression types");
	}

	private Supplier<Codec<T>> codec;

	private ResourceLocation rl;

	private Usage usage;

	private BiFunction<ServerPlayer, IDeityInfo, T> createTest;

	private Supplier<StreamCodec<? super RegistryFriendlyByteBuf, T>> streamCodec;

	public ImpressionType(Supplier<Codec<T>> codec2, Supplier<StreamCodec<? super RegistryFriendlyByteBuf, T>> scodec,
			ResourceLocation nam, IImpression.Usage usage, BiFunction<ServerPlayer, IDeityInfo, T> ct) {
		this.codec = codec2 == null ? null : Suppliers.memoize(codec2::get);
		this.streamCodec = scodec == null ? null : Suppliers.memoize(scodec::get);
		this.rl = nam;
		this.createTest = ct;
		this.usage = usage;

	}

	public Usage usage() {
		return usage;
	}

	public ResourceLocation path() {
		return rl;
	}

	public Codec<T> codec() {
		return codec.get();
	}

	public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
		return streamCodec.get();
	}

	/**
	 * Returns a random/dummy instance of this impression for testing
	 * 
	 * @return
	 */
	public IImpression createTest(ServerPlayer forP, IDeityInfo byDeity) {
		return createTest.apply(forP, byDeity);
	}

	public static ImpressionType<?> get(ResourceLocation loc) {
		return REGISTRY.get() != null ? REGISTRY.get().getValue(loc) : BUILTIN_MAP.get(loc);
	}

	public static Stream<ResourceLocation> allImpressionTypeResourceLocations() {
		return REGISTRY.get() != null ? REGISTRY.get().getKeys().stream() : BUILTIN_MAP.keySet().stream();
	}

	@Override
	public String toString() {
		return rl + "[" + usage + "]";
	}

	private static Codec<IImpression> D_CODEC = null;

	private static Optional<Codec<IImpression>> O_CODEC = Optional.empty();

	private static StreamCodec<RegistryFriendlyByteBuf, IImpression> D_SCODEC = null;

	private static Optional<StreamCodec<RegistryFriendlyByteBuf, IImpression>> O_SCODEC = Optional.empty();

	public static Codec<ImpressionType<?>> typeCodec() {
		return REGISTRY.get().getCodec();
	}

	public static StreamCodec<RegistryFriendlyByteBuf, ImpressionType<?>> typeStreamCodec() {
		return ByteBufCodecs.fromCodecWithRegistries(REGISTRY.get().getCodec());
	}

	/**
	 * Get the codec for party resources
	 * 
	 * @return
	 */
	public static Codec<IImpression> resourceCodec() {
		if (O_CODEC.isEmpty()) {
			if (REGISTRY.get() == null) {
				if (D_CODEC == null) {
					D_CODEC = ResourceLocation.CODEC.xmap(BUILTIN_MAP::get, BUILTIN_MAP.inverse()::get).dispatch("type",
							(e) -> (ImpressionType) e.getImpressionType(), (x) -> x.codec().fieldOf("impression"));
				}
				return D_CODEC;
			} else {
				O_CODEC = Optional.of(REGISTRY.get()).map((f) -> f.getCodec()).map((f) -> f.dispatch("type",
						IImpression::getImpressionType, (x) -> x.codec().fieldOf("impression")));
			}
		}
		return O_CODEC.get();
	}

	public static StreamCodec<RegistryFriendlyByteBuf, IImpression> resourceStreamCodec() {
		if (O_SCODEC.isEmpty()) {
			if (REGISTRY.get() == null) {
				if (D_SCODEC == null) {
					D_SCODEC = ByteBufCodecs.fromCodecWithRegistries(ResourceLocation.CODEC)
							.map(BUILTIN_MAP::get, BUILTIN_MAP.inverse()::get)
							.dispatch((e) -> (ImpressionType) e.getImpressionType(), (x) -> x.streamCodec());
				}
				return D_SCODEC;
			} else {
				O_SCODEC = Optional
						.of(typeStreamCodec().dispatch(IImpression::getImpressionType, (x) -> x.streamCodec()));
			}
		}
		return O_SCODEC.get();
	}
}
