package com.gm910.sotdivine.language.phonology.rules;

import com.gm910.sotdivine.language.phonology.phonemes.PhoneSet;

public enum RuleType {
	/**
	 * Adds an element specified in the {@link PhoneSet} in the "element" field
	 * right before this one
	 */
	INSERT(false, true),
	/**
	 * Delete the element at the rawPosition. There is only one ruleOrString of this
	 * kind, and it is universally available
	 */
	DELETE(false, true),
	/**
	 * Change an element at the given rawPosition to an item from the {@link PhoneSet}
	 * specified in the "element" field
	 */
	CHANGE(false, false),
	/**
	 * Move an element at the given rawPosition to the rawPosition specified in the
	 * "destination" field
	 */
	MOVE(false, true),
	/**
	 * Inverts the values of the features (list) in the "features" field; if the
	 * feature is a number, negativize it; otherwise, throw an exception
	 */
	INVERT_FEATURE(true, false),
	/** Sets the features to the specified values (map) in the "features" field */
	SET_FEATURE(true, false),
	/** Removes the features (list) in the "features" field */
	DELETE_FEATURE(true, false),
	/** Do nothing */
	NOTHING(false, false);

	public final boolean featural;
	public final boolean changeStructure;

	private RuleType(boolean f, boolean cs) {
		featural = f;
		changeStructure = cs;
	}
}
