package com.gm910.sotdivine.systems.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gm910.sotdivine.systems.deity.IDeity;
import com.gm910.sotdivine.systems.party.relation.IPartyMemory;
import com.gm910.sotdivine.systems.party.relation.IRelationship;
import com.gm910.sotdivine.systems.party.relation.MemoryType;
import com.gm910.sotdivine.systems.party.resource.IPartyResource;
import com.gm910.sotdivine.systems.party.resource.PartyResourceType;
import com.gm910.sotdivine.systems.party.resource.type.IRegionResource;
import com.google.common.base.Functions;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * A "Party" the deity can have a relationship with, which may be another deity
 * 
 * @author borah
 *
 */
public interface IParty {

	public static final Codec<IParty> CODEC = RecordCodecBuilder.create(instance -> instance.group( // Define the fields
																									// within the
																									// emanation
			Codec.BOOL.fieldOf("canWorship").forGetter(IParty::canWorship),
			Codec.STRING.fieldOf("uniqueName").forGetter(IParty::uniqueName),
			Codec.BOOL.fieldOf("isEntity").forGetter(IParty::isEntity),
			Codec.BOOL.fieldOf("isGroup").forGetter(IParty::isGroup),
			Codec.list(Codec.STRING).optionalFieldOf("members")
					.forGetter((par) -> par.memberCollection().isEmpty() ? Optional.empty()
							: Optional.of(par.memberCollection().stream().map(Object::toString)
									.collect(Collectors.toList()))),
			Codec.list(IPartyMemory.CODEC).fieldOf("memories")
					.forGetter((party) -> new ArrayList<>(party.allMemories())),
			Codec.unboundedMap(Codec.STRING, IRelationship.CODEC).fieldOf("relationships")
					.forGetter((par) -> par.knownParties().stream()
							.collect(Collectors.toMap(Functions.identity(), (x) -> par.relationshipWith(x).get()))),
			ComponentSerialization.CODEC.optionalFieldOf("descriptiveName").forGetter(IParty::descriptiveName),
			Codec.list(Codec.pair(IPartyResource.codec().get().fieldOf("object").codec(),
					Codec.INT.fieldOf("quantity").codec())).fieldOf("resources")
					.forGetter((x) -> x.ownedResources().stream().map((y) -> Pair.of(y, x.getResourceAmount(y)))
							.collect(Collectors.toList()))

	).apply(instance, (a, b, c, d, mems, f, g, n, x) -> new Party(a, b, c, d, mems, f, g, n, x)));

	/**
	 * Creates a party as an individual entity, usually a player; also include a
	 * small component to give this entity a name
	 * 
	 * @param id
	 * @param position
	 * @return
	 */
	public static IParty createEntity(UUID id, Component name) {
		return new Party(id.toString(), true, true, false, Optional.ofNullable(name));
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
		IParty p = new Party(unique, true, false, true, Optional.ofNullable(displayName));
		for (IRegionResource res : holdings) {
			p.setResourceAmount(res, 1);
		}
		return p;
	}

	/**
	 * Whether this party gives worship (i.e. it is not another deity)
	 * 
	 * @return
	 */
	public boolean canWorship();

	/**
	 * The unique id of this party; for a player, this is the player's UUID; each
	 * village also generates its own id
	 * 
	 * @return
	 */
	public String uniqueName();

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

	/**
	 * If this party is an entity
	 */
	public boolean isEntity();

	/**
	 * If this party is a group, i.e. a village
	 * 
	 * @return
	 */
	public boolean isGroup();

	/**
	 * If this party is a deity (which is neither an entity nor group)
	 * 
	 * @return
	 */
	public default boolean isDeity() {
		return this instanceof IDeity;
	}

	/**
	 * If this party is an individual entity, return the entity if it is currently
	 * loaded
	 * 
	 * @return
	 */
	public Optional<Entity> opEntity(ServerLevel world);

	/**
	 * Near-alias of {@link #opEntity(ServerLevel)} that casts the returned item to
	 * a player or returns null if it is not a player
	 * 
	 * @param world
	 * @return
	 */
	public default Optional<ServerPlayer> opPlayer(ServerLevel world) {
		return opEntity(world).filter((x) -> x instanceof ServerPlayer).map((x) -> (ServerPlayer) x);
	}

	/**
	 * Return all Parties this party has a relationship with
	 * 
	 * @return
	 */
	public Collection<String> knownParties();

	/**
	 * Return all members of this group; the returned collection should be editable
	 * to add and remove members
	 * 
	 * @return
	 */
	public Collection<UUID> memberCollection();

	/**
	 * Return the relation this party has with the given party by ID (or empty
	 * optional if it has no relationship)
	 * 
	 * @param party
	 * @return
	 */
	public Optional<IRelationship> relationshipWith(String party);

	/**
	 * Create and return a relationship with the givne entity
	 * 
	 * @param randomUUID
	 * @return
	 */
	public IRelationship addRelationshipWith(String randomUUID);

	/**
	 * Return the memories of this party of the given type as an editable collection
	 * 
	 * @param type
	 * @return
	 */
	public Collection<IPartyMemory> memoriesOfTypeCollection(MemoryType type);

	/**
	 * Return all this party's memories; editability does not matter
	 * 
	 * @return
	 */
	public Collection<IPartyMemory> allMemories();

	/**
	 * A string giving a name to this entity for ease of use
	 * 
	 * @return
	 */
	public Optional<Component> descriptiveName();

	/**
	 * All resources that this party owns
	 * 
	 * @return
	 */
	public Collection<IPartyResource> ownedResources();

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
	 * How much of the specified resource this party owns
	 * 
	 * @param resource
	 * @return
	 */
	public int getResourceAmount(IPartyResource resource);

	/**
	 * Return all party resources of the given type
	 * 
	 * @param <T>
	 * @param type
	 * @return
	 */
	public <T extends IPartyResource> Collection<T> getResourcesOfType(PartyResourceType<T> type);

	/**
	 * Report info about this [party
	 * 
	 * @return
	 */
	String report();

	/**
	 * Repotr stuff about this party with some additional info by having access to a
	 * world emanation
	 * 
	 * @param level
	 * @return
	 */
	String report(ServerLevel level);

	/**
	 * For certain parties, having a reference to the server level is useful
	 * 
	 * @param level
	 */
	public void updateLevelReference(ServerLevel level);

	/**
	 * Return some general info about this party
	 * 
	 * @param level
	 * @return
	 */
	public Component descriptiveInfo(ServerLevel level);

}
