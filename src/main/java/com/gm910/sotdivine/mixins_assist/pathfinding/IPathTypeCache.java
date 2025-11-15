package com.gm910.sotdivine.mixins_assist.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathTypeCache;

public interface IPathTypeCache {

	public static IPathTypeCache get(ServerLevel level) {
		return (IPathTypeCache) level.getPathTypeCache();
	}

	/**
	 * Same as {@link PathTypeCache#getOrCompute(BlockGetter, BlockPos)}, but
	 * sensitive to the mobID
	 * 
	 * @param getter
	 * @param pos
	 * @param forMob
	 * @return
	 */
	public PathType getOrCompute(BlockGetter getter, BlockPos pos, Mob forMob);

	/**
	 * Same idea as {@link PathTypeCache#invalidate(BlockPos)}, but sensitive to
	 * sanctuary presence
	 * 
	 * @param pos
	 */
	public void invalidateSanctuary(BlockPos pos);
}
