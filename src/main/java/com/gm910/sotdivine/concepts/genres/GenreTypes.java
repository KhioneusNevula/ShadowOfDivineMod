package com.gm910.sotdivine.concepts.genres;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.concepts.genres.other.MagicWord;
import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.concepts.genres.provider.entity.EquipmentGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.BlockGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.EntityGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.ItemGenreProvider;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.base.Suppliers;
import com.google.common.collect.Range;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.DeferredRegister.RegistryHolder;
import net.minecraftforge.registries.RegistryBuilder;

public class GenreTypes {

	public final static RegistryHolder<IGenreType<?>> REGISTRY = SOTDMod.GENRE_TYPES.makeRegistry(
			() -> RegistryBuilder.<IGenreType<?>>of(ModRegistries.GENRE_TYPES.location()).allowModification());
	static final Map<ResourceLocation, IGenreType<?>> GENRES = new HashMap<>();

	/**
	 * Deserialized as either an item or a predicate
	 */
	public static final GenreType<ItemGenreProvider> OFFERING = register(ModUtils.path("offering"),
			ItemGenreProvider.class, () -> ItemGenreProvider.codec(), Range.atLeast(0), true, false);

	/**
	 * A ritual instrument, i.e. something used to right click on a ritual to start
	 * it
	 */
	public static final GenreType<ItemGenreProvider> RITUAL_INSTRUMENT = registerCopy(OFFERING,
			ModUtils.path("ritual_instrument"));

	/**
	 * A drug, i.e. a consumable item used to communicate with the deity(?)
	 */
	public static final GenreType<ItemGenreProvider> DRUG = registerCopy(OFFERING, ModUtils.path("drug"));

	/**
	 * A building block, i.e. a (full-faced) block used to build rituals
	 */
	public static final GenreType<BlockGenreProvider> BUILDING_BLOCK = register(ModUtils.path("building_block"),
			ProviderType.BLOCK, Range.atLeast(0), true, true);

	/**
	 * A block that a ritual is centered at
	 */
	public static final GenreType<BlockGenreProvider> FOCUS_BLOCK = registerCopy(BUILDING_BLOCK,
			ModUtils.path("focus_block"));

	/**
	 * A block that a ritual uses as a decoration
	 */
	public static final GenreType<IPlaceableGenreProvider<?, ?>> DECOR = register(ModUtils.path("decor"),
			Set.of(ProviderType.BLOCK, ProviderType.ENTITY), IPlaceableGenreProvider.class, Range.atLeast(0), true,
			false);
	/**
	 * A sacred mobID of the deity
	 */
	public static final GenreType<EntityGenreProvider> SACRED_MOB = register(ModUtils.path("sacred_mob"),
			ProviderType.ENTITY, Range.atLeast(0), true, true);

	/**
	 * An enemy mob of the deity, i.e. one against which the deity's powers do
	 * damage
	 */
	public static final GenreType<EntityGenreProvider> FORBIDDEN_MOB = register(ModUtils.path("forbidden_mob"),
			ProviderType.ENTITY, Range.atLeast(0), true, true);
	/**
	 * A sacred attire for the deity
	 */
	public static final GenreType<EquipmentGenreProvider> SACRED_ATTIRE = register(ModUtils.path("sacred_attire"),
			ProviderType.EQUIPMENT, Range.atLeast(0), false, false);

	/**
	 * A dimension type that a deity has power in relation to
	 */
	public static final GenreType<ResourceKey<DimensionType>> DIMENSION = register(ModUtils.path("dimension"),
			ResourceKey.class, () -> ResourceKey.codec(Registries.DIMENSION_TYPE), Range.atLeast(0), false, false);

	/**
	 * A biome that a deity has power in relation to
	 */
	public static final GenreType<ResourceKey<Biome>> BIOME = register(ModUtils.path("biome"), ResourceKey.class,
			() -> ResourceKey.codec(Registries.BIOME), Range.atLeast(0), false, false);

	/**
	 * A word that the deity considers powerful
	 */
	public static final GenreType<MagicWord> MAGIC_WORD = register(ModUtils.path("magic_word"), MagicWord.class,
			() -> MagicWord.CODEC, Range.atLeast(0), false, false);

	private GenreTypes() {
	}

	public static <T> GenreType<T> registerCopy(GenreType<T> offering2, ResourceLocation id) {
		return register(id, offering2.clazz, offering2.codec, offering2.amountPermitted(), offering2.giveable,
				offering2.placeable);
	}

	/**
	 * Register a genre
	 * 
	 * @param <T>
	 * @param id
	 * @param sphere
	 * @return
	 */

	public static <T extends IGenreProvider<?, ?>> GenreType<T> register(ResourceLocation id, ProviderType<T> provider,
			Range<Integer> amountPermitted, boolean give, boolean place) {
		LogUtils.getLogger().debug("Registering genre type " + id);// + " to registry " + SOTDMod.GENRE_TYPES);
		var gen = new GenreType<T>(id, provider.providerClass(), () -> provider.codec().get(), amountPermitted, give,
				place);
		GENRES.put(id, gen);
		SOTDMod.GENRE_TYPES.register(id.getPath(), () -> gen);
		return gen;
	}

