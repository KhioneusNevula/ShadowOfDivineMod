package com.gm910.sotdivine.magic.emanation.spell;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
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
		private Optional<UUID> opCaster = Optional.empty();
		private Optional<EntityReference<Entity>> opTargetEntity = Optional.empty();
		private Optional<GlobalPos> opTargetPos = Optional.empty();

		private Builder(IDeity deity, ServerLevel level) {
			this.deity = deity;
			this.level = level;
		}

		/**
		 * To simplify some more complex constructions
		 * 
		 * @param <L>
		 * @param <R>
		 * @param from
		 * @param left
		 * @param right
		 * @return
		 */
		public <L, R> Builder branch(Either<L, R> from, BiConsumer<L, Builder> left, BiConsumer<R, Builder> right) {
			from.ifLeft((l) -> left.accept(l, this)).ifRight((r) -> right.accept(r, this));
			return this;
		}

		/**
		 * To simplify some more complex constructions
		 * 
		 * @param <L>
		 * @param <R>
		 * @param goLeft
		 * @param left
		 * @param right
		 * @return
		 */
		public Builder branch(boolean goLeft, Consumer<Builder> left, Consumer<Builder> right) {
			if (goLeft)
				left.accept(this);
			else
				right.accept(this);
			return this;
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
			if (this.level == null)
				throw new IllegalArgumentException();
			return targetPos(GlobalPos.of(level.dimension(), pos));
		}

		public Builder targetEntityAndPos(Entity target) {
			targetEntity(target);
			return targetPos(GlobalPos.of(target.level().dimension(), target.blockPosition()));
		}

		public Builder caster(UUID caster) {
			this.opCaster = Optional.of(caster);
			return this;
		}

		public Builder caster(Entity caster) {
			this.opCaster = Optional.of(caster.getUUID());
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
	 * Encodes a {@link ISpellTargetInfo} emanation, though it only decodes
	 * deficient instances
	 */
	public static final Codec<ISpellTargetInfo> DEFICIENT_CODEC = RecordCodecBuilder.create(instance -> // Given an
																										// emanation
	instance.group(GlobalPos.CODEC.optionalFieldOf("targetPos").forGetter(ISpellTargetInfo::opTargetPos),
			EntityReference.<Entity>codec().optionalFieldOf("targetEntity").forGetter(ISpellTargetInfo::opTargetEntity),
			UUIDUtil.CODEC.optionalFieldOf("caster").forGetter(ISpellTargetInfo::opCaster))
			.apply(instance, (tp, te, c) -> new SPTI(null, null, c, te, tp)));

	/**
	 * creates the most basic kind of SPELL target info
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
	 * The entity or party that casts this SPELL, if it was not directly the deity
	 * 
	 * @return
	 */
	public Optional<UUID> opCaster();

	/**
	 * The divine source of this SPELL
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
