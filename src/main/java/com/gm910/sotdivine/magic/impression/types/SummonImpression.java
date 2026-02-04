package com.gm910.sotdivine.magic.impression.types;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;

import com.gm910.sotdivine.magic.afterlife.ISoulState.LifeState;
import com.gm910.sotdivine.magic.afterlife.Soul;
import com.gm910.sotdivine.magic.impression.IImpression;
import com.gm910.sotdivine.magic.impression.ImpressionHolder;
import com.gm910.sotdivine.magic.impression.ImpressionType;
import com.gm910.sotdivine.magic.impression.MentalState;
import com.gm910.sotdivine.magic.impression.cap.ImpressionTimetracker;
import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Resurrects or teleports an entity to the user
 */
public enum SummonImpression implements IImpression {
	INSTANCE;

	public static Codec<SummonImpression> codec() {
		return Codec.unit(INSTANCE);
	}

	public static StreamCodec<RegistryFriendlyByteBuf, SummonImpression> streamCodec() {
		return StreamCodec.unit(INSTANCE);
	}

	@Override
	public boolean canActivate(Player player, MentalState mentalState, ImpressionTimetracker timeInfo) {
		return true;
	}

	@Override
	public void activate(ServerLevel level, LivingEntity activator, List<ImpressionHolder> inputs,
			ImpressionTimetracker instance) {
		inputs.get(0).impression().filter(s -> s.getImpressionType() == ImpressionType.SOUL.get())
				.map(s -> (SoulImpression) s).ifPresent(si -> {
					Soul soul = si.soul();

					Vec3 bringPos = activator.position().add(activator.getLookAngle().x() * 2, 0,
							activator.getLookAngle().z() * 2);
					soul.getOrCreateEntity(level, LifeState.ghost, true).ifPresentOrElse(en -> {
						if (en.distanceTo(activator) > 10) {
							en.teleport(new TeleportTransition(level, bringPos, Vec3.ZERO, 0, 0.0F,
									Relative.union(Relative.DELTA, Relative.ROTATION),
									TeleportTransition.PLAY_PORTAL_SOUND));
						}
					}, () -> {
						if (activator instanceof ServerPlayer splaya) {
							splaya.sendSystemMessage(
									Component.translatable("sotd.summon.failed").withStyle(ChatFormatting.RED), false);
						}
					});
				});

	}

	@Override
	public List<ImpressionType<?>> requireInputs() {
		return List.of(ImpressionType.SOUL.get());
	}

	@Override
	public ImpressionType<?> getImpressionType() {
		return ImpressionType.SUMMON.get();
	}

	@Override
	public void showInformation(GuiGraphics graphics, int index, Rectangle maxRect, MentalState state,
			ImpressionTimetracker instance) {

	}

	private final Component displayText() {
		return Component.translatable("sotd.cmd.parenthetical", getPrintOutput(null, null));
	}

	@Override
	public Shape calculateShape(GuiGraphics graphics, int index, int numberOfNodes, DeltaTracker tracker,
			MentalState state, ImpressionTimetracker instance, CompoundTag storedState) {

		return new Rectangle2D.Double(0, 0, 27, Minecraft.getInstance().font.wordWrapHeight(displayText(), 24) + 3);
	}

	@Override
	public void render(GuiGraphics graphics, int index, int outOf, DeltaTracker tracker,
			Optional<Shape> calculatedShape, MentalState state, ImpressionTimetracker instance,
			CompoundTag storedState) {
		if (state == MentalState.MEDITATING) {
			int textWidth = 24;
			float hue1 = (float) (Math.sin(index * 0.1f));
			float hue2 = Minecraft.getInstance().level.getGameTime();
			Rectangle2D rect = calculatedShape.get().getBounds2D();
			graphics.fillGradient((int) rect.getMinX(), (int) rect.getMinY(), (int) rect.getMaxX(),
					(int) rect.getMaxY(), Color.getHSBColor(hue2, 1.0f, 1.0f).getRGB(),
					Color.getHSBColor(hue1, 1.0f, 1.0f).getRGB());
			graphics.drawWordWrap(Minecraft.getInstance().font, displayText(),
					(int) (rect.getMinX() + rect.getWidth() / 2 - textWidth / 2),
					(int) (rect.getMinY() + rect.getHeight() / 2
							- Minecraft.getInstance().font.wordWrapHeight(displayText(), 24) / 2),
					textWidth, Color.white.getRGB(), true);
		}
	}

	@Override
	public String toString() {
		return "SUMMON";
	}

	@Override
	public Component getPrintOutput(ServerLevel levelRef, List<ImpressionHolder> inputs) {
		return Component.translatable("sotd.impression.summon");
	}

}
