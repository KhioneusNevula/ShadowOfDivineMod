package com.gm910.sotdivine.systems.deity.ritual.trigger;

import com.mojang.serialization.Codec;

/**
 * Something that triggers a ritual. This interface merely stores basic info;
 * more specific info should be per-subclass
 */
public interface IRitualTrigger {

	public static Codec<IRitualTrigger> codec() {
		return RitualTriggerType.instanceCodec();
	}

	/**
	 * Return the type of trigger this is (to retrieve a codec)
	 * 
	 * @return
	 */
	public RitualTriggerType<?> triggerType();
}
