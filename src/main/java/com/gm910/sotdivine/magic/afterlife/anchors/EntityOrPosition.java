package com.gm910.sotdivine.magic.afterlife.anchors;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;

/**
 * A simple wrapper class which contains either an entity or a position
 * 
 * @param <AO>
 * @param <SO>
 */
public record EntityOrPosition(Either<EntityReference<Entity>, GlobalPos> entityOrPos) {

	public static final Codec<EntityOrPosition> CODEC = Codec
			.either(EntityReference.<Entity>codec().fieldOf("entity").codec(),
					GlobalPos.CODEC.fieldOf("position").codec())
			.xmap((s) -> s.map(e -> entity(e), b -> position(b)), (s) -> s.entityOrPos);

	public static EntityOrPosition entity(EntityReference<Entity> en) {
		return new EntityOrPosition(Either.left(en));
	}

	public static EntityOrPosition position(GlobalPos en) {
		return new EntityOrPosition(Either.right(en));
	}
}
