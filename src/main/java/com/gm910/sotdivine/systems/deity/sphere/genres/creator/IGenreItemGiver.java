package com.gm910.sotdivine.systems.deity.sphere.genres.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * A version of a genre creator which gives an item
 */
public interface IGenreItemGiver {

	/**
	 * Return the element in this giver as an item with metadata. Return
	 * ItemStack.EMPTY if not possible
	 * 
	 * @return
	 */
	ItemStack getAsItem(ServerLevel level, BlockPos pos);
}
