package com.gm910.sotdivine.magic.ritual;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;

import com.gm910.sotdivine.concepts.deity.Deity;
import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.magic.emanation.DeityInteractionType;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatternSet;
import com.gm910.sotdivine.magic.ritual.properties.IRitualParameters;
import com.gm910.sotdivine.magic.ritual.properties.RitualParameters;
import com.gm910.sotdivine.magic.ritual.properties.RitualQuality;
import com.gm910.sotdivine.magic.ritual.properties.RitualType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;
import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Rituals that can be invoked for deities, which includes both
 */
public sealed interface IRitual permits Ritual {

	/**
	 * Scans for the sanctuary at this position and its deity
	 * 
	 * @param world
	 * @param fromPos
	 * @param generalize = whether to generalize the search to all deities if no
	 *                   deities are found
	 * @return
	 */

	public static Optional<IDeity> identifyWinningDeity(ServerLevel world, BlockPos fromPos, double radius,
			boolean generalize) {
		return ISanctuarySystem.get(world).getSanctuaryAtPos(fromPos).filter((s) -> s.deityName() != null)
				.flatMap((n) -> IPartySystem.get(world).getDeityByName(n.deityName()));
	}

	/*
	public static Optional<Entry<IDeity, Stream<IDeity>>> identifyWinningDeity(ServerLevel world, BlockPos fromPos,
			double radius, boolean generalize) {
		IPartySystem system = IPartySystem.get(world);
		List<Entry<Either<Entity, BlockPos>, IDeity>> patternBearers = Lists
				.newArrayList(system.findDeitySymbols(world, fromPos, radius).iterator());
		// count which deity has the most
		Multiset<IDeity> deityCounts = HashMultiset.create();
		for (var entry : patternBearers) {
			deityCounts.add(entry.getValue());
		}
		Optional<Map.Entry<IDeity, Float>> enta = deityCounts.entrySet().stream()
				.map((en) -> Map.entry(en.getElement(), en.getCount() * en.getElement().statValue(DeityStat.POWER)))
				.max((s, s2) -> Float.compare(s.getValue(), s2.getValue()));
		if (enta.isEmpty()) {
			if (generalize) {
				LogUtils.getLogger().debug(
						"No symbols found around " + fromPos + ", (DEBUG) generalizing offering to all deities..");
				
	List<IDeity> deitets = Lists.newArrayList(system.allDeities().stream()
			.iterator());Collections.shuffle(deitets);return Optional.of(Map.entry(deitets.removeFirst(),deitets.stream()));}return Optional.empty();}
	float maxPower = enta.get().getValue();
	List<IDeity> competitors = new ArrayList<>();competitors.add(enta.get().getKey());deityCounts.entrySet().stream().filter((s)->s.getCount()*s.getElement().statValue(DeityStat.POWER)>=maxPower).forEach((s)->competitors.add(s.getElement()));Collections.shuffle(competitors);
	// sort this by power return it in case
	patternBearers.sort((s,s2)->-Float.compare(deityCounts.count(s.getValue())*s.getValue().statValue(DeityStat.POWER),deityCounts.count(s2.getValue())*s2.getValue().statValue(DeityStat.POWER)));
	var winner = competitors.getFirst();
	
	return Optional.of(Map.entry(winner,patternBearers.stream().filter((s)->!s.getValue().equals(winner)).map((s)->s.getValue()).distinct()));}*/

	/**
	 * Either converts the surrounding symbols to instrument the given deity, or
	 * just plays a recognition effect on them
	 * 
	 * @param world
	 * @param fromPos
	 * @param radius
	 */
	public static void convertOrRecognizeSymbols(ServerLevel world, BlockPos fromPos, double radius, IDeity winner) {
		IPartySystem system = IPartySystem.get(world);
		List<Entry<Either<Entity, BlockPos>, IDeity>> list = system.findDeitySymbols(world, fromPos, radius).toList();
		LogUtils.getLogger().debug("Converting symbols around " + fromPos + " (distance=" + radius + ") to that of "
				+ winner + ": " + list);
		for (var entry : list) {
			Either<Entity, BlockPos> either = entry.getKey();
			if (either.map((p) -> system.convertShieldPatterns(world, p, winner),
					(p) -> system.convertBannerPatterns(world, p, winner)).booleanValue()) {
				winner.triggerAnEmanation(DeityInteractionType.SYMBOL_CONVERSION,
						ISpellTargetInfo.builder(winner, world)
								.branch(either, (l, b) -> b.targetEntityAndPos(l), (r, b) -> b.targetPos(r)).build(),
						1.0f);
				/*if (didConversionEmanation != null)
					return;*/
			}
		}
	}

