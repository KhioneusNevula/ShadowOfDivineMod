package com.gm910.sotdivine.magic.ritual.pattern;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.google.common.base.Functions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * A set of ritual patterns rotated/flipped in different directions
 */
public class RitualPatternSet {
	private static final Function<IRitualPattern, IRitualPattern> TURN_NORTH = Functions.identity();
	private static final Function<IRitualPattern, IRitualPattern> TURN_EAST = (rp) -> rp.rotated90Copy(false);
	private static final Function<IRitualPattern, IRitualPattern> TURN_WEST = (rp) -> rp.rotated90Copy(true);
	private static final Function<IRitualPattern, IRitualPattern> TURN_SOUTH = IRitualPattern::rotated180Copy;
	private static final Map<Direction, Function<IRitualPattern, IRitualPattern>> DIRECTION_TURNS = Map.of(
			Direction.NORTH, TURN_NORTH, Direction.EAST, TURN_EAST, Direction.WEST, TURN_WEST, Direction.SOUTH,
			TURN_SOUTH);
	private IRitualPattern pattern;
	private Table<FlipAxis, Direction, IRitualPattern> rotationsAndFlips;

	/**
	 * Codec for collection of pairs of axes and directions; "axis:direction" or
	 * perhaps just "direction"
	 */
	private static final Codec<Pair<FlipAxis, Direction>> AXIS_DIR_CODEC = Codec.STRING.comapFlatMap((string) -> {
		String[] split = string.split("[:.]");
		if (split.length <= 0 || split.length > 2)
			return DataResult.error(() -> "Invalid expression " + string);
		FlipAxis flipAxis = split.length == 1 ? FlipAxis.NONE : FlipAxis.valueOf(split[0].toUpperCase());
		Direction direction = split.length == 1 ? Direction.valueOf(split[0].toUpperCase())
				: Direction.valueOf(split[1].toUpperCase());
		if (flipAxis == null || direction == null) {
			return DataResult.error(() -> "Invalid flip axis or direction : " + flipAxis + "." + direction);
		}
		if (!Direction.Plane.HORIZONTAL.test(direction)) {
			return DataResult.error(() -> "Only permit horizontal directions");
		}
		return DataResult.success(Pair.of(flipAxis, direction));
	}, (pair) -> (pair.getFirst() == FlipAxis.NONE ? "" : pair.getFirst().name().toLowerCase() + ":")
			+ pair.getSecond().name().toLowerCase());

	/**
	 * Multimap interpreter; turns a list of {@link #AXIS_DIR_CODEC} into a multimap
	 */
	private static final Codec<Multimap<FlipAxis, Direction>> AXIS_DIR_MULTI_CODEC = Codec.list(AXIS_DIR_CODEC)
			.<Multimap<FlipAxis, Direction>>xmap(
					(list) -> ImmutableMultimap.copyOf((Iterable<Entry<FlipAxis, Direction>>) () -> list.stream()
							.<Entry<FlipAxis, Direction>>map((p) -> Map.entry(p.getFirst(), p.getSecond())).iterator()),
					(multi) -> multi.entries().stream().map((p) -> Pair.of(p.getKey(), p.getValue())).toList());

	private static final BiMap<String, Multimap<FlipAxis, Direction>> KEYWORD_MAP = ImmutableBiMap.of(
			/** Any and all rotations and flips */
			"all",
			ImmutableMultimap.copyOf(Arrays.stream(FlipAxis.values())
					.flatMap((f) -> Direction.Plane.HORIZONTAL.stream().map((x) -> Map.entry(f, x)))
					.collect(Collectors.toSet())),

			/** Only cardinal direction rotations */
			"cardinals",
			ImmutableMultimap.copyOf(Direction.Plane.HORIZONTAL.stream().map((x) -> Map.entry(FlipAxis.NONE, x))
					.collect(Collectors.toSet())),

			/** Cardinal direction rotations and horizontal flips */
			"horizontals",
			ImmutableMultimap
					.copyOf(Direction.Plane.HORIZONTAL
							.stream().flatMap((x) -> Set.of(Map.entry(FlipAxis.NONE, x),
									Map.entry(FlipAxis.NORTH_SOUTH, x), Map.entry(FlipAxis.WEST_EAST, x)).stream())
							.collect(Collectors.toSet())));

