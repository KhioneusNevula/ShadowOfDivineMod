package com.gm910.sotdivine.systems.deity.emanation.types;

import java.util.Objects;

import com.gm910.sotdivine.systems.deity.emanation.EmanationInstance;
import com.gm910.sotdivine.systems.deity.emanation.EmanationType;
import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellProperties;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;

/**
 * Emanation which grants a status effect
 * 
 * @author borah
 *
 */
public class GiveEffectEmanation extends AbstractEmanation {
	public static final Codec<GiveEffectEmanation> CODEC = RecordCodecBuilder.create(instance -> // Given an instance
	instance.group(MobEffectInstance.CODEC.fieldOf("effect").forGetter(GiveEffectEmanation::getEffectInstance),
			ISpellProperties.CODEC.fieldOf("spell_properties").forGetter((p) -> p.optionalSpellProperties().get()))
			.apply(instance, GiveEffectEmanation::new));

	private MobEffectInstance effectInstance;

	public GiveEffectEmanation(MobEffectInstance effect, ISpellProperties properties) {
		super(true, false, properties);
		this.effectInstance = effect;
	}

	@Override
	protected String emanationName() {
		return "give_effect_" + effectInstance.getEffect().getRegisteredName();
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
	public boolean trigger(EmanationInstance info) {
		if (info.targetInfo().opTargetEntity().isEmpty())
			return true;
		EntityReference<Entity> entity = info.targetInfo().opTargetEntity().get();
		if (getEntity(info.targetInfo()).orElse(null) instanceof LivingEntity creature) {
			creature.addEffect(new MobEffectInstance(effectInstance));
			return false;
		}
		return true;
	}

	@Override
	public EmanationType<GiveEffectEmanation> getEmanationType() {
		return EmanationType.GIVE_EFFECT.get();
	}

	public MobEffectInstance getEffectInstance() {
		return effectInstance;
	}

}
