package com.gm910.sotdivine.concepts.parties.party.resource.type;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.gm910.sotdivine.common.entities.ModEntityTags;
import com.gm910.sotdivine.concepts.parties.party.resource.IPartyResource;
import com.mojang.logging.LogUtils;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * A resource representing a single unique uuid, i.e. a "soul"
 */
public sealed interface ISoulResource extends IPartyResource permits SoulResource {

	public static final String TYPE_SAVE_TAG = "entity_type_sotdivine";

	/**
	 * Create a resource for the given uuid. Please make sure the entity is not
	 * riding
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static ISoulResource create(Entity entity) {
		try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
				entity.problemPath(), LogUtils.getLogger())) {
			TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector,
					entity.registryAccess());
			entity.saveWithoutId(tagvalueoutput);
			tagvalueoutput.discard(Entity.TAG_PASSENGERS);
			tagvalueoutput.store(Entity.TAG_UUID, UUIDUtil.CODEC, entity.getUUID());
			tagvalueoutput.store(TYPE_SAVE_TAG, EntityType.CODEC, entity.getType());
			return new SoulResource(tagvalueoutput.buildResult());

		}

	}

	public ISoulResource copy();

	/**
	 * Return the soul data as a compound tag
	 * 
	 * @return
	 */
	public CompoundTag getSoulData();

	/**
	 * REturn uuid of stored uuid
	 * 
	 * @return
	 */
	public default Optional<UUID> getUUID() {
		return getSoulData().read(Entity.TAG_UUID, UUIDUtil.CODEC);
	}

	/**
	 * Set uuid of stored uuid
	 * 
	 * @return
	 */
	public default ISoulResource replaceUUID(UUID id) {
		getSoulData().storeNullable(Entity.TAG_UUID, UUIDUtil.CODEC, id);
		return this;
	}

	/**
	 * Returns custom name of uuid (if present)
	 * 
	 * @return
	 */
	public default Optional<Component> getCustomName() {
		return getSoulData().read("CustomName", ComponentSerialization.CODEC);
	}

	/**
	 * Returns a name for this entity, either its custom name or a broader
	 * description based on its entity type
	 * 
	 * @return
	 */
	public default Component getName(ServerLevel level) {

		return getCustomName().orElse(this.getEntityType().getDescription());
	}

	/**
	 * Set custom name of entity (can be to null)
	 * 
	 * @return
	 */
	public default ISoulResource replaceCustomName(Component name) {
		getSoulData().storeNullable("CustomName", ComponentSerialization.CODEC, name);
		return this;
	}

	/**
	 * Returns the tag representing the brain
	 * 
	 * @return
	 */
	public default Optional<Tag> getBrainTag() {
		return Optional.ofNullable(getSoulData().get(LivingEntity.TAG_BRAIN));
	}

	/**
	 * Replace the tag of the brain of this soul
	 * 
	 * @param tag
	 */
	public default ISoulResource replaceBrainTag(@Nullable Tag tag) {
		if (tag == null) {
			getSoulData().remove(LivingEntity.TAG_BRAIN);
		} else {
			getSoulData().put(LivingEntity.TAG_BRAIN, tag);
		}
		return this;
	}

	/**
	 * Return the health of this soul
	 * 
	 * @return
	 */
	public default float getHealth() {
		return getSoulData().getFloat(LivingEntity.TAG_HEALTH)
				.orElseGet(() -> this.getAttributes().map(am -> (float) am.getValue(Attributes.MAX_HEALTH)).orElse(0f));
	}

	/**
	 * Change health of this entity
	 * 
	 * @param to
	 */
	public default void setHealth(float to) {

		getSoulData().putFloat(LivingEntity.TAG_HEALTH, to);
	}

	/**
	 * Return a map of the attributes of this entity
	 * 
	 * @return
	 */
	public default Optional<AttributeMap> getAttributes() {
		return getSoulData().read(LivingEntity.TAG_ATTRIBUTES, AttributeInstance.Packed.LIST_CODEC).flatMap(ama -> {
			AttributeMap ret;
			try {
				ret = new AttributeMap(
						DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) this.getEntityType()));
			} catch (ClassCastException e) {
				return Optional.empty();
			}
			ret.apply(ama);
			return Optional.ofNullable(ret);
		});
	}

	/**
	 * Writes a map of attributes to this
	 * 
	 * @param attributes
	 */
	public default void setAttributes(AttributeMap attributes) {
		getSoulData().store(LivingEntity.TAG_ATTRIBUTES, AttributeInstance.Packed.LIST_CODEC, attributes.pack());
	}

	/**
	 * Return the owner of this, or null
	 * 
	 * @param level
	 * @return
	 */
	public default Optional<EntityReference<LivingEntity>> getOwner(ServerLevel level) {
		ValueInput tagval = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), getSoulData());
		EntityReference<LivingEntity> entityreference = EntityReference.readWithOldOwnerConversion(tagval, "Owner",
				level);
		return Optional.ofNullable(entityreference);
	}

	/**
	 * Set the owner of this, or set it to null
	 */
	public default ISoulResource setOwner(@Nullable EntityReference<LivingEntity> owner) {
		getSoulData().storeNullable("Owner", UUIDUtil.CODEC, owner.getUUID());
		return this;
	}

	/**
	 * If this soul is the kind that should not be "replaced", aside from being a
	 * pet or something ismilar
	 * 
	 * @return
	 */
	public default boolean isSignificant() {
		return getSoulData().getBoolean("PersistenceRequired").orElse(false)
				|| getEntityType().is(ModEntityTags.NATURALLY_PERSISTENT);
	}

	/**
	 * Returns the type of uuid stored
	 * 
	 * @return
	 */
	public default EntityType<?> getEntityType() {
		return getSoulData().read(EntityType.CODEC.fieldOf(TYPE_SAVE_TAG))
				.orElseThrow(() -> new IllegalArgumentException("No uuid type stored in this tag: " + getSoulData()));
	}

	/**
	 * Return an entity generated from the soul data
	 * 
	 * @param inWorld
	 * @return
	 */
	public default Optional<Entity> regenerateEntity(Level inWorld, EntitySpawnReason reason) {
		Optional<Entity> entityO = Optional.ofNullable(getEntityType().create(inWorld, reason));

		return entityO.map(entity -> {
			this.applyToEntity(entity);
			return entity;
		});
	}

	/**
	 * Apply this to the given entity
	 * 
	 * @param entity
	 */
	public default void applyToEntity(Entity entity) {
		if (this.getEntityType() != entity.getType()) {
			throw new IllegalArgumentException("Entity " + entity + " is of wrong type");
		}

		try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
				entity.problemPath(), LogUtils.getLogger())) {
			ValueInput tvi = TagValueInput.create(problemreporter$scopedcollector, entity.registryAccess(),
					getSoulData());
			entity.load(tvi);

			if (entity instanceof LivingEntity liv) {
				if (liv.getHealth() <= 0) {
					liv.setHealth(liv.getMaxHealth());
				}
			}
		}
	}
}
