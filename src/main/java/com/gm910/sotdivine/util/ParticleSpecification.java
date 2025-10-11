package com.gm910.sotdivine.util;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Specification for generating particles, assuming that this specification will
 * be used at a specific position
 */
public record ParticleSpecification(ParticleOptions particle, Vec3 offset, Vec3 delta, double speed, int count,
		boolean force, boolean alwaysVisible) {
	/**
	 * Codec makes all fields except particle type optional
	 */
	public static final Codec<ParticleSpecification> CODEC = RecordCodecBuilder.create(instance -> // Given an instance
	instance.group(
			Codec.either(BuiltInRegistries.PARTICLE_TYPE.byNameCodec(), ParticleTypes.CODEC).fieldOf("particle")
					.forGetter((p) -> Either.right(p.particle())),
			Vec3.CODEC.optionalFieldOf("offset").forGetter((p) -> Optional.of(p.offset())),
			Vec3.CODEC.optionalFieldOf("delta").forGetter((p) -> Optional.of(p.delta())),
			Codec.DOUBLE.optionalFieldOf("speed").forGetter((p) -> Optional.of(p.speed())),
			Codec.INT.optionalFieldOf("count").forGetter((p) -> Optional.of(p.count())),
			Codec.BOOL.optionalFieldOf("force").forGetter((p) -> Optional.of(p.force())),
			Codec.BOOL.optionalFieldOf("alwaysVisible").forGetter((p) -> Optional.of(p.alwaysVisible()))

	).apply(instance, ParticleSpecification::new));

	private ParticleSpecification(Either<ParticleType<?>, ParticleOptions> options, Optional<Vec3> offset,
			Optional<Vec3> delta, Optional<Double> speed, Optional<Integer> count, Optional<Boolean> force,
			Optional<Boolean> alwaysVisible) {
		this(options.left().filter((l) -> l instanceof ParticleOptions).map((l) -> (ParticleOptions) l)
				.or(options::right).orElseThrow(() -> new IllegalArgumentException("Nonsensical particle " + options)),
				offset.orElse(Vec3.ZERO), delta.orElse(new Vec3(1, 1, 1)), speed.orElse(0.0), count.orElse(1),
				force.filter(Boolean::booleanValue).orElse(false),
				alwaysVisible.filter(Boolean::booleanValue).orElse(force.filter(Boolean::booleanValue).orElse(false)));

	}

	/**
	 * Display the particle specified here at the given position
	 * 
	 * @param level
	 * @param pos
	 */
	public void sendParticle(ServerLevel level, Vec3 pos) {
		level.sendParticles(this.particle, force, alwaysVisible, pos.add(offset).x, pos.add(offset).y,
				pos.add(offset).z, count, delta.x, delta.y, delta.z, speed);
	}
}
