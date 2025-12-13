package com.gm910.sotdivine.network.packet_types;

import com.gm910.sotdivine.magic.theophany.cap.IMind;
import com.gm910.sotdivine.util.CodecUtils;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

/**
 * Package representing actions done on client side to impressions
 */
public record ServerboundMeditationPacket(Action action) {
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundMeditationPacket> STREAM_CODEC = StreamCodec
			.composite(CodecUtils.enumStreamCodec(Action.class), ServerboundMeditationPacket::action,
					ServerboundMeditationPacket::new);

	public void handle(Context ctxt) {
		ServerPlayer player = ctxt.getSender();
		ctxt.setPacketHandled(true);
		IMind experiencer = IMind.get(player);
		switch (action) {
		case START_MEDITATION:
			if (experiencer.canMeditate()) {
				experiencer.setMeditating(true);
			}
			break;
		case END_MEDITATION:
			experiencer.setMeditating(false);
			break;
		}
	}

	public static enum Action {
		START_MEDITATION, END_MEDITATION
	}

	public static ServerboundMeditationPacket startMeditating() {
		return new ServerboundMeditationPacket(Action.START_MEDITATION);
	}

	public static ServerboundMeditationPacket stopMeditating() {
		return new ServerboundMeditationPacket(Action.END_MEDITATION);
	}
}
