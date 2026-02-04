package com.gm910.sotdivine.magic.impression.client;

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
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.gm910.sotdivine.common.effects.ModEffects;
import com.gm910.sotdivine.common.effects.types.MeditationEffect;
import com.gm910.sotdivine.events.ClientEvents;
import com.gm910.sotdivine.magic.impression.IImpression;
import com.gm910.sotdivine.magic.impression.ImpressionHolder;
import com.gm910.sotdivine.magic.impression.MentalState;
import com.gm910.sotdivine.magic.impression.cap.ImpressionTimetracker;
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
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

/**
 * A theophany is an interaction that introduces a player to a deity's
 * impression, typically by saving them from harm
 */
public class ImpressionsClient {

	public static final ResourceLocation IMPRESSION_LAYER = ModUtils.path("impessions_layer");
	private final static Map<IImpression, ImpressionNode> IMPRESSIONS = new HashMap<>();

	private static Point2D.Double ORBIT_NEXUS = null;

	/**
	 * The node currently held by the mouse
	 */
	private static ImpressionNode GRABBED = null;

	private final static AffineTransform TRANSFORM = new AffineTransform();

	private static void removeNode(IImpression imp) {
		synchronized (IMPRESSIONS) {
			if (IMPRESSIONS.remove(imp) instanceof ImpressionNode node) {
				if (node.getMount() instanceof ImpressionNode mount) {
					mount.clearInput(node);
				}
				for (ImpressionNode child : node.getInputs().toList()) {
					child.setMountAndUpdate(null);
				}
			}
		}
	}

	private static void updateNode(IImpression k) {
		synchronized (IMPRESSIONS) {
			if (IMPRESSIONS.containsKey(k)) {
				var node = IMPRESSIONS.remove(k);
				node.setImpression(k);
				node.impression().onReceiveUpdateFromServer(getMentalState(), node.getTimeInfo());
				IMPRESSIONS.put(k, node);
			}
		}
	}

	/**
	 * Thread safe way to access an iterable of all impressions
	 * 
	 * @param function
	 */
	public static void runOnAllImpressions(Consumer<Stream<IImpression>> function) {
		synchronized (IMPRESSIONS) {
			function.accept(IMPRESSIONS.keySet().stream());
		}
	}

