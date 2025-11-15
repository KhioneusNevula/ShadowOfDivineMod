package com.gm910.sotdivine.concepts.symbol.impl;

import java.util.stream.Stream;

import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.concepts.symbol.ISymbolBearer;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public interface ISymbolWearer extends ISymbolBearer {

	public static final Capability<ISymbolWearer> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
	});
	public static final ResourceLocation CAPABILITY_PATH = ModUtils.path("symbol_wearer");

	/**
	 * Return all symbols in the given equipment slot
	 * 
	 * @param slot
	 * @return
	 */
	public Stream<? extends IDeitySymbol> getSymbolsInSlot(EquipmentSlot slot);
}
