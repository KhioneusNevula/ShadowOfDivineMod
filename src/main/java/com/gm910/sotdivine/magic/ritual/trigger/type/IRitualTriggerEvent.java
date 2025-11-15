package com.gm910.sotdivine.magic.ritual.trigger.type;

import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;

/**
 * An event that a trigger can be checked against to determine if it matches it
 */
public interface IRitualTriggerEvent {

	public RitualTriggerType<?> triggerType();
}
