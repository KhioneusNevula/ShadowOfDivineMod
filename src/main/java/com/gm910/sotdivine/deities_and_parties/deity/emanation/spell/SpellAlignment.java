package com.gm910.sotdivine.deities_and_parties.deity.emanation.spell;

import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;

public enum SpellAlignment {

	/** A harmful SPELL */
	CURSE,
	/** A helpful SPELL */
	BLESSING,
	/** A neutral SPELL that isn't helpful or harmful */
	PRAGMA,
	/** A helpful SPELL that can have a dangerous cost */
	CURSED_BLESSING;

	public static final Codec<SpellAlignment> CODEC = CodecUtils.caselessEnumCodec(SpellAlignment.class);

	/**
	 * Returns the SPELL alignment fitting the given booleans, left being whether it
	 * is BLESSING and right being whether it's a CURSE
	 * 
	 * @param benefit
	 * @param malefit
	 * @return
	 */
	public static SpellAlignment from(boolean bless, boolean cursea) {
		if (bless && cursea) {
			return CURSED_BLESSING;
		} else if (bless) {
			return BLESSING;
		} else if (cursea) {
			return CURSE;
		}
		return PRAGMA;
	}

	/**
	 * Whether this SPELL is a BLESSING
	 * 
	 * @return
	 */
	public boolean isBlessing() {
		return this == BLESSING || this == CURSED_BLESSING;
	}

	/**
	 * Return true if this SPELL is a CURSE
	 * 
	 * @return
	 */
	public boolean isCurse() {
		return this == CURSE || this == CURSED_BLESSING;
	}

	/**
	 * Return true if this SPELL is a PRAGMA
	 * 
	 * @return
	 */
	public boolean isPragma() {
		return this == PRAGMA;
	}
}
