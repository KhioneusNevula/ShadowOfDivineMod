package com.gm910.sotdivine.systems.deity.ritual;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.systems.deity.IDeity;
import com.gm910.sotdivine.systems.deity.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.systems.deity.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.systems.deity.ritual.pattern.RitualPatternSet;
import com.gm910.sotdivine.systems.deity.ritual.properties.RitualQuality;
import com.gm910.sotdivine.systems.deity.ritual.properties.RitualType;
import com.gm910.sotdivine.systems.deity.ritual.trigger.IRitualTrigger;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;

import net.minecraft.server.level.ServerLevel;

/**
 * A record to store information needed to generate a ritual. This information
 * may update as each stage of generation progresses
 */
public record RitualGeneration(ServerLevel level, Phase phase, IDeity forDeity, RitualType type, RitualQuality quality,
		Optional<Map<RitualEffectType, RitualEmanationTargeter>> effects, Optional<RitualPatternSet> patterns,
		Optional<Map<String, IPlaceableGenreProvider<?, ?>>> symbols,
		Optional<Map<IGiveableGenreProvider<?, ?>, Integer>> offerings, Optional<IRitualTrigger> trigger) {

	/**
	 * Instantiates the first phase of generation: the selection of Effects
	 */
	public static RitualGeneration phase1Effects(ServerLevel level, IDeity ford, RitualType type,
			RitualQuality quality) {
		return new RitualGeneration(level, Phase.EFFECTS, ford, type, quality, Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty(), Optional.empty());
	}

	/**
	 * Instantiates the second stage of generation: selection of the pattern
	 * 
	 * @param fromSpheres
	 * @param type
	 * @param quality
	 * @param effects
	 * @return
	 */
	public static RitualGeneration phase2Pattern(RitualGeneration prior,
			Map<RitualEffectType, RitualEmanationTargeter> effects) {
		if (prior.phase != Phase.EFFECTS)
			throw new IllegalArgumentException(prior + "");
		return new RitualGeneration(prior.level, Phase.PATTERN, prior.forDeity, prior.type, prior.quality,
				Optional.of(effects), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
	}

	/**
	 * Instantiates the third phase: selection of offerings
	 * 
	 * @param effectsPhase
	 * @param patterns
	 * @param symbols
	 * @return
	 */
	public static RitualGeneration phase3Offerings(RitualGeneration prior, RitualPatternSet patterns,
			Map<String, IPlaceableGenreProvider<?, ?>> symbols) {
		if (prior.phase != Phase.PATTERN)
			throw new IllegalArgumentException(prior + "");
		return new RitualGeneration(prior.level, Phase.OFFERINGS, prior.forDeity, prior.type, prior.quality,
				prior.effects, Optional.of(patterns), Optional.of(symbols), Optional.empty(), Optional.empty());
	}

	/**
	 * Fourth phase, selection of trigger
	 * 
	 * @param prior
	 * @param offerings
	 * @return
	 */
	public static RitualGeneration phase4Trigger(RitualGeneration prior,
			Map<IGiveableGenreProvider<?, ?>, Integer> offerings) {
		if (prior.phase != Phase.OFFERINGS)
			throw new IllegalArgumentException(prior + "");
		return new RitualGeneration(prior.level, Phase.TRIGGER, prior.forDeity, prior.type, prior.quality,
				prior.effects, prior.patterns, prior.symbols, Optional.of(offerings), Optional.empty());
	}

	/**
	 * Final phase complete; returns a full ritual
	 * 
	 * @param prior
	 * @param trigger
	 * @return
	 */
	public static IRitual phase5Completion(RitualGeneration prior, IRitualTrigger trigger) {
		if (prior.phase != Phase.TRIGGER)
			throw new IllegalArgumentException(prior + "");
		return new Ritual(prior.type, prior.quality, prior.patterns.get(), prior.effects.get(), prior.symbols.get(),
				prior.offerings.get(), prior.trigger.get());
	}

	public static enum Phase {
		EFFECTS, PATTERN, OFFERINGS, TRIGGER

	}
}
