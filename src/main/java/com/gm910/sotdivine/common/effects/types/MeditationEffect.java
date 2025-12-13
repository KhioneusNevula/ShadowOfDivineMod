package com.gm910.sotdivine.common.effects.types;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class MeditationEffect extends MobEffect {

	public static final int USUAL_TIME = 1 * 20;

	public MeditationEffect(MobEffectCategory category, int color) {
		super(category, color, ParticleTypes.ENCHANT);
	}

}
