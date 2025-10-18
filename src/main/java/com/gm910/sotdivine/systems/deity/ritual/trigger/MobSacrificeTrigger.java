package com.gm910.sotdivine.systems.deity.ritual.trigger;

import java.util.List;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.EntityGenreProvider;

/**
 * A trigger which begins due to the killing of a mob within a ritual
 */
public record MobSacrificeTrigger(List<EntityGenreProvider> sacrifice) implements IRitualTrigger {

	@Override
	public RitualTriggerType<MobSacrificeTrigger> triggerType() {
		return RitualTriggerType.SACRIFICE;
	}

}
