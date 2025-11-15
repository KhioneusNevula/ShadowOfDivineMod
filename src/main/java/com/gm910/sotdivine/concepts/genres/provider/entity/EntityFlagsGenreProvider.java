package com.gm910.sotdivine.concepts.genres.provider.entity;

import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.concepts.genres.provider.independent.IEntityGenreProvider;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;

public record EntityFlagsGenreProvider(Optional<Boolean> onFire, Optional<Boolean> isBaby)
		implements IEntityGenreProvider<Entity, Entity> {

	public static final Codec<EntityFlagsGenreProvider> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(Codec.BOOL.optionalFieldOf("is_on_fire").forGetter(EntityFlagsGenreProvider::onFire),
					Codec.BOOL.optionalFieldOf("is_baby").forGetter(EntityFlagsGenreProvider::isBaby))
			.apply(instance, EntityFlagsGenreProvider::new));

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

	@Override
	public Component translate() {
		var onFText = onFire.map((s) -> s ? "" : ".not")
				.map((x) -> TextUtils.transPrefix("sotd.genre.provider.flags.onfire" + x));
		var iBText = isBaby.map((s) -> s ? "" : ".not")
				.map((x) -> TextUtils.transPrefix("sotd.genre.provider.flags.baby" + x));
		if (!isBaby.isPresent() && !onFire.isPresent()) {
			return TextUtils.transPrefix("sotd.genre.provider.flags.0");
		} else if (isBaby.isPresent() && onFire.isPresent()) {
			return TextUtils.transPrefix("sotd.genre.provider.flags.2", onFText, iBText);
		} else {
			return TextUtils.transPrefix("sotd.genre.provider.flags.1", onFText.or(() -> iBText).get());
		}
	}

}
