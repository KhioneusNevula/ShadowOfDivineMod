package com.gm910.sotdivine.systems.deity.emanation;

import javax.annotation.Nullable;

import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.systems.deity.type.IDeity;

import net.minecraft.server.level.ServerLevel;

/**
 * An emanation instance. Emanation instances are considered identical if they
 * have the same emanation and spell properties. Otherwise, they are not.
 */
public class EmanationInstance {

	public final IEmanation emanation;
	private ISpellTargetInfo spellTarget;
	private int ticks;
	/**
	 * Extra bit of data (not used in equals/hashcode) to store additional info
	 * while executing
	 */
	public Object extraData;

	public EmanationInstance(IEmanation emanation, ISpellTargetInfo target) {
		this(emanation, target, 0);
	}

	public EmanationInstance(IEmanation emanation, ISpellTargetInfo target, int ticks) {
		this.emanation = emanation;
		this.spellTarget = target;
		this.ticks = ticks;
	}

	public int getTicks() {
		return ticks;
	}

	public void incrementTicks() {
		ticks++;
	}

	public void ticksToZero() {
		ticks = 0;
	}

	public ISpellTargetInfo targetInfo() {
		return spellTarget;
	}

	/**
	 * Equivalent to {@link ISpellTargetInfo#complete(IDeity, ServerLevel)}; returns
	 * this emanation, NOT a copy
	 * 
	 * @param deity
	 * @param level
	 */
	public EmanationInstance completeSelf(@Nullable IDeity deity, @Nullable ServerLevel level) {
		spellTarget = spellTarget.complete(deity, level);
		return this;
	}

	@Override
	public String toString() {
		return "EmanationInstance(\"" + emanation + "\"){targetInfo=" + spellTarget + ",ticks=" + ticks
				+ (extraData != null ? ",extraData=" + extraData : "") + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof EmanationInstance em) {
			return this.emanation.equals(em.emanation) && this.spellTarget.equals(em.spellTarget);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return emanation.hashCode() + spellTarget.hashCode();
	}

}
