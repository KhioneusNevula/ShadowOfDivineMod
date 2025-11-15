package com.gm910.sotdivine.common.command.args.party;

/**
 * Party argument
 */
public class PartyArgument extends AbstractPartyArgument {

	public PartyArgument() {
		super(false);
	}

	public static PartyArgument argument() {
		return new PartyArgument();
	}
}
