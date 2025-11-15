package com.gm910.sotdivine.magic.ritual;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatternSet;
import com.gm910.sotdivine.magic.ritual.properties.RitualQuality;
import com.gm910.sotdivine.magic.ritual.properties.RitualType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;

import net.minecraft.server.level.ServerLevel;

/**
 * A record to store information needed to generate a ritual. This information
 * may update as each stage of generation progresses
 * 
 * @param offerings => each set is a joint offering set where all are
 *                  obligatory; the entire map is an "or" set where at minimum
 *                  one is oblgiatory
 */
public record RitualGeneration(ServerLevel level, Phase phase, IDeity forDeity, RitualType type, RitualQuality quality,
		Optional<Map<RitualEffectType, RitualEmanationTargeter>> effects, Optional<RitualPatternSet> patterns,
		Optional<Map<String, IPlaceableGenreProvider<?, ?>>> symbols,
		Optional<Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer>> offerings) {

	/**
	 * Instantiates the first phase of generation: the selection of Effects
	 */
	public static RitualGeneration phase1Effects(ServerLevel level, IDeity ford, RitualType type,
			RitualQuality quality) {
		return new RitualGeneration(level, Phase.EFFECTS, ford, type, quality, Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty());
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
		return new RitualGeneration(prior.level, Phase.PATTERN, prior.forDeity, prior.type, prior.quality,
				Optional.of(effects), Optional.empty(), Optional.empty(), Optional.empty());
	}

	/**
	 * Instantiates the third-half phase: selection of symbols
	 * 
	 * @param effectsPhase
	 * @param patterns
	 * @param symbols
	 * @return
	 */
	public static RitualGeneration phase2Point5Symbols(RitualGeneration prior, RitualPatternSet patterns) {
		prior.effects.orElseThrow(() -> new IllegalStateException("No effects in " + prior));
		return new RitualGeneration(prior.level, Phase.SYMBOLS, prior.forDeity, prior.type, prior.quality,
				prior.effects, Optional.of(patterns), Optional.empty(), Optional.empty());
	}

	/**
	 * Instantiates the fourth phase: selection of offerings
	 * 
	 * @param effectsPhase
	 * @param patterns
	 * @param symbols
	 * @return
	 */
	public static RitualGeneration phase3Offerings(RitualGeneration prior,
			Map<String, IPlaceableGenreProvider<?, ?>> symbols) {
		prior.patterns.orElseThrow(() -> new IllegalStateException("No patterns in " + prior));
		prior.effects.orElseThrow(() -> new IllegalStateException("No effects in " + prior));
		return new RitualGeneration(prior.level, Phase.OFFERINGS, prior.forDeity, prior.type, prior.quality,
				prior.effects, prior.patterns, Optional.of(symbols), Optional.empty());
	}

	/**
	 * Fourth phase, selection of trigger
	 * 
	 * @param prior
	 * @param offerings
	 * @return
	 */
	public static RitualGeneration phase4Trigger(RitualGeneration prior,
			Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> offerings) {
		prior.patterns.orElseThrow(() -> new IllegalStateException("No patterns in " + prior));
		prior.effects.orElseThrow(() -> new IllegalStateException("No effects in " + prior));
		prior.symbols.orElseThrow(() -> new IllegalStateException("No symbols in " + prior));
		return new RitualGeneration(prior.level, Phase.TRIGGER, prior.forDeity, prior.type, prior.quality,
				prior.effects, prior.patterns, prior.symbols, Optional.of(offerings));
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
		prior.patterns.orElseThrow(() -> new IllegalStateException("No patterns in " + prior));
		prior.effects.orElseThrow(() -> new IllegalStateException("No effects in " + prior));
		prior.symbols.orElseThrow(() -> new IllegalStateException("No symbols in " + prior));
		prior.offerings.orElseThrow(() -> new IllegalStateException("No offerings in " + prior));
		return new Ritual(prior.type, prior.quality, prior.patterns.get(), prior.effects.get(), prior.symbols.get(),
				prior.offerings.get(), Objects.requireNonNull(trigger, "Supplied null trigger " + prior));
	}

	@Override
	public final String toString() {
		return "Generating{(Phase 0) deity=" + forDeity + ",type=" + type + ",quality=" + quality + (effects.isPresent()
				? ", (Phase 1) effects=" + effects.get()
						+ (patterns.isPresent()
								? ", (Phase 2) patterns=" + patterns.get()
										+ (symbols.isPresent()
												? ", (Phase 2.5) symbols=" + symbols.get()
														+ (offerings.isPresent() ? ", (Phase 3) offerings=" + offerings
																: "")
												: "")
								: "")
				: "") + ",in-phase=" + phase + "}";
	}

	public static enum Phase {
		EFFECTS, PATTERN, SYMBOLS, OFFERINGS, TRIGGER

	}
}
