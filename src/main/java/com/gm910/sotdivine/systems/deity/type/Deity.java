package com.gm910.sotdivine.systems.deity.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.gm910.sotdivine.systems.deity.emanation.EmanationInstance;
import com.gm910.sotdivine.systems.deity.emanation.IEmanation;
import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.systems.deity.personality.IDeityStat;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.symbol.IDeitySymbol;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party.Party;
import com.gm910.sotdivine.systems.party.relation.IPartyMemory;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.base.Functions;
import com.mojang.datafixers.util.Pair;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public class Deity extends Party implements IDeity {

	private Set<ISphere> spheres;
	private Map<IDeityStat, Float> stats;
	private IDeitySymbol symbol;
	private Set<EmanationInstance> runningEmanations;

	protected Deity(String unique, Collection<ISphere> spheres, Map<IDeityStat, Float> stats, Component name,
			IDeitySymbol symbol) {
		super(unique, false, false, false, Optional.ofNullable(name));
		this.spheres = new HashSet<>(spheres);
		this.stats = new HashMap<>(stats);
		this.runningEmanations = new HashSet<>();
		this.symbol = symbol;
	}

	protected Deity(IParty partyData, List<ISphere> spheres, Map<IDeityStat, Float> stats, IDeitySymbol symbol) {
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

	}

	@Override
	public void tick(ServerLevel level, long time) {
		Set<EmanationInstance> toRemove = new HashSet<>();
		for (EmanationInstance instance : this.runningEmanations) {
			if (instance.emanation.checkIfCanTick(instance.completeSelf(this, level))) {
				if (instance.emanation.tick(instance.completeSelf(this, level))) {
					toRemove.add(instance);
				} else {
					instance.incrementTicks();
				}
			} else {
				toRemove.add(instance);
			}
		}
		toRemove.forEach((i) -> this.runningEmanations.remove(i));
	}

	@Override
	public Collection<EmanationInstance> runningEmanations() {
		return this.runningEmanations;
	}

	@Override
	public void stopEmanation(EmanationInstance instance) {
		if (this.runningEmanations.contains(instance)) {
			instance.emanation
					.interrupt(instance.completeSelf(this, instance.targetInfo().level() == null ? level : null));
			this.runningEmanations.remove(instance);
		} else {
			throw new IllegalArgumentException(
					"Deity " + this + " is asked to remove non-running emanation " + instance);
		}

	}

	@Override
	public boolean triggerEmanation(IEmanation emanation, ISpellTargetInfo target) {
		EmanationInstance instance = new EmanationInstance(emanation,
				target.complete(this, target.level() == null ? this.level : null), 0);
		if (emanation.checkIfCanTrigger(instance)) {
			boolean fail = emanation.trigger(instance);
			if (!fail) {
				this.runningEmanations.add(instance);
			}
			return fail;
		}
		return false;
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

		return ModUtils.trans("sotd.cmd.deityinfo",
				"[" + this.spheres.stream().map(ISphere::name).map(ResourceLocation::getPath)
						.collect(ModUtils.setStringCollector(",")) + "]",
				ModUtils.trans(this.symbol.bannerPattern().get().translationKey()));
	}

	@Override
	public String toString() {
		return "Deity{"
				+ (this.descriptiveName().isPresent() ? "name=\"" + this.descriptiveName().get().getString() + "\""
						: "id=" + this.uniqueName())
				+ ",spheres={" + this.spheres().stream().map(ISphere::name).map(Object::toString)
						.collect(ModUtils.setStringCollector(","))
				+ "}}";
	}

}
