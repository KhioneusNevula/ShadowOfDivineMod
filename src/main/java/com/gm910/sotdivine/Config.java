package com.gm910.sotdivine;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

	private static final ForgeConfigSpec.FloatValue SANCTUARY_ESCAPE_TIME = BUILDER.comment(
			"The amount of time (in seconds) an entity is allowed to remain in a sanctuary after it has been rejected")
			.defineInRange("sanctuaryEscapeTime", 10, 0, Float.MAX_VALUE);

	private static final ForgeConfigSpec.FloatValue SANCTUARY_PERMISSION_TIME = BUILDER.comment(
			"The amount of time (in seconds) an entity is allowed to remain in a sanctuary if let in by an attack")
			.defineInRange("sanctuaryPermissionTime", 30, 0, Float.MAX_VALUE);

	// a list of strings that are treated as resource locations for items
	private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
			.comment("A list of items to log on common setup.")
			.defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

	static final ForgeConfigSpec SPEC = BUILDER.build();

	/**
	 * The time allowed for an entity to escape a sanctuary after it has been
	 * rejected
	 */
	public static float sanctuaryEscapeTime;

	/**
	 * The time allowed for for an entity to remain in a sanctuary when it has been
	 * allowed in due to a violation
	 */
	public static float sanctuaryPermissionTime;
	public static Set<Item> items;

	private static boolean validateItemName(final Object obj) {
		return obj instanceof String && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse((String) obj));
	}

	@SubscribeEvent
	static void onLoad(final ModConfigEvent event) {

		sanctuaryEscapeTime = SANCTUARY_ESCAPE_TIME.get() * 20f;
		sanctuaryPermissionTime = SANCTUARY_PERMISSION_TIME.get() * 20f;

		// convert the list of strings into a set of items
		items = ITEM_STRINGS.get().stream()
				.map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
				.collect(Collectors.toSet());
	}

}
