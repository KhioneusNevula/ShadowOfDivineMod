package com.gm910.sotdivine.systems.villagers.sensor;

import java.util.Set;
import java.util.stream.Collectors;

import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party_system.IPartySystem;
import com.gm910.sotdivine.systems.villagers.ModBrainElements;

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
public class PartyTerritorySensor extends Sensor<LivingEntity> {

	@Override
	protected void doTick(ServerLevel level, LivingEntity villager) {
		IPartySystem system = IPartySystem.get(level);
		ChunkPos currentChunk = villager.chunkPosition();
		Set<IParty> parties = system.regionOwners(currentChunk, level.dimension()).filter(IParty::isGroup)
				.collect(Collectors.toSet());
		villager.getBrain().setMemory(ModBrainElements.MemoryModuleTypes.CURRENT_PARTY_TERRITORY.get(), parties);
	}

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(ModBrainElements.MemoryModuleTypes.CURRENT_PARTY_TERRITORY.get());
	}

}
