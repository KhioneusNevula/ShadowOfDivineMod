package com.gm910.sotdivine.magic.theophany.client;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gm910.sotdivine.magic.theophany.cap.ImpressionTimetracker;
import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class ImpressionNode {

	private IImpression impression;
	private ImpressionTimetracker timeInfo;
	private List<ImpressionNode> inputs = new ArrayList<>();
	private ImpressionNode mount = null;
	private CompoundTag tag = new CompoundTag();
	private Point2D mouseOffset = null;
	private Shape shape = null;
	private Area areaTemp = null;
	private Collection<ImpressionNode> collidingWith = Set.of();
	private long lastClickedTick = -1;
	private static final long DOUBLE_CLICK_INTERVAL = 10;

	private double x;
	private double y;

	public ImpressionNode(IImpression impression, ImpressionTimetracker tt, int x, int y) {
		timeInfo = tt;
		this.x = x;
		this.y = y;
		this.impression = impression;
	}

	public ImpressionTimetracker getTimeInfo() {
		return timeInfo;
	}

	/**
	 * Impressions that have been inserted into this node as inputs
	 * 
	 * @return
	 */
	public List<ImpressionNode> getInputs() {
		return inputs;
	}

	/**
	 * Return the node that this node is affixed to
	 * 
	 * @return
	 */
	public ImpressionNode getMount() {
		return mount;
	}

	/**
	 * Set mount and remove this as a mount from current mount node. Return false if
	 * this wasn't possible, i.e. due to a cycle forming
	 * 
	 * @param mount
	 */
	public boolean setMountAndUpdate(ImpressionNode mount) {
		if (mount != null) {
			ImpressionNode start = mount;
			while (start.mount != null) {
				start = start.mount;
				if (start == this) {
					return false;
				}
			}
		}
		if (this.mount != null) {
			this.mount.inputs.remove(this);
		}
		this.mount = mount;
		return true;
	}

	public CompoundTag getTag() {
		return tag;
	}

	public IImpression impression() {
		return impression;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	/**
	 * If this is non-null, then this is the offset from the center of this item's
	 * figure to the mount or mouse (if the mouse is the mount).
	 * 
	 * @return
	 */
	public Point2D getMountOffset() {
		return mouseOffset;
	}

	public void setMountOffset(Point2D mouseOffset) {
		this.mouseOffset = mouseOffset;
	}

	/**
	 * If the mouse is down on this node
	 * 
	 * @return
	 */
	public boolean checkClicked() {
		return shape == null ? false : ImpressionsClient.clickedShape(shape);
	}

	public void setLastClickedTick(long lastClickedTick) {
		this.lastClickedTick = lastClickedTick;
	}

	public long getLastClickedTick() {
		return lastClickedTick;
	}

	/**
	 * If this was double clicked
	 * 
	 * @param currentTime
	 * @return
	 */
	public boolean wasDoubleClicked(long currentTime) {
		if (lastClickedTick < 0)
			return false;
		return currentTime - lastClickedTick <= DOUBLE_CLICK_INTERVAL;
	}

	/**
	 * If these nodes are in contact
	 * 
	 * @param one
	 * @param two
	 * @return
	 */
	public static boolean colliding(ImpressionNode one, ImpressionNode two) {
		if (one.shape == null || two.shape == null)
			return false;
		if (!one.shape.getBounds2D().intersects(two.shape.getBounds2D())) {
			return false;
		}
		one.areaTemp = new Area(one.shape);
		two.areaTemp = new Area(two.shape);
		Area aCopy = (Area) one.areaTemp.clone();
		aCopy.intersect(two.areaTemp);
		return !aCopy.isEmpty();
	}

	/**
	 * If the first node can be the next input of the second node, return the index;
	 * else, return -1
	 * 
	 * @param argument
	 * @param function
	 * @return
	 */
	public static int canBeNextInput(ImpressionNode argument, ImpressionNode function) {
		if (function.impression.requireInputs().isEmpty())
			return -1;
		int nextIndex = function.inputs.size();
		if (nextIndex < function.impression.requireInputs().size()) {
			if (function.impression.requireInputs().get(nextIndex) == ImpressionType.ANY
					|| function.impression.requireInputs().get(nextIndex) == argument.impression.getImpressionType()) {
				return nextIndex;
			}
		}
		return -1;
	}

	/**
	 * Return the offset position of the next input node in a circular cycle around
	 * this
	 * 
	 * @param inputIndex
	 * @return
	 */
	public Point2D getInputMountOffset(int inputIndex) {
		if (inputIndex > impression.requireInputs().size()) {
			throw new IllegalArgumentException("Index " + inputIndex + " too big for " + impression.requireInputs());
		}
		if (shape == null)
			return null;
		double radius = Point.distance(shape.getBounds2D().getMinX(), shape.getBounds2D().getMinY(),
				shape.getBounds2D().getMaxX(), shape.getBounds2D().getMaxY()) / 2;
		double position = 2 * Math.PI / (impression.requireInputs().size() + 1) * inputIndex;
		return new Point2D.Double(radius / 2 + -radius * Math.sin(position), radius / 2 + radius * Math.cos(position));
	}

	public void setShape(Shape shape) {
		this.shape = shape;
		this.areaTemp = null;
	}

	public void setCollidingWith(Collection<ImpressionNode> collidingWith) {
		this.collidingWith = collidingWith;
	}

	public Iterable<ImpressionNode> collidingWith() {
		return this.collidingWith;
	}

	/**
	 * Return the shape of this node
	 */
	public Optional<Shape> getShape() {
		return Optional.ofNullable(shape);
	}
}
