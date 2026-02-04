package com.gm910.sotdivine.magic.impression.client;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.gm910.sotdivine.magic.impression.IImpression;
import com.gm910.sotdivine.magic.impression.ImpressionType;
import com.gm910.sotdivine.magic.impression.cap.ImpressionTimetracker;

import net.minecraft.nbt.CompoundTag;

public class ImpressionNode {

	private final static ImpressionNode EMPTY = new ImpressionNode(null, null, 0, 0);

	private IImpression impression;
	private ImpressionTimetracker timeInfo;
	private List<ImpressionNode> inputs = new ArrayList<>();
	private ImpressionNode mount = null;
	private CompoundTag tag = new CompoundTag();
	private Point2D mountOffset = null;
	private Shape shape = null;
	private Area areaTemp = null;
	private Collection<ImpressionNode> collidingWith = Set.of();
	private long lastClickedTick = -1;
	private static final long DOUBLE_CLICK_INTERVAL = 10;

	private double x;
	private double y;

	private double vX;
	private double vY;

	public ImpressionNode(IImpression impression, ImpressionTimetracker tt, int x, int y) {
		timeInfo = tt;
		this.x = x;
		this.y = y;
		this.impression = impression;
		if (impression != null) {
			this.inputs = new ArrayList<>(impression.requireInputs().size());
			for (ImpressionType<?> typa : impression.requireInputs()) {
				inputs.add(EMPTY);
			}
		}
	}

	public ImpressionTimetracker getTimeInfo() {
		return timeInfo;
	}

	/**
	 * Impressions that have been inserted into this node as inputs
	 * 
	 * @return
	 */
	public Stream<ImpressionNode> getInputs() {
		return inputs.stream().filter((s) -> s != EMPTY);
	}

	public List<ImpressionNode> getInputListUnsafe() {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		return inputs;
	}

	/**
	 * Return if inputs are full
	 * 
	 * @return
	 */
	public boolean areInputsFull() {
		return inputCount() == inputs.size();
	}

	/**
	 * Sets an input
	 * 
	 * @param i
	 * @param input
	 */
	public void putInput(int i, ImpressionNode input) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		inputs.set(i, input);
	}

	public void clearInputs() {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		inputs.clear();
	}

	/**
	 * Clears a given input in this node
	 * 
	 * @param i
	 */
	public void clearInput(int i) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		inputs.set(i, EMPTY);
	}

	public void clearInput(ImpressionNode node) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		int i = inputs.indexOf(node);
		if (i >= 0) {
			clearInput(i);
		}
	}

	public boolean hasInput(ImpressionNode input) {
		return inputs.contains(input);
	}

	public int inputCount() {
		int count = 0;
		for (ImpressionNode nodo : inputs) {
			if (nodo != EMPTY) {
				count++;
			}
		}
		return count;
	}

	public ImpressionNode getInputAt(int i) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		return inputs.get(i);
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
	 * either nothing changed or this wasn't possible, i.e. due to a cycle forming
	 * 
	 * @param mount
	 */
	public boolean setMountAndUpdate(ImpressionNode mount) {
		if (mount == EMPTY)
			throw new IllegalArgumentException("cannot mount empty ");
		if (this.mount == mount)
			return false;
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
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
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

	public double getXVelocity() {
		return vX;
	}

	public double getYVelocity() {
		return vY;
	}

	public void setVelocity(double vX, double vY) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		this.vX = vX;
		this.vY = vY;
	}

	public void setX(double x) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		this.x = x;
	}

	public void setY(double y) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		this.y = y;
	}

	/**
	 * If this is non-null, then this is the offset from the center of this item's
	 * figure to the mount or mouse (if the mouse is the mount).
	 * 
	 * @return
	 */
	public Point2D getMountOffset() {
		return mountOffset;
	}

	public void setMountOffset(Point2D mouseOffset) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		this.mountOffset = mouseOffset;
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
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
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
		if (function.inputs.isEmpty())
			return -1;
		for (int i = 0; i < function.inputs.size(); i++) {
			if (function.inputs.get(i) == EMPTY && (function.impression.requireInputs().get(i) == ImpressionType.ANY
					|| function.impression.requireInputs().get(i) == argument.impression.getImpressionType())) {
				return i;
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
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		if (inputIndex > impression.requireInputs().size()) {
			throw new IllegalArgumentException("Index " + inputIndex + " too big for " + impression.requireInputs());
		}
		if (shape == null)
			return null;
		double radius = Point.distance(shape.getBounds2D().getMinX(), shape.getBounds2D().getMinY(),
				shape.getBounds2D().getMaxX(), shape.getBounds2D().getMaxY()) / 2;
		double position = 2 * Math.PI / (impression.requireInputs().size()) * inputIndex;
		return new Point2D.Double(shape.getBounds2D().getWidth() / 2 + radius * Math.sin(position),
				shape.getBounds2D().getHeight() / 2 - radius * Math.cos(position));
	}

	public void setShape(Shape shape) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		this.shape = shape;
		this.areaTemp = null;
	}

	public void setCollidingWith(Collection<ImpressionNode> collidingWith) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
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

	public void setImpression(IImpression k) {
		if (this == EMPTY) {
			throw new UnsupportedOperationException("Illegal operation for empty ");
		}
		this.impression = k;
	}
}
