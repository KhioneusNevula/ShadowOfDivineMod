package com.gm910.sotdivine.magic.ritual.trigger.type.right_click;

import java.util.Optional;

import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Right clicking block, as a trigger event
 */
public record RightClickTriggerEvent(Optional<Player> player, InteractionHand hand, ItemStack stack)
		implements IRitualTriggerEvent {

	@Override
	public RitualTriggerType<?> triggerType() {
		return RitualTriggerType.RIGHT_CLICK;
	}
}
