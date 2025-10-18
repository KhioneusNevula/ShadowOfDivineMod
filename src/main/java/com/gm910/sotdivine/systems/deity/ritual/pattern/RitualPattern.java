package com.gm910.sotdivine.systems.deity.ritual.pattern;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.gm910.sotdivine.systems.deity.sphere.genres.creator.IGenreCreator;
import com.gm910.sotdivine.systems.deity.sphere.genres.creator.IGenrePlacer;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public non-sealed class RitualPattern implements IRitualPattern {

	HashMap<Vec3i, String> blockMap;
	private Vec3i minPos;
	private Vec3i maxPos;
	private Set<String> symbols;

	static Codec<IRitualPattern> EITHER_CODEC = null;

	public static final Codec<IRitualPattern> READER_CODEC = new PatternCodec().xmap((r) -> r,
			(r) -> (RitualPattern) r);

	public RitualPattern(Map<Vec3i, String> bMap) {
		if (bMap.isEmpty()) {
			throw new IllegalArgumentException("Cannot make empty patterns");
		}
		this.blockMap = new HashMap<>(bMap);
		this.blockMap.values().removeIf((v) -> v.isBlank());

		if (!blockMap.values().stream().allMatch((x) -> x.length() == 1))
			throw new IllegalArgumentException("All strings should be length 1");

		this.symbols = blockMap.values().stream().collect(Collectors.toSet());

		this.minPos = new Vec3i(bMap.keySet().stream().mapToInt(Vec3i::getX).min().getAsInt(),
				bMap.keySet().stream().mapToInt(Vec3i::getY).min().getAsInt(),
				bMap.keySet().stream().mapToInt(Vec3i::getZ).min().getAsInt());

		this.maxPos = new Vec3i(bMap.keySet().stream().mapToInt(Vec3i::getX).max().getAsInt(),
				bMap.keySet().stream().mapToInt(Vec3i::getY).max().getAsInt(),
				bMap.keySet().stream().mapToInt(Vec3i::getZ).max().getAsInt());

		if (List.of(minPos.getX(), minPos.getY(), minPos.getZ()).stream().anyMatch((x) -> x < -32)) {
			throw new IllegalArgumentException("Minimum point too far from center: " + minPos);
		}

		if (List.of(maxPos.getX(), maxPos.getY(), maxPos.getZ()).stream().anyMatch((x) -> x > 32)) {
			throw new IllegalArgumentException("Maximum point too far from center: " + maxPos);
		}
	}

	@Override
	public boolean matches(ServerLevel level, BlockPos focus, Map<String, IPlaceableGenreProvider<?, ?>> blockPreds) {
		for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
			for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
				for (int z = minPos.getZ(); z <= minPos.getZ(); z++) {
					Vec3i relativePos = new Vec3i(x, y, z);
					BlockPos absolutePos = focus.offset(relativePos);
					String symbol = this.blockMap.get(relativePos);
					if (symbol == null)
						continue;

					IPlaceableGenreProvider<?, ?> matcher = blockPreds.get(symbol);
					if (matcher != null) {
						if (!matcher.matchesPos(level, absolutePos)) {
							return false;
						}

					} else {
						throw new IllegalArgumentException("Cannot match " + this + " because a symbol \"" + symbol
								+ "\" is not bound within the map: " + blockPreds.keySet());
					}
				}
			}
		}
		return true;
	}

	@Override
	public void generatePossible(ServerLevel world, BlockPos focus,
			Map<String, IPlaceableGenreProvider<?, ?>> blockPreds) {
		Map<String, IGenrePlacer> placers = new HashMap<>(
				Maps.transformValues(blockPreds, (v) -> v.generateRandom(world, Optional.empty())));

		for (int y = maxPos.getY(); y >= minPos.getY(); y--) {
			for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
				for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
					Vec3i relativePos = new Vec3i(x, y, z);
					BlockPos absolutePos = focus.offset(relativePos);
					String symbol = this.blockMap.get(relativePos);

					if (symbol == null)
						continue;
					if (placers.get(symbol) instanceof IGenreCreator pred) {
						pred.tryPlace(world, absolutePos);
					} else {
						if (relativePos.equals(Vec3i.ZERO)) {
							world.setBlockAndUpdate(absolutePos, Blocks.RED_TERRACOTTA.defaultBlockState());
						} else {
							world.setBlockAndUpdate(absolutePos, Blocks.BLACK_TERRACOTTA.defaultBlockState());
						}
					}
				}
			}
		}
	}

	@Override
	public IRitualPattern rotated180Copy() {
		return new RitualPattern(
				this.blockMap.entrySet().stream().map((x) -> Map.entry(x.getKey().multiply(-1), x.getValue()))
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}

	@Override
	public IRitualPattern rotated90Copy(boolean counterClockWise) {
		return new RitualPattern(this.blockMap
				.entrySet().stream().map((x) -> Map.entry(new Vec3i((counterClockWise ? 1 : -1) * x.getKey().getZ(),
						x.getKey().getY(), x.getKey().getX()), x.getValue()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
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
		}).collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}

	@Override
	public Vec3i focus() {
		return Vec3i.ZERO;
	}

	@Override
	public Vec3i minPos() {
		return minPos;
	}

	@Override
	public Vec3i maxPos() {
		return maxPos;
	}

	@Override
	public Collection<String> symbols() {
		return Collections.unmodifiableSet(symbols);
	}

	@Override
	public String getBlock(Vec3i relativePos) {
		return blockMap.get(relativePos);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof RitualPattern pat) {
			return this.blockMap.equals(pat.blockMap);
		}
		return false;
	}

	public String drawLayer(int y) {
		if (y < minPos.getY() || y > maxPos.getY())
			throw new IllegalArgumentException(
					"Only available between " + minPos.getY() + " and " + maxPos.getY() + ", not " + y);
		StringBuilder builder = new StringBuilder();
		builder.append("+");
		for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
			builder.append("-");
		}
		builder.append("+\n");
		for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
			builder.append("\n");
			builder.append("|");
			for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
				builder.append(blockMap.getOrDefault(new Vec3i(x, y, z), " "));
			}
			builder.append("|");
		}
		builder.append("\n+");
		for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
			builder.append("-");
		}
		builder.append("+");
		return builder.toString();
	}

	@Override
	public String toString() {

		return "RitualPattern(min=" + this.minPos + ",max=" + this.maxPos + ",itemCount=" + this.blockMap.size()
				+ ",symbols=" + this.symbols + ")";
	}

	@Override
	public int hashCode() {
		return this.blockMap.hashCode();
	}

}
