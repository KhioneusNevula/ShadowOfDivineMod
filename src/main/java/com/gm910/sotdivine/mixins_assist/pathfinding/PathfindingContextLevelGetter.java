package com.gm910.sotdivine.mixins_assist.pathfinding;

import java.util.List;

import com.gm910.sotdivine.common.misc.ParticleSpecification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public record PathfindingContextLevelGetter(CollisionGetter level, Mob mob) implements CollisionGetter {

	@Override
	public BlockEntity getBlockEntity(BlockPos p_45570_) {
		return level.getBlockEntity(p_45570_);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		if (PathfindingMixinHelper.tryReturnSanctuaryPathType(pos.getX(), pos.getY(), pos.getZ(),
				mob) == PathfindingMixinHelper.SANCTUARY_BARRIER_PATH_TYPE) {
			if (!mob.fireImmune()) {
				return Blocks.LAVA.defaultBlockState();
			}
			return Blocks.NETHER_BRICK_FENCE.defaultBlockState();
		}
		return level.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		if (PathfindingMixinHelper.tryReturnSanctuaryPathType(pos.getX(), pos.getY(), pos.getZ(),
				mob) == PathfindingMixinHelper.SANCTUARY_BARRIER_PATH_TYPE) {
			return Fluids.EMPTY.defaultFluidState();
		}
		return level.getFluidState(pos);
	}

	@Override
	public int getHeight() {
		return level.getHeight();
	}

	@Override
	public int getMinY() {
		return level.getMinY();
	}

	@Override
	public WorldBorder getWorldBorder() {
		return level.getWorldBorder();
	}

	@Override
	public BlockGetter getChunkForCollisions(int p_45774_, int p_45775_) {
		return new WrapChunk(level.getChunkForCollisions(p_45774_, p_45775_));
	}

	@Override
	public List<VoxelShape> getEntityCollisions(Entity p_186427_, AABB p_186428_) {
		return level.getEntityCollisions(p_186427_, p_186428_);
	}

	private class WrapChunk implements BlockGetter {

		private BlockGetter chunk;

		private WrapChunk(BlockGetter chunk) {
			this.chunk = chunk;
		}

		@Override
		public int getHeight() {
			return chunk.getHeight();
		}

		@Override
		public int getMinY() {
			return chunk.getMinY();
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos p_45570_) {
			return chunk.getBlockEntity(p_45570_);
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			if (PathfindingMixinHelper.tryReturnSanctuaryPathType(pos.getX(), pos.getY(), pos.getZ(),
					mob) == PathfindingMixinHelper.SANCTUARY_BARRIER_PATH_TYPE) {
				return Blocks.BARRIER.defaultBlockState();
			}
			return chunk.getBlockState(pos);
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			if (PathfindingMixinHelper.tryReturnSanctuaryPathType(pos.getX(), pos.getY(), pos.getZ(),
					mob) == PathfindingMixinHelper.SANCTUARY_BARRIER_PATH_TYPE) {
				return Fluids.EMPTY.defaultFluidState();
			}
			return chunk.getFluidState(pos);
		}

	}

}
