package com.gm910.sotdivine.mixins_assist.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DummyMoveControl extends MoveControl {

	protected LivingEntity mob;

	public DummyMoveControl(LivingEntity mob) {
		super(null);
		this.mob = mob;
	}

	public LivingEntity entity() {
		return mob;
	}

	public boolean immovable() {
		return true;
	}

	public void tick() {
	}

}
