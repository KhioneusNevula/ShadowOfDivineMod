package com.gm910.sotdivine.events;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.concepts.symbol.ISymbolBearer;
import com.gm910.sotdivine.concepts.symbol.impl.BannerSymbolBearer;
import com.gm910.sotdivine.concepts.symbol.impl.ISymbolWearer;
import com.gm910.sotdivine.concepts.symbol.impl.ItemStackSymbolBearer;
import com.gm910.sotdivine.concepts.symbol.impl.LivingEntitySymbolWearer;
import com.gm910.sotdivine.magic.afterlife.Afterlife;
import com.gm910.sotdivine.magic.afterlife.SoulState;
import com.gm910.sotdivine.magic.impression.cap.IMindsEye;
import com.gm910.sotdivine.magic.impression.cap.MindsEye;
import com.gm910.sotdivine.magic.afterlife.IAfterlife;
import com.gm910.sotdivine.magic.afterlife.ISoulState;
import com.gm910.sotdivine.magic.sanctuary.cap.ISanctuaryInfo;
import com.gm910.sotdivine.magic.sanctuary.cap.SanctuaryInfo;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityEvents {

	@SubscribeEvent
	public static void attachCaps(PlayerEvent.Clone event) {
		if (event.isWasDeath()) {
			LogUtils.getLogger().debug("Reattaching caps to player after death ");
			event.getEntity().getCapability(IMindsEye.CAPABILITY)
					.ifPresent((cap) -> event.getOriginal().getCapability(IMindsEye.CAPABILITY)
							.ifPresent((ocap) -> cap.deserializeNBT(event.getEntity().registryAccess(),
									ocap.serializeNBT(event.getEntity().registryAccess()))));

		}
	}

	@SubscribeEvent
	public static void attachCaps(AttachCapabilitiesEvent.Levels event) {
		if (event.getObject() instanceof ServerLevel level) {
			if (level.dimension().equals(Level.OVERWORLD)) {
				event.addCapability(IAfterlife.CAPABILITY_PATH, new Afterlife(level));
				LogUtils.getLogger().debug("Attachin afterlife  cap to level ");
			}
		}
	}

	@SubscribeEvent
	public static void attachCaps(AttachCapabilitiesEvent.BlockEntities event) {
		if (event.getObject() instanceof BannerBlockEntity ban) {
			event.addCapability(ISymbolBearer.CAPABILITY_PATH, new BannerSymbolBearer(ban));
		}
	}

	@SubscribeEvent
	public static void attachCaps(AttachCapabilitiesEvent.Entities event) {
		if (event.getObject() instanceof LivingEntity ban) {
			event.addCapability(ISymbolWearer.CAPABILITY_PATH, new LivingEntitySymbolWearer(ban));
			event.addCapability(ISanctuaryInfo.CAPABILITY_PATH, new SanctuaryInfo(ban));
			event.addCapability(IMindsEye.CAPABILITY_PATH, new MindsEye(ban));
			event.addCapability(ISoulState.CAPABILITY_PATH, new SoulState(ban));
			if (ban instanceof ServerPlayer) {
				LogUtils.getLogger().debug("Attaching caps to player ");
			}

		}
	}

	@SubscribeEvent
	public static void attachCaps(AttachCapabilitiesEvent.ItemStacks event) {
		event.addCapability(ISymbolBearer.CAPABILITY_PATH, new ItemStackSymbolBearer(event.getObject()));

	}

}