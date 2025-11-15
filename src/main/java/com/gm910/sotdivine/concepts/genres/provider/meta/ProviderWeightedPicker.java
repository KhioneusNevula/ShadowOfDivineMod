package com.gm910.sotdivine.concepts.genres.provider.meta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.StreamUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * A provider of optional weights; can return a range of possible providers at
 * different possibilities. Can either deserialize from a list of "provider:,
 * optional/probability:" mini-maps, a map of "probability":{provider}, or a
 * single element of "provider:, optional/probability", or just the basic form
 * of the element
 * 
 * @param <E>
 */
public record ProviderWeightedPicker<T, G, E extends IGenreProvider>(WeightedSet<E> set)
		implements IGenreProvider<T, G> {

	private static <T, G, X extends IGenreProvider> Codec<Entry<X, Float>> justCodec(Codec<X> singleCodec) {
		return singleCodec.<Entry<X, Float>>flatComapMap((x) -> Map.<X, Float>entry(x, 0.5f),
				(x) -> DataResult.error(() -> "Not enough to make pair"));
	}

	private static <T, G, X extends IGenreProvider> Codec<Entry<X, Float>> pairCodec(Codec<X> generalCodec) {
		return RecordCodecBuilder.create(instance -> instance
				.group(generalCodec.fieldOf("provider").forGetter(Entry::getKey),
						Codec.mapEither(Codec.floatRange(0.0f, 1.0f).fieldOf("probability"),
								Codec.BOOL.optionalFieldOf("optional", false))
								.forGetter((op) -> op.getValue() == 1.0f ? Either.right(false)
										: (op.getValue() == 0.5f ? Either.right(true) : Either.left(op.getValue()))))
				.apply(instance, (x, y) -> Map.entry(x, y.map((b) -> b, (b) -> b ? 1.0f : 0.5f))));
	}

	private static <T, G, X extends IGenreProvider> Codec<Entry<X, Float>> singleCodec(Codec<X> generalCodec) {
		Codec<Entry<X, Float>> justProvider = justCodec(generalCodec);

		Codec<Entry<X, Float>> withProbability = pairCodec(generalCodec);

		return Codec.either(justProvider, withProbability).xmap(Either::unwrap,
				(s) -> s.getValue() < 1.0f ? Either.right(s) : Either.left(s));
	}

	private static <T, G, X extends IGenreProvider> Codec<List<Entry<X, Float>>> listCodec(Codec<X> generalCodec) {
		return CodecUtils.listOrSingleCodec(singleCodec(generalCodec));
	}

	private static <T, G, X extends IGenreProvider> Codec<Map<Float, X>> mapCodec(Codec<X> generalCodec) {
		return Codec.unboundedMap(Codec.STRING.comapFlatMap((f) -> {
			float value;
			try {
				value = Float.parseFloat(f);
			} catch (NumberFormatException e) {
				return DataResult.error(() -> "Not a float: " + f);
			}
			return DataResult.success(value);
		}, (f) -> "" + f), generalCodec);
	}

	private static <T, G, X extends IGenreProvider> Codec<Map<X, Float>> inverseMapCodec(Codec<X> generalCodec) {
		return mapCodec(generalCodec).flatXmap((m1) -> {
			Map<X, Float> bimap;
			try {
				bimap = HashBiMap.create(m1).inverse();
			} catch (IllegalArgumentException e) {
				return DataResult.error(() -> "All providers and floats must be unique: " + e.getMessage());
			}
			return DataResult.success(bimap);
		}, (invm) -> {
			Map<Float, X> bimap;
			try {
				bimap = HashBiMap.create(invm).inverse();
			} catch (IllegalArgumentException e) {
				return DataResult.error(() -> "All providers and floats must be unique: " + e.getMessage());
			}
			return DataResult.success(bimap);
		});
	}

	private static final Map<Codec, Codec> CODEC_CACHE = new HashMap<>();

	@Override
	public ProviderType<? extends IGenreProvider<T, G>> providerType() {
		throw new UnsupportedOperationException("Not an independent provider");
	}

	/**
	 * Can either deserialize the codec itself, or an optional specification
	 * 
	 * @param <X>
	 * @param generalCodec
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T, G, X extends IGenreProvider> Codec<ProviderWeightedPicker<T, G, X>> codec(Codec<X> generalCodec) {

		return (Codec<ProviderWeightedPicker<T, G, X>>) CODEC_CACHE.computeIfAbsent(generalCodec,
				(c) -> Codec.either(listCodec(generalCodec), inverseMapCodec(generalCodec))
						.xmap((either) -> either.map((x) -> new ProviderWeightedPicker<>(WeightedSet.fromEntries(x)),
								(x) -> new ProviderWeightedPicker<>(new WeightedSet<>(x))),
								(op) -> Either.right(op.set.asWeightMap())));
	}

	/**
	 * Return true if the total weight of all optional elements here is less than
	 * 1.0f
	 * 
	 * @return
	 */
	public boolean isOptional() {
		return set.totalWeight() < 1.0f;
	}

	@Override
	public boolean matches(ServerLevel level, T instance) {
		if (isOptional()) {
			return true;
		}
		return this.set.stream().anyMatch((e) -> e.matches(level, instance));
	}

	/**
	 * Keep in mind that, due to the nature of this item, it may return null if
	 * {@link #isOptional()} is true
	 */
	@Override
	@Nullable
	public G generateRandom(ServerLevel level, Optional<T> prior) {
		if (this.set.totalWeight() < 1.0f) {
			if (level.random.nextFloat() <= (1.0f - this.set.totalWeight())) {
				return null;
			}
		}
		E provider = this.set.get(level.random);
		var obj = provider.generateRandom(level, prior);
		return (G) obj;
	}

	@Override
	public final String toString() {
		return this.set.asWeightMap().toString();
	}

	@Override
	public Component translate() {
		return this.set.stream().map((e) -> TextUtils.transPrefix("sotd.cmd.quote", e.translate()))
				.collect(StreamUtils.componentCollectorCommasPretty());
	}

}
