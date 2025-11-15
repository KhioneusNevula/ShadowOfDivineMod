package com.gm910.sotdivine.magic.emanation;

import java.util.Optional;

import com.gm910.sotdivine.magic.emanation.spell.ISpellProperties;
import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;

/**
 * Something a deity causes to occur
 * 
 * @author borah
 *
 */
public interface IEmanation {

	public static Codec<IEmanation> codec() {

		return EmanationType.resourceCodec();
	}

	/**
	 * If this emanation is a kind of SPELL, return the SPELL properties
	 * 
	 * @return
	 */
	public Optional<ISpellProperties> optionalSpellProperties();

	/**
	 * If this effect targets an entity
	 * 
	 * @return
	 */
	public boolean targetsEntity();

	/**
	 * If this effect targets a block pos
	 * 
	 * @return
	 */
	public boolean targetsPos();

	/**
	 * Whether this emanation occurs over a period of time
	 * 
	 * @return
	 */
	public boolean isDurative();

	/**
	 * Return true if this effect failed to trigger.
	 * 
	 * @param info
	 * @param intensity 1.0f is "normal" intensity, with greater or lesser being
	 *                  increased/decreased intensity
	 * @return
	 */
	public boolean trigger(EmanationInstance instance, float intensity);

	/**
	 * Return true if this SPELL can continue ticking; update variables that are
	 * relevant, too
	 * 
	 * @param info
	 * @param tick
	 * @return
	 */
	public default boolean checkIfCanTick(EmanationInstance instance) {
		return false;
	}

	/**
	 * Whether this emanation can trigger; update variables relevant, too
	 * 
	 * @param emanation
	 * @return
	 */
	public default boolean checkIfCanTrigger(EmanationInstance instance) {
		return true;
	}

	/**
	 * Return true if this SPELL failed while ticking
	 * 
	 * @param info
	 * @param tick
	 * @return
	 */
	public default boolean tick(EmanationInstance instance) {

		return false;
	}

	/**
	 * Call this when an emanation is interrupted to clean up loose end
	 * 
	 * @param emanation
	 */
	public default void interrupt(EmanationInstance instance) {

	}

	/**
	 * Get what type of emanation this is
	 * 
	 * @return
	 */
	public EmanationType<?> getEmanationType();

	/**
	 * Return a name that uniquely captures this emanation
	 * 
	 * @return
	 */
	public String uniqueName();

	/**
	 * Represent emanation as translation component
	 * 
	 * @return
	 */
	public Component translate();
}
