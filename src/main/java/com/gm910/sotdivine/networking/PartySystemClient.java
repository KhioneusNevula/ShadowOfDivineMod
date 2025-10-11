package com.gm910.sotdivine.networking;

import java.util.Optional;

import com.gm910.sotdivine.systems.party_system.IPartySystem;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

@OnlyIn(Dist.CLIENT)
public class PartySystemClient {

	/**
	 * The party system representation on the client side
	 */
	static Optional<IPartySystem> CLIENT_REPRESENTATION = Optional.empty();

	private PartySystemClient() {
	}

	public static Optional<IPartySystem> instance() {
		return CLIENT_REPRESENTATION;
	}

}
