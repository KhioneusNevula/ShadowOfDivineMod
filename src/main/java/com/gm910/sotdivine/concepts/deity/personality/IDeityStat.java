package com.gm910.sotdivine.concepts.deity.personality;

import com.gm910.sotdivine.util.ModUtils;
import com.mojang.serialization.Lifecycle;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * A stat of a deity's personality/tendencies
 * 
 * @author borah
 *
 */
public interface IDeityStat {

	public static final Registry<IDeityStat> REGISTRY = new MappedRegistry<>(
			ResourceKey.createRegistryKey(ModUtils.path("deity_stats")), Lifecycle.stable());

	/**
	 * Register a deity stat
	 * 
	 * @param <T>
	 * @param id
	 * @param sphere
	 * @return
	 */
	public static <T extends IDeityStat> T register(ResourceLocation id, T sphere) {
		return Registry.register(REGISTRY, id, sphere);
	}

	/**
	 * Default value of this stat
	 * 
	 * @return
	 */
	public float defaultValue();

}
