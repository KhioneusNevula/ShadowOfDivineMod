package com.gm910.sotdivine.systems.deity.ritual.properties;

import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;

/**
 * The level of a ritual
 */
public enum RitualQuality {

	/**
	 * For rituals done so badly they actually offend a deity
	 */
	OFFENSIVE,
	/**
	 * For rituals that are done at a bare minimum
	 */
	MEAGER,
	/** For average, normal rituals */
	FINE,

	/**
	 * For a good quality ritual
	 */
	GOOD,

	/**
	 * For an amazing ritual
	 */
	WONDROUS;

	public static final Codec<RitualQuality> CODEC = CodecUtils.caselessEnumCodec(RitualQuality.class);
}
