package com.gm910.sotdivine.network.party_system;

import java.util.Optional;

import com.gm910.sotdivine.concepts.parties.IPartyLister;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

/**
 * Packet to indicate that the party system has to be updated
 * 
 * @author borah
 *
 */
public class ClientboundPartySystemUpdatePacket {

	public final IPartyLister system;
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPartySystemUpdatePacket> STREAM_CODEC = StreamCodec
			.composite(IPartyLister.STREAM_CODEC, ClientboundPartySystemUpdatePacket::system,
					ClientboundPartySystemUpdatePacket::new);

	public IPartyLister system() {
		return system;
	}

	public ClientboundPartySystemUpdatePacket(IPartyLister sys) {
		this.system = sys;
	}

	/**
	 * Handle party system client side update
	 * 
	 * @param x
	 * @param y
	 */
	public static void handle(ClientboundPartySystemUpdatePacket x, Context y) {
		y.setPacketHandled(true);
		ClientParties.CLIENT_REPRESENTATION = Optional.of(x.system);
	}

}
