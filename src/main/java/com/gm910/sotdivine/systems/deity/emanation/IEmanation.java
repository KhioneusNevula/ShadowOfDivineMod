package com.gm910.sotdivine.systems.deity.emanation;

import java.util.Optional;

import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellTargetInfo;
import com.mojang.serialization.Codec;

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
	 * If this emanation is a kind of spell, return the spell properties
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
	 * Return true if this effect failed to trigger
	 * 
	 * @param info
	 * @return
	 */
	public boolean trigger(EmanationInstance instance);

	/**
	 * Return true if this spell can continue ticking; update variables that are
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
	 * @param instance
	 * @return
	 */
	public default boolean checkIfCanTrigger(EmanationInstance instance) {
		return true;
	}

	/**
	 * Return true if this spell failed while ticking
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
	 * @param instance
	 */
	public default void interrupt(EmanationInstance instance) {

	}

	/**
	 * Get what type of emanation this is
	 * 
	 * @return
	 */
	public EmanationType<?> getEmanationType();
}
