package com.gm910.sotdivine.network.packet_types;

import java.util.List;
import java.util.Optional;

import com.gm910.sotdivine.magic.theophany.cap.IMind;
import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionHolder;
import com.gm910.sotdivine.util.CodecUtils;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

/**
 * Package representing actions done on client side to impressions
 */
public record ServerboundImpressionsUpdatePacket(Action action, Optional<ImpressionHolder> holder) {
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundImpressionsUpdatePacket> STREAM_CODEC = StreamCodec
			.composite(CodecUtils.enumStreamCodec(Action.class), ServerboundImpressionsUpdatePacket::action,
					ByteBufCodecs.optional(ImpressionHolder.STREAM_CODEC), ServerboundImpressionsUpdatePacket::holder,
					ServerboundImpressionsUpdatePacket::new);

	public void handle(Context ctxt) {
		ServerPlayer player = ctxt.getSender();
		ctxt.setPacketHandled(true);
		IMind experiencer = IMind.get(player);
		experiencer.updateServer(this);
	}

	public static enum Action {
		ACTIVATE, REMOVE
	}

	public static ServerboundImpressionsUpdatePacket activate(ImpressionHolder holder) {
		return new ServerboundImpressionsUpdatePacket(Action.ACTIVATE, Optional.of(holder));
	}

	public static ServerboundImpressionsUpdatePacket remove(IImpression impression) {
		return new ServerboundImpressionsUpdatePacket(Action.REMOVE, Optional.of(new ImpressionHolder(impression)));
	}
}
