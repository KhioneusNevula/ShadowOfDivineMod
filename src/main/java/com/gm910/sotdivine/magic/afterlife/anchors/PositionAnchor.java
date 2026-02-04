package com.gm910.sotdivine.magic.afterlife.anchors;

import java.util.Optional;

import com.gm910.sotdivine.magic.afterlife.Afterlife;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

/**
 * An anchor which is considered to be just a position and never destroyed
 * non-manually.
 */
public record PositionAnchor(GlobalPos storedObject) implements IPositionableAnchor<GlobalPos, GlobalPos> {

	public static MapCodec<PositionAnchor> createCodec() {
		return RecordCodecBuilder.mapCodec(
				instance -> instance.group(GlobalPos.CODEC.fieldOf("position").forGetter(PositionAnchor::storedObject))
						.apply(instance, PositionAnchor::new));
	}

	@Override
	public PositionAnchor updateAfterlifeReference(Afterlife afterlife) {
		return this;
	}

	@Override
	public GlobalPos getPosition() {
		return storedObject;
	}

	@Override
	public Optional<GlobalPos> getAnchor(ServerLevel level) {
		return Optional.of(storedObject);
	}

	@Override
	public AfterlifeAnchorType<?> getAnchorType() {
		return AfterlifeAnchorType.POSITION.get();
	}

	@Override
	public String toString() {
		return "Anchor{" + this.storedObject + "}";
	}

}
