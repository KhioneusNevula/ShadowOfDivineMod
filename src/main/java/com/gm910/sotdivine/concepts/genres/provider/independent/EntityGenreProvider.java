package com.gm910.sotdivine.concepts.genres.provider.independent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.Lists;

import com.gm910.sotdivine.concepts.genres.creator.EntityCreator;
import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.concepts.genres.provider.data.ComponentMapProvider;
import com.gm910.sotdivine.concepts.genres.provider.data.other.NbtInternalProvider;
import com.gm910.sotdivine.concepts.genres.provider.entity.EntityFlagsGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.entity.EntityTypeProvider;
import com.gm910.sotdivine.concepts.genres.provider.entity.EquipmentGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.entity.MobEffectGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.entity_preds.CodecsTypeSpecificProviders;
import com.gm910.sotdivine.concepts.genres.provider.entity_preds.ITypeSpecificProvider;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;

/**
 * Notes:
 * 
 * @param flags only allows "onFire" and "baby" as permissible flags; all others
 *              are not permitted
 */
public record EntityGenreProvider(EntityTypeProvider entityType, Collection<MobEffectGenreProvider> effects,
		Optional<NbtInternalProvider> nbt, Optional<EntityFlagsGenreProvider> flags,
		Optional<EquipmentGenreProvider> equipment, Optional<EntityGenreProvider> passenger,
		ComponentMapProvider components, Collection<ITypeSpecificProvider<?>> typeSpecific, float rarity,
		boolean canRightClick)
		implements IPlaceableGenreProvider<Entity, EntityCreator>, IEntityGenreProvider<Entity, EntityCreator> {

	private static Codec<EntityGenreProvider> CODEC = null;

	public EntityGenreProvider(EntityTypeProvider type) {
		this(type, Set.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				ComponentMapProvider.ANY, List.of(), 0.0f, true);
	}

	public EntityGenreProvider(EntityTypePredicate type) {
		this(new EntityTypeProvider(Optional.ofNullable(type), Set.of()), Set.of(), Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty(), ComponentMapProvider.ANY, List.of(), 0.0f, true);
	}

	public EntityGenreProvider(EntityTypePredicate type, Collection<ITypeSpecificProvider<?>> provider) {
		this(new EntityTypeProvider(Optional.ofNullable(type), Set.of()), Set.of(), Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty(), ComponentMapProvider.ANY, Set.copyOf(provider), 0.0f, true);
	}

	public EntityGenreProvider(EntityTypeProvider type, Collection<ITypeSpecificProvider<?>> provider) {
		this(type, Set.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				ComponentMapProvider.ANY, Set.copyOf(provider), 0.0f, true);
	}

	public static Codec<EntityGenreProvider> codec() {
		if (CODEC == null) {
			Codec<EntityGenreProvider> entityType = EntityTypePredicate.CODEC
					.flatComapMap((et) -> new EntityGenreProvider(et), (eg) -> {
						if (!eg.entityType.entityType().isEmpty() && eg.entityType.categories().isEmpty()
								&& eg.components.isEmpty() && eg.effects.isEmpty() && eg.equipment.isEmpty()
								&& eg.flags.isEmpty() && eg.nbt.isEmpty() && eg.passenger.isEmpty()
								&& eg.typeSpecific.isEmpty()) {
							return DataResult.success(eg.entityType.entityType().get());
						}
						if (eg.entityType.entityType().isEmpty()) {
							return DataResult.error(() -> "Doesn't make sense to serialize with no actual entity type");
						}
						return DataResult.error(
								() -> "Doesn't make sense to serialize only one aspect of complex entity provider",
								eg.entityType.entityType().get());
					});
			Codec<EntityGenreProvider> construction = Codec.recursive("EntityGenreProvider",
					(recurse) -> RecordCodecBuilder.create(instance -> instance.group(
							EntityTypeProvider.codec().fieldOf("entities").forGetter((f) -> f.entityType()),
							Codec.list(MobEffectGenreProvider.codec()).optionalFieldOf("effects", List.of())
									.forGetter((x) -> new ArrayList<>(x.effects)),
							NbtInternalProvider.CODEC.optionalFieldOf("nbt").forGetter(EntityGenreProvider::nbt),
							EntityFlagsGenreProvider.CODEC.optionalFieldOf("flags")
									.forGetter(EntityGenreProvider::flags),
							EquipmentGenreProvider.codec().optionalFieldOf("equipment")
									.forGetter(EntityGenreProvider::equipment),
							recurse.optionalFieldOf("passenger").forGetter(EntityGenreProvider::passenger),
							ComponentMapProvider.codec().forGetter(EntityGenreProvider::components),
							Codec.list(CodecsTypeSpecificProviders.codec()).optionalFieldOf("type_specific", List.of())
									.forGetter((x) -> new ArrayList<>(x.typeSpecific)),
							CodecUtils.enumFloatScaleCodec(Rarity.class).optionalFieldOf("rarity", 0.0f)
									.forGetter(EntityGenreProvider::rarity),
							Codec.BOOL.optionalFieldOf("can_right_click", true)
									.forGetter(EntityGenreProvider::canRightClick))
							.apply(instance, (et, ef, nbt, fl, eq, pa, co, ts, r, cr) -> new EntityGenreProvider(et, ef,
									nbt, fl, eq, pa, co, ts, r, cr))));
			CODEC = Codec.either(entityType, construction).xmap(Either::unwrap, Either::right);
		}
		return CODEC;
	}

	@Override
	public ProviderType<EntityGenreProvider> providerType() {
		return ProviderType.ENTITY;
	}

	@Override
	public boolean matches(ServerLevel level, Entity instance) {
		if (!entityType.matches(level, instance.getType())) {
			return false;
		}
		if (!flags.isEmpty()) {
			if (!flags.get().matches(level, instance)) {
				return false;
			}
		}
		if (!effects.isEmpty() || equipment.isPresent()) {
			if (instance instanceof LivingEntity liv) {
				if (!effects.isEmpty() && effects.stream().anyMatch((e) -> e.matchesEntity(level, liv))) {
					return false;
				}

				if (equipment.isPresent() && !equipment.get().matches(level, instance)) {
					return false;
				}
			} else {
				return false;
			}
		}
		if (!components.isEmpty()) {
			if (!components.matches(level, instance)) {
				return false;
			}
		}
		if (!nbt.isEmpty()) {
			if (!nbt.get().asPredicate().matches(instance)) {
				return false;
			}
		}
		if (!typeSpecific.isEmpty()) {
			if (!typeSpecific.stream().allMatch((x) -> x.entityClass().isAssignableFrom(instance.getClass())
					&& ((ITypeSpecificProvider) x).matches(level, instance))) {
				return false;
			}
		}
		if (passenger.isPresent()) {
			if (!instance.getPassengers().stream().anyMatch((x) -> passenger.get().matches(level, x))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean matchesPos(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(Entity.class, new AABB(pos)).stream().anyMatch((x) -> this.matches(level, x));
	}

	@Override
	public boolean canPlaceOn(LevelReader level, BlockPos pos) {
		return true;
	}

	@Override
	public EntityCreator generateRandom(ServerLevel level, Optional<Entity> prev) {
		Stream<EntityType<?>> stream = entityType.streamEntityTypes(level).filter((s) -> {
			var entity = s.create(level, EntitySpawnReason.COMMAND);
			boolean works = true;
			if (!this.typeSpecific.isEmpty()) {
				works = typeSpecific.stream().allMatch((coca) -> coca.entityClass().isAssignableFrom(entity.getClass()))
						&& works;
			}
			if (equipment.isPresent() || !effects.isEmpty()) {
				works = entity instanceof LivingEntity && works;
			}
			return works;
		});
		var listet = Lists.newArrayList(stream.iterator());
		Collections.shuffle(listet);
		if (listet.isEmpty()) {
			throw new IllegalStateException("Bad provider could not produce shi-- " + this.report());
		}
		EntityType<?> type = listet.getFirst();
		Consumer<? extends Entity> consumer = (e) -> {
			if (nbt.isPresent()) {
				e.deserializeNBT(level.registryAccess(), nbt.get().tag());
			}
			if (!components.isEmpty()) {
				DataComponentMap map = components.generateRandom(level, Optional.of(e));
				for (DataComponentType key : map.keySet()) {
					e.setComponent(key, map.get(key));
				}
			}
			if (!typeSpecific.isEmpty()) {
				typeSpecific.forEach((ts) -> {
					if (ts.entityClass().isAssignableFrom(e.getClass())) {
						((ITypeSpecificProvider) ts).generateRandom(level, Optional.of(e));
					}
				});
			}
			if (e instanceof LivingEntity liv) {

				if (!effects.isEmpty()) {
					effects.forEach((effect) -> effect.generateRandomForEntity(level, Optional.of(liv)));
				}
				if (equipment.isPresent()) {
					equipment.get().generateRandom(level, Optional.of(e));
				}
			}
			if (flags.isPresent()) {
				flags.get().generateRandom(level, Optional.of(e));
			} else {
				if (e instanceof AgeableMob age && level.random.nextInt(0, 100) < 10) {
					age.setBaby(true);
				}
			}

			if (passenger.isPresent()) {
				EntityCreator placer = passenger.get().generateRandom(level, Optional.empty());
				placer.createWithoutPlacing(level, e.blockPosition()).startRiding(e, true);
			}
		};

		return new EntityCreator(type, consumer);
	}

	@Override
	public boolean matchesEntity(ServerLevel level, Entity en) {
		return this.matches(level, en);
	}

	@Override
	public Entity generateRandomForEntity(ServerLevel level, Optional<Entity> entity) {
		return this.generateRandomForEntity(level, entity);
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof EntityGenreProvider egp) {
			return this.entityType.equals(egp.entityType) && this.components.equals(egp.components)
					&& this.effects.equals(egp.effects) && this.equipment.equals(egp.equipment)
					&& this.flags.equals(egp.flags) && this.nbt.equals(egp.nbt) && this.passenger.equals(egp.passenger);
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return this.entityType.hashCode() + this.components.hashCode() + this.effects.hashCode()
				+ this.equipment.hashCode() + this.flags.hashCode() + this.nbt.hashCode() + this.passenger.hashCode();
	}

	@Override
	public final String toString() {
		return "Entity(" + entityType + ")";
	}

	@Override
	public Component translate() {
		String pref = "sotd.genre.provider.entity.";
		return TextUtils.transPrefix("sotd.genre.provider.entity.conditions",
				effects.isEmpty() ? Component.empty()
						: TextUtils.transPrefix(pref + "effects",
								effects.stream().map(MobEffectGenreProvider::translate)
										.collect(StreamUtils.componentCollectorCommasPretty())),
				flags.map((s) -> TextUtils.transPrefix(pref + "flags", s.translate())).orElse(Component.empty()),
				equipment.map((s) -> TextUtils.transPrefix(pref + "equipment", s.translate()))
						.orElse(Component.empty()),
				passenger.map((s) -> TextUtils.transPrefix(pref + "passenger", s.translate()))
						.orElse(Component.empty()),
				typeSpecific.isEmpty() ? Component.empty()
						: TextUtils.transPrefix(pref + "special",
								typeSpecific.stream().map(ITypeSpecificProvider::translate)
										.collect(StreamUtils.componentCollectorCommasPretty())),
				nbt.map((s) -> TextUtils.transPrefix(pref + "nbt", s.translate())).orElse(Component.empty()),
				components.isEmpty() ? Component.empty()
						: TextUtils.transPrefix(pref + "components", components.translate()),
				entityType.translate());

	}

	@Override
	public String report() {
		return "Entity{types=" + entityType.report() + (rarity != 0 ? ",rarity=" + rarity : "")
				+ (components.isEmpty() ? "" : ",components=" + components)
				+ (this.effects.isEmpty() ? "" : ",effects=" + effects)
				+ (this.equipment.isEmpty() ? "" : ",equipment=" + equipment.get())
				+ (this.flags.isEmpty() ? "" : ",flags=" + flags.get())
				+ (this.nbt.isEmpty() ? "" : ",nbt=" + nbt.get())
				+ (this.passenger.isEmpty() ? "" : ",passenger(s)=" + passenger.get().report()) + "}";
	}

}
