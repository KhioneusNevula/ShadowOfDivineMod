package com.gm910.sotdivine.concepts.parties.party.resource.type;

import java.util.UUID;

import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.concepts.parties.party.resource.ResourceValue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public non-sealed class SoulResource implements ISoulResource {

	private CompoundTag data;

	/**
	 * Create a resource for the given uuid
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static SoulResource create(Entity entity) {
		return (SoulResource) ISoulResource.create(entity);
	}

	public SoulResource(CompoundTag data) {
		this.data = data;
	}

	@Override
	public PartyResourceType<ISoulResource> resourceType() {
		return PartyResourceType.SOUL.get();
	}

	@Override
	public int resourceValue(IParty party, ResourceValue value) {
		switch (value) {
		case DEITY_EMPOWERING:
			return 1;
		default:
			return 0;
		}
	}

	@Override
	public CompoundTag getSoulData() {
		return this.data;
	}

	@Override
	public ISoulResource copy() {
		return new SoulResource(data.copy());
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof ISoulResource) {
			ISoulResource resource = (ISoulResource) obj;
			return this.data.equals(resource.getSoulData());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.data.hashCode();
	}

	@Override
	public String toString() {
		return "SoulResource[\""
				+ getCustomName().map(Object::toString)
						.orElse(Component.translatable(getEntityType().getDescriptionId()).getString())
				+ "\",uuid=" + getUUID().map(UUID::toString).orElse("null") + "]";
	}

	@Override
	public String report(Level access) {
		return "SoulResource[" + this.data + "]";
	}

}
