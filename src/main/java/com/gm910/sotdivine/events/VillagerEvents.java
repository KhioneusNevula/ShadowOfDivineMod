package com.gm910.sotdivine.events;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.systems.villagers.VillagerTweaks;
import com.gm910.sotdivine.systems.villagers.ModBrainElements.MemoryModuleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.LivingMakeBrainEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillagerEvents {

	@SubscribeEvent
	public static void brainForVillager(LivingMakeBrainEvent event) {
		if (event.getEntity() instanceof Villager && event.getEntity().level() instanceof ServerLevel) {
			VillagerTweaks.modifyMind((Villager) event.getEntity(), (ServerLevel) event.getEntity().level(),
					event.getTypedBrainBuilder(null));
		}
	}

	@SubscribeEvent
	public static void tickVillager(LivingTickEvent event) {

		if (event.getEntity() instanceof Villager && event.getEntity().level() instanceof ServerLevelAccessor) {
			// if this villager has already had the mind modified
			if (event.getEntity().getBrain().checkMemory(MemoryModuleTypes.PARTY_ID.get(), MemoryStatus.REGISTERED)) {
				VillagerTweaks.updatePartyStatus((Villager) event.getEntity(), (ServerLevel) event.getEntity().level());
			} else { // modify the mind
				VillagerTweaks.modifyMind((Villager) event.getEntity(), (ServerLevel) event.getEntity().level(), null);
			}
		}
	}
}