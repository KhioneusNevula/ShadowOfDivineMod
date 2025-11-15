package com.gm910.sotdivine.common.misc;

import java.util.function.Supplier;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.items.ModItems;
import com.gm910.sotdivine.util.TextUtils;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
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

	// Creates a creative tab for the example after Combat tab
	public static final RegistryObject<CreativeModeTab> SOTD_TAB = register("sotd_tab",
			() -> CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT)
					.title(TextUtils.transPrefix("tab_group.name"))
					.icon(() -> new ItemStack(ModItems.DIVINE_BANNER_PATTERN.get())));

	public static RegistryObject<CreativeModeTab> register(String name, Supplier<CreativeModeTab.Builder> func) {
		return SOTDMod.CREATIVE_MODE_TABS.register(name, () -> func.get().build());
	}

}
