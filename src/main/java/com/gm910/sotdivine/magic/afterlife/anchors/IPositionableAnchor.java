package com.gm910.sotdivine.magic.afterlife.anchors;

import net.minecraft.core.GlobalPos;

/**
 * An anchor which is tied to a position in the world
 * 
 * @param <AO>
 * @param <SO>
 */
public interface IPositionableAnchor<AO, SO> extends IAfterlifeAnchor<AO, SO> {

	/**
	 * Returns the position stored in this
	 * 
	 * @return
	 */
	public GlobalPos getPosition();
}
