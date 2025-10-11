package com.gm910.sotdivine.systems.deity.emanation;

/**
 * A way a deity interacts with the world
 */
public enum DeityInteractionType {
	/**
	 * An emanation that occurs when a deity accepts an offering. By nature, this
	 * targets the block where the offering was made
	 */
	accept_offering,
	/**
	 * An emanation that occurs when a deity finds one of its symbols during an
	 * offering or something similar. Targets the entity with the symbol or the
	 * position of the block it is at
	 */
	symbol_recognition,
	/** A singular spell, which may be targeted at a block or entity */
	spell,
	/** The effects of an attack from one deity to another on the world */
	attack,
	/** The effects of a deity taking damage on the world */
	take_damage,
	/** The effects of a deity absorbing energy from a dimension to attack */
	absorb,
	/** The effects of a deity being killed on its dimensions of ownership */
	neutralize,
	/**
	 * The effects of a deity being born/resurrected on the dimension it resurrects
	 * in
	 */
	revitalize,
	/**
	 * A massive spell cast by a deity that usually affects the whole world rather
	 * than just a block or entity
	 */
	legislate,
	/** The manifestation of a deity at a location */
	theophany,
	/** The effects of a deity putting a palyer in a vision */
	vision,
	/** A manifestation of a sign that a deity is about to do something big */
	omen,
	/** The effects of a deity angrily trying to destroy the world */
	apocalypse
}
