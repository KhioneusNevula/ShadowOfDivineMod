package com.gm910.sotdivine.systems.deity.emanation.spell;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.gm910.sotdivine.systems.deity.type.IDeity;
import com.gm910.sotdivine.systems.party_system.IPartySystem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;

/**
 * A piece of info passed to an emanation to allow it to target
 * 
 * @author borah
 *
 */
public interface ISpellTargetInfo {

	public static class Builder {
		private IDeity deity;
		private ServerLevel level;
		private Optional<EntityReference<Entity>> opCaster = Optional.empty();
		private Optional<EntityReference<Entity>> opTargetEntity = Optional.empty();
		private Optional<GlobalPos> opTargetPos = Optional.empty();

		private Builder(IDeity deity, ServerLevel level) {
			this.deity = deity;
			this.level = level;
		}

		public Builder targetEntity(UUID target) {
			this.opTargetEntity = Optional.of(new EntityReference<>(target));
			return this;
		}

		public Builder targetEntity(Entity target) {
			this.opTargetEntity = Optional.of(new EntityReference<>(target));
			return this;
		}

		public Builder targetPos(GlobalPos pos) {
			this.opTargetPos = Optional.of(pos);
			return this;
		}

		public Builder targetPos(BlockPos pos) {
			return targetPos(GlobalPos.of(level.dimension(), pos));
		}

		public Builder targetEntityAndPos(Entity target) {
			targetEntity(target);
			return targetPos(GlobalPos.of(target.level().dimension(), target.blockPosition()));
		}

		public Builder caster(UUID caster) {
			this.opCaster = Optional.of(new EntityReference<>(caster));
			return this;
		}

		public Builder caster(Entity caster) {
			this.opCaster = Optional.of(new EntityReference<>(caster));
			return this;
		}

		public ISpellTargetInfo build() {
			return new SPTI(deity, level, opCaster, opTargetEntity, opTargetPos);
		}

		public static Builder builder(IDeity deity, ServerLevel level) {
			return new Builder(deity, level);
		}
	}

	/**
	 * creates the most basic kind of spell target info
	 * 
	 * @param deity
	 * @param level
	 * @return
	 */
	public static ISpellTargetInfo createBasic(IDeity deity, ServerLevel level) {
		return new SPTI(deity, level, Optional.empty(), Optional.empty(), Optional.empty());
	}

	/**
	 * To build a Spell target info
	 * 
	 * @param deity
	 * @param level
	 * @return
	 */
	public static Builder builder(IDeity deity, ServerLevel level) {
		return new Builder(deity, level);
	}

	/**
	 * To build a Spell target info without deity/world info
	 * 
	 * @param deity
	 * @param level
	 * @return
	 */
	public static Builder builder() {
		return new Builder(null, null);
	}

	/**
	 * Whether this is missing world & deity info
	 * 
	 * @return
	 */
	public default boolean isDeficient() {
		return this.deity() == null || this.level() == null;
	}

	/**
	 * Creates a copy (or this, if it is equivalent) with deficient info filled in
	 * 
	 * @param deity
	 * @param level
	 * @return
	 */
	public default ISpellTargetInfo complete(@Nullable IDeity deity, @Nullable ServerLevel level) {
		if (this.deity() == deity && this.level() == level) {
			return this;
		}
		if (deity == null)
			deity = this.deity();
		if (level == null)
			level = this.level();
		return new SPTI(deity, level, this.opCaster(), this.opTargetEntity(), this.opTargetPos());
	}

	/**
	 * Makes a deficient version of this
	 * 
	 * @return
	 */
	public default ISpellTargetInfo makeIncomplete() {
		return new SPTI(null, null, this.opCaster(), this.opTargetEntity(), this.opTargetPos());
	}

	/**
	 * The entity that casts this spell, if it was not directly the deity
	 * 
	 * @return
	 */
	public Optional<EntityReference<Entity>> opCaster();

	/**
	 * The divine source of this spell
	 * 
	 * @return
	 */
	public IDeity deity();

	/**
	 * Returns the world of origin
	 * 
	 * @return
	 */
	public ServerLevel level();

	/**
	 * Return the party system
	 * 
	 * @return
	 */
	public default IPartySystem system() {
		return IPartySystem.get(level());
	}

	/**
	 * Update the party system with any information changed
	 */
	public default void updatePartySystem() {
		this.system().markDirty(level());
	}

	/**
	 * The entity at the target location, i.e. the entity who is being
	 * punished/enchanted, etc
	 * 
	 * @return
	 */
	public Optional<EntityReference<Entity>> opTargetEntity();

	/**
	 * The position at the target location
	 * 
	 * @return
	 */
	public Optional<GlobalPos> opTargetPos();

}
