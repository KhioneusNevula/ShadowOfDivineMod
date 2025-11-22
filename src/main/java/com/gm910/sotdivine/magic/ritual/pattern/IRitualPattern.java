package com.gm910.sotdivine.magic.ritual.pattern;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.concepts.genres.IGenreType;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Ritual patterns use symbols to signify significant elements used to construct
 * the pattern, and asterisks (*) to signify positions that are not important
 * but still are registered as part of the pattern's "active space"
 */
public sealed interface IRitualPattern permits RitualPattern {

	/**
	 * The symbol that indicates 'active space' in the ritual but not anything else
	 */
	public static final String EMPTY_SYMBOL = "*";

	/**
	 * Codec that either reads a ritual patterns from the registry or directly
	 */
	public static Codec<IRitualPattern> eitherCodec() {
		if (RitualPattern.EITHER_CODEC == null) {
			RitualPattern.EITHER_CODEC = Codec.either(
					ResourceLocation.CODEC.flatXmap(
							s -> RitualPatterns.instance().getPatternMap().get(s) instanceof IRitualPattern rp
									? DataResult.success(rp)
									: DataResult.error(() -> "Invalid resource location " + s),
							(s) -> RitualPatterns.instance().getPatternMap().inverse()
									.get(s) instanceof ResourceLocation rl ? DataResult.success(rl)
											: DataResult.error(() -> "No resource location for pattern " + s)),
					RitualPattern.READER_CODEC).xmap((x) -> Either.unwrap(x),
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
	 * Return the genres allowed for this symbol; does not need to return anything
	 * for the symbol associated with {@link #focusSymbol()}. If this returns empty,
	 * use {@link GenreTypes#BUILDING_BLOCK}
	 * 
	 * @param symbol
	 * @return
	 */
	public Collection<IGenreType<? extends IPlaceableGenreProvider<?, ?>>> permittedGenres(String symbol);

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
	 * that should be at the given rawPosition relative to the center; return null if
	 * it does not matter
	 * 
	 * @param relativePos
	 * @return
	 */
	public String getSymbolAt(Vec3i relativePos);

	/**
	 * Return all positions associated with this symbol
	 * 
	 * @param symbol
	 * @return
	 */
	public Collection<Vec3i> getPositions(String symbol);

	/**
	 * Position of the focus block; this just returns {@link Vec3i#ZERO} since all
	 * focuses are at rawPosition 0
	 * 
	 * @return
	 */
	Vec3i focus();

	/**
	 * Return a component to translate this pattern
	 * 
	 * @return
	 */
	public default Component translate() {
		return Optional.ofNullable(RitualPatterns.instance().getPatternMap().inverse().get(this))
				.map((s) -> TextUtils.transPrefix("sotd.pattern." + s.toLanguageKey()))
				.orElse(TextUtils.transPrefix("sotd.pattern.unnamed"));
	}

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
	 * Return all entities at any notable or asterisk-ed rawPosition within this
	 * pattern
	 * 
	 * @param world
	 * @param focus
	 * @return
	 */
	public <T extends Entity> Stream<T> getEntitiesInPattern(ServerLevel world, BlockPos focus,
			EntityTypeTest<Entity, T> test, Predicate<T> pred);

	/**
	 * Return all positions in this pattern
	 * 
	 * @return
	 */
	public Stream<? extends Vec3i> getAllBlockPositions();

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

	/**
	 * Return number of blocks this pattern has which are not {@link #EMPTY_SYMBOL}
	 * or blank spaces
	 * 
	 * @return
	 */
	int blockCount();

	/**
	 * Returns the symbol at the focus (center) of this pattern
	 * 
	 * @return
	 */
	public String focusSymbol();
}
