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
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Capability provider of symbol bearer for banners
 */
public class BannerSymbolBearer implements ICapabilityProvider, ISymbolBearer {

	private BannerBlockEntity banner;

	private final LazyOptional<ISymbolBearer> cached = LazyOptional.of(() -> this);

	public BannerSymbolBearer(BannerBlockEntity componentGetter) {
		this.banner = componentGetter;
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
		List<BannerPatternLayers.Layer> layers = new ArrayList<>(banner.getPatterns().layers());
		for (int i = 0; i < layers.size(); i++) {
			BannerPatternLayers.Layer layer = layers.get(i);
			if (DeitySymbols.instance().getFromPattern(layer.pattern()).orElse(null) instanceof IDeitySymbol symbol
					&& !symbol.equals(toSymbol)) {
				layers.set(i, new BannerPatternLayers.Layer(toSymbol.bannerPattern(), layer.color()));
			}
		}
		if (!layers.equals(banner.getPatterns().layers())) {
			banner.applyComponents(DataComponentMap.builder()
					.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layers)).build(),
					DataComponentPatch.EMPTY);
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
		return banner.getPatterns().layers().stream();
	}

}
