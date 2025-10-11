package com.gm910.sotdivine.blocks;

import java.util.function.Function;

import com.gm910.sotdivine.SOTDMod;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/**
 * Blocks from this mod
 * 
 * @author borah
 *
 */
public class ModBlocks {
	private ModBlocks() {
	}

	public static void init() {
		System.out.println("Initializing blocks...");
	}

	/**
	 * Creates a new Block with the id "examplemod:example_block", combining the
	 * namespace and path
	 */
	public static final RegistryObject<Block> EXAMPLE_BLOCK = register("example_block",
			(key) -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE)));

	public static RegistryObject<Block> register(String blockID, Function<ResourceKey<Block>, Block> supplier) {
		return SOTDMod.BLOCKS.register(blockID, () -> supplier.apply(SOTDMod.BLOCKS.key(blockID)));
	}

}
