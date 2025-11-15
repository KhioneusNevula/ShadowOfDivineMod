package com.gm910.sotdivine.network.party_system;

import java.util.Optional;

import com.gm910.sotdivine.concepts.parties.IPartyLister;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Retrieve party info on the client side
 */
@OnlyIn(Dist.CLIENT)
public class ClientParties {

	/**
	 * The party system representation on the client side
	 */
	static Optional<IPartyLister> CLIENT_REPRESENTATION = Optional.empty();

	private ClientParties() {
	}

	public static Optional<IPartyLister> instance() {
		return CLIENT_REPRESENTATION;
	}

}
