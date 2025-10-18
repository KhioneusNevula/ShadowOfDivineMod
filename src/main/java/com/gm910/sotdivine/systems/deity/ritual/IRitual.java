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
import com.mojang.serialization.Codec;

public sealed interface IRitual permits Ritual {

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
	 * Offerings needed for this ritual + minimum amounts required
	 * 
	 * @return
	 */
	public Map<IGiveableGenreProvider<?, ?>, Integer> offerings();

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
	 * Coalesces the emanation emanation(s) in this (if any) with the emanation
	 * emanation of the deity to avoid redundancy
	 * 
	 * @param deity
	 * @return
	 */
	public Ritual coalesce(IDeity deity);
}
