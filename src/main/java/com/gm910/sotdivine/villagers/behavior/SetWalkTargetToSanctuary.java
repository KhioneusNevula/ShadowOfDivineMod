package com.gm910.sotdivine.villagers.behavior;

import java.util.Optional;

import com.gm910.sotdivine.common.misc.ParticleSpecification;
import com.gm910.sotdivine.magic.sanctuary.ICachedSanctuaries;
import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;
import com.gm910.sotdivine.util.CollectionUtils;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetToSanctuary {

	public static BehaviorControl<PathfinderMob> sanctuary(MemoryModuleType<? extends ICachedSanctuaries> mem,
			float speedModifier) {
		return create(mem, speedModifier);
	}

	private static OneShot<PathfinderMob> create(MemoryModuleType<? extends ICachedSanctuaries> memoire,
			float speedModifier) {
		return BehaviorBuilder.create(
				instance -> instance.group(instance.registered(MemoryModuleType.WALK_TARGET), instance.present(memoire))
						.apply(instance, (setWalkTarget, getPositional) -> (level, mob, ticks) -> {
							ICachedSanctuaries system = instance.get(getPositional);
							Optional<WalkTarget> optional = instance.tryGet(setWalkTarget);

							if (optional.filter(
									(s) -> system.getSanctuaryAtPos(s.getTarget().currentBlockPosition()).isPresent())
									.isEmpty()) {

								for (int i = 0; i < 20; i++) {
									Vec3 vec34 = LandRandomPos.getPos(mob, 64, 7,
											(bp) -> system.getSanctuaryAtPos(bp).isPresent() ? 10.0f : 0.0f);
									if (vec34 != null) {
										if (system.getSanctuaryAtPos(vec34).isPresent()) {
											new ParticleSpecification(ParticleTypes.END_ROD, Vec3.ZERO,
													new Vec3(0.2, 0.2, 0.2), 0, 12, false, false)
													.sendParticle((ServerLevel) level, vec34);
											setWalkTarget.set(new WalkTarget(vec34, speedModifier, 0));
											return true;
										} else {
											/*new ParticleSpecification(ParticleTypes.FLAME, Vec3.ZERO,
													new Vec3(0.2, 0.2, 0.2), 0, 12, false, false)
													.sendParticle((ServerLevel) level, vec34);*/
										}
									}
								}

							}
							return false;
						}));
	}
}
