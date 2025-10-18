package com.gm910.sotdivine.systems.deity.sphere;

import java.util.Collection;

import com.gm910.sotdivine.systems.deity.emanation.DeityInteractionType;
import com.gm910.sotdivine.systems.deity.emanation.IEmanation;
import com.gm910.sotdivine.systems.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.systems.deity.sphere.genres.IGenreType;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The divine domain of influence of a deity
 * 
 * @author borah
 *
 */
public sealed interface ISphere permits Sphere {
	/**
	 * The name of this sphere
	 * 
	 * @return
	 */
	public ResourceLocation name();

	/**
	 * Display name of this sphere
	 * 
	 * @return
	 */
	public Component displayName();

	/**
	 * Return all list of the given type under this sphere
	 * 
	 * @param type
	 * @return
	 */
	Collection<IEmanation> emanationsOfType(DeityInteractionType type);

	/**
	 * If this item stack cna be an offering
	 * 
	 * @param stack
	 * @return
	 */
	public default boolean canOffer(ServerLevel level, ItemStack stack) {
		return this.getGenres(GenreTypes.OFFERING).stream().anyMatch((x) -> x.matches(level, stack));
	}

	/**
	 * Get all things of the given genre; may return null if the genre is not
	 * represented for this sphere, for some reason
	 * 
	 * @param <T>
	 * @param genre
	 * @return
	 */
	public <T> Collection<T> getGenres(IGenreType<T> genre);

	public Collection<IGenreType<?>> representedGenres();

	String report();

	public Collection<IEmanation> allEmanations();

	// Map<String, ISemanticSpecificationValue> semantics();

}
