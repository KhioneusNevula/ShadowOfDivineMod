package com.gm910.sotdivine.deities_and_parties.party.relation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.gm910.sotdivine.deities_and_parties.system_storage.IPartySystem;
import com.gm910.sotdivine.util.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class Relationship implements IRelationship {

	private Map<RelationStat, Float> stats;
	private String target;

	protected Relationship(String targ) {
		this.target = targ;
		this.stats = new HashMap<>();
	}

	protected Relationship(String targ, Map<String, Float> stat) {
		this(targ);
		for (String key : stat.keySet()) {
			this.stats.put(RelationStat.valueOf(key), stat.get(key));
		}
	}

	@Override
	public String target() {
		return this.target;
	}

	@Override
	public float statValue(RelationStat stat) {
		return stats.getOrDefault(stat, 0f);
	}

	@Override
	public void setStat(RelationStat stat, float value) {
		stats.put(stat, value);
	}

	@Override
	public int hashCode() {
		return target.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof IRelationship) {
			IRelationship relation = (IRelationship) obj;
			return target.equals(relation.target());
		}
		return false;
	}

	@Override
	public String toString() {
		return "Relationship(" + target + ")" + stats;
	}

	@Override
	public Component report(ServerLevel access) {
		IPartySystem system = IPartySystem.get(access);
		return system.getPartyByName(target)
				.map((party) -> TextUtils.transPrefix("cmd.relationship",
						party.descriptiveName().orElse(TextUtils.transPrefix("cmd.noname")), party.uniqueName(),
						stats.toString()))
				.orElse(Component.literal(this.toString()));

	}

}
