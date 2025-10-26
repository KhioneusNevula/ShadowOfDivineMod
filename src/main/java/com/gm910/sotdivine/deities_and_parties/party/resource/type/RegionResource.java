package com.gm910.sotdivine.deities_and_parties.party.resource.type;

import com.gm910.sotdivine.deities_and_parties.party.IParty;
import com.gm910.sotdivine.deities_and_parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.deities_and_parties.party.resource.ResourceValue;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class RegionResource implements IRegionResource {

	private ResourceKey<Level> dimm;
	private ChunkPos cpos;

	public RegionResource(ChunkPos pos, ResourceKey<Level> level) {
		this.dimm = level;
		this.cpos = pos;
	}

	@Override
	public PartyResourceType<?> resourceType() {
		return PartyResourceType.REGION.get();
	}

	@Override
	public int resourceValue(IParty party, ResourceValue value) {
		switch (value) {
		case MORTAL_STATUS:
			return 1; // TODO deity value
		default:
			return 0;
		}
	}

	@Override
	public ResourceKey<Level> dimension() {
		return dimm;
	}

	@Override
	public ChunkPos chunkPos() {
		return cpos;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof IDimensionResource) {
			return this.dimm.equals(((IRegionResource) obj).dimension())
					&& this.cpos.equals(((IRegionResource) obj).chunkPos());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return dimm.hashCode() + this.cpos.hashCode();
	}

	@Override
	public String toString() {
		return "RegionResource[CX=" + cpos.x + ",CY=" + cpos.z + ",D=" + dimm.toString() + "]";
	}

	@Override
	public String report(ServerLevel access) {
		return toString();
	}

}
