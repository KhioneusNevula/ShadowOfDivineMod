package com.gm910.sotdivine.magic.ritual;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.Lists;

import com.gm910.sotdivine.concepts.deity.Deity;
import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.deity.personality.DeityStat;
import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.concepts.genres.IGenreType;
import com.gm910.sotdivine.concepts.genres.creator.IGenrePlacer;
import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.ItemGenreProvider;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.magic.emanation.DeityInteractionType;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatternSet;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.ritual.properties.IRitualParameters;
import com.gm910.sotdivine.magic.ritual.properties.RitualParameters;
import com.gm910.sotdivine.magic.ritual.properties.RitualQuality;
import com.gm910.sotdivine.magic.ritual.properties.RitualType;
import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Rituals that can be invoked for deities, which includes both
 */
public sealed interface IRitual permits Ritual {

	/**
	 * Return all genres that should be used for the pattern
	 * 
	 * @param pattern
	 * @param quality
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
				List<RitualGeneration> failedPhases = new ArrayList<>();
				rgenTriesLoop: for (int i = 0; i < tries; i++) {
					if (!madeARitual) {
						tries += level.random.nextInt(0, 2); // optionally add up to two extra iterations on fails
						if (tries >= max * 3)
							break;
					} else {
						madeARitual = false;
					}

					RitualGeneration gen = RitualGeneration.phase1Effects(level, ford, type, quality);
					var effects = RitualEmanationTargeter.createRitualEmanations(type, quality, level, ford, rituals);
					if (effects == null) {
						failedPhases.add(gen);
						continue;
					}
					gen = RitualGeneration.phase2Pattern(gen, effects);
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
							failedPhases.add(gen);
							continue rgenTriesLoop;
						}
						gen = RitualGeneration.phase2Point5Symbols(gen,
								RitualPatternSet.fromKeyword(pattern, "horizontals"));
						var patSyms = makePatternSymbols(pattern, type, quality, level, ford);
						if (patSyms == null) {
							failedPhases.add(gen);
							continue rgenTriesLoop;
						}
						gen = RitualGeneration.phase3Offerings(gen, patSyms);
						if (rituals.stream().noneMatch((r) -> r.ritualType() == type
								&& r.patterns().getBasePattern().equals(pattern) && r.symbols().equals(patSyms))) {
							// try to avoid duplicate pattern/symbols up to n times
							break;
						}
					}
					gen = RitualGeneration.phase4Trigger(gen, makeOfferings(type, quality, level, ford));
					IRitualTrigger trigger = null;
					for (int j = 0; j < max && trigger == null; j++) {
						var trigs = new WeightedSet<>(RitualTriggerType.getAllTypes(), (c) -> 1f
								/ (rituals.stream().filter((x) -> x.trigger().triggerType().equals(c)).count() + 1f));

						IRitualTrigger triggeru = trigs.get(level.random).createNew(gen);
						if (rituals.stream().noneMatch((x) -> x.trigger().equals(triggeru))) {
							trigger = triggeru;
							break;
						}
					}
					if (trigger == null) {
						failedPhases.add(gen);
						continue;
					}
					// System.out.println("Generating ritual with trigger " + trigger + " and gen: "
					// + gen);
					IRitual ritual = RitualGeneration.phase5Completion(gen, trigger);
					if (ritual == null) {
						failedPhases.add(gen);
						continue;
					}
					rituals.add(ritual);
					madeARitual = true;
					if (type == RitualType.VENERATION) { // only one veneration per quality
						break;
					}
				}
				if (!failedPhases.isEmpty())
					LogUtils.getLogger().debug("Failed attempts(" + type + "," + quality + "): " + failedPhases);

			}
		}

		return rituals;
	}

	/**
	 * Scans for all deity symbols around this rawPosition and identify one "winning
	 * deity", and return a stream all other deities matched
	 * 
	 * @param world
	 * @param fromPos
	 * @param generalize = whether to generalize the search to all deities if no
	 *                   deities are found
	 * @return
	 */
	public static Optional<Entry<IDeity, Stream<IDeity>>> identifyWinningDeity(ServerLevel world, BlockPos fromPos,
			double radius, boolean generalize) {
		IPartySystem system = IPartySystem.get(world);
		List<Entry<Either<Entity, BlockPos>, IDeity>> patternBearers = Lists
				.newArrayList(system.findDeitySymbols(world, fromPos, radius).iterator());
		// count which deity has the most
		Multiset<IDeity> deityCounts = HashMultiset.create();
		for (var entry : patternBearers) {
			deityCounts.add(entry.getValue());
		}
		Optional<Map.Entry<IDeity, Float>> enta = deityCounts.entrySet().stream()
				.map((en) -> Map.entry(en.getElement(), en.getCount() * en.getElement().statValue(DeityStat.POWER)))
				.max((s, s2) -> Float.compare(s.getValue(), s2.getValue()));
		if (enta.isEmpty()) {
			if (generalize) {
				LogUtils.getLogger().debug(
						"No symbols found around " + fromPos + ", (DEBUG) generalizing offering to all deities..");
				/** continue; */
				List<IDeity> deitets = Lists.newArrayList(system.allDeities().stream().iterator());
				Collections.shuffle(deitets);
				return Optional.of(Map.entry(deitets.removeFirst(), deitets.stream()));
			}
			return Optional.empty();
		}
		float maxPower = enta.get().getValue();
		List<IDeity> competitors = new ArrayList<>();
		competitors.add(enta.get().getKey());
		deityCounts.entrySet().stream()
				.filter((s) -> s.getCount() * s.getElement().statValue(DeityStat.POWER) >= maxPower)
				.forEach((s) -> competitors.add(s.getElement()));
		Collections.shuffle(competitors);
		// sort this by power return it in case
		patternBearers.sort(
				(s, s2) -> -Float.compare(deityCounts.count(s.getValue()) * s.getValue().statValue(DeityStat.POWER),
						deityCounts.count(s2.getValue()) * s2.getValue().statValue(DeityStat.POWER)));
		var winner = competitors.getFirst();

		return Optional.of(Map.entry(winner, patternBearers.stream().filter((s) -> !s.getValue().equals(winner))
				.map((s) -> s.getValue()).distinct()));
	}

