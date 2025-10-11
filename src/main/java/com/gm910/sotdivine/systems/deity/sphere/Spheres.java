package com.gm910.sotdivine.systems.deity.sphere;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;

public class Spheres extends SimpleJsonResourceReloadListener<ISphere> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private BiMap<ResourceLocation, ISphere> spheres = ImmutableBiMap.of();
	private static Optional<Spheres> INSTANCE = Optional.empty();

	private static Codec<ISphere> CODEC;

	public static final Codec<ISphere> sphereCodec() {
		if (CODEC == null)
			CODEC = Sphere.createCodec();
		return CODEC;
	}

	private Spheres(Provider prov) {
		super(prov, sphereCodec(), Sphere.REGISTRY_KEY);

	}

	@Override
	protected Map<ResourceLocation, ISphere> prepare(ResourceManager man, ProfilerFiller p_10772_) {
		LOGGER.info("InfoTag {}", SphereTags.CONCEPTUAL);
		return super.prepare(man, p_10772_);
	}

	@Override
	protected void apply(Map<ResourceLocation, ISphere> map, ResourceManager rm, ProfilerFiller p_10795_) {
		this.spheres = HashBiMap.create(Maps.transformEntries(map, (k, v) -> {
			((Sphere) v).name = k;
			return v;
		}));
		LOGGER.info("RM " + rm.getNamespaces());
		LOGGER.info("Loaded spheres: {}", spheres.values());
	}

	public static Spheres instance() {
		return INSTANCE.get();
	}

	public static void init() {

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
		// provider.lookup(ISphere.REGISTRY_KEY).get().listTagIds().collect(ModUtils.setStringCollector(","));
		/*
		 * LOGGER.debug("TEST SPHERE json: " +
		 * sphereCodec().encodeStart(JsonOps.INSTANCE, new Sphere(
		 * Map.of(Genres.DIMENSION, Set.of(BuiltinDimensionTypes.NETHER),
		 * Genres.OFFERING, Set.of(ItemPredicate.Builder.item().of(null,
		 * Items.GOLDEN_SWORD)
		 * 
		 * .withComponents(DataComponentMatchers.Builder.components()
		 * .partial(DataComponentPredicates.ENCHANTMENTS,
		 * EnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
		 * provider.lookupOrThrow(Registries.ENCHANTMENT)
		 * .getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))))
		 * .build())
		 * 
		 * .build())), Map.of(DeityInteractionType.spell, Set.of( new SpawnEmanation(
		 * WeightedList.of(new Weighted<>(EntityType.AXOLOTL, 1), new
		 * Weighted<>(EntityType.CAT, 1)),
		 * ISpellProperties.create(SpellAlignment.blessing, false)), new
		 * GiveEffectEmanation(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60),
		 * ISpellProperties.create(SpellAlignment.blessing, false)))))));
		 */
	}
}
