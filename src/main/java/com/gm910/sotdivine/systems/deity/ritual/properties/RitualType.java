package com.gm910.sotdivine.systems.deity.ritual.properties;

import com.gm910.sotdivine.systems.deity.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Kinds of rituals
 */
public enum RitualType {
	/** A ritual solely for showing respect to a deity, with no effect */
	VENERATION,
	/** A ritual to assuage the anger of a deity, with no effect */
	PENANCE,
	/**
	 * A ritual to trigger a certain emanation; signified by
	 * {@link RitualEffectType#SUCCESS}
	 */
	EMANATION;

	public static final Codec<RitualType> CODEC = CodecUtils.caselessEnumCodec(RitualType.class);
}
