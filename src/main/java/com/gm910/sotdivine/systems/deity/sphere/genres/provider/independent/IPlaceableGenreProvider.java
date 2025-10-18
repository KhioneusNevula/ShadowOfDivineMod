package com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent;

import java.util.Optional;

import com.gm910.sotdivine.systems.deity.sphere.genres.creator.IGenreCreator;
import com.gm910.sotdivine.systems.deity.sphere.genres.creator.IGenrePlacer;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Genre provider for something which can be placed at a block position in the
 * world
 * 
 * @param <T>
 * @param <G>
 */

public interface IPlaceableGenreProvider<T, G extends IGenrePlacer> extends IGenreProvider<T, G> {

	/**
	 * Whether the specific position matches
	 * 
	 * @param emanation
	 * @return
	 */
	public boolean matchesPos(ServerLevel level, BlockPos pos);

}
