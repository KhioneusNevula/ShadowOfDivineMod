package com.gm910.sotdivine.events;

import java.util.Random;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.systems.deity.sphere.Spheres;
import com.gm910.sotdivine.systems.deity.type.IDeity;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party.resource.type.DimensionResource;
import com.gm910.sotdivine.systems.party_system.IPartySystem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

	@SubscribeEvent
	public static void playerJoin(PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			IPartySystem system = IPartySystem.get(player.level());
			if (system.getPartyByName(player.getUUID().toString()).isEmpty()) {
				system.addParty(IParty.createEntity(player.getUUID(), player.getDisplayName()), player.level());
				Random random = new Random(new Random(player.level().getSeed()).longs()
						.skip(system.allDeities().size() + 1).findAny().orElse(player.level().getSeed()));
				int deities = random.nextInt(Spheres.instance().getSphereMap().size(),
						Spheres.instance().getSphereMap().size() * 2);
				for (int mama = 0; mama < deities; mama++) {
					IDeity dimde = IDeity.generateDeity(player.level(), random, system);
					if (system.dimensionOwners(Level.NETHER).count() == 0) {
						dimde.setResourceAmount(new DimensionResource(Level.NETHER), 1);
					}
				}
			} else {
				system.markDirty(player.level());
			}
		}
	}
}