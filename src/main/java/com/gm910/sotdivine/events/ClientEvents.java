package com.gm910.sotdivine.events;

import com.gm910.sotdivine.ModKeys;
import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.effects.ModEffects;
import com.gm910.sotdivine.magic.theophany.client.ImpressionsClient;
import com.gm910.sotdivine.magic.theophany.client.MeditationScreen;
import com.gm910.sotdivine.network.ModNetwork;
import com.gm910.sotdivine.network.packet_types.ServerboundImpressionsUpdatePacket;
import com.gm910.sotdivine.network.packet_types.ServerboundMeditationPacket;
import com.gm910.sotdivine.network.party_system.ClientParties;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Layer;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed;
import net.minecraftforge.client.event.ScreenEvent.MouseButtonReleased;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.BOTH)
public class ClientEvents {

	private static boolean leftPressed;
	private static boolean rightPressed;

	private static boolean meditating = false;
	private static long meditationStartTime = -1;

	/**
	 * This is true if the meditation key has not been released recently
	 */
	private static boolean medKeyStuck = false;

	public static void startMeditatingAndChangeScreen(boolean requireKey) {
		setMeditatingAndChangeScreen(true, requireKey);
	}

	public static void stopMeditatingAndChangeScreen() {
		setMeditatingAndChangeScreen(false, false);
	}

	public static void setMeditatingAndChangeScreen(boolean meditating, boolean requireKey) {
		ClientEvents.meditating = meditating;
		if (meditating) {
			LogUtils.getLogger().debug("Started meditating.");
			if (!(Minecraft.getInstance().screen instanceof MeditationScreen)) {
				boolean keyDown = ModKeys.KEY_MEDITATION.get().isDown();
				Minecraft.getInstance().setScreen(new MeditationScreen(requireKey));
				meditationStartTime = Minecraft.getInstance().level.getGameTime();
				if (keyDown && requireKey)
					ModKeys.KEY_MEDITATION.get().setDown(true);
			}
		} else {
			LogUtils.getLogger().debug("Stopped meditating.");
			if (Minecraft.getInstance().screen instanceof MeditationScreen) {
				boolean keyDown = ModKeys.KEY_MEDITATION.get().isDown();
				Minecraft.getInstance().setScreen(null);
				if (keyDown)
					ModKeys.KEY_MEDITATION.get().setDown(true);
				Minecraft.getInstance().mouseHandler.releaseMouse();
				Minecraft.getInstance().schedule(() -> Minecraft.getInstance().mouseHandler.grabMouse());
				meditationStartTime = -1;
			}

		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {

		if (ModKeys.KEY_MEDITATION.get().isDown() && !medKeyStuck) {
			if (!meditating) {
				ModNetwork.sendToServer(ServerboundMeditationPacket.startMeditating());
				startMeditatingAndChangeScreen(true);
			}
		}

		if (!ModKeys.KEY_MEDITATION.get().isDown()) {
			medKeyStuck = false;
		} else {
			medKeyStuck = true;
		}

		if (Minecraft.getInstance().player != null
				&& !Minecraft.getInstance().player.hasEffect(ModEffects.MEDITATING.getHolder().get())) {
			meditating = false;
			meditationStartTime = -1;

		}
	}

	@SubscribeEvent
	public static void click(MouseButtonPressed.Post pressEvent) {

		if (!pressEvent.wasHandled() && !pressEvent.getResult().isDenied()) {
			if (pressEvent.getButton() == 0) {
				leftPressed = true;
			}
			if (pressEvent.getButton() == 1) {
				rightPressed = true;
			}
		}

	}

	@SubscribeEvent
	public static void releaseClick(MouseButtonReleased.Pre pressEvent) {
		if (pressEvent.getButton() == 0) {
			leftPressed = false;
		}
		if (pressEvent.getButton() == 1) {
			rightPressed = false;
		}
	}

	@SubscribeEvent
	public static void releaseClick(ScreenEvent.Closing pressEvent) {
		leftPressed = false;
		rightPressed = false;
	}

	public static long getMeditationStartTime() {
		return meditationStartTime;
	}

	public static boolean isMeditating() {
		return meditating;
	}

	public static boolean leftMousePressed() {
		return leftPressed;
	}

	public static boolean rightMousePressed() {
		return rightPressed;
	}

	/**
	 * Want to show impressions floating on screen
	 * 
	 * @param event
	 */
	@SubscribeEvent
	public static void overlayAddingEvent(AddGuiOverlayLayersEvent event) {
		event.getLayeredDraw().addAbove(ImpressionsClient.IMPRESSION_LAYER, ForgeLayeredDraw.SLEEP_OVERLAY,
				ImpressionsClient::renderTick);
	}

	/** TODO send a {@link ServerboundImpressionsUpdatePacket} */
	@SubscribeEvent
	public static void tick(LevelTickEvent.Post event) {
		if (event.side == LogicalSide.CLIENT && Minecraft.getInstance().level.getGameTime() % 10 == 0) {
		}
	}

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
						var deities = ClientParties.instance().get().deitiesByPattern(layer.pattern().get()).toList();
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
