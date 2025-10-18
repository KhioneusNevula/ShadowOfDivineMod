package com.gm910.sotdivine.systems.deity.ritual.pattern;

import java.util.Collection;
import java.util.Map;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public sealed interface IRitualPattern permits RitualPattern {
	/**
	 * Codec that either reads a ritual patterns from the registry or directly
	 */
	public static Codec<IRitualPattern> eitherCodec() {
		if (RitualPattern.EITHER_CODEC == null) {
			RitualPattern.EITHER_CODEC = Codec
					.either(ResourceLocation.CODEC.xmap(RitualPatterns.instance().getPatternMap()::get,
							RitualPatterns.instance().getPatternMap().inverse()::get), RitualPattern.READER_CODEC)
					.xmap((x) -> Either.unwrap(x),
							(x) -> RitualPatterns.instance().getPatternMap().inverse().containsKey(x) ? Either.left(x)
									: Either.right(x));
		}
		return RitualPattern.EITHER_CODEC;
	}

	/**
	 * Returns all symbols used in this patterns
	 * 
	 * @return
	 */
	public Collection<String> symbols();

	/**
	 * The minimum bound of this patterns (relative to the focus block)
	 * 
	 * @return
	 */
	public Vec3i minPos();

	/**
	 * The maximum bound of this patterns (relative to the focus block)
	 * 
	 * @return
	 */
	public Vec3i maxPos();

	/**
	 * The block (as a string id; could also be an entity such as an armor stand)
	 * that should be at the given position relative to the center; return null if
	 * it does not matter
	 * 
	 * @param relativePos
	 * @return
	 */
	public String getBlock(Vec3i relativePos);

	/**
	 * Position of the focus block; this just returns {@link Vec3i#ZERO} since all
	 * focuses are at position 0
	 * 
	 * @return
	 */
	Vec3i focus();

	/**
	 * Return a copy of this rotated around the focus block by a quarter step
	 * 
	 * @param counterClockWise
	 * @return
	 */
	public IRitualPattern rotated90Copy(boolean counterClockWise);

	/**
	 * Return a copy of this rotated 180 degrees around the focus block
	 * 
	 * @param counterClockWise
	 * @return
	 */
	public IRitualPattern rotated180Copy();

	/**
	 * Return a copy of this fully flipped for the coordinate of the given axes
	 * 
	 * @return
	 */
	public IRitualPattern flippedCopy(FlipAxis... axis);

	/**
	 * Check if the patterns matches. Keep in mind that the rotation matters.
	 * 
	 * @param world
	 * @param center
	 * @param blockPreds
	 * @param entityPreds
	 * @return
	 */
	public boolean matches(ServerLevel world, BlockPos focus, Map<String, IPlaceableGenreProvider<?, ?>> blockPreds);

	/**
	 * Generates a version of this patterns at the given pos, overriding anything
	 * there
	 * 
	 * @param world
	 * @param center
	 * @param blockPreds
	 * @param entityPreds
	 * @return
	 */
	public void generatePossible(ServerLevel world, BlockPos focus, Map<String, IPlaceableGenreProvider<?, ?>> preds);
}
