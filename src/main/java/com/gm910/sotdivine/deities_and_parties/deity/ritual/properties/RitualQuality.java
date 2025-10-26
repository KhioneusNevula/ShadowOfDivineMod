package com.gm910.sotdivine.deities_and_parties.deity.ritual.properties;

import com.gm910.sotdivine.deities_and_parties.deity.emanation.spell.SpellPower;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;

import net.minecraft.world.item.Rarity;

/**
 * The level of a ritual
 */
public enum RitualQuality {

	/**
	 * For rituals that are done at a bare minimum
	 */
	MEAGER(0.9f, 2, SpellPower.EASY),
	/**
	 * For rituals that are standard
	 */
	COMMON(1.0f, 6, SpellPower.NORMAL),
	/** For "rarer" than average rituals */
	FINE(2.0f, 18, SpellPower.HARD),

	/**
	 * For a good quality ritual
	 */
	GOOD(3.0f, 32, SpellPower.POWERFUL),

	/**
	 * For an amazing ritual
	 */
	WONDROUS(4.0f, Integer.MAX_VALUE, SpellPower.WONDROUS);

	/**
	 * Return a number 1-4 representing the maximum rarity allowed
	 */
	public final float rarity;
	/**
	 * Max number of blocks that can be in the pattern
	 */
	public final int maxPatternBlocks;
	/**
	 * The power required of the spell effect for this ritual
	 */
	public final SpellPower spellPower;

	RitualQuality(float f, int maxBlocks, SpellPower power) {
		rarity = f;
		maxPatternBlocks = maxBlocks;
		spellPower = power;
	}

	public static final Codec<RitualQuality> CODEC = CodecUtils.caselessEnumCodec(RitualQuality.class);
}
