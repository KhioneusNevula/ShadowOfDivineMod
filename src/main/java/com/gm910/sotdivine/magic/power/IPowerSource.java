package com.gm910.sotdivine.magic.power;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.magic.impression.spell.ISpellEffect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Something which can be channeled to grant a power
 */
public interface IPowerSource {

	/**
	 * Obtain the magical effect that this power uses; return null if this is not a
	 * possible operation
	 * 
	 * @param pos what position we are invoking from ( by default, null )
	 */
	public ISpellEffect invoke(ServerLevel level, Entity invoker, IDeity conduit, @Nullable BlockPos pos);
}
