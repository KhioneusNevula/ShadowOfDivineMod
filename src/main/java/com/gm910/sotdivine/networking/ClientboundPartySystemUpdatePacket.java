package com.gm910.sotdivine.networking;

import java.util.Optional;

import com.gm910.sotdivine.systems.party_system.IPartySystem;

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

	public final IPartySystem system;
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPartySystemUpdatePacket> STREAM_CODEC = new StreamCodec<>() {

		@Override
		public ClientboundPartySystemUpdatePacket decode(RegistryFriendlyByteBuf buf) {
			IPartySystem system = buf.readLenientJsonWithCodec(IPartySystem.CODEC);
			return new ClientboundPartySystemUpdatePacket(system);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, ClientboundPartySystemUpdatePacket msg) {
			buf.writeJsonWithCodec(IPartySystem.CODEC, msg.system);

		}

	};

	public ClientboundPartySystemUpdatePacket(IPartySystem sys) {
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
		PartySystemClient.CLIENT_REPRESENTATION = Optional.of(x.system);
	}

}
