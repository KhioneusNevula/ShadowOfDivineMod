package com.gm910.sotdivine.systems.deity.sphere;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.tags.TagKey;

public class SphereTags {

	public static final TagKey<ISphere> CONCEPTUAL = tag("conceptual");
	public static final TagKey<ISphere> ELEMENTAL = tag("elemental");
	public static final TagKey<ISphere> CREATION = tag("creation");
	public static final TagKey<ISphere> DESTRUCTION = tag("destruction");

	private SphereTags() {
	}

	private static TagKey<ISphere> tag(String p_203855_) {
		return SOTDMod.SPHERES.createTagKey(ModUtils.path(p_203855_));
	}

}
