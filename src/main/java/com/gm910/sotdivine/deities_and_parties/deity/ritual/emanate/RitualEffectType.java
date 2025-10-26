package com.gm910.sotdivine.deities_and_parties.deity.ritual.emanate;

import com.gm910.sotdivine.deities_and_parties.deity.emanation.DeityInteractionType;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;

/**
 * Different kinds of events that occur alongside rituals
 */
public enum RitualEffectType {
	/**
	 * The emanation that occurs if the emanation succeeds. Pick from:
	 * {@link DeityInteractionType#SPELL}
	 */
	SUCCESS,
	/**
	 * The emanation that occurs if the ritual fails to start. Pick from:
	 * {@link DeityInteractionType#FAIL_SPELL} or
	 * {@link DeityInteractionType#SPELL}, specifically curses
	 */
	FAIL_START,
	/**
	 * The emanation that occurs if the ritual fails in the middle. Pick from
	 * {@link DeityInteractionType#FAIL_SPELL}
	 */
	FAIL_TICK,
	/**
	 * The emanation that occurs if the ritual is interrupted in the middle. Pick
	 * from: {@link DeityInteractionType#FAIL_SPELL}
	 */
	INTERRUPTION,
	/**
	 * The emanation that occufrs when the ritual starts to signify it has started.
	 * Pick from: {@link DeityInteractionType#ACCEPT_OFFERING} and
	 * {@link DeityInteractionType#SYMBOL_RECOGNITION}
	 */
	SIGNIFIER;

	public static final Codec<RitualEffectType> CODEC = CodecUtils.caselessEnumCodec(RitualEffectType.class);
}
