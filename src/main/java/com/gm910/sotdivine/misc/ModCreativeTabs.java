package com.gm910.sotdivine.misc;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.items.ModItems;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Class for the creative tabs of this mod
 * 
 * @author borah
 *
 */
public class ModCreativeTabs {
	private ModCreativeTabs() {
	}

	public static void init() {
		System.out.println("Initializing creative tabs...");
	}

	// Creates a creative tab with the id "examplemod:example_tab" for the example
	// item, that is placed after the combat tab
	public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = register("example_tab",
			() -> CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT)
					.icon(() -> ModItems.EXAMPLE_BLOCK_ITEM.get().getDefaultInstance()));

	public static RegistryObject<CreativeModeTab> register(String name, Supplier<CreativeModeTab.Builder> func) {
		@NotNull
		ResourceKey<CreativeModeTab> key = SOTDMod.CREATIVE_MODE_TABS.key(name);
		return SOTDMod.CREATIVE_MODE_TABS.register(name, () -> func.get().displayItems((p, output) -> {
			ModItems.itemsOfTab(key).forEach(
					(ri) -> ForgeRegistries.ITEMS.getDelegate(ri).map((x) -> x.get()).ifPresent(output::accept));
		}).build());
	}

}