	/**
	 * Either converts the surrounding symbols to instrument the given deity, or
	 * just plays a recognition effect on them
	 * 
	 * @param world
	 * @param fromPos
	 * @param radius
	 */
	public static void convertOrRecognizeSymbols(ServerLevel world, BlockPos fromPos, double radius, IDeity winner) {
		IPartySystem system = IPartySystem.get(world);
		List<Entry<Either<Entity, BlockPos>, IDeity>> list = system.findDeitySymbols(world, fromPos, radius).toList();
		LogUtils.getLogger().debug("Converting symbols around " + fromPos + " (distance=" + radius + ") to that of "
				+ winner + ": " + list);
		for (var entry : list) {
			Either<Entity, BlockPos> either = entry.getKey();
			if (either.map((p) -> system.convertShieldPatterns(world, p, winner),
					(p) -> system.convertBannerPatterns(world, p, winner)).booleanValue()) {
				var didConversionEmanation = winner.triggerAnEmanation(DeityInteractionType.SYMBOL_CONVERSION,
						ISpellTargetInfo.builder(winner, world)
								.branch(either, (l, b) -> b.targetEntityAndPos(l), (r, b) -> b.targetPos(r)).build(),
						1.0f);
				/*if (didConversionEmanation != null)
					return;*/
			}
		}
	}

