package com.gm910.sotdivine.magic.ritual.properties;

import com.gm910.sotdivine.magic.emanation.spell.SpellPower;
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
	MEAGER(0.5f, 2, SpellPower.EASY, 1),
	/**
	 * For rituals that are standard
	 */
	COMMON(1.0f, 6, SpellPower.NORMAL, 4),
	/** For "rarer" than average rituals */
	FINE(2.0f, 18, SpellPower.HARD, 16),

	/**
	 * For a good $quality ritual
	 */
	GOOD(3.0f, 32, SpellPower.POWERFUL, 64),

	/**
	 * For an amazing ritual
	 */
	WONDROUS(4.0f, Integer.MAX_VALUE, SpellPower.WONDROUS, 256);

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

	/**
	 * The number of offerings needed for this qualtiy of ritua
	 */
	public final int offeringsNeeded;

	RitualQuality(float rarity, int maxBlocks, SpellPower power, int numOfferings) {
		this.rarity = rarity;
		maxPatternBlocks = maxBlocks;
		spellPower = power;
		offeringsNeeded = numOfferings;
	}

	public static final Codec<RitualQuality> CODEC = CodecUtils.caselessEnumCodec(RitualQuality.class);
}
