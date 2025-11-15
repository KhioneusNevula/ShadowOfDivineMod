package com.gm910.sotdivine.magic.ritual.trigger.type.item_offering;

import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;
import com.gm910.sotdivine.util.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Trigger the ritual when its focus is right-clicked
 * 
 * @return
 */
public enum ItemOfferingTrigger implements IRitualTrigger {
	INSTANCE;

	@Override
	public boolean matchesEvent(IRitualTriggerEvent event, IRitual ritual, ServerLevel level) {
		if (event instanceof ItemOfferingTriggerEvent ev) {
			return ritual.offerings().keySet().stream()
					.anyMatch((s) -> s.stream().anyMatch((g) -> g.matchesItem(level, ev.item())));
		}
		return false;
	}

	@Override
	public RitualTriggerType<?> triggerType() {
		return RitualTriggerType.OFFER_ITEM;
	}

	@Override
	public String toString() {
		return "OfferItem";
	}

	@Override
	public Component translate() {
		return TextUtils.transPrefix("sotd.cmd.ritual.trigger.offer_item");
	}

}
