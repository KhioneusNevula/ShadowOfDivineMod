package com.gm910.sotdivine.networking;

import com.gm910.sotdivine.systems.party_system.IPartySystem;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.network.Connection;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public class ModNetwork {

	public static final SimpleChannel INSTANCE = ChannelBuilder.named(ModUtils.path("main")).simpleChannel().play()
			.clientbound().add(ClientboundPartySystemUpdatePacket.class,
					ClientboundPartySystemUpdatePacket.STREAM_CODEC, ClientboundPartySystemUpdatePacket::handle)
			.build();

	private ModNetwork() {
	}

	/**
	 * Request party system update
	 * 
	 * @param system
	 * @param connection
	 */
	public static void requestUpdate(IPartySystem system, Connection connection) {
		INSTANCE.send(new ClientboundPartySystemUpdatePacket(system), connection);
	}

	public static void send(Object msg, Connection connection) {
		INSTANCE.send(msg, connection);
	}

	public static void init() {
		System.out.println("Initializing network...");
	}

}
