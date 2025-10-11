package com.gm910.sotdivine.systems.deity.emanation.types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gm910.sotdivine.systems.deity.emanation.EmanationInstance;
import com.gm910.sotdivine.systems.deity.emanation.EmanationType;
import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.ParticleSpecification;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;

public class ParticleEmanation extends AbstractEmanation {
	public static final Codec<ParticleEmanation> CODEC = RecordCodecBuilder.create(instance -> // Given an instance
	instance.group(
			Codec.list(ParticleSpecification.CODEC).fieldOf("particles")
					.forGetter((p) -> new ArrayList<>(p.particles())),
			ISpellProperties.CODEC.optionalFieldOf("spell_properties").forGetter((p) -> p.optionalSpellProperties())

	).apply(instance, ParticleEmanation::new));

	private HashSet<ParticleSpecification> particles;

	public ParticleEmanation(List<ParticleSpecification> particles, Optional<ISpellProperties> properties) {
		super(true, true, properties.orElse(null));
		if (particles.isEmpty())
			throw new IllegalArgumentException("Cannot have empty particle list");
		this.particles = new HashSet<>(particles);
	}

	public Set<ParticleSpecification> particles() {
		return particles;
	}

	@Override
	public boolean trigger(EmanationInstance info) {
		getEntity(info.targetInfo()).map((x) -> x.position())
				.or(() -> info.targetInfo().opTargetPos().map(GlobalPos::pos).map(BlockPos::getBottomCenter))
				.ifPresent((pos) -> {
					getCorrectLevel(info.targetInfo())
							.ifPresent((l) -> this.particles.stream().forEach((p) -> p.sendParticle(l, pos)));
				});
		return false;
	}

	@Override
	public EmanationType<?> getEmanationType() {
		return EmanationType.PARTICLE.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof ParticleEmanation em) {
			return this.particles.equals(em.particles) && super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() + this.particles.hashCode();
	}

	@Override
	protected String emanationName() {

		return "Particles["
				+ this.particles.stream().map((p) -> BuiltInRegistries.PARTICLE_TYPE.getKey(p.particle().getType()))
						.collect(ModUtils.setStringCollector(","))
				+ "]";
	}

}
