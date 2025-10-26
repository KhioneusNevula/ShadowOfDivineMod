package com.gm910.sotdivine.deities_and_parties.deity.ritual.properties;

import com.gm910.sotdivine.deities_and_parties.deity.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Kinds of rituals
 */
public enum RitualType {
	/** A ritual solely for showing respect to a deity, usually with no effect */
	VENERATION,
	/**
	 * A ritual to trigger a certain emanation; signified by
	 * {@link RitualEffectType#SUCCESS}
	 */
	SPELL;

	public static final Codec<RitualType> CODEC = CodecUtils.caselessEnumCodec(RitualType.class);
}
