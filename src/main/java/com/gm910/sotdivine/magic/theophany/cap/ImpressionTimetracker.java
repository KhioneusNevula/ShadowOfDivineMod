package com.gm910.sotdivine.magic.theophany.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A holder for an impression's time info
 * 
 * @param timeAdded when this impression was inserted into the mind
 * @param lifetime  how long this impression will last; -1 means infinite
 */
public record ImpressionTimetracker(String fromDeity, long timeAdded, int lifetime) {
	public static final Codec<ImpressionTimetracker> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(Codec.STRING.fieldOf("deity").forGetter(ImpressionTimetracker::fromDeity),
					Codec.LONG.fieldOf("time_created").forGetter(ImpressionTimetracker::timeAdded),
					Codec.INT.fieldOf("lifetime").forGetter(ImpressionTimetracker::lifetime))
			.apply(instance, ImpressionTimetracker::new));

	public static final StreamCodec<ByteBuf, ImpressionTimetracker> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, ImpressionTimetracker::fromDeity, ByteBufCodecs.LONG,
			ImpressionTimetracker::timeAdded, ByteBufCodecs.INT, ImpressionTimetracker::lifetime,
			ImpressionTimetracker::new);

	public static final ImpressionTimetracker DEFAULT = new ImpressionTimetracker("", -1, -1);
}
