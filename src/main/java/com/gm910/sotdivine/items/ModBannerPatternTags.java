package com.gm910.sotdivine.items;

import com.gm910.sotdivine.deities_and_parties.deity.symbol.DeitySymbols;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BannerPattern;

public class ModBannerPatternTags {

	public static final TagKey<BannerPattern> ALL_DIVINE = BannerPatternTags.create(ModUtils.path("all_divine"));
	// public final static TagKey<BannerPattern> DIVINE_TAG =
	// TagKey.create(Registries.BANNER_PATTERN, DeitySymbols.DIVINE_TAG_PATH);

	private ModBannerPatternTags() {
	}
}
