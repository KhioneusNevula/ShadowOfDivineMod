package com.gm910.sotdivine.systems.party.resource.type;

import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party.resource.PartyResourceType;
import com.gm910.sotdivine.systems.party.resource.ResourceValue;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class DimensionResource implements IDimensionResource {

	private ResourceKey<Level> key;

	public DimensionResource(ResourceKey<Level> key) {
		this.key = key;
	}

	@Override
	public PartyResourceType<?> resourceType() {
		return PartyResourceType.DIMENSION.get();
	}

	@Override
	public int resourceValue(IParty party, ResourceValue value) {
		switch (value) {
		case DEITY_EMPOWERING:
			return 10; // TODO deity value
		default:
			return 0;
		}
	}

	@Override
	public ResourceKey<Level> dimension() {
		return key;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof IDimensionResource) {
			return this.key.equals(((IDimensionResource) obj).dimension());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public String toString() {
		return "DimensionResource[" + key.location() + "]";
	}

	@Override
	public String report(ServerLevel access) {
		return toString();
	}

}
