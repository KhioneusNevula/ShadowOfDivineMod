package com.gm910.sotdivine;

import org.slf4j.Logger;

import com.gm910.sotdivine.common.blocks.ModBlocks;
import com.gm910.sotdivine.common.command.ModCommandArgumentTypes;
import com.gm910.sotdivine.common.items.ModItems;
import com.gm910.sotdivine.common.misc.ModCreativeTabs;
import com.gm910.sotdivine.common.misc.ModDataComponents;
import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.concepts.genres.IGenreType;
import com.gm910.sotdivine.concepts.genres.provider.data.CodecsComponentMatchers;
import com.gm910.sotdivine.concepts.genres.provider.entity_preds.CodecsTypeSpecificProviders;
import com.gm910.sotdivine.concepts.language.LanguageGen;
import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.concepts.parties.villagers.ModBrainElements;
import com.gm910.sotdivine.concepts.parties.villagers.poi.ModPoiTypes;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.magic.emanation.EmanationType;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.sphere.ISphere;
import com.gm910.sotdivine.magic.sphere.Spheres;
import com.gm910.sotdivine.network.ModNetwork;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

// The value here should instrument an entry in the META-INF/mods.toml file
@Mod(SOTDMod.MODID)
public final class SOTDMod {
	// Define mod id in a common place for everything to reference
	public static final String MODID = "sotdivine";
	// Directly reference a slf4j logger
	public static final Logger LOGGER = LogUtils.getLogger();
	// Create a Deferred Register to hold Blocks which will all be registered under
	// the "examplemod" namespace
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
	// Create a Deferred Register to hold Items which will all be registered under
	// the "examplemod" namespace
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

	// defered register for banner patterns
	public static final DeferredRegister<BannerPattern> BANNER_PATTERNS = DeferredRegister
			.create(Registries.BANNER_PATTERN, MODID);

	// Create a Deferred Register to hold pois which will all be registered under
	// the "examplemod" namespace
	public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE,
			MODID);

	public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister
			.create(Registries.DATA_COMPONENT_TYPE, MODID);

	public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = DeferredRegister
			.create(Registries.MEMORY_MODULE_TYPE, MODID);

	public static final DeferredRegister<SensorType<?>> SENSOR_TYPES = DeferredRegister.create(Registries.SENSOR_TYPE,
			MODID);

	// Create a Deferred Register to hold CreativeModeTabs which will all be
	// registered under the "examplemod" namespace
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
			.create(Registries.CREATIVE_MODE_TAB, MODID);

	public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister
			.create(Registries.COMMAND_ARGUMENT_TYPE, MODID);

	// deferred register for genres
	public static final DeferredRegister<IGenreType<?>> GENRE_TYPES = DeferredRegister.create(ModRegistries.GENRE_TYPES,
			MODID);

	/**
	 * Emanation Type register
	 */
	public static final DeferredRegister<EmanationType<?>> EMANATION_TYPES = DeferredRegister
			.create(ModRegistries.EMANATION_TYPES, SOTDMod.MODID);

	/**
	 * Party resource types register
	 */
	public static final DeferredRegister<PartyResourceType<?>> PARTY_RESOURCE_TYPES = DeferredRegister
			.create(ModRegistries.PARTY_RESOURCE_TYPES, SOTDMod.MODID);

	/**
	 * Deity language generators
	 */
	public static final DeferredRegister<LanguageGen> LANGUAGE_GENS = DeferredRegister
			.create(ModRegistries.LANGUAGE_GEN, SOTDMod.MODID);

	public SOTDMod(FMLJavaModLoadingContext context) {
		var modBusGroup = context.getModBusGroup();

		// Register the commonSetup method for modloading
		FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

		BLOCKS.register(modBusGroup);
		ModBlocks.init();
		ITEMS.register(modBusGroup);
		ModItems.init();
		BANNER_PATTERNS.register(modBusGroup);
		CREATIVE_MODE_TABS.register(modBusGroup);
		ModCreativeTabs.init();

		POI_TYPES.register(modBusGroup);
		ModPoiTypes.init();
		DATA_COMPONENT_TYPES.register(modBusGroup);
		ModDataComponents.init();

		MEMORY_MODULE_TYPES.register(modBusGroup);
		SENSOR_TYPES.register(modBusGroup);
		ModBrainElements.init();

		GenreTypes.init();
		GENRE_TYPES.register(modBusGroup);
		EmanationType.init();
		EMANATION_TYPES.register(modBusGroup);
		PartyResourceType.init();
		PARTY_RESOURCE_TYPES.register(modBusGroup);
		LanguageGen.init();
		LANGUAGE_GENS.register(modBusGroup);
		LanguageGen.registerInit();

		ModCommandArgumentTypes.init();
		COMMAND_ARGUMENT_TYPES.register(modBusGroup);

		AddReloadListenerEvent.BUS.addListener(RitualPatterns::eventAddListener);
		AddReloadListenerEvent.BUS.addListener(DeitySymbols::eventAddListener);
		AddReloadListenerEvent.BUS.addListener(Spheres::eventAddListener);

		// Register the item to a creative tab
		BuildCreativeModeTabContentsEvent.getBus(modBusGroup).addListener(SOTDMod::addCreative);

		// Register our mod's ForgeConfigSpec so that Forge can create and load the
		// config file for us
		context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

		ModNetwork.init();

		CodecsComponentMatchers.registerInit();
		CodecsTypeSpecificProviders.registerInit();

	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		// Some common setup code
		LOGGER.info("HELLO FROM COMMON SETUP");

		Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
	}

	// Add the example block item to the building blocks tab
	private static void addCreative(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
			event.accept(ModItems.EXAMPLE_BLOCK_ITEM);
	}

	// You can use EventBusSubscriber to automatically register all static methods
	// in the class annotated with @SubscribeEvent
	@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
	public static class ClientModEvents {
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event) {
			// Some client setup code
			LOGGER.info("HELLO FROM CLIENT SETUP");
			LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
		}
	}
}
