package com.gm910.sotdivine.magic.sanctuary.cap;

import java.util.Optional;

import javax.annotation.Nullable;

import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * A mob's internal storage of information pertaining to worldly sanctuaries
 */
public interface ISanctuaryInfo {

	public static final ResourceLocation CAPABILITY_PATH = ModUtils.path("sanctuary_info");

	public static final Capability<ISanctuaryInfo> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
	});

	public static ISanctuaryInfo get(ICapabilityProvider provider) {
		return provider.getCapability(CAPABILITY).orElseThrow(
				() -> new UnsupportedOperationException("Object " + provider + " does not have this capability"));
	}

	/**
	 * return true if this entity was banned recently from a sanctuary, i.e. more
	 * recently than the last time it was permitted into it
	 * 
	 * @return
	 */
	public boolean recentlyBanned(String sanct);

	/**
	 * return true if this entity was permitted recently from a sanctuary, i.e. more
	 * recently than the last time it was banned into it
	 * 
	 * @return
	 */
	public boolean recentlyPermitted(String sanct);

	/**
	 * The amount of time an entity can continue to remain in the given sanctuary;
	 * return -1 if this is not specified
	 * 
	 * @param sanctuary
	 * @return
	 */
	public int permissionTime(String sanctuaryName);

	/**
	 * Gives permission to enter the given sanctuary. Put {@link Integer#MAX_VALUE}
	 * to give indefinite permission
	 * 
	 * @param toSanctuary
	 */
	public void permitEntryTo(String sanctuaryName, int ticks);

	/**
	 * Revokes (temporary) permission to enter the given sanctuary
	 * 
	 * @param toSanctuary
	 */
	public void revokePermission(String sanctuaryName);

	/**
	 * Inverse of {@link #permitEntryTo( String, int)}; removes existing permission
	 * to enter the sanctuary for the given amount of time.
	 * 
	 * @param level
	 * @param sanctuaryName
	 * @param ticks
	 */
	public void banFrom(String sanctuaryName, int ticks);

	/**
	 * Inverse of {@link #permissionTime(String)}; the amount of remaining time this
	 * is banned from the sanctuary; return -1 if never spcified
	 * 
	 * @param level
	 * @param sanctuaryName
	 * @return
	 */
	public int banTime(String sanctuaryName);

	/**
	 * Removes the ban; inverse of {@link #revokePermission(String)}
	 * 
	 * @param sanctuaryName
	 */
	public void liftBan(String sanctuaryName);

	/**
	 * Sets the current sanctuary this is in (to avoid cosntant lookups)
	 * 
	 * @param sanctuary
	 */
	public void setCurrentSanctuary(@Nullable ISanctuary sanctuary);

	/**
	 * Returns the current sanctuary this is in
	 * 
	 * @return
	 */
	public Optional<ISanctuary> currentSanctuary();

}
