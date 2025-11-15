package com.gm910.sotdivine.concepts.genres.creator;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

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

	/**
	 * Return the block state this placer creates, or an emtpy
	 * 
	 * @return
	 */
	public Optional<BlockState> getBlockState();
}
