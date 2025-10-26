package com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.entity;

import java.util.Optional;

import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.IEntityGenreProvider;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;

public record EntityFlagsGenreProvider(Optional<Boolean> onFire, Optional<Boolean> isBaby)
		implements IEntityGenreProvider<Entity, Entity> {

	public static final Codec<EntityFlagsGenreProvider> CODEC = Codec
			.pair(Codec.BOOL.optionalFieldOf("is_on_fire").codec(), Codec.BOOL.optionalFieldOf("is_baby").codec())
			.xmap((s) -> new EntityFlagsGenreProvider(s.getFirst(), s.getSecond()), (s) -> Pair.of(s.onFire, s.isBaby));

	@Override
	public ProviderType<EntityFlagsGenreProvider> providerType() {

		return ProviderType.ENTITY_FLAGS;
	}

	@Override
	public boolean matches(ServerLevel level, Entity instance) {

		return (onFire.isEmpty() || onFire.get().equals(instance.isOnFire()))
				&& (isBaby.isEmpty() || (instance instanceof AgeableMob age && isBaby.get().equals(age.isBaby())));
	}

	@Override
	public Entity generateRandom(ServerLevel level, Optional<Entity> prev) {
		Entity en = prev.orElseThrow();
		if (onFire.isPresent()) {
			if (onFire.get()) {
				en.setRemainingFireTicks(100);
			} else {
				en.extinguishFire();
			}
		}
		if (isBaby.isPresent()) {
			if (en instanceof AgeableMob mob) {
				mob.setBaby(isBaby.get());
			}
		}
		return prev.get();
	}

	@Override
	public boolean matchesEntity(ServerLevel level, Entity entity) {
		return this.matches(level, entity);
	}

	@Override
	public Entity generateRandomForEntity(ServerLevel level, Optional<Entity> entity) {
		return this.generateRandom(level, entity);
	}

	@Override
	public final String toString() {
		return "Flags(" + onFire.map((s) -> (s ? "" : "!") + "on_fire,").orElse("")
				+ isBaby.map((s) -> (s ? "" : "!") + "isBaby").orElse("") + ")";
	}

}
