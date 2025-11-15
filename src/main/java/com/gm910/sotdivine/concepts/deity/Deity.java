package com.gm910.sotdivine.concepts.deity;

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

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.deity.personality.IDeityStat;
import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.party.Party;
import com.gm910.sotdivine.concepts.parties.party.relation.IPartyMemory;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.events.custom.EmanationEvent;
import com.gm910.sotdivine.magic.emanation.DeityInteractionType;
import com.gm910.sotdivine.magic.emanation.EmanationInstance;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.RitualInstance;
import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;
import com.gm910.sotdivine.magic.sphere.ISphere;
import com.gm910.sotdivine.util.StreamUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.base.Functions;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public non-sealed class Deity extends Party implements IDeity {

	protected Set<ISphere> spheres;
	protected Map<IDeityStat, Float> stats;
	protected IDeitySymbol symbol;
	protected Set<IRitual> rituals;

	protected Set<EmanationInstance> runningEmanations;
	protected Map<EmanationInstance, RitualInstance> emanationsToRituals;
	protected Multimap<RitualInstance, EmanationInstance> ritualsToEmanations;

	protected Deity(String unique, Collection<ISphere> spheres, Map<IDeityStat, Float> stats, Component name,
			IDeitySymbol symbol) {
		super(unique, false, false, false, Optional.ofNullable(name), Optional.empty());
		this.spheres = new HashSet<>(spheres);
		if (spheres.isEmpty()) {
			throw new IllegalArgumentException(unique + " requires at least one sphere");
		}
		this.stats = new HashMap<>(stats);
		this.runningEmanations = new HashSet<>();
		this.symbol = symbol;
		this.emanationsToRituals = new HashMap<>();
		this.ritualsToEmanations = MultimapBuilder.hashKeys().hashSetValues().build();
		this.rituals = new HashSet<>();
	}

	protected Deity(IParty partyData, List<ISphere> spheres, Map<IDeityStat, Float> stats, IDeitySymbol symbol,
			List<IRitual> rituals, List<EmanationInstance> runningEms,
			List<Pair<EmanationInstance, RitualInstance>> ritualEms) {
		super(false, partyData.uniqueName(), false, false, partyData.memberCollection(),
				new ArrayList<IPartyMemory>(partyData.allMemories()),
				partyData.knownParties().stream()
						.collect(Collectors.toMap(Functions.identity(), (u) -> partyData.relationshipWith(u).get())),
				partyData.descriptiveName(), partyData.ownedResources().stream()
						.map((y) -> Pair.of(y, partyData.getResourceAmount(y))).collect(Collectors.toList()));
		this.spheres = new HashSet<>(spheres);
		if (spheres.isEmpty()) {
			throw new IllegalArgumentException(partyData.uniqueName() + " requires at least one sphere");
		}
		this.stats = new HashMap<>(stats);
		this.symbol = symbol;
		this.runningEmanations = new HashSet<>(runningEms);
		this.runningEmanations.forEach((e) -> e.coalesce(this));
		this.rituals = new HashSet<>(rituals);
		this.emanationsToRituals = new HashMap<>();
		this.ritualsToEmanations = MultimapBuilder.hashKeys().hashSetValues().build();
		ritualEms.forEach((pair) -> {
			emanationsToRituals.put(pair.getFirst(), pair.getSecond());
			ritualsToEmanations.put(pair.getSecond(), pair.getFirst());
		});
	}

	private void removeEmanation(EmanationInstance instance, boolean interrupt) {
		this.runningEmanations.remove(instance);
		RitualInstance ritIns = this.emanationsToRituals.remove(instance);
		this.ritualsToEmanations.removeAll(ritIns);
		if (ritIns != null) {
			ritIns.ritual().signalStop(ritIns, level, this, ritIns.focusPos(), ritIns.successfulPattern(),
					ritIns.intensity(), interrupt);
		}

	}

	/*
	 * private void removeRitual(RitualInstance instance) { for (EmanationInstance
	 * em : this.emanationRituals.removeAll(instance)) {
	 * this.ritualEmanations.remove(em); this.runningEmanations.remove(em); } }
	 */

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
				this.removeEmanation(instance, false);
			}
		}
		for (EmanationInstance instance : toInterrupt) {
			this.stopEmanation(instance);
		}
		ISanctuarySystem sanctuaries = ISanctuarySystem.get(level);
		sanctuaries.getCompleteSanctuaries().filter((s) -> !s.isClaimed())
				.filter((s) -> s.symbol() != null && s.symbol().equals(this.symbol)).forEach((sanct) -> {
					LogUtils.getLogger()
							.debug("Deity "
									+ this.descriptiveName().orElse(Component.literal(this.uniqueName())).getString()
									+ " claimed a sanctuary");
					sanct.claim(this.uniqueName());
					for (BlockPos border : sanct.boundaryPositions()) {
						if (DeitySymbols.instance().convertSymbolsAtPosition(level, border, symbol)) {
							this.triggerAnEmanation(DeityInteractionType.SYMBOL_CONVERSION,
									ISpellTargetInfo.builder(this, level).targetPos(border).build(), 1.0f);
						} else {
							this.triggerAnEmanation(DeityInteractionType.SYMBOL_RECOGNITION,
									ISpellTargetInfo.builder(this, level).targetPos(border).build(), 1.0f);
						}
					}
				});
	}

	@Override
	public Collection<EmanationInstance> runningEmanations() {
		return Collections.unmodifiableCollection(this.runningEmanations);
	}

	@Override
	public void stopEmanation(EmanationInstance instance) {
		if (this.runningEmanations.contains(instance) || this.emanationsToRituals.containsKey(instance)) {
			EmanationEvent.End event = new EmanationEvent.End(instance.emanation(), this, instance.targetInfo(),
					instance.getTicks(), true, false, instance.extraData);
			if (EmanationEvent.End.BUS.post(event)) {
				LogUtils.getLogger().debug("Continuing " + instance + " due to interrupt event cancellation");
				instance.extraData = event.getFinalData();
			} else {
				instance.extraData = event.getFinalData();
				instance.emanation()
						.interrupt(instance.completeSelf(this, instance.targetInfo().level() == null ? level : null));

				this.removeEmanation(instance, true);
			}
		} else {
			throw new IllegalArgumentException(
					"Deity " + this + " is asked to remove non-running emanation " + instance);
		}

	}

	@Override
	public EmanationInstance triggerEmanation(IEmanation emanation, ISpellTargetInfo target, float intensity,
			@Nullable RitualInstance ritual) {

		// run an event
		EmanationEvent.Start event = new EmanationEvent.Start(emanation, this, target);
		if (EmanationEvent.Start.BUS.post(event)) {
			LogUtils.getLogger().debug("Not running emanation " + emanation + " because an event was canceled");
			return null;
		}

		EmanationInstance instance = new EmanationInstance(event.getEmanation(),
				event.getTargetInfo().complete(this, event.getTargetInfo().level() == null ? this.level : null), 0);

		if (emanation.checkIfCanTrigger(instance)) {
			// LogUtils.getLogger()
			// .debug("Deity " + this + " running emanation " + emanation + " with targeting
			// info " + target);
			boolean fail = emanation.trigger(instance, intensity);
			if (!fail) {
				this.runningEmanations.add(instance);
				if (ritual != null) {
					this.emanationsToRituals.put(instance, ritual);
					this.ritualsToEmanations.put(ritual, instance);
				}
			} else {
				LogUtils.getLogger().debug("Emanation failed " + emanation + " with targeting info " + target);
			}
			return fail ? null : instance;
		}
		return instance;
	}

	@Override
	public Collection<ISphere> spheres() {
		return Collections.unmodifiableCollection(spheres);
	}

	@Override
	public boolean permitsInSanctuary(ServerLevel level, Entity entity) {
		if (spheres.stream().flatMap((s) -> s.getGenres(GenreTypes.FORBIDDEN_MOB).stream())
				.allMatch((s) -> s.matches(level, entity))) {
			return false;
		}
		return true;
	}

	@Override
	public Collection<IRitual> getRituals() {
		return Collections.unmodifiableCollection(this.rituals);
	}

	@Override
	public IDeitySymbol symbol() {
		return this.symbol;
	}

	@Override
	public float statValue(IDeityStat stat) {
		return stats.getOrDefault(stat, stat.defaultValue());
	}

	@Override
	public RitualInstance getRitualSource(EmanationInstance instance) {
		return this.emanationsToRituals.get(instance);
	}

	@Override
	public Collection<EmanationInstance> getEmanationsOf(RitualInstance ritual) {
		return Collections.unmodifiableCollection(this.ritualsToEmanations.get(ritual));
	}

	@Override
	public String report() {
		return "Deity{spheres=" + this.spheres + ",stats=" + this.stats + ",symbol=" + this.symbol
				+ ",running_emanations=" + this.runningEmanations + ",partyData=" + super.report() + "}";
	}

	@Override
	public String report(Level level) {
		return "Deity{spheres=" + this.spheres + ",stats=" + this.stats + ",symbol=" + this.symbol
				+ ",running_emanations=" + this.runningEmanations + ",partyData=" + super.report(level) + "}";
	}

	@Override
	public Component descriptiveInfo(Level level) {
		return TextUtils.transPrefix("sotd.cmd.deityinfo",
				this.spheres.stream().map(ISphere::displayName).collect(StreamUtils.componentCollectorCommasPretty()),
				Component.translatableEscape(this.symbol.bannerPattern().get().translationKey()));
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
