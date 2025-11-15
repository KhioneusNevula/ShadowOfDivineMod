package com.gm910.sotdivine.magic.emanation.types.combined;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.gm910.sotdivine.magic.emanation.EmanationDataType;
import com.gm910.sotdivine.magic.emanation.EmanationInstance;
import com.gm910.sotdivine.magic.emanation.EmanationType;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.emanation.EmanationDataType.IEmanationInstanceData;
import com.gm910.sotdivine.magic.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.magic.emanation.spell.SpellAlignment;
import com.gm910.sotdivine.magic.emanation.spell.SpellPower;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.StreamUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;

/**
 * A "combined" emanation that executes a series of inner emanations, either in
 * sequence or simultaneously
 */
public class CombinedEmanation implements IEmanation {
	public static final Codec<CombinedEmanation> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group(Codec.list(IEmanation.codec()).fieldOf("values").forGetter(CombinedEmanation::getEmanationsList),
			Codec.STRING.comapFlatMap((s) -> {
				var out = ExecutionOrder.valueOf(s);
				if (out == null)
					return DataResult.error(() -> "Invalid execution order " + s);
				return DataResult.success(out);
			}, ExecutionOrder::name).fieldOf("order").forGetter(CombinedEmanation::getOrder),
			Codec.BOOL.optionalFieldOf("conditional").forGetter((x) -> Optional.of(x.getConditionality())),
			Codec.STRING.xmap(SpellAlignment::valueOf, SpellAlignment::name).optionalFieldOf("alignment")
					.forGetter((x) -> x.optionalSpellProperties().map(ISpellProperties::alignment)))
			.apply(instance, CombinedEmanation::new));

	private List<IEmanation> emanations;
	private ExecutionOrder order;
	private boolean conditional;
	private Optional<ISpellProperties> properties;
	private final boolean aggtargetsEntity;
	private final boolean aggtargetsPos;
	private final boolean aggdurative;

	public CombinedEmanation(Collection<IEmanation> emanations, ExecutionOrder order, Optional<Boolean> conditional,
			Optional<SpellAlignment> alignment) {
		if (emanations.isEmpty())
			throw new IllegalArgumentException();
		this.emanations = new ArrayList<>(emanations);
		this.order = order;
		this.conditional = conditional.filter(Boolean::booleanValue).isPresent();
		this.properties = alignment.map((s) -> ISpellProperties.create(s,
				emanations.stream()
						.flatMap((sp) -> sp.optionalSpellProperties().map(ISpellProperties::difficulty).stream())
						.max(Comparator.naturalOrder()).orElse(SpellPower.IMPOSSIBLE)));
		this.aggtargetsEntity = emanations.stream().anyMatch(IEmanation::targetsEntity);
		this.aggtargetsPos = emanations.stream().anyMatch(IEmanation::targetsPos);
		this.aggdurative = emanations.stream().anyMatch(IEmanation::isDurative);
	}

	public List<IEmanation> getEmanationsList() {
		return emanations;
	}

	@Override
	public boolean isDurative() {
		return this.aggdurative;
	}

	public ExecutionOrder getOrder() {
		return order;
	}

	public boolean getConditionality() {
		return conditional;
	}

	@Override
	public Optional<ISpellProperties> optionalSpellProperties() {
		return properties;
	}

	@Override
	public boolean targetsEntity() {
		return aggtargetsEntity;
	}

	@Override
	public boolean targetsPos() {
		return aggtargetsPos;
	}

	@Override
	public boolean checkIfCanTrigger(EmanationInstance instance) {
		return this.checkIfCanTick(new EmanationInstance(instance.emanation(), instance.targetInfo(), -1));
	}

	@Override
	public boolean checkIfCanTick(EmanationInstance mainInstance) {
		boolean initial = mainInstance.getTicks() < 0;
		switch (order) {
		case simultaneous: {
			if (initial) {
				mainInstance.extraData = new SimultaneousInstance(new ArrayList<>(emanations), false,
						mainInstance.getIntensity());
			}
			SimultaneousInstance simult = (SimultaneousInstance) mainInstance.extraData;
			boolean b = false;
			Set<IEmanation> toRemove = new HashSet<>();
			for (int i = 0; i < simult.list.size(); i++) {
				IEmanation eman = simult.list.get(i).getKey();
				EmanationInstance instanceInner = new EmanationInstance(eman, mainInstance.targetInfo(),
						mainInstance.getTicks());
				boolean can = initial ? eman.checkIfCanTrigger(instanceInner) : eman.checkIfCanTick(instanceInner);
				if (!can) {
					if (conditional) {
						return false;
					}
					toRemove.add(simult.list.get(i).getKey()); // remove if it fails
				} else {
					simult.list.set(i, Map.entry(eman, instanceInner.extraData));
				}
				b = b || can;
			}
			simult.list.removeIf((x) -> toRemove.contains(x.getKey()));
			return b;
		}
		default: {
			SequentialInstance sequential;
			List<IEmanation> ordering;
			if (initial) {
				ordering = new ArrayList<>(emanations);
				if (order == ExecutionOrder.random) {
					Collections.shuffle(ordering);
				}
				sequential = new SequentialInstance(ordering,
						new EmanationInstance(ordering.getFirst(), mainInstance.targetInfo(), -1),
						mainInstance.getIntensity());
				mainInstance.extraData = sequential;

			} else {
				sequential = (SequentialInstance) mainInstance.extraData;
				ordering = sequential.list;
			}

			while (!ordering.isEmpty()) {
				IEmanation first = ordering.getFirst();
				boolean output = false;
				if (sequential.instance.getTicks() < 0) {
					output = first.checkIfCanTrigger(sequential.instance);
				} else {
					output = first.checkIfCanTick(sequential.instance);
				}
				if (output) {
					mainInstance.extraData = sequential;
					return true;
				} else {
					ordering.removeFirst();
					if (!ordering.isEmpty()) {
						sequential = new SequentialInstance(ordering,
								new EmanationInstance(ordering.getFirst(), mainInstance.targetInfo(), -1),
								mainInstance.getIntensity());
					} else {
						break;
					}
				}
			}
			return false;

		}
		}
	}

	@Override
	public boolean trigger(EmanationInstance mainInstance, float intensity) {
		EmanationInstance copy = new EmanationInstance(mainInstance.emanation(), mainInstance.targetInfo(), -1,
				intensity);
		boolean out = tick(copy);
		mainInstance.extraData = mainInstance.extraData;
		return out;
	}

	@Override
	public boolean tick(EmanationInstance mainInstance) {
		boolean trigger = mainInstance.getTicks() < 0;
		switch (order) {
		case simultaneous:
			SimultaneousInstance simult = (SimultaneousInstance) mainInstance.extraData;
			Set<IEmanation> toRemove = new HashSet<>();
			for (int i = 0; i < simult.list.size(); i++) {
				Entry<IEmanation, IEmanationInstanceData> entry = simult.list.get(i);
				EmanationInstance newInst = new EmanationInstance(mainInstance.emanation(), mainInstance.targetInfo(),
						mainInstance.getTicks());
				newInst.extraData = entry.getValue();
				boolean out = trigger ? entry.getKey().trigger(newInst, simult.intensity)
						: entry.getKey().tick(newInst);
				if (!out) {
					if (conditional) {
						for (int j = i + 1; j < simult.list.size(); j++) {
							EmanationInstance interruptionInst = new EmanationInstance(simult.list.get(j).getKey(),
									mainInstance.targetInfo(), mainInstance.getTicks());
							simult.list.get(j).getKey().interrupt(interruptionInst);
						}
						return true;
					}
					toRemove.add(entry.getKey());
				} else {
					simult.list.set(i, Map.entry(entry.getKey(), newInst.extraData));
				}
			}
			simult.list.removeIf((x) -> toRemove.contains(x.getKey()));
			return simult.list.isEmpty();
		default:
			SequentialInstance sequence = (SequentialInstance) mainInstance.extraData;
			EmanationInstance instance = sequence.instance;
			IEmanation emanation = sequence.list.getFirst();
			boolean fail = trigger ? emanation.trigger(instance, sequence.intensity) : emanation.tick(instance);
			if (fail) {
				if (conditional) {
					return true;
				}
				sequence.list.removeFirst();
				if (!sequence.list.isEmpty()) {
					sequence = new SequentialInstance(sequence.list,
							new EmanationInstance(sequence.list.getFirst(), mainInstance.targetInfo(), -1),
							sequence.intensity);
				}
				mainInstance.extraData = sequence;
				return sequence.list.isEmpty();
			}
			return false;
		}
	}

	@Override
	public void interrupt(EmanationInstance instance) {
		switch (order) {
		case simultaneous:
			SimultaneousInstance simult = (SimultaneousInstance) instance.extraData;
			for (Entry<IEmanation, IEmanationInstanceData> emanation : simult.list) {
				EmanationInstance instance2 = new EmanationInstance(emanation.getKey(), instance.targetInfo(),
						instance.getTicks());
				instance2.extraData = emanation.getValue();
				emanation.getKey().interrupt(instance2);
			}
			break;
		default:
			((SequentialInstance) instance.extraData).list.getFirst()
					.interrupt(((SequentialInstance) instance.extraData).instance);
		}
	}

	@Override
	public EmanationType<CombinedEmanation> getEmanationType() {
		return EmanationType.COMBINED.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof CombinedEmanation em) {
			return this.emanations.equals(em.emanations) && this.order.equals(em.order)
					&& this.conditional == em.conditional && this.properties.equals(em.properties);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.emanations.hashCode() + this.order.hashCode() + this.optionalSpellProperties().hashCode();
	}

	@Override
	public String toString() {
		return (conditional ? "Conditional_" : "Unconditional_") + this.order.name()
				+ (this.optionalSpellProperties().map((x) -> "_" + x.alignment()).orElse("")) + this.emanations;
	}

	@Override
	public Component translate() {
		return TextUtils.transPrefix("sotd.emanation.combined" + ("_" + this.order.name().toLowerCase()),
				this.emanations.stream().map((s) -> TextUtils.transPrefix("sotd.cmd.quote", s.translate()))
						.collect(StreamUtils.componentCollector("sotd.cmd.list" + (conditional ? ".conditional" : ""),
								null, null)));
	}

	@Override
	public String uniqueName() {
		return (conditional ? "conditional_" : "unconditional_") + this.order + "{"
				+ this.optionalSpellProperties().map((x) -> "properties=" + x + ",").orElse("") + "emanations="
				+ emanations + "}";
	}

	private static record SequentialInstance(List<IEmanation> list, EmanationInstance instance, float intensity)
			implements IEmanationInstanceData {

		public static final EmanationDataType<SequentialInstance> TYPE = new EmanationDataType<SequentialInstance>(
				ModUtils.path("sequential"),
				Codec.pair(
						Codec.pair(Codec.list(IEmanation.codec()).fieldOf("list").codec(),
								EmanationInstance.CODEC.fieldOf("emanation").codec()),
						Codec.FLOAT.fieldOf("intensity").codec())
						.xmap((p) -> new SequentialInstance(p.getFirst().getFirst(), p.getFirst().getSecond(),
								p.getSecond()), (s) -> Pair.of(Pair.of(s.list(), s.instance()), s.intensity)));

		@Override
		public EmanationDataType<SequentialInstance> dataType() {
			return TYPE;
		}

	}

	private static record SimultaneousInstance(List<Map.Entry<IEmanation, IEmanationInstanceData>> list,
			float intensity) implements IEmanationInstanceData {

		public static final EmanationDataType<SimultaneousInstance> TYPE = new EmanationDataType<>(
				ModUtils.path("simultaneous"), Codec
						.pair(Codec
								.list(Codec.pair(IEmanation.codec().fieldOf("emanation").codec(),
										EmanationDataType.DISPATCH_CODEC.optionalFieldOf("data").codec()))
								.fieldOf("list").codec(), Codec.FLOAT.fieldOf("intensity").codec())
						.xmap((lis) -> new SimultaneousInstance(
								Lists.transform(lis.getFirst(),
										(v) -> Map.entry(v.getFirst(), v.getSecond().orElse(null))),
								lis.getSecond()),
								(lis) -> Pair.of(
										Lists.transform(lis.list,
												(v) -> Pair.of(v.getKey(), Optional.ofNullable(v.getValue()))),
										lis.intensity)));

		public SimultaneousInstance(List<IEmanation> emanations, boolean f, float intensity) {
			this(new ArrayList<>(Lists.transform(emanations, (m) -> Map.entry(m, null))), intensity);
		}

		@Override
		public EmanationDataType<SimultaneousInstance> dataType() {
			return TYPE;
		}
	}

}
