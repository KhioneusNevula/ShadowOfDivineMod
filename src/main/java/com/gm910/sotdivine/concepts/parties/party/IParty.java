package com.gm910.sotdivine.concepts.parties.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.parties.IPartyLister.IPartyInfo;
import com.gm910.sotdivine.concepts.parties.party.relation.IPartyMemory;
import com.gm910.sotdivine.concepts.parties.party.relation.IRelationship;
import com.gm910.sotdivine.concepts.parties.party.relation.MemoryType;
import com.gm910.sotdivine.concepts.parties.party.resource.IPartyResource;
import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.concepts.parties.party.resource.type.IRegionResource;
import com.google.common.base.Functions;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;

/**
 * A "Party" the deity can have a relationship with, which may be another deity
 * 
 * @author borah
 *
 */
public interface IParty extends IPartyInfo {

	public static final MapCodec<IParty> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group( // Define
																												// the
																												// fields
			// within the
			// emanation
			Codec.BOOL.optionalFieldOf("can_worship", false).forGetter(IParty::canWorship),
			Codec.STRING.fieldOf("unique_name").forGetter(IParty::uniqueName),
			Codec.BOOL.optionalFieldOf("is_entity", false).forGetter(IParty::isEntity),
			Codec.BOOL.optionalFieldOf("is_group", false).forGetter(IParty::isGroup),
			Codec.list(EntityReference.<Entity>codec()).optionalFieldOf("members", new ArrayList<>())
					.forGetter((par) -> new ArrayList<EntityReference<Entity>>(par.memberCollection())),
			Codec.list(IPartyMemory.CODEC).optionalFieldOf("memories", List.of())
					.forGetter((party) -> new ArrayList<>(party.allMemories())),
			ComponentSerialization.CODEC.optionalFieldOf("descriptive_name").forGetter(IParty::descriptiveName),
			Codec.unboundedMap(Codec.STRING, IRelationship.CODEC).optionalFieldOf("relationships", Map.of())
					.forGetter((par) -> par.knownParties().stream()
							.collect(Collectors.toMap(Functions.identity(), (x) -> par.relationshipWith(x).get()))),
			Codec.list(Codec.pair(IPartyResource.codec().fieldOf("resource").codec(),
					Codec.INT.fieldOf("quantity").codec())).optionalFieldOf("resources", List.of())
					.forGetter((x) -> x.ownedResources().stream().map((y) -> Pair.of(y, x.getResourceAmount(y)))
							.collect(Collectors.toList()))

	).apply(instance,
			(a, b, c, d, mems, memors, nam, rels, res) -> new Party(a, b, c, d, mems, memors, rels, nam, res)));

	/**
	 * Creates a party as an individual entity, usually a player; also include a
	 * small component to give this entity a name
	 * 
	 * @param id
	 * @param position
	 * @return
	 */
	public static IParty createEntity(Entity en, Component name) {
		return new Party(en.getStringUUID(), true, true, false, Optional.ofNullable(name),
				Optional.of(new EntityReference<Entity>(en)));
	}

	/**
	 * Creates a party as a (worshiper) group centered at a certain (optional)
	 * position;
	 * 
	 * @param id
	 * @param position
	 * @return
	 */
	public static IParty createGroup(String unique, Component displayName,
			Collection<? extends IRegionResource> holdings) {
		IParty p = new Party(unique, true, false, true, Optional.ofNullable(displayName), Optional.empty());
		for (IRegionResource res : holdings) {
			p.setResourceAmount(res, 1);
		}
		return p;
	}

	/**
	 * If this is an entity, try to convert unique name to uUID
	 * 
	 * @return
	 */
	public default Optional<UUID> optionalEntityUUID() {
		try {
			return Optional.of(UUID.fromString(this.uniqueName()));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	@Override
	public default boolean isDeity() {
		return this instanceof IDeity;
	}

	/**
	 * Method that returns a casted entity if this is a single entity of the given
	 * class
	 * 
	 * @param world
	 * @return
	 */
	public default <T extends Entity> Optional<T> optionallyAsEntity(ServerLevel world, Class<T> clazz) {
		return singleEntity().map((s) -> s.getEntity(world, Entity.class))
				.filter((x) -> clazz.isAssignableFrom(x.getClass())).map((x) -> (T) x);
	}

	/**
	 * If this party is an individual entity, return the entity reference
	 * 
	 * @return
	 */
	public Optional<EntityReference<Entity>> singleEntity();

	@Override
	public default Collection<EntityReference<Entity>> members() {
		return Collections.unmodifiableCollection(memberCollection());
	}

	/**
	 * Return all members of this group; the returned collection should be editable
	 * to add and remove members if this is a group.
	 * 
	 * @return
	 */
	public Collection<EntityReference<Entity>> memberCollection();

	/**
	 * Create and return a relationship with the givne entity
	 * 
	 * @param randomUUID
	 * @return
	 */
	public IRelationship addRelationshipWith(String randomUUID);

	@Override
	public Collection<String> knownParties();

	/**
	 * Return the memories of this party of the given type as an editable collection
	 * 
	 * @param type
	 * @return
	 */
	public Collection<IPartyMemory> memoriesOfTypeCollection(MemoryType type);

	/**
	 * Return the memories of this party of the given type as an editable collection
	 * 
	 * @param type
	 * @return
	 */
	@Override
	public default Collection<IPartyMemory> memoriesOfType(MemoryType type) {
		return Collections.unmodifiableCollection(memoriesOfTypeCollection(type));
	}

	@Override
	public Collection<IPartyMemory> allMemories();

	/**
	 * How much of the specified resource this party owns
	 * 
	 * @param resource
	 * @return
	 */
	public int getResourceAmount(IPartyResource resource);

	/**
	 * All resources that this party owns
	 * 
	 * @return
	 */
	public Collection<IPartyResource> ownedResources();

	/**
	 * Return all party resources of the given type
	 * 
	 * @param <T>
	 * @param type
	 * @return
	 */
	public <T extends IPartyResource> Collection<T> getResourcesOfType(PartyResourceType<T> type);

	/**
	 * Set tehe amount of the given resource for this party. If 0, the resource is
	 * removed. If this resource is not Fungible, throw error if it is set to more
	 * than 1
	 * 
	 * @param resource
	 */
	public void setResourceAmount(IPartyResource resource, int amount);

	/**
	 * Alias for {@link #setResourceAmount(IPartyResource, int)} with amount=0
	 * 
	 * @param resource
	 */
	public default void removeResource(IPartyResource resource) {
		setResourceAmount(resource, 0);
	}

	/**
	 * Change the resource amount by
	 * 
	 * @param resource
	 * @param by
	 */
	public default void changeResourceAmountBy(IPartyResource resource, int by) {
		this.setResourceAmount(resource, this.getResourceAmount(resource) + by);
	}

	/**
	 * For certain parties, having a reference to the server level is useful
	 * 
	 * @param level
	 */
	public void updateLevelReference(ServerLevel level);

}
