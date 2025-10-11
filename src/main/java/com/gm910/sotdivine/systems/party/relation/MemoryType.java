package com.gm910.sotdivine.systems.party.relation;

public enum MemoryType {
	/** Memory of a deity dying, with an optional killer */
	DIED,
	/** Memory of making an offering */
	OFFERING,
	/** Memory of a transgression */
	TRANSGRESSION,
	/** Memory of claiming of a dimension */
	DIMENSION_THEFT,
	/** Memory of desecration of a temple */
	DESECRATION,
	/** Memory of construction of a temple */
	CONSECRATION,
	/** Memory of being attacked */
	ATTACKED,
	/**
	 * Memory of another entity being attacked (e.g. a follower or something
	 * similar)
	 */
	ATTACKED_OTHER,
	/** Memory of being protected */
	PROTECTED,
	/** Memory of another entity being protected (e.g. a follower or something) */
	PROTECTED_OTHER;

}
