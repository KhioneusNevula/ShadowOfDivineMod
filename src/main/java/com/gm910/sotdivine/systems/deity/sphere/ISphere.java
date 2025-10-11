package com.gm910.sotdivine.systems.deity.sphere;

import java.util.Collection;

import com.gm910.sotdivine.systems.deity.emanation.DeityInteractionType;
import com.gm910.sotdivine.systems.deity.emanation.IEmanation;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The divine domain of influence of a deity
 * 
 * @author borah
 *
 */
public sealed interface ISphere permits Sphere {
	public static final ResourceKey<Registry<ISphere>> REGISTRY_KEY = ResourceKey
			.createRegistryKey(ModUtils.path("deity_sphere"));

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
	public default boolean canOffer(ItemStack stack) {
		return this.getGenres(Genres.OFFERING).stream().anyMatch((x) -> x.test(stack));
	}

	/**
	 * Get all things of the given genre; may return null if the genre is not
	 * represented for this sphere, for some reason
	 * 
	 * @param <T>
	 * @param genre
	 * @return
	 */
	public <T> Collection<T> getGenres(IGenre<T> genre);

	public Collection<IGenre<?>> representedGenres();

	// Map<String, ISemanticSpecificationValue> semantics();

}
