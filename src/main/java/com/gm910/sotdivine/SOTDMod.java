package com.gm910.sotdivine;

import org.slf4j.Logger;

import com.gm910.sotdivine.blocks.ModBlocks;
import com.gm910.sotdivine.command.ModCommandArgumentTypes;
import com.gm910.sotdivine.items.ModItems;
import com.gm910.sotdivine.language.LanguageGen;
import com.gm910.sotdivine.misc.ModCreativeTabs;
import com.gm910.sotdivine.networking.ModNetwork;
import com.gm910.sotdivine.systems.deity.emanation.EmanationType;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.sphere.Spheres;
import com.gm910.sotdivine.systems.deity.symbol.DeitySymbols;
import com.gm910.sotdivine.systems.party.resource.PartyResourceType;
import com.gm910.sotdivine.systems.villagers.ModBrainElements;
import com.gm910.sotdivine.systems.villagers.poi.ModPoiTypes;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

// The value here should match an entry in the META-INF/mods.toml file
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
	public static final DeferredRegister<BannerPattern> PATTERNS = DeferredRegister.create(Registries.BANNER_PATTERN,
			MODID);

	// Create a Deferred Register to hold pois which will all be registered under
	// the "examplemod" namespace
	public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, MODID);
	// deferred register for spheree
	public static final DeferredRegister<ISphere> SPHERES = DeferredRegister.create(ISphere.REGISTRY_KEY, MODID);

	public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = DeferredRegister
			.create(ForgeRegistries.MEMORY_MODULE_TYPES, MODID);

	public static final DeferredRegister<SensorType<?>> SENSOR_TYPES = DeferredRegister
			.create(ForgeRegistries.SENSOR_TYPES, MODID);

	// Create a Deferred Register to hold CreativeModeTabs which will all be
	// registered under the "examplemod" namespace
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
			.create(Registries.CREATIVE_MODE_TAB, MODID);

	public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister
			.create(Registries.COMMAND_ARGUMENT_TYPE, MODID);
	/**
	 * Party resource types register
	 */
	public static final DeferredRegister<PartyResourceType<?>> PARTY_RESOURCE_TYPES = DeferredRegister.create(
			ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "resource_type")),
			SOTDMod.MODID);

	/**
	 * Emanation Type register
	 */
	public static final DeferredRegister<EmanationType<?>> EMANATION_TYPES = DeferredRegister.create(
			ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "emanation_type")),
			SOTDMod.MODID);
	/**
	 * Deity language generators
	 */
	public static final DeferredRegister<LanguageGen> LANGUAGE_GENS = DeferredRegister.create(
			ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "language_gen")),
			SOTDMod.MODID);

	public SOTDMod(FMLJavaModLoadingContext context) {
		var modBusGroup = context.getModBusGroup();

		// Register the commonSetup method for modloading
		FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

		// Register the Deferred Register to the mod event bus so blocks get registered
		BLOCKS.register(modBusGroup);
		ModBlocks.init();
		// Register the Deferred Register to the mod event bus so items get registered
		ITEMS.register(modBusGroup);
		ModItems.init();
		// Register the Deferred Register to the mod event bus so banner patterns get
		// registered
		PATTERNS.register(modBusGroup);
		// Register the Deferred Register to the mod event bus so tabs get registered
		CREATIVE_MODE_TABS.register(modBusGroup);
		ModCreativeTabs.init();

		POI_TYPES.register(modBusGroup);
		ModPoiTypes.init();

		MEMORY_MODULE_TYPES.register(modBusGroup);
		SENSOR_TYPES.register(modBusGroup);
		ModBrainElements.init();

		COMMAND_ARGUMENT_TYPES.register(modBusGroup);
		ModCommandArgumentTypes.init();

		PARTY_RESOURCE_TYPES.register(modBusGroup);
		PartyResourceType.init();

		SPHERES.register(modBusGroup);
		Spheres.init();

		AddReloadListenerEvent.BUS.addListener(Spheres::eventAddListener);
		AddReloadListenerEvent.BUS.addListener(DeitySymbols::eventAddListener);

		EMANATION_TYPES.register(modBusGroup);
		EmanationType.init();

		LANGUAGE_GENS.register(modBusGroup);
		LanguageGen.init();

		// Register the item to a creative tab
		BuildCreativeModeTabContentsEvent.getBus(modBusGroup).addListener(SOTDMod::addCreative);

		// Register our mod's ForgeConfigSpec so that Forge can create and load the
		// config file for us
		context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

		ModNetwork.init();

	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		// Some common setup code
		LOGGER.info("HELLO FROM COMMON SETUP");

		if (Config.logDirtBlock)
			LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

		LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

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
