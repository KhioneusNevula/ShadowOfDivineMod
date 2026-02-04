package com.gm910.sotdivine.magic.afterlife.anchors;

import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.magic.afterlife.Afterlife;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An anchor which is considered destroyed if the block (or armor stand/etc) at
 * its position is no longer the block stored here.
 * 
 * @param block the block that has to be at the position
 */
public record BlockAnchor(GlobalPos storedObject, IPlaceableGenreProvider<?, ?> block)
		implements IPositionableAnchor<GlobalPos, GlobalPos> {

	public static MapCodec<BlockAnchor> createCodec() {
		return RecordCodecBuilder.mapCodec(
				instance -> instance.group(GlobalPos.CODEC.fieldOf("position").forGetter(BlockAnchor::storedObject),
						IGenreProvider.castCodec(IPlaceableGenreProvider.class).fieldOf("block_matcher")
								.forGetter(BlockAnchor::block))
						.apply(instance, BlockAnchor::new));
	}

	@Override
	public BlockAnchor updateAfterlifeReference(Afterlife afterlife) {
		return this;
	}

	@Override
	public GlobalPos getPosition() {
		return storedObject;
	}

	@Override
	public Optional<GlobalPos> getAnchor(ServerLevel level) {
		if (block.matchesPos(level.getServer().getLevel(this.storedObject.dimension()), this.storedObject.pos())) {
			return Optional.of(storedObject);
		}
		return Optional.empty();
	}

	@Override
	public AfterlifeAnchorType<?> getAnchorType() {
		return AfterlifeAnchorType.BLOCK.get();
	}

	@Override
	public String toString() {
		return "Anchor{pos=" + this.storedObject + ",block_matcher=" + this.block + "}";
	}

}
