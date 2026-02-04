package com.gm910.sotdivine.common.effects;

import java.awt.Color;
import java.util.function.Supplier;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.effects.types.MeditationEffect;
import com.gm910.sotdivine.common.effects.types.IntangibleEffect;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.RegistryObject;

/**
 * Mod items class
 * 
 * @author borah
 *
 */
public class ModEffects {
	private ModEffects() {
	}

	public static void init() {
		System.out.println("Initializing mod effects");
	}

	public static final RegistryObject<MobEffect> MEDITATING = register("meditating",
			() -> new MeditationEffect(MobEffectCategory.NEUTRAL, 9154528));

	/**
	 * An effect for ghosts, making them completely non-interactive with the world
	 */
	public static final RegistryObject<MobEffect> INTANGIBLE = register("intangible",
			() -> new IntangibleEffect(MobEffectCategory.NEUTRAL, Color.cyan.brighter().getRGB()));

	/**
	 * Register items, also permit them to be applied to creative mode tabs
	 * 
	 * @param blockID
	 * @param supplier
	 * @param tabs
	 * @return
	 */
	public static RegistryObject<MobEffect> register(String blockID, Supplier<MobEffect> supplier) {
		ResourceKey<MobEffect> key = SOTDMod.EFFECTS.key(blockID);
		var obj = SOTDMod.EFFECTS.register(blockID, supplier);
		return obj;
	}

}
