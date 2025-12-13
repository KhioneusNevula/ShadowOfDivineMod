package com.gm910.sotdivine.magic.theophany.impression.types;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gm910.sotdivine.magic.theophany.cap.ImpressionTimetracker;
import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionHolder;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionType;
import com.gm910.sotdivine.magic.theophany.impression.MentalState;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Debug impression that can require any number of arguments and prints each of
 * them when activated
 */
public class DebugPrintImpression implements IImpression {

	private int argCount;
	private Component name;
	private List<ImpressionType<?>> list;

	public static final int MAX_ARGS = 10;

	public static Codec<DebugPrintImpression> codec() {
		return RecordCodecBuilder.create(inst -> inst
				.group(ComponentSerialization.CODEC.fieldOf("name").forGetter(a -> a.name),
						Codec.INT.fieldOf("arg_count").forGetter(a -> a.argCount))
				.apply(inst, DebugPrintImpression::new));
	}

	public static StreamCodec<RegistryFriendlyByteBuf, DebugPrintImpression> streamCodec() {
		return StreamCodec.composite(ComponentSerialization.STREAM_CODEC, DebugPrintImpression::getName,
				ByteBufCodecs.INT, DebugPrintImpression::getArgCount, DebugPrintImpression::new);
	}

	public DebugPrintImpression(Component name, int argCount) {
		assert argCount > 0 && argCount <= MAX_ARGS;
		this.argCount = argCount;
		this.name = name;
		this.list = new ArrayList<>(argCount);
		for (int i = 0; i < argCount; i++) {
			list.add(ImpressionType.ANY);
		}
		list = ImmutableList.copyOf(list);
	}

	public Component getName() {
		return name;
	}

	public int getArgCount() {
		return argCount;
	}

	@Override
	public boolean canActivate(Player player, MentalState mentalState, ImpressionTimetracker timeInfo) {
		return true;
	}

	@Override
	public void activate(ServerLevel level, LivingEntity activator, List<ImpressionHolder> inputs,
			ImpressionTimetracker instance) {
		if (activator instanceof ServerPlayer sp) {
			sp.sendSystemMessage(this.getPrintOutput(level, inputs));
		} else {
			level.players()
					.forEach(
							(sp) -> sp
									.sendSystemMessage(Component.translatable("chat.type.text",
											activator.hasCustomName() ? activator.getCustomName()
													: (Component.translatable("sotd.cmd.parenthetical",
															activator.getName(), activator.getUUID())),
											this.getPrintOutput(level, inputs))));
		}
	}

	@Override
	public List<ImpressionType<?>> requireInputs() {
		return list;
	}

	@Override
	public ImpressionType<?> getImpressionType() {
		return ImpressionType.DEBUG.get();
	}

	@Override
	public void showInformation(GuiGraphics graphics, int index, Rectangle maxRect, MentalState state,
			ImpressionTimetracker instance) {

	}

	private final Component displayText() {
		return Component.translatable("sotd.cmd.parenthetical", name, argCount);
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
			float hue1 = (float) (Math.sin(name.hashCode() * 0.1f));
			float hue2 = argCount / (float) MAX_ARGS;
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
	public int hashCode() {
		return name.hashCode() + Integer.hashCode(argCount);
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof DebugPrintImpression di)
			return this.name.equals(di.name) && this.argCount == di.argCount;
		return false;
	}

	@Override
	public String toString() {
		return "debugImp[\"" + name.getString() + "\"](" + argCount + ")";
	}

	@Override
	public Component getPrintOutput(ServerLevel levelRef, List<ImpressionHolder> inputs) {
		return Component.empty().append(name).append(TextUtils.transPrefix("sotd.cmd.bracket", inputs.stream().map(
				(i) -> Component.translatable("sotd.cmd.quote", i.impression().getPrintOutput(levelRef, i.inputs())))
				.collect(CollectionUtils.componentCollectorCommas())));
	}

}
