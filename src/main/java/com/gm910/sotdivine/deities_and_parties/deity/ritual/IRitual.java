package com.gm910.sotdivine.deities_and_parties.deity.ritual;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gm910.sotdivine.deities_and_parties.deity.Deity;
import com.gm910.sotdivine.deities_and_parties.deity.IDeity;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.IEmanation;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.pattern.RitualPatternSet;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.properties.IRitualParameters;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.properties.RitualQuality;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.properties.RitualType;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.trigger.IRitualTrigger;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.IGenreType;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Rituals that can be invoked for deities, which includes both
 */
public sealed interface IRitual permits Ritual {

	/**
	 * The mapping of symbols to genres
	 */
	public static final Multimap<String, IGenreType<?>> SYMBOLS_TO_GENRES = ImmutableMultimap
			.<String, IGenreType<?>>builder()

			.put("X", GenreTypes.FOCUS_BLOCK).put("F", GenreTypes.DECOR_BLOCK) // X and F are focus blocks

			.put("B", GenreTypes.BUILDING_BLOCK) // B is a building block

			.build();

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
		Map<String, IPlaceableGenreProvider<?, ?>> map = new HashMap<>();
		for (String sym : pattern.symbols()) {
			var genreTypes = SYMBOLS_TO_GENRES.asMap().getOrDefault(sym,
					GenreTypes.getAllGenreTypes().values().stream().filter((s) -> s.isPlaceable()).toList());

			IPlaceableGenreProvider<?, ?> gen = WeightedSet.getRandom(genreTypes.stream()
					.flatMap((x) -> deity.spheres().stream().flatMap((s) -> s.getGenres(x).stream()))
					.filter((x) -> x instanceof IPlaceableGenreProvider).map((x) -> (IPlaceableGenreProvider<?, ?>) x)
					.filter((x) -> x.rarity() <= quality.rarity).toList(), IGenreProvider::selectionWeight,
					level.random);
			if (gen == null) {
				return null;
			}
			map.put(sym, gen);
		}
		return map;
	}

	public static Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> makeOfferings(RitualType type,
			RitualQuality quality, ServerLevel level, IDeity deity) {
		if (type == RitualType.VENERATION) {
			return Map.of(deity.spheres().stream().flatMap((s) -> s.getGenres(GenreTypes.OFFERING).stream())
					.collect(Collectors.toUnmodifiableSet()), 1);
		}
		return Map.of(); // offerings
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
				int tries = Math.min(level.random.nextInt(1, max), level.random.nextInt(1, max));
				boolean made = false;
				for (int i = 0; i < tries; i++) {
					if (!made) {
						tries += level.random.nextInt(0, 2); // optionally add up to two extra iterations on fails
						if (tries >= max * 3)
							break;
					} else {
						made = false;
					}

					RitualGeneration gen = RitualGeneration.phase1Effects(level, ford, type, quality);
					var effects = RitualEmanationTargeter.createRitualEmanations(type, quality, level, ford);
					if (effects == null)
						continue;
					gen = RitualGeneration.phase2Pattern(gen, effects);
					IRitualPattern pattern = RitualPatterns.instance().getRandom(level.random,
							quality.maxPatternBlocks);
					if (pattern == null)
						continue;
					var patSyms = makePatternSymbols(pattern, type, quality, level, ford);
					if (patSyms == null)
						continue;
					gen = RitualGeneration.phase3Offerings(gen, RitualPatternSet.fromKeyword(pattern, "horizontals"),
							patSyms);
					gen = RitualGeneration.phase4Trigger(gen, makeOfferings(type, quality, level, ford));
					IRitualTrigger trigger = null;
					for (int j = 0; j < max && trigger == null; j++) {
						trigger = WeightedSet.getRandom(RitualTriggerType.getAllTypes(), level.random).createNew(gen);
					}
					if (trigger == null)
						continue;
					// System.out.println("Generating ritual with trigger " + trigger + " and gen: "
					// + gen);
					IRitual ritual = RitualGeneration.phase5Completion(gen, trigger);
					rituals.add(ritual);
					made = true;
				}
			}
		}

		return rituals;
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
	 * picked
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
	 * @param offerings entities that act as offerings
	 * @param
	 */
	public void initiateRitual(ServerLevel level, IDeity deity, UUID caster, GlobalPos atPos, IRitualPattern pattern,
			IRitualParameters parameters, Collection<BlockPos> banners, Collection<Entity> shields,
			Collection<Entity> offerings);

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
