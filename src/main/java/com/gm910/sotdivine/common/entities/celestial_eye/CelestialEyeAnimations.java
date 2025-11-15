package com.gm910.sotdivine.common.entities.celestial_eye;

// Save this class in your mod and generate all required imports

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Made with Blockbench 5.0.2 Exported for Minecraft version 1.19 or later with
 * Mojang mappings
 * 
 * @author Author
 */
public class CelestialEyeAnimations {
	public static final AnimationDefinition eye_move = AnimationDefinition.Builder.withLength(1.96F)
			.addAnimation("pupil",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.24F, KeyframeAnimations.degreeVec(-11.1658F, 12.2485F, 10.8572F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 32.5F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.84F, KeyframeAnimations.degreeVec(7.2572F, 31.4166F, 11.6548F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.04F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.28F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.64F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.96F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("pupil",
					new AnimationChannel(AnimationChannel.Targets.POSITION,
							new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.32F, KeyframeAnimations.posVec(1.0F, 0.0F, 2.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(0.84F, KeyframeAnimations.posVec(-1.0F, 0.0F, -2.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.28F, KeyframeAnimations.posVec(2.0F, 0.0F, -2.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.68F, KeyframeAnimations.posVec(-2.0F, 0.0F, 2.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.96F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR)))
			.build();

	public static final AnimationDefinition rotation = AnimationDefinition.Builder.withLength(1.92F).looping()
			.addAnimation("eye_WE",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 180.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.92F, KeyframeAnimations.degreeVec(0.0F, -180.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR)))
			.addAnimation("eye_NS",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 180.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(1.92F, KeyframeAnimations.degreeVec(0.0F, -180.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR)))
			.build();
}