package com.gm910.sotdivine.util;

import com.gm910.sotdivine.SOTDMod;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Some utilities for mod registration and mod cycles
 * 
 * @author borah
 *
 */
public class ModUtils {

	private ModUtils() {
	}

	/**
	 * Forcibly registers an item, unfreezing a registry in order to do it. If I
	 * could shatter these stupid registries to pieces I WOULD.
	 * 
	 * @param <T>
	 * @param registry
	 * @param location
	 * @param value
	 */
	public static <T> void forceRegister(IForgeRegistry<T> registry, ResourceLocation location, T value) {

		ForgeRegistry<T> stupidRegistry = (ForgeRegistry<T>) registry;
		boolean frozenBefore = FieldUtils.getInstanceField("isFrozen", stupidRegistry);

		FieldUtils.setInstanceField("isFrozen", stupidRegistry, false);

		if (registry.containsKey(location)) {
			LogUtils.getLogger().debug("Re-registering " + location + " to frozen registry with value " + value);
			stupidRegistry.remove(location);
		} else {
			LogUtils.getLogger().debug("Initially registering " + location + " to frozen registry with value " + value);

		}
		stupidRegistry.register(location, value);

		FieldUtils.setInstanceField("isFrozen", stupidRegistry, frozenBefore);
	}

	/**
	 * Return a resource location with the mod id
	 * 
	 * @param path
	 * @return
	 */
	public static ResourceLocation path(String path) {
		return ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, path);
	}

}
