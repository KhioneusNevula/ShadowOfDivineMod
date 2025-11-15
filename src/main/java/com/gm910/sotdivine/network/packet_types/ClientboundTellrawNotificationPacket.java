package com.gm910.sotdivine.network.packet_types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.magic.sphere.Spheres;
import com.gm910.sotdivine.util.FieldUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

/**
 * Packet to indicate that the party system has to be updated
 * 
 * @author borah
 *
 */
public record ClientboundTellrawNotificationPacket(Component message, EntityReference<Entity> source) {

	private static final Multimap<Component, ServerboundIncantationChatPacket> dummyPackets = MultimapBuilder.hashKeys()
			.hashSetValues().build();

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTellrawNotificationPacket> STREAM_CODEC = StreamCodec
			.composite(ComponentSerialization.STREAM_CODEC, ClientboundTellrawNotificationPacket::message,
					EntityReference.streamCodec(), ClientboundTellrawNotificationPacket::source,
					ClientboundTellrawNotificationPacket::new); /*new StreamCodec<>() {
																
																@Override
																public ClientboundTellrawNotificationPacket decode(RegistryFriendlyByteBuf buf) {
																return new ClientboundTellrawNotificationPacket(ComponentSerialization.STREAM_CODEC.decode(buf),
																EntityReference.<Entity>streamCodec().decode(buf));
																}
																
																@Override
																public void encode(RegistryFriendlyByteBuf buf, ClientboundTellrawNotificationPacket msg) {
																ComponentSerialization.STREAM_CODEC.encode(buf, msg.message);
																EntityReference.<Entity>streamCodec().encode(buf, msg.source);
																}
																
																};*/

	public static void clearAllPackets() {
		dummyPackets.clear();
	}

	/**
	 * All packets which have accumulated on the server side
	 * 
	 * @return
	 */
	public static Collection<ServerboundIncantationChatPacket> getQueuedPackets() {
		return Collections.unmodifiableCollection(dummyPackets.values());
	}

	/**
	 * Removes and returns the packets which are associated with the given message
	 * 
	 * @return
	 */
	public static Collection<ServerboundIncantationChatPacket> popPackets(Component msg) {
		return dummyPackets.removeAll(msg);
	}

	/**
	 * Handle party system client side update
	 * 
	 * @param x
	 * @param y
	 */
	public static void handle(ClientboundTellrawNotificationPacket x, Context y) {
		y.setPacketHandled(true);
		String message = x.message.getString();
		Either<BlockPos, EntityReference<Entity>> either;
		if (Minecraft.getInstance().hitResult instanceof EntityHitResult enhit) {
			either = Either.right(new EntityReference<Entity>(enhit.getEntity()));
		} else {
			either = Either.left(((BlockHitResult) Minecraft.getInstance().hitResult).getBlockPos());
		}
		Spheres.instance().getSphereMap().values().stream().flatMap((s) -> s.getGenres(GenreTypes.MAGIC_WORD).stream())
				.forEach((m) -> {
					List<Integer> searchSequence = TextUtils.searchForStringIgnoreCase(m.translation().getString(),
							x.message);
					if (searchSequence == null)
						return;
					Component containing = TextUtils.getComponent(x.message, searchSequence);
					LogUtils.getLogger().debug("Detected word " + m.translation().getString() + " (" + m.translation()
							+ ") in message \"" + message + "\" (" + x.message + ") at component "
							+ containing.getString() + " (" + containing + ")" + " with sequence " + searchSequence);
					Component containingCopy = containing.copy();
					FieldUtils.setInstanceField("siblings", "d", containingCopy, new ArrayList<>());
					dummyPackets.put(x.message,
							new ServerboundIncantationChatPacket(x.message, containingCopy.getString(), searchSequence,
									m.translation(), m.translation().getString(), either, false,
									Optional.ofNullable(x.source).map(u -> new EntityReference<Player>(u.getUUID()))));
				});

	}

}
