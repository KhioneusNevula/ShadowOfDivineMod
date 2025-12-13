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
import com.gm910.sotdivine.magic.theophany.cap.Mind;
import com.gm910.sotdivine.magic.theophany.cap.IMind;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityEvents {

	@SubscribeEvent
	public static void attachCaps(PlayerEvent.Clone event) {
		if (event.isWasDeath()) {
			event.getEntity().getCapability(IMind.CAPABILITY)
					.ifPresent((cap) -> event.getOriginal().getCapability(IMind.CAPABILITY)
							.ifPresent((ocap) -> cap.deserializeNBT(event.getEntity().registryAccess(),
									ocap.serializeNBT(event.getEntity().registryAccess()))));

		}
	}

	@SubscribeEvent
	public static void attachCaps(AttachCapabilitiesEvent.BlockEntities event) {
		if (event.getObject() instanceof BannerBlockEntity ban) {
			event.addCapability(ISymbolBearer.CAPABILITY_PATH, new BannerSymbolBearer(ban));
		}
	}

	@SubscribeEvent
	public static void attachCaps2(AttachCapabilitiesEvent.Entities event) {
		if (event.getObject() instanceof LivingEntity ban) {
			event.addCapability(ISymbolWearer.CAPABILITY_PATH, new LivingEntitySymbolWearer(ban));
			event.addCapability(ISanctuaryInfo.CAPABILITY_PATH, new SanctuaryInfo(ban));
			event.addCapability(IMind.CAPABILITY_PATH, new Mind(ban));
			if (ban instanceof ServerPlayer) {
				LogUtils.getLogger().debug("Attaching caps to player ");
			}

		}
	}

	@SubscribeEvent
	public static void attachCaps3(AttachCapabilitiesEvent.ItemStacks event) {
		event.addCapability(ISymbolBearer.CAPABILITY_PATH, new ItemStackSymbolBearer(event.getObject()));

	}

}