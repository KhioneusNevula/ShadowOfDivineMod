package com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.creator;

import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockCreator implements IGenreCreator {

	private final Logger LOGGER = LogUtils.getLogger();

	private BlockState state;
	private Function<BlockEntity, Boolean> modifyBlockEntity;
	private Supplier<ItemStack> stackGetter;

	public BlockCreator(BlockState state, Function<BlockEntity, Boolean> entity, Supplier<ItemStack> stackGetter) {
		this.state = state;
		modifyBlockEntity = entity;
		this.stackGetter = stackGetter;
	}

	/**
	 * The state this placer constructs
	 * 
	 * @return
	 */
	public BlockState getState() {
		return state;
	}

	/**
	 * The function which modifies the block entity after placement
	 * 
	 * @return
	 */
	public Function<BlockEntity, Boolean> getModifyBlockEntity() {
		return modifyBlockEntity;
	}

	@Override
	public boolean tryPlace(ServerLevel level, BlockPos at) {
		boolean b = level.setBlockAndUpdate(at, state);

		modifyBlockEntity.apply(level.getBlockEntity(at));
		return b;
	}

	public ItemStack getAsItem(ServerLevel level, BlockPos pos) {
		return stackGetter.get();
	}

}
