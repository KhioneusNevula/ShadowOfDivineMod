package com.gm910.sotdivine.magic.afterlife;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.gm910.sotdivine.concepts.parties.party.resource.type.ISoulResource;
import com.gm910.sotdivine.magic.afterlife.ISoulState.LifeState;
import com.gm910.sotdivine.magic.afterlife.anchors.AfterlifeAnchorType;
import com.gm910.sotdivine.magic.afterlife.anchors.BlockAnchor;
import com.gm910.sotdivine.magic.afterlife.anchors.IAfterlifeAnchor;
import com.gm910.sotdivine.magic.afterlife.anchors.IPositionableAnchor;
import com.gm910.sotdivine.magic.afterlife.anchors.PositionAnchor;
import com.gm910.sotdivine.magic.afterlife.anchors.SoulAnchor;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IExtensibleEnum;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Capability storing souls that players killed, across the entire server
 */
public interface IAfterlife {

	public static final ResourceLocation CAPABILITY_PATH = ModUtils.path("afterlife");

	public static final Capability<IAfterlife> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
	});

	public static IAfterlife get(MinecraftServer server) {
		return server.overworld().getCapability(CAPABILITY)
				.orElseThrow(() -> new UnsupportedOperationException("world does not have this capability"));
	}

	public static IAfterlife get(ServerLevel level) {
		return (level.dimension() == Level.OVERWORLD ? level : level.getServer().overworld()).getCapability(CAPABILITY)
				.orElseThrow(() -> new UnsupportedOperationException("world does not have this capability"));
	}

	/**
	 * Returns the soul resource pertaining to the given uuid's UUID
	 * 
	 * @param ofID
	 * @return
	 */
	public Optional<ISoulResource> getSoulResource(Soul soul);

	/**
	 * Replace the soul resource reference in this with the given soul resource
	 * reference; return false if this soul is not stored at all, otherwise return
	 * the previous version
	 * 
	 * @param soul
	 * @param resource
	 * @return
	 */
	public ISoulResource changeSoulResource(Soul soul, ISoulResource resource);

	/**
	 * Returns an iterable of all souls for the given anchor
	 * 
	 * @return
	 */
	public Iterable<Soul> getAllSouls(IAfterlifeAnchor<?, ?> anchor);

	/**
	 * Number of souls anchored to this
	 * 
	 * @param anchor
	 * @return
	 */
	public int soulCount(IAfterlifeAnchor<?, ?> anchor);

	/**
	 * REturns all afterlife anchors of the given type
	 * 
	 * @param <T>
	 * @param type
	 * @return
	 */
	public <T extends IAfterlifeAnchor<?, ?>> Collection<T> getAnchorsOfType(AfterlifeAnchorType<T> type);

	/**
	 * Utility mainly to obtain anchors like {@link PositionAnchor} and
	 * {@link BlockAnchor}, which are associate to positions
	 * 
	 * @param anchor
	 */
	public Collection<IPositionableAnchor<?, ?>> getAnchors(GlobalPos position);

	/**
	 * Utility mainly to obtain {@link ItemAnchor}s, which are typically in an
	 * inventory of an entity or block at position
	 * 
	 * @param anchor
	 */
//	public Collection<IAfterlifeAnchor<?, ?>> getAnchors(EntityOrPosition connection);

	/**
	 * connect an anchor to an entity or position
	 * 
	 * @param anchor
	 * @param connection
	 */
