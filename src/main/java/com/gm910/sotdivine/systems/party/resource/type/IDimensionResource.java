package com.gm910.sotdivine.systems.party.resource.type;

import com.gm910.sotdivine.systems.party.resource.IPartyResource;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A dimension-based resource
 * 
 * @author borah
 *
 */
public interface IDimensionResource extends IPartyResource {

	/**
	 * Return the dimension's key itself
	 * 
	 * @return
	 */
	public ResourceKey<Level> dimension();
}
