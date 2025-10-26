package com.gm910.sotdivine.deities_and_parties.deity.emanation;

import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;

/**
 * A way a deity interacts with the world
 */
public enum DeityInteractionType {
	/**
	 * An emanation that occurs when a deity starts a ritual at a position
	 */
	ACCEPT_OFFERING,
	/**
	 * An emanation that occurs when a deity finds one of its symbols during an
	 * offering or something similar. Targets the entity with the symbol or the
	 * position of the block it is at
	 */
	SYMBOL_RECOGNITION,
	/** An emanation that occurs when a deity spell fails; applies to the symbols */
	FAIL_SPELL,
	/** A singular SPELL, which may be targeted at a block or entity */
	SPELL,
	/** The effects of an ATTACK from one deity to another on the world */
	ATTACK,
	/** The effects of a deity taking damage on the world */
	TAKE_DAMAGE,
	/** The effects of a deity absorbing energy from a dimension to ATTACK */
	ABSORB,
	/** The effects of a deity being killed on its dimensions of ownership */
	NEUTRALIZE,
	/**
	 * The effects of a deity being born/resurrected on the dimension it resurrects
	 * in
	 */
	REVITALIZE,
	/**
	 * A massive SPELL cast by a deity that usually affects the whole world rather
	 * than just a block or entity
	 */
	LEGISLATE,
	/** The manifestation of a deity at a location */
	THEOPHANY,
	/** The effects of a deity putting a palyer in a VISION */
	VISION,
	/** A manifestation of a sign that a deity is about to do something big */
	OMEN,
	/** The effects of a deity angrily trying to destroy the world */
	APOCALYPSE;

	public static final Codec<DeityInteractionType> CODEC = CodecUtils.caselessEnumCodec(DeityInteractionType.class);
}
