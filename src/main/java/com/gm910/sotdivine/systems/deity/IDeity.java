package com.gm910.sotdivine.systems.deity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.language.LanguageGen;
import com.gm910.sotdivine.language.phono.IPhoneme;
import com.gm910.sotdivine.systems.deity.emanation.DeityInteractionType;
import com.gm910.sotdivine.systems.deity.emanation.EmanationInstance;
import com.gm910.sotdivine.systems.deity.emanation.IEmanation;
import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.systems.deity.personality.IDeityStat;
import com.gm910.sotdivine.systems.deity.ritual.IRitual;
import com.gm910.sotdivine.systems.deity.ritual.RitualInstance;
import com.gm910.sotdivine.systems.deity.ritual.properties.RitualParameter;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.sphere.Spheres;
import com.gm910.sotdivine.systems.deity.symbol.DeitySymbols;
import com.gm910.sotdivine.systems.deity.symbol.IDeitySymbol;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party_system.IPartySystem;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A class representing a deity
 * 
 * @author borah
 *
 */
public interface IDeity extends IParty {

	public static final Codec<IDeity> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group(IParty.CODEC.fieldOf("partyData").forGetter((x) -> x),
			Codec.list(ResourceLocation.CODEC.xmap(Spheres.instance().getSphereMap()::get, ISphere::name))
					.fieldOf("spheres").forGetter((x) -> new ArrayList<>(x.spheres())),
			Codec.unboundedMap(IDeityStat.REGISTRY.byNameCodec(), Codec.FLOAT).fieldOf("deityStats")
					.forGetter((pa) -> IDeityStat.REGISTRY.entrySet().stream()
							.map((e) -> Map.entry(e.getValue(), pa.statValue(e.getValue())))
							.filter((e) -> e.getValue() > 0)
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))),
			ResourceLocation.CODEC
					.xmap(DeitySymbols.instance().getDeitySymbolMap()::get,
							DeitySymbols.instance().getDeitySymbolMap().inverse()::get)
					.fieldOf("symbol").forGetter(IDeity::symbol),
			Codec.list(EmanationInstance.CODEC).fieldOf("runningEmanations")
					.forGetter((x) -> new ArrayList<>(x.runningEmanations())),

			Codec.compoundList(EmanationInstance.CODEC, RitualInstance.codec()).fieldOf("ritualEmanations")
					.forGetter((x) -> ((Deity) x).ritualEmanations.entrySet().stream()
							.map((p) -> Pair.of(p.getKey(), p.getValue())).toList())

	).apply(instance, Deity::new));

	public static IDeity create(String name, Component displayName, Collection<ISphere> spheres,
			Map<IDeityStat, Float> stats, IDeitySymbol symbol) {
		return new Deity(name, spheres, stats, displayName, symbol);
	}

	/**
	 * Pick a number of deity spheres
	 * 
	 * @param max
	 * @param system
	 * @return
	 */
	public static Set<ISphere> pickSpheres(int max, Random source, IPartySystem system) {
		Set<ISphere> spheres = Spheres.instance().getSphereMap().values();
		Map<ISphere, Integer> sphereCounts = spheres.stream().map(
				(x) -> Map.entry(x, (int) system.allDeities().stream().filter((d) -> d.spheres().contains(x)).count()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		WeightedSet<ISphere> sphereList = new WeightedSet<>(sphereCounts.keySet(),
				(x) -> 0.1f / (sphereCounts.get(x) + 0.1f));
		if (sphereList.isEmpty())
			return Set.of();
		if (max == 1) {
			return Set.of(sphereList.get(source));
		}
		Set<ISphere> output = new HashSet<>();
		for (int i = 0; i < max; i++) {
			if (sphereList.get(source) instanceof ISphere sphere) {
				output.add(sphere);
				sphereList.removeIf(output::contains);
			} else
				break;
		}
		return output;
	}

	/**
	 * Pick a deity symbol
	 * 
	 * @param system
	 * @return
	 */
	public static IDeitySymbol pickSymbol(Collection<ISphere> spheres, Random source, IPartySystem system) {
		Set<IDeitySymbol> symbols = DeitySymbols.instance().getDeitySymbolMap().values();
		WeightedSet<IDeitySymbol> weighted = new WeightedSet<>(symbols,
				(sym) -> (0.1f / (system.deitiesBySymbol(sym).count() + 0.1f))
						* spheres.stream().mapToInt(sym::permittedOrPreferred).sum());
		return weighted.get(source);
	}

	/**
	 * Generates a suitable deity
	 */
	public static IDeity generateDeity(ServerLevel level, Random random, IPartySystem system) {

		Set<ISphere> spheres = pickSpheres(3, random, system);
		IDeitySymbol symbol = pickSymbol(spheres, random, system);
		ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
				.dropWhile((a) -> random.nextInt(ForgeRegistries.ENTITY_TYPES.getKeys().size()) > 5).findAny()
				.orElse(EntityType.getKey(EntityType.COW));
		ResourceLocation type2 = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
				.dropWhile((a) -> random.nextInt(ForgeRegistries.ENTITY_TYPES.getKeys().size()) > 5).findAny()
				.orElse(EntityType.getKey(EntityType.COW));

		SOTDMod.LOGGER.debug("[DEITY] Attempting name generation for deity");

		String finalName = null;
		for (int i = 0; i < 5; i++) {
			LanguageGen language = LanguageGen
					.getGenAfterReset(ModUtils.path(level.players().getFirst().getLanguage()));
			List<IPhoneme> ls = language.generatePhonemeSequence(4, 15);
			if (ls != null) {
				finalName = IPhoneme.toString(ls);
				finalName = ("" + finalName.charAt(0)).toUpperCase() + finalName.substring(1);
			}
		}
		// assert (finalName != null) : ("Failed to make name for deity...");

		IDeity dimde = IDeity.create(type.getPath() + "_" + type2.getPath(),
				TextUtils.literal(Optional.ofNullable(finalName).orElse(type.getPath())), spheres, Map.of(), symbol);
		SOTDMod.LOGGER.debug("[DEITY] " + finalName + " " + dimde.report());
		system.addParty(dimde, level);
		return dimde;
	}

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
	 * Begins an emanation for this deity; return null if it failed
	 * 
	 * @param emanation
	 * @param target
	 * @return
	 */
	public EmanationInstance triggerEmanation(IEmanation emanation, ISpellTargetInfo target);

	/**
	 * Selects a random emanation from the given category and runs it; returns null
	 * if failure or no such emanations exist
	 * 
	 * @param type
	 * @param target
	 * @return
	 */
	public default EmanationInstance triggerAnEmanation(DeityInteractionType type, ISpellTargetInfo target) {
		LogUtils.getLogger().debug("Deity " + this.uniqueName() + " looking for emanation of type " + type
				+ " with targeting info " + target);
		List<IEmanation> ems = Lists
				.newArrayList(this.spheres().stream().flatMap((s) -> s.emanationsOfType(type).stream()).iterator());
		if (ems.isEmpty())
			return null;
		Collections.shuffle(ems);
		return triggerEmanation(ems.getFirst(), target);
	}

	/**
	 * Returns all the deity's currently running emanations
	 * 
	 * @return
	 */
	public Collection<EmanationInstance> runningEmanations();

	/**
	 * Interrupt a certain emanation and stop it
	 * 
	 * @param emanation
	 */
	public void stopEmanation(EmanationInstance instance);

	/**
	 * Stops all instances of the given emanation
	 * 
	 * @param emanation
	 */
	public default void stopEmanations(IEmanation emanation) {
		new HashSet<>(this.runningEmanations()).stream().filter((x) -> x.emanation().equals(emanation))
				.forEach((x) -> this.stopEmanation(x));
	}

	/**
	 * Stops all emanations of the given kind targeting this position
	 * 
	 * @param pos
	 */
	public default void stopEmanationsAt(IEmanation emanation, GlobalPos pos) {
		new HashSet<>(this.runningEmanations()).stream()
				.filter((x) -> x.emanation().equals(emanation)
						&& x.targetInfo().opTargetPos().filter((p) -> p.equals(pos)).isPresent())
				.forEach((x) -> this.stopEmanation(x));
	}

	/**
	 * Stops all emanations of the given kind targeting this entity
	 * 
	 * @param pos
	 */
	public default void stopEmanationsTargeting(IEmanation emanation, EntityReference<?> entity) {
		new HashSet<>(this.runningEmanations()).stream()
				.filter((x) -> x.emanation().equals(emanation)
						&& x.targetInfo().opTargetEntity().filter((p) -> p.equals(entity)).isPresent())
				.forEach((x) -> this.stopEmanation(x));
	}

	/**
	 * Stops all emanations of the given kind cast by this entity
	 * 
	 * @param pos
	 */
	public default void stopEmanationsCastBy(IEmanation emanation, UUID entity) {
		new HashSet<>(this.runningEmanations()).stream()
				.filter((x) -> x.emanation().equals(emanation)
						&& x.targetInfo().opCaster().filter((p) -> p.equals(entity)).isPresent())
				.forEach((x) -> this.stopEmanation(x));
	}

	/**
	 * Initiate a ritual
	 * 
	 * @param ritual
	 * @param center
	 * @param initiator
	 * @param parameters
	 */
	public void startRitual(IRitual ritual, GlobalPos center, Entity initiator,
			Map<RitualParameter, Integer> parameters);

	/**
	 * Returns the ritual that this emanation isntance is tied to (if any)
	 * 
	 * @param instance
	 * @return
	 */
	public RitualInstance getRitualSource(EmanationInstance instance);

	/**
	 * Returns the emanations tied to this ritual isntance
	 * 
	 * @param ritual
	 * @return
	 */
	public Collection<EmanationInstance> getEmanationsOf(RitualInstance ritual);

	/**
	 * Universal tick method for this deity, where it updates emanations and such
	 * 
	 * @param level
	 * @param time
	 */
	public void tick(ServerLevel level, long time);

}
