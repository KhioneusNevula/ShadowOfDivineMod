package com.gm910.sotdivine;

import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.gm910.sotdivine.SOTDMod.EitherContext;
import com.gm910.sotdivine.common.misc.keys.ModKeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.logging.LogUtils;

import cpw.mods.util.Lazy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.BOTH)
public class ModKeys {

	public static final Lazy<ModKeyMapping> KEY_MEDITATION = Lazy.of(() -> new ModKeyMapping(

			"key.sotd.meditate", // Will be localized using this translation key
			// avoid conflicts between keys used in gui and in-game
			new SOTDMod.EitherContext(Set.of(ModKeyMapping.MEDITATION_CONFLICT_CONTEXT, KeyConflictContext.IN_GAME)),
			InputConstants.Type.KEYSYM, // Default mapping is on the keyboard
			GLFW.GLFW_KEY_M, // Default key is P
			"key.categories.sotd" // Mapping will be in the misc category
	));

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		// Some client setup code
		LogUtils.getLogger().info("HELLO FROM CLIENT SETUP");
		LogUtils.getLogger().info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
	}

	@SubscribeEvent
	public static void keys(RegisterKeyMappingsEvent event) {
		event.register(ModKeys.KEY_MEDITATION.get());
	}

}
