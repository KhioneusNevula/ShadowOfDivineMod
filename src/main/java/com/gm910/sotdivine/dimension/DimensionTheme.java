package com.gm910.sotdivine.dimension;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.dimension.LevelStem;

/**
 * Define a dimension's theme;
 */
public record DimensionTheme(Optional<LevelStem> stem) {

	/**
	 * Codec for defining dimension etc
	 */
	public static final Codec<DimensionTheme> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(
			instance -> instance.group(LevelStem.CODEC.optionalFieldOf("definition").forGetter(DimensionTheme::stem))
					.apply(instance, DimensionTheme::new)));

	/**
	 * Codec that does not contain meaningful information (for network sending)
	 */
	public static final Codec<DimensionTheme> NETWORK_CODEC = Codec.unit(new DimensionTheme(Optional.empty()));
}
