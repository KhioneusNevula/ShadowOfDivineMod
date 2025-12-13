package com.gm910.sotdivine.magic.theophany.client;

import com.gm910.sotdivine.ModKeys;
import com.gm910.sotdivine.events.ClientEvents;
import com.gm910.sotdivine.network.ModNetwork;
import com.gm910.sotdivine.network.packet_types.ServerboundMeditationPacket;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * To release mouse while meditating
 */
public class MeditationScreen extends Screen {

	private static final int GRACE_PERIOD = 1;

	private int ticksPassedWithoutKeydown = 0;

	public MeditationScreen(boolean requireKey) {
		super(Component.translatable("sotd.meditation.title"));
		if (!requireKey)
			ticksPassedWithoutKeydown = -1;
	}

	public boolean requireKey() {
		return ticksPassedWithoutKeydown >= 0;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !requireKey();
	}

	@Override
	public void tick() {
		super.tick();
		if (requireKey()) {
			if (ModKeys.KEY_MEDITATION.get().isDown()) {
				ticksPassedWithoutKeydown = 0;
			} else {
				ticksPassedWithoutKeydown++;
				if (ticksPassedWithoutKeydown > GRACE_PERIOD) {
					ModNetwork.sendToServer(ServerboundMeditationPacket.stopMeditating());
					ClientEvents.stopMeditatingAndChangeScreen();
				}
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics p_283688_, int p_299421_, int p_298679_, float p_297268_) {

	}

}