	/**
	 * Start the process of determining whether a ritual can be executed.
	 * 
	 * @param level          the world to do this in
	 * @param deity          the deity which is being checked
	 * @param causer         the uuid of the entity which is "responsible" for the
	 *                       ritual
	 * @param searchRadius   the radius to search for symbols
	 * @param triggerEvent   the "trigger"
	 * @param checkPositions the positions to try to use as the focus
	 * @return whether any ritual started
	 */
	public static boolean tryDetectAndInitiateAnyRitual(ServerLevel level, IDeity deity, UUID causer,
			double searchRadius, IRitualTriggerEvent triggerEvent, Collection<BlockPos> checkPositions) {
		IPartySystem system = IPartySystem.get(level);

		for (BlockPos focusPos : checkPositions) {
			List<Entry<IRitual, IRitualPattern>> rituals = Lists
					.newArrayList(system.getMatchingRituals(level, focusPos, deity, triggerEvent).iterator());
			Collections.sort(rituals, (e1, e2) -> {
				if (e1.getKey().ritualType() == RitualType.SPELL) {
					return -1;
				} else if (e2.getKey().ritualType() == RitualType.SPELL) {
					return 1;
				}
				return e2.getValue().blockCount() - e1.getValue().blockCount();
			});
			if (!rituals.isEmpty()) {
				LogUtils.getLogger().debug(
						"Found matching rituals for pos " + focusPos + " and event " + triggerEvent + "; iterating");
				for (Entry<IRitual, IRitualPattern> ritEntry : rituals) {

					/*LogUtils.getLogger().debug(
							"Checking ritual for pos " + focusPos + " and event " + triggerEvent + ": " + ritEntry);*/
					Map<ItemEntity, Integer> offeringItems = ritEntry.getKey().offeringsPresent(level, focusPos,
							ritEntry.getValue());
					if (offeringItems != null) {
						LogUtils.getLogger().debug("Matched to ritual " + ritEntry);
						IRitual.convertOrRecognizeSymbols(level, focusPos, searchRadius, deity);
						// get all the shield/banner holders
						Set<Entity> shieldHolders = new HashSet<>();
						Set<BlockPos> bannerPoses = new HashSet<>();
						deity.findDeitySymbols(level, focusPos, searchRadius).forEach(
								(e) -> e.ifLeft((s) -> shieldHolders.add(s)).ifRight((s) -> bannerPoses.add(s)));

						// TODO calc parameters
						ritEntry.getKey().initiateRitual(level, deity, causer,
								GlobalPos.of(level.dimension(), focusPos), ritEntry.getValue(),
								new RitualParameters(Map.of()), triggerEvent, bannerPoses, shieldHolders,
								offeringItems);
						return true;
					} else {
						LogUtils.getLogger().debug("Inadequate offerings for " + focusPos + " and event " + triggerEvent
								+ ": " + ritEntry);
					}
				}
			} else {
				/*LogUtils.getLogger()
						.debug("Failed to instrument any ritual at blockpos " + focusPos + " for deity: " + deity);*/
			}
		}
		return false;
	}

	/**
	 * Return a set of all offering item entities present with the amount of the
	 * item that was "extracted", or null if they are not
	 * 
	 * @param level
	 * @param focus
	 * @param pattern
	 * @return
	 */
	public default Map<ItemEntity, Integer> offeringsPresent(ServerLevel level, BlockPos focus,
			IRitualPattern pattern) {
		Map<ItemEntity, Integer> items = new HashMap<>();
		Multiset<Set<? extends IGiveableGenreProvider<?, ?>>> countingOfferings = HashMultiset.create();
		int completedSets = 0;
		List<ItemEntity> entityList = Lists.newArrayList(pattern
				.getEntitiesInPattern(level, focus, EntityTypeTest.forClass(ItemEntity.class), Predicates.alwaysTrue())
				.iterator());
		Collections.shuffle(entityList);
		itemLoop: for (ItemEntity item : entityList) {
			var setsIterator = this.offerings().keySet().stream()
					.filter((s) -> countingOfferings.count(s) < offerings().get(s)) // only sets which are incomplete
					.filter((s) -> s.stream().anyMatch((g) -> g.matchesItem(level, item.getItem()))).iterator();
			// get count of the item entity
			if (setsIterator.hasNext()) {
				int count = item.getItem().getCount();
				while (setsIterator.hasNext() && count >= 0) {
					var offeringSet = setsIterator.next();
					int minCount = offerings().get(offeringSet); // min req count for this set
					/* Add as many instances to the set as the count of the stack; calculate how many we have overflowing higher than minCount */
					int overflow = countingOfferings.add(offeringSet, count) + count - minCount;
					if (overflow >= 0) {
						// if we have overflow or we perfectly hit the limit, mark completed
						completedSets++;
						// decrease count for next sets since offerings are a limited resource
						count = overflow;
					} else {
						// if overflow is negative (i.e. count did not complete the requisite amount)
						// then we are done with this item
						count = 0;
						break;
					}
				}
				items.put(item, item.getItem().getCount() - count);
			}

		}
		return completedSets >= offerings().size() ? items : null;
	}