	/**
	 * Start the process of determining whether a ritual can be executed.
	 * 
	 * @param level          the world to do this in
	 * @param deity          the deity which is being checked
	 * @param causer         the uuid of the entity which is "responsible" for the
	 *                       ritual
	 * @param searchRadius   the radius to search for symbols
	 * @param triggerEvent   the "trigger"
	 * @param checkPositions the positions to try to use as the focus
	 * @return whether any ritual started
	 */
	public static boolean tryDetectAndInitiateAnyRitual(ServerLevel level, IDeity deity, UUID causer,
			double searchRadius, IRitualTriggerEvent triggerEvent, Collection<BlockPos> checkPositions) {
		IPartySystem system = IPartySystem.get(level);

		for (BlockPos focusPos : checkPositions) {
			List<Entry<IRitual, IRitualPattern>> rituals = Lists
					.newArrayList(system.getMatchingRituals(level, focusPos, deity, triggerEvent).iterator());
			Collections.sort(rituals, (e1, e2) -> {
				if (e1.getKey().ritualType() == RitualType.SPELL) {
					return -1;
				} else if (e2.getKey().ritualType() == RitualType.SPELL) {
					return 1;
				}
				return e2.getValue().blockCount() - e1.getValue().blockCount();
			});
			if (!rituals.isEmpty()) {
				LogUtils.getLogger().debug(
						"Found matching rituals for pos " + focusPos + " and event " + triggerEvent + "; iterating");
				for (Entry<IRitual, IRitualPattern> ritEntry : rituals) {

					Map<ItemEntity, Integer> offeringItems = ritEntry.getKey().offeringsPresent(level, focusPos,
							ritEntry.getValue());
					if (offeringItems != null) {
						LogUtils.getLogger().debug("Matched to ritual " + ritEntry);
						// IRitual.convertOrRecognizeSymbols(level, focusPos, searchRadius, deity);

						ritEntry.getKey().initiateRitual(level, deity, causer,
								GlobalPos.of(level.dimension(), focusPos), ritEntry.getValue(),
								new RitualParameters(Map.of()), triggerEvent, offeringItems);
						return true;
					} else {
						LogUtils.getLogger().debug("Inadequate offerings for " + focusPos + " and event " + triggerEvent
								+ ": " + ritEntry);
					}
				}
			} else {
			}
		}
		return false;
	}

	/**
	 * Return a set of all offering item entities present with the amount of the
	 * item that was "extracted", or null if they are not
	 * 
	 * @param level
	 * @param focus
	 * @param pattern
	 * @return
	 */
	public default Map<ItemEntity, Integer> offeringsPresent(ServerLevel level, BlockPos focus,
			IRitualPattern pattern) {
		Map<ItemEntity, Integer> items = new HashMap<>();
		Multiset<Set<? extends IGiveableGenreProvider<?, ?>>> countingOfferings = HashMultiset.create();
		int completedSets = 0;
		List<ItemEntity> entityList = Lists.newArrayList(pattern
				.getEntitiesInPattern(level, focus, EntityTypeTest.forClass(ItemEntity.class), Predicates.alwaysTrue())
				.iterator());
		Collections.shuffle(entityList);
		itemLoop: for (ItemEntity item : entityList) {
			var setsIterator = this.offerings().keySet().stream()
					.filter((s) -> countingOfferings.count(s) < offerings().get(s)) // only sets which are incomplete
					.filter((s) -> s.stream().anyMatch((g) -> g.matchesItem(level, item.getItem()))).iterator();
			// get count of the item entity
			if (setsIterator.hasNext()) {
				int count = item.getItem().getCount();
				while (setsIterator.hasNext() && count >= 0) {
					var offeringSet = setsIterator.next();
					int minCount = offerings().get(offeringSet); // min req count for this set
					/* Add as many instances to the set as the count of the stack; calculate how many we have overflowing higher than minCount */
					int overflow = countingOfferings.add(offeringSet, count) + count - minCount;
					if (overflow >= 0) {
						// if we have overflow or we perfectly hit the limit, mark completed
						completedSets++;
						// decrease count for next sets since offerings are a limited resource
						count = overflow;
					} else {
						// if overflow is negative (i.e. count did not complete the requisite amount)
						// then we are done with this item
						count = 0;
						break;
					}
				}
				items.put(item, item.getItem().getCount() - count);
			}

		}
		return completedSets >= offerings().size() ? items : null;
	}