//	public void connectAnchor(IAfterlifeAnchor<?, ?> anchor, EntityOrPosition connection);

	/**
	 * Moves everything under this old anchor to the given new anchor
	 * 
	 * @param old
	 * @param newAnchor
	 */
	public void reanchor(IAfterlifeAnchor<?, ?> old, IAfterlifeAnchor<?, ?> newAnchor);

	/**
	 * Remove this afterlife anchor and everything tied to it
	 * 
	 * @param anchor
	 * @return
	 */
	public boolean destroyAnchor(IAfterlifeAnchor<?, ?> anchor);

	/**
	 * Adds a soul anchored to the given individual; defaults to a soul of type
	 * {@link SignificanceType#VICTIM} if persistent. Use
	 * {@link #addSignificantSoul(UUID, Soul, ISoulResource, SignificanceType)} to
	 * control the type
	 * 
	 * @param anchor
	 * @param block  if this is true, uuid will not be added if there are too many
	 *               souls in this afterlife slot
	 * @return false if this soul was not added
	 */
	public boolean addSoul(IAfterlifeAnchor<?, ?> anchor, Soul soulID, ISoulResource info, boolean block);

	/**
	 * Adds a soul; forces it to be persistent of the given type
	 * 
	 * @param anchor
	 * @param soul
	 */
	public void addSignificantSoul(IAfterlifeAnchor<?, ?> anchor, Soul soulID, ISoulResource info,
			SignificanceType type);

	/**
	 * If the afterlife contains the given soul
	 * 
	 * @param anchor
	 * @param soul
	 * @return
	 */
	public boolean containsSoul(IAfterlifeAnchor<?, ?> anchor, Soul soul);

	/**
	 * Return true if this soul is present
	 * 
	 * @param soul
	 * @return
	 */
	public default boolean containsSoul(Soul soul) {
		return getSoulResource(soul).isPresent();
	}

	/**
	 * Returns the significance types of this soul
	 * 
	 * @param forAnchor
	 * @param soul
	 * @return
	 */
	public Collection<SignificanceType> getSignificance(IAfterlifeAnchor<?, ?> forAnchor, Soul soul);

	/**
	 * Returns the souls with the given significance type
	 * 
	 * @param anchor
	 * @param soul
	 * @return
	 */
	public Collection<Soul> getSignificantSouls(IAfterlifeAnchor<?, ?> anchor, SignificanceType type);

	/**
	 * All anchors containing this soul
	 * 
	 * @param soul
	 * @return
	 */
	public Collection<IAfterlifeAnchor<?, ?>> ownersOfSoul(Soul soul);

	public void removeSoul(IAfterlifeAnchor<?, ?> anchor, Soul soul);

	/**
	 * Removes the given significance type from this soul; might remove the soul
	 * entirely if no significances are left
	 * 
	 * @param anchor
	 * @param soul
	 * @param type
	 */
	public void removeSignificance(IAfterlifeAnchor<?, ?> anchor, Soul soul, SignificanceType type);

	/**
	 * Remove this soul from all anchors containign it
	 * 
	 * @param soul
	 */
	public ISoulResource removeSoul(Soul soul);

	/**
	 * Return if this soul is stored persistently
	 * 
	 * @param anchor
	 * @param soul
	 * @return
	 */
	public boolean isStoredPersistently(IAfterlifeAnchor<?, ?> anchor, Soul soul);

	/**
	 * peeks a random significant soul
	 * 
	 * @return
	 */
	public Optional<Soul> peekSignificantSoul(IAfterlifeAnchor<?, ?> anchor);

	/**
	 * peeks newest non-persistent soul
	 * 
	 * @return
	 */
	public Optional<Soul> peekNonsignificantSoul(IAfterlifeAnchor<?, ?> anchor);

	/**
	 * Souls that are named or require persistence are distinguished (since they are
	 * not replaced)
	 * 
	 * @return
	 */
	public Iterable<Soul> significantSouls(IAfterlifeAnchor<?, ?> anchor);

	/**
	 * Souls that are unnamed or don't require persistence are distinguished
	 * 
	 * @return
	 */
	public Iterable<Soul> nonSignificantSouls(IAfterlifeAnchor<?, ?> anchor);

	/**
	 * Create instance of this entity and delete internal record of it
	 * 
	 * @param soul
	 * @return
	 */
	public Optional<Entity> extractEntity(Soul soul, ServerLevel in, LifeState inState);

	public static enum SignificanceType implements IExtensibleEnum {
		/** Soul of something killed */
		VICTIM,
		/** Soul of a pet or other friendly being */
		FRIEND,
		/** A tether formed by magically tying a spirit to something */
		ENCHANTED, OTHER;

		public static SignificanceType create(String name) {
			throw new IllegalStateException();
		}
	}

}
