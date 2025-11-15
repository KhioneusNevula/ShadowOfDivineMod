package com.gm910.sotdivine.magic.sphere;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.RegistryManager;

public class SphereTags {

	public static final TagKey<ISphere> CONCEPTUAL = tag("conceptual");
	public static final TagKey<ISphere> ELEMENTAL = tag("elemental");
	public static final TagKey<ISphere> CREATION = tag("creation");
	public static final TagKey<ISphere> DESTRUCTION = tag("destruction");

	private SphereTags() {
	}

	private static TagKey<ISphere> tag(String p_203855_) {
		return RegistryManager.FROZEN.getRegistry(ModRegistries.SPHERES).tags().createTagKey(ModUtils.path(p_203855_));
	}

}
