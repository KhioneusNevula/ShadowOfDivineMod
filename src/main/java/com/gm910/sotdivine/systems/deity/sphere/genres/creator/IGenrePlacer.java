package com.gm910.sotdivine.systems.deity.sphere.genres.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Something which can place elements of a genre
 */
public interface IGenrePlacer {

	/**
	 * Try to place an emanation of whatever this is at the given position. Return
	 * false if not possible.
	 * 
	 * @param level
	 * @param at
	 * @return
	 */
	public boolean tryPlace(ServerLevel level, BlockPos at);
}
