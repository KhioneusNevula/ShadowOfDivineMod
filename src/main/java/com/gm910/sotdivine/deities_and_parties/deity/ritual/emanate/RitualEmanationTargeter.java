package com.gm910.sotdivine.deities_and_parties.deity.ritual.emanate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.gm910.sotdivine.deities_and_parties.deity.IDeity;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.DeityInteractionType;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.IEmanation;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.spell.SpellPower;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.RitualInstance;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.properties.RitualQuality;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.properties.RitualType;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.entity_preds.IsWorshiper;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.EntityGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.IEntityGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.entities.ModEntityTags;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * A ritual emanation targeter which can optionally target the position(s) of
 * instances of the given (placeable) genre and optionally target instances of a
 * given (entity) genre in the ritual
 * 
 * @param alsoTargetEntityPositions whether this ritual should target the
 *                                  positions of entities as well as the
 *                                  entities themselves, if given
 */
public record RitualEmanationTargeter(IEmanation emanation, Optional<IPlaceableGenreProvider<?, ?>> targetPositions,
		Optional<IEntityGenreProvider<?, ?>> targetEntities, boolean alsoTargetEntityPositions,
		Set<RitualElement> elementTargets) {

	private static Codec<RitualEmanationTargeter> CODEC;

	public static Codec<RitualEmanationTargeter> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder.create(instance -> instance
					.group(IEmanation.codec().fieldOf("emanation").forGetter(RitualEmanationTargeter::emanation),
							IGenreProvider.<IPlaceableGenreProvider<?, ?>>castCodec(IPlaceableGenreProvider.class)
									.optionalFieldOf("position_targeter")
									.<RitualEmanationTargeter>forGetter((r) -> r.targetPositions),
							IGenreProvider.<IEntityGenreProvider<?, ?>>castCodec(IEntityGenreProvider.class)
									.optionalFieldOf("entity_targeter")
									.<RitualEmanationTargeter>forGetter((r) -> r.targetEntities),
							Codec.BOOL.optionalFieldOf("also_target_entity_positions", true)
									.forGetter(RitualEmanationTargeter::alsoTargetEntityPositions),
							Codec.list(RitualElement.CODEC).optionalFieldOf("element_targets", List.of())
									.forGetter((x) -> new ArrayList<>(x.elementTargets)))
					.apply(instance,
							(i, tp, te, at, e) -> new RitualEmanationTargeter(i, tp, te, at, new HashSet<>(e))));
		}
		return CODEC;
	}

	public RitualEmanationTargeter(IEmanation instance) {
		this(instance, Optional.empty(), Optional.empty(), false, Set.of());
	}

	public RitualEmanationTargeter(IEmanation instance, Collection<RitualElement> elements) {
		this(instance, Optional.empty(), Optional.empty(), false, Set.copyOf(elements));
	}

	public RitualEmanationTargeter(IEmanation instance, IPlaceableGenreProvider<?, ?> posTargeter) {
		this(instance, Optional.of(posTargeter), Optional.empty(), false, Set.of());
	}

	public RitualEmanationTargeter(IEmanation instance, IEntityGenreProvider<?, ?> enTargeter, boolean enAndPos) {
		this(instance, Optional.empty(), Optional.of(enTargeter), enAndPos, Set.of());
	}

	public RitualEmanationTargeter(RitualEmanationTargeter t) {
		this(t.emanation, t.targetPositions, t.targetEntities, t.alsoTargetEntityPositions, t.elementTargets);
	}

	/**
	 * Matches this emanation to every entity or position relevant and run it.
	 * Return false if all emanations failed
	 * 
	 * @param level
	 * @param deity
	 * @param atPos
	 * @param pattern
	 * @return
	 */
	public boolean runEmanations(@Nullable RitualInstance ritual, ServerLevel level, IDeity deity, GlobalPos atPos,
			IRitualPattern pattern, @Nullable UUID caster, float intensity, Collection<BlockPos> banners,
			Collection<Entity> shields, Collection<Entity> offerings) {

		boolean[] out = { false };

		if (this.elementTargets.contains(RitualElement.RECOGNIZED_SYMBOL)) {
			banners.stream().forEach((ex) -> {
				out[0] = deity.triggerEmanation(this.emanation, ISpellTargetInfo.builder().targetPos(ex).build(),
						intensity, ritual) != null || out[0];
			});
			shields.stream().forEach((ex) -> {
				var info1 = ISpellTargetInfo.builder();
				if (this.alsoTargetEntityPositions)
					info1.targetEntityAndPos(ex);
				else
					info1.targetEntity(ex);
				out[0] = deity.triggerEmanation(this.emanation, info1.build(), intensity, ritual) != null || out[0];
			});
		}
		if (this.elementTargets.contains(RitualElement.OFFERING)) {
			offerings.stream().forEach((ex) -> {
				var info1 = ISpellTargetInfo.builder();
				if (this.alsoTargetEntityPositions)
					info1.targetEntityAndPos(ex);
				else
					info1.targetEntity(ex);
				out[0] = deity.triggerEmanation(this.emanation, info1.build(), intensity, ritual) != null || out[0];
			});
		}

		this.targetEntities.ifPresent((te) -> {
			pattern.getEntitiesInPattern(level, atPos.pos(), (e) -> te.matchesEntity(level, e)).stream()
					.forEach((ex) -> {
						var info1 = ISpellTargetInfo.builder();
						if (caster != null)
							info1.caster(caster);
						if (this.alsoTargetEntityPositions)
							info1.targetEntityAndPos(ex);
						else
							info1.targetEntity(ex);
						out[0] = deity.triggerEmanation(this.emanation, info1.build(), intensity, ritual) != null
								|| out[0];
					});
		});
		this.targetPositions.ifPresent((te) -> {
			pattern.getAllBlockPositions().filter((p) -> te.matchesPos(level, new BlockPos(p))).forEach((ex) -> {
				var info1 = ISpellTargetInfo.builder();
				if (caster != null)
					info1.caster(caster);
				info1.targetPos(new GlobalPos(atPos.dimension(), new BlockPos(ex)));
				out[0] = deity.triggerEmanation(this.emanation, info1.build(), intensity, ritual) != null || out[0];
			});
		});

		return out[0];
	}

	/**
	 * Return a map of ritual effects
	 * 
	 * @param type
	 * @param quality
	 * @param level
	 * @param ford
	 * @return
	 */
	public static Map<RitualEffectType, RitualEmanationTargeter> createRitualEmanations(RitualType type,
			RitualQuality quality, ServerLevel level, IDeity ford) {
		Map<RitualEffectType, RitualEmanationTargeter> map = new HashMap<>();
		EntityGenreProvider worshiperPred = new EntityGenreProvider(
				EntityTypePredicate.of(level.holderLookup(Registries.ENTITY_TYPE), ModEntityTags.WORSHIPER),
				Set.of(new IsWorshiper(ford.uniqueName())));

		switch (type) {
		case SPELL: {
			var list = Lists.newArrayList(
					ford.spheres().stream().flatMap((s) -> s.emanationsOfType(DeityInteractionType.SPELL).stream())
							.filter((x) -> ford.getRituals().stream().noneMatch((r) -> r.emanations().contains(x)))
							.filter((em) -> em.optionalSpellProperties().map((s) -> !s.alignment().isCurse()).isEmpty())
							.filter((em) -> em.optionalSpellProperties().map(ISpellProperties::difficulty)
									.orElse(SpellPower.IMPOSSIBLE) == quality.spellPower)
							.iterator());
			Collections.shuffle(list);
			if (list.isEmpty()) {
				return null;
			}

			IEmanation ema = list.getFirst();
			map.put(RitualEffectType.SUCCESS, new RitualEmanationTargeter(ema, worshiperPred));
			break;
		}
		default:
			break;
		}

		if (level.random.nextFloat() > 0.2f) {
			var list = Lists.newArrayList(ford.spheres().stream()
					.flatMap((s) -> s.emanationsOfType(DeityInteractionType.ACCEPT_OFFERING).stream()).iterator());
			Collections.shuffle(list);
			if (!list.isEmpty()) {
				map.put(RitualEffectType.SIGNIFIER,
						new RitualEmanationTargeter(list.getFirst(), Set.of(RitualElement.OFFERING)));
			}
		}
		if (level.random.nextFloat() > 0.2f) {
			// curse
			var list = Lists.newArrayList(
					ford.spheres().stream().flatMap((s) -> s.emanationsOfType(DeityInteractionType.SPELL).stream())
							.filter((em) -> em.optionalSpellProperties().map((s) -> s.alignment().isCurse()).isEmpty())
							.iterator());
			Collections.shuffle(list);
			if (!list.isEmpty()) {
				map.put(RitualEffectType.FAIL_START, new RitualEmanationTargeter(list.getFirst(), worshiperPred));
			}
		}
		if (level.random.nextFloat() > 0.2f) {
			var list = Lists.newArrayList(ford.spheres().stream()
					.flatMap((s) -> s.emanationsOfType(DeityInteractionType.FAIL_SPELL).stream()).iterator());
			Collections.shuffle(list);
			if (!list.isEmpty()) {
				map.put(RitualEffectType.FAIL_TICK,
						new RitualEmanationTargeter(list.getFirst(), Set.of(RitualElement.RECOGNIZED_SYMBOL)));
			}
		}
		if (level.random.nextFloat() > 0.2f) {
			var list = Lists.newArrayList(ford.spheres().stream()
					.flatMap((s) -> s.emanationsOfType(DeityInteractionType.FAIL_SPELL).stream()).iterator());
			Collections.shuffle(list);
			if (!list.isEmpty()) {
				map.put(RitualEffectType.INTERRUPTION,
						new RitualEmanationTargeter(list.getFirst(), Set.of(RitualElement.RECOGNIZED_SYMBOL)));
			}
		}
		return map;
	}

}
