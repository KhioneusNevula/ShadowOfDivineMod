package com.gm910.sotdivine.concepts.genres.creator;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Genre creator to create an item stack
 */
public record ItemStackCreator(ItemStack stack) implements IGenreCreator {

	public static final ItemStackCreator EMPTY = new ItemStackCreator(ItemStack.EMPTY);

	@Override
	public boolean tryPlace(ServerLevel level, BlockPos at) {
		return false;
	}

	@Override
	public ItemStack getAsItem(ServerLevel level, BlockPos pos) {
		return stack.copy();
	}

	@Override
	public Optional<BlockState> getBlockState() {
		return Optional.empty();
	}

}
