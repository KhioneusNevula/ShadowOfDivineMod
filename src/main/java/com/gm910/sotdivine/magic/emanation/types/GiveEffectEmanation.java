package com.gm910.sotdivine.magic.emanation.types;

import java.util.Optional;

import com.gm910.sotdivine.magic.emanation.EmanationInstance;
import com.gm910.sotdivine.magic.emanation.EmanationType;
import com.gm910.sotdivine.magic.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.magic.emanation.spell.SpellAlignment;
import com.gm910.sotdivine.util.FieldUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectCategory;
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
			ISpellProperties.CODEC.optionalFieldOf("spell_properties").forGetter((p) -> p.optionalSpellProperties()))
			.apply(instance, GiveEffectEmanation::new));

	private MobEffectInstance effectInstance;

	private static final int DEFAULT_DURATION = 1200;

	public GiveEffectEmanation(MobEffectInstance effect, Optional<ISpellProperties> properties) {
		super(true, false, properties);
		this.effectInstance = new MobEffectInstance(effect.getEffect(), DEFAULT_DURATION, effect.getAmplifier(),
				effect.isAmbient(), effect.isVisible(), effect.showIcon(),
				FieldUtils.getInstanceField("hiddenEffect", "m", effect));
	}

	@Override
	protected String emanationName() {
		return "Effect(" + effectInstance + ")";
	}

	@Override
	public boolean createsObject() {
		return false;
	}

	@Override
	public boolean damagesTarget() {
		return effectInstance.getEffect().get().isInstantenous()
				&& effectInstance.getEffect().get().getCategory() == MobEffectCategory.HARMFUL;
	}

	@Override
	public Component translate() {
		MutableComponent mutablecomponent = effectInstance.getEffect().value().getDisplayName().copy();
		if (effectInstance.getAmplifier() >= 1 && effectInstance.getAmplifier() <= 9) {
			mutablecomponent.append(CommonComponents.SPACE)
					.append(Component.translatable("enchantment.level." + (effectInstance.getAmplifier() + 1)));
		}
		return TextUtils
				.transPrefix(
						"sotd.emanation.give_effect." + (this.optionalSpellProperties().isEmpty() ? "pragma"
								: this.optionalSpellProperties().get().alignment().name().toLowerCase()),
						mutablecomponent);
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
	public boolean trigger(EmanationInstance info, float intensity) {
		if (info.targetInfo().opTargetEntity().isEmpty()) {
			LogUtils.getLogger().error("Received no entity targeting info; could not run " + this);
			return true;
		}
		if (getEntity(info.targetInfo(), LivingEntity.class).orElse(null) instanceof LivingEntity creature) {
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
