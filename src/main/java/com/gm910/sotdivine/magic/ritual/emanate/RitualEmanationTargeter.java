package com.gm910.sotdivine.magic.ritual.emanate;

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
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IEntityGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.magic.emanation.DeityInteractionType;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.magic.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.magic.emanation.spell.SpellPower;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.RitualInstance;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.ritual.properties.RitualQuality;
import com.gm910.sotdivine.magic.ritual.properties.RitualType;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * A ritual emanation targeter which can optionally target the rawPosition(s) of
 * instances of the given (placeable) genre and optionally target instances of a
 * given (entity) genre in the ritual
 * 
 * @param alsoTargetEntityPositions whether this ritual should target the
 *                                  positions of entities as well as the
 *                                  entities themselves, if given
 */
public record RitualEmanationTargeter(IEmanation emanation, Optional<IPlaceableGenreProvider<?, ?>> positionTargeter,
		Optional<IEntityGenreProvider<?, ?>> entityTargeter, boolean alsoTargetEntityPositions,
		Set<RitualElement> elementTargeter) {

	private static Codec<RitualEmanationTargeter> CODEC;

	public static Codec<RitualEmanationTargeter> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder.create(instance -> instance
					.group(IEmanation.codec().fieldOf("emanation").forGetter(RitualEmanationTargeter::emanation),
							IGenreProvider.<IPlaceableGenreProvider<?, ?>>castCodec(IPlaceableGenreProvider.class)
									.optionalFieldOf("position_targeter")
									.<RitualEmanationTargeter>forGetter((r) -> r.positionTargeter),
							IGenreProvider.<IEntityGenreProvider<?, ?>>castCodec(IEntityGenreProvider.class)
									.optionalFieldOf("entity_targeter")
									.<RitualEmanationTargeter>forGetter((r) -> r.entityTargeter),
							Codec.BOOL.optionalFieldOf("also_target_entity_positions", true)
									.forGetter(RitualEmanationTargeter::alsoTargetEntityPositions),
							Codec.list(RitualElement.CODEC).optionalFieldOf("element_targets", List.of())
									.forGetter((x) -> new ArrayList<>(x.elementTargeter)))
					.apply(instance,
							(i, tp, te, at, e) -> new RitualEmanationTargeter(i, tp, te, at, new HashSet<>(e))));
		}
		return CODEC;
	}

	public RitualEmanationTargeter(IEmanation instance) {
		this(instance, Optional.empty(), Optional.empty(), false, Set.of());
	}

	public RitualEmanationTargeter(IEmanation instance, Collection<RitualElement> elements) {
		this(instance, Optional.empty(), Optional.empty(), true, Set.copyOf(elements));
	}

	public RitualEmanationTargeter(IEmanation instance, IPlaceableGenreProvider<?, ?> posTargeter) {
		this(instance, Optional.of(posTargeter), Optional.empty(), false, Set.of());
	}

	public RitualEmanationTargeter(IEmanation instance, IEntityGenreProvider<?, ?> enTargeter, boolean enAndPos) {
		this(instance, Optional.empty(), Optional.of(enTargeter), enAndPos, Set.of());
	}

	public RitualEmanationTargeter(RitualEmanationTargeter t) {
		this(t.emanation, t.positionTargeter, t.entityTargeter, t.alsoTargetEntityPositions, t.elementTargeter);
	}

	/**
	 * Matches this emanation to every entity or rawPosition relevant and run it.
	 * Return false if all emanations failed
	 * 
	 * @param level
	 * @param deity
	 * @param atPos
	 * @param pattern
	 * @return
	 */
	public boolean runEmanations(@Nullable RitualInstance ritual, ServerLevel level, IDeity deity, GlobalPos atPos,
			IRitualPattern pattern, @Nullable UUID caster, float intensity, Collection<ItemEntity> offerings) {
		LogUtils.getLogger().debug("running " + this);

		boolean[] out = { false };

		if (this.elementTargeter.contains(RitualElement.CENTER)) {
			out[0] = deity.triggerEmanation(this.emanation,
					ISpellTargetInfo.builder(deity, level).targetPos(atPos.pos()).build(), intensity, ritual) != null
					|| out[0];

		}
		if (this.elementTargeter.contains(RitualElement.AREA_RANDOM)) {
			pattern.getAllBlockPositions().map((v) -> atPos.pos().offset(v))
					.filter((x) -> level.random.nextFloat() <= 10f / pattern.blockCount()).forEach((ex) -> {
						out[0] = deity.triggerEmanation(this.emanation,
								ISpellTargetInfo.builder(deity, level).targetPos(ex).build(), intensity, ritual) != null
								|| out[0];
					});
		}
		if (this.elementTargeter.contains(RitualElement.OFFERING)) {
			offerings.stream().forEach((ex) -> {
				out[0] = deity.triggerEmanation(this.emanation,
						ISpellTargetInfo.builder(deity, level).branch(this.alsoTargetEntityPositions,
								(b) -> b.targetEntityAndPos(ex), (b) -> b.targetEntity(ex)).build(),
						intensity, ritual) != null || out[0];
			});
		}
		if (this.elementTargeter.contains(RitualElement.TARGET)) {
			// if (!targetSelector.isEmpty())
			// TODO allow targeting things outside ritual
			Entity castingEntity = new EntityReference<Entity>(caster).getEntity(level, Entity.class);

			Stream<Entity> allEntities = pattern.getEntitiesInPattern(level, atPos.pos(),
					EntityTypeTest.forClass(Entity.class), (en) -> true);
			if (castingEntity != null) {
				var parties = Sets.newHashSet(IPartySystem.get(level).getPartiesOf(castingEntity).iterator());
				allEntities = Streams.concat(Stream.of(castingEntity),
						allEntities.filter((x) -> IPartySystem.get(level).getPartiesOf(x).anyMatch(parties::contains)));
			}
			allEntities
					.forEach(
							(enti) -> out[0] = deity
									.triggerEmanation(this.emanation, ISpellTargetInfo.builder(deity, level)
											.branch(this.alsoTargetEntityPositions, (b) -> b.targetEntityAndPos(enti),
													(b) -> b.targetEntity(enti))
											.build(), intensity, ritual) != null
									|| out[0]);

		}

		this.entityTargeter.ifPresent((te) -> {
			pattern.getEntitiesInPattern(level, atPos.pos(), EntityTypeTest.forClass(Entity.class),
					(e) -> te.matchesEntity(level, e)).forEach((ex) -> {
						var info1 = ISpellTargetInfo.builder(deity, level);
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
		this.positionTargeter.ifPresent((te) -> {
			pattern.getAllBlockPositions().filter((p) -> te.matchesPos(level, new BlockPos(p))).forEach((ex) -> {
				var info1 = ISpellTargetInfo.builder(deity, level);
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
	 * @param $quality
	 * @param level
	 * @param ford
	 * @return
	 */
	public static Map<RitualEffectType, RitualEmanationTargeter> createRitualEmanations(RitualType type,
			RitualQuality quality, ServerLevel level, IDeity ford, Collection<IRitual> existingRituals) {
		Map<RitualEffectType, RitualEmanationTargeter> map = new HashMap<>();
		/*EntityGenreProvider worshiperPred = new EntityGenreProvider(
				EntityTypePredicate.of(level.holderLookup(Registries.ENTITY_TYPE), ModEntityTags.WORSHIPER),
				Set.of(new IsWorshiper(ford.uniqueName())));*/

		switch (type) {
		case SPELL: {
			var list = Lists.newArrayList(
					ford.spheres().stream().flatMap((s) -> s.emanationsOfType(DeityInteractionType.SPELL).stream())
							.filter((em) -> em.optionalSpellProperties().map(ISpellProperties::difficulty)
									.orElse(SpellPower.IMPOSSIBLE) == quality.spellPower)
							.filter((poss) -> {
								return existingRituals.stream().noneMatch((r) -> {
									if (r.ritualEffect(RitualEffectType.SUCCESS)
											.orElse(null) instanceof RitualEmanationTargeter ret) {
										return poss.equals(ret.emanation);
									}
									return false;
								});
							}).iterator());
			Collections.shuffle(list);
			if (list.isEmpty()) {
				LogUtils.getLogger().debug("No spell effects available for " + type + ", " + quality + " (spheres="
						+ ford.spheres() + "), deity already has rituals: " + ford.getRituals());
				return null;
			}

			IEmanation ema = list.getFirst();
			map.put(RitualEffectType.SUCCESS, new RitualEmanationTargeter(ema, Set.of(RitualElement.TARGET)));
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
				map.put(RitualEffectType.FAIL_START,
						new RitualEmanationTargeter(list.getFirst(), Set.of(RitualElement.TARGET)));
			}
		}
		if (level.random.nextFloat() > 0.2f) {
			var list = Lists.newArrayList(ford.spheres().stream()
					.flatMap((s) -> s.emanationsOfType(DeityInteractionType.FAILED_CAST).stream()).iterator());
			Collections.shuffle(list);
			if (!list.isEmpty()) {
				map.put(RitualEffectType.FAIL_TICK,
						new RitualEmanationTargeter(list.getFirst(), Set.of(RitualElement.AREA_RANDOM)));
			}
		}
		if (level.random.nextFloat() > 0.2f) {
			var list = Lists.newArrayList(ford.spheres().stream()
					.flatMap((s) -> s.emanationsOfType(DeityInteractionType.FAILED_CAST).stream()).iterator());
			Collections.shuffle(list);
			if (!list.isEmpty()) {
				map.put(RitualEffectType.INTERRUPTION,
						new RitualEmanationTargeter(list.getFirst(), Set.of(RitualElement.AREA_RANDOM)));
			}
		}
		return map;
	}

	@Override
	public final String toString() {
		return "{" + emanation + (positionTargeter.isEmpty() ? "" : ",positions=" + positionTargeter.get())
				+ (entityTargeter.isEmpty() ? ""
						: ",entities" + (alsoTargetEntityPositions ? "(&pos)" : "") + "=" + entityTargeter.get())
				+ (elementTargeter.isEmpty() ? "" : ",elements=" + elementTargeter) + "}";
	}

	public Component translate() {
		return TextUtils.transPrefix(
				"sotd.cmd.ritual.targeter" + (positionTargeter.isPresent() ? "_targ" : "")
						+ (entityTargeter.isPresent() ? "_en" + (alsoTargetEntityPositions ? "pos" : "") : "")
						+ (elementTargeter.isEmpty() ? "" : "_el"),
				TextUtils.transPrefix("sotd.cmd.quote", emanation.translate()),
				positionTargeter.map((s) -> s.translate()).orElse(Component.empty()),
				entityTargeter.map((s) -> s.translate()).orElse(Component.empty()),
				elementTargeter.stream()
						.map((s) -> TextUtils.transPrefix("sotd.cmd.ritual.element." + s.name().toLowerCase()))
						.collect(CollectionUtils.componentCollectorCommasPretty()));
	}

}
