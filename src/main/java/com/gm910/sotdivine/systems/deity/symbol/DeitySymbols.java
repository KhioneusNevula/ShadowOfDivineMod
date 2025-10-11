package com.gm910.sotdivine.systems.deity.symbol;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AddReloadListenerEvent;

public class DeitySymbols extends SimpleJsonResourceReloadListener<IDeitySymbol> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private BiMap<ResourceLocation, IDeitySymbol> SYMBOLS = ImmutableBiMap.of();
	private Map<BannerPattern, IDeitySymbol> SYMBOL_BY_PATTERN = null;
	private static Optional<DeitySymbols> INSTANCE = Optional.empty();

	private static Codec<IDeitySymbol> CODEC;

	public static final Codec<IDeitySymbol> symbolCodec() {
		if (CODEC == null)
			CODEC = IDeitySymbol.createCodec();
		return CODEC;
	}

	private DeitySymbols(Provider prov) {
		super(prov, symbolCodec(), IDeitySymbol.REGISTRY_KEY);
	}

	@Override
	protected void apply(Map<ResourceLocation, IDeitySymbol> map, ResourceManager rm, ProfilerFiller filler) {
		this.SYMBOLS = HashBiMap.create(map);
		LOGGER.info("Loaded SYMBOLS: {}", SYMBOLS.values());
	}

	public static DeitySymbols instance() {
		return INSTANCE.get();
	}

	/**
	 * Get all SYMBOLS
	 * 
	 * @return
	 */
	public BiMap<ResourceLocation, IDeitySymbol> getDeitySymbolMap() {
		return Maps.unmodifiableBiMap(SYMBOLS);
	}

	/**
	 * Get a deity symbol using a banner pattern
	 * 
	 * @param pattern
	 * @return
	 */
	public Optional<IDeitySymbol> getFromPattern(BannerPattern pattern) {
		if (SYMBOL_BY_PATTERN == null)
			SYMBOL_BY_PATTERN = SYMBOLS.values()
					.stream().<Map.Entry<BannerPattern, IDeitySymbol>>map((p) -> Map.entry(p.bannerPattern().get(), p))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		return Optional.ofNullable(SYMBOL_BY_PATTERN.get(pattern));
	}

	/**
	 * Gets stream of symbols from a given item stack
	 * 
	 * @param level
	 * @param pos
	 * @return
	 */
	public Stream<IDeitySymbol> getFromItem(ItemStack stack) {
		return stack.getComponents().get(DataComponents.BANNER_PATTERNS).layers().stream()
				.map(BannerPatternLayers.Layer::pattern).map(Holder::get).map(this::getFromPattern)
				.flatMap(Optional::stream);
	}

	/**
	 * Gets a stream of deity symbols from a given block
	 * 
	 * @param level
	 * @param pos
	 * @return
	 */
	public Stream<IDeitySymbol> getFromBlock(ServerLevel level, BlockPos pos) {
		if (level.getExistingBlockEntity(pos) instanceof BlockEntity blockEn) {
			if (blockEn instanceof BannerBlockEntity banner) {
				return banner.getPatterns().layers().stream().map((x) -> x.pattern().get()).map(this::getFromPattern)
						.flatMap(Optional::stream);
			} // TODO other kinds of symbol blocks?
		}

		return Stream.empty();
	}

	/**
	 * So we can add this reloadable registry to the listener
	 * 
	 * @param event
	 */
	public static void eventAddListener(AddReloadListenerEvent event) {
		INSTANCE = Optional.of(new DeitySymbols(event.getRegistries()));
		event.addListener(INSTANCE.get());
	}

}
