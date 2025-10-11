package com.gm910.sotdivine.systems.party.resource.type;

import java.util.UUID;

import com.gm910.sotdivine.systems.party.resource.IPartyResource;

/**
 * Resource for a party which is a member
 * 
 * @author borah
 *
 */
public interface IIDResource extends IPartyResource {

	/**
	 * UUID of a member of this party
	 * 
	 * @return
	 */
	public UUID memberID();

}
