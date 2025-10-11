package com.gm910.sotdivine.events;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.sphere.Spheres;
import com.gm910.sotdivine.systems.deity.symbol.DeitySymbols;
import com.gm910.sotdivine.systems.deity.symbol.IDeitySymbol;

import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistryEvents {

	@SubscribeEvent
	public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
		event.dataPackRegistry(ISphere.REGISTRY_KEY, Spheres.sphereCodec());
		event.dataPackRegistry(IDeitySymbol.REGISTRY_KEY, DeitySymbols.symbolCodec());
	}

}