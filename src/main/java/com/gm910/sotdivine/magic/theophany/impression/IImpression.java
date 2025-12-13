package com.gm910.sotdivine.magic.theophany.impression;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.gm910.sotdivine.magic.theophany.cap.ImpressionTimetracker;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * A piece of magical "information" that hovers in your mind (HUD) pertaining to
 * a deity. Should be immutable.
 */
@Immutable
public interface IImpression {

	/**
	 * Returns what to show for this impression when it is printed; if the given
	 * levelRef argument is null, you can assume this is being called from
	 * client-side
	 * 
	 * @param levelRef
	 * @param inputs   if this has any inputs, the inputs
	 * @return
	 */
	public Component getPrintOutput(@Nullable ServerLevel levelRef, List<ImpressionHolder> inputs);

	/**
	 * Activate this impression's effect, if possible
	 * 
	 * @param level
	 * @param activator
	 * @param inputs
	 */
	public void activate(ServerLevel level, LivingEntity activator, List<ImpressionHolder> inputs,
			ImpressionTimetracker instance);

	/**
	 * If this can activate. May be called on either client or server
	 * 
	 * @param mentalState
	 * @param timeInfo
	 * @param tag
	 * @return
	 */
	public default boolean canActivate(Player player, MentalState mentalState, ImpressionTimetracker timeInfo) {
		return this.getImpressionType().usage() != Usage.KNOWLEDGE;
	}

	/**
	 * If this can be clicked (and dragged).
	 * 
	 * @param mentalState
	 * @param timeInfo
	 * @param tag
	 * @return
	 */
	public default boolean canClick(MentalState mentalState, ImpressionTimetracker timeInfo) {
		return mentalState == MentalState.ASLEEP || mentalState == MentalState.MEDITATING;
	}

	/**
	 * Return what kinds of inputs this requires. Use {@link ImpressionType#ANY} for
	 * an input accepting anything
	 * 
	 * @return
	 */
	public List<ImpressionType<?>> requireInputs();

	/**
	 * Return the type of impression this is
	 * 
	 * @return
	 */
	public ImpressionType<?> getImpressionType();

	/**
	 * Render info about the impression's contents, i.e. when hovered over
	 * 
	 * @param index    what position in sequence this is being drawn
	 * @param graphics
	 * @param maxRect  max size this can take up
	 */
	public void showInformation(GuiGraphics graphics, int index, Rectangle maxRect, MentalState state,
			ImpressionTimetracker instance);

	/**
	 * Determines the shape of this impression's "click box/collision box" to decide
	 * other calculations; place the shape at (0,0)
	 * 
	 * @param index       what position in sequence this is being drawn
	 * @param outOf       how many other impressions are being drawn (for color
	 *                    effects and such)
	 * @param state       what mental state we are in
	 * @param clicked     whether the impression's rectangle is being clicked
	 * @param maxRect     max size this can tak
	 * @param storedState stored info about this impression to allow it to have some
	 *                    persistent info while beign rendered
	 * @param gui
	 * @return a shape representing the most salient element of this impression, or
	 *         null if the impression cannot be interpreted that way
	 */
	public Shape calculateShape(GuiGraphics graphics, int index, int numberOfNodes, DeltaTracker tracker,
			MentalState state, ImpressionTimetracker instance, CompoundTag storedState);

	/**
	 * Draw this impression somewhere on the screen while not consciously viewing it
	 * 
	 * @param index           what position in sequence this is being drawn
	 * @param outOf           how many other impressions are being drawn (for color
	 *                        effects and such)
	 * @param state           what mental state we are in
	 * @param clicked         whether the impression's rectangle is being clicked
	 * @param calculatedShape the shape returned by
	 *                        {@link #calculateShape(GuiGraphics, int, int, DeltaTracker, MentalState, ImpressionTimetracker, CompoundTag)}
	 * @param storedState     stored info about this impression to allow it to have
	 *                        some persistent info while beign rendered
	 * @param gui
	 */
	public void render(GuiGraphics graphics, int index, int outOf, DeltaTracker tracker,
			Optional<Shape> calculatedShape, MentalState state, ImpressionTimetracker instance,
			CompoundTag storedState);

	public static enum Usage {
		/** An impression which is nothing but a piece of info */
		KNOWLEDGE,
		/**
		 * An impression which finds a piece of information by creating a new impression
		 */
		WONDER,
		/** An impression which has an effect on the world */
		POWER
	}

}
