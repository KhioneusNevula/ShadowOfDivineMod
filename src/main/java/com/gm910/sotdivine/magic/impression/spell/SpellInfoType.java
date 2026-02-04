package com.gm910.sotdivine.magic.impression.spell;

import com.gm910.sotdivine.concepts.genres.provider.independent.BlockGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.EntityGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IEntityGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.ItemGenreProvider;
import com.gm910.sotdivine.magic.afterlife.Soul;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IExtensibleEnum;

/**
 * Kinds of information that can be fed to spells
 */
public enum SpellInfoType implements IExtensibleEnum {
	/** Placeholder type for non-info givers */
	NONE,
	/** A target entity */
	SOUL,
	/** A target deity */
	DEITY,
	/** A target position in the universe */
	POSITION,
	/** A direction vector */
	DIRECTION,
	/** An item generation/matching info */
	ITEM_GENRE,
	/** A block generation/matching information */
	BLOCK_GENRE,
	/** An entity generation/matching information */
	ENTITY_GENRE,
	/** Represents an entire dimension */
	WORLD;

	public static SpellInfoType create(String name) {
		throw new UnsupportedOperationException("Unextended enum; " + name);
	}
}
