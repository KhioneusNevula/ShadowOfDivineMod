package com.gm910.sotdivine.concepts.parties.party;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.parties.party.relation.IPartyMemory;
import com.gm910.sotdivine.concepts.parties.party.relation.IRelationship;
import com.gm910.sotdivine.concepts.parties.party.relation.MemoryType;
import com.gm910.sotdivine.concepts.parties.party.resource.IPartyResource;
import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Table;
import com.mojang.datafixers.util.Pair;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.level.Level;

public class Party implements IParty {

	protected Multimap<MemoryType, IPartyMemory> memories;
	private boolean canWorship;
	private String uniqueName;
	private boolean isEntity;
	private boolean isGroup;
	protected Map<String, IRelationship> relations;
	protected Set<EntityReference<Entity>> members;
	private Optional<Component> descriptiveName;
	protected ServerLevel level;
	@Nullable
	private Optional<EntityReference<Entity>> singleEntity;

	private Table<PartyResourceType<?>, IPartyResource, Integer> resources = HashBasedTable.create();

	/**
	 * Creates a party from the given details
	 * 
	 * @param pos
	 */
	protected Party(String id, boolean cw, boolean e, boolean g, Optional<Component> n,
			Optional<EntityReference<Entity>> singleEntity) {
		this.uniqueName = Objects.requireNonNull(id, "Needs id");
		this.canWorship = cw;
		this.isEntity = e;
		this.isGroup = g;
		this.memories = MultimapBuilder.enumKeys(MemoryType.class).linkedListValues().build();
		this.relations = new HashMap<>();
		this.singleEntity = Objects.requireNonNull(singleEntity, "Cannot provide null when an optional is expected");
		this.descriptiveName = Objects.requireNonNull(n, "Cannot provide null when an optional is expected");
		this.members = new HashSet<>();
		if (singleEntity.isEmpty()) {
			if (isEntity) {
				throw new IllegalArgumentException("Expected single entity but received none");
			}
		} else {
			if (!isEntity) {
				throw new IllegalArgumentException("Expected no entity but received one: " + singleEntity.get());
			}
			this.members = Set.of(singleEntity.get());
		}
	}

	/**
	 * data constructor
	 * 
	 * @param mems
	 */
	protected Party(boolean canWorship, String uname, boolean isEntity, boolean isGroup,
			Collection<EntityReference<Entity>> mems, List<IPartyMemory> memories, Map<String, IRelationship> relations,
			Optional<Component> n, List<Pair<IPartyResource, Integer>> resources) {
		this(uname, canWorship, isEntity, isGroup, n, Optional.ofNullable(isEntity ? mems.iterator().next() : null));
		for (IPartyMemory memory : memories) {
			this.memories.put(memory.memoryType(), memory);
		}
		for (String key : relations.keySet()) {
			this.relations.put(key, relations.get(key));
		}
		for (Pair<IPartyResource, Integer> resourcePair : resources) {
			this.resources.put(resourcePair.getFirst().resourceType(), resourcePair.getFirst(),
					resourcePair.getSecond());
		}
		if (!isEntity) {
			mems.forEach((member) -> {
				Objects.requireNonNull(member, "why tf would this be null... " + mems);
				this.members.add(member);
			});
		}
	}

	@Override
	public boolean canWorship() {
		return canWorship;
	}

	@Override
	public String uniqueName() {
		return this.uniqueName;
	}

	@Override
	public boolean isEntity() {
		return this.isEntity;
	}

	@Override
	public boolean isGroup() {
		return this.isGroup;
	}

	@Override
	public Optional<EntityReference<Entity>> singleEntity() {
		return this.singleEntity;
	}

	@Override
	public Collection<String> knownParties() {
		return this.relations.keySet();
	}

	@Override
	public Optional<IRelationship> relationshipWith(String party) {
		return Optional.ofNullable(relations.get(party));
	}

	@Override
	public IRelationship addRelationshipWith(String randomUUID) {
		return relations.computeIfAbsent(randomUUID, IRelationship::create);
	}

	@Override
	public Collection<IPartyMemory> memoriesOfTypeCollection(MemoryType type) {
		return memories.get(type);
	}

	@Override
	public Collection<IPartyMemory> allMemories() {
		return memories.values();
	}

	@Override
	public Collection<EntityReference<Entity>> memberCollection() {
		return this.members;
	}

	@Override
	public String toString() {
		return "Party" + (canWorship ? "Worshiper" : "") + (isEntity ? "Entity" : "") + (isGroup ? "Group" : "") + "("
				+ (this.descriptiveName.isPresent() ? "\"" + this.descriptiveName.get().getString() + "\", "
						: this.uniqueName + "")
				+ ")";
	}

	@Override
	public String report() {
		if (this.level != null)
			return report(level);
		return "Party{relations=" + relations + ",memories=" + memories + ",resources=" + this.resources + "}";
	}

	@Override
	public String report(Level level) {
		return "Party{relations="
				+ relations.values().stream().map((en) -> en.report(level).getString()).collect(Collectors.toSet())
				+ ",memories=" + memories + ",resources="
				+ this.resources.rowMap().entrySet().stream()
						.map((en) -> Map.entry(en.getKey(),
								en.getValue().entrySet().stream()
										.map((en2) -> Map.entry(en2.getKey().report(level), en2.getValue()))
										.collect(Collectors.toSet())))
						.collect(Collectors.toSet())
				+ "}";
	}

	@Override
	public Component descriptiveInfo(Level level) {
		return TextUtils.transPrefix("sotd.cmd.partyinfo." + (isGroup ? "group" : (isEntity ? "entity" : "other"))
				+ (canWorship ? ".worshiper" : ""), relations.size());
	}

	@Override
	public Optional<Component> descriptiveName() {
		return this.descriptiveName;
	}

	@Override
	public void setResourceAmount(IPartyResource resource, int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException();
		}
		if (!resource.isFungible() && amount > 1) {
			throw new IllegalArgumentException("Non-fungible resource " + resource + " given amount " + amount);
		}
		if (amount == 0) {
			resources.remove(resource.resourceType(), resource);
		} else {
			resources.put(resource.resourceType(), resource, amount);
		}
	}

	@Override
	public Collection<IPartyResource> ownedResources() {
		return resources.columnKeySet();
	}

	@Override
	public int getResourceAmount(IPartyResource resource) {
		Integer num = resources.get(resource.resourceType(), resource);
		return num == null ? 0 : num;
	}

	@Override
	public void updateLevelReference(ServerLevel level) {
		this.level = level;
	}

	@Override
	public <T extends IPartyResource> Collection<T> getResourcesOfType(PartyResourceType<T> type) {
		return (Collection<T>) resources.row(type).keySet();
	}

}
