package com.gm910.sotdivine.deities_and_parties.party.resource.type;

import com.gm910.sotdivine.deities_and_parties.party.IParty;
import com.gm910.sotdivine.deities_and_parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.deities_and_parties.party.resource.ResourceValue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

public class SoulResource implements ISoulResource {

	private CompoundTag data;

	/**
	 * Create a resource for the given entity
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static SoulResource create(Entity entity) {
		return ISoulResource.create(entity);
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
		return "SoulResource[" + this.data + "]";
	}

	@Override
	public String report(ServerLevel access) {
		Entity en = this.regenerateEntity(access, EntitySpawnReason.COMMAND);
		return "SoulResource[\"" + en.getDisplayName().getString() + "\",uuid=" + en.getUUID() + "]";
	}

}
