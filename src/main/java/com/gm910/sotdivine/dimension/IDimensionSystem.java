package com.gm910.sotdivine.dimension;

import java.util.Map;
import java.util.Optional;

import com.google.common.base.Functions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * A system of storing dimensions
 */
public interface IDimensionSystem {

	public static final String SAVED_DATA_ID = "sotdivine_dimensions";
	/**
	 * Codec for sanctuary system
	 */
	public static final Codec<IDimensionSystem> CODEC = RecordCodecBuilder
			.create(instance -> instance.group(Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, DimensionProperties.CODEC)
					.fieldOf("dimensions").forGetter(IDimensionSystem::getDimensions))
					.apply(instance, DimensionSystem::new));

	public static final SavedDataType<DimensionSystem> SAVE_TYPE = new SavedDataType<>(SAVED_DATA_ID,
			DimensionSystem::new, CODEC.xmap((x) -> (DimensionSystem) x, Functions.identity()), null);

	/**
	 * Returns the dimension system accessed most recently, if present. Useful if we
	 * have no {@link ServerLevel} reference. However, naturally
	 * {@link IDimensionSystem#get(ServerLevel)} is preferred categorically
	 * 
	 * @return
	 */
	public static Optional<IDimensionSystem> getCached() {
		return Optional.ofNullable(DimensionSystem.mostRecentCached);
	}

	/**
	 * Retrieve an instance of the sanctuary system from the given level.
	 * 
	 * @param level
	 * @return
	 */
	public static IDimensionSystem get(ServerLevel levelg) {
		DimensionSystem obtain = null;
		ServerLevel level;
		if (levelg.dimension().equals(Level.OVERWORLD)) {
			level = levelg;
		} else {
			level = levelg.getServer().overworld();
		}
		if (DimensionSystem.cachedSystems.containsKey(level)) {
			obtain = DimensionSystem.cachedSystems.get(level);
		} else {

			obtain = level.getDataStorage().computeIfAbsent(SAVE_TYPE);

			DimensionSystem.cachedSystems.put(level, obtain);
			obtain.server = levelg.getServer();
		}
		DimensionSystem.mostRecentCached = obtain;
		return obtain;
	}

	/**
	 * Load in all dimensions
	 * 
	 * @param server
	 */
	public void onLoad(MinecraftServer server);

	/**
	 * Add a new dimension
	 * 
	 * @param loadIn whether to load this dimension into the world or not; should be
	 *               true for most cases
	 */
	public void addDimension(ResourceKey<Level> dimension, DimensionProperties definition, boolean loadIn);

	/**
	 * Add a new dimension and load it in
	 */
	public default void addDimension(ResourceKey<Level> dimension, DimensionProperties definition) {
		addDimension(dimension, definition, true);
	}

	/**
	 * Remove the given dimension
	 * 
	 * @param dimension
	 * @return
	 */
	public DimensionProperties removeDimension(ResourceKey<Level> dimension);

	/**
	 * Changes the properties of the given dimension
	 * 
	 * @param dimension
	 * @param properties
	 */
	public void setProperties(ResourceKey<Level> dimension, DimensionProperties properties);

	/**
	 * Return a map of dimensions
	 * 
	 * @return
	 */
	Map<ResourceKey<Level>, DimensionProperties> getDimensions();

	/**
	 * Returns the dimension properties of the given level
	 * 
	 * @param level
	 * @return
	 */
	public static Optional<DimensionProperties> propertiesOf(ServerLevel level) {
		return Optional.ofNullable(IDimensionSystem.get(level).getDimensions().get(level.dimension()));
	}

	/**
	 * Returns the dimension properties of the given level
	 * 
	 * @param level
	 * @return
	 */
	public static Optional<DimensionProperties> propertiesOf(ResourceKey<Level> level) {
		return IDimensionSystem.getCached().map(di -> di.getDimensions().get(level));
	}

}
