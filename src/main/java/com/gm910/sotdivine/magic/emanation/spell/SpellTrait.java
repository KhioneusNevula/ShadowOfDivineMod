package com.gm910.sotdivine.magic.emanation.spell;

import net.minecraftforge.common.IExtensibleEnum;

/**
 * SpellTraits are side-effects or general mystical happenings that occur in the
 * vicinity of a SPELL being used
 */
public enum SpellTrait implements IExtensibleEnum {
	/**
	 * Whether this SPELL changes all nearby banners to only show the deity's symbol
	 */
	BANNER_FLIP,
	/** Whether this SPELL protects an entity/rawPosition from other deities' spells */
	PROTECTION,
	/** Whether this SPELL absorbs energy for the deity */
	ABSORB;

	public static SpellTrait create(String name) {
		throw new IllegalStateException("Enum not extended");
	}
}
