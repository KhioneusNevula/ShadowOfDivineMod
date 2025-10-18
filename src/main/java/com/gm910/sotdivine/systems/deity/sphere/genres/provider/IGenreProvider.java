package com.gm910.sotdivine.systems.deity.sphere.genres.provider;

import java.util.Optional;

import com.gm910.sotdivine.systems.deity.sphere.genres.creator.BlockCreator;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.BlockGenreProvider;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.server.level.ServerLevel;

/**
 * A provider for a specific genre
 * 
 * @param <T> What this provider tests (e.g. a {@link BlockGenreProvider} tests
 *            a BlockPos
 * @param <G> What this provider generates (e.g. a {@link BlockGenreProvider}
 *            provides a {@link BlockCreator}
 */
public interface IGenreProvider<T, G> {

	/**
	 * 
	 * Codec but cast to only work for one class of {@link IGenreProvider}
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public static <T extends IGenreProvider<?, ?>> Codec<T> castCodec(Class<? super T> clazz) {
		return ProviderType.DISPATCH_CODEC
				.comapFlatMap(
						(s) -> clazz.isAssignableFrom(s.providerType().providerClass()) ? DataResult.success((T) s)
								: DataResult
										.error(() -> "Wrong type of genreProvider: " + s.getClass().getSimpleName()),
						(s) -> s);
	}

	/**
	 * Codec for all {@link IGenreProvider} instances
	 * 
	 * @param <T>
	 * @return
	 */
	public static Codec<IGenreProvider<?, ?>> codec() {
		return ProviderType.DISPATCH_CODEC;
	}

	/**
	 * Whether the specific emanation matches
	 * 
	 * @param emanation
	 * @return
	 */
	public boolean matches(ServerLevel level, T instance);

	/**
	 * Generate an provider of this provider (with an optional prior emanation of
	 * the given thing, if needed)
	 * 
	 * @param level
	 * @param atPos
	 * @return
	 */
	public G generateRandom(ServerLevel level, Optional<T> prior);

	/**
	 * Returns this provider, but in a more elaborated form
	 * 
	 * @return
	 */
	public default String report() {
		return toString();
	}

	/**
	 * Return the type of this provider
	 * 
	 * @return
	 */
	public ProviderType<? extends IGenreProvider<T, G>> providerType();

}
