package com.gm910.sotdivine.magic.theophany.client;

import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.gm910.sotdivine.common.effects.ModEffects;
import com.gm910.sotdivine.common.effects.types.MeditationEffect;
import com.gm910.sotdivine.events.ClientEvents;
import com.gm910.sotdivine.magic.theophany.cap.ImpressionTimetracker;
import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionHolder;
import com.gm910.sotdivine.magic.theophany.impression.MentalState;
import com.gm910.sotdivine.network.ModNetwork;
import com.gm910.sotdivine.network.packet_types.ClientboundImpressionsUpdatePacket;
import com.gm910.sotdivine.network.packet_types.ServerboundImpressionsUpdatePacket;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.logging.LogUtils;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

/**
 * A theophany is an interaction that introduces a player to a deity's
 * impression, typically by saving them from harm
 */
public class ImpressionsClient {

	public static final ResourceLocation IMPRESSION_LAYER = ModUtils.path("impessions_layer");
	private final static Map<IImpression, ImpressionNode> IMPRESSIONS = new HashMap<>();

	/**
	 * The node currently held by the mouse
	 */
	private static ImpressionNode GRABBED = null;

	private final static AffineTransform TRANSFORM = new AffineTransform();

	private static void removeNode(IImpression imp) {
		if (IMPRESSIONS.remove(imp) instanceof ImpressionNode node) {
			if (node.getMount() instanceof ImpressionNode mount) {
				mount.getInputs().remove(node);
			}
			for (ImpressionNode child : new ArrayList<>(node.getInputs())) {
				child.setMountAndUpdate(null);
			}
		}
	}

	/**
	 * returns a stream of all impressions
	 * 
	 * @return
	 */
	public static Stream<IImpression> allImpressions() {
		return IMPRESSIONS.keySet().stream();

	}

