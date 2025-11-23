package com.gm910.sotdivine.villagers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.concepts.parties.party.resource.type.IRegionResource;
import com.gm910.sotdivine.concepts.parties.party.resource.type.RegionResource;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.util.FieldUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.gm910.sotdivine.util.WorldUtils;
import com.gm910.sotdivine.villagers.ModBrainElements.MemoryModuleTypes;
import com.gm910.sotdivine.villagers.ModBrainElements.SensorTypes;
import com.gm910.sotdivine.villagers.behavior.SetWalkTargetToSanctuary;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.util.BrainBuilder;

/**
 * Things villagers will do (event-based)
 * 
 * @author borah
 *
 */
public class VillagerTweaks {
	private VillagerTweaks() {
	}

	/**
	 * Add things to the mind of this villager
	 * 
	 * @param villager
	 * @param level
	 * @param brainBuilder
	 */
	@SuppressWarnings("deprecation")
	public static void modifyMind(Villager villager, ServerLevel level, @Nullable BrainBuilder<Villager> brainBuilder) {

		if (brainBuilder != null) {

			Collection<SensorType<? extends Sensor<? super Villager>>> sensors = FieldUtils
					.getInstanceField("sensorTypes", "sensorTypes", brainBuilder);
			Collection<MemoryModuleType<?>> memories = FieldUtils.getInstanceField("memoryTypes", "memoryTypes",
					brainBuilder);

			// add new sensors
			sensors.add(SensorTypes.PARTY_TERRITORY.get());
			sensors.add(SensorTypes.NEAREST_SANCTUARIES.get());

			// add new memories
			memories.add(MemoryModuleTypes.CURRENT_PARTY_TERRITORY.get());
			memories.add(MemoryModuleTypes.NEAREST_SANCTUARIES.get());
			memories.add(MemoryModuleTypes.PARTY_ID.get());
			memories.add(MemoryModuleTypes.VILLAGE_LEADER.get());

			brainBuilder.addBehaviorToActivityByPriority(0, Activity.PANIC,
					SetWalkTargetToSanctuary.sanctuary(MemoryModuleTypes.NEAREST_SANCTUARIES.get(), 0.5f * 1.5f));
			LogUtils.getLogger().debug("Added sanctuary fleeing activity to " + villager);

		} else {

			Map<SensorType<? extends Sensor<? super Villager>>, Sensor<? super Villager>> sensors = FieldUtils
					.getInstanceField("sensors", "e", villager.getBrain());
			Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = villager.getBrain()
					.getMemories();

			// add new sensors
			sensors.put(SensorTypes.PARTY_TERRITORY.get(), SensorTypes.PARTY_TERRITORY.get().create());

			// add new memories
			memories.put(MemoryModuleTypes.CURRENT_PARTY_TERRITORY.get(), Optional.empty());
			memories.put(MemoryModuleTypes.NEAREST_SANCTUARIES.get(), Optional.empty());
			memories.put(MemoryModuleTypes.PARTY_ID.get(), Optional.empty());
			memories.put(MemoryModuleTypes.VILLAGE_LEADER.get(), Optional.empty());

		}
	}

