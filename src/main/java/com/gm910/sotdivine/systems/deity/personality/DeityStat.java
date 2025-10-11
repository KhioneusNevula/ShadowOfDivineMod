package com.gm910.sotdivine.systems.deity.personality;

import com.gm910.sotdivine.util.ModUtils;

public enum DeityStat implements IDeityStat {
	/** Higher chance of granting follower requests */
	GENEROSITY,
	/** Higher chance of punishing followers for more minor transgressions */
	PUNITY,
	/** Higher chance of attacking other deities */
	CHAOTICNESS,
	/** Higher chance of trying to take another deity's dimension */
	COVETOUSNESS,
	/**
	 * Higher chance of going after a deity or even individual that has wronged it
	 */
	VINDICTIVENESS,
	/** Higher chance of communicating and manifesting a theophany */
	PERSONABILITY,
	/**
	 * Higher chance of taking independent actions; if 0, the deity will only act in
	 * response to players
	 */
	AUTONOMY,
	/**
	 * How much damage this deity can take before being neutralized
	 */
	HEALTH;

	private DeityStat() {
		IDeityStat.register(ModUtils.path("deity_stat_" + this.name().toLowerCase()), this);

	}

}
