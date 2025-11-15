package com.gm910.sotdivine.concepts.parties.party.relation;

/**
 * The different stats constituting a deity's relation with a party
 * 
 * @author borah
 *
 */
public enum RelationStat {
	/**
	 * High values indicate the deity is more likely to try and protect the party if
	 * in danger
	 */
	CARE,
	/**
	 * High values indicate the deity is more likely to try and actively destroy the
	 * party
	 */
	HATRED,
	/**
	 * High values indicate the deity is more likely to punish the party; decreases
	 * when the punishment is given
	 */
	DISAPPROVAL(2),
	/**
	 * High values indicate the deity is more likely to grant requests from the
	 * party
	 */
	FAVOR(2);

	private int kind;

	private RelationStat() {

	}

	private RelationStat(int kind) {
		this.kind = kind;
	}

	/**
	 * Whether the stat is only allowed with other deities, i.e. kind=1
	 * 
	 * @return
	 */
	public boolean onlyWithDeities() {
		return kind % 2 != 0;
	}

	/**
	 * Whether the stat is only allwoed with worshipers, i.e. kind = 2
	 * 
	 * @return
	 */
	public boolean onlyWithWorshipers() {
		return kind != 0 && kind % 2 == 0;
	}

	/**
	 * Equivalent to !{@link #onlyWithDeities()} && !{@link #onlyWithWorshipers()}
	 * 
	 * @return
	 */
	public boolean allowedWithAll() {
		return kind == 0;
	}

}
