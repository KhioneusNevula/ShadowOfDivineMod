package com.gm910.sotdivine.mixins_assist.pathfinding;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * A record to store a pathType type and the mobID it pertains to, which may be
 * null
 */
public record WrappedPathType(PathType pathType, Optional<UUID> mobID) {

	public WrappedPathType(PathType path, Entity mob) {
		this(path, Optional.ofNullable(mob).map(Entity::getUUID));

	}

	public WrappedPathType(PathType path, UUID mob) {
		this(path, Optional.ofNullable(mob));
	}

	public WrappedPathType(PathType path) {
		this(path, Optional.empty());
	}

}
