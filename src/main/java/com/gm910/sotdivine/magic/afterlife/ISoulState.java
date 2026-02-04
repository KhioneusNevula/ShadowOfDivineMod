package com.gm910.sotdivine.magic.afterlife;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nullable;

import com.gm910.sotdivine.common.effects.ModEffects;
import com.gm910.sotdivine.common.entities.ModEntityTags;
import com.gm910.sotdivine.concepts.parties.party.resource.type.ISoulResource;
import com.gm910.sotdivine.magic.afterlife.IAfterlife.SignificanceType;
import com.gm910.sotdivine.magic.afterlife.anchors.IAfterlifeAnchor;
import com.gm910.sotdivine.mixins_assist.entity.IUndeadable;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Capability storing souls that players killed, across the entire server
 */
public sealed interface ISoulState permits SoulState {

	public static final ResourceLocation CAPABILITY_PATH = ModUtils.path("soul_state");

	public static final Capability<ISoulState> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
	});

	public static ISoulState get(LivingEntity entity) {
		return entity.getCapability(CAPABILITY).orElseThrow(
				() -> new UnsupportedOperationException("uuid " + entity + " does not have this capability"));
	}

	public void tick();

	/**
	 * How powerful this is when possessing something else; if this number is
	 * greater than that of what it is possessing, then the intention of this will
	 * override the intention of what it possesses, and vice versa. If both are
	 * equal, they both control the body
	 * 
	 * @return
	 */
	public float getPossessionStrength();

	/**
	 * The thing this is possessing (may be null)
	 * 
	 * @return
	 */
	public Soul getPossessor();

	/**
	 * The thing possessed by this (may be null)
	 * 
	 * @return
	 */
	public Soul getPossessee();

	/**
	 * Return true if something is possessing this
	 * 
	 * @return
	 */
	public boolean isPossessed();

	/**
	 * Return true if this is possessing something else
	 * 
	 * @return
	 */
	public boolean isPossessingOther();

	/**
	 * possess something; return the previous possessor of the target
	 * 
	 * @return
	 */
	public Soul possess(LivingEntity other);

	/**
	 * Return the entity possessing this
	 * 
	 * @return
	 */
	public Soul removePossessor();

	/**
	 * Return that which was possessed
	 * 
	 * @return
	 */
	public Soul removePossessee();

	/**
	 * REturn the entity
	 * 
	 * @return
	 */
	public LivingEntity self();

	/**
	 * Resets all info
	 */
	public void reset();

	/**
	 * Return the state of this being
	 * 
	 * @return
	 */
	public LifeState getLifeState();

	/**
	 * Change the state of this being, updating its traits
	 * 
	 * @param state
	 */
	public void changeState(LifeState state);

	/**
	 * Changes the state without updating its traits
	 * 
	 * @param state
	 */
	public void setStateUnsafely(LifeState state);

	/**
	 * Sets the prior state of this soul, if it is relevant to
	 * {@link LifeState#doesStateNotAffectResurrection()}
	 * 
	 * @param soulState
	 */
	public void setPriorSoulState(@Nullable ISoulResource soulState);

	/**
	 * Gets the priorSoul state
	 * 
	 * @return
	 */
	public Optional<ISoulResource> getPriorSoulState();

	/**
	 * Return a mutable compound tag to store extra data
	 * 
	 * @return
	 */
	public CompoundTag extraData();

	/**
	 * Return the beings which have the given significance relationship with this
	 * 
	 * @param type
	 * @return
	 */
	public Collection<IAfterlifeAnchor<?, ?>> getAnchors(SignificanceType type);

	/**
	 * If this entity has anchors of any kind
	 * 
	 * @return
	 */
	public boolean hasAnyAnchors();

	/**
	 * Adds an anchor with the given significance relation
	 * 
	 * @param owner
	 * @param type
	 */
	public void addAnchor(IAfterlifeAnchor<?, ?> owner, SignificanceType type);

	/**
	 * Remove the given owner
	 * 
	 * @param owner
	 */
	public void removeAnchor(IAfterlifeAnchor<?, ?> owner);

	/**
	 * Return the signifiance types of the given owner
	 * 
	 * @param owner
	 * @return
	 */
	public Collection<SignificanceType> getAnchorSignifiances(IAfterlifeAnchor<?, ?> owner);

	/**
	 * If this has the given owner
	 * 
	 * @param owner
	 * @return
	 */
	public boolean hasAnchor(IAfterlifeAnchor<?, ?> owner);

	/**
	 * Whether this has any owners of the specific relationship
	 * 
	 * @param type
	 * @return
	 */
	public boolean hasAnchors(SignificanceType type);

	/**
	 * If this ghost should be registered as being persistent
	 */
	public boolean isPersistent();

	/**
	 * Make this into a persistent entity
	 * 
	 * @return
	 */
	public void setPersistent(boolean persistent);

	/**
	 * Marks this uuid as REMOVED and converts it into a soul resource in the
	 * Afterlife; returns the soul resource
	 * 
	 * @return
	 */
	public ISoulResource deleteAndEnsoul();

	public static enum LifeState {
		/** Default state; this uuid is alive */
		living {
			@Override
			public void onAddedState(ISoulState status, LivingEntity toEntity, LifeState oldState) {
				toEntity.setHealth(toEntity.getMaxHealth());
			}
		},
		/** Entities are set to this when they die (i.e. are removed) */
		dead {
			@Override
			public void onAddedState(ISoulState status, LivingEntity toEntity, LifeState oldState) {
				if (toEntity.getHealth() > 0) {
					toEntity.kill((ServerLevel) toEntity.level());
				}
				status.removePossessee();
				status.removePossessor();
			}

			@Override
			public void onRemovedState(ISoulState status, LivingEntity fromEntity, LifeState newState) {
				if (fromEntity.getHealth() <= 0) {
					fromEntity.heal(1);
				}

			}
		},
		/**
		 * Alternate state; default state for undead mobs; this uuid is not alive, but
		 * not dead
		 */
		undead {
			@Override
			public void onAddedState(ISoulState status, LivingEntity toEntity, LifeState oldState) {
				if (toEntity instanceof Mob mob && mob.getNavigation() instanceof GroundPathNavigation gpn) {
					gpn.setAvoidSun(true);
				}

			}

			@Override
			public void onTickState(ISoulState status, LivingEntity fromEntity) {
				if (!fromEntity.getType().is(EntityTypeTags.UNDEAD)) {
					if (fromEntity.isAlive() && ((IUndeadable) fromEntity).$isSunBurnTick()) {
						fromEntity.igniteForSeconds(8.0F);
					}
				}
			}

			@Override
			public void onRemovedState(ISoulState status, LivingEntity fromEntity, LifeState newState) {
				if (fromEntity instanceof Mob mob && mob.getNavigation() instanceof GroundPathNavigation gpn) {
					gpn.setAvoidSun(false);
				}

			}
		},
		/**
		 * This uuid is a ghost, not alive or dead and unable to die due to damage
		 * (i.e., it temporarily fades from existence and then reforms)
		 */
		ghost {
			@Override
			public void onAddedState(ISoulState status, LivingEntity toEntity, LifeState oldState) {
				status.setPriorSoulState(ISoulResource.create(toEntity));
				status.extraData().putBoolean("wasGlowingBeforeGhost", toEntity.isCurrentlyGlowing());
				status.extraData().putBoolean("wasNoGravityBeforeGhost", toEntity.isNoGravity());
				status.extraData().putBoolean("wasNoPhysicsBeforeGhost", toEntity.noPhysics);
				toEntity.setHealth(toEntity.getMaxHealth());
			}

			@Override
			public void onTickState(ISoulState status, LivingEntity fromEntity) {
				fromEntity.setGlowingTag(true);
				fromEntity.setNoGravity(true);
				fromEntity.noPhysics = true;
			}

			@Override
			public void onRemovedState(ISoulState status, LivingEntity fromEntity, LifeState newState) {
				fromEntity.removeEffect(ModEffects.INTANGIBLE.getHolder().get());
				fromEntity.setGlowingTag(status.extraData().getBooleanOr("wasGlowingBeforeGhost", false));
				status.extraData().remove("wasGlowingBeforeGhost");
				fromEntity.setNoGravity(status.extraData().getBooleanOr("wasNoGravityBeforeGhost", false));
				status.extraData().remove("wasNoGravityBeforeGhost");
				fromEntity.noPhysics = status.extraData().getBooleanOr("wasNoPhysicsBeforeGhost", false);
				status.extraData().remove("wasNoPhysicsBeforeGhost");
				status.getPriorSoulState().ifPresent(ps -> {
					ISoulResource[] user = { ps };
					fromEntity.getBrain().serializeStart(NbtOps.INSTANCE)
							.ifSuccess(tg -> user[0] = ps.copy().replaceBrainTag(tg));
					user[0].applyToEntity(fromEntity);
				});
				status.setPriorSoulState(null);
			}
		},
		/** For armor stands and the like, which are not alive */
		nonliving {
			@Override
			public void onAddedState(ISoulState status, LivingEntity toEntity, LifeState oldState) {
				if (toEntity instanceof Mob mob) {
					status.extraData().putBoolean("wasNoAiBeforeNonliving", mob.isNoAi());
				}
			}

			@Override
			public void onTickState(ISoulState status, LivingEntity fromEntity) {
				if (fromEntity instanceof Mob mob) {
					mob.setNoAi(true);
				}
				fromEntity.setHealth(1);
			}

			@Override
			public void onRemovedState(ISoulState status, LivingEntity fromEntity, LifeState newState) {
				if (fromEntity instanceof Mob mob) {
					mob.setNoAi(status.extraData().getBooleanOr("wasNoAiBeforeNonliving", false));
					status.extraData().remove("wasNoAiBeforeNonliving");
				}
				fromEntity.setHealth(fromEntity.getMaxHealth());
			}
		},
		/** For deities and similar beings which are not confined to death and life */
		eternal {
			@Override
			public void onAddedState(ISoulState status, LivingEntity toEntity, LifeState oldState) {
				status.extraData().putBoolean("wasInvulnerableBeforeEternal", toEntity.isInvulnerable());
				status.extraData().putBoolean("hadNoPhysicsBeforeEternal", toEntity.noPhysics);
				status.extraData().putBoolean("hadNoGravityBeforeEternal", toEntity.isNoGravity());
				if (toEntity instanceof ServerPlayer player) {
					status.extraData().store("gameModeBeforeEternal", CodecUtils.caselessEnumCodec(GameType.class),
							player.gameMode());
				}
			}

			@Override
			public void onTickState(ISoulState status, LivingEntity fromEntity) {
				fromEntity.setInvulnerable(true);
				fromEntity.noPhysics = true;
				fromEntity.setNoGravity(true);
				if (fromEntity instanceof ServerPlayer player) {
					player.setGameMode(GameType.CREATIVE);

				}
			}

			@Override
			public void onRemovedState(ISoulState status, LivingEntity fromEntity, LifeState newState) {
				fromEntity.setInvulnerable(status.extraData().getBooleanOr("wasInvulnerableBeforeEternal", false));
				status.extraData().remove("wasInvulnerableBeforeEternal");
				fromEntity.noPhysics = status.extraData().getBooleanOr("hadNoPhysicsBeforeEternal", false);
				status.extraData().remove("hadNoPhysicsBeforeEternal");
				fromEntity.setNoGravity(status.extraData().getBooleanOr("hadNoGravityBeforeEternal", false));
				status.extraData().remove("hadNoGravityBeforeEternal");
				if (fromEntity instanceof ServerPlayer player) {
					status.extraData().read("gameModeBeforeEternal", CodecUtils.caselessEnumCodec(GameType.class))
							.ifPresent(type -> player.setGameMode(type));
					status.extraData().remove("gameModeBeforeEternal");
					;
				}
			}
		};

		/**
		 * {@link #living}
		 * 
		 * @return
		 */
		public boolean isLiving() {
			return this == living;
		}

		/**
		 * {@link #undead} / {@link #ghost}
		 * 
		 * @return
		 */
		public boolean isBeyondDeath() {
			return this == undead || this == ghost;
		}

		/**
		 * For states that are "managed" by afterlife-managers, i.e. {@link #ghost}
		 * 
		 * @return
		 */
		public boolean isSpirit() {
			return this == ghost;
		}

		/**
		 * States that inhabit what they possess
		 * 
		 * @return
		 */
		public boolean inhabitsPossessee() {
			return this == ghost || this == eternal;
		}

		/**
		 * If this can be killed via damage, i.e. {@link #living} or {@link #undead}
		 * 
		 * @return
		 */
		public boolean canBeKilled() {
			return this == living || this == undead;
		}

		/**
		 * {@link #nonliving}
		 * 
		 * @return
		 */
		public boolean isNonliving() {
			return this == nonliving;
		}

		/**
		 * If this being should revert to its original state when resurrected (aside
		 * from its brain)
		 * 
		 * @return
		 */
		public boolean doesStateNotAffectResurrection() {
			return this == ghost;
		}

		/**
		 * If this being is undead, i.e. burns in sun, susceptible to smite, etc
		 * 
		 * @return
		 */
		public boolean isUndead() {
			return this == undead;
		}

		public void onAddedState(ISoulState status, LivingEntity toEntity, LifeState oldState) {

		}

		public void onRemovedState(ISoulState status, LivingEntity fromEntity, LifeState newState) {

		}

		public void onTickState(ISoulState status, LivingEntity fromEntity) {

		}

		public static ISoulState.LifeState getDefaultState(LivingEntity en) {
			if (en instanceof Player) {
				return living;
			} else if (en instanceof Mob mob) {
				if (mob.getType().is(EntityTypeTags.UNDEAD)) {
					return undead;
				}
				return living;
			} else if (en instanceof LivingEntity le) {
				if (le.isDeadOrDying()) {
					return dead;
				}
			}
			return nonliving;
		}

	}

	public static float getDefaultPossessionStrength(LivingEntity en) {
		return (en instanceof Player ? 100 : 0) + (en instanceof Enemy ? 20 : 0)
				+ (en instanceof AbstractIllager || en instanceof Witch ? 50 : 0)
				+ (en instanceof SpellcasterIllager ? 40 : 0) + (en instanceof Npc ? 60 : 0) * 10 + 10;
	}

	/**
	 * Return if this entity is persistent by default (ignoring soul state and
	 * current 'world state')
	 * 
	 * @param en
	 * @return
	 */
	public static boolean isPersistentByDefault(LivingEntity en) {
		return en.getType().is(ModEntityTags.NATURALLY_PERSISTENT)
				|| (en instanceof Mob mob
						? (mob.isPersistenceRequired() || mob.requiresCustomPersistence()
								|| !mob.removeWhenFarAway(Integer.MAX_VALUE))
						: true);
	}

	/**
	 * Return if this entity is persistent (also checks its soul state)
	 * 
	 * @param en
	 * @return
	 */
	public static boolean isPersistent(LivingEntity en) {
		return ISoulState.get(en).isPersistent() || isPersistentByDefault(en);
	}

}
