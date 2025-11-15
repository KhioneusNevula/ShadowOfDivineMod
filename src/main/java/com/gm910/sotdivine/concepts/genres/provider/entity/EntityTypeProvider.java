package com.gm910.sotdivine.concepts.genres.provider.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.Lists;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.HolderUtils;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.StreamUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public record EntityTypeProvider(Optional<EntityTypePredicate> entityType, Collection<MobCategory> categories)
		implements IGenreProvider<EntityType<?>, EntityType<?>> {

	private static Codec<EntityTypeProvider> CODEC = null;

	public static Codec<EntityTypeProvider> codec() {
		if (CODEC == null)
			CODEC = Codec.lazyInitialized(() -> Codec
					.<EntityTypeProvider, EntityTypeProvider>either(EntityTypePredicate.CODEC
							.flatComapMap((s) -> new EntityTypeProvider(Optional.of(s), Set.of()),
									(z) -> z.entityType().isPresent() && z.categories().isEmpty()
											? DataResult.success(z.entityType().get())
											: DataResult.error(() -> "Too much/little info")),
							RecordCodecBuilder.create(instance -> instance.group(
									EntityTypePredicate.CODEC.optionalFieldOf("types").forGetter((s) -> s.entityType()),
									CodecUtils.listOrSingleCodec(CodecUtils.caselessEnumCodec(MobCategory.class))
											.optionalFieldOf("categories", List.of())
											.forGetter((s) -> new ArrayList<>(s.categories())))
									.apply(instance, (t, c) -> new EntityTypeProvider(t, c))))
					.xmap((s) -> Either.unwrap(s),
							(x) -> x.categories().isEmpty() && !x.entityType().isEmpty() ? Either.left(x)
									: Either.right(x)));
		return CODEC;
	}

	@Override
	public boolean matches(ServerLevel level, EntityType<?> instance) {
		if (entityType.isPresent()) {
			if (!entityType.get().matches(instance)) {
				return false;
			}
		}
		if (!categories.isEmpty()) {
			if (!categories.contains(instance.getCategory())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public EntityType<?> generateRandom(ServerLevel level, Optional<EntityType<?>> prior) {
		var lista = Lists.newArrayList(streamEntityTypes(level).iterator());
		Collections.shuffle(lista);
		return lista.isEmpty() ? null : lista.getFirst();
	}

	public Stream<EntityType<?>> streamEntityTypes(ServerLevel level) {

		var entityreg = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
		return (entityType.isPresent() ? entityType.get().types().stream().map(Holder::get) : entityreg.stream())
				.filter((s) -> categories.isEmpty() ? true : categories.contains(s.getCategory())).map((s) -> s);
	}

	@Override
	public ProviderType<? extends IGenreProvider<EntityType<?>, EntityType<?>>> providerType() {
		return ProviderType.ENTITY_TYPE;
	}

	@Override
	public Component translate() {
		return TextUtils
				.transPrefix(
						"sotd.genre.provider.entity_type" + (entityType.isPresent() ? ".types"
								: "") + (!categories.isEmpty() ? ".categories" : ""),
						TextUtils.transPrefix(
								"genre.provider.entity."
										+ (entityType.get().types().unwrap().left().isPresent() ? "tag" : "list"),
								entityType.flatMap((s) -> s.types().unwrap().left())
										.map((s) -> ModUtils.toShortString(s.location())).orElse(""),
								entityType.flatMap((s) -> s.types().unwrap().right())
										.map((s) -> s.stream().map(Holder::get).map((et) -> et.getDescription())
												.collect(StreamUtils.componentCollectorCommasPretty()))),
						categories.stream().map((s) -> Component.literal(s.getName()))
								.collect(StreamUtils.componentCollectorCommasPretty()));
	}

	@Override
	public final boolean equals(Object arg0) {
		if (arg0 instanceof EntityTypeProvider egp) {
			if (arg0 == this)
				return true;

			return HolderUtils.optionalEquals(this.entityType.map(e -> e.types()), egp.entityType.map(e -> e.types()),
					HolderUtils::holderSetEquals) && this.categories.equals(egp.categories);
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return this.entityType.map((s) -> HolderUtils.holderSetHashCode(s.types())).hashCode()
				+ this.categories.hashCode();
	}

	@Override
	public final String toString() {
		return "Types(" + entityType
				.map(et -> et.types().unwrap().map((tg) -> "#" + tg.location(),
						(ls) -> ls.stream().map((g) -> BuiltInRegistries.ENTITY_TYPE.getKey(g.get()))
								.collect(StreamUtils.setStringCollector(","))))
				.orElse(categories.isEmpty() ? "<any>" : "Categories:" + categories) + ")";
	}

	@Override
	public String report() {
		return "(types={"
				+ entityType.map(et -> et.types().unwrap().map((tg) -> "#" + tg.location(),
						(ls) -> ls.stream().map((g) -> BuiltInRegistries.ENTITY_TYPE.getKey(g.get()))
								.collect(StreamUtils.setStringCollector(","))))
						.orElse("")
				+ "},categories=" + categories + ")";
	}

}
