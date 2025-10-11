package com.gm910.sotdivine.items;

import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BannerPattern;

public class ModBannerPatternTags {

	public static final TagKey<BannerPattern> ALL_DIVINE = BannerPatternTags.create(ModUtils.path("all_divine"));

	private ModBannerPatternTags() {
	}
}
