package com.gm910.sotdivine.systems.deity.ritual.pattern;

import net.minecraft.core.Direction.Axis;

/**
 * An axis for flipping
 */
public enum FlipAxis {
	NORTH_SOUTH(Axis.Z), WEST_EAST(Axis.X), DOWN_UP(Axis.Y), NONE(null);

	public final Axis axis;

	public static FlipAxis from(Axis axis) {
		switch (axis) {
		case X:
			return WEST_EAST;
		case Y:
			return DOWN_UP;
		case Z:
			return NORTH_SOUTH;
		}
		return NONE;
	}

	private FlipAxis(Axis axis) {
		this.axis = axis;
	}

	public String lowercaseName() {
		return this.name().toLowerCase();
	}

	public static FlipAxis getFor(String str) {
		String pass = str.toUpperCase().replace("-", "_");
		if (pass.equals("SOUTH_NORTH"))
			return NORTH_SOUTH;
		if (pass.equals("EAST_WEST"))
			return WEST_EAST;
		if (pass.equals("UP_DOWN"))
			return DOWN_UP;
		return valueOf(pass);
	}
}
