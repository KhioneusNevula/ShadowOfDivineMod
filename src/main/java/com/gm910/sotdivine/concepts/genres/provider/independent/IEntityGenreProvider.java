package com.gm910.sotdivine.concepts.genres.provider.independent;

import java.util.Optional;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Genre provider for something which can target a specific entity or
 * create/transform an entity
 * 
 * @param <T>
 * @param <G>
 */

public interface IEntityGenreProvider<T, G> extends IGenreProvider<T, G> {

	/**
	 * Whether the specific Entity matches
	 * 
	 * @param emanation
	 * @return
	 */
	public boolean matchesEntity(ServerLevel level, Entity entity);

	/**
	 * Either create a new entity or transform an existing one (return null if only
	 * transformation is possible and the given entity is empty)
	 * 
	 * @param level
	 * @param entity
	 * @return
	 */
	@Nullable
	public Entity generateRandomForEntity(ServerLevel level, Optional<Entity> entity);

}
