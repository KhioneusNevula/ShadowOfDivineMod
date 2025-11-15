package com.gm910.sotdivine.magic.ritual.trigger.type.item_offering;

import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Throwing an offering, as a trigger event
 */
public record ItemOfferingTriggerEvent(ItemStack item) implements IRitualTriggerEvent {

	@Override
	public RitualTriggerType<?> triggerType() {
		return RitualTriggerType.OFFER_ITEM;
	}
}
