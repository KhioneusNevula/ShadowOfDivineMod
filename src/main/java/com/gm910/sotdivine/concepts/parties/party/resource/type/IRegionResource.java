package com.gm910.sotdivine.concepts.parties.party.resource.type;

import net.minecraft.world.level.ChunkPos;

/**
 * A resource symbolizing ownership over a specific chunk
 * 
 * @author borah
 *
 */
public interface IRegionResource extends IDimensionResource {

	/**
	 * Get the chunkpos
	 * 
	 * @return
	 */
	public ChunkPos chunkPos();
}
