package com.gm910.sotdivine.magic.theophany.impression;

import net.minecraftforge.common.IExtensibleEnum;

public enum MentalState implements IExtensibleEnum {
	/**
	 * Wakeful state; impressions are basically impossible to witness here and only
	 * noticeable as particles or some general vibe
	 */
	AWAKE,
	/**
	 * Sleeping or meditating; impressions will be visible but hard to interact with
	 */
	ASLEEP,
	/** Meditating state; impressions will be visible and easy to interact with. */
	MEDITATING,
	/** Witnessing state; this is for impressions that work as visions */
	WITNESSING;

	public static MentalState create(String name) {
		throw new IllegalStateException("Enum not extended");
	}
}
