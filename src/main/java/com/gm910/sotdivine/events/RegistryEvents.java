package com.gm910.sotdivine.events;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.items.ModItems;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.language.Languages;
import com.gm910.sotdivine.language.lexicon.Lexicons;
import com.gm910.sotdivine.language.phonology.Phonologies;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.sphere.Spheres;

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
		event.dataPackRegistry(ModRegistries.LEXICONS, Lexicons.lexiconCodec(), Lexicons.lexiconCodec());
		event.dataPackRegistry(ModRegistries.PHONOLOGIES, Phonologies.phonologyCodec(), Phonologies.phonologyCodec());
		event.dataPackRegistry(ModRegistries.LANGUAGES, Languages.languageCodec(), Languages.languageCodec());

	}

}