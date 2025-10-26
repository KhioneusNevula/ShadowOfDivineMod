package com.gm910.sotdivine.client;

import java.util.Optional;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientPartyLister {

	/**
	 * The party system representation on the client side
	 */
	static Optional<IPartyLister> CLIENT_REPRESENTATION = Optional.empty();

	private ClientPartyLister() {
	}

	public static Optional<IPartyLister> instance() {
		return CLIENT_REPRESENTATION;
	}

}
