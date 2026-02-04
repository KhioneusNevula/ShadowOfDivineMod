package com.gm910.sotdivine.magic.afterlife;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.concepts.parties.party.resource.type.ISoulResource;
import com.gm910.sotdivine.magic.afterlife.IAfterlife.SignificanceType;
import com.gm910.sotdivine.magic.afterlife.anchors.IAfterlifeAnchor;
import com.gm910.sotdivine.mixins_assist.ai.IDisableable;
import com.gm910.sotdivine.util.CodecUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public non-sealed class SoulState implements ISoulState, ICapabilitySerializable<CompoundTag> {

	private final LazyOptional<ISoulState> cached = LazyOptional.of(() -> this);

	/**
	 * The world (overworld) with this capability
	 */
	private LivingEntity entity;

	private final Soul selfSoul;

	private float possessionStrength;

	private Soul possessor;
	private Soul possessee;

	private LifeState state;
	private CompoundTag extra = new CompoundTag();

	private Multimap<SignificanceType, IAfterlifeAnchor<?, ?>> anchors = MultimapBuilder
			.enumKeys(SignificanceType.class).hashSetValues().build();

	private boolean persistent = false;

	private Optional<ISoulResource> priorSoulState = Optional.empty();

	private static final Codec<Multimap<SignificanceType, IAfterlifeAnchor<?, ?>>> MCODEC = CodecUtils.multimapCodec(
			CodecUtils.caselessEnumCodec(SignificanceType.class), IAfterlifeAnchor.codec(),
			() -> MultimapBuilder.enumKeys(SignificanceType.class).hashSetValues().build());

	public SoulState(LivingEntity entity) {
		this.entity = entity;
		state = LifeState.getDefaultState(entity);
		this.possessionStrength = ISoulState.getDefaultPossessionStrength(entity);
		this.selfSoul = Soul.createSoulFromEntityUnsafely(entity);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ISoulState.CAPABILITY) {
			return cached.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public void tick() {
		this.state.onTickState(this, entity);
		ServerLevel level1 = (ServerLevel) entity.level();
		if (isPossessingOther()
				&& getPossessee().getExistingEntity(level1).orElse(null) instanceof LivingEntity possessee) {
			if (getLifeState().inhabitsPossessee()) {
				if (!extra.contains("invisibilityBeforePossession")) {
					extra.putBoolean("invisibilityBeforePossession", entity.isInvisible());
				}
				entity.setInvisible(true);
				possessee.setYHeadRot(entity.getYHeadRot());
				possessee.setXRot(entity.getXRot());
				possessee.setYRot(entity.getYRot());
				entity.copyPosition(possessee);
			}
			if (possessee instanceof Mob posseeMob) {
				if (possessionStrength > ISoulState.get(possessee).getPossessionStrength()) {
					posseeMob.getMoveControl().setWantedPosition(posseeMob.getX(), posseeMob.getY(), posseeMob.getZ(),
							0);
					((IDisableable) posseeMob.targetSelector).setDisabled(true);
					((IDisableable) posseeMob.goalSelector).setDisabled(true);
				} else {
					((IDisableable) posseeMob.targetSelector).setDisabled(false);
					((IDisableable) posseeMob.goalSelector).setDisabled(false);
				}
			}
		} else {
			if (extra.contains("invisibilityBeforePossession")) {
				entity.setInvisible(extra.getBooleanOr("invisibilityBeforePossession", false));
				extra.remove("invisibilityBeforePossession");
			}
		}
	}

	@Override
	public LivingEntity self() {
		return entity;
	}

	@Override
	public void reset() {
		this.changeState(LifeState.getDefaultState(entity));
		this.anchors.clear();
		this.extra = new CompoundTag();
		this.removePossessor();
		this.removePossessee();

	}

	@Override
	public void deserializeNBT(Provider registryAccess, CompoundTag nbt) {
		this.state = nbt.read("state", CodecUtils.caselessEnumCodec(LifeState.class)).orElse(state);
		this.extra = nbt.getCompound("extra").orElse(this.extra).copy();
		this.anchors = nbt.read("anchors", MCODEC).orElse(anchors);
		this.persistent = nbt.getBooleanOr("persistent", false);
		this.priorSoulState = nbt.read("prior_body_data", PartyResourceType.SOUL.get().codec().codec());
		this.possessionStrength = nbt.getFloatOr("strength", this.possessionStrength);
		this.possessee = nbt.read("possessee", Soul.CODEC).orElse(null);
		if (possessee != null
				&& this.possessee.getExistingEntity(entity.getServer()).orElse(null) instanceof LivingEntity pose
				&& ISoulState.get(pose).getPossessor() instanceof Soul posor && posor.matches(entity)) {
			this.possess(pose);
		} else {
			this.removePossessee();
		}
	}

	@Override
	public CompoundTag serializeNBT(Provider registryAccess) {
		CompoundTag tag = new CompoundTag();
		tag.store("state", CodecUtils.caselessEnumCodec(LifeState.class), state);
		tag.put("extra", this.extra);
		if (!anchors.isEmpty()) {
			tag.store("anchors", MCODEC, this.anchors);
		}
		tag.putBoolean("persistent", persistent);
		tag.storeNullable("prior_body_data", PartyResourceType.SOUL.get().codec().codec(),
				this.priorSoulState.orElse(null));
		tag.putFloat("strength", possessionStrength);
		tag.storeNullable("possessee", Soul.CODEC, this.possessee);
		return tag;
	}

	@Override
	public LifeState getLifeState() {
		return state;
	}

	@Override
	public void changeState(LifeState state) {
		if (state != this.state) {
			LogUtils.getLogger().debug("Changing state of " + this.entity + " from " + this.state + " to " + state);
			this.state.onRemovedState(this, entity, state);
			state.onAddedState(this, entity, this.state);
			this.state = state;
		}
	}

	@Override
	public void setStateUnsafely(LifeState state) {
		this.state = state;
	}

	@Override
	public CompoundTag extraData() {
		return extra;
	}

	@Override
	public boolean hasAnyAnchors() {
		return !this.anchors.isEmpty();
	}

	@Override
	public void addAnchor(IAfterlifeAnchor<?, ?> owner, SignificanceType type) {
		this.anchors.put(type, owner);
	}

	@Override
	public Collection<IAfterlifeAnchor<?, ?>> getAnchors(SignificanceType type) {
		return anchors.get(type);
	}

	@Override
	public Collection<SignificanceType> getAnchorSignifiances(IAfterlifeAnchor<?, ?> owner) {
		return anchors.keySet().stream().filter(s -> anchors.containsEntry(s, owner)).collect(Collectors.toSet());
	}

	@Override
	public boolean hasAnchor(IAfterlifeAnchor<?, ?> owner) {
		return anchors.keySet().stream().anyMatch(ks -> anchors.containsEntry(ks, owner));
	}

	@Override
	public boolean hasAnchors(SignificanceType type) {
		return !anchors.get(type).isEmpty();
	}

	@Override
	public void removeAnchor(IAfterlifeAnchor<?, ?> owner) {
		anchors.keySet().stream().collect(Collectors.toSet()).forEach(st -> {
			anchors.get(st).remove(owner);
		});
	}

	@Override
	public boolean isPersistent() {
		return this.persistent;
	}

	@Override
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	@Override
	public Optional<ISoulResource> getPriorSoulState() {
		return priorSoulState;
	}

	@Override
	public void setPriorSoulState(ISoulResource soulState) {
		priorSoulState = Optional.ofNullable(soulState);
	}

	@Override
	public float getPossessionStrength() {
		return possessionStrength; // TODO vary possession strength
	}

	@Override
	public Soul getPossessor() {
		if (this.possessor != null) {
			if (possessor.getExistingEntity(entity.getServer()).orElse(null) instanceof LivingEntity pos
					&& ((SoulState) ISoulState.get(pos)).possessee instanceof Soul otherpossee
					&& otherpossee.matches(entity)) {
				return possessor;
			} else {
				return (this.possessor = null);
			}
		}
		return null;
	}

	@Override
	public Soul getPossessee() {
		if (this.possessee != null) {
			if (possessee.getExistingEntity(entity.getServer()).orElse(null) instanceof LivingEntity pos
					&& ((SoulState) ISoulState.get(pos)).possessor instanceof Soul otherpossor
					&& otherpossor.matches(entity)) {
				return possessee;
			} else {
				return (this.possessee = null);
			}
		}
		return null;
	}

	@Override
	public boolean isPossessed() {
		return this.getPossessor() != null;
	}

	@Override
	public boolean isPossessingOther() {
		return this.getPossessee() != null;
	}

	@Override
	public Soul possess(LivingEntity other) {
		if (other.getUUID().equals(this.entity.getUUID())) {
			throw new IllegalArgumentException("Cannot be same entity");
		}
		SoulState otherState = (SoulState) ISoulState.get(other);
		Soul previous = otherState.getPossessor();
		otherState.removePossessor();
		this.removePossessee();
		otherState.possessor = selfSoul;
		this.possessee = otherState.selfSoul;
		LivingEntity topLevel = this.entity;
		LivingEntity bottomLevel = this.entity;
		while (topLevel != null || bottomLevel != null) {
			if (topLevel != null && ISoulState.get(topLevel).getPossessor() instanceof Soul er) {
				if (er.getExistingEntity(entity.getServer()).orElse(null) instanceof LivingEntity le) {
					if (ISoulState.get(le).getPossessor() instanceof Soul nextE && nextE.matches(entity)) {
						this.possessor = null;
						throw new IllegalArgumentException(
								"Cycle found; " + le + " is both a possessor in the hierarchy above " + entity
										+ " and a possessee below it");
					}
					topLevel = le;
				} else {
					topLevel = null;
				}
			} else {
				topLevel = null;
			}
			if (bottomLevel != null && ISoulState.get(bottomLevel).getPossessee() instanceof Soul er) {
				if (er.getExistingEntity(entity.getServer()).orElse(null) instanceof LivingEntity le) {
					if (ISoulState.get(le).getPossessee() instanceof Soul nextE && nextE.matches(entity)) {
						this.possessee = null;
						throw new IllegalArgumentException(
								"Cycle found; " + le + " is both a possessee in the hierarchy below " + entity
										+ " and a possessor above it");
					}
					bottomLevel = le;
				} else {
					bottomLevel = null;
				}
			} else {
				bottomLevel = null;
			}
		}
		return previous;
	}

	@Override
	public Soul removePossessee() {
		if (this.possessee != null) {
			var poss = possessee;
			this.possessee = null;
			poss.<LivingEntity>getExistingEntity(entity.getServer()).ifPresent(previousPossessee -> {
				SoulState stateOfPrev = ((SoulState) ISoulState.get(previousPossessee));
				if (stateOfPrev.getPossessor() instanceof Soul otherprevpossor && otherprevpossor.matches(self())) {
					stateOfPrev.removePossessor();
				}
			});

			return poss;
		} else {
			return null;
		}
	}

	@Override
	public Soul removePossessor() {
		if (this.possessor != null) {
			var poss = possessor;
			this.possessor = null;
			if (entity instanceof Mob thisMob) {
				((IDisableable) thisMob.targetSelector).setDisabled(false);
				((IDisableable) thisMob.goalSelector).setDisabled(false);
			}
			poss.<LivingEntity>getExistingEntity(entity.getServer()).ifPresent(previousPossessor -> {
				SoulState stateOfPrev = ((SoulState) ISoulState.get(previousPossessor));
				if (stateOfPrev.getPossessee() instanceof Soul otherprevpossee && otherprevpossee.matches(self())) {
					stateOfPrev.removePossessee();
				}
			});
			return poss;
		} else {
			return null;
		}
	}

	@Override
	public ISoulResource deleteAndEnsoul() {
		IAfterlife afterlife = IAfterlife.get((ServerLevel) entity.level());

		getPriorSoulState().ifPresent(ps -> {
			ISoulResource[] user = { ps };
			entity.getBrain().serializeStart(NbtOps.INSTANCE).ifSuccess(tg -> user[0] = ps.copy().replaceBrainTag(tg));
			user[0].applyToEntity(entity);
		});
		setPriorSoulState(null);
		ISoulResource soulData = ISoulResource.create(entity);
		entity.remove(RemovalReason.DISCARDED);
		Soul soul = Soul.createSoulFromEntityUnsafely(entity);
		for (var entry : this.anchors.entries()) {
			if (this.persistent) {
				afterlife.addSignificantSoul(entry.getValue(), soul, soulData, entry.getKey());
			} else {
				afterlife.addSoul(entry.getValue(), soul, soulData, true);
			}
		}

		LogUtils.getLogger().debug("Removing soul of " + this.entity + " and transforming it into soul: " + soulData);
		return soulData;
	}

}
