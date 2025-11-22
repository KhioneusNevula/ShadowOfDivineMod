package com.gm910.sotdivine.events;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.network.party_system.ClientParties;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.datafixers.util.Either;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Layer;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

	/**
	 * We want to make a banner show what deity its symbols are associated to
	 * 
	 * @param event
	 */
	@SubscribeEvent
	public static void tooltipEvent(RenderTooltipEvent.GatherComponents event) {
		if (event.getItemStack().getComponents()
				.get(DataComponents.BANNER_PATTERNS) instanceof BannerPatternLayers layers) {
			for (int i = 0; i < event.getTooltipElements().size(); i++) {
				var tooltip = event.getTooltipElements().get(i);
				if (tooltip.left().orElse(null) instanceof FormattedText text) {
					if (layers.layers().stream().filter((l) -> l.description().getString().equals(text.getString()))
							.findAny().orElse(null) instanceof Layer layer) {
						var deities = ClientParties.instance().get().deitiesByPattern(layer.pattern().get())
								.toList();
						event.getTooltipElements().set(i,
								Either.left(TextUtils.transPrefix("sotd.tooltip.deity.symbol_of", text,
										deities.stream().map(
												(s) -> s.descriptiveName().orElse(Component.literal(s.uniqueName())))
												.collect(CollectionUtils.componentCollectorCommasPretty()))));
					}
				}
			}
		}
	}
}
