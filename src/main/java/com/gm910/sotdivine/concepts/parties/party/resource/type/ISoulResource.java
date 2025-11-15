package com.gm910.sotdivine.concepts.parties.party.resource.type;

import com.gm910.sotdivine.concepts.parties.party.resource.IPartyResource;
import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * A resource representing a single unique entity, i.e. a "soul"
 */
public interface ISoulResource extends IPartyResource {

	public static final String TYPE_SAVE_TAG = "entity_type_sotdivine";

	/**
	 * Create a resource for the given entity
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static SoulResource create(Entity entity) {
		try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
				entity.problemPath(), LogUtils.getLogger())) {
			TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector,
					entity.registryAccess());
			entity.saveWithoutId(tagvalueoutput);
			tagvalueoutput.store(EntityType.CODEC.fieldOf(TYPE_SAVE_TAG), entity.getType());
			return new SoulResource(tagvalueoutput.buildResult());

		}

	}

	/**
	 * Return the soul data as a compound tag
	 * 
	 * @return
	 */
	public CompoundTag getSoulData();

	/**
	 * Return an entity generated from the soul data
	 * 
	 * @param inWorld
	 * @return
	 */
	public default Entity regenerateEntity(Level inWorld, EntitySpawnReason reason) {
		EntityType<?> entityType = getSoulData().read(EntityType.CODEC.fieldOf(TYPE_SAVE_TAG))
				.orElseThrow(() -> new IllegalArgumentException("No entity type stored in this tag: " + getSoulData()));
		Entity entity = entityType.create(inWorld, reason);
		try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
				entity.problemPath(), LogUtils.getLogger())) {
			ValueInput tvi = TagValueInput.create(problemreporter$scopedcollector, entity.registryAccess(),
					getSoulData());
			entity.load(tvi);
			return entity;
		}
	}
}
