package com.gm910.sotdivine.magic.ritual.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gm910.sotdivine.concepts.genres.IGenreType;
import com.gm910.sotdivine.concepts.genres.creator.BlockCreator;
import com.gm910.sotdivine.concepts.genres.creator.IGenreCreator;
import com.gm910.sotdivine.concepts.genres.creator.IGenrePlacer;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public non-sealed class RitualPattern implements IRitualPattern {

	Map<Vec3i, String> blockMap;
	Multimap<String, IGenreType<? extends IPlaceableGenreProvider<?, ?>>> genres;
	private Vec3i utility_minPos;
	private Vec3i utility_maxPos;
	private Multimap<String, Vec3i> utility_symbols;
	private int utility_numBlocks;

	static Codec<IRitualPattern> EITHER_CODEC = null;

	public static final Codec<IRitualPattern> READER_CODEC = new PatternCodec().xmap((r) -> r,
			(r) -> (RitualPattern) r);

	public RitualPattern(Map<Vec3i, String> bMap,
			Multimap<String, IGenreType<? extends IPlaceableGenreProvider<?, ?>>> genres) {
		if (bMap.isEmpty()) {
			throw new IllegalArgumentException("Cannot make empty patterns");
		}
		this.blockMap = new TreeMap<>(bMap);
		this.blockMap.values().removeIf((v) -> v.isBlank());

		this.genres = ImmutableMultimap.copyOf(genres);
		this.utility_symbols = blockMap.entrySet().stream()
				.filter((x) -> !x.getValue().equals(EMPTY_SYMBOL) && !x.getValue().isBlank())
				.collect(Multimaps.toMultimap((s) -> s.getValue(), (s) -> s.getKey(),
						MultimapBuilder.hashKeys().hashSetValues()::build));

		if (!blockMap.values().stream().allMatch((x) -> x.length() == 1))
			throw new IllegalArgumentException("All strings should be length 1");

		Map<String, IGenreType<? extends IPlaceableGenreProvider<?, ?>>> orphanedGenres = genres.entries().stream()
				.filter((s) -> !utility_symbols.containsKey(s.getKey()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		if (!orphanedGenres.isEmpty()) {
			throw new IllegalArgumentException("Symbols " + orphanedGenres + " are specified but not in pattern... ");
		}

		this.utility_numBlocks = (int) blockMap.values().stream().filter((x) -> !x.equals(EMPTY_SYMBOL) && !x.isBlank())
				.count();

		this.utility_minPos = new Vec3i(bMap.keySet().stream().mapToInt(Vec3i::getX).min().getAsInt(),
				bMap.keySet().stream().mapToInt(Vec3i::getY).min().getAsInt(),
				bMap.keySet().stream().mapToInt(Vec3i::getZ).min().getAsInt());

		this.utility_maxPos = new Vec3i(bMap.keySet().stream().mapToInt(Vec3i::getX).max().getAsInt(),
				bMap.keySet().stream().mapToInt(Vec3i::getY).max().getAsInt(),
				bMap.keySet().stream().mapToInt(Vec3i::getZ).max().getAsInt());

		if (List.of(utility_minPos.getX(), utility_minPos.getY(), utility_minPos.getZ()).stream()
				.anyMatch((x) -> x < -32)) {
			throw new IllegalArgumentException("Minimum point too far from center: " + utility_minPos);
		}

		if (List.of(utility_maxPos.getX(), utility_maxPos.getY(), utility_maxPos.getZ()).stream()
				.anyMatch((x) -> x > 32)) {
			throw new IllegalArgumentException("Maximum point too far from center: " + utility_maxPos);
		}
	}

	@Override
	public boolean matches(ServerLevel level, BlockPos focus, Map<String, IPlaceableGenreProvider<?, ?>> blockPreds) {
		for (int y = utility_minPos.getY(); y <= utility_maxPos.getY(); y++) {
			for (int x = utility_minPos.getX(); x <= utility_maxPos.getX(); x++) {
				for (int z = utility_minPos.getZ(); z <= utility_minPos.getZ(); z++) {
					Vec3i relativePos = new Vec3i(x, y, z);
					BlockPos absolutePos = focus.offset(relativePos);
					String symbol = this.blockMap.get(relativePos);
					if (symbol == null || symbol.equals(EMPTY_SYMBOL))
						continue;

					IPlaceableGenreProvider<?, ?> matcher = blockPreds.get(symbol);
					if (matcher != null) {
						if (!matcher.matchesPos(level, absolutePos)) {
							return false;
						}
					} /*else {
						throw new IllegalArgumentException("Cannot check pattern " + this + " because a symbol \""
								+ symbol + "\" is not bound within the map: " + blockPreds.keySet());
						}*/
				}
			}
		}
		return true;
	}

	@Override
	public Stream<? extends Vec3i> getAllBlockPositions() {
		return this.blockMap.keySet().stream();
	}

	@Override
	public <T extends Entity> Stream<T> getEntitiesInPattern(ServerLevel world, BlockPos focus,
			EntityTypeTest<Entity, T> test, Predicate<T> pred) {
		return this.blockMap.keySet().stream().flatMap((relativePos) -> {
			BlockPos absolutePos = focus.offset(relativePos);
			return world.getEntities(test, new AABB(absolutePos), pred).stream();
		});
		/**
		 * List<T> ens = new ArrayList<>(); for (int y = utility_minPos.getY(); y <=
		 * utility_maxPos.getY(); y++) { for (int x = utility_minPos.getX(); x <=
		 * utility_maxPos.getX(); x++) { for (int z = utility_minPos.getZ(); z <=
		 * utility_minPos.getZ(); z++) { Vec3i relativePos = new Vec3i(x, y, z);
		 * BlockPos absolutePos = focus.offset(relativePos); String symbol =
		 * this.blockMap.get(relativePos); if (symbol == null) continue;
		 * ens.addAll(world.getEntities(test, new AABB(absolutePos), pred)); } } }
		 * return ens;
		 */
	}

	@Override
	public void generatePossible(ServerLevel world, BlockPos focus,
			Map<String, IPlaceableGenreProvider<?, ?>> blockPreds) {
		for (int attempt = 0; attempt <= 10; attempt++) { // make ten attempts?
			Map<String, IGenrePlacer> placers = new HashMap<>(
					Maps.transformValues(blockPreds, (v) -> v.generateRandom(world, Optional.empty())));

			for (int y = utility_maxPos.getY(); y >= utility_minPos.getY(); y--) {
				for (int x = utility_minPos.getX(); x <= utility_maxPos.getX(); x++) {
					for (int z = utility_minPos.getZ(); z <= utility_maxPos.getZ(); z++) {
						Vec3i relativePos = new Vec3i(x, y, z);
						BlockPos absolutePos = focus.offset(relativePos);
						String symbol = this.blockMap.get(relativePos);

						if (symbol == null || symbol.equals(EMPTY_SYMBOL))
							continue;
						if (placers.get(symbol) instanceof IGenreCreator pred) {
							pred.tryPlace(world, absolutePos);
						} else {
							var blockReg = world.registryAccess().lookupOrThrow(Registries.BLOCK);
							var blockPicked = blockReg.getRandom(world.random).get().get();
							var blockStateList = new ArrayList<>(blockPicked.getStateDefinition().getPossibleStates());
							Collections.shuffle(blockStateList);
							var blockState = blockStateList.getFirst();
							if (blockState.hasProperty(BlockStateProperties.WATERLOGGED))
								blockState = blockState.setValue(BlockStateProperties.WATERLOGGED, false);
							IGenrePlacer forSym = new BlockCreator(blockState, (f) -> true,
									() -> blockPicked.asItem() != null ? new ItemStack(blockPicked.asItem())
											: ItemStack.EMPTY);
							placers.put(symbol, forSym);
							world.setBlock(absolutePos, blockState, 2);
							/*if (relativePos.equals(Vec3i.ZERO)) {
								world.setBlockAndUpdate(absolutePos, Blocks.RED_TERRACOTTA.defaultBlockState());
							} else {
								world.setBlockAndUpdate(absolutePos, Blocks.BLACK_TERRACOTTA.defaultBlockState());
							}*/
						}
					}
				}
			}
			if (this.matches(world, focus, blockPreds)) {
				break; // we done
			}
		}
	}

	@Override
	public IRitualPattern rotated180Copy() {
		return new RitualPattern(
				this.blockMap.entrySet().stream().map((x) -> Map.entry(x.getKey().multiply(-1), x.getValue()))
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue)),
				this.genres);
	}

	@Override
	public IRitualPattern rotated90Copy(boolean counterClockWise) {
		return new RitualPattern(this.blockMap.entrySet().stream()
				.map((x) -> Map.entry(new Vec3i((counterClockWise ? 1 : -1) * x.getKey().getZ(), x.getKey().getY(),
						x.getKey().getX()), x.getValue()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue)), this.genres);
	}

	@Override
	public IRitualPattern flippedCopy(FlipAxis... axias) {
		if (axias.length == 1 && axias[0] == FlipAxis.NONE) {
			return this;
		}
		if (axias.length == 0) {
			return this;
		}
		Set<Axis> axes = Arrays.stream(axias).flatMap((a) -> Optional.ofNullable(a.axis).stream())
				.collect(Collectors.toSet());
		return new RitualPattern(this.blockMap.entrySet().stream().map((x) -> {
			return Map.entry(new Vec3i(x.getKey().getX() * (axes.contains(Axis.X) ? -1 : 1),
					x.getKey().getY() * (axes.contains(Axis.Y) ? -1 : 1),
					x.getKey().getZ() * (axes.contains(Axis.Z) ? -1 : 1)), x.getValue());
		}).collect(Collectors.toMap(Entry::getKey, Entry::getValue)), this.genres);
	}

	@Override
	public Vec3i focus() {
		return Vec3i.ZERO;
	}

	@Override
	public String focusSymbol() {
		return this.blockMap.get(focus());
	}

	@Override
	public Vec3i minPos() {
		return utility_minPos;
	}

	@Override
	public Vec3i maxPos() {
		return utility_maxPos;
	}

	@Override
	public Collection<String> symbols() {
		return Collections.unmodifiableSet(utility_symbols.keySet());
	}

	@Override
	public int blockCount() {
		return utility_numBlocks;
	}

	@Override
	public String getSymbolAt(Vec3i relativePos) {
		return blockMap.get(relativePos);
	}

	@Override
	public Collection<Vec3i> getPositions(String symbol) {
		return Collections.unmodifiableCollection(utility_symbols.get(symbol));
	}

	@Override
	public Collection<IGenreType<? extends IPlaceableGenreProvider<?, ?>>> permittedGenres(String symbol) {
		return this.genres.get(symbol);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof RitualPattern pat) {
			return this.blockMap.equals(pat.blockMap) && this.genres.equals(pat.genres);
		}
		return false;
	}

	public String drawLayer(int y) {
		if (y < utility_minPos.getY() || y > utility_maxPos.getY())
			throw new IllegalArgumentException(
					"Only available between " + utility_minPos.getY() + " and " + utility_maxPos.getY() + ", not " + y);
		StringBuilder builder = new StringBuilder();
		builder.append("+");
		for (int x = utility_minPos.getX(); x <= utility_maxPos.getX(); x++) {
			builder.append("-");
		}
		builder.append("+\n");
		for (int x = utility_minPos.getX(); x <= utility_maxPos.getX(); x++) {
			builder.append("\n");
			builder.append("|");
			for (int z = utility_minPos.getZ(); z <= utility_maxPos.getZ(); z++) {
				builder.append(blockMap.getOrDefault(new Vec3i(x, y, z), " "));
			}
			builder.append("|");
		}
		builder.append("\n+");
		for (int x = utility_minPos.getX(); x <= utility_maxPos.getX(); x++) {
			builder.append("-");
		}
		builder.append("+");
		return builder.toString();
	}

	@Override
	public String toString() {

		return "#[(" + this.utility_minPos.toShortString() + "),(" + this.utility_maxPos.toShortString() + ")](count="
				+ this.utility_numBlocks + ",utility_symbols=" + this.utility_symbols.keySet() + ")";
	}

	@Override
	public int hashCode() {
		return this.blockMap.hashCode();
	}

}
