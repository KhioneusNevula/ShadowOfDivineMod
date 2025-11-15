package com.gm910.sotdivine.magic.ritual.trigger.type.incantation;

import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;

import net.minecraft.network.chat.Component;

/**
 * An incantation trigger event
 */
public record IncantationTriggerEvent(Component magicWord) implements IRitualTriggerEvent {

	@Override
	public RitualTriggerType<?> triggerType() {
		return RitualTriggerType.INCANTATION;
	}

}
