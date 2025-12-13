package com.gm910.sotdivine.magic.theophany.impression;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Holds an impression AND its inputs
 */
public record ImpressionHolder(IImpression impression, List<ImpressionHolder> inputs) {
	public ImpressionHolder(IImpression impression2) {
		this(impression2, List.of());
	}

	public static final Codec<ImpressionHolder> CODEC = Codec.lazyInitialized(() -> Codec.recursive("ImpressionHolder",
			codec -> RecordCodecBuilder.create(instance -> instance
					.group(ImpressionType.resourceCodec().fieldOf("impression").forGetter(ImpressionHolder::impression),
							Codec.list(codec).optionalFieldOf("inputs", List.of()).forGetter(ImpressionHolder::inputs))
					.apply(instance, ImpressionHolder::new))));

	public static final StreamCodec<RegistryFriendlyByteBuf, ImpressionHolder> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ImpressionHolder>() {
		@Override
		public ImpressionHolder decode(RegistryFriendlyByteBuf buf) {
			IImpression imp = ImpressionType.resourceStreamCodec().decode(buf);
			int length = buf.readInt();
			List<ImpressionHolder> list = new ArrayList<>(length);
			for (int i = 0; i < length; i++) {
				list.add(this.decode(buf));
			}
			return new ImpressionHolder(imp, List.copyOf(list));
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, ImpressionHolder holder) {
			ImpressionType.resourceStreamCodec().encode(buf, holder.impression);
			buf.writeInt(holder.inputs.size());
			if (!holder.inputs.isEmpty()) {
				holder.inputs.forEach((ih) -> this.encode(buf, ih));
			}
		}
	};

}
