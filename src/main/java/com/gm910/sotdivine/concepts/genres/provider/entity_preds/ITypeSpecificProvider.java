package com.gm910.sotdivine.concepts.genres.provider.entity_preds;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public interface ITypeSpecificProvider<E extends Entity> extends IGenreProvider<E, E> {

	public Class<? super E> entityClass();

	public ResourceLocation path();
}
