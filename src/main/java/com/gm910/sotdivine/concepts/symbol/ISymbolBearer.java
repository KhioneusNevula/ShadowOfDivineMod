package com.gm910.sotdivine.concepts.symbol;

import java.util.Optional;
import java.util.stream.Stream;

import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * A {@link BlockEntity}, {@link Entity}, or {@link ItemStack} containing deity
 * symbols
 */
public interface ISymbolBearer {

	public static final ResourceLocation CAPABILITY_PATH = ModUtils.path("symbol_bearer");

	public static final Capability<ISymbolBearer> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
	});

	/**
	 * Tries casting the given thing to an {@link ISymbolBearer}; returns an empty
	 * optional if failed
	 * 
	 * @param be
	 * @return
	 */
	public static Optional<ISymbolBearer> tryCast(Object be) {
		return be instanceof ISymbolBearer sym ? Optional.of(sym) : Optional.empty();
	}

	/**
	 * Converts all symbols in this block to the given symbol; return false if
	 * nothing changed
	 * 
	 * @param toSymbol
	 * @return
	 */
	public boolean convertAllSymbols(IDeitySymbol toSymbol);

	/**
	 * Return all symbols on this
	 * 
	 * @return
	 */
	public Stream<? extends IDeitySymbol> getSymbols();

	/**
	 * Return whether this block has any deity symbols at all
	 * 
	 * @return
	 */
	public boolean hasAnySymbol();

	/**
	 * Return whether this block has the given deity symbol
	 * 
	 * @return
	 */
	public boolean hasSymbol(IDeitySymbol sym);
}
