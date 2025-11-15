package com.gm910.sotdivine.concepts.parties.party.resource.type;

import java.util.Optional;

import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.concepts.parties.party.resource.ResourceValue;
import com.mojang.logging.LogUtils;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

public class EntityResource implements IEntityResource {

	private EntityType<?> type;
	private Optional<CompoundTag> data;
	private boolean fungible;

	/**
	 * REturn an entity resource with no data. The resource will be marked as
	 * fungible.
	 */
	public static EntityResource create(EntityType<?> type) {
		return new EntityResource(type, Optional.empty(), true);
	}

	/**
	 * Return an entity resource. The resource will be marked as fungible if the
	 * data tag contains no UUID.
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static EntityResource create(EntityType<?> type, CompoundTag data) {
		return new EntityResource(type, Optional.of(data), data.read("UUID", UUIDUtil.CODEC).isEmpty());
	}

	public EntityResource(EntityType<?> type, Optional<CompoundTag> data, boolean fungible) {
		this.type = type;
		this.data = data;
		this.fungible = fungible;
	}

	@Override
	public boolean isDeed() {
		return false;
	}

	@Override
	public boolean isFungible() {
		return fungible;
	}

	@Override
	public PartyResourceType<IEntityResource> resourceType() {
		return PartyResourceType.ENTITY.get();
	}

	@Override
	public int resourceValue(IParty party, ResourceValue value) {
		return 0;
	}

	@Override
	public EntityType<?> entityType() {
		return type;
	}

	@Override
	public Optional<CompoundTag> opTag() {
		return data;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof IEntityResource) {
			IEntityResource resource = (IEntityResource) obj;
			return this.type.equals(resource.entityType()) && this.data.equals(resource.opTag())
					&& this.fungible == resource.isFungible();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.type.hashCode() + this.opTag().hashCode();
	}

	@Override
	public String toString() {
		return "EntityResource[" + this.type + "," + this.data.map(CompoundTag::toString).orElse("(no data)") + "]";
	}

	/**
	 * Return an entity generated from the data
	 * 
	 * @param inWorld
	 * @return
	 */
	private Entity regenerateEntity(Level inWorld, EntitySpawnReason reason) {
		Entity entity = type.create(inWorld, reason);
		if (data.isPresent()) {
			try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
					entity.problemPath(), LogUtils.getLogger())) {
				ValueInput tvi = TagValueInput.create(problemreporter$scopedcollector, entity.registryAccess(),
						data.get());
				entity.load(tvi);
				return entity;
			}
		}
		return entity;
	}

	@Override
	public String report(Level access) {
		Entity en = this.regenerateEntity(access, EntitySpawnReason.COMMAND);
		return "EntityResource" + (this.fungible ? "Fungible" : "Unique") + "[\"" + en.getDisplayName().getString()
				+ "\"]";
	}

}
