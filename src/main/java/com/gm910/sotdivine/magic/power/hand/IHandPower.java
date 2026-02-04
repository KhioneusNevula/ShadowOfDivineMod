package com.gm910.sotdivine.magic.power.hand;

import java.util.Optional;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A power that manifests on a spellcaster's hand, which they can then use by
 * right clicking on a block, entity, or in the air (depending on what it
 * targets; it will do nothing if used on something it cannot be used on). If a
 * hand power is active, item placement will do nothing. May have certain
 * effects on caster if not used quickly enough. Usually given by activating
 * certain impressions
 */
public interface IHandPower {
	/** */
	public boolean activate(ServerLevel level, Vec3 location, Either<BlockPos, Entity> target,
			Optional<Direction> hitFace, ItemStack heldItem);
}
