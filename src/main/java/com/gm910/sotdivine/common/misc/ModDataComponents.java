package com.gm910.sotdivine.common.misc;

import java.util.function.Supplier;

import com.gm910.sotdivine.SOTDMod;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.registries.RegistryObject;

/**
 * Class for the creative tabs of this mod
 * 
 * @author borah
 *
 */
public class ModDataComponents {
	private ModDataComponents() {
	}

	public static void init() {
		System.out.println("Initializing data components...");
	}

	/**
	 * The time an item is protected from fire
	 */
	public static final RegistryObject<DataComponentType<Integer>> FIRE_PROTECTION_TIME = register(
			"fire_protection_time", () -> DataComponentType.<Integer>builder().persistent(Codec.INT)
					.networkSynchronized(ByteBufCodecs.INT).build());

	public static <T> RegistryObject<DataComponentType<T>> register(String name, Supplier<DataComponentType<T>> func) {
		return SOTDMod.DATA_COMPONENT_TYPES.register(name, () -> func.get());
	}

}
