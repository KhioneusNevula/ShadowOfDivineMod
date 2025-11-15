package com.gm910.sotdivine.magic.auspiciousness;

import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Calculator for the auspiciousness of a location
 */
public class Auspiciousness {
	private Auspiciousness() {
	}

	/**
	 * Calculate auspiciousness of a location
	 * 
	 * @param level
	 * @param pos
	 * @return
	 */
	public static float calculateAuspiciousness(ServerLevel level, BlockPos pos) {
		return pos.getY() > 100 ? (pos.getY() / 256f) : (pos.getY() < 0 ? (-pos.getY() / 265f) : 1f);
	}

	/**
	 * Calculate auspiciousness of a game time
	 * 
	 * @param level
	 * @param pos
	 * @return
	 */
	public static float calculateTimeAuspiciousness(ServerLevel level) {
		return 1 / level.getMoonPhase();
	}

}
