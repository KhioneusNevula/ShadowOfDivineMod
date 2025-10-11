package com.gm910.sotdivine.systems.villagers.poi;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.gm910.sotdivine.SOTDMod;

import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

public class ModPoiTypes {

	/**
	 * Poi for all banners
	 */
	public static final RegistryObject<PoiType> BANNER = SOTDMod.POI_TYPES.register("banner",
			() -> new PoiType(getBlockStates(Blocks.BLACK_BANNER, Blocks.BLACK_WALL_BANNER, Blocks.BLUE_BANNER,
					Blocks.BLUE_WALL_BANNER, Blocks.BROWN_BANNER, Blocks.BROWN_WALL_BANNER, Blocks.CYAN_BANNER,
					Blocks.CYAN_WALL_BANNER, Blocks.GRAY_BANNER, Blocks.GRAY_WALL_BANNER, Blocks.GREEN_BANNER,
					Blocks.GREEN_WALL_BANNER, Blocks.LIGHT_BLUE_BANNER, Blocks.LIGHT_BLUE_WALL_BANNER,
					Blocks.LIGHT_GRAY_BANNER, Blocks.LIGHT_GRAY_WALL_BANNER, Blocks.LIME_BANNER,
					Blocks.LIME_WALL_BANNER, Blocks.MAGENTA_BANNER, Blocks.MAGENTA_WALL_BANNER, Blocks.ORANGE_BANNER,
					Blocks.ORANGE_WALL_BANNER, Blocks.PINK_BANNER, Blocks.PINK_WALL_BANNER, Blocks.PURPLE_BANNER,
					Blocks.PURPLE_WALL_BANNER, Blocks.RED_BANNER, Blocks.RED_WALL_BANNER, Blocks.WHITE_BANNER,
					Blocks.WHITE_WALL_BANNER, Blocks.YELLOW_BANNER, Blocks.YELLOW_WALL_BANNER), 0, 1));

	private ModPoiTypes() {

	}

	public static Set<BlockState> getBlockStates(Block... blocks) {
		return Arrays.stream(blocks).flatMap((x) -> x.getStateDefinition().getPossibleStates().stream())
				.collect(Collectors.toSet());

	}

	public static void init() {
		System.out.println("Loading mod pois");
	}

}
