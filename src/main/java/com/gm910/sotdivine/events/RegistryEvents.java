package com.gm910.sotdivine.events;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.items.ModItems;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.ISymbolBearer;
import com.gm910.sotdivine.concepts.symbol.impl.BannerSymbolBearer;
import com.gm910.sotdivine.concepts.symbol.impl.ISymbolWearer;
import com.gm910.sotdivine.concepts.symbol.impl.ItemStackSymbolBearer;
import com.gm910.sotdivine.concepts.symbol.impl.LivingEntitySymbolWearer;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.sanctuary.cap.ISanctuaryInfo;
import com.gm910.sotdivine.magic.sanctuary.cap.SanctuaryInfo;
import com.gm910.sotdivine.magic.sphere.Spheres;
import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
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
		event.dataPackRegistry(ModRegistries.SPHERES, Spheres.sphereCodec(), Spheres.sphereCodec());
		event.dataPackRegistry(ModRegistries.DEITY_SYMBOLS, DeitySymbols.symbolCodec(), DeitySymbols.symbolCodec());
		event.dataPackRegistry(ModRegistries.RITUAL_PATTERN, RitualPatterns.patternCodec(),
				RitualPatterns.patternCodec());

	}

}