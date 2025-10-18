package com.gm910.sotdivine.systems.deity.sphere.genres.provider.data;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.data.components.IComponentMatcherProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.EntityGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.meta.ProviderWeightedPicker;
import com.gm910.sotdivine.util.StreamUtils;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Can match data components or return them. Each partial component is provided
 * as a pair to indicate whether it is optional.
 */
@SuppressWarnings("rawtypes")
public record ComponentMapProvider(DataComponentExactPredicate exact,
		Map<ResourceLocation, ProviderWeightedPicker<DataComponentGetter, TypedDataComponent, IComponentMatcherProvider>> partial)
		implements IGenreProvider<DataComponentGetter, DataComponentMap> {

	public static final ComponentMapProvider ANY = new ComponentMapProvider(DataComponentExactPredicate.EMPTY,
			Map.of());
	private static MapCodec<ComponentMapProvider> CODEC = null;

	public static MapCodec<ComponentMapProvider> codec() {
		if (CODEC == null) {

			CODEC = RecordCodecBuilder.mapCodec(p_392397_ -> p_392397_.group(
					DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY)
							.forGetter(ComponentMapProvider::exact),
					Codec.<ResourceLocation, ProviderWeightedPicker<DataComponentGetter, TypedDataComponent, IComponentMatcherProvider>>dispatchedMap(
							ResourceLocation.CODEC, (rl) -> {
								var codigo = ComponentMatchers.<Object, IComponentMatcherProvider>getCodec(rl);
								if (codigo == null) {
									throw new IllegalArgumentException("No component matcher codec for " + rl);
								}
								return ProviderWeightedPicker.codec(codigo);
							}).optionalFieldOf("predicates", Map.of()).forGetter((x) -> x.partial))
					.apply(p_392397_, ComponentMapProvider::new));
		}
		return CODEC;
	}

	@Override
	public ProviderType<ComponentMapProvider> providerType() {
		throw new UnsupportedOperationException("Not a real provider");
	}

	@Override
	public boolean matches(ServerLevel level, DataComponentGetter instance) {
		if (!this.exact.test(instance)) {
			return false;
		}

		for (ProviderWeightedPicker loc : this.partial.values()) {
			if (!loc.matches(level, instance)) {
				return false;
			}
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataComponentMap generateRandom(ServerLevel level, Optional<DataComponentGetter> prior) {
		LogUtils.getLogger().debug("Making data map... " + this);
		var exactPatch = exact.asPatch();
		var builder = DataComponentMap.builder();
		for (Entry<DataComponentType<?>, Optional<?>> entry : exactPatch.entrySet()) {
			if (entry.getValue().isPresent()) {
				builder.set((DataComponentType) entry.getKey(), entry.getValue());
			}
		}

		for (var entry : partial.entrySet()) {
			var setter = entry.getValue().generateRandom(level, prior);
			if (setter == null)
				continue;
			builder.set(setter.type(), setter.value());
		}
		return builder.build();
	}

	public boolean isEmpty() {
		return exact.isEmpty() && partial.isEmpty();
	}

}
