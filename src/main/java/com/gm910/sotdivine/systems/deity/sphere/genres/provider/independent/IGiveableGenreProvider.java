package com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent;

import com.gm910.sotdivine.systems.deity.sphere.genres.creator.IGenreItemGiver;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Genre provider for something which can be placed at a block position in the
 * world
 * 
 * @param <T>
 * @param <G>
 */

public interface IGiveableGenreProvider<T, G extends IGenreItemGiver> extends IGenreProvider<T, G> {

	/**
	 * Whether the specific item stack matches
	 * 
	 * @param emanation
	 * @return
	 */
	public boolean matchesItem(ServerLevel level, ItemStack stack);

}
