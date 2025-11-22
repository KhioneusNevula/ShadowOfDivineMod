package com.gm910.sotdivine.concepts.symbol;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.concepts.symbol.impl.ISymbolWearer;
import com.gm910.sotdivine.util.FieldUtils;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.AddReloadListenerEvent;

public class DeitySymbols extends SimpleJsonResourceReloadListener<IDeitySymbol> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private BiMap<ResourceLocation, IDeitySymbol> SYMBOLS = ImmutableBiMap.of();
	private Map<BannerPattern, IDeitySymbol> SYMBOL_BY_PATTERN = null;
	private Multimap<Block, IDeitySymbol> SYMBOL_BY_BLOCK = null;
	private static Optional<DeitySymbols> INSTANCE = Optional.empty();
	public static final ResourceLocation DIVINE_TAG_PATH = ModUtils.path("divine_patterns");
	private static Codec<IDeitySymbol> CODEC;

	/**
	 * Codec to retrieve symbols by resource location
	 */
	public static final Codec<IDeitySymbol> BY_NAME_CODEC = ResourceLocation.CODEC.flatXmap((s) -> {
		var symbol = DeitySymbols.instance().SYMBOLS.get(s);
		if (symbol == null) {
			return DataResult.error(() -> "No deity symbol for " + s);
		}
		return DataResult.success(symbol);
	}, (s) -> {
		var rl = DeitySymbols.instance().SYMBOLS.inverse().get(s);
		if (rl == null) {
			return DataResult.error(() -> "Unregistered symbol: " + s.toString());
		}
		return DataResult.success(rl);
	});

	public static final Codec<IDeitySymbol> symbolCodec() {
		if (CODEC == null)
			CODEC = IDeitySymbol.createCodec();
		return CODEC;
	}

	private DeitySymbols(Provider prov) {
		super(prov, symbolCodec(), ModRegistries.DEITY_SYMBOLS);
	}

	@Override
	protected Map<ResourceLocation, IDeitySymbol> prepare(ResourceManager mana, ProfilerFiller p_10772_) {

		return super.prepare(mana, p_10772_);
	}

	@Override
	protected void apply(Map<ResourceLocation, IDeitySymbol> map, ResourceManager rm, ProfilerFiller filler) {
		this.SYMBOLS = HashBiMap.create(map);
		LOGGER.info("Loaded symbols: {}", SYMBOLS.values());
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
	 * Get a deity symbol using a banner patterns
	 * 
	 * @param patterns
	 * @return
	 */
	public Optional<IDeitySymbol> getFromPattern(Holder<BannerPattern> pattern) {
		return pattern.isBound() ? this.getFromPattern(pattern.value())
				: Optional.ofNullable(this.SYMBOLS.get(pattern.unwrapKey().get().location()));
	}

	/**
	 * Get a deity symbol using a banner patterns
	 * 
	 * @param patterns
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
	 * Get a deity symbol using an effigy block
	 * 
	 * @param patterns
	 * @return
	 */
	public Collection<IDeitySymbol> getFromBlock(Block block) {
		if (SYMBOL_BY_BLOCK == null)
			SYMBOL_BY_BLOCK = SYMBOLS.values()
					.stream().<Map.Entry<Block, IDeitySymbol>>flatMap((p) -> p.effigies().stream()
							.flatMap((s) -> s.stream()).map(Holder::get).map((x) -> Map.entry(x, p)))
					.collect(Multimaps.toMultimap(Entry::getKey, Entry::getValue,
							MultimapBuilder.hashKeys().hashSetValues()::build));
		return Collections.unmodifiableCollection(SYMBOL_BY_BLOCK.get(block));
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
	 * Get a deity symbol using a block
	 * 
	 * @param patterns
	 * @return
	 */
	public Collection<IDeitySymbol> getFromBlock(Holder<Block> pattern) {
		return pattern.isBound() ? this.getFromBlock(pattern.value())
				: Optional.ofNullable(this.SYMBOLS.get(pattern.unwrapKey().get().location()))
						.map((s) -> Collections.singleton(s)).orElse(Set.of());
	}

	/**
	 * Gets a stream of deity symbols from a given block
	 * 
	 * @param level
	 * @param pos
	 * @return
	 */
	public Stream<IDeitySymbol> getFromPosition(ServerLevel level, BlockPos pos) {
		Block block = level.getBlockState(pos).getBlock();
		if (!this.getFromBlock(block).isEmpty()) {
			return this.getFromBlock(block).stream();
		} else if (block instanceof ISymbolBlock symblock) {
			return symblock.getSymbols().map((s) -> s);
		}
		if (level.getBlockEntity(pos) instanceof BlockEntity blockEn) {

			var cap = blockEn.getCapability(ISymbolBearer.CAPABILITY);

			if (cap.lazyMap((s) -> s.getSymbols().map((t) -> (IDeitySymbol) t))
					.orElse(null) instanceof Stream<IDeitySymbol> stream) {
				return stream;
			}
		}
		return Stream.empty();
	}

	/**
	 * Gets a stream of deity symbols from a given entity, worn or inherent
	 * 
	 * @param level
	 * @param pos
	 * @return
	 */
	public Stream<IDeitySymbol> getFromEntity(Entity entity) {
		return Streams.concat(
				entity.getCapability(ISymbolBearer.CAPABILITY).resolve().stream().flatMap((s) -> s.getSymbols()),
				entity.getCapability(ISymbolWearer.CAPABILITY).resolve().stream().flatMap((s) -> s.getSymbols()));
	}

	/**
	 * Convert all symbols at the rawPosition to the given symbol
	 * 
	 * @param level
	 * @param pos
	 * @param sym
	 * @return
	 */
	public boolean convertSymbolsAtPosition(ServerLevel level, BlockPos pos, IDeitySymbol sym) {
		BlockState state = level.getBlockState(pos);
		boolean success = false;
		if (state instanceof ISymbolBlock symblock) {
			BlockState newState = symblock.convertSymbols(sym);
			if (newState != null) {
				success = level.setBlock(pos, newState, 3);
			}
		}
		if (level.getBlockEntity(pos) instanceof ISymbolBearer symblock) {
			success = symblock.convertAllSymbols(sym) || success;
		}

		return success;

	}

	/**
	 * Convert all symbols in the entity to the given symbol
	 * 
	 * @param level
	 * @param pos
	 * @return
	 */
	public boolean convertSymbolsOfEntity(Entity entity, IDeitySymbol sym) {
		boolean[] worked = { false };
		entity.getCapability(ISymbolBearer.CAPABILITY).ifPresent((s) -> s.convertAllSymbols(sym));

		entity.getCapability(ISymbolWearer.CAPABILITY).ifPresent((s) -> s.convertAllSymbols(sym));
		return worked[0];
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
