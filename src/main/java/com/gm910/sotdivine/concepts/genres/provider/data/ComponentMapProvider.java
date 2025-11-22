package com.gm910.sotdivine.concepts.genres.provider.data;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.concepts.genres.provider.data.components.IComponentMatcherProvider;
import com.gm910.sotdivine.concepts.genres.provider.meta.ProviderWeightedPicker;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Can instrument data components or return them. Each partial component is
 * provided as a pair to indicate whether it is optional.
 */
@SuppressWarnings("rawtypes")
public record ComponentMapProvider(DataComponentExactPredicate exact,
		Map<ResourceLocation, ProviderWeightedPicker<DataComponentGetter, TypedDataComponent, IComponentMatcherProvider<?>>> partial)
		implements IGenreProvider<DataComponentGetter, DataComponentMap> {

	public static final ComponentMapProvider ANY = new ComponentMapProvider(DataComponentExactPredicate.EMPTY,
			Map.of());
	private static MapCodec<ComponentMapProvider> CODEC = null;

	public static MapCodec<ComponentMapProvider> codec() {
		if (CODEC == null) {

			CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
					DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY)
							.forGetter(ComponentMapProvider::exact),
					Codec.<ResourceLocation, ProviderWeightedPicker<DataComponentGetter, TypedDataComponent, IComponentMatcherProvider<?>>>dispatchedMap(
							ResourceLocation.CODEC, (rl) -> {
								Codec<IComponentMatcherProvider<?>> codigo = CodecsComponentMatchers.getCodec(rl);
								if (codigo == null) {
									throw new IllegalArgumentException("No component matcher codec for " + rl);
								}
								return ProviderWeightedPicker.codec(codigo);
							}).optionalFieldOf("predicates", Map.of()).forGetter((x) -> x.partial()))
					.apply(instance, (DataComponentExactPredicate e,
							Map<ResourceLocation, ProviderWeightedPicker<DataComponentGetter, TypedDataComponent, IComponentMatcherProvider<?>>> p) -> new ComponentMapProvider(
									e, p)));
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

	@Override
	public final String toString() {
		return "{" + (!exact.isEmpty() ? "exact=" + exact + (!partial.isEmpty() ? "," : "") : "")
				+ (this.partial.isEmpty() ? ""
						: "partial={" + this.partial.entrySet().stream()
								.map((e) -> Map.entry(ModUtils.toShortString(e.getKey()), e.getValue()))
								.collect(CollectionUtils.setStringCollector(",")) + "}")
				+ "}";
	}

	@Override
	public Component translate() {
		Component partialS = partial
				.entrySet().stream().map((a) -> TextUtils.transPrefix("sotd.cmd.map",
						ModUtils.toShortString(a.getKey()), a.getValue().translate()))
				.collect(CollectionUtils.componentCollectorCommas());
		if (!exact.isEmpty() && !partial.isEmpty()) {
			return TextUtils.transPrefix("genre.provider.cmap.exact_and_partial", exact.toString(), partialS);
		} else if (!exact.isEmpty()) {
			return TextUtils.transPrefix("genre.provider.cmap.exact", exact.toString());
		} else if (!partial.isEmpty()) {
			return TextUtils.transPrefix("genre.provider.cmap.partial", partialS);
		}
		return TextUtils.transPrefix(TextUtils.SET_BRACE_TRANSLATION_PREFIX, Component.empty());
	}

}
