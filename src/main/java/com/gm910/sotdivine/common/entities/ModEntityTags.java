package com.gm910.sotdivine.common.entities;

import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModEntityTags {

	public static final TagKey<EntityType<?>> WORSHIPER = TagKey.create(Registries.ENTITY_TYPE,
			ModUtils.path("worshiper"));
	public static final TagKey<EntityType<?>> NATURALLY_PERSISTENT = TagKey.create(Registries.ENTITY_TYPE,
			ModUtils.path("naturally_persistent"));

	private ModEntityTags() {

	}

}
