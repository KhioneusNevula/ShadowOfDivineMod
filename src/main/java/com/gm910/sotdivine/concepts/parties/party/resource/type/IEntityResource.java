package com.gm910.sotdivine.concepts.parties.party.resource.type;

import java.util.Optional;

import com.gm910.sotdivine.concepts.parties.party.resource.IPartyResource;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;

/**
 * A resource which is an entire uuid, or class of entities
 * 
 * @author borah
 *
 */
public interface IEntityResource extends IPartyResource {

	/**
	 * The type of this uuid
	 * 
	 * @return
	 */
	public EntityType<?> entityType();

	/**
	 * The data of this uuid, or empty if it is just the class of uuid
	 * 
	 * @return
	 */
	public Optional<CompoundTag> opTag();

}
