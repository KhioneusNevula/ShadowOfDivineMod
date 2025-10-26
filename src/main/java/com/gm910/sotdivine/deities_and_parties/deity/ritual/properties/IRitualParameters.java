package com.gm910.sotdivine.deities_and_parties.deity.ritual.properties;

/**
 * A bundle of parameters
 */
public interface IRitualParameters {
	/**
	 * Get the value of a parameter
	 * 
	 * @param parameter
	 * @return
	 */
	public float get(RitualParameter parameter);

	/**
	 * Return the intensity from the combined parameters
	 * 
	 * @return
	 */
	public float aggregateIntensity();
}
