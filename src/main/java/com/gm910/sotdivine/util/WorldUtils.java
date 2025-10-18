package com.gm910.sotdivine.util;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public class WorldUtils {

	private WorldUtils() {
	}

	/**
	 * Return the central block pos (with y = 0) of a group of chunkposes
	 * 
	 * @param others
	 * @return
	 */
	public static BlockPos centerCP(Collection<ChunkPos> others) {
		return BlockPos.containing(others.stream().map((c) -> c.getMiddleBlockPosition(0)).map(BlockPos::getCenter)
				.reduce(Vec3.ZERO, (a, b) -> a.add(b)).scale(1.0 / others.size()));
	}

	/**
	 * Gets the center/average of a group of blocks
	 * 
	 * @param others
	 * @return
	 */
	public static BlockPos center(Collection<BlockPos> others) {
		return BlockPos.containing(others.stream().map(BlockPos::getCenter).reduce(Vec3.ZERO, (a, b) -> a.add(b))
				.scale(1.0 / others.size()));
	}

}
