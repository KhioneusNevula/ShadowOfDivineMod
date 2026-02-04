package com.gm910.sotdivine.magic.afterlife.anchors;

import java.util.Optional;

import com.gm910.sotdivine.magic.afterlife.Afterlife;
import com.gm910.sotdivine.magic.afterlife.IAfterlife;
import com.gm910.sotdivine.magic.afterlife.Soul;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * An anchor for a soul
 */
public record SoulAnchor(Soul storedObject) implements IAfterlifeAnchor<Entity, Soul> {

	public static MapCodec<SoulAnchor> getCodec() {
		return RecordCodecBuilder
				.mapCodec(instance -> instance.group(Soul.CODEC.fieldOf("soul").forGetter(SoulAnchor::storedObject))
						.apply(instance, SoulAnchor::new));
	}

	public static SoulAnchor from(Soul entity) {
		return new SoulAnchor(entity);
	}

	/**
	 * Creates from entity and sets internal afterlife reference to that of the
	 * given entity's world
	 * 
	 * @param entity
	 * @return
	 */
	public static SoulAnchor withAfterlife(Entity entity) {
		return new SoulAnchor(Soul.createSoulFromEntityUnsafely(entity, IAfterlife.get(entity.getServer())));
	}

	@Override
	public SoulAnchor updateAfterlifeReference(Afterlife afterlife) {
		storedObject.updateAfterlifeReference(afterlife);
		return this;
	}

	@Override
	public Optional<Entity> getAnchor(ServerLevel level) {
		return storedObject.getExistingEntity(level);
	}

	@Override
	public AfterlifeAnchorType<?> getAnchorType() {
		return AfterlifeAnchorType.SOUL.get();
	}

	@Override
	public String toString() {
		return "Anchor(" + this.storedObject + ")";
	}

}
