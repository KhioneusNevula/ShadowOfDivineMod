package com.gm910.sotdivine.client;

import java.util.Optional;

import com.gm910.sotdivine.deities_and_parties.system_storage.IPartySystem;

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
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPartySystemUpdatePacket> STREAM_CODEC = new StreamCodec<>() {

		@Override
		public ClientboundPartySystemUpdatePacket decode(RegistryFriendlyByteBuf buf) {
			IPartyLister system = buf.readLenientJsonWithCodec(IPartyLister.CODEC);
			return new ClientboundPartySystemUpdatePacket(system);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, ClientboundPartySystemUpdatePacket msg) {
			buf.writeJsonWithCodec(IPartyLister.CODEC, msg.system);

		}

	};

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
		ClientPartyLister.CLIENT_REPRESENTATION = Optional.of(x.system);
	}

}
