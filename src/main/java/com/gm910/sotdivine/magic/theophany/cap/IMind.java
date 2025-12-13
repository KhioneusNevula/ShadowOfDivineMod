package com.gm910.sotdivine.magic.theophany.cap;

import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionType;
import com.gm910.sotdivine.network.packet_types.ClientboundImpressionsUpdatePacket;
import com.gm910.sotdivine.network.packet_types.ServerboundImpressionsUpdatePacket;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * A mob's internal storage of information pertaining to theophanies and
 * experienceable magic
 */
public interface IMind {

	public static final ResourceLocation CAPABILITY_PATH = ModUtils.path("experiencer");

	public static final Capability<IMind> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
	});

	public static IMind get(ICapabilityProvider provider) {
		return provider.getCapability(CAPABILITY).orElseThrow(
				() -> new UnsupportedOperationException("Object " + provider + " does not have this capability"));
	}

	/**
	 * tick this capabbility
	 */
	public void tick();

	/**
	 * Return all impressions
	 * 
	 * @return
	 */
	public Iterable<IImpression> getAllImpressions();

	/**
	 * Return the instance-info of this impression
	 * 
	 * @param impression
	 * @return
	 */
	public ImpressionTimetracker getTimetracker(IImpression impression);

	/**
	 * Returns all impressions of the given type
	 * 
	 * @param <T>
	 * @param type
	 * @return
	 */
	public <T extends IImpression> Iterable<? super T> getImpressions(ImpressionType<T> type);

	/**
	 * receive an update package from client
	 */
	public void updateServer(ServerboundImpressionsUpdatePacket pkg);

	/**
	 * Applies a new impression and sends packet to client; if this impression
	 * already exists, overwrites it
	 * 
	 * @param impression
	 */
	public void addImpression(IImpression impression, ImpressionTimetracker instance);

	/**
	 * Removes an impression and sends packet to client
	 * 
	 * @param impression
	 */
	public void removeImpression(IImpression impression);

	/**
	 * Removes all impressions and sends packet to client
	 * 
	 * @param impression
	 */
	void clearImpressions();

	public CompoundTag serializeNBT(Provider registryAccess);

	public void deserializeNBT(Provider registryAccess, CompoundTag nbt);

	/**
	 * Sets this to be meditating
	 * 
	 * @param b
	 */
	public void setMeditating(boolean b);

	/**
	 * IF this has the ability to meditate
	 * 
	 * @return
	 */
	public boolean canMeditate();

	/**
	 * If this is meditating
	 * 
	 * @return
	 */
	public boolean isMeditating();

	/**
	 * Stops meditating and seends an update to client
	 */
	public void forceStopMeditating();

	/**
	 * Starts meditating and seends an update to client
	 */
	public void forceStartMeditating();

}
