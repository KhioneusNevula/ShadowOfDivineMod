package com.gm910.sotdivine.magic.ritual.trigger.type.sacrifice;

import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Trigger event for a mobID sacrificed
 */
public record MobSacrificeTriggerEvent(Entity sacrificed) implements IRitualTriggerEvent {

	@Override
	public RitualTriggerType<?> triggerType() {
		return RitualTriggerType.SACRIFICE;
	}
}
