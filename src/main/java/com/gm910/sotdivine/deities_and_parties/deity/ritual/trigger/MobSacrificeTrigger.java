package com.gm910.sotdivine.deities_and_parties.deity.ritual.trigger;

import java.util.List;

import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.EntityGenreProvider;
import com.gm910.sotdivine.util.StreamUtils;

/**
 * A trigger which begins due to the killing of a mob within a ritual
 */
public record MobSacrificeTrigger(List<EntityGenreProvider> sacrifice) implements IRitualTrigger {

	@Override
	public RitualTriggerType<MobSacrificeTrigger> triggerType() {
		return RitualTriggerType.SACRIFICE;
	}

	@Override
	public final String toString() {
		return "Sacrifice" + sacrifice;
	}

}