	/**
	 * Check if this villager is in a party; if not, join one or make one; if in
	 * territory that is not part of a party but is village-like, add it to a party
	 * 
	 * @param villager
	 * @param level
	 */
	public static void updatePartyStatus(Villager villager, ServerLevel level) {
		IPartySystem system = IPartySystem.get(level);
		try {

			/*
			 * if (!villager.getBrain().checkMemory(MemoryModuleTypes.PARTY_ID.get(),
			 * MemoryStatus.REGISTERED)) { System.out.println(
			 * "For some stupid reason this dumb npc has deleted its memories again and if this fails again IFeatures will kill it immediately."
			 * ); modifyMind(villager, level, null); villager.setRemainingFireTicks(5);
			 * return; }
			 */
			Optional<String> party = villager.getBrain().getMemory(MemoryModuleTypes.PARTY_ID.get())
					.filter(system::partyExists);

			if (party.isEmpty()) {
				if (!tryJoinParty(system, villager, level)) {
					if (level.getGameTime() % 20 == 0) {
						tryEstablishParty(system, villager, level);
					}
				}
			} else {
				if (level.getGameTime() % 20 == 0) {
					removeDefunctHoldings(system, villager, level, party.get());
				}
				tryCheckLeader(system, villager, level, party.get());

			}
		} catch (Exception e) {
			throw new RuntimeException("Villager " + villager.getUUID() + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Make this villager try to join an existing party
	 * 
	 * 
	 * @param villager
	 * @param level
	 */
	private static boolean tryJoinParty(IPartySystem system, Villager villager, ServerLevel level) {
		List<IParty> parties = system.regionOwners(villager.chunkPosition(), level.dimension()).filter(IParty::isGroup)
				.collect(Collectors.toCollection(ArrayList::new));
		if (!parties.isEmpty()) {
			// pick a random party if we already have a local party
			Collections.shuffle(parties, new Random(level.random.nextLong()));
			villager.getBrain().setMemory(MemoryModuleTypes.PARTY_ID.get(), parties.getFirst().uniqueName());
			parties.getFirst().memberCollection().add(new EntityReference<>(villager));
			// parties.getFirst().setResourceAmount(new IDResource(villager.getUUID()), 1);
			return true;
		}
		return false;

	}

	/**
	 * 
	 * establish a village party based on the current block pos
	 */
	private static boolean tryEstablishParty(IPartySystem system, Villager villager, ServerLevel level) {
		List<PoiRecord> villPois = level.getPoiManager().getInRange(p_219845_ -> p_219845_.is(PoiTypeTags.VILLAGE),
				villager.blockPosition(), 124, PoiManager.Occupancy.IS_OCCUPIED).toList();

		if (villPois.isEmpty()) {
			return false;
		}
		Set<ChunkPos> villageChunks = villPois.stream().map((poi) -> new ChunkPos(poi.getPos()))
				.collect(Collectors.toSet());
		BlockPos centra = WorldUtils.centerCP(villageChunks);
		List<IParty> existing = Lists.newArrayList(
				villageChunks.stream().flatMap((p) -> system.regionOwners(p, level.dimension())).iterator());
		IParty party;
		if (!existing.isEmpty()) {
			Collections.shuffle(existing);
			party = existing.getFirst();
		} else {
			party = IParty.createGroup(
					"village[" + centra.getX() + "," + centra.getZ() + "," + level.dimension().location() + "]",
					TextUtils.literal("Village at " + "[" + centra.getX() + "," + centra.getZ() + ","
							+ level.dimension().location() + "]"),
					villageChunks.stream().map((r) -> new RegionResource(r, level.dimension())).toList());
			system.addParty(party, level);
		}
		party.memberCollection().add(new EntityReference<>(villager));
		// party.setResourceAmount(new IDResource(villager.getUUID()), 1);
		villager.getBrain().setMemory(MemoryModuleTypes.VILLAGE_LEADER.get(), new EntityReference<>(villager));
		return true;
	}

	/**
	 * Remove regions from personal party if they have no occupants anymore
	 * 
	 * @param system
	 * @param villager
	 * @param level
	 * @return
	 */
	private static void removeDefunctHoldings(IPartySystem system, Villager villager, ServerLevel level,
			String partyID) {
		IParty party = system.getPartyByName(partyID)
				.orElseThrow(() -> new IllegalStateException("Party " + partyID + " doesn't exist"));
		// get resource for this region
		Optional<IRegionResource> resourceOp = party.getResourcesOfType(PartyResourceType.REGION.get()).stream()
				.filter((x) -> x.chunkPos().equals(villager.chunkPosition())).findAny();
		if (resourceOp.isPresent()) {
			List<PoiRecord> villPois = level.getPoiManager().getInChunk(p_219845_ -> p_219845_.is(PoiTypeTags.VILLAGE),
					villager.chunkPosition(), PoiManager.Occupancy.IS_OCCUPIED).toList();
			if (villPois.isEmpty() || villPois.stream().allMatch((x) -> !x.isOccupied())) {
				party.setResourceAmount(resourceOp.get(), 0);
			}
			system.markDirty(level);
		}
	}

	/**
	 * Tries to check who the local leader is and instrument memories to that leader
	 * 
	 * @param system
	 * @param villager
	 * @param level
	 * @param party
	 */
	private static void tryCheckLeader(IPartySystem system, Villager villager, ServerLevel level, String party) {
		Optional<LivingEntity> sameFaction = villager.getBrain()
				.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
				.flatMap((x) -> x
						.findClosest((a) -> a.getType() == villager.getType()
								&& a.getBrain().checkMemory(MemoryModuleTypes.PARTY_ID.get(), MemoryStatus.REGISTERED)
								&& a.getBrain().getMemory(MemoryModuleTypes.PARTY_ID.get()).filter(party::equals)
										.isPresent())
						.filter((br) -> br.getBrain().checkMemory(MemoryModuleTypes.VILLAGE_LEADER.get(),
								MemoryStatus.VALUE_PRESENT)));
		sameFaction.ifPresent((correspondent) -> villager.getBrain().setMemory(MemoryModuleTypes.VILLAGE_LEADER.get(),
				correspondent.getBrain().getMemory(MemoryModuleTypes.VILLAGE_LEADER.get()).get()));
	}

}
