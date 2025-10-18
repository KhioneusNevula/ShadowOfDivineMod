package com.gm910.sotdivine.systems.deity.ritual;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gm910.sotdivine.systems.deity.IDeity;
import com.gm910.sotdivine.systems.deity.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.systems.deity.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.systems.deity.ritual.pattern.RitualPatternSet;
import com.gm910.sotdivine.systems.deity.ritual.properties.RitualQuality;
import com.gm910.sotdivine.systems.deity.ritual.properties.RitualType;
import com.gm910.sotdivine.systems.deity.ritual.trigger.IRitualTrigger;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public non-sealed class Ritual implements IRitual {

	private RitualPatternSet patterns;
	private Map<String, IPlaceableGenreProvider<?, ?>> symbols;
	private Map<IGiveableGenreProvider<?, ?>, Integer> offerings;
	private Map<RitualEffectType, RitualEmanationTargeter> effects;
	private RitualType type;
	private RitualQuality quality;
	private IRitualTrigger trigger;

	private static Codec<IRitual> CODEC = null;

	public static Codec<IRitual> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder.create(instance -> instance.group(
					RitualType.CODEC.fieldOf("type").forGetter(IRitual::ritualType),
					RitualQuality.CODEC.fieldOf("quality").forGetter(IRitual::ritualQuality),
					RitualPatternSet.codec().fieldOf("patterns").forGetter(IRitual::patterns),
					Codec.unboundedMap(RitualEffectType.CODEC, RitualEmanationTargeter.codec())
							.optionalFieldOf("effects", Map.of()).forGetter((x) -> ((Ritual) x).effects),
					Codec.unboundedMap(Codec.STRING,
							IGenreProvider.<IPlaceableGenreProvider<?, ?>>castCodec(IPlaceableGenreProvider.class))
							.fieldOf("symbols").forGetter(IRitual::symbols),
					Codec.compoundList(
							IGenreProvider.<IGiveableGenreProvider<?, ?>>castCodec(IGiveableGenreProvider.class),
							Codec.INT).fieldOf("offerings").forGetter((x) -> {
								return Lists.newArrayList(x.offerings().entrySet().stream()
										.<Pair<IGiveableGenreProvider<?, ?>, Integer>>map(
												(e) -> Pair.of(e.getKey(), e.getValue()))
										.iterator());
							}),
					IRitualTrigger.codec().fieldOf("trigger").forGetter(IRitual::trigger))
					.apply(instance, (t, q, p, e, eep, o, tr) -> new Ritual(t, q, p, e, eep,
							o.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)), tr)));
		}
		return CODEC;
	}

	public Ritual(RitualType type, RitualQuality quality, RitualPatternSet patterns,
			Map<RitualEffectType, RitualEmanationTargeter> effects, Map<String, IPlaceableGenreProvider<?, ?>> symbols,
			Map<IGiveableGenreProvider<?, ?>, Integer> offerings, IRitualTrigger trigger) {
		this.type = type;
		this.quality = quality;
		this.patterns = patterns;
		this.effects = new HashMap<>(effects);
		this.symbols = new HashMap<>(symbols);
		this.offerings = new HashMap<>(offerings);
		this.trigger = trigger;
	}

	@Override
	public RitualPatternSet patterns() {
		return patterns;
	}

	@Override
	public Ritual coalesce(IDeity deity) {
		if (this.effects.isEmpty()) {
			return this;
		}
		for (Entry<RitualEffectType, RitualEmanationTargeter> entry : this.effects.entrySet()) {
			deity.spheres().stream().flatMap((f) -> f.allEmanations().stream())
					.filter((x) -> x.equals(entry.getValue().emanation())).findAny()
					.ifPresent((em) -> entry.setValue(new RitualEmanationTargeter(em,
							entry.getValue().targetPositions(), entry.getValue().targetEntities())));

		}

		return this;
	}

	@Override
	public Map<String, IPlaceableGenreProvider<?, ?>> symbols() {
		return Collections.unmodifiableMap(symbols);
	}

	@Override
	public Map<IGiveableGenreProvider<?, ?>, Integer> offerings() {
		return Collections.unmodifiableMap(offerings);
	}

	@Override
	public Optional<RitualEmanationTargeter> ritualEffect(RitualEffectType effect) {
		return Optional.ofNullable(this.effects.get(effect));
	}

	@Override
	public RitualType ritualType() {
		return type;
	}

	@Override
	public RitualQuality ritualQuality() {
		return quality;
	}

	@Override
	public IRitualTrigger trigger() {
		return trigger;
	}

	@Override
	public int hashCode() {
		return Objects.hash(patterns, type, quality, effects, offerings, symbols, trigger);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof Ritual ritual) {
			return this.type == ritual.type && this.quality == ritual.quality && this.patterns.equals(ritual.patterns)
					&& this.effects.equals(ritual.effects) && this.offerings.equals(ritual.offerings)
					&& this.symbols.equals(ritual.symbols) && this.trigger.equals(ritual.trigger);
		}
		return false;
	}

	@Override
	public String toString() {
		return "Ritual(" + this.type + "," + this.quality + (!effects.isEmpty() ? ",effects=" + effects : "")
				+ "){trigger=" + trigger + ",patterns=" + this.patterns.toString() + "}";
	}

}
