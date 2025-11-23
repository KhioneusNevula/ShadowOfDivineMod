package com.gm910.sotdivine.magic.ritual.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.concepts.genres.IGenreType;
import com.gm910.sotdivine.concepts.genres.creator.IGenrePlacer;
import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.ItemGenreProvider;
import com.gm910.sotdivine.magic.emanation.DeityInteractionType;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatternSet;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.ritual.properties.RitualQuality;
import com.gm910.sotdivine.magic.ritual.properties.RitualType;
import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Static class for ritual-generation-related methods
 */
public class RitualGeneration {
	private RitualGeneration() {
	}

	/**
	 * Return all genres that should be used for the pattern
	 * 
	 * @param pattern
	 * @param $quality
	 * @param type
	 * 
	 * @param level
	 * @param deity
	 * @return
	 */
	public static Map<String, IPlaceableGenreProvider<?, ?>> makePatternSymbols(IRitualPattern pattern, RitualType type,
			RitualQuality quality, ServerLevel level, IDeity deity) {
		Map<String, IPlaceableGenreProvider<?, ?>> symbolMap = new HashMap<>();
		List<String> symbols = new ArrayList<>(pattern.symbols());
		int maxTries = symbols.size() > 5 ? symbols.size() * 3 : symbols.size() * 10;
		while (!symbols.isEmpty()) {
			if (maxTries <= 0)
				break;
			maxTries--;

			String sym = symbols.removeFirst();
			Collection<? extends IGenreType<?>> genreTypes = pattern.permittedGenres(sym);
			if (genreTypes.isEmpty()) {
				if (sym.equals(pattern.focusSymbol())) {
					genreTypes = List.of(GenreTypes.FOCUS_BLOCK);
				} else {
					genreTypes = List.of(GenreTypes.BUILDING_BLOCK);
				}
			}
			IPlaceableGenreProvider<?, ?> atGenre = WeightedSet
					.getRandom(
							genreTypes.stream()
									.flatMap((x) -> deity.spheres().stream()
											.<IPlaceableGenreProvider<?, ?>>flatMap((s) -> s.getGenres(x).stream()
													.map((y) -> (IPlaceableGenreProvider<?, ?>) y)))
									.filter((x) -> x.rarity() <= quality.rarity).toList(),
							IGenreProvider::rarity, level.random);
			if (atGenre == null) {
				return null;
			}
			Map<Vec3i, BlockState> blocks = new HashMap<>();
			CoveredLevelReader temporaryLevelReader = new CoveredLevelReader(level, blocks);
			// we must check no impossible positions exist
			boolean survivable = true;
			posCheckLoop: for (Vec3i atPos : pattern.getPositions(sym)) {
				for (int tryer = 0; tryer < 5; tryer++) {
					IGenrePlacer atPlacer = atGenre.generateRandom(level, Optional.empty());
					if (atPlacer == null)
						continue; // try again
					BlockState atState = atPlacer.getBlockState().orElse(Blocks.AIR.defaultBlockState());
					for (Direction direction : Direction.values()) {
						BlockPos adjPos = new BlockPos(atPos).relative(direction);
						BlockState onState = Optional.ofNullable(pattern.getSymbolAt(adjPos)).map(symbolMap::get)
								.map((adjP) -> adjP.generateRandom(level, Optional.empty()))
								.flatMap(IGenrePlacer::getBlockState).orElse(Blocks.AIR.defaultBlockState());
						blocks.put(adjPos, onState);
					}
					if (!atState.canSurvive(temporaryLevelReader, new BlockPos(atPos))) {
						survivable = false;
						break posCheckLoop;
					} else {
						break;
					}
				}
			}

			if (!survivable) { // if this block won't survive here, re-add it to revisit later
				symbols.add(sym);
			} else {
				symbolMap.put(sym, atGenre);
			}
		}
		if (!symbolMap.keySet().containsAll(pattern.symbols())) {
			return null;
		}
		return symbolMap;
	}

	/**
	 * Create offerings for a ritual
	 * 
	 * @param type
	 * @param $quality
	 * @param level
	 * @param deity
	 * @return
	 */
	public static Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> makeOfferings(RitualType type,
			RitualQuality quality, ServerLevel level, IDeity deity) {
		if (type == RitualType.VENERATION) {
			return Map.of(deity.spheres().stream().flatMap((s) -> s.getGenres(GenreTypes.OFFERING).stream())
					.collect(Collectors.toUnmodifiableSet()), quality.offeringsNeeded);
		}
		Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> map = new HashMap<>();
		int numPortion = 0;
		int cumulative = 0;
		final int maxGenresAllowed_TODO = 2; // TODO determine whether offerings should be individual genres or not
		do {
			numPortion = quality.offeringsNeeded - cumulative == 1 ? 1
					: level.random.nextIntBetweenInclusive(1, quality.offeringsNeeded - cumulative);
			cumulative += numPortion;
			Set<? extends IGiveableGenreProvider<?, ?>> genSet = Set.of();
			for (int i = 0; i < 10 && genSet.isEmpty(); i++) {
				List<ItemGenreProvider> genPoss = Lists
						.newArrayList(deity.spheres().stream().flatMap((s) -> s.getGenres(GenreTypes.OFFERING).stream())
								.filter((p) -> map.keySet().stream().noneMatch((set) -> set.contains(p))).iterator());
				if (genPoss.isEmpty()) {
					genPoss = Lists.newArrayList(deity.spheres().stream()
							.flatMap((s) -> s.getGenres(GenreTypes.OFFERING).stream()).iterator());
				}
				Collections.shuffle(genPoss);
				genSet = ImmutableSet.copyOf(genPoss.stream()
						.limit(level.random.nextIntBetweenInclusive(1, maxGenresAllowed_TODO)).distinct().iterator());
			}
			if (!genSet.isEmpty())
				map.put(genSet, numPortion);
		} while (quality.offeringsNeeded - cumulative > 0);

		return map; // offerings
	}

