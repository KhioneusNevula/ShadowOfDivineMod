package com.gm910.sotdivine.common.blocks;

import com.gm910.sotdivine.concepts.symbol.ISymbolBlock;
import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class SymbolBlocks {

	/** Whether this effigy is active */
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private SymbolBlocks() {
	}

	public static void onPlace(ISymbolBlock block, BlockState newState, Level lread, BlockPos pos, BlockState oldState,
			boolean p_60570_) {
		if (lread instanceof ServerLevel level) {
			ISanctuarySystem system = ISanctuarySystem.get(level);
			if (newState.getBlock() != oldState.getBlock()) {
				system.getSanctuaryAtPos(pos).ifPresent((sanct) -> {
					if (block.hasSymbol(sanct.symbol())) {
						sanct.addSymbolBlock(level, pos);
					}
				});
			}
		}
	}

	public static void onBlockStateChange(ISymbolBlock block, LevelReader lread, BlockPos pos, BlockState oldState,
			BlockState newState) {
		if (lread instanceof ServerLevel level) {
			ISanctuarySystem system = ISanctuarySystem.get(level);
			if (newState.getBlock() != oldState.getBlock()) {
				system.getSanctuaryAtPos(pos).ifPresent((sanct) -> {
					if (block.hasSymbol(sanct.symbol())) {
						sanct.removeSymbolBlock(level, pos);
					}
				});
			}
		}
	}
}
