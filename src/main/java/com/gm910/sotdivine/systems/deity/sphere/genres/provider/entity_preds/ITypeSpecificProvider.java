package com.gm910.sotdivine.systems.deity.sphere.genres.provider.entity_preds;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;

import net.minecraft.world.entity.Entity;

public interface ITypeSpecificProvider<E extends Entity> extends IGenreProvider<E, E> {

	public Class<? super E> entityClass();
}
