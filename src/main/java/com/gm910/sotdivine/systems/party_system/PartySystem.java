package com.gm910.sotdivine.systems.party_system;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.networking.ModNetwork;
import com.gm910.sotdivine.systems.deity.IDeity;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party.resource.PartyResourceType;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Implementation of deity system
 * 
 * @author borah
 *
 */
class PartySystem extends SavedData implements IPartySystem {

	protected static final WeakHashMap<ServerLevel, PartySystem> cachedSystems = new WeakHashMap<>(1);

	private Map<String, IParty> parties;
	private Map<String, IDeity> deities;

	private AllSet allset = new AllSet();

	public PartySystem() {
		this.parties = new HashMap<>();
		this.deities = new HashMap<>();
		SOTDMod.LOGGER.debug("[PartySystem::new] Creating new PartySystem for world");
	}

	protected PartySystem(Collection<IParty> par, Collection<IDeity> deities) {
		this.parties = par.stream().collect(Collectors.toMap(IParty::uniqueName, Function.identity()));
		this.deities = deities.stream().collect(Collectors.toMap(IParty::uniqueName, Function.identity()));
		// SOTDMod.LOGGER.debug("[PartySystem::new] Loading new PartySystem from data
		// lists " + deities + " and " + par);

	}

	@Override
	public void markDirty(ServerLevel level) {
		this.setDirty();
		level.players().forEach((player) -> ModNetwork.requestUpdate(this, player.connection.getConnection()));
	}

	@Override
	public String report() {
		return "{\n    Deities:" + deities.values().stream().map(IDeity::report).collect(Collectors.toSet()) + ","
				+ "\n    Other parties:" + parties.values().stream().map(IParty::report).collect(Collectors.toSet())
				+ "\n}";
	}

	@Override
	public Optional<IParty> getPartyByName(String id) {
		return Optional.<IParty>ofNullable(deities.get(id)).or(() -> Optional.ofNullable(parties.get(id)));
	}

	@Override
	public Stream<IParty> dimensionOwners(ResourceKey<Level> dimension) {
		return allParties().stream().filter((x) -> x.getResourcesOfType(PartyResourceType.DIMENSION.get()).stream()
				.anyMatch((y) -> y.dimension().equals(dimension)));
	}

	@Override
	public Stream<IParty> regionOwners(ChunkPos position, ResourceKey<Level> dimension) {

		return allParties().stream().filter((x) -> x.getResourcesOfType(PartyResourceType.REGION.get()).stream()
				.anyMatch((y) -> position.equals(y.chunkPos()) && dimension.equals(y.dimension())));
	}

	@Override
	public void addParty(IParty party, ServerLevel level) {
		this.markDirty(level);
		if (party instanceof IDeity) {
			this.deities.put(party.uniqueName(), (IDeity) party);
		} else {
			this.parties.put(party.uniqueName(), party);
		}
	}

	@Override
	public IParty removeParty(String party, ServerLevel level) {
		this.markDirty(level);
		return Optional.<IParty>ofNullable(deities.remove(party)).or(() -> Optional.ofNullable(parties.remove(party)))
				.orElse(null);
	}

	@Override
	public Collection<IParty> allParties() {
		return allset;
	}

	@Override
	public Collection<IDeity> allDeities() {
		return this.deities.values();
	}

	@Override
	public Collection<IParty> nonDeityParties() {
		return this.parties.values();
	}

	private class AllSet implements Set<IParty> {

		@Override
		public int size() {
			return parties.size() + deities.size();
		}

		@Override
		public boolean isEmpty() {
			return parties.isEmpty() && deities.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof IParty) {
				IParty p = (IParty) o;
				if (parties.containsKey(p.uniqueName()) && parties.get(p.uniqueName()).equals(p)) {
					return true;
				} else {
					return deities.containsKey(p.uniqueName()) && deities.get(p.uniqueName()).equals(p);
				}
			}
			return false;
		}

		@Override
		public Iterator<IParty> iterator() {
			return Iterators.concat(deities.values().iterator(), parties.values().iterator());
		}

		@Override
		public Object[] toArray() {
			return Streams.concat(deities.values().stream(), parties.values().stream()).toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return Streams.concat(deities.values().stream(), parties.values().stream()).toArray((x) -> a);
		}

		@Override
		public boolean add(IParty e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!contains(o)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends IParty> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

	}

}
