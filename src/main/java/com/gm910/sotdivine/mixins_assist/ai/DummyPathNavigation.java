package com.gm910.sotdivine.mixins_assist.ai;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * An immoveable pathfinding system
 */
public class DummyPathNavigation extends PathNavigation {

	private LivingEntity mob;

	public DummyPathNavigation(LivingEntity en, Level p_26516_, Mob controller) {
		super(controller, p_26516_);
		this.mob = en;
	}

	public LivingEntity entity() {
		return mob;
	}

	@Override
	protected PathFinder createPathFinder(int p_26531_) {
		this.nodeEvaluator = new WalkNodeEvaluator();
		return new PathFinder(nodeEvaluator, p_26531_);
	}

	@Override
	protected Vec3 getTempMobPos() {
		return mob.position();
	}

	@Override
	protected boolean canUpdatePath() {
		return false;
	}

	@Override
	public boolean canNavigateGround() {
		return false;
	}

	@Override
	protected void doStuckDetection(Vec3 p_26539_) {

	}

	@Override
	protected void followThePath() {
	}

	@Override
	protected Path createPath(Set<BlockPos> p_26552_, int p_26553_, boolean p_26554_, int p_26555_) {
		return this.createPath(p_26552_, p_26553_, p_26554_, p_26555_, 0);
	}

	@Override
	public void updatePathfinderMaxVisitedNodes() {

	}

	@Override
	public boolean shouldRecomputePath(BlockPos p_200904_) {
		return false;
	}

	@Override
	public void tick() {

	}

}
