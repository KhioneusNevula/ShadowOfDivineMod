package com.gm910.sotdivine.concepts.symbol.impl;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.concepts.symbol.ISymbolBearer;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Capability provider of symbol bearer for entities with shields
 */
public class LivingEntitySymbolWearer implements ICapabilityProvider, ISymbolWearer {

	private LivingEntity entity;

	private final LazyOptional<ISymbolWearer> cached = LazyOptional.of(() -> this);

	public LivingEntitySymbolWearer(LivingEntity componentGetter) {
		this.entity = componentGetter;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ISymbolWearer.CAPABILITY) {
			return cached.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public boolean convertAllSymbols(IDeitySymbol toSymbol) {
		boolean[] changed = { false };
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			ItemStack item = entity.getItemBySlot(slot);
			if (item.getCount() > 0) {
				item.getCapability(ISymbolBearer.CAPABILITY).ifPresent((sym) -> {
					sym.convertAllSymbols(toSymbol);
					changed[0] = true;
				});
			}
		}
		return changed[0];
	}

	@Override
	public Stream<? extends IDeitySymbol> getSymbolsInSlot(EquipmentSlot slot) {
		return Stream.of(entity.getItemBySlot(slot)).filter((s) -> !s.isEmpty())
				.flatMap((st) -> Optional.ofNullable(st.get(DataComponents.BANNER_PATTERNS)).stream())
				.flatMap((s) -> s.layers().stream()).map((x) -> x.pattern())
				.map(DeitySymbols.instance()::getFromPattern).flatMap(Optional::stream);
	}

	@Override
	public Stream<? extends IDeitySymbol> getSymbols() {
		return layerStream().map((x) -> x.pattern()).map(DeitySymbols.instance()::getFromPattern)
				.flatMap(Optional::stream);
	}

	@Override
	public boolean hasAnySymbol() {
		return layerStream().anyMatch((s) -> DeitySymbols.instance().getFromPattern(s.pattern()).isPresent());
	}

	@Override
	public boolean hasSymbol(IDeitySymbol sym) {
		return layerStream().flatMap((s) -> DeitySymbols.instance().getFromPattern(s.pattern()).stream())
				.anyMatch((ds) -> ds.equals(sym));
	}

	private Stream<BannerPatternLayers.Layer> layerStream() {
		return Arrays.stream(EquipmentSlot.values()).map((s) -> entity.getItemBySlot(s))
				.flatMap((st) -> Optional.ofNullable(st.get(DataComponents.BANNER_PATTERNS)).stream())
				.flatMap((s) -> s.layers().stream());
	}

}
