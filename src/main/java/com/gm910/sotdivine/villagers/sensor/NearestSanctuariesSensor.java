package com.gm910.sotdivine.villagers.sensor;

import java.awt.Point;
import java.util.Set;

import com.gm910.sotdivine.magic.sanctuary.CachedSanctuaries;
import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.villagers.ModBrainElements;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.ChunkPos;

/**
 * Senses what party's territory an entity is currently in
 * 
 * @author borah
 *
 */
public class NearestSanctuariesSensor extends Sensor<LivingEntity> {

	/**
	 * How many more chunks in each direction to tcheck
	 */
	private static final int CHUNK_RADIUS = 2;

	@Override
	protected void doTick(ServerLevel level, LivingEntity villager) {
		ISanctuarySystem system = ISanctuarySystem.get(level);
		ChunkPos currentChunk = villager.chunkPosition();

		villager.getBrain().setMemory(ModBrainElements.MemoryModuleTypes.NEAREST_SANCTUARIES.get(),
				new CachedSanctuaries(() -> CollectionUtils
						.stream2D(new Point(currentChunk.x - CHUNK_RADIUS, currentChunk.z - CHUNK_RADIUS),
								new Point(currentChunk.x + CHUNK_RADIUS, currentChunk.z + CHUNK_RADIUS))
						.map((p) -> new ChunkPos(p.x, p.y)).flatMap((cur) -> system.getSanctuaries(cur))
						.filter((s) -> s.timeUntilForbidden(villager) > 0).iterator()));

	}

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(ModBrainElements.MemoryModuleTypes.NEAREST_SANCTUARIES.get());
	}
}