	public static void renderTick(GuiGraphics graphics, DeltaTracker tracker) {
		renderMeditation(graphics, tracker);
		graphics.drawCenteredString(Minecraft.getInstance().font, "currentGui: " + Minecraft.getInstance().screen,
				graphics.guiWidth() / 2, 10, Color.white.getRGB());
		graphics.drawCenteredString(Minecraft.getInstance().font, "meditating: " + ClientEvents.isMeditating(),
				graphics.guiWidth() / 2, 20, Color.white.getRGB());
		Minecraft minecraft = Minecraft.getInstance();
		Collection<IImpression> collection = IMPRESSIONS.keySet();
		double ySin = 0;
		double xSin = 0;
		int idx = 0;
		if (!collection.isEmpty() && doesScreenShowImpressions(Minecraft.getInstance().screen)) {
			for (IImpression imp : Ordering
					.<IImpression>from(
							(e1, e2) -> e1.getImpressionType().path().compareTo(e2.getImpressionType().path()))
					.reverse().sortedCopy(collection)) {
				ImpressionNode node = IMPRESSIONS.get(imp);

				Shape toShape = imp.calculateShape(graphics, idx, collection.size(), tracker, getMentalState(),
						node.getTimeInfo(), node.getTag());
				idx++;

				if (toShape != null) {
					double diameter = Math.sqrt(Math.pow(toShape.getBounds2D().getWidth(), 2)
							+ Math.pow(toShape.getBounds2D().getHeight(), 2)) / 2;

					if (node.getMountOffset() != null) {
						double mx = node.getMount() == null ? mouseX() : node.getMount().getX();
						double my = node.getMount() == null ? mouseY() : node.getMount().getY();
						node.setX(mx + node.getMountOffset().getX());
						node.setY(my + node.getMountOffset().getY());

						TRANSFORM.setToTranslation(node.getX(), node.getY());
						Shape collisionShape = TRANSFORM.createTransformedShape(toShape);
						node.setShape(collisionShape);

						node.setCollidingWith(collidingWith(node, false));
					} else {

						ySin = ((Mth.sin(minecraft.level.getGameTime() * 0.003f * (Mth.sin(idx)) + idx * idx) + 1)
								* graphics.guiHeight() / 3f) + graphics.guiHeight() / 12;
						xSin = (((idx % 2 == 0 ? -1 : 1)
								* Mth.cos(minecraft.level.getGameTime() * 0.006f * Mth.sin(idx) + idx * idx) + 1)
								* graphics.guiWidth() / 3f) + graphics.guiWidth() / 6;
						double distance = Point.distance(node.getX(), node.getY(), xSin, ySin);
						if (distance > diameter) {
							double xFactor = (xSin - node.getX()) / (Math.pow(distance, 4));
							double yFactor = (ySin - node.getY()) / (Math.pow(distance, 4));
							node.setX((node.getX() + xFactor));
							node.setY((node.getY() + yFactor));
						} else {
							node.setX(xSin);
							node.setY(ySin);
						}

						TRANSFORM.setToTranslation(node.getX(), node.getY());

						Shape collisionShape = TRANSFORM.createTransformedShape(toShape);
						node.setShape(collisionShape);
						node.setCollidingWith(collidingWith(node, true));
						double vX = 0;
						double vY = 0;

						for (var collider : node.collidingWith()) {
							double diameter2 = Math.sqrt(Math.pow(collider.getShape().get().getBounds2D().getWidth(), 2)
									+ Math.pow(collider.getShape().get().getBounds2D().getHeight(), 2)) / 2;
							if (diameter <= diameter2) {
								vX += collisionShape.getBounds2D().getCenterX()
										- collider.getShape().get().getBounds2D().getCenterX();
								vY += collisionShape.getBounds2D().getCenterY()
										- collider.getShape().get().getBounds2D().getCenterY();
							}
						}

						if (vX != 0 || vY != 0) {
							double norm = 1 / Math.sqrt(vX * vX + vY * vY) * diameter / 2;
							vX *= norm;
							vY *= norm;
							node.setX(node.getX() + vX);
							node.setY(node.getY() + vY);

							TRANSFORM.setToTranslation(node.getX(), node.getY());
							node.setShape(TRANSFORM.createTransformedShape(toShape));

							node.setCollidingWith(collidingWith(node, false));
						}

					}

				} else {
					node.setX(0);
					node.setY(0);
					node.setShape(null);
				}

				if (node.impression().canClick(getMentalState(), node.getTimeInfo())) {
					node.getShape().ifPresent(clickShape -> {

						if (node.checkClicked()) {
							// on click
							if (GRABBED == null || GRABBED == node) {
								node.setMountAndUpdate(null);
								GRABBED = node;
								node.setMountOffset(new Point2D.Double(node.getX() - mouseX(), node.getY() - mouseY()));
							}
						} else {
							if (GRABBED == node)
								GRABBED = null;
							if (node.getMountOffset() != null) {
								// just un-clicked
								if (node.getMount() == null) {
									// if double-clicked
									if (node.wasDoubleClicked(Minecraft.getInstance().level.getGameTime())
											&& node.impression().canActivate(Minecraft.getInstance().player,
													getMentalState(), node.getTimeInfo())) {
										ModNetwork.sendToServer(
												ServerboundImpressionsUpdatePacket.activate(createHolder(node)));
									}
									node.setLastClickedTick(Minecraft.getInstance().level.getGameTime());

									boolean did = false;
									for (var collider : node.collidingWith()) {
										int argIdx = ImpressionNode.canBeNextInput(node, collider);
										if (argIdx >= 0) {
											LogUtils.getLogger().debug("Possible input node: " + node.impression()
													+ " for " + collider.impression() + " at position " + argIdx);
											if (node.setMountAndUpdate(collider)) {
												Point2D off = collider.getInputMountOffset(argIdx);
												node.setMountOffset(off);
												collider.getInputs().add(node);
												did = true;
											}
										} else {
											argIdx = ImpressionNode.canBeNextInput(collider, node);
											if (argIdx >= 0) {
												LogUtils.getLogger()
														.debug("Possible function " + node.impression()
																+ " for input node " + collider.impression()
																+ " at position " + argIdx);
												Point2D off = node.getInputMountOffset(argIdx);
												collider.setMountOffset(off);
												collider.setMountAndUpdate(node);
												node.getInputs().add(collider);
											}
										}
									}
									if (!did)
										node.setMountOffset(null);
								}
							}
						}
					});
				}

				imp.render(graphics, idx, collection.size(), tracker, node.getShape(), getMentalState(),
						node.getTimeInfo(), node.getTag());

			}
		}
	}

