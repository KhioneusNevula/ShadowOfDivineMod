package com.gm910.sotdivine.common.command.args.party;

/**
 * Deity argument
 */
public class DeityArgument extends AbstractPartyArgument {

	public DeityArgument() {
		super(true);
	}

	public static DeityArgument argument() {
		return new DeityArgument();
	}
}