	/**
	 * Keywords: {@link #KEYWORD_MAP}
	 */
	private static final Codec<Multimap<FlipAxis, Direction>> KEYWORDS_CODEC = Codec.STRING.flatXmap((kw) -> {
		if (KEYWORD_MAP.containsKey(kw)) {
			return DataResult.success(KEYWORD_MAP.get(kw));
		}
		return DataResult.error(() -> "Unrecognized keyword: " + kw);
	}, (multi) -> {
		String val = KEYWORD_MAP.inverse().get(multi);
		if (val == null) {
			return DataResult.error(() -> "Given multimap does not fit any keyword");
		}
		return DataResult.success(val);
	});

	private static final Multimap<FlipAxis, Direction> DEFAULT_DIRECTIONS = ImmutableMultimap.of(FlipAxis.NONE,
			Direction.NORTH);

	/**
	 * Codec which interprets as either a keyword or a full definition
	 */
	private static final Codec<Multimap<FlipAxis, Direction>> EITHER_KW_OR_FLIPDIR_CODEC = Codec
			.either(KEYWORDS_CODEC, AXIS_DIR_MULTI_CODEC).xmap((x) -> Either.unwrap(x), (multi) -> {
				String kw = KEYWORD_MAP.inverse().get(multi);
				if (kw != null) {
					return Either.left(multi);
				}
				return Either.right(multi);
			});

	private static final Function<RitualPatternSet, Optional<Multimap<FlipAxis, Direction>>> MAPGETTER = (
			rp) -> Optional.of(ImmutableMultimap.copyOf(
					rp.getAllOrientations().stream().map((e) -> Map.entry(e.getFirst(), e.getSecond())).toList()));

	private static Codec<RitualPatternSet> CODEC = null;

	/*
	 * Codec permits formats such as:
	 * {
	 * 	"patterns":"sotdivine:circle",
	 * 	"directions":"cardinals"
	 * },
	 * {
	 * 	"directions":"horizontals",
	 * 	"patterns":{
	 * 		"focus":"X",
	 * 		"template":[
	 * 			"  D  ",
	 * 			" D D ",
	 * 			"D X D",
	 * 			" D D ",
	 * 			"  D  "
	 * 		]
	 * 	}
	 * },
	 * {
	 * 	"patterns":"sotdivine:mojang",
	 * 	"directions":["north","south","east","west"]
	 * }
	 * 
	 */
	public static Codec<RitualPatternSet> codec() {
		if (CODEC == null)
			CODEC = RecordCodecBuilder.create(instance -> instance
					.group(IRitualPattern.eitherCodec().fieldOf("base_pattern")
							.forGetter(RitualPatternSet::getBasePattern),
							EITHER_KW_OR_FLIPDIR_CODEC.optionalFieldOf("directions").forGetter(MAPGETTER))
					.apply(instance, (a, b) -> new RitualPatternSet(a, b.orElse(DEFAULT_DIRECTIONS))));
		return CODEC;
	}

	public static RitualPatternSet fromKeyword(IRitualPattern random, String word) {
		return new RitualPatternSet(random, KEYWORD_MAP.get(word));
	}

	/**
	 * 
	 * @param patterns
	 * @param rotationAndFlip
	 */
	public RitualPatternSet(IRitualPattern pattern, Multimap<FlipAxis, Direction> map) {

		this.pattern = pattern;
		this.rotationsAndFlips = HashBasedTable.create();
		for (Map.Entry<FlipAxis, Direction> entry : map.entries()) {
			if (!DIRECTION_TURNS.keySet().contains(entry.getValue())) {
				throw new IllegalArgumentException("Only permit horizontal directions.");
			}
			rotationsAndFlips.put(entry.getKey(), entry.getValue(),
					DIRECTION_TURNS.get(entry.getValue()).apply(pattern.flippedCopy(entry.getKey())));
		}
	}

	/**
	 * Check if any rotated version of this matches, return the first version
	 * matched
	 * 
	 * @param world
	 * @param center
	 * @param blockPreds
	 * @param entityPreds
	 * @return
	 */
	public Pair<FlipAxis, Direction> firstMatch(ServerLevel world, BlockPos focus,
			Map<String, IPlaceableGenreProvider<?, ?>> blockPreds) {
		for (Cell<FlipAxis, Direction, IRitualPattern> cell : this.rotationsAndFlips.cellSet()) {
			if (cell.getValue().matches(world, focus, blockPreds)) {
				return Pair.of(cell.getRowKey(), cell.getColumnKey());
			}
		}
		return null;
	}

