package com.gm910.sotdivine.network.packet_types;

import com.gm910.sotdivine.events.ClientEvents;
import com.gm910.sotdivine.util.CodecUtils;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

/**
 * Package representing actions done on client side to impressions
 */
public record ClientboundMeditationPacket(Action action) {
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMeditationPacket> STREAM_CODEC = StreamCodec
			.composite(CodecUtils.enumStreamCodec(Action.class), ClientboundMeditationPacket::action,
					ClientboundMeditationPacket::new);

	public void handle(Context ctxt) {
		ctxt.setPacketHandled(true);
		switch (action) {
		case START_MEDITATION:
			ClientEvents.startMeditatingAndChangeScreen(false);
			break;
		case END_MEDITATION:
			ClientEvents.stopMeditatingAndChangeScreen();
			break;
		}
	}

	public static enum Action {
		START_MEDITATION, END_MEDITATION
	}

	public static ClientboundMeditationPacket startMeditating() {
		return new ClientboundMeditationPacket(Action.START_MEDITATION);
	}

	public static ClientboundMeditationPacket stopMeditating() {
		return new ClientboundMeditationPacket(Action.END_MEDITATION);
	}
}
