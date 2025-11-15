package com.gm910.sotdivine.mixins_assist.pathfinding;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.gm910.sotdivine.Config;
import com.gm910.sotdivine.common.misc.ParticleSpecification;
import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;
import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeBlock;

public class PathfindingMixinHelper {
	public static final PathType SANCTUARY_BARRIER_PATH_TYPE = PathType.create("SANCTUARY_BARRIER", -1.0f);
	public static final PathType SANCTUARY_AVOID_PATH_TYPE = PathType.create("SANCTUARY_AVOID", 12f);

	private PathfindingMixinHelper() {

	}

	/**
	 * Substitution for
	 * {@link IForgeBlock#getAdjacentBlockPathType(BlockState, BlockGetter, BlockPos, Mob, PathType)}
	 * to replace pathtypes in sanctuaries
	 * 
	 * @param state
	 * @param world
	 * @param pos
	 * @param mobID
	 * @param originalType
	 * @param pathType
	 */
	public static PathType getAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos,
			@Nullable Mob mob, PathType originalType) {
		PathType typa = tryReturnSanctuaryPathType(pos.getX(), pos.getY(), pos.getZ(), mob);
		if (typa != null) {
			// if (mob instanceof Monster)
			// new ParticleSpecification(ParticleTypes.ENCHANTED_HIT, Vec3.ZERO, new
			// Vec3(0.2, 0.2, 0.2), 0, 12, false,
			// false).sendParticle((ServerLevel) level, pos.getBottomCenter());
			return SANCTUARY_AVOID_PATH_TYPE;
		}

		return originalType;
	}

	public static PathType tryReturnSanctuaryPathType(int x, int y, int z, Mob mob) {
		if (mob.level() instanceof ServerLevel level) {
			ISanctuarySystem system = ISanctuarySystem.get(level);
			BlockPos pos = new BlockPos(x, y, z);
			Optional<ISanctuary> blockerOptional = system.getSanctuaryAtPos(pos);

			if (blockerOptional.isPresent()) {

				int time = blockerOptional.get().timeUntilForbidden(mob);
				if (time <= Config.sanctuaryEscapeTime) {

					if (time <= 0)
						return (SANCTUARY_BARRIER_PATH_TYPE);
					return SANCTUARY_AVOID_PATH_TYPE;
				}
			}
		}
		return null;

	}

}
