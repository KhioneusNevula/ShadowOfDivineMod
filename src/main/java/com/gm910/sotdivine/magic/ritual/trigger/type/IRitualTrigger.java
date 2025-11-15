package com.gm910.sotdivine.magic.ritual.trigger.type;

import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Something that triggers a ritual. This interface merely stores basic info;
 * more specific info should be per-subclass
 */
public interface IRitualTrigger {

	/**
	 * Whether this trigger matches the given event
	 * 
	 * @param event
	 * @return
	 */
	public boolean matchesEvent(IRitualTriggerEvent event, IRitual ritual, ServerLevel level);

	public static Codec<IRitualTrigger> codec() {
		return RitualTriggerType.instanceCodec();
	}

	/**
	 * Return the type of trigger this is (to retrieve a codec)
	 * 
	 * @return
	 */
	public RitualTriggerType<?> triggerType();

	/**
	 * Return a displayable name for this trigger
	 * 
	 * @return
	 */
	public Component translate();
}
