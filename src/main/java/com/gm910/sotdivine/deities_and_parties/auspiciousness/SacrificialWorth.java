package com.gm910.sotdivine.deities_and_parties.auspiciousness;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * A class to handle the calculation of the sacrificial worth of offerings
 */
public class SacrificialWorth {
	private SacrificialWorth() {
	}

	/**
	 * Returns the "worth" of an offering to the gods
	 * 
	 * @param offering
	 * @return
	 */
	public static float calculateWorth(Entity offering) {
		if (offering instanceof LivingEntity liv) {
			return (float) (liv.getMaxHealth() + liv.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
		}
		return 0f;
	}

}
