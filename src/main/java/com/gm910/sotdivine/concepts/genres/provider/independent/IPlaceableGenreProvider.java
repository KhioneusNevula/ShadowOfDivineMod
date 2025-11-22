package com.gm910.sotdivine.concepts.genres.provider.independent;

import com.gm910.sotdivine.concepts.genres.creator.IGenrePlacer;
import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;

/**
 * Genre provider for something which can be placed at a block rawPosition in the
 * world
 * 
 * @param <T>
 * @param <G>
 */

public interface IPlaceableGenreProvider<T, G extends IGenrePlacer> extends IGenreProvider<T, G> {

	/**
	 * Whether the specific rawPosition matches
	 * 
	 * @param emanation
	 * @return
	 */
	public boolean matchesPos(ServerLevel level, BlockPos pos);

	/**
	 * Whether this can be placed at the given pos
	 * 
	 * @param state
	 * @return
	 */
	public boolean canPlaceOn(LevelReader level, BlockPos pos);

	/**
	 * If this block can be right-clicked
	 * 
	 * @return
	 */
	public boolean canRightClick();

}
