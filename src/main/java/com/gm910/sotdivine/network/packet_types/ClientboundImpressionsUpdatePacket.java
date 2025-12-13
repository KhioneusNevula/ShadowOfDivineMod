package com.gm910.sotdivine.network.packet_types;

import java.util.Optional;

import com.gm910.sotdivine.magic.theophany.cap.ImpressionTimetracker;
import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionType;
import com.gm910.sotdivine.util.CodecUtils;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Package representing actions done on client side to impressions
 */
public record ClientboundImpressionsUpdatePacket(Action action, Optional<IImpression> impression,
		Optional<ImpressionTimetracker> additionalInfo) {

	public static ClientboundImpressionsUpdatePacket add(IImpression impression, ImpressionTimetracker timet) {
		return new ClientboundImpressionsUpdatePacket(Action.ADD, Optional.of(impression), Optional.of(timet));
	}

	public static ClientboundImpressionsUpdatePacket remove(IImpression imp) {
		return new ClientboundImpressionsUpdatePacket(Action.REMOVE, Optional.of(imp), Optional.empty());
	}

	public static ClientboundImpressionsUpdatePacket clear() {
		return new ClientboundImpressionsUpdatePacket(Action.CLEAR, Optional.empty(), Optional.empty());
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundImpressionsUpdatePacket> STREAM_CODEC = StreamCodec
			.composite(CodecUtils.enumStreamCodec(Action.class), ClientboundImpressionsUpdatePacket::action,
					ByteBufCodecs.optional(ImpressionType.resourceStreamCodec()),
					ClientboundImpressionsUpdatePacket::impression,
					ByteBufCodecs.optional(ImpressionTimetracker.STREAM_CODEC),
					ClientboundImpressionsUpdatePacket::additionalInfo, ClientboundImpressionsUpdatePacket::new);

	public static enum Action {
		ADD, REMOVE, CLEAR
	}

}
