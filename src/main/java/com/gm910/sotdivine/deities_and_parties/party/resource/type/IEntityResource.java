package com.gm910.sotdivine.deities_and_parties.party.resource.type;

import java.util.Optional;

import com.gm910.sotdivine.deities_and_parties.party.resource.IPartyResource;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;

/**
 * A resource which is an entire entity, or class of entities
 * 
 * @author borah
 *
 */
public interface IEntityResource extends IPartyResource {

	/**
	 * The type of this entity
	 * 
	 * @return
	 */
	public EntityType<?> entityType();

	/**
	 * The data of this entity, or empty if it is just the class of entity
	 * 
	 * @return
	 */
	public Optional<CompoundTag> opTag();

}
