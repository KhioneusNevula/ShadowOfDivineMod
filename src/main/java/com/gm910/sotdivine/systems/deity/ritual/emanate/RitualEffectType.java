package com.gm910.sotdivine.systems.deity.ritual.emanate;

import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;

public enum RitualEffectType {
	/**
	 * The emanation that occurs if the emanation succeeds
	 */
	SUCCESS,
	/**
	 * The emanation that occurs if the ritual fails
	 */
	FAILURE,
	/**
	 * The emanation that occurs if the ritual is interrupted
	 */
	INTERRUPTION,
	/**
	 * The emanation that occufrs when the ritual starts to signify it has started
	 */
	SIGNIFIER;

	public static final Codec<RitualEffectType> CODEC = CodecUtils.caselessEnumCodec(RitualEffectType.class);
}
