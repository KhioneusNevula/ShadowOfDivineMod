package com.gm910.sotdivine.dimension.powers;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.magic.power.IPowerSource;
import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;

/**
 * Properties that are ambient in a dimension, and (can be channeled using
 * magic?)
 */
public interface IDimensionPower extends IPowerSource {

	public static Codec<IDimensionPower> codec() {

		return DimensionPowerType.resourceCodec();
	}

	/**
	 * Behaviors that occur repeatedly within the dimension
	 * 
	 * @param level
	 * @param spawnHostiles
	 * @param spawnFriendlies
	 */
	public void tick(IDeity creator, ServerLevel level, boolean spawnHostiles, boolean spawnFriendlies);

	/**
	 * For powers that affect deaths
	 * 
	 * @param creator
	 * @param event
	 * @return whether the event is canceled
	 */
	public default boolean effectOnDeath(IDeity creator, LivingDeathEvent event) {
		return false;
	}

	/**
	 * For powers that affect ticking entities
	 * 
	 * @param creator
	 * @param event
	 */
	public default void effectOnLivingTick(IDeity creator, LivingTickEvent event) {

	}

	/**
	 * For powers that affect entities undergoing damage
	 * 
	 * @param creator
	 * @param event
	 * @return whether the event is canceled
	 */
	public default boolean effectOnDamage(IDeity creator, LivingDamageEvent event) {
		return false;
	}

	/**
	 * For powers that affect entities entering this dimesnon
	 * 
	 * @param creator
	 * @param event
	 * @return true if the uuid is not permitted to come to this dimension
	 */
	public default boolean effectBeforeEntry(IDeity creator, EntityTravelToDimensionEvent event) {
		return false;
	}

	/**
	 * For powers that affect entities exiting this dimension
	 * 
	 * @param creator
	 * @param event
	 * @return true if the uuid is not permitted to exit this dimension
	 */
	public default boolean effectOnExit(IDeity creator, EntityTravelToDimensionEvent event) {
		return false;
	}

	/**
	 * Return whether this power is primary, secondary, or can be either
	 * 
	 * @return
	 */
	public Importance getImportance();

	/**
	 * Return what type of dimension power this is
	 * 
	 * @return
	 */
	public DimensionPowerType<?> getDimensionPowerType();

	/**
	 * A dimension name. If this is a {@link Importance#PRIMARY} or
	 * {@link Importance#EITHER} power, this method will be called to determine a
	 * dimension's name; otherwise, it will not.
	 * 
	 * @return
	 */
	public Component dimensionName();

	/**
	 * A path for a resource location. If this is a {@link Importance#PRIMARY} or
	 * {@link Importance#EITHER} power, this method will be called to determine a
	 * dimension's path; otherwise, it will not.
	 * 
	 * @return
	 */
	public String dimensionPath();

	public static enum Importance {
		/**
		 * This power must be the predominant power of its own, independent dimension
		 */
		PRIMARY,
		/** This power can only exist as a secondary power in a dimension */
		SECONDARY,
		/** This power may be primary or secondary */
		EITHER;

		public boolean canBePrimary() {
			return this == PRIMARY || this == EITHER;
		}

		public boolean canBeSecondary() {
			return this == SECONDARY || this == EITHER;
		}
	}
}
