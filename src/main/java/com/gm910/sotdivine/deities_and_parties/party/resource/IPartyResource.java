package com.gm910.sotdivine.deities_and_parties.party.resource;

import java.util.Optional;

import com.gm910.sotdivine.deities_and_parties.party.IParty;
import com.mojang.serialization.Codec;

import net.minecraft.server.level.ServerLevel;

/**
 * Something which parties can trade amongst one another, e.g. dimensions
 * 
 * @author borah
 *
 */
public interface IPartyResource {

	public static Optional<Codec<IPartyResource>> codec() {
		return PartyResourceType.resourceCodec();
	}

	/**
	 * Return the party resource type for this resource
	 * 
	 * @return
	 */
	public PartyResourceType<?> resourceType();

	/**
	 * Whether "identical copies" of this resource can be owned in quantities > 1
	 * 
	 * @return
	 */
	public default boolean isFungible() {
		return resourceType().isFungible();
	}

	/**
	 * Whether this resource is something that exists independently and is owned by
	 * deed rather than kept fully
	 * 
	 * @return
	 */
	public default boolean isDeed() {
		return resourceType().isDeed();
	}

	/**
	 * How much value this resource has relating to the given type of valuability to
	 * the given party
	 * 
	 * @param value
	 * @return
	 */
	public int resourceValue(IParty party, ResourceValue value);

	/**
	 * Report the information in this resource if an accessor is available
	 * 
	 * @param access
	 * @return
	 */
	public String report(ServerLevel access);
}
