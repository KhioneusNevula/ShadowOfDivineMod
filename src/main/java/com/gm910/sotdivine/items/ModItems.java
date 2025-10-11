package com.gm910.sotdivine.items;

import java.util.Collection;
import java.util.function.Function;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.blocks.ModBlocks;
import com.gm910.sotdivine.misc.ModCreativeTabs;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/**
 * Mod items class
 * 
 * @author borah
 *
 */
public class ModItems {
	private ModItems() {
	}

	public static void init() {
		System.out.println("Initializing mod items");
	}

	private static final Multimap<ResourceKey<CreativeModeTab>, ResourceKey<Item>> TABS = MultimapBuilder.hashKeys()
			.hashSetValues().build();

	/**
	 * Creates a new BlockItem with the id "examplemod:example_block", combining the
	 * namespace and path
	 */
	public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = register("example_block",
			(key) -> new BlockItem(ModBlocks.EXAMPLE_BLOCK.get(), new Item.Properties().setId(key)));

	public static final RegistryObject<Item> DIVINE_BANNER_PATTERN = register("divine_banner_pattern",
			(key) -> new Item(new Item.Properties().setId(key).stacksTo(1)
					.component(DataComponents.PROVIDES_BANNER_PATTERNS, ModBannerPatternTags.ALL_DIVINE)),
			CreativeModeTabs.TOOLS_AND_UTILITIES);

	/**
	 * Creates a new food item with the id "examplemod:example_id", nutrition 1 and
	 * saturation 2
	 */
	/**
	 * public static final RegistryObject<Item> EXAMPLE_ITEM =
	 * register("example_item", (key) -> new Item(new Item.Properties().setId(key)
	 * .food(new
	 * FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build())),
	 * ModCreativeTabs.EXAMPLE_TAB);
	 */

	/**
	 * Register items
	 * 
	 * @param blockID
	 * @param supplier
	 * @param tabs
	 * @return
	 */
	public static RegistryObject<Item> register(String blockID, Function<ResourceKey<Item>, Item> supplier) {
		ResourceKey<Item> key = SOTDMod.ITEMS.key(blockID);
		return SOTDMod.ITEMS.register(blockID, () -> supplier.apply(key));
	}

	/**
	 * Register items, also permit them to be applied to creative mode tabs
	 * 
	 * @param blockID
	 * @param supplier
	 * @param tabs
	 * @return
	 */
	@SafeVarargs
	public static RegistryObject<Item> register(String blockID, Function<ResourceKey<Item>, Item> supplier,
			RegistryObject<CreativeModeTab>... tabs) {
		ResourceKey<Item> key = SOTDMod.ITEMS.key(blockID);
		for (RegistryObject<CreativeModeTab> tab : tabs)
			TABS.put(tab.getKey(), key);
		return SOTDMod.ITEMS.register(blockID, () -> supplier.apply(key));
	}

	/**
	 * Register items, also permit them to be applied to creative mode tabs
	 * 
	 * @param blockID
	 * @param supplier
	 * @param tabs
	 * @return
	 */
	@SafeVarargs
	public static RegistryObject<Item> register(String blockID, Function<ResourceKey<Item>, Item> supplier,
			ResourceKey<CreativeModeTab>... tabs) {
		ResourceKey<Item> key = SOTDMod.ITEMS.key(blockID);
		for (ResourceKey<CreativeModeTab> tab : tabs)
			TABS.put(tab, key);
		return SOTDMod.ITEMS.register(blockID, () -> supplier.apply(key));
	}

	public static Collection<ResourceKey<Item>> itemsOfTab(ResourceKey<CreativeModeTab> tab) {
		return TABS.get(tab);
	}
}
