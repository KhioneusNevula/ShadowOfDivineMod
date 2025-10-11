package com.gm910.sotdivine.systems.party.resource.type;

import java.util.UUID;

import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party.resource.PartyResourceType;
import com.gm910.sotdivine.systems.party.resource.ResourceValue;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;

public class IDResource implements IIDResource {

	private UUID id;

	public IDResource(UUID id) {
		this.id = id;
	}

	@Override
	public PartyResourceType<?> resourceType() {
		return PartyResourceType.MEMBER.get();
	}

	@Override
	public int resourceValue(IParty party, ResourceValue value) {
		return 0;
	}

	@Override
	public UUID memberID() {
		return id;
	}

	@Override
	public String toString() {
		return "IDResource[" + this.id + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			return true;
		}
		if (obj instanceof IIDResource) {
			IIDResource imr = (IIDResource) obj;
			return this.id.equals(imr.memberID());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public String report(ServerLevel access) {
		Entity x = new EntityReference<Entity>(id).getEntity(access, Entity.class);
		if (x == null) {
			return this.toString();
		} else {
			return "IDResource[\"" + x.getDisplayName().getString() + "\", uuid=" + id + "]";
		}
	}

}
