package com.gm910.sotdivine.events;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.items.ModItems;
import com.gm910.sotdivine.registries.ModRegistries;
import com.gm910.sotdivine.systems.deity.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.systems.deity.sphere.Spheres;
import com.gm910.sotdivine.systems.deity.symbol.DeitySymbols;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistryEvents {

	@SubscribeEvent
	public static void buildContents(BuildCreativeModeTabContentsEvent event) {
		ModItems.itemsOfTab(event.getTabKey()).forEach((e) -> event.accept(e));
	}

	@SubscribeEvent
	public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
		event.dataPackRegistry(ModRegistries.DEITY_SYMBOLS, DeitySymbols.symbolCodec(), DeitySymbols.symbolCodec());
		event.dataPackRegistry(ModRegistries.RITUAL_PATTERN, RitualPatterns.patternCodec(),
				RitualPatterns.patternCodec());
		event.dataPackRegistry(ModRegistries.SPHERES, Spheres.sphereCodec(), Spheres.sphereCodec());

	}

}