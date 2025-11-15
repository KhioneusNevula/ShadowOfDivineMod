package com.gm910.sotdivine.concepts.parties.party.resource.type;

import java.util.UUID;

import com.gm910.sotdivine.concepts.parties.party.resource.IPartyResource;

/**
 * Resource for a party which is an individual unique entity
 * 
 * @author borah
 *
 */
public interface IIDResource extends IPartyResource {

	/**
	 * UUID of the resource
	 * 
	 * @return
	 */
	public UUID memberID();

}
