package com.gm910.sotdivine.deities_and_parties.deity.emanation.spell;

/**
 * The complexity of this spell to cast; 0 - 4
 */
public enum SpellPower {
	/** For super simple spells (e.g. making food) */
	EASY,
	/** For normal-level spells (e.g. creating common items, minor damage) */
	NORMAL,
	/** A hard spell (e.g. summoning regular mobs, major damage) */
	HARD,
	/**
	 * A powerful spell (e.g. summoning poweful mobs, causing lots of damage to many
	 * mobs)
	 */
	POWERFUL,
	/** A spell capable of changing properties of the world */
	WONDROUS,
	/** For things which cannot be made into spells */
	IMPOSSIBLE;

}