	/**
	 *
	 * @return
	 */
	public static Codec<IRitual> codec() {
		return Ritual.codec();
	}

	/**
	 * Return the patterns the ritual uses
	 * 
	 * @return
	 */
	public RitualPatternSet patterns();

	/**
	 * Predicates for the blocks in this ritual's patterns
	 * 
	 * @return
	 */
	public Map<String, IPlaceableGenreProvider<?, ?>> symbols();

	/**
	 * Offerings needed for this ritual. At least the specified number of any
	 * offering from each set must be selected. E.g. if the distribution was:
	 * {(bone, arrow, egg)=1, (bottle_of_enchanting)=2}, then at least one bone,
	 * arrow, or egg must be picked, and at least two bottles of enchanting must be
	 * picked. Thus it can be thought of as "(bone >= 1 || arrow >= 1 || egg >= 1)
	 * && (bottle_of_enchanting >= 2)"
	 * 
	 * @return
	 */
	public Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> offerings();

	/**
	 * If this ritual triggers an emanation for the given kind of effect, return the
	 * emanation (Targeter)
	 * 
	 * @return
	 */
	public Optional<RitualEmanationTargeter> ritualEffect(RitualEffectType effect);

	/**
	 * The type of this ritual
	 * 
	 * @return
	 */
	public RitualType ritualType();

	/**
	 * The quality of this ritual
	 * 
	 * @return
	 */
	public RitualQuality ritualQuality();

	/**
	 * Return this ritual's trigger
	 * 
	 * @return
	 */
	public IRitualTrigger trigger();

	/**
	 * Return all emanations of this ritual
	 * 
	 * @return
	 */
	public Collection<IEmanation> emanations();

	/**
	 * Coalesces the emanation emanation(s) in this (if any) with the emanation
	 * emanation of the deity to avoid redundancy
	 * 
	 * @param deity
	 * @return
	 */
	public Ritual coalesce(IDeity deity);

	/**
	 * Starts this ritual, creating/triggering an appropriate emanation instance and
	 * supplying the appropriate info
	 * 
	 * @param level
	 * @param deity
	 * @param caster
	 * @param atPos
	 * @param pattern   the pattern which successfully matched
	 * @param banners   banner positions for signifier detection
	 * @param shields   entities with symbol shields
	 * @param offerings entities that act as offerings and the portion of their
	 *                  count to remove
	 * @param
	 */
	public void initiateRitual(ServerLevel level, IDeity deity, UUID caster, GlobalPos atPos, IRitualPattern pattern,
			IRitualParameters parameters, IRitualTriggerEvent event, Collection<BlockPos> banners,
			Collection<? extends Entity> shields, Map<ItemEntity, Integer> offerings);

	/**
	 * Signals to the ritual to run its 'interrupt' effect or 'fail' effect
	 * depending on what happened
	 * 
	 * @param level
	 * @param deity
	 * @param focusPos
	 * @param successfulPattern
	 * @param intensity
	 * @param interrupt
	 */
	public void signalStop(RitualInstance instance, ServerLevel level, Deity deity, GlobalPos focusPos,
			IRitualPattern successfulPattern, float intensity, boolean interrupt);

}