	/**
	 * Check if any rotated version of this matches, return all versions matched
	 * 
	 * @param world
	 * @param center
	 * @param blockPreds
	 * @param entityPreds
	 * @return
	 */
	public Collection<IRitualPattern> allMatches(ServerLevel world, BlockPos focus,
			Map<String, IPlaceableGenreProvider<?, ?>> blockPreds) {
		Set<IRitualPattern> matches = new HashSet<>();
		for (Cell<FlipAxis, Direction, IRitualPattern> cell : this.rotationsAndFlips.cellSet()) {
			if (cell.getValue().matches(world, focus, blockPreds)) {
				matches.add(cell.getValue());
			}
		}
		return matches;
	}

	public Collection<Pair<FlipAxis, Direction>> getAllOrientations() {
		return rotationsAndFlips.cellSet().stream().map((c) -> Pair.of(c.getRowKey(), c.getColumnKey()))
				.collect(Collectors.toUnmodifiableSet());
	}

	public Multimap<FlipAxis, Direction> getAllOrientationsAsMultimap() {
		return rotationsAndFlips.cellSet().stream().collect(Multimaps.toMultimap(Cell::getRowKey, Cell::getColumnKey,
				MultimapBuilder.hashKeys().hashSetValues()::build));
	}

	/**
	 * The patterns this set is based on
	 * 
	 * @return
	 */
	public IRitualPattern getBasePattern() {
		return pattern;
	}

	/**
	 * Gets all rotated/flipped patterns variants
	 * 
	 * @return
	 */
	public Collection<IRitualPattern> getAllPatterns() {
		return Collections.unmodifiableCollection(this.rotationsAndFlips.values());
	}

	/**
	 * Return all versions of this that are flipped along an axis
	 * 
	 * @return
	 */
	public Collection<IRitualPattern> getFlips() {
		return Collections.unmodifiableCollection(this.rotationsAndFlips.column(Direction.NORTH).values());
	}

	/**
	 * Return all versions of this that are flipped along an axis and also rotated
	 * in the direction given
	 * 
	 * @return
	 */
	public Collection<IRitualPattern> getRotatedFlips(Direction dir) {
		return Collections.unmodifiableCollection(this.rotationsAndFlips.column(dir).values());
	}

	/**
	 * Returns all horizontally rotated versions
	 * 
	 * @return
	 */
	public Collection<IRitualPattern> getHorizontalRotations() {
		return Collections.unmodifiableCollection(this.rotationsAndFlips.row(FlipAxis.NONE).values());
	}

	/**
	 * Returns all versions flipped along the given axis and also horizontally
	 * rotated
	 * 
	 * @return
	 */
	public Collection<IRitualPattern> getFlippedHorizontalRotations(FlipAxis axis) {
		return Collections.unmodifiableCollection(this.rotationsAndFlips.row(axis).values());
	}

	/**
	 * Return whether this patterns-set has the given direction
	 * 
	 */
	public boolean has(Direction direction) {
		return this.rotationsAndFlips.contains(null, direction);
	}

	/**
	 * Return whether this patterns-set has the given flipped variant in the given
	 * direction
	 * 
	 */
	public boolean has(FlipAxis flip, Direction direction) {
		return this.rotationsAndFlips.contains(flip, direction);
	}

	/**
	 * Return the rotated variant of the patterns, or null if not present
	 * 
	 * @param direction
	 * @return
	 */
	public IRitualPattern getRotation(Direction direction) {
		return this.rotationsAndFlips.get(null, direction);
	}

	/**
	 * Return the flipped and rotated variant of the patterns, or null if not
	 * present
	 * 
	 * @param direction
	 * @return
	 */
	public IRitualPattern getFlippedRotation(FlipAxis axis, Direction direction) {
		return this.rotationsAndFlips.get(axis, direction);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof RitualPatternSet rps) {
			return this.pattern.equals(rps.pattern) && this.getAllOrientations().equals(rps.getAllOrientations());
		}
		return false;
	}

	@Override
	public int hashCode() {

		return this.pattern.hashCode() + this.getAllOrientations().hashCode();
	}

	@Override
	public String toString() {
		var orientations = this.getAllOrientationsAsMultimap();
		return "RPSet(" + this.pattern + "){directions="
				+ KEYWORD_MAP.inverse().getOrDefault(orientations, orientations.toString()) + "}";
	}
}
