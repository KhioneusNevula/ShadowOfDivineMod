package com.gm910.sotdivine.systems.deity.sphere.genres.provider.data.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.data.other.BannerPatternLayerMatcher;
import com.gm910.sotdivine.util.CodecUtils;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Matches/provides a banner pattern
 */
public class BannerPatternComponentMatcher implements IComponentMatcherProvider<BannerPatternLayers> {

	public static final ResourceLocation PATH = ResourceLocation.withDefaultNamespace("banner_patterns");

	private static Codec<BannerPatternComponentMatcher> CODEC;

	public static Codec<BannerPatternComponentMatcher> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder.create(instance -> instance
					.group(Codec.list(BannerPatternLayerMatcher.codec()).fieldOf("layers").forGetter((x) -> x.layers()),
							Ordering.CODEC.optionalFieldOf("ordering", Ordering.UNORDERED)
									.forGetter((x) -> x.sequential && x.contiguous ? Ordering.CONTIGUOUS_SEQUENTIAL
											: x.sequential ? Ordering.SEQUENTIAL : Ordering.UNORDERED),
							Position.CODEC.optionalFieldOf("position", Position.ANYWHERE).forGetter((x) -> x.position))
					.apply(instance,
							(layer, order, position) -> new BannerPatternComponentMatcher(layer, order, position)));
		}
		return CODEC;
	}

	private boolean sequential;
	private List<BannerPatternLayerMatcher> predicateLayers;
	private Position position;
	private boolean contiguous;

	public BannerPatternComponentMatcher(List<BannerPatternLayerMatcher> layers, Ordering ordering, Position position) {
		this.sequential = ordering == Ordering.SEQUENTIAL || ordering == Ordering.CONTIGUOUS_SEQUENTIAL;
		this.predicateLayers = layers;
		if (layers.isEmpty())
			throw new IllegalArgumentException();
		this.position = position;
		this.contiguous = ordering == Ordering.CONTIGUOUS_SEQUENTIAL;
	}

	@Override
	public boolean matches(ServerLevel level, DataComponentGetter instance) {
		BannerPatternLayers layersS = instance.get(DataComponents.BANNER_PATTERNS);
		if (layersS == null)
			return false;

		List<BannerPatternLayers.Layer> layers = layersS.layers();
		if (this.predicateLayers.size() > layers.size()) {
			return false;
		}
		if (sequential) {
			int check = 0;
			for (int i = 0; i < layers.size(); i++) {
				if (this.predicateLayers.get(check).matches(level, layers.get(i))) {
					check++;
					if (check >= this.predicateLayers.size()) {
						if (position == Position.ANYWHERE) {
							return true;
						} else if (position == Position.OUTERMOST) {
							return i == layers.size() - 1;
						}
					}
				} else {
					if (position == Position.INNERMOST && i == 0) {
						return false;
					}
					if (!contiguous)
						check = 0;
				}
			}
			return false;
		} else {
			// For the layer at index i, we store the set of indices that matched it
			List<Set<Integer>> matchersAtIndex = new ArrayList<>();
			boolean matchedFirst = false;
			boolean matchedLast = false;
			for (int i = 0; i < layers.size(); i++) {
				matchersAtIndex.add(new HashSet<>());
			}

			for (int curPred = 0; curPred < this.predicateLayers.size(); curPred++) {
				for (int curInstance = 0; curInstance < layers.size(); curInstance++) {
					if (predicateLayers.get(curPred).matches(level, layers.get(curInstance))) {
						matchersAtIndex.get(curInstance).add(curPred);
						if (curInstance == 0) {
							matchedFirst = true;
						}
						if (curInstance == layers.size() - 1) {
							matchedLast = true;
						}
					}
				}
			}
			if (position == Position.INNERMOST && !matchedFirst)
				return false;
			if (position == Position.OUTERMOST && !matchedLast)
				return false;

			// if (!contiguous) // not gonna do an np-hard problem...
			return true;

		}
	}

	@Override
	public TypedDataComponent<BannerPatternLayers> generateRandom(ServerLevel level,
			Optional<DataComponentGetter> prior) {
		List<BannerPatternLayers.Layer> priorLayers = prior.map((p) -> p.get(DataComponents.BANNER_PATTERNS))
				.map((p) -> new ArrayList<>(p.layers())).orElse(new ArrayList<>());
		List<BannerPatternLayers.Layer> layersToAdd = Lists.newArrayList(
				this.predicateLayers.stream().map((x) -> x.generateRandom(level, Optional.empty())).iterator());
		if (!this.sequential) {
			Collections.shuffle(layersToAdd);
		}
		if (this.contiguous) {
			int index;
			if (position == Position.INNERMOST || priorLayers.size() == 0) {
				index = 0;
			} else if (position == Position.OUTERMOST) {
				index = priorLayers.size();
			} else {
				index = level.random.nextInt(0, priorLayers.size());
			}
			priorLayers.addAll(index, layersToAdd);
			return new TypedDataComponent<>(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(priorLayers));
		} else {
			int firstIndex = position == Position.INNERMOST ? 0 : 1;
			for (BannerPatternLayers.Layer layer : priorLayers) {
				int lastIndex = layersToAdd.size() - (position == Position.OUTERMOST ? 1 : 0);
				int index;
				if (firstIndex >= lastIndex)
					index = firstIndex;
				else
					index = level.random.nextInt(firstIndex, lastIndex);
				layersToAdd.add(index, layer);
				firstIndex = index + 1;
			}
			return new TypedDataComponent<>(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layersToAdd));
		}
	}

	public List<BannerPatternLayerMatcher> layers() {
		return predicateLayers;
	}

	@Override
	public ProviderType<BannerPatternComponentMatcher> providerType() {
		throw new UnsupportedOperationException("Not a real provider");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof BannerPatternComponentMatcher cm) {
			return this.predicateLayers.equals(cm.predicateLayers) && this.contiguous == cm.contiguous
					&& this.sequential == cm.sequential && this.position == cm.position;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.predicateLayers.hashCode() + this.position.hashCode() + Boolean.hashCode(this.sequential)
				+ Boolean.hashCode(this.contiguous);
	}

	@Override
	public String toString() {
		return "(" + PATH + "){layers=" + this.layers() + ",position=" + this.position + ",sequential="
				+ this.sequential + ",contiguous=" + this.contiguous + "}";
	}

	public static enum Position {
		ANYWHERE, OUTERMOST, INNERMOST;

		public final static Codec<Position> CODEC = CodecUtils.caselessEnumCodec(Position.class);

	}

	public static enum Ordering {
		SEQUENTIAL, CONTIGUOUS_SEQUENTIAL, UNORDERED;
		// no CONTIGUOUS_UNORDERED because that is probably NP hard

		public final static Codec<Ordering> CODEC = CodecUtils.caselessEnumCodec(Ordering.class);
	}

}