	/**
	 * Returns a collection of rituals for a deity
	 * 
	 * @param level
	 * @param ford
	 * @return
	 */
	public static Collection<IRitual> generateRituals(ServerLevel level, IDeity ford) {
		Set<IRitual> rituals = new HashSet<>();
		List<RitualType> typesShuffled = Arrays.asList(RitualType.values());
		List<RitualQuality> qualitiesShuffled = Arrays.asList(RitualQuality.values());
		Collections.shuffle(typesShuffled);
		Collections.shuffle(qualitiesShuffled);
		for (RitualType type : typesShuffled) {
			for (RitualQuality quality : qualitiesShuffled) {
				final int max = 5;
				int tries = (type != RitualType.SPELL)
						? Math.min(level.random.nextInt(1, max), level.random.nextInt(1, max))
						: (int) (ford.spheres().stream()
								.flatMap((s) -> s.emanationsOfType(DeityInteractionType.SPELL).stream())
								.filter((s) -> s.optionalSpellProperties().isEmpty() ? true
										: s.optionalSpellProperties().get().difficulty() == quality.spellPower)
								.count());
				boolean madeARitual = false;
				LinkedHashMap<String, IncompleteRitual> failedPhases = new LinkedHashMap<>();
				rgenTriesLoop: for (int i = 0; i < tries; i++) {
					if (!madeARitual) {
						tries += level.random.nextInt(0, 2); // optionally add up to two extra iterations on fails
						if (tries >= max * 3)
							break;
					} else {
						madeARitual = false;
					}

					IncompleteRitual gen = IncompleteRitual.phase1Effects(level, ford, type, quality);
					var effects = RitualEmanationTargeter.createRitualEmanations(type, quality, level, ford, rituals);
					if (effects == null) {
						failedPhases.put("creating_emanations", gen);
						continue;
					}
					gen = IncompleteRitual.phase2Pattern(gen, effects);
					for (int rp = 0; rp < 7; rp++) {
						IRitualPattern patternm = RitualPatterns.instance().getRandom(level.random,
								quality.maxPatternBlocks,
								(p) -> rituals.stream().noneMatch((r) -> r.patterns().getBasePattern().equals(p)));
						if (patternm == null) {
							patternm = RitualPatterns.instance().getRandom(level.random, quality.maxPatternBlocks,
									null);
						}
						final IRitualPattern pattern = patternm;
						if (pattern == null) {
							failedPhases.put("creating_pattern", gen);
							continue rgenTriesLoop;
						}
						gen = IncompleteRitual.phase2Point5Symbols(gen,
								RitualPatternSet.fromKeyword(pattern, "horizontals"));
						var patSyms = makePatternSymbols(pattern, type, quality, level, ford);
						if (patSyms == null) {
							failedPhases.put("creating_pattern_symbols", gen);
							continue rgenTriesLoop;
						}
						gen = IncompleteRitual.phase3Offerings(gen, patSyms);
						if (rituals.stream().noneMatch((r) -> r.ritualType() == type
								&& r.patterns().getBasePattern().equals(pattern) && r.symbols().equals(patSyms))) {
							// try to avoid duplicate pattern/symbols up to n times
							break;
						}
					}
					gen = IncompleteRitual.phase4Trigger(gen, makeOfferings(type, quality, level, ford));

					IncompleteRitual genAlias = gen;

					var trigs = new WeightedSet<>(
							RitualTriggerType.getAllTypes().stream().flatMap(
									(rtt) -> CollectionUtils.streamRepetitions(max, () -> rtt.createNew(genAlias))),
							(c) -> 1f / (rituals.stream().filter((x) -> x.trigger().equals(c)).count() + 1f));

					IRitualTrigger trigger = trigs.get(level.random);
					if (trigger == null) {
						failedPhases.put("creating_trigger", gen);
						continue;
					}
					// System.out.println("Generating ritual with trigger " + trigger + " and gen: "
					// + gen);
					IRitual ritual = IncompleteRitual.phase5Completion(gen, trigger);
					if (ritual == null) {
						failedPhases.put("creating_whole_ritual", gen);
						continue;
					}
					rituals.add(ritual);
					madeARitual = true;
					if (type == RitualType.VENERATION) { // only one veneration per $quality
						break;
					}
				}
				if (!failedPhases.isEmpty())
					LogUtils.getLogger().debug("Failed attempts(" + type + "," + quality + "): " + failedPhases);

			}
		}

		LogUtils.getLogger().debug("Generated " + rituals.size() + " rituals for deity " + ford);

		return rituals;
	}
}
