package com.gm910.sotdivine.deities_and_parties.deity.emanation.types.spawn;

import java.util.Optional;

import com.gm910.sotdivine.deities_and_parties.deity.emanation.EmanationInstance;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.EmanationType;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.types.AbstractEmanation;
import com.gm910.sotdivine.util.StreamUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A spawning SPELL, to spawn an entity
 * 
 * @author borah
 *
 */
public class SpawnEmanation extends AbstractEmanation {

	public static final Codec<SpawnEmanation> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group(Codec.STRING.optionalFieldOf("name").forGetter((m) -> Optional.ofNullable(m.emanationName())), Codec
			.mapEither(WeightedList.codec(EntityType.CODEC).fieldOf("entities"), EntityType.CODEC.fieldOf("entity"))
			.forGetter((sp) -> {
				if (sp.getEntities().unwrap().size() == 1) {
					return Either.right(sp.getEntities().unwrap().get(0).value());
				}
				return Either.left(sp.getEntities());
			}), ISpellProperties.CODEC.fieldOf("spell_properties").forGetter((p) -> p.optionalSpellProperties().get()))
			.apply(instance, SpawnEmanation::new));

	private WeightedList<EntityType<?>> mobs;
	private String name;

	private SpawnEmanation(Optional<String> name, Either<WeightedList<EntityType<?>>, EntityType<?>> entity,
			ISpellProperties properties) {
		super(false, true, properties);
		if (entity.right().isPresent()) {
			this.mobs = WeightedList.<EntityType<?>>builder().add(entity.right().get()).build();
		} else {
			this.mobs = entity.left().get();
		}
		if (name.isEmpty()) {
			this.name = "spawn_" + mobs.unwrap().stream().sorted((w, w2) -> -Integer.compare(w.weight(), w2.weight()))
					.map(Weighted<EntityType<?>>::value).map(EntityType::getKey).map(ResourceLocation::getPath)
					.collect(StreamUtils.setStringCollector("_or_"));
		} else {
			this.name = name.get();
		}
	}

	public SpawnEmanation(String name, WeightedList<EntityType<?>> mobSettings, ISpellProperties properties) {
		this(Optional.ofNullable(name), Either.left(mobSettings), properties);
	}

	public SpawnEmanation(EntityType<?> entity, ISpellProperties properties) {
		this(Optional.empty(), Either.right(entity), properties);
	}

	public SpawnEmanation(WeightedList<EntityType<?>> mobSettings, ISpellProperties properties) {
		this(Optional.empty(), Either.left(mobSettings), properties);
	}

	@Override
	protected String emanationName() {
		return name;
	}

	public WeightedList<EntityType<?>> getEntities() {
		return mobs;
	}

	@Override
	public EmanationType<SpawnEmanation> getEmanationType() {
		return EmanationType.SPAWN.get();
	}

	@Override
	public boolean isDurative() {
		return false;
	}

	/**
	 * Called after the entity is spawned
	 * 
	 * @param genEntity
	 * @param targetPos
	 */
	protected void entityPostprocessing(Entity genEntity, EmanationInstance info) {

	}

	@Override
	public int hashCode() {
		return this.name.hashCode() + this.mobs.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof SpawnEmanation em) {
			return this.name.equals(em.name) && this.mobs.equals(em.mobs) && super.equals(obj);
		}
		return false;
	}

	@Override
	public String toString() {
		return "SpawnEmanation(\"" + this.name + "\")" + this.mobs.unwrap().toString();
	}

	@Override
	public boolean trigger(EmanationInstance info, float intensity) {
		if (info.targetInfo().opTargetPos().isEmpty())
			return true;
		GlobalPos gpos = info.targetInfo().opTargetPos().get();
		Optional<EntityType<?>> randomMobOp = mobs.getRandom(info.targetInfo().level().random);
		if (randomMobOp.isEmpty())
			return true;
		EntityType<?> etype = randomMobOp.get();

		ServerLevel level = getCorrectLevel(info.targetInfo()).get();
		for (int i = 0; i < intensity; i++) {
			Entity entity = etype.create(level, EntitySpawnReason.MOB_SUMMONED);
			entity.snapTo(gpos.pos(), 0.0F, 0.0F);
			if (entity instanceof Mob) {
				((Mob) entity).finalizeSpawn(level, info.targetInfo().level().getCurrentDifficultyAt(gpos.pos()),
						EntitySpawnReason.MOB_SUMMONED, null);
			}
			info.targetInfo().level().addFreshEntityWithPassengers(entity);
			entityPostprocessing(entity, info);
			info.targetInfo().level().gameEvent(GameEvent.ENTITY_PLACE, gpos.pos(), GameEvent.Context
					.of(info.targetInfo().opCaster().map((u) -> info.targetInfo().level().getEntity(u)).orElse(null)));
		}
		return false;
	}

}
