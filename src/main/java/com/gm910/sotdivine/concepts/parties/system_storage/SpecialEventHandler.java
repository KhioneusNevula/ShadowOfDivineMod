package com.gm910.sotdivine.concepts.parties.system_storage;

import java.util.ArrayList;
import java.util.List;

import com.gm910.sotdivine.SOTDMod;
import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registered to the event bus
 */
@Mod.EventBusSubscriber(modid = SOTDMod.MODID)
public class SpecialEventHandler {

	static final List<PartySystem> SYSTEMS = new ArrayList<>();

	/**
	 * Set the party system of this level('s server) to the given one
	 * 
	 * @param system
	 * @param levelg
	 */
	@SubscribeEvent
	public static void set(ServerStartingEvent event) {
		MinecraftServer server = event.getServer();
		ServerLevel level = server.overworld();

		for (PartySystem system : SYSTEMS) {

			level.getDataStorage().set(IPartySystem.SAVE_TYPE, system);
			system.allParties().stream().forEach((p) -> p.updateLevelReference(level));
			LogUtils.getLogger().debug("Setting PartySystem for world " + level.toString());
			PartySystem.cachedSystems.put(level, system);
			PartySystem.mostRecentCached = system;
		}
		SYSTEMS.clear();
	}

}