package com.gm910.sotdivine.magic.ritual;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gm910.sotdivine.concepts.deity.Deity;
import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.magic.emanation.DeityInteractionType;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.magic.ritual.emanate.RitualElement;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatternSet;
import com.gm910.sotdivine.magic.ritual.properties.IRitualParameters;
import com.gm910.sotdivine.magic.ritual.properties.RitualQuality;
import com.gm910.sotdivine.magic.ritual.properties.RitualType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;

public non-sealed class Ritual implements IRitual {

	private RitualPatternSet patterns;
	private Map<String, IPlaceableGenreProvider<?, ?>> symbols;
	private Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> offerings;
	private Map<RitualEffectType, RitualEmanationTargeter> effects;
	private RitualType type;
	private RitualQuality quality;
	private IRitualTrigger trigger;
	private Set<IEmanation> emanations;

	private static Codec<IRitual> CODEC = null;

	public static Codec<IRitual> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder
					.create(instance -> instance.group(RitualType.CODEC.fieldOf("type").forGetter(IRitual::ritualType),
							RitualQuality.CODEC.fieldOf("quality").forGetter(IRitual::ritualQuality),
							RitualPatternSet.codec().fieldOf("patterns").forGetter(IRitual::patterns),
							Codec.unboundedMap(RitualEffectType.CODEC, RitualEmanationTargeter.codec())
									.optionalFieldOf("effects", Map.of()).forGetter((x) -> ((Ritual) x).effects),
							Codec.unboundedMap(Codec.STRING, IGenreProvider
									.<IPlaceableGenreProvider<?, ?>>castCodec(IPlaceableGenreProvider.class))
									.fieldOf("symbols").forGetter(IRitual::symbols),
							CodecUtils
									.listOrSingleCodec(CodecUtils.singleOrCompoundCodec("offering",
											Codec.list(IGenreProvider.<IGiveableGenreProvider<?, ?>>castCodec(
													IGiveableGenreProvider.class))
													.<Set<? extends IGiveableGenreProvider<?, ?>>>xmap(Sets::newHashSet,
															Lists::newArrayList),
											"count", Codec.INT, 1))
									.fieldOf("offerings").forGetter((x) -> {
										return Lists.newArrayList(x.offerings().entrySet().stream()
												.<Pair<Set<? extends IGiveableGenreProvider<?, ?>>, Integer>>map(
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
			Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> offerings, IRitualTrigger trigger) {
		this.type = type;
		this.quality = quality;
		this.patterns = patterns;
		this.effects = new HashMap<>(effects);
		this.symbols = new HashMap<>(symbols);
		this.offerings = new HashMap<>(offerings);
		this.trigger = trigger;
		this.emanations = Sets.newHashSet(this.effects.values().stream().map((s) -> s.emanation()).iterator());
	}

	@Override
	public RitualPatternSet patterns() {
		return patterns;
	}

	@Override
	public void initiateRitual(ServerLevel level, IDeity deity, UUID caster, GlobalPos atPos, IRitualPattern pattern,
			IRitualParameters parameters, IRitualTriggerEvent event, Collection<BlockPos> banners,
			Collection<? extends Entity> shields, Map<ItemEntity, Integer> offerings) {

		if (level.random.nextFloat() > 0.2f) {
			var list = Lists.newArrayList(deity.spheres().stream()
					.flatMap((s) -> s.emanationsOfType(DeityInteractionType.SYMBOL_RECOGNITION).stream()).iterator());
			Collections.shuffle(list);
			if (!list.isEmpty()) {
				new RitualEmanationTargeter(list.getFirst(), Set.of(RitualElement.RECOGNIZED_SYMBOL)).runEmanations(
						null, level, deity, atPos, pattern, caster, 1f, banners, shields, offerings.keySet());
			}
		}

		float intensity = parameters.aggregateIntensity();
		RitualInstance instance = new RitualInstance(this, new EntityReference<Entity>(caster), atPos, pattern,
				intensity, banners, shields.stream().<EntityReference<Entity>>map(EntityReference::new).toList(),
				offerings.keySet().stream().map(EntityReference::new).toList());

		if (this.effects.get(RitualEffectType.SIGNIFIER) instanceof RitualEmanationTargeter signifier) {
			signifier.runEmanations(null, level, deity, atPos, pattern, caster, intensity, banners, shields,
					offerings.keySet());
		}
		var player = new EntityReference<ServerPlayer>(caster).getEntity(level, ServerPlayer.class);

		if (player != null) {
			player.sendSystemMessage(TextUtils.transPrefix("sotd.deity.accept_offering",
					deity.descriptiveName().orElse(Component.literal(deity.uniqueName())),
					offerings.entrySet().stream()
							.map((s) -> TextUtils.transPrefix("sotd.cmd.count", s.getValue(),
									s.getKey().getItem().getDisplayName()))
							.collect(CollectionUtils.componentCollectorCommasPretty())));
		}
		boolean success = false;
		switch (this.type) {
		case SPELL: {
			if (this.effects.get(RitualEffectType.SUCCESS) instanceof RitualEmanationTargeter succ) {
				player.sendSystemMessage(TextUtils.transPrefix("sotd.deity.run.spell",
						deity.descriptiveName().orElse(Component.literal(deity.uniqueName())),
						succ.emanation().translate()));

				success = succ.runEmanations(instance, level, deity, atPos, pattern, caster, intensity, banners,
						shields, offerings.keySet());
			} else {

				LogUtils.getLogger().debug("No success effect");
			}
			break;
		}

		case VENERATION: {
			// TODO deity veneration
			LogUtils.getLogger().debug("Deity " + deity + " accepted venerations: " + offerings);
			success = true;
			break;
		}

		default: {

		}
		}
		if (success) {

			LogUtils.getLogger().debug("Succeeded at starting ritual");
		} else {
			if (this.effects.get(RitualEffectType.FAIL_START) instanceof RitualEmanationTargeter fail) {
				fail.runEmanations(instance, level, deity, atPos, pattern, caster, intensity, banners, shields,
						offerings.keySet());
			}
			LogUtils.getLogger().debug("Failed to start ritual");
		}

		LogUtils.getLogger().debug("Deleting/decreasing offerings for ritual");
		for (Entry<ItemEntity, Integer> entry : offerings.entrySet()) {
			entry.getKey().getItem().setCount(entry.getKey().getItem().getCount() - entry.getValue());
			if (entry.getKey().getItem().isEmpty()) {
				entry.getKey().remove(RemovalReason.KILLED);
			}
		}
	}

	@Override
	public void signalStop(RitualInstance instance, ServerLevel level, Deity deity, GlobalPos atPos,
			IRitualPattern pattern, float intensity, boolean interrupt) {
		if (interrupt) {
			if (this.effects.get(RitualEffectType.INTERRUPTION) instanceof RitualEmanationTargeter succ) {
				succ.runEmanations(instance, level, deity, atPos, pattern, instance.caster().getUUID(), intensity,
						instance.banners(),
						instance.shields().stream().map((s) -> s.getEntity(level, Entity.class)).toList(),
						instance.offerings().stream().map((s) -> s.getEntity(level, ItemEntity.class)).toList());
			}
		} else {
			if (this.effects.get(RitualEffectType.FAIL_TICK) instanceof RitualEmanationTargeter fail) {
				fail.runEmanations(instance, level, deity, atPos, pattern, instance.caster().getUUID(), intensity,
						instance.banners(),
						instance.shields().stream().map((s) -> s.getEntity(level, Entity.class)).toList(),
						instance.offerings().stream().map((s) -> s.getEntity(level, ItemEntity.class)).toList());
			}
		}
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
							entry.getValue().positionTargeter(), entry.getValue().entityTargeter(),
							entry.getValue().alsoTargetEntityPositions(), entry.getValue().elementTargeter())));

		}

		return this;
	}

	@Override
	public Map<String, IPlaceableGenreProvider<?, ?>> symbols() {
		return Collections.unmodifiableMap(symbols);
	}

	@Override
	public Map<Set<? extends IGiveableGenreProvider<?, ?>>, Integer> offerings() {
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
	public Collection<IEmanation> emanations() {
		return Collections.unmodifiableCollection(this.emanations);
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
				+ "){trigger=" + trigger + ",offerings=" + this.offerings + ",patterns=" + this.patterns.toString()
				+ "}";
	}

}
