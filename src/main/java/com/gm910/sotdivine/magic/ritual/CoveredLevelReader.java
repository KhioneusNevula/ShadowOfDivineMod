package com.gm910.sotdivine.magic.ritual;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A level reader designed exclusively to "fake" a version of a level with
 * certain blocks set in a certain way
 */
public class CoveredLevelReader implements LevelReader {

	private ServerLevel base;
	private Map<? extends Vec3i, BlockState> blocks;

	public CoveredLevelReader(ServerLevel base, Map<? extends Vec3i, BlockState> blocks) {
		this.base = base;
		this.blocks = blocks;
	}

	public Map<? extends Vec3i, BlockState> allBlocks() {
		return blocks;
	}

	@Override
	public float getShade(Direction p_45522_, boolean p_45523_) {
		return base.getShade(p_45522_, p_45523_);
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return base.getLightEngine();
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos p_45570_) {
		return base.getBlockEntity(p_45570_);
	}

	@Override
	public BlockState getBlockState(BlockPos p_45571_) {
		return blocks.getOrDefault(p_45571_, base.getBlockState(p_45571_));
	}

	@Override
	public FluidState getFluidState(BlockPos p_45569_) {
		if (blocks.get(p_45569_) instanceof BlockState state) {
			return state.getFluidState();
		}
		return base.getFluidState(p_45569_);
	}

	@Override
	public WorldBorder getWorldBorder() {
		return base.getWorldBorder();
	}

	@Override
	public List<VoxelShape> getEntityCollisions(Entity p_186427_, AABB p_186428_) {
		return base.getEntityCollisions(p_186427_, p_186428_);
	}

	@Override
	public ChunkAccess getChunk(int p_46823_, int p_46824_, ChunkStatus p_333298_, boolean p_46826_) {
		return base.getChunk(p_46823_, p_46824_, p_333298_, p_46826_);
	}

	@Override
	public boolean hasChunk(int p_46838_, int p_46839_) {
		return base.hasChunk(p_46838_, p_46839_);
	}

	@Override
	public int getHeight(Types p_46827_, int p_46828_, int p_46829_) {
		return base.getHeight(p_46827_, p_46828_, p_46829_);
	}

	@Override
	public int getSkyDarken() {
		return base.getSkyDarken();
	}

	@Override
	public BiomeManager getBiomeManager() {
		return base.getBiomeManager();
	}

	@Override
	public Holder<Biome> getUncachedNoiseBiome(int p_204159_, int p_204160_, int p_204161_) {
		return base.getUncachedNoiseBiome(p_204159_, p_204160_, p_204161_);
	}

	@Override
	public boolean isClientSide() {

		return false;
	}

	@Override
	public int getSeaLevel() {
		return base.getSeaLevel();
	}

	@Override
	public DimensionType dimensionType() {
		return base.dimensionType();
	}

	@Override
	public RegistryAccess registryAccess() {
		return base.registryAccess();
	}

	@Override
	public FeatureFlagSet enabledFeatures() {
		return base.enabledFeatures();
	}

	@Override
	public String toString() {
		return "TempLR" + this.blocks;
	}

}
