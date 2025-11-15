package com.gm910.sotdivine.magic.sphere;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.gm910.sotdivine.ModRegistries;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.event.AddReloadListenerEvent;

public class Spheres extends SimpleJsonResourceReloadListener<ISphere> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private BiMap<ResourceLocation, ISphere> spheres = ImmutableBiMap.of();
	private static Optional<Spheres> INSTANCE = Optional.empty();
	private Map<ISphere, PoiType> ritualPoiTypes = new HashMap<>();
	private static Codec<ISphere> CODEC;

	/**
	 * Codec that retrieves spheres based on their resource location
	 */
	public static final Codec<ISphere> BY_NAME_CODEC = ResourceLocation.CODEC.comapFlatMap((rl) -> {
		var sphere = Spheres.instance().spheres.get(rl);
		if (sphere == null) {
			return DataResult.error(() -> "No sphere found for resource location " + rl);
		}
		return DataResult.success(sphere);
	}, ISphere::name);

	public static final Codec<ISphere> sphereCodec() {
		if (CODEC == null)
			CODEC = Codec.lazyInitialized(Sphere::createCodec);
		return CODEC;
	}

	private Spheres(Provider prov) {
		super(prov, sphereCodec(), ModRegistries.SPHERES);

	}

	@Override
	protected Map<ResourceLocation, ISphere> prepare(ResourceManager man, ProfilerFiller p_10772_) {
		// LOGGER.info("InfoTag {}", SphereTags.CONCEPTUAL);
		return super.prepare(man, p_10772_);
	}

	@Override
	protected void apply(Map<ResourceLocation, ISphere> map, ResourceManager rm, ProfilerFiller p_10795_) {
		this.spheres = HashBiMap.create(Maps.transformEntries(map, (k, v) -> {
			((Sphere) v).name = k;
			return v;
		}));

		map.values().forEach((sphere) -> {

			ResourceLocation path = ResourceLocation.fromNamespaceAndPath(sphere.name().getNamespace(),
					sphere.name().getPath() + "_sotdivine_focus");

			/*if (!sphere.getGenres(GenreTypes.FOCUS_BLOCK).isEmpty()) {
				if (!ForgeRegistries.POI_TYPES.containsKey(pathType)) {
			
					Set<BlockState> states = new HashSet<>();
			
					sphere.getGenres(GenreTypes.FOCUS_BLOCK).stream().forEach((pred) -> {
						if (pred.kind().left().orElse(null) instanceof HolderSet<Block> blocks) {
							blocks.forEach((hol) -> {
								hol.get().getStateDefinition().getPossibleStates().stream()
										.filter((state) -> pred.properties().isPresent()
												? pred.properties().get().matches(state)
												: true)
										.forEach((state) -> {
											states.add(state);
										});
							});
						}
					});
			
					PoiType poi = new PoiType(states, 0, 1);
			
					// ModUtils.forceRegister(ForgeRegistries.POI_TYPES, pathType, poi);
					this.ritualPoiTypes.put(sphere, poi);
				} else {
					this.ritualPoiTypes.put(sphere, ForgeRegistries.POI_TYPES.getValue(pathType));
				}
			}*/
		});

		LOGGER.info("Loaded spheres: {}", spheres.values());
	}

	/**
	 * Return the poi type for focus blocks for the given sphere
	 * 
	 * @return
	 */
	@Deprecated
	public PoiType focusType(ISphere sphere) {
		return this.ritualPoiTypes.get(sphere);
	}

	public static Spheres instance() {
		return INSTANCE.get();
	}

	public static void init() {
		LogUtils.getLogger().debug("Initializing spheres");
	}

	/**
	 * Get all spheres
	 * 
	 * @return
	 */
	public BiMap<ResourceLocation, ISphere> getSphereMap() {
		return Maps.unmodifiableBiMap(spheres);
	}

	public ISphere sphere(ResourceLocation location) {
		return spheres.get(location);
	}

	public ResourceLocation rl(ISphere from) {
		return spheres.inverse().get(from);
	}

	/**
	 * So we can add this reloadable registry to the listener
	 * 
	 * @param event
	 */
	public static void eventAddListener(AddReloadListenerEvent event) {
		INSTANCE = Optional.of(new Spheres(event.getRegistries()));
		event.addListener(INSTANCE.get());
	}

	public static void initTest(HolderLookup.Provider provider) {
		// provider.lookup(ISphere.SPHERES).get().listTagIds().collect(ModUtils.setStringCollector(","));
		/*
		 * LOGGER.debug("TEST SPHERE json: " +
		 * sphereCodec().encodeStart(JsonOps.INSTANCE, new Sphere(
		 * Map.of(GenreTypes.DIMENSION, Set.of(BuiltinDimensionTypes.NETHER),
		 * GenreTypes.OFFERING, Set.of(ItemPredicate.Builder.item().of(null,
		 * Items.GOLDEN_SWORD)
		 * 
		 * .withComponents(DataComponentMatchers.Builder.components()
		 * .partial(DataComponentPredicates.ENCHANTMENTS,
		 * EnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
		 * provider.lookupOrThrow(Registries.ENCHANTMENT)
		 * .getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))))
		 * .build())
		 * 
		 * .build())), Map.of(DeityInteractionType.SPELL, Set.of( new SpawnEmanation(
		 * WeightedList.of(new Weighted<>(EntityType.AXOLOTL, 1), new
		 * Weighted<>(EntityType.CAT, 1)),
		 * ISpellProperties.create(SpellAlignment.BLESSING, false)), new
		 * GiveEffectEmanation(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60),
		 * ISpellProperties.create(SpellAlignment.BLESSING, false)))))));
		 */
	}
}
