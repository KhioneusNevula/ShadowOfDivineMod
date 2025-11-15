package com.gm910.sotdivine.network.packet_types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Either;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

/**
 * Packet to indicate that the party system has to be updated
 * 
 * @author borah
 *
 */
public record ServerboundIncantationChatPacket(Component originalMessage, String componentAsString,
		List<Integer> accessSequence, Component magicWord, String wordAsString,
		Either<BlockPos, EntityReference<Entity>> lookAt, boolean needsParsing,
		Optional<EntityReference<Player>> sender) {

	private static final Multimap<Component, ServerboundIncantationChatPacket> queuedPackets = MultimapBuilder
			.hashKeys().hashSetValues().build();

	private static final List<ServerboundIncantationChatPacket> queuedParsedPackets = new ArrayList<>();

	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundIncantationChatPacket> STREAM_CODEC = StreamCodec
			.composite(ComponentSerialization.STREAM_CODEC, ServerboundIncantationChatPacket::originalMessage,
					ByteBufCodecs.STRING_UTF8, ServerboundIncantationChatPacket::componentAsString,
					ByteBufCodecs.<ByteBuf, Integer>list().apply(ByteBufCodecs.INT),
					ServerboundIncantationChatPacket::accessSequence, ComponentSerialization.STREAM_CODEC,
					ServerboundIncantationChatPacket::magicWord, ByteBufCodecs.STRING_UTF8,
					ServerboundIncantationChatPacket::wordAsString,
					ByteBufCodecs.either(BlockPos.STREAM_CODEC, EntityReference.streamCodec()),
					ServerboundIncantationChatPacket::lookAt, ByteBufCodecs.BOOL,
					ServerboundIncantationChatPacket::needsParsing,
					ByteBufCodecs.optional(EntityReference.streamCodec()), ServerboundIncantationChatPacket::sender,
					ServerboundIncantationChatPacket::new);
	/*
		public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundIncantationChatPacket> STREAM_CODEC1 = new StreamCodec<RegistryFriendlyByteBuf, ServerboundIncantationChatPacket>() {
	
			@Override
			public void encode(RegistryFriendlyByteBuf buf, ServerboundIncantationChatPacket packet) {
				ComponentSerialization.STREAM_CODEC.encode(buf, packet.magicWord);
				packet.lookAt.ifLeft((pos) -> {
					buf.writeBoolean(true);
					BlockPos.STREAM_CODEC.encode(buf, pos);
				}).ifRight((en) -> {
					buf.writeBoolean(false);
					EntityReference.<Entity>streamCodec().encode(buf, en);
				});
				ComponentSerialization.STREAM_CODEC.encode(buf, packet.originalMessage);
				buf.writeJsonWithCodec(Codec.STRING, packet.wordAsString);
				buf.writeJsonWithCodec(Codec.STRING, packet.componentAsString);
				buf.writeVarIntArray(packet.accessSequence.stream().mapToInt((s) -> s).toArray());
				buf.writeBoolean(packet.needsParsing);
				buf.writeBoolean(packet.sender.isPresent());
				if (packet.sender.isPresent())
					EntityReference.<Player>streamCodec().encode(buf, packet.sender.get());
			}
	
			@Override
			public ServerboundIncantationChatPacket decode(RegistryFriendlyByteBuf buf) {
				var cm = ComponentSerialization.STREAM_CODEC.decode(buf);
				boolean isBlockPos = buf.readBoolean();
				Either<BlockPos, EntityReference<Entity>> vc;
				if (isBlockPos) {
					vc = Either.left(BlockPos.STREAM_CODEC.decode(buf));
				} else {
					vc = Either.right(EntityReference.<Entity>streamCodec().decode(buf));
				}
				var ogmsg = ComponentSerialization.STREAM_CODEC.decode(buf);
				var was = buf.readLenientJsonWithCodec(Codec.STRING);
				var cas = buf.readLenientJsonWithCodec(Codec.STRING);
				var intArray = buf.readVarIntArray();
				var np = buf.readBoolean();
				boolean issender = buf.readBoolean();
				Optional<EntityReference<Player>> sender = Optional.empty();
				if (issender) {
					sender = Optional.of(EntityReference.<Player>streamCodec().decode(buf));
				}
				return new ServerboundIncantationChatPacket(ogmsg, cas, Arrays.stream(intArray).mapToObj((i) -> i).toList(),
						cm, was, vc, np, sender);
			}
		};*/

	public ServerboundIncantationChatPacket(Component originalMsg, String componentAsString,
			List<Integer> accessSequence, Component chatElement, String componentForm,
			Either<BlockPos, EntityReference<Entity>> lookAt, boolean needsParsing) {
		this(originalMsg, componentAsString, accessSequence, chatElement, componentForm, lookAt, needsParsing,
				Optional.empty());
	}

	public Component magicWord() {
		return magicWord;
	}

	public Component originalMessage() {
		return originalMessage;
	}

	/**
	 * Returns the precise string that was matched to the word
	 * 
	 * @return
	 */
	public String wordAsString() {
		return wordAsString;
	}

	/**
	 * Returns the matched string of the component containing the word which was
	 * matched
	 * 
	 * @return
	 */
	public String componentAsString() {
		return componentAsString;
	}

	/**
	 * Returns the block position or entity being looked at by the sender
	 * 
	 * @return
	 */
	public Either<BlockPos, EntityReference<Entity>> lookAt() {
		return lookAt;
	}

	/**
	 * Returns the list of indices required to get to the component containing the
	 * magic word
	 * 
	 * @return
	 */
	public List<Integer> accessSequence() {
		return Collections.unmodifiableList(accessSequence);
	}

	/**
	 * Returns whether this message "needs to be parsed", i.e. whether it needs to
	 * be color-changed and such, or whether this has already been done
	 * 
	 * @return
	 */
	public boolean needsParsing() {
		return needsParsing;
	}

	public static void clearAllPackets() {
		queuedPackets.clear();
		queuedParsedPackets.clear();
	}

	/**
	 * All packets which have accumulated on the server side and are not
	 * color=changed
	 * 
	 * @return
	 */
	public static Collection<ServerboundIncantationChatPacket> getQueuedUnparsedPackets() {
		return Collections.unmodifiableCollection(queuedPackets.values());
	}

	/**
	 * All packets which have accumulated on the server side and are already
	 * color=changed
	 * 
	 * @return
	 */
	public static Collection<ServerboundIncantationChatPacket> getQueuedParsedPackets() {
		return Collections.unmodifiableCollection(queuedParsedPackets);
	}

	/**
	 * Removes and returns the packet which is associated with the given message
	 * 
	 * @return
	 */
	public static Collection<ServerboundIncantationChatPacket> popUnparsedPackets(Component msg) {
		return queuedPackets.removeAll(msg);
	}

	/**
	 * Removes and returns the packet that has been color-parsed
	 * 
	 * @return
	 */
	public static Optional<ServerboundIncantationChatPacket> popParsedPacket() {
		return queuedParsedPackets.isEmpty() ? Optional.empty() : Optional.of(queuedParsedPackets.removeFirst());
	}

	/**
	 * Handle party system client side update
	 * 
	 * @param x
	 * @param y
	 */
	public static void handle(ServerboundIncantationChatPacket x, Context y) {
		y.setPacketHandled(true);
		var packet2 = new ServerboundIncantationChatPacket(x.originalMessage, x.componentAsString, x.accessSequence,
				x.magicWord, x.wordAsString, x.lookAt, x.needsParsing,
				Optional.ofNullable(y.getSender()).map((e) -> new EntityReference<>(e)));
		if (packet2.needsParsing) {
			queuedPackets.put(x.originalMessage, packet2);
		} else {
			queuedParsedPackets.add(packet2);
		}
	}

}
