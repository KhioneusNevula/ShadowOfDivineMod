package com.gm910.sotdivine.concepts.parties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.deity.personality.IDeityStat;
import com.gm910.sotdivine.concepts.parties.party.relation.IPartyMemory;
import com.gm910.sotdivine.concepts.parties.party.relation.IRelationship;
import com.gm910.sotdivine.concepts.parties.party.relation.MemoryType;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.magic.sphere.ISphere;
import com.gm910.sotdivine.magic.sphere.Spheres;
import com.gm910.sotdivine.network.party_system.ClientParties;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;

public interface IPartyLister {

	/**
	 * Returns a party lister regardless of client or server side
	 * 
	 * @param level
	 * @return
	 */
	public static IPartyLister getLister(Level level) {
		if (level instanceof ServerLevel serv) {
			return IPartySystem.get(serv);
		}
		return ClientParties.instance().get();
	}

	public static final Codec<IPartyLister> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group( // Define the fields within the emanation
			Codec.list(IPartyInfo.MAP_CODEC.codec()).fieldOf("parties").forGetter((ds) -> {
				return new ArrayList<>(ds.nonDeityParties());
			}),
			Codec.list(IDeityInfo.MAP_CODEC.codec()).fieldOf("deities")
					.forGetter((ds) -> new ArrayList<>(ds.allDeities())))
			.apply(instance, (d, nd) -> new PartyLister(d, nd)));

	public static final StreamCodec<RegistryFriendlyByteBuf, IPartyLister> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.collection((i) -> new ArrayList<>(i), IPartyInfo.STREAM_CODEC),
			(p) -> (Collection<IPartyInfo>) p.nonDeityParties(),
			ByteBufCodecs.collection((i) -> new ArrayList<>(i), IDeityInfo.STREAM_CODEC),
			(p) -> (Collection<IDeityInfo>) p.allDeities(), (s, y) -> new PartyLister(s, y));

	/**
	 * Return all parties that aren't deities
	 * 
	 * @return
	 */
	public Collection<? extends IPartyInfo> nonDeityParties();

	/**
	 * Return all parties, deity and non-deity
	 * 
	 * @return
	 */
	public Collection<? extends IDeityInfo> allDeities();

	/**
	 * Return the party associated with this ID
	 */
	public Optional<? extends IPartyInfo> getPartyByName(String id);

	/**
	 * Returns party by its display name
	 * 
	 * @param name
	 * @return
	 */
	public Optional<? extends IPartyInfo> getPartyByDisplayName(String name);

	/**
	 * If a certain party exists
	 * 
	 * @param id
	 * @return
	 */
	public default boolean partyExists(String id) {
		return getPartyByName(id).isPresent();
	}

	/**
	 * REturn a deity associated with this id, or an empty optional if the thing
	 * accessed is null or a non-deity
	 * 
	 * @param id
	 * @return
	 */
	public Optional<? extends IDeityInfo> deityByName(String id);

	/**
	 * Returns stream of deities based on a given sphere
	 * 
	 * @param sphere
	 * @return
	 */
	public Stream<? extends IDeityInfo> deitiesBySphere(ISphere sphere);

	/**
	 * Returns stream of deities based on a given symbol
	 * 
	 * @param sphere
	 * @return
	 */
	public default Stream<? extends IDeityInfo> deitiesBySymbol(IDeitySymbol symbol) {
		return allDeities().stream().filter((a) -> {
			ResourceLocation aRL = DeitySymbols.instance().getDeitySymbolMap().inverse().get(a.symbol());
			ResourceLocation arg = DeitySymbols.instance().getDeitySymbolMap().inverse().get(symbol);
			boolean eq = arg.equals(aRL);
			return eq;
		});
	}

	/**
	 * Returns stream of deities based on a given banner patterns (calls
	 * {@link #deitiesBySymbol(IDeitySymbol)})
	 * 
	 * @param sphere
	 * @return
	 */
	public default Stream<? extends IDeityInfo> deitiesByPattern(BannerPattern symbol) {
		return DeitySymbols.instance().getFromPattern(symbol).map(this::deitiesBySymbol).orElse(Stream.empty());
	}

	/**
	 * Return all parties, deity and non-deity
	 * 
	 * @return
	 */
	public Iterable<? extends IPartyInfo> allParties();

	public interface IPartyInfo {

		public static final MapCodec<IPartyInfo> MAP_CODEC = RecordCodecBuilder
				.mapCodec(instance -> instance
						.group(Codec.STRING.fieldOf("unique_name").forGetter(IPartyInfo::uniqueName),
								ComponentSerialization.CODEC.optionalFieldOf("descriptive_name")
										.forGetter(IPartyInfo::descriptiveName),
								Codec.BOOL.optionalFieldOf("is_entity", false).forGetter(IPartyInfo::isEntity),
								Codec.BOOL.optionalFieldOf("is_group", false).forGetter(IPartyInfo::isGroup),
								Codec.BOOL.optionalFieldOf("is_deity", false).forGetter(IPartyInfo::isDeity),

								Codec.BOOL.optionalFieldOf("can_worship", false).forGetter(IPartyInfo::canWorship),
								Codec.unboundedMap(Codec.STRING, IRelationship.CODEC)
										.optionalFieldOf("relationships", Map.of())
										.forGetter((par) -> Streams.stream(par.knownParties())
												.collect(Collectors.toMap(Functions.identity(),
														(x) -> par.relationshipWith(x).get()))),
								Codec.list(IPartyMemory.CODEC).optionalFieldOf("memories", List.of())
										.forGetter((party) -> Lists.newArrayList(party.allMemories())),
								Codec.list(EntityReference.<Entity>codec())
										.optionalFieldOf("members", new ArrayList<>())
										.forGetter((par) -> new ArrayList<EntityReference<Entity>>(par.members())))
						.apply(instance,
								(n, n2, e, g, d, cw, rl, me, mem) -> new PartyInfo(n, n2, e, g, d, cw, rl, me, mem)));

		public static final StreamCodec<RegistryFriendlyByteBuf, IPartyInfo> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, IPartyInfo::uniqueName, ComponentSerialization.OPTIONAL_STREAM_CODEC,
				IPartyInfo::descriptiveName, ByteBufCodecs.BOOL, IPartyInfo::isEntity, ByteBufCodecs.BOOL,
				IPartyInfo::isGroup, ByteBufCodecs.BOOL, IPartyInfo::isDeity, ByteBufCodecs.BOOL,
				IPartyInfo::canWorship,
				ByteBufCodecs.map((s) -> new HashMap<>(s), ByteBufCodecs.STRING_UTF8,
						ByteBufCodecs.fromCodecWithRegistries(IRelationship.CODEC)),
				(par) -> Streams.stream(par.knownParties())
						.collect(Collectors.toMap(Functions.identity(), (x) -> par.relationshipWith(x).get())),
				ByteBufCodecs.collection((i) -> new ArrayList<>(i),
						ByteBufCodecs.fromCodecWithRegistries(IPartyMemory.CODEC)),
				(party) -> Lists.newArrayList(party.allMemories()),
				ByteBufCodecs.collection((i) -> new ArrayList<>(i),
						ByteBufCodecs.fromCodecWithRegistries(EntityReference.<Entity>codec())),
				(party) -> new ArrayList<EntityReference<Entity>>(party.members()),
				(n, n2, e, g, d, cw, rl, me, mem) -> new PartyInfo(n, n2, e, g, d, cw, rl, me, mem));

		/**
		 * The unique id of this party; for a player, this is the player's UUID; each
		 * village also generates its own id
		 * 
		 * @return
		 */
		public String uniqueName();

		/**
		 * A string giving a name to this entity for ease of use
		 * 
		 * @return
		 */
		public Optional<Component> descriptiveName();

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
		public boolean isDeity();

		/**
		 * Whether this party gives worship (i.e. it is not another deity)
		 * 
		 * @return
		 */
		public boolean canWorship();

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
		String report(Level level);

		/**
		 * Return some general info about this party
		 * 
		 * @param level
		 * @return
		 */
		public Component descriptiveInfo(Level level);

		/**
		 * Return all Parties this party has a relationship with
		 * 
		 * @return
		 */
		public Iterable<String> knownParties();

		/**
		 * Return the relation this party has with the given party by ID (or empty
		 * optional if it has no relationship)
		 * 
		 * @param party
		 * @return
		 */
		public Optional<IRelationship> relationshipWith(String party);

		/**
		 * Return the memories of this party of the given type
		 * 
		 * @param type
		 * @return
		 */
		public Iterable<IPartyMemory> memoriesOfType(MemoryType type);

		/**
		 * Return all this party's memories
		 * 
		 * @return
		 */
		public Iterable<IPartyMemory> allMemories();

		/**
		 * Return all members of this group
		 * 
		 * @return
		 */
		public Collection<EntityReference<Entity>> members();
	}

	public interface IDeityInfo extends IPartyInfo {
		public static final MapCodec<IDeityInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
				.group(IPartyInfo.MAP_CODEC.forGetter((d) -> d),
						Codec.list(Spheres.BY_NAME_CODEC).fieldOf("spheres")
								.forGetter((a) -> new ArrayList<>(a.spheres())),
						DeitySymbols.BY_NAME_CODEC.fieldOf("symbol").forGetter((a) -> a.symbol()),
						Codec.unboundedMap(IDeityStat.REGISTRY.byNameCodec(), Codec.FLOAT)
								.optionalFieldOf("stats", Map.of())
								.forGetter((pa) -> IDeityStat.REGISTRY.entrySet().stream()
										.map((e) -> Map.entry(e.getValue(), pa.statValue(e.getValue())))
										.filter((e) -> e.getValue() != e.getKey().defaultValue())
										.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))

				.apply(instance, (p, sp, sym, ds) -> new DeityInfo(p, sp, sym, ds)));

		public static final StreamCodec<RegistryFriendlyByteBuf, IDeityInfo> STREAM_CODEC = StreamCodec.composite(
				IPartyInfo.STREAM_CODEC, (party) -> (IPartyInfo) party,
				ByteBufCodecs.collection((i) -> new ArrayList<>(i),
						ByteBufCodecs.fromCodecWithRegistries(Spheres.BY_NAME_CODEC)),
				IDeityInfo::spheres, ByteBufCodecs.fromCodecWithRegistries(DeitySymbols.BY_NAME_CODEC),
				IDeityInfo::symbol,
				ByteBufCodecs.map((i) -> new HashMap<>(i),
						ByteBufCodecs.fromCodecWithRegistries(IDeityStat.REGISTRY.byNameCodec()), ByteBufCodecs.FLOAT),
				(pa) -> IDeityStat.REGISTRY.entrySet().stream()
						.map((e) -> Map.entry(e.getValue(), pa.statValue(e.getValue())))
						.filter((e) -> e.getValue() != e.getKey().defaultValue())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
				(p, sp, sym, ds) -> new DeityInfo(p, sp, sym, ds));

		/**
		 * This deity's spheres
		 */
		public Collection<ISphere> spheres();

		/**
		 * The symbol to invoke this deity
		 * 
		 * @return
		 */
		public IDeitySymbol symbol();

		/**
		 * Return the value of the given stat for this deity
		 * 
		 * @param stat
		 * @return
		 */
		public float statValue(IDeityStat stat);

		/**
		 * If this is already a deity, return self; if not, return a copy of this deity
		 * which is a FULL deity. Should not be called on client side.
		 * 
		 * @return
		 */
		public IDeity becomeDeity(ServerLevel level);

	}

	public class PartyInfo implements IPartyInfo {
		protected String uniqueName;
		protected Optional<Component> descriptiveName;
		protected boolean isEntity;
		protected boolean isGroup;
		protected boolean isDeity;
		protected boolean canWorship;
		protected Map<String, IRelationship> relations;
		protected Multimap<MemoryType, IPartyMemory> memories;
		protected Set<EntityReference<Entity>> members;

		public PartyInfo(String uniqueName, Optional<Component> descriptiveName, boolean isEntity, boolean isGroup,
				boolean isDeity, boolean canWorship, Map<String, IRelationship> rl, Iterable<IPartyMemory> me,
				Iterable<EntityReference<Entity>> mems) {
			this.uniqueName = uniqueName;
			this.descriptiveName = descriptiveName;
			this.isEntity = isEntity;
			this.isGroup = isGroup;
			this.isDeity = isDeity;
			this.canWorship = canWorship;
			this.relations = new HashMap<>(rl);
			this.members = new HashSet<>();
			this.memories = MultimapBuilder.enumKeys(MemoryType.class).linkedListValues().build();
			for (IPartyMemory memory : me) {
				this.memories.put(memory.memoryType(), memory);
			}
			mems.forEach((member) -> {
				this.members.add(member);
			});

		}

		@Override
		public Optional<Component> descriptiveName() {
			return descriptiveName;
		}

		@Override
		public boolean isDeity() {
			return isDeity;
		}

		@Override
		public boolean isEntity() {
			return isEntity;
		}

		@Override
		public boolean isGroup() {
			return isGroup;
		}

		@Override
		public boolean canWorship() {
			return canWorship;
		}

		@Override
		public String uniqueName() {
			return uniqueName;
		}

		@Override
		public Optional<IRelationship> relationshipWith(String party) {
			return Optional.ofNullable(relations.get(party));
		}

		@Override
		public Collection<String> knownParties() {
			return Collections.unmodifiableCollection(relations.keySet());
		}

		@Override
		public Iterable<IPartyMemory> allMemories() {
			return Collections.unmodifiableCollection(memories.values());
		}

		@Override
		public Iterable<IPartyMemory> memoriesOfType(MemoryType type) {
			return Collections.unmodifiableCollection(memories.get(type));
		}

		@Override
		public Collection<EntityReference<Entity>> members() {
			return Collections.unmodifiableCollection(this.members);
		}

		@Override
		public String report() {
			return "Party(Info){relations=" + relations + ",memories=" + memories + "}";
		}

		@Override
		public String report(Level level) {
			return "Party(Info){relations="
					+ relations.values().stream().map((en) -> en.report(level).getString()).collect(Collectors.toSet())
					+ ",memories=" + memories + "}";
		}

		@Override
		public Component descriptiveInfo(Level level) {
			return TextUtils.transPrefix("sotd.cmd.partyinfo." + (isGroup ? "group" : (isEntity ? "entity" : "other"))
					+ (canWorship ? ".worshiper" : ""), relations.size());
		}

		@Override
		public String toString() {
			return "Party(Info)" + (canWorship ? "Worshiper" : "") + (isEntity ? "Entity" : "")
					+ (isGroup ? "Group" : "") + "("
					+ (this.descriptiveName.isPresent() ? "\"" + this.descriptiveName.get().getString() + "\", "
							: this.uniqueName + "")
					+ ")";
		}

	}

	public class DeityInfo extends PartyInfo implements IDeityInfo {

		protected Collection<ISphere> spheres;
		protected IDeitySymbol symbol;
		protected Map<IDeityStat, Float> stats;

		public DeityInfo(IPartyInfo party, Collection<ISphere> sp, IDeitySymbol sym, Map<IDeityStat, Float> ds) {
			super(party.uniqueName(), party.descriptiveName(), party.isEntity(), party.isGroup(), party.isDeity(),
					party.canWorship(), Maps.toMap(party.knownParties(), (k) -> party.relationshipWith(k).get()),
					party.allMemories(), party.members());
			this.spheres = sp;
			this.symbol = sym;
			this.stats = new HashMap<>(ds);
		}

		@Override
		public Collection<ISphere> spheres() {
			return Collections.unmodifiableCollection(spheres);
		}

		@Override
		public IDeitySymbol symbol() {
			return symbol;
		}

		@Override
		public float statValue(IDeityStat stat) {
			return stats.getOrDefault(stat, stat.defaultValue());
		}

		@Override
		public IDeity becomeDeity(ServerLevel level) {
			IDeity deity = IDeity.create(uniqueName, this.descriptiveName.orElse(null), spheres, stats, symbol);
			IDeity.addRitualsTo(level, deity);
			return deity;
		}

		@Override
		public String toString() {
			return "Deity(Info){"
					+ (this.descriptiveName().isPresent() ? "name=\"" + this.descriptiveName().get().getString() + "\""
							: "id=" + this.uniqueName())
					+ ",spheres={" + this.spheres().stream().map(ISphere::name).map(Object::toString)
							.collect(CollectionUtils.setStringCollector(","))
					+ "}}";
		}

		@Override
		public String report() {
			return "Deity(Info){spheres=" + this.spheres + ",stats=" + this.stats + ",symbol=" + this.symbol
					+ ",partyData=" + super.report() + "}";
		}

		@Override
		public String report(Level level) {
			return "Deity(Info){spheres=" + this.spheres + ",stats=" + this.stats + ",symbol=" + this.symbol
					+ ",partyData=" + super.report(level) + "}";
		}

		@Override
		public Component descriptiveInfo(Level level) {
			return TextUtils.transPrefix("sotd.cmd.deityinfo",
					this.spheres.stream().map(ISphere::displayName)
							.collect(CollectionUtils.componentCollectorCommasPretty()),
					Component.translatableEscape(this.symbol.bannerPattern().get().translationKey()));
		}

	}

	public record PartyLister(Collection<IPartyInfo> nonDeityParties, Collection<IDeityInfo> allDeities)
			implements IPartyLister {

		@Override
		public Iterable<? extends IPartyInfo> allParties() {
			return () -> Streams.concat(allDeities.stream(), nonDeityParties.stream()).iterator();
		}

		@Override
		public Optional<IPartyInfo> getPartyByName(String id) {
			return Streams.concat(allDeities.stream(), nonDeityParties.stream())
					.filter((s) -> s.uniqueName().equals(id)).findAny();
		}

		@Override
		public Optional<? extends IPartyInfo> getPartyByDisplayName(String name) {
			return Streams.concat(allDeities.stream(), nonDeityParties.stream())
					.filter((x) -> x.descriptiveName().filter((m) -> m.getString().equals(name)).isPresent()).findAny();
		}

		@Override
		public Optional<? extends IDeityInfo> deityByName(String name) {
			return allDeities.stream().filter((m) -> m.uniqueName().equals(name)).findAny();
		}

		@Override
		public Stream<? extends IDeityInfo> deitiesBySphere(ISphere sphere) {
			return allDeities().stream().filter((a) -> a.spheres().contains(sphere));
		}
	}

}