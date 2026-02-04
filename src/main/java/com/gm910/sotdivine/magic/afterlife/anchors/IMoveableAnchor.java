package com.gm910.sotdivine.magic.afterlife.anchors;

/**
 * An anchor that can be repositioned to different things
 * 
 * @param <SO>
 */
public interface IMoveableAnchor<SO> extends IAfterlifeAnchor<EntityOrPosition, SO> {

	/**
	 * Update an internal reference for this anchor
	 * 
	 * @param eop
	 */
	public void moveAnchor(EntityOrPosition eop);
}
