package com.gm910.sotdivine.concepts.parties.villagers;

import java.util.Collection;
import java.util.Optional;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.villagers.sensor.PartyTerritorySensor;
import com.mojang.serialization.Codec;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Memory module types for this mod
 * 
 * @author borah
 *
 */
public final class ModBrainElements {
	private ModBrainElements() {
	}

	public static void init() {
		System.out.println("Initializing mod brain elements...");
		MemoryModuleTypes.init();
		SensorTypes.init();
	}

	public static final class MemoryModuleTypes {
		private MemoryModuleTypes() {
		}

		public static void init() {
			System.out.println("Initializing memory modules...");
		}

		/**
		 * Memory module for what party's territory we are currently in
		 */
		public static final RegistryObject<MemoryModuleType<Collection<IParty>>> CURRENT_PARTY_TERRITORY = SOTDMod.MEMORY_MODULE_TYPES
				.register("current_party_territory", () -> new MemoryModuleType<>(Optional.empty()));

		/**
		 * A memory module for what the party ID is
		 */
		public static final RegistryObject<MemoryModuleType<String>> PARTY_ID = SOTDMod.MEMORY_MODULE_TYPES
				.register("party_id", () -> new MemoryModuleType<>(Optional.of(Codec.STRING)));

		/**
		 * A memory module for who the leader of the village is
		 */
		public static final RegistryObject<MemoryModuleType<EntityReference<LivingEntity>>> VILLAGE_LEADER = SOTDMod.MEMORY_MODULE_TYPES
				.register("village_leader", () -> new MemoryModuleType<>(Optional.of(EntityReference.codec())));
	}

	public static final class SensorTypes {
		private SensorTypes() {
		}

		public static void init() {
			System.out.println("Initializing sensors...");
		}

		/**
		 * Sensor that senses what parties' territory we're on rn
		 */
		public static final RegistryObject<SensorType<PartyTerritorySensor>> PARTY_TERRITORY = SOTDMod.SENSOR_TYPES
				.register("party_territory", () -> new SensorType<>(PartyTerritorySensor::new));
	}

}
