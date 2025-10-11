package com.gm910.sotdivine.systems.deity.emanation.spell;

public enum SpellAlignment {

	/** A harmful spell */
	curse,
	/** A helpful spell */
	blessing,
	/** A neutral spell that isn't helpful or harmful */
	pragma,
	/** A helpful spell that can have a dangerous cost */
	cursed_blessing;

	/**
	 * Returns the spell alignment fitting the given booleans, left being whether it
	 * is blessing and right being whether it's a curse
	 * 
	 * @param benefit
	 * @param malefit
	 * @return
	 */
	public static SpellAlignment from(boolean bless, boolean cursea) {
		if (bless && cursea) {
			return cursed_blessing;
		} else if (bless) {
			return blessing;
		} else if (cursea) {
			return curse;
		}
		return pragma;
	}

	/**
	 * Whether this spell is a blessing
	 * 
	 * @return
	 */
	public boolean isBlessing() {
		return this == blessing || this == cursed_blessing;
	}

	/**
	 * Return true if this spell is a curse
	 * 
	 * @return
	 */
	public boolean isCurse() {
		return this == curse || this == cursed_blessing;
	}

	/**
	 * Return true if this spell is a pragma
	 * 
	 * @return
	 */
	public boolean isPragma() {
		return this == pragma;
	}
}
