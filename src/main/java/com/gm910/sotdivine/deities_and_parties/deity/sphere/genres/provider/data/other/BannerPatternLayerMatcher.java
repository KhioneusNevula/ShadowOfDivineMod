package com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.data.other;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.deities_and_parties.deity.symbol.DeitySymbols;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.HolderUtils;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Layer;

/**
 * A provider/selector for one layer of a banner patterns. Either "patterns" or
 * "permit_symbols" may be set, or neither.
 */
public record BannerPatternLayerMatcher(Optional<HolderSet<BannerPattern>> patterns, Set<DyeColor> colors,
		SymbolPredicate permitSymbols) implements IGenreProvider<BannerPatternLayers.Layer, BannerPatternLayers.Layer> {

	private static Codec<BannerPatternLayerMatcher> CODEC = null;

	public static Codec<BannerPatternLayerMatcher> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec
					.mapEither(RegistryCodecs.homogeneousList(Registries.BANNER_PATTERN).optionalFieldOf("patterns"),
							SymbolPredicate.CODEC.optionalFieldOf("symbols", SymbolPredicate.PERMIT))
					.forGetter((x) -> x.patterns.isPresent() ? Either.left(x.patterns) : Either.right(x.permitSymbols)),
					CodecUtils.listOrSingleCodec(DyeColor.CODEC).optionalFieldOf("colors", List.of())
							.forGetter((s) -> new ArrayList<>(s.colors)))
					.apply(instance, (e, c) -> new BannerPatternLayerMatcher(e.left().orElse(Optional.empty()),
							new HashSet<>(c), e.right().orElse(SymbolPredicate.PERMIT))));
		}
		return CODEC;
	}

	@Override
	public boolean matches(ServerLevel level, Layer instance) {
		if (patterns.isPresent()) {
			if (!HolderUtils.holderSetContains(patterns.get(), instance.pattern())) {
				return false;
			}
		}
		if (permitSymbols != SymbolPredicate.PERMIT) {
			if ((DeitySymbols.instance()
					.getFromPattern(instance.pattern()) == null) == (permitSymbols == SymbolPredicate.ONLY)) {
				return false;
			}
		}
		if (!colors.isEmpty()) {
			if (!colors.contains(instance.color())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Layer generateRandom(ServerLevel level, Optional<Layer> prior) {
		Holder<BannerPattern> pattern;
		if (patterns.isPresent()) {
			pattern = patterns.get().getRandomElement(level.random).orElseThrow();
		} else {
			Registry<BannerPattern> registry = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
			List<BannerPattern> pickFrom = Lists
					.newArrayList(registry.stream()
							.filter((pt) -> this.permitSymbols != SymbolPredicate.PERMIT
									? (DeitySymbols.instance()
											.getFromPattern(pt) == null) == (permitSymbols == SymbolPredicate.FORBID)
									: true)
							.iterator());
			Collections.shuffle(pickFrom);
			pattern = registry.wrapAsHolder(pickFrom.getFirst());
		}

		List<DyeColor> allowedColors = new ArrayList<>(colors);

		if (allowedColors.isEmpty()) {
			allowedColors = Lists.newArrayList(DyeColor.values());
		}
		Collections.shuffle(allowedColors);
		DyeColor dye = allowedColors.getFirst();

		return new BannerPatternLayers.Layer(pattern, dye);
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof BannerPatternLayerMatcher prov) {
			return HolderUtils.optionalEquals(patterns, prov.patterns, HolderUtils::holderSetEquals)
					&& this.colors.equals(prov.colors) && this.permitSymbols.equals(prov.permitSymbols);
		}
		return false;
	}

	@Override
	public ProviderType<BannerPatternLayerMatcher> providerType() {
		throw new UnsupportedOperationException("Not a real provider");
	}

	@Override
	public final int hashCode() {
		return patterns.map(HolderUtils::holderSetHashCode).orElse(0) + colors.hashCode();
	}

	@Override
	public final String toString() {
		return "Match(" + patterns + "){" + (colors.isEmpty() ? "" : "colors=" + colors + ",") + "symbols="
				+ permitSymbols + "}";
	}

	public static enum SymbolPredicate {
		/** Allow symbols on this layer */
		PERMIT,
		/** Forbid symbols from appearing on this layer */
		FORBID,
		/** Only allow symbols on this layer */
		ONLY;

		public static final Codec<SymbolPredicate> CODEC = CodecUtils.caselessEnumCodec(SymbolPredicate.class);
	}

}
