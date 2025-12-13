package com.gm910.sotdivine.network;

import java.util.List;

import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.magic.theophany.client.ImpressionsClient;
import com.gm910.sotdivine.network.packet_types.ClientboundImpressionsUpdatePacket;
import com.gm910.sotdivine.network.packet_types.ClientboundMeditationPacket;
import com.gm910.sotdivine.network.packet_types.ClientboundTellrawNotificationPacket;
import com.gm910.sotdivine.network.packet_types.ServerboundImpressionsUpdatePacket;
import com.gm910.sotdivine.network.packet_types.ServerboundIncantationChatPacket;
import com.gm910.sotdivine.network.packet_types.ServerboundMeditationPacket;
import com.gm910.sotdivine.network.party_system.ClientboundPartySystemUpdatePacket;
import com.gm910.sotdivine.util.ModUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public class ModNetwork {

	public static final SimpleChannel INSTANCE = ChannelBuilder.named(ModUtils.path("main")).simpleChannel().play()
			.clientbound()
			.add(ClientboundPartySystemUpdatePacket.class, ClientboundPartySystemUpdatePacket.STREAM_CODEC,
					ClientboundPartySystemUpdatePacket::handle)
			.add(ClientboundTellrawNotificationPacket.class, ClientboundTellrawNotificationPacket.STREAM_CODEC,
					ClientboundTellrawNotificationPacket::handle)
			.add(ClientboundImpressionsUpdatePacket.class, ClientboundImpressionsUpdatePacket.STREAM_CODEC,
					ImpressionsClient::handlePackageFromServer)
			.add(ClientboundMeditationPacket.class, ClientboundMeditationPacket.STREAM_CODEC,
					ClientboundMeditationPacket::handle)
			.serverbound()
			.add(ServerboundIncantationChatPacket.class, ServerboundIncantationChatPacket.STREAM_CODEC,
					ServerboundIncantationChatPacket::handle)
			.add(ServerboundImpressionsUpdatePacket.class, ServerboundImpressionsUpdatePacket.STREAM_CODEC,
					ServerboundImpressionsUpdatePacket::handle)
			.add(ServerboundMeditationPacket.class, ServerboundMeditationPacket.STREAM_CODEC,
					ServerboundMeditationPacket::handle)
			.build();

	private ModNetwork() {
	}

	/**
	 * Request party system update
	 * 
	 * @param system
	 * @param connection
	 */
	public static void sendPartySystemToClients(IPartySystem system, MinecraftServer server) {
		sendToClients(new ClientboundPartySystemUpdatePacket(system), server);
	}

	/**
	 * Send an incantation chat notification for a block looked at
	 * 
	 * @param system
	 * @param connection
	 */
	@OnlyIn(Dist.CLIENT)
	public static void sendIncantationToServer(Component originalMessage, String componentAsString,
			List<Integer> accessSequence, Component magicWord, String wordAsString,
			Either<BlockPos, EntityReference<Entity>> look, boolean needsParsing) {
		sendToServer(new ServerboundIncantationChatPacket(originalMessage, componentAsString, accessSequence, magicWord,
				wordAsString, look, needsParsing));
	}

	/**
	 * Notifies client(s) that a tellraw command is being used
	 * 
	 * @param c
	 * @param source
	 * @param server
	 */
	public static void sendTellrawToClients(Component c, Entity source, Iterable<ServerPlayer> players) {
		sendToClients(new ClientboundTellrawNotificationPacket(c, new EntityReference<Entity>(source)), players);
	}

	public static void send(Object msg, Connection connection) {
		INSTANCE.send(msg, connection);
	}

	/**
	 * Send a message to the server from the client
	 * 
	 * @param msg
	 */
	@OnlyIn(Dist.CLIENT)
	public static void sendToServer(Object msg) {
		INSTANCE.send(msg, Minecraft.getInstance().getConnection().getConnection());
	}

	/**
	 * Send a message to a specific player
	 * 
	 * @param msg
	 * @param server
	 */
	public static void sendToClient(Object msg, ServerPlayer player) {
		INSTANCE.send(msg, player.connection.getConnection());
	}

	/**
	 * Send a message to all player clients
	 * 
	 * @param msg
	 * @param server
	 */
	public static void sendToClients(Object msg, MinecraftServer server) {
		server.getPlayerList().getPlayers().forEach((player) -> {
			INSTANCE.send(msg, player.connection.getConnection());
		});
	}

	/**
	 * Send a message to all given player clients
	 * 
	 * @param msg
	 * @param server
	 */
	public static void sendToClients(Object msg, Iterable<ServerPlayer> players) {
		players.forEach((player) -> {
			INSTANCE.send(msg, player.connection.getConnection());
		});
	}

	public static void init() {
		LogUtils.getLogger().debug("Initializing network...");
	}

}