	/**
	 * Register a genre
	 * 
	 * @param <T>
	 * @param id
	 * @param sphere
	 * @return
	 */

	public static <T extends IGenreProvider<?, ?>> GenreType<T> register(ResourceLocation id,
			Collection<ProviderType<? extends T>> provider, Class<? super T> clazz, Range<Integer> amountPermitted,
			boolean give, boolean place) {
		LogUtils.getLogger().debug("Registering genre type " + id);// + " to registry " + SOTDMod.GENRE_TYPES);

		var gen = new GenreType<T>(id, clazz,
				() -> CodecUtils.multiCodecEither(provider.stream().map((s) -> s.codec().get()).iterator()),
				amountPermitted, give, place);
		GENRES.put(id, gen);
		SOTDMod.GENRE_TYPES.register(id.getPath(), () -> gen);
		return gen;
	}

	/**
	 * Register a genre
	 * 
	 * @param <T>
	 * @param id
	 * @param sphere
	 * @return
	 */

	public static <T> GenreType<T> register(ResourceLocation id, Class<? super T> clazz, Supplier<Codec<T>> codec,
			Range<Integer> amountPermitted, boolean give, boolean place) {
		LogUtils.getLogger().debug("Registering genre type " + id);// + " to registry " + SOTDMod.GENRE_TYPES);
		var gen = new GenreType<T>(id, clazz, codec, amountPermitted, give, place);
		GENRES.put(id, gen);
		SOTDMod.GENRE_TYPES.register(id.getPath(), () -> gen);
		return gen;
	}

	public static void init() {
		LogUtils.getLogger().debug("Initializing genres");
	}

	/**
	 * Return genre codec
	 * 
	 * @return
	 */
	public static <T extends IGenreType<?>> Codec<T> genreCodec() {
		return REGISTRY.get() != null ? REGISTRY.get().getCodec().comapFlatMap((x) -> {
			try {
				return DataResult.success((T) x);
			} catch (ClassCastException e) {
				return DataResult.error(() -> "genre is of invalid type: " + x);
			}
		}, (g) -> g) : ResourceLocation.CODEC.comapFlatMap((x) -> {
			try {
				return DataResult.success((T) GENRES.get(x));
			} catch (ClassCastException e) {
				return DataResult.error(() -> "genre is of invalid type: " + x);
			}
		}, IGenreType::resourceLocation);

	}

	public static <T> IGenreType<T> getGenreType(ResourceLocation loc) {
		return (IGenreType<T>) GENRES.get(loc);
	}

	public static Map<ResourceLocation, IGenreType<?>> getAllGenreTypes() {
		return Collections.unmodifiableMap(GENRES);
	}

	static final class GenreType<T> implements IGenreType<T> {

		private ResourceLocation name;
		private Class<? super T> clazz;
		private Supplier<Codec<T>> codec;
		private Supplier<Codec<Collection<T>>> setCodec;
		private Supplier<Codec<Collection<?>>> setCodec2;
		private Range<Integer> ap;
		private boolean giveable;
		private boolean placeable;

		GenreType(ResourceLocation name, Class<? super T> clazz, Supplier<Codec<T>> codec,
				Range<Integer> amountPermitted, boolean giveable, boolean placeable) {
			this.name = name;
			this.clazz = clazz;
			this.codec = Suppliers.memoize(codec::get);
			this.ap = amountPermitted;
			this.setCodec = Suppliers
					.memoize(() -> Codec.list(this.codec.get()).xmap((l) -> new HashSet<>(l), ArrayList::new));
			this.setCodec2 = Suppliers.memoize(() -> Codec.list(this.codec.get()).xmap((l) -> new HashSet<>(l),
					(b) -> b.stream().map((x) -> (T) x).toList()));
			this.giveable = giveable;
			this.placeable = placeable;
		}

		@Override
		public boolean isGiveable() {
			return giveable;
		}

		@Override
		public boolean isPlaceable() {
			return placeable;
		}

		@Override
		public ResourceLocation resourceLocation() {
			return name;
		}

		@Override
		public Class<? super T> genreClass() {
			return clazz;
		}

		@Override
		public Codec<T> classCodec() {
			return this.codec.get();
		}

		@Override
		public Codec<Collection<T>> genreSetCodec() {
			return this.setCodec.get();
		}

		@Override
		public Codec<Collection<?>> typelessGenreSetCodec() {
			return this.setCodec2.get();
		}

		@Override
		public Range<Integer> amountPermitted() {
			return ap;
		}

		@Override
		public String toString() {
			return this.name + "(" + this.clazz.getSimpleName() + ")";
		}
	}

}
