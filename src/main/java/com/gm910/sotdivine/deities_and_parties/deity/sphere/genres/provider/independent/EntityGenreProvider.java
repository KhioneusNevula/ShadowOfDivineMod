package com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.creator.EntityCreator;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.data.ComponentMapProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.data.other.NbtInternalProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.entity.EntityFlagsGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.entity.EquipmentGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.entity.MobEffectGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.entity_preds.ITypeSpecificProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.entity_preds.TypeSpecificProviders;
import com.gm910.sotdivine.util.HolderUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Notes:
 * 
 * @param flags only allows "onFire" and "baby" as permissible flags; all others
 *              are not permitted
 */
public record EntityGenreProvider(EntityTypePredicate entityType, Collection<MobEffectGenreProvider> effects,
		Optional<NbtInternalProvider> nbt, Optional<EntityFlagsGenreProvider> flags,
		Optional<EquipmentGenreProvider> equipment, Optional<EntityGenreProvider> passenger,
		ComponentMapProvider components, Collection<ITypeSpecificProvider<?>> typeSpecific)
		implements IPlaceableGenreProvider<Entity, EntityCreator>, IEntityGenreProvider<Entity, EntityCreator> {

	private static Codec<EntityGenreProvider> CODEC = null;

	public EntityGenreProvider(EntityTypePredicate type) {
		this(type, Set.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				ComponentMapProvider.ANY, List.of());
	}

	public EntityGenreProvider(EntityTypePredicate type, Collection<ITypeSpecificProvider<?>> provider) {
		this(type, Set.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				ComponentMapProvider.ANY, Set.copyOf(provider));
	}

	public static Codec<EntityGenreProvider> codec() {
		if (CODEC == null) {
			Codec<EntityGenreProvider> entityType = EntityTypePredicate.CODEC
					.flatComapMap((et) -> new EntityGenreProvider(et), (eg) -> {
						if (eg.components.isEmpty() && eg.effects.isEmpty() && eg.equipment.isEmpty()
								&& eg.flags.isEmpty() && eg.nbt.isEmpty() && eg.passenger.isEmpty()
								&& eg.typeSpecific.isEmpty()) {
							return DataResult.success(eg.entityType);
						}
						return DataResult.error(
								() -> "Doesn't make sense to serialize only one aspect of complex entity provider",
								eg.entityType);
					});
			Codec<EntityGenreProvider> construction = Codec.recursive("EntityGenreProvider",
					(recurse) -> RecordCodecBuilder.create(instance -> instance.group(
							EntityTypePredicate.CODEC.fieldOf("entities").forGetter(EntityGenreProvider::entityType),
							Codec.list(MobEffectGenreProvider.codec()).optionalFieldOf("effects", List.of())
									.forGetter((x) -> new ArrayList<>(x.effects)),
							NbtInternalProvider.CODEC.optionalFieldOf("nbt").forGetter(EntityGenreProvider::nbt),
							EntityFlagsGenreProvider.CODEC.optionalFieldOf("flags")
									.forGetter(EntityGenreProvider::flags),
							EquipmentGenreProvider.codec().optionalFieldOf("equipment")
									.forGetter(EntityGenreProvider::equipment),
							recurse.optionalFieldOf("passenger").forGetter(EntityGenreProvider::passenger),
							ComponentMapProvider.codec().forGetter(EntityGenreProvider::components),
							Codec.list(TypeSpecificProviders.codec()).optionalFieldOf("type_specific", List.of())
									.forGetter((x) -> new ArrayList<>(x.typeSpecific)))
							.apply(instance, (et, ef, nbt, fl, eq, pa, co, ts) -> new EntityGenreProvider(et, ef, nbt,
									fl, eq, pa, co, ts))));
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
		if (!HolderUtils.holderSetContains(entityType.types(), instance.getType().builtInRegistryHolder())) {
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
	public EntityCreator generateRandom(ServerLevel level, Optional<Entity> prev) {
		EntityType<?> type = entityType.types().stream()
				.flatMap((h) -> Optional.ofNullable(h.isBound() ? h.get() : null).stream()).findAny().orElseThrow();
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
			return HolderUtils.holderSetEquals(this.entityType.types(), egp.entityType.types())
					&& this.components.equals(egp.components) && this.effects.equals(egp.effects)
					&& this.equipment.equals(egp.equipment) && this.flags.equals(egp.flags) && this.nbt.equals(egp.nbt)
					&& this.passenger.equals(egp.passenger);
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return HolderUtils.holderSetHashCode(this.entityType.types()) + this.components.hashCode()
				+ this.effects.hashCode() + this.equipment.hashCode() + this.flags.hashCode() + this.nbt.hashCode()
				+ this.passenger.hashCode();
	}

	@Override
	public final String toString() {
		return "Entity(" + entityType.types().unwrap().map(Object::toString, Object::toString) + ")";
	}

	@Override
	public String report() {
		return "Entity{types=" + entityType().types().unwrap().map(Object::toString, Object::toString)
				+ (components.isEmpty() ? "" : ",components=" + components)
				+ (this.effects.isEmpty() ? "" : ",effects=" + effects)
				+ (this.equipment.isEmpty() ? "" : ",equipment=" + equipment.get())
				+ (this.flags.isEmpty() ? "" : ",flags=" + flags.get())
				+ (this.nbt.isEmpty() ? "" : ",nbt=" + nbt.get())
				+ (this.passenger.isEmpty() ? "" : ",passenger(s)=" + passenger.get().report()) + "}";
	}

}
