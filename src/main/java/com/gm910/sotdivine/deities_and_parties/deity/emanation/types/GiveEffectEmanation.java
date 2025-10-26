package com.gm910.sotdivine.deities_and_parties.deity.emanation.types;

import com.gm910.sotdivine.deities_and_parties.deity.emanation.EmanationInstance;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.EmanationType;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.util.FieldUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Emanation which grants a status effect
 * 
 * @author borah
 *
 */
public class GiveEffectEmanation extends AbstractEmanation {
	public static final Codec<GiveEffectEmanation> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group(MobEffectInstance.CODEC.fieldOf("effect").forGetter(GiveEffectEmanation::getEffectInstance),
			ISpellProperties.CODEC.fieldOf("spell_properties").forGetter((p) -> p.optionalSpellProperties().get()))
			.apply(instance, GiveEffectEmanation::new));

	private MobEffectInstance effectInstance;

	private static final int DEFAULT_DURATION = 1200;

	public GiveEffectEmanation(MobEffectInstance effect, ISpellProperties properties) {
		super(true, false, properties);
		this.effectInstance = new MobEffectInstance(effect.getEffect(), DEFAULT_DURATION, effect.getAmplifier(),
				effect.isAmbient(), effect.isVisible(), effect.showIcon(),
				FieldUtils.getInstanceField("hiddenEffect", "m", effect));
	}

	@Override
	protected String emanationName() {
		return "give_effect_" + effectInstance.getEffect().unwrap()
				.map((x) -> x.location().toString().replace(":", "_"), (x) -> BuiltInRegistries.MOB_EFFECT.getKey(x));
	}

	@Override
	public boolean isDurative() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof GiveEffectEmanation em)
			return this.effectInstance.equals(em.effectInstance) && super.equals(obj);
		return false;
	}

	@Override
	public int hashCode() {
		return this.effectInstance.hashCode();
	}

	@Override
	public String toString() {
		return "GiveEffectEmanation(" + this.effectInstance.toString() + ")";
	}

	@Override
	public boolean trigger(EmanationInstance info, float intensity) {
		if (info.targetInfo().opTargetEntity().isEmpty())
			return true;
		if (getEntity(info.targetInfo()).orElse(null) instanceof LivingEntity creature) {
			creature.addEffect(new MobEffectInstance(effectInstance).withScaledDuration(intensity));
			return false;
		}
		return true;
	}

	@Override
	public boolean tick(EmanationInstance instance) {
		int ticks = (int) (DEFAULT_DURATION * instance.getIntensity()) - 10; // minor grace period just because
		if (instance.getTicks() > ticks) { // if the effect instance has already used all its time, we good
			return false;
		}
		return instance.targetInfo().opTargetEntity()
				.map((x) -> x.getEntity(instance.targetInfo().level(), Entity.class))
				.map((x) -> (LivingEntity) (x instanceof LivingEntity ? x : null))
				.filter((x) -> x.hasEffect(this.effectInstance.getEffect())).isEmpty();
	}

	@Override
	public void interrupt(EmanationInstance instance) {
		instance.targetInfo().opTargetEntity().map((x) -> x.getEntity(instance.targetInfo().level(), Entity.class))
				.map((x) -> (LivingEntity) (x instanceof LivingEntity ? x : null))
				.filter((x) -> x.hasEffect(this.effectInstance.getEffect()))
				.ifPresent((liv) -> liv.removeEffect(this.effectInstance.getEffect()));
	}

	@Override
	public EmanationType<GiveEffectEmanation> getEmanationType() {
		return EmanationType.GIVE_EFFECT.get();
	}

	public MobEffectInstance getEffectInstance() {
		return effectInstance;
	}

}