	/**
	 *
	 * @return
	 */
	public static Codec<IRitual> codec() {
		return Ritual.codec();
	}

	/**
	 * Return the patterns the ritual uses
	 * 
	 * @return
	 */
	public RitualPatternSet patterns();

	/**
	 * Predicates for the blocks in this ritual's patterns
	 * 
	 * @return
	 */
	public Map<String, IPlaceableGenreProvider<?, ?>> symbols();

	/**
	 * Offerings needed for this ritual. At least the specified number of any
	 * offering from each set must be selected. E.g. if the distribution was:
	 * {(bone, arrow, egg)=1, (bottle_of_enchanting)=2}, then at least one bone,
	 * arrow, or egg must be picked, and at least two bottles of enchanting must be
	 * picked. Thus it can be thought of as "(bone >= 1 || arrow >= 1 || egg >= 1)
	 * && (bottle_of_enchanting >= 2)"
	 * 
	 * @return
	 */
	public Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> offerings();

	/**
	 * If this ritual triggers an emanation for the given kind of effect, return the
	 * emanation (Targeter)
	 * 
	 * @return
	 */
	public Optional<RitualEmanationTargeter> ritualEffect(RitualEffectType effect);

	/**
	 * The type of this ritual
	 * 
	 * @return
	 */
	public RitualType ritualType();

	/**
	 * The $quality of this ritual
	 * 
	 * @return
	 */
	public RitualQuality ritualQuality();

	/**
	 * Return this ritual's trigger
	 * 
	 * @return
	 */
	public IRitualTrigger trigger();

	/**
	 * Return all emanations of this ritual
	 * 
	 * @return
	 */
	public Collection<IEmanation> emanations();

	/**
	 * Coalesces the emanation emanation(s) in this (if any) with the emanation
	 * emanation of the deity to avoid redundancy
	 * 
	 * @param deity
	 * @return
	 */
	public Ritual coalesce(IDeity deity);

	/**
	 * Starts this ritual, creating/triggering an appropriate emanation instance and
	 * supplying the appropriate info
	 * 
	 * @param level
	 * @param deity
	 * @param caster
	 * @param atPos
	 * @param pattern   the pattern which successfully matched
	 * @param banners   banner positions for signifier detection
	 * @param shields   entities with symbol shields
	 * @param offerings entities that act as offerings and the portion of their
	 *                  count to remove
	 * @param
	 */
	public void initiateRitual(ServerLevel level, IDeity deity, UUID caster, GlobalPos atPos, IRitualPattern pattern,
			IRitualParameters parameters, IRitualTriggerEvent event, Map<ItemEntity, Integer> offerings);

	/**
	 * Signals to the ritual to run its 'interrupt' effect or 'fail' effect
	 * depending on what happened
	 * 
	 * @param level
	 * @param deity
	 * @param focusPos
	 * @param successfulPattern
	 * @param intensity
	 * @param interrupt
	 */
	public void signalStop(RitualInstance instance, ServerLevel level, Deity deity, GlobalPos focusPos,
			IRitualPattern successfulPattern, float intensity, boolean interrupt);

}
