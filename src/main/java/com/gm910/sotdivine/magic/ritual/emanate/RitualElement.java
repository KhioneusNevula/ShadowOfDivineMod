package com.gm910.sotdivine.magic.ritual.emanate;

import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;

/**
 * Elements of a ritual that can be targeted
 */
public enum RitualElement {
	/** A banner/shield with a symbol */
	RECOGNIZED_SYMBOL,
	/** An item being offered, or an entity being sacrificed */
	OFFERING,
	/**
	 * An entity or rawPosition which is designated the target of the ritual's effect;
	 * defaults to the caster
	 */
	TARGET;

	public static final Codec<RitualElement> CODEC = CodecUtils.caselessEnumCodec(RitualElement.class);
}
