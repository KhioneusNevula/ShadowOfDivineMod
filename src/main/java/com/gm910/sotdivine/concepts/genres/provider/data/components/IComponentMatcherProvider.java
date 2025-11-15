package com.gm910.sotdivine.concepts.genres.provider.data.components;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.TypedDataComponent;

/**
 * A provider which matches some component
 * 
 * @param <T>
 */
public interface IComponentMatcherProvider<T> extends IGenreProvider<DataComponentGetter, TypedDataComponent<T>> {

}
