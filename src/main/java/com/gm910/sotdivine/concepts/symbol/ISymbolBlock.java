package com.gm910.sotdivine.concepts.symbol;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface ISymbolBlock {

	/**
	 * Return a stream of symbols thsi block inherently contains
	 * 
	 * @return
	 */
	public default Stream<? extends IDeitySymbol> getSymbols() {
		return DeitySymbols.instance().getFromBlock((Block) this).stream();
	}

	/**
	 * Return whether this block contains the given symbol
	 * 
	 * @return
	 */
	public default boolean hasSymbol(IDeitySymbol symbol) {
		return DeitySymbols.instance().getFromBlock((Block) this).contains(symbol);
	}

	/**
	 * Return a new block state with the symbols on it converted
	 * 
	 * @param to
	 * @return
	 */
	public BlockState convertSymbols(IDeitySymbol to);

}