	public static void renderTick(GuiGraphics graphics, DeltaTracker tracker) {
		Minecraft minecraft = Minecraft.getInstance();

		ORBIT_NEXUS = new Point2D.Double(
				Math.cos(minecraft.level.getGameTime() * 0.01f) * graphics.guiWidth() / 4 + graphics.guiWidth() / 2,
				Math.sin(minecraft.level.getGameTime() * 0.02f) * graphics.guiHeight() / 4 + graphics.guiHeight() / 2);

		var profiler = Profiler.get();

		profiler.push("impressionGuiTick");
		profiler.push("drawMeditationScreen");
		renderMeditation(graphics, tracker);

		profiler.popPush("drawImpressionGuiDebug");

		graphics.drawCenteredString(Minecraft.getInstance().font, "currentGui: " + Minecraft.getInstance().screen,
				graphics.guiWidth() / 2, 10, Color.white.getRGB());
		graphics.drawCenteredString(Minecraft.getInstance().font, "meditating: " + ClientEvents.isMeditating(),
				graphics.guiWidth() / 2, 20, Color.white.getRGB());

		// graphics.fill((int) ORBIT_NEXUS.x - 12, (int) ORBIT_NEXUS.y - 12, (int)
		// ORBIT_NEXUS.x + 12,
		// (int) ORBIT_NEXUS.y + 12, Color.magenta.getRGB());

		profiler.popPush("drawingImpressions");

		List<ImpressionNode> iteratingImpressions = List.of();
		synchronized (IMPRESSIONS) {
			if (!IMPRESSIONS.isEmpty()) {
				iteratingImpressions = Ordering
						.<ImpressionNode>from((e1, e2) -> e1.impression().getImpressionType().path()
								.compareTo(e2.impression().getImpressionType().path()))
						.reverse().sortedCopy(IMPRESSIONS.values());
			}
		}

		if (!iteratingImpressions.isEmpty() && doesScreenShowImpressions(Minecraft.getInstance().screen)) {
			int idx = 0;
			for (var node : iteratingImpressions) {

				profiler.push(() -> node.impression().getImpressionType().path().toString());
				profiler.incrementCounter("drawImpression");

				IImpression imp = node.impression();

				profiler.push("calculateShape");
				Shape toShape = imp.calculateShape(graphics, idx, iteratingImpressions.size(), tracker,
						getMentalState(), node.getTimeInfo(), node.getTag());
				idx++;

				if (toShape != null) {
					double diameter = Math.sqrt(Math.pow(toShape.getBounds2D().getWidth(), 2)
							+ Math.pow(toShape.getBounds2D().getHeight(), 2)) / 2;

					profiler.popPush("evaluateNodePhysics");

					profiler.push("wallCollisions");
					if (normalVectorOfCollidedWall(graphics, node.getX() + node.getXVelocity(),
							node.getY() + node.getYVelocity()) instanceof Point2D.Double nvec) {

						double dp = node.getXVelocity() * nvec.getX() + node.getYVelocity() * nvec.getY();
						double newX = node.getXVelocity() - 2 * dp * nvec.getX();
						double newY = node.getYVelocity() - 2 * dp * nvec.getY();
						node.setVelocity(newX, newY);

					}
					node.setX(Math.clamp(node.getX() + node.getXVelocity(), 0,
							graphics.guiWidth() - toShape.getBounds().width));
					node.setY(Math.clamp(node.getY() + node.getYVelocity(), 0,
							graphics.guiHeight() - toShape.getBounds().height));

					if (node.getMountOffset() != null) {
						profiler.popPush("mouseBasedPosition");
						double mx = node.getMount() == null ? mouseX() : node.getMount().getX();
						double my = node.getMount() == null ? mouseY() : node.getMount().getY();
						node.setVelocity(0, 0);
						node.setX(mx + node.getMountOffset().getX());
						node.setY(my + node.getMountOffset().getY());

						TRANSFORM.setToTranslation(node.getX(), node.getY());
						Shape collisionShape = TRANSFORM.createTransformedShape(toShape);
						node.setShape(collisionShape);

						node.setCollidingWith(collidingWith(node, false));
					} else {
						profiler.popPush("orbitingPosition");
						double mag = 0.001f;
						double distToMouse = Point.distance(mouseX(), mouseY(), node.getX(), node.getY());
						double xSin = ((idx % 2) * (graphics.guiWidth() - ORBIT_NEXUS.getX() - ORBIT_NEXUS.getX())
								+ ORBIT_NEXUS.getX() - node.getX()) / distToMouse * mag * (Math.sin(idx) + 2);
						double ySin = ((node.impression().hashCode() % 2)
								* (graphics.guiHeight() - ORBIT_NEXUS.getY() - ORBIT_NEXUS.getY()) + ORBIT_NEXUS.getY()
								- node.getY()) / distToMouse * mag;

						node.setVelocity(node.getXVelocity() + xSin * Math.sqrt(idx + 1),
								node.getYVelocity() + ySin * Math.sqrt(idx + 1));

						TRANSFORM.setToTranslation(node.getX(), node.getY());

						Shape collisionShape = TRANSFORM.createTransformedShape(toShape);
						node.setShape(collisionShape);
						node.setCollidingWith(collidingWith(node, true));
						double vX = 0;
						double vY = 0;

						profiler.push("collisions");
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
							double norm = 1 / Math.sqrt(vX * vX + vY * vY);
							vX *= norm;
							vY *= norm;
							node.setVelocity(vX, vY);

							TRANSFORM.setToTranslation(node.getX(), node.getY());
							node.setShape(TRANSFORM.createTransformedShape(toShape));

							node.setCollidingWith(collidingWith(node, false));
						}
						profiler.pop();

					}

					node.getShape().ifPresent(clickShape -> {
						if (node.impression().canClick(getMentalState(), node.getTimeInfo())) {
							profiler.popPush("checkClicks");
							if (node.checkClicked()) {
								// on click
								profiler.push("clickPress");
								if (GRABBED == null || GRABBED == node) {
									node.setMountAndUpdate(null);
									GRABBED = node;
									node.setMountOffset(
											new Point2D.Double(node.getX() - mouseX(), node.getY() - mouseY()));
								}
								profiler.pop();
							} else {
								if (GRABBED == node)
									GRABBED = null;
								if (node.getMountOffset() != null) {
									// just un-clicked
									if (node.getMount() == null) {
										profiler.push("clickRelease");
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
											if (node.getMount() == collider || collider.getMount() == node) {
												continue;
											}
											int argIdx = ImpressionNode.canBeNextInput(node, collider);
											if (argIdx >= 0) {
												LogUtils.getLogger().debug("Possible input node: " + node.impression()
														+ " for " + collider.impression() + " at position " + argIdx);
												if (node.setMountAndUpdate(collider)) {
													Point2D off = collider.getInputMountOffset(argIdx);
													node.setMountOffset(off);
													collider.putInput(argIdx, node);
													did = true;
												}
											} else {
												argIdx = ImpressionNode.canBeNextInput(collider, node);
												if (argIdx >= 0) {
													LogUtils.getLogger()
															.debug("Possible function " + node.impression()
																	+ " for input node " + collider.impression()
																	+ " at position " + argIdx);
													if (collider.setMountAndUpdate(node)) {
														Point2D off = node.getInputMountOffset(argIdx);
														collider.setMountOffset(off);
														node.putInput(argIdx, collider);
													}
												}
											}
										}
										if (!did)
											node.setMountOffset(null);
										profiler.pop();
									}
								}
							}
						}
					});
					profiler.pop();
				} else {
					node.setX(0);
					node.setY(0);
					node.setShape(null);
				}

				profiler.popPush("rendering");
				imp.render(graphics, idx, iteratingImpressions.size(), tracker, node.getShape(), getMentalState(),
						node.getTimeInfo(), node.getTag());
				profiler.pop();

				profiler.pop();
			}
		}
		profiler.pop();
		profiler.pop();
	}

	/**
	 * Returns the normal (unit) vector of the wall collided with by the current
	 * node, or null if not colliding
	 * 
	 * @param graphics
	 * @param x
	 * @param y
	 * @return
	 */
	private static Point2D normalVectorOfCollidedWall(GuiGraphics graphics, double x, double y) {
		if (y <= 0) {
			return new Point2D.Double(0, 1);
		}
		if (y >= graphics.guiHeight()) {
			return new Point2D.Double(0, -1);
		}
		if (x <= 0) {
			return new Point2D.Double(1, 0);
		}
		if (x >= graphics.guiWidth()) {
			return new Point2D.Double(-1, 0);
		}
		return null;
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
		synchronized (IMPRESSIONS) {
			for (ImpressionNode node : IMPRESSIONS.values()) {
				if (node == with)
					continue;
				if (ignoreMouseNode && node == GRABBED)
					continue;
				if (ImpressionNode.colliding(node, with)) {
					col.add(node);
				}
			}
		}
		return col;
	}

	/**
	 * Creates a piece of data to send to server
	 * 
	 * @param node
	 * @return
	 */
	private static ImpressionHolder createHolder(ImpressionNode node) {
		if (node.inputCount() == 0) {
			return new ImpressionHolder(node.impression());
		}
		return new ImpressionHolder(node.impression(),
				node.getInputListUnsafe().stream().map((s) -> createHolder(s)).toList());
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
			int xpos = (int) ((Mth.sin(
					Minecraft.getInstance().level.getGameTime() + (Minecraft.getInstance().level.random.nextInt(20)))));
			int ypos = (int) ((Mth.cos(
					Minecraft.getInstance().level.getGameTime() + (Minecraft.getInstance().level.random.nextInt(20)))));
			addNode(x.impression().orElseThrow(), new ImpressionNode(x.impression().get(),
					x.additionalInfo().orElse(ImpressionTimetracker.DEFAULT), xpos, ypos));
			break;
		case REMOVE:
			removeNode(x.impression().orElseThrow());
			break;
		case CLEAR:
			clearImpressions();
			break;
		case UPDATE:
			updateNode(x.impression().orElseThrow());
			break;
		}
	}

	private static void addNode(IImpression orElseThrow, ImpressionNode impressionNode) {
		synchronized (IMPRESSIONS) {
			IMPRESSIONS.put(orElseThrow, impressionNode);
		}
	}

	private static void clearImpressions() {
		synchronized (IMPRESSIONS) {
			IMPRESSIONS.clear();
		}
	}

}
