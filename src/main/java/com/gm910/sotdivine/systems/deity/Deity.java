package com.gm910.sotdivine.systems.deity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.gm910.sotdivine.events.custom.EmanationEvent;
import com.gm910.sotdivine.systems.deity.emanation.EmanationInstance;
import com.gm910.sotdivine.systems.deity.emanation.IEmanation;
import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.systems.deity.personality.IDeityStat;
import com.gm910.sotdivine.systems.deity.ritual.IRitual;
import com.gm910.sotdivine.systems.deity.ritual.RitualInstance;
import com.gm910.sotdivine.systems.deity.ritual.properties.RitualParameter;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.symbol.IDeitySymbol;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party.Party;
import com.gm910.sotdivine.systems.party.relation.IPartyMemory;
import com.gm910.sotdivine.util.StreamUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.base.Functions;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class Deity extends Party implements IDeity {

	Set<ISphere> spheres;
	Map<IDeityStat, Float> stats;
	IDeitySymbol symbol;
	Set<EmanationInstance> runningEmanations;
	Map<EmanationInstance, RitualInstance> ritualEmanations;
	/** An "inverse map" to permit lookups in both directions */
	Multimap<RitualInstance, EmanationInstance> emanationRituals;

	protected Deity(String unique, Collection<ISphere> spheres, Map<IDeityStat, Float> stats, Component name,
			IDeitySymbol symbol) {
		super(unique, false, false, false, Optional.ofNullable(name));
		this.spheres = new HashSet<>(spheres);
		this.stats = new HashMap<>(stats);
		this.runningEmanations = new HashSet<>();
		this.symbol = symbol;
		this.ritualEmanations = new HashMap<>();
		this.emanationRituals = MultimapBuilder.hashKeys().hashSetValues().build();
	}

	protected Deity(IParty partyData, List<ISphere> spheres, Map<IDeityStat, Float> stats, IDeitySymbol symbol,
			List<EmanationInstance> runningEms, List<Pair<EmanationInstance, RitualInstance>> ritualEms) {
		super(false, partyData.uniqueName(), false, false,
				Optional.of(partyData.memberCollection().stream().map(Object::toString).collect(Collectors.toList())),
				new ArrayList<IPartyMemory>(partyData.allMemories()),
				partyData.knownParties().stream()
						.collect(Collectors.toMap(Functions.identity(), (u) -> partyData.relationshipWith(u).get())),
				partyData.descriptiveName(), partyData.ownedResources().stream()
						.map((y) -> Pair.of(y, partyData.getResourceAmount(y))).collect(Collectors.toList()));
		this.spheres = new HashSet<>(spheres);
		this.stats = new HashMap<>(stats);
		this.symbol = symbol;
		this.runningEmanations = new HashSet<>(runningEms);
		this.runningEmanations.forEach((e) -> e.coalesce(this));
		this.ritualEmanations = new HashMap<>();
		this.emanationRituals = MultimapBuilder.hashKeys().hashSetValues().build();
		ritualEms.forEach((pair) -> {
			ritualEmanations.put(pair.getFirst(), pair.getSecond());
			emanationRituals.put(pair.getSecond(), pair.getFirst());
		});
	}

	@Override
	public void tick(ServerLevel level, long time) {
		Set<EmanationInstance> toEnd = new HashSet<>();
		Set<EmanationInstance> toFail = new HashSet<>();
		Set<EmanationInstance> toInterrupt = new HashSet<>();

		for (EmanationInstance instance : this.runningEmanations) {
			EmanationEvent.Update event = new EmanationEvent.Update(instance.emanation(), this, instance.targetInfo(),
					instance.getTicks(), instance.extraData);
			if (EmanationEvent.Update.BUS.post(event)) {
				LogUtils.getLogger().debug("Interrupting " + instance + " due to event cancellation");
				toInterrupt.add(instance);
			} else {
				instance.extraData = event.getData();
				if (instance.emanation().checkIfCanTick(instance.completeSelf(this, level))) {
					if (instance.emanation().tick(instance.completeSelf(this, level))) {
						toFail.add(instance);
					} else {
						instance.incrementTicks();
					}
				} else {
					toEnd.add(instance);
				}
			}
		}
		Set<EmanationInstance> nonInterruptEndings = new HashSet<>();
		nonInterruptEndings.addAll(toEnd);
		nonInterruptEndings.addAll(toFail);
		for (EmanationInstance instance : nonInterruptEndings) {
			EmanationEvent.End event = new EmanationEvent.End(instance.emanation(), this, instance.targetInfo(),
					instance.getTicks(), false, toFail.contains(instance), instance.extraData);
			if (EmanationEvent.End.BUS.post(event)) {
				LogUtils.getLogger().debug("Continuing " + instance + " due to end event cancellation");
				instance.extraData = event.getFinalData();
			} else {
				this.runningEmanations.remove(instance);
			}
		}
		for (EmanationInstance instance : toInterrupt) {
			this.stopEmanation(instance);
		}
	}

	@Override
	public Collection<EmanationInstance> runningEmanations() {
		return this.runningEmanations;
	}

	@Override
	public void stopEmanation(EmanationInstance instance) {
		if (this.runningEmanations.contains(instance)) {

			EmanationEvent.End event = new EmanationEvent.End(instance.emanation(), this, instance.targetInfo(),
					instance.getTicks(), true, false, instance.extraData);
			if (EmanationEvent.End.BUS.post(event)) {
				LogUtils.getLogger().debug("Continuing " + instance + " due to interrupt event cancellation");
				instance.extraData = event.getFinalData();
			} else {
				instance.extraData = event.getFinalData();
				instance.emanation()
						.interrupt(instance.completeSelf(this, instance.targetInfo().level() == null ? level : null));
				this.runningEmanations.remove(instance);
			}
		} else {
			throw new IllegalArgumentException(
					"Deity " + this + " is asked to remove non-running emanation " + instance);
		}

	}

	@Override
	public EmanationInstance triggerEmanation(IEmanation emanation, ISpellTargetInfo target) {

		// run an event
		EmanationEvent.Start event = new EmanationEvent.Start(emanation, this, target);
		if (EmanationEvent.Start.BUS.post(event)) {
			LogUtils.getLogger().debug("Not running emanation " + emanation + " because an event was canceled");
			return null;
		}

		EmanationInstance instance = new EmanationInstance(event.getEmanation(),
				event.getTargetInfo().complete(this, event.getTargetInfo().level() == null ? this.level : null), 0);

		if (emanation.checkIfCanTrigger(instance)) {
			LogUtils.getLogger().debug("Deity " + this.uniqueName() + " running emanation " + emanation
					+ " with targeting info " + target);
			boolean fail = emanation.trigger(instance);
			if (!fail) {
				this.runningEmanations.add(instance);
			}
			return fail ? null : instance;
		}
		return instance;
	}

	@Override
	public Collection<ISphere> spheres() {
		return spheres;
	}

	@Override
	public IDeitySymbol symbol() {
		return this.symbol;
	}

	@Override
	public float statValue(IDeityStat stat) {
		return stats.getOrDefault(stat, 0f);
	}

	@Override
	public RitualInstance getRitualSource(EmanationInstance instance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void startRitual(IRitual ritual, GlobalPos center, Entity initiator,
			Map<RitualParameter, Integer> parameters) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<EmanationInstance> getEmanationsOf(RitualInstance ritual) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String report() {
		return "Deity{spheres=" + this.spheres + ",stats=" + this.stats + ",symbol=" + this.symbol + ",partyData="
				+ super.report() + "}";
	}

	@Override
	public String report(ServerLevel level) {
		return "Deity{spheres=" + this.spheres + ",stats=" + this.stats + ",symbol=" + this.symbol + ",partyData="
				+ super.report(level) + "}";
	}

	@Override
	public Component descriptiveInfo(ServerLevel level) {

		return TextUtils.transPrefix("sotd.cmd.deityinfo",
				"[" + this.spheres.stream().map(ISphere::name).map(ResourceLocation::getPath)
						.collect(StreamUtils.setStringCollector(",")) + "]",
				TextUtils.transPrefix(this.symbol.bannerPattern().get().translationKey()));
	}

	@Override
	public String toString() {
		return "Deity{"
				+ (this.descriptiveName().isPresent() ? "name=\"" + this.descriptiveName().get().getString() + "\""
						: "id=" + this.uniqueName())
				+ ",spheres={" + this.spheres().stream().map(ISphere::name).map(Object::toString)
						.collect(StreamUtils.setStringCollector(","))
				+ "}}";
	}

}
