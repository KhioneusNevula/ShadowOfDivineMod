package com.gm910.sotdivine.concepts.symbol.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.concepts.symbol.ISymbolBearer;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Capability provider of symbol bearer for entities with shields
 */
public class ItemStackSymbolBearer implements ICapabilityProvider, ISymbolBearer {

	private ItemStack stack;

	private final LazyOptional<ISymbolBearer> cached = LazyOptional.of(() -> this);

	public ItemStackSymbolBearer(ItemStack componentGetter) {
		this.stack = componentGetter;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ISymbolBearer.CAPABILITY) {
			return cached.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public boolean convertAllSymbols(IDeitySymbol toSymbol) {

		BannerPatternLayers patterns = stack.getComponents().get(DataComponents.BANNER_PATTERNS);
		if (patterns == null)
			return false;
		List<BannerPatternLayers.Layer> layers = new ArrayList<>(patterns.layers());
		for (int i = 0; i < layers.size(); i++) {
			BannerPatternLayers.Layer layer = layers.get(i);
			if (DeitySymbols.instance().getFromPattern(layer.pattern()).orElse(null) instanceof IDeitySymbol symbol
					&& !symbol.equals(toSymbol)) {
				layers.set(i, new BannerPatternLayers.Layer(toSymbol.bannerPattern(), layer.color()));
			}
		}
		BannerPatternLayers newLayers = new BannerPatternLayers(layers);
		if (!newLayers.equals(patterns)) {
			DataComponentMap data = DataComponentMap.builder().set(DataComponents.BANNER_PATTERNS, newLayers).build();
			stack.applyComponents(data);
			return true;
		}

		return false;
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
		return Optional.ofNullable(stack.get(DataComponents.BANNER_PATTERNS)).stream()
				.flatMap((s) -> s.layers().stream());
	}

}