	private static void renderMeditation(GuiGraphics graphics, DeltaTracker tracker) {
		if (Minecraft.getInstance().player.getEffect(ModEffects.MEDITATING.getHolder().get()) == null) {
			return;
		}

		Profiler.get().push("meditate");
		graphics.nextStratum();
		Color destinationColor = new Color(0f, 0f, 0f, 0.7f);
		Color startColor = new Color(0f, 0f, 0f, 0);
		float f = ((Minecraft.getInstance().level.getGameTime() - ClientEvents.getMeditationStartTime())
				/ (float) MeditationEffect.USUAL_TIME);
		if (f >= 1) {
			if (ClientEvents.isMeditating()) {
				f = 1;
			} else {
				f = (Minecraft.getInstance().player.getEffect(ModEffects.MEDITATING.getHolder().get()).getDuration()
						/ (float) MeditationEffect.USUAL_TIME);
			}
		}
		f *= 100;
		float f1 = f / 100.0F;
		if (f1 > 1.0F) {
			f1 = 1.0F - (f - 100.0F) / 10.0F;
		}

		int i = (int) (220.0F * f1) << 24 | 1052704;
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), i);
		Profiler.get().pop();
	}

	private static Collection<ImpressionNode> collidingWith(ImpressionNode with, boolean ignoreMouseNode) {
		List<ImpressionNode> col = new ArrayList<>();
		for (ImpressionNode node : IMPRESSIONS.values()) {
			if (node == with)
				continue;
			if (ignoreMouseNode && node == GRABBED)
				continue;
			if (ImpressionNode.colliding(node, with)) {
				col.add(node);
			}
		}
		return col;
	}

	private static ImpressionHolder createHolder(ImpressionNode node) {
		if (node.getInputs().isEmpty()) {
			return new ImpressionHolder(node.impression());
		}
		return new ImpressionHolder(node.impression(), node.getInputs().stream().map((s) -> createHolder(s)).toList());
	}

	private static boolean doesScreenShowImpressions(Screen screen) {
		if (screen instanceof LevelLoadingScreen) {
			return true;
		}
		if (Minecraft.getInstance().level != null
				&& Minecraft.getInstance().level.isLoaded(Minecraft.getInstance().player.blockPosition()))
			return true;
		return false;
	}

	/**
	 * Return the player's mental state
	 * 
	 * @return
	 */
	public static MentalState getMentalState() {
		if (ClientEvents.isMeditating()) {
			return MentalState.MEDITATING;
		}
		var player = Minecraft.getInstance().player;
		if (player.isSleeping())
			return MentalState.ASLEEP;
		return MentalState.AWAKE;
	}

	/**
	 * For convenience...
	 * 
	 * @param graphics
	 * @param pipeline
	 * @param tex
	 * @param x
	 * @param y
	 * @param u
	 * @param v
	 * @param width
	 * @param height
	 * @param uvWidth
	 * @param uvHeight
	 * @param color
	 */
	public static void blit(GuiGraphics graphics, RenderPipeline pipeline, ResourceLocation tex, int x, int y, float u,
			float v, int width, int height, int uvWidth, int uvHeight, int color) {
		graphics.blit(pipeline, tex, x, y, u, v, width, height, uvWidth, uvHeight, color);
	}

	/**
	 * If we clicked inside the given rectangle
	 * 
	 * @param rect
	 * @return
	 */
	public static boolean clickedRectangle(Rectangle2D rect) {
		return mouseAvailable() && mouseDown() && rect.contains(mouseX(), mouseY());
	}

	/**
	 * If we clicked inside the given circle
	 * 
	 * @param centerX
	 * @param centerY
	 * @param radius
	 * @return
	 */
	public static boolean clickedCircle(Ellipse2D ellipse) {
		return mouseAvailable() && mouseDown() && ellipse.contains(mouseX(), mouseY());
	}

	/**
	 * If we clicked inside the given shape
	 */
	public static boolean clickedShape(Shape shape) {
		return mouseAvailable() && mouseDown() && shape.contains(mouseX(), mouseY());
	}

	/**
	 * Whether the mouse is able to move around
	 */
	public static boolean mouseAvailable() {
		return !Minecraft.getInstance().mouseHandler.isMouseGrabbed();
	}

	/**
	 * Mouse X position
	 * 
	 * @return
	 */
	public static double mouseX() {
		return Minecraft.getInstance().mouseHandler.getScaledXPos(Minecraft.getInstance().getWindow());
	}

	/**
	 * Mouse Y position
	 * 
	 * @return
	 */
	public static double mouseY() {
		return Minecraft.getInstance().mouseHandler.getScaledYPos(Minecraft.getInstance().getWindow());
	}

	/**
	 * If the left button of the mouse is down
	 * 
	 * @return
	 */
	public static boolean mouseDown() {
		return ClientEvents.leftMousePressed();
	}

	/**
	 * If the left button of the mouse is down while not in a gui
	 * 
	 * @return
	 */
	public static boolean mouseDownWhilePlaying() {
		return Minecraft.getInstance().mouseHandler.isLeftPressed();
	}

	/**
	 * Handle received packet
	 * 
	 * @param x
	 * @param y
	 */
	public static void handlePackageFromServer(ClientboundImpressionsUpdatePacket x, Context y) {
		y.setPacketHandled(true);
		switch (x.action()) {
		case ADD:
			int xpos = 0;
			int ypos = 0;
			IMPRESSIONS.put(x.impression().orElseThrow(), new ImpressionNode(x.impression().get(),
					x.additionalInfo().orElse(ImpressionTimetracker.DEFAULT), xpos, ypos));
			break;
		case REMOVE:
			removeNode(x.impression().orElseThrow());
			break;
		case CLEAR:
			IMPRESSIONS.clear();
			break;
		}
	}
}
