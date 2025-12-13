package com.gm910.sotdivine.magic.emanation.types.spawn;

import java.util.Optional;

import com.gm910.sotdivine.magic.emanation.EmanationInstance;
import com.gm910.sotdivine.magic.emanation.EmanationType;
import com.gm910.sotdivine.magic.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.magic.emanation.types.AbstractEmanation;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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
			}),
			ISpellProperties.CODEC.optionalFieldOf("spell_properties").forGetter((p) -> p.optionalSpellProperties()))
			.apply(instance, SpawnEmanation::new));

	private WeightedList<EntityType<?>> mobs;
	private String name;
	private boolean dname; // if the name is default

	private SpawnEmanation(Optional<String> name, Either<WeightedList<EntityType<?>>, EntityType<?>> entity,
			Optional<ISpellProperties> properties) {
		super(false, true, properties);
		if (entity.right().isPresent()) {
			this.mobs = WeightedList.<EntityType<?>>builder().add(entity.right().get()).build();
		} else {
			this.mobs = entity.left().get();
		}
		if (name.isEmpty()) {
			this.name = "Spawn(" + mobs.unwrap().stream().sorted((w, w2) -> -Integer.compare(w.weight(), w2.weight()))
					.map((w) -> EntityType.getKey(w.value()) + "=" + w.weight())
					.collect(CollectionUtils.setStringCollector("|")) + ")";
			dname = true;
		} else {
			this.name = name.get();
		}
	}

	public SpawnEmanation(String name, WeightedList<EntityType<?>> mobSettings, ISpellProperties properties) {
		this(Optional.ofNullable(name), Either.left(mobSettings), Optional.ofNullable(properties));
	}

	public SpawnEmanation(EntityType<?> entity, ISpellProperties properties) {
		this(Optional.empty(), Either.right(entity), Optional.ofNullable(properties));
	}

	public SpawnEmanation(WeightedList<EntityType<?>> mobSettings, ISpellProperties properties) {
		this(Optional.empty(), Either.left(mobSettings), Optional.ofNullable(properties));
	}

	@Override
	protected String emanationName() {
		return name;
	}

	@Override
	public Component translate() {
		if (dname) {
			return TextUtils.transPrefix("sotd.emanation.spawn", mobs.unwrap().stream().map(Weighted::value)
					.map((et) -> et.getDescription()).collect(CollectionUtils.componentCollectorCommasPretty()));
		}
		return Component.translatableEscape("sotd.emanation.spawn." + name);
	}

	@Override
	public boolean createsObject() {
		return true;
	}

	@Override
	public boolean damagesTarget() {
		return false;
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
	public boolean trigger(EmanationInstance info, float intensity) {
		if (info.targetInfo().opTargetPos().isEmpty()) {
			System.out.println("No target pos available to spawn mobID at; cannot run " + this);
			return true;
		}
		GlobalPos gpos = info.targetInfo().opTargetPos().get();
		Optional<EntityType<?>> randomMobOp = mobs.getRandom(info.targetInfo().level().random);
		if (randomMobOp.isEmpty()) {
			System.out.println("Could not select mobID to spawn for some reason; cannot run " + this);
			return true;
		}
		EntityType<?> etype = randomMobOp.get();

		ServerLevel level = getCorrectLevel(info.targetInfo()).get();
		for (int i = 0; i < intensity; i++) {
			Entity entity = etype.create(level, EntitySpawnReason.MOB_SUMMONED);
			entity.snapTo(gpos.pos(), 0.0F, 0.0F);
			if (entity instanceof Mob) {
				((Mob) entity).finalizeSpawn(level, info.targetInfo().level().getCurrentDifficultyAt(gpos.pos()),
						EntitySpawnReason.MOB_SUMMONED, null);
			}
			if (entity instanceof LightningBolt b) {
				b.setVisualOnly(true);
			}
			info.targetInfo().level().addFreshEntityWithPassengers(entity);
			entityPostprocessing(entity, info);
			info.targetInfo().level().gameEvent(GameEvent.ENTITY_PLACE, gpos.pos(), GameEvent.Context
					.of(info.targetInfo().opCaster().map((u) -> info.targetInfo().level().getEntity(u)).orElse(null)));
		}
		return false;
	}

}
