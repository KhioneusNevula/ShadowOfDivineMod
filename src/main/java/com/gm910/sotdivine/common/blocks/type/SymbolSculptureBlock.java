package com.gm910.sotdivine.common.blocks.type;

import javax.annotation.Nullable;

import com.gm910.sotdivine.common.blocks.SymbolBlocks;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.concepts.symbol.ISymbolBlock;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A block for a sculpture which is symbolic
 */
public class SymbolSculptureBlock extends HorizontalDirectionalBlock implements ISymbolBlock {
	public static final MapCodec<SymbolSculptureBlock> CODEC = RecordCodecBuilder
			.mapCodec(p_360402_ -> p_360402_.group(propertiesCodec()).apply(p_360402_, SymbolSculptureBlock::new));

	public SymbolSculptureBlock(Properties props) {
		super(props.lightLevel(stata -> stata.getValue(SymbolBlocks.ACTIVE) ? 15 : 0));
	}

	@Override
	public void onBlockStateChange(LevelReader lread, BlockPos pos, BlockState oldState, BlockState newState) {
		SymbolBlocks.onBlockStateChange(this, lread, pos, oldState, newState);
		super.onBlockStateChange(lread, pos, oldState, newState);
	}

	@Override
	public BlockState convertSymbols(IDeitySymbol to) {
		// TODO allow symbolic blocks of varios kinds to convert among one another
		return null;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55673_) {
		p_55673_.add(SymbolBlocks.ACTIVE);
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctxt) {
		return this.defaultBlockState().setValue(SymbolBlocks.ACTIVE, false);
	}

}
