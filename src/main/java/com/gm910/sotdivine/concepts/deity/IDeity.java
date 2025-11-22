package com.gm910.sotdivine.concepts.deity;

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
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.concepts.deity.personality.IDeityStat;
import com.gm910.sotdivine.concepts.parties.IPartyLister.IDeityInfo;
import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.language.Languages;
import com.gm910.sotdivine.magic.emanation.DeityInteractionType;
import com.gm910.sotdivine.magic.emanation.EmanationInstance;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.RitualInstance;
import com.gm910.sotdivine.magic.sphere.ISphere;
import com.gm910.sotdivine.magic.sphere.Spheres;
import com.gm910.sotdivine.util.TextUtils;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.Registry;
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
public sealed interface IDeity extends IParty, IDeityInfo permits Deity {

	public static final MapCodec<IDeity> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> // Given an emanation
	instance.group(IParty.MAP_CODEC.forGetter((x) -> x),
			Codec.list(Spheres.BY_NAME_CODEC).fieldOf("spheres").forGetter((x) -> new ArrayList<>(x.spheres())),
			Codec.unboundedMap(IDeityStat.REGISTRY.byNameCodec(), Codec.FLOAT).optionalFieldOf("stats", Map.of())
					.forGetter((pa) -> IDeityStat.REGISTRY.entrySet().stream()
							.map((e) -> Map.entry(e.getValue(), pa.statValue(e.getValue())))
							.filter((e) -> e.getValue() != e.getKey().defaultValue())
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))),
			DeitySymbols.BY_NAME_CODEC.fieldOf("symbol").forGetter(IDeity::symbol),
			Codec.list(IRitual.codec()).fieldOf("rituals").forGetter((x) -> new ArrayList<IRitual>(x.getRituals())),
			Codec.list(EmanationInstance.CODEC).optionalFieldOf("running_emanations", List.of())
					.forGetter((x) -> new ArrayList<>(x.runningEmanations())),
			Codec.compoundList(EmanationInstance.CODEC, RitualInstance.codec())
					.optionalFieldOf("ritual_emanations", List.of()).forGetter((x) -> ((Deity) x).emanationsToRituals
							.entrySet().stream().map((p) -> Pair.of(p.getKey(), p.getValue())).toList())

	).apply(instance, Deity::new));

	public static IDeity create(String name, Component displayName, Collection<ISphere> spheres,
			Map<IDeityStat, Float> stats, IDeitySymbol symbol) {
		return new Deity(name, spheres, stats, displayName, symbol);
	}

	/**
	 * A number representing the bonus ratio that an unselected elment gets over a
	 * selected one
	 */
	public static final float UNPICKED_BONUS_FACTOR = 10f;

	/**
	 * Pick a number of deity spheres
	 * 
	 * @param max
	 * @param system
	 * @return
	 */
	public static Set<ISphere> pickSpheres(ServerLevel level, int max, int avg, Random source, IPartySystem system) {
		Set<ISphere> spheres = Spheres.instance().getSphereMap().values();
		Map<ISphere, Integer> sphereCounts = spheres.stream().map(
				(x) -> Map.entry(x, (int) system.allDeities().stream().filter((d) -> d.spheres().contains(x)).count()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		WeightedSet<ISphere> sphereList = new WeightedSet<>(sphereCounts.keySet(),
				(x) -> (1 / UNPICKED_BONUS_FACTOR) / (sphereCounts.get(x) + (1 / UNPICKED_BONUS_FACTOR)));
		LogUtils.getLogger().debug("Sphere distributions: " + sphereList.asWeightMap());
		if (sphereList.isEmpty())
			return Set.of();
		if (max == 1) {
			return Set.of(sphereList.get(source));
		}
		Set<ISphere> output = new HashSet<>();
		Registry<ISphere> sphereRegistry = level.registryAccess().lookupOrThrow(ModRegistries.SPHERES);

		int iters = (int) Math.round(Math.max(Math.min(source.nextGaussian(avg, 1), max), 1));
		for (int i = 0; i < iters; i++) {
			if (sphereList.get(source) instanceof ISphere sphere) {
				if (Math.min(source.nextFloat(), source.nextFloat()) > UNPICKED_BONUS_FACTOR
						* sphereList.getWeight(sphere)) {
					break; // we might just forget picking a sphere if the sphere's probability is too low
				}
				output.add(sphere);
				for (Named<ISphere> tag : sphereRegistry.getTags()
						.filter((t) -> t.contains(sphereRegistry.wrapAsHolder(sphere))).toList()) {
					tag.stream().map(Holder::get).forEach((similar) -> {
						if (!similar.equals(sphere)) {
							sphereList.setWeight(similar, sphereList.getWeight(sphere) * 4);
						}
					});
				}
				sphereList.remove(sphere);
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
		Set<IDeitySymbol> symbols = new HashSet<>(DeitySymbols.instance().getDeitySymbolMap().values());
		for (IDeitySymbol sym : DeitySymbols.instance().getDeitySymbolMap().values()) {
			if (system.deitiesBySymbol(sym).findAny().isPresent()) {
				symbols.remove(sym);
			}
		}
		if (symbols.isEmpty()) {
			return null;
		}
		WeightedSet<IDeitySymbol> weighted = new WeightedSet<>(symbols,
				(sym) -> Math.pow(UNPICKED_BONUS_FACTOR, spheres.stream().mapToDouble(sym::permittedOrPreferred).sum())
						- 1);
		return weighted.get(source);
	}

	/**
	 * Generates rituals for this deity and adds them to the deity
	 * 
	 * @param deity
	 */
	public static void addRitualsTo(ServerLevel level, IDeity deity) {

		((Deity) deity).rituals.addAll(IRitual.generateRituals(level, deity));
	}

	/**
	 * Generates a suitable deity
	 */
	public static IDeity generateDeity(ServerLevel level, String language, Random random, IPartySystem system) {

		Set<ISphere> spheres = pickSpheres(level, 3, 1, random, system);
		IDeitySymbol symbol = pickSymbol(spheres, random, system);
		if (symbol == null)
			return null;
		ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
				.dropWhile((a) -> random.nextInt(ForgeRegistries.ENTITY_TYPES.getKeys().size()) > 5).findAny()
				.orElse(EntityType.getKey(EntityType.COW));
		ResourceLocation type2 = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
				.dropWhile((a) -> random.nextInt(ForgeRegistries.ENTITY_TYPES.getKeys().size()) > 5).findAny()
				.orElse(EntityType.getKey(EntityType.COW));

		LogUtils.getLogger().debug("[DEITY] Attempting name generation for deity");

		String finalName = null;
		for (int i = 0; i < 5; i++) {
			Optional<String> ls = Languages.instance().get(language).map((l) -> l.generateName(1, 7, level.random));
			if (ls.isPresent()) {
				finalName = ls.get();
				finalName = ("" + finalName.charAt(0)).toUpperCase() + finalName.substring(1);
				break;
			}
		}
		// assert (finalName != null) : ("Failed to make name for deity...");

		Deity dimde = (Deity) IDeity.create(type.getPath() + "_" + type2.getPath(),
				TextUtils.literal(Optional.ofNullable(finalName).orElse(type.getPath())), spheres, Map.of(), symbol);
		addRitualsTo(level, dimde);

		LogUtils.getLogger().debug("[DEITY] " + finalName + " " + dimde.report());

		system.addParty(dimde, level);

		return dimde;
	}

	/**
	 * All rituals that this deity can patronize
	 * 
	 * @return
	 */
	public Collection<IRitual> getRituals();

	@Override
	public default IDeity becomeDeity(ServerLevel level) {
		return this;
	}

	/**
	 * Return all blocks or entities with a symbol of this deity
	 * 
	 * @param level
	 * @param from
	 * @param radius
	 * @return
	 */
	public default Stream<Either<Entity, BlockPos>> findDeitySymbols(ServerLevel level, BlockPos from, double radius) {
		return IPartySystem.get(level).findDeitySymbols(level, from, radius).filter((s) -> s.getValue().equals(this))
				.map((s) -> s.getKey());
	}

	/**
	 * Begins an emanation for this deity; return null if it failed
	 * 
	 * @param emanation
	 * @param target
	 * @return
	 */
	public EmanationInstance triggerEmanation(IEmanation emanation, ISpellTargetInfo target, float intensity,
			@Nullable RitualInstance source);

	/**
	 * Selects a random emanation from the given category and runs it; returns null
	 * if failure or no such emanations exist
	 * 
	 * @param type
	 * @param target
	 * @return
	 */
	public default EmanationInstance triggerAnEmanation(DeityInteractionType type, ISpellTargetInfo target,
			float intensity) {
		LogUtils.getLogger().debug("Deity " + this.uniqueName() + " looking for emanation of type " + type
				+ " with targeting info " + target);
		List<IEmanation> ems = Lists
				.newArrayList(this.spheres().stream().flatMap((s) -> s.emanationsOfType(type).stream()).iterator());
		if (ems.isEmpty()) {
			LogUtils.getLogger().debug("No such emanation found");
			return null;
		}
		Collections.shuffle(ems);
		return triggerEmanation(ems.getFirst(), target, intensity, null);
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
	 * Stops all emanations of the given kind targeting this rawPosition
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

	/**
	 * Whether this deity permits this kind of entity in its sanctuary
	 */
	public boolean permitsInSanctuary(ServerLevel level, Entity entity);

}
