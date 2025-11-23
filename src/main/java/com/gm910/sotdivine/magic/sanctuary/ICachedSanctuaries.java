package com.gm910.sotdivine.magic.sanctuary;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/**
 * Some readable storage of sanctuaries
 */
public interface ICachedSanctuaries extends Iterable<ISanctuary> {

	/**
	 * Return all sanctuaries in this system
	 * 
	 * @param dimension
	 * @return
	 */
	public Stream<ISanctuary> getCompleteSanctuaries();

	/**
	 * Return all sanctuaries intersecting with this chunk
	 * 
	 * @param dimension
	 * @return
	 */
	public Stream<ISanctuary> getSanctuaries(ChunkPos pos);

	/**
	 * Return true if this mobID should be allowed to pathfind to the given
	 * rawPosition; false otherwise
	 * 
	 * @param pos
	 * @param entity
	 * @return
	 */
	public boolean canStandAt(BlockPos pos, Entity entity);

	/**
	 * Return the sanctuary that contains the given position, or null if none exists
	 * 
	 * @param pos
	 * @return
	 */
	public Optional<ISanctuary> getSanctuaryAtPos(BlockPos pos);

	/**
	 * Return the sanctuary that contains the given Position, or null if none exists
	 * 
	 * @param pos
	 * @return
	 */
	public Optional<ISanctuary> getSanctuaryAtPos(Position pos);

	/**
	 * Stream all sanctuaries
	 * 
	 * @return
	 */
	public Stream<ISanctuary> stream();

	/**
	 * number of sanctuaries
	 * 
	 * @return
	 */
	public int size();

	@Override
	default Iterator<ISanctuary> iterator() {
		return stream().iterator();
	}

}
