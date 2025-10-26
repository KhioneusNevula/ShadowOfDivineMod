package com.gm910.sotdivine.deities_and_parties.deity.ritual.trigger;

/**
 * Trigger the ritual when any of its offerings are dropped
 * 
 * @return
 */
public enum OfferItemTrigger implements IRitualTrigger {
	INSTANCE;

	@Override
	public RitualTriggerType<?> triggerType() {
		return RitualTriggerType.OFFER_ITEM;
	}

	@Override
	public String toString() {
		return "OfferItem";
	}

}
