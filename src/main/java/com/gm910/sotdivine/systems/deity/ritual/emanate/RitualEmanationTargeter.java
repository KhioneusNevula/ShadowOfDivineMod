package com.gm910.sotdivine.systems.deity.ritual.emanate;

import java.util.Optional;

import com.gm910.sotdivine.systems.deity.emanation.IEmanation;
import com.gm910.sotdivine.systems.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.systems.deity.sphere.genres.IGenreType;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IEntityGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A ritual emanation targeter which can optionally target the position(s) of
 * instances of the given (placeable) genre and optionally target instances of a
 * given (entity) genre in the ritual
 */
public record RitualEmanationTargeter(IEmanation emanation, Optional<IGenreType<?>> targetPositions,
		Optional<IGenreType<?>> targetEntities) {

	private static Codec<RitualEmanationTargeter> CODEC;

	@SuppressWarnings("unchecked")
	public static Codec<RitualEmanationTargeter> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder.create(instance -> instance
					.group(IEmanation.codec().fieldOf("emanation").forGetter(RitualEmanationTargeter::emanation),
							GenreTypes.genreCodec()
									.validate((x) -> IPlaceableGenreProvider.class.isAssignableFrom(x.genreClass())
											? DataResult.success(x)
											: DataResult.error(() -> "Needs placeable class type, not "
													+ x.genreClass().getSimpleName()))
									.optionalFieldOf("position_targeter")
									.<RitualEmanationTargeter>forGetter((r) -> r.targetPositions),
							GenreTypes.genreCodec()
									.validate((x) -> IEntityGenreProvider.class.isAssignableFrom(x.genreClass())
											? DataResult.success(x)
											: DataResult.error(() -> "Needs entity class type, not "
													+ x.genreClass().getSimpleName()))
									.optionalFieldOf("entity_targeter")
									.<RitualEmanationTargeter>forGetter((r) -> r.targetEntities))
					.apply(instance, (i, tp, te) -> new RitualEmanationTargeter(i, tp, te)));
		}
		return CODEC;
	}

	public RitualEmanationTargeter(IEmanation emanation, Optional<IGenreType<?>> targetPositions,
			Optional<IGenreType<?>> targetEntities) {
		this.emanation = emanation;
		this.targetPositions = targetPositions;
		this.targetEntities = targetEntities;
		if (this.targetPositions.isPresent()
				&& !IPlaceableGenreProvider.class.isAssignableFrom(this.targetPositions.get().genreClass())) {
			throw new IllegalArgumentException(
					"Genre provider for targetPositions must be instanceof IPlaceableGenreProvider, not "
							+ targetPositions.get().genreClass().getSimpleName());
		}
		if (this.targetEntities.isPresent()
				&& !IEntityGenreProvider.class.isAssignableFrom(this.targetEntities.get().genreClass())) {
			throw new IllegalArgumentException(
					"Genre provider for targetEntities must be instanceof IEntityGenreProvider, not "
							+ targetEntities.get().genreClass().getSimpleName());
		}
	}

	public RitualEmanationTargeter(IEmanation instance) {
		this(instance, Optional.empty(), Optional.empty());
	}

	public RitualEmanationTargeter(RitualEmanationTargeter t) {
		this(t.emanation, t.targetPositions, t.targetEntities);
	}

}
