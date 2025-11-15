package com.gm910.sotdivine.magic.emanation;

import java.util.Optional;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.magic.emanation.EmanationDataType.IEmanationInstanceData;
import com.gm910.sotdivine.magic.emanation.spell.ISpellTargetInfo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerLevel;

/**
 * An emanation emanation. Emanation instances are considered identical if they
 * have the same emanation and SPELL properties. Otherwise, they are not.
 */
public class EmanationInstance {

	public static final Codec<EmanationInstance> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group(IEmanation.codec().fieldOf("emanation").forGetter(EmanationInstance::emanation),
			ISpellTargetInfo.DEFICIENT_CODEC.fieldOf("targetInfo").forGetter(EmanationInstance::targetInfo),
			Codec.INT.fieldOf("ticks").forGetter(EmanationInstance::getTicks),
			EmanationDataType.DISPATCH_CODEC.optionalFieldOf("extraData")
					.forGetter((e) -> Optional.ofNullable(e.extraData)),
			Codec.FLOAT.fieldOf("intensity").forGetter(EmanationInstance::getIntensity))
			.apply(instance, EmanationInstance::new));

	private IEmanation emanation;
	private ISpellTargetInfo spellTarget;
	private int ticks;
	private float intensity;
	/**
	 * Extra bit of data (not used in equals/hashcode) to store additional info
	 * while executing
	 */
	public IEmanationInstanceData extraData;

	public EmanationInstance(IEmanation emanation, ISpellTargetInfo target, int ticks,
			Optional<IEmanationInstanceData> dat, float intensity) {
		this(emanation, target, ticks, intensity);
		this.extraData = dat.orElse(null);
	}

	public EmanationInstance(IEmanation emanation, ISpellTargetInfo target, float intensity) {
		this(emanation, target, 0, intensity);

	}

	public EmanationInstance(IEmanation emanation, ISpellTargetInfo target, int ticks, float intensity) {
		this.emanation = emanation;
		this.spellTarget = target;
		this.ticks = ticks;
		this.intensity = intensity;
	}

	public float getIntensity() {
		return intensity;
	}

	public int getTicks() {
		return ticks;
	}

	public void incrementTicks() {
		ticks++;
	}

	public void ticksToZero() {
		ticks = 0;
	}

	public ISpellTargetInfo targetInfo() {
		return spellTarget;
	}

	public IEmanation emanation() {
		return emanation;
	}

	/**
	 * Equivalent to {@link ISpellTargetInfo#complete(IDeity, ServerLevel)}; returns
	 * this emanation, NOT a copy
	 * 
	 * @param deity
	 * @param level
	 */
	public EmanationInstance completeSelf(@Nullable IDeity deity, @Nullable ServerLevel level) {
		spellTarget = spellTarget.complete(deity, level);
		return this;
	}

	/**
	 * Coalesces the emanation emanation in this with the emanation emanation of the
	 * deity to avoid redundancy
	 * 
	 * @param deity
	 * @return
	 */
	public EmanationInstance coalesce(IDeity deity) {
		deity.spheres().stream().flatMap((f) -> f.allEmanations().stream()).filter((x) -> x.equals(this.emanation))
				.findAny().ifPresent((em) -> this.emanation = em);

		return this;
	}

	@Override
	public String toString() {
		return "EmanationInstance(\"" + emanation + "\"){targetInfo=" + spellTarget + ",ticks=" + ticks
				+ (extraData != null ? ",extraData=" + extraData : "") + ",intensity=" + intensity + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof EmanationInstance em) {
			return this.emanation.equals(em.emanation) && this.spellTarget.equals(em.spellTarget);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return emanation.hashCode() + spellTarget.hashCode();
	}

}
