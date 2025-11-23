package com.gm910.sotdivine.magic.sanctuary;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

public class CachedSanctuaries implements ICachedSanctuaries {

	private Multimap<ISanctuary, ChunkPos> sanctuariesToChunks = MultimapBuilder.hashKeys().hashSetValues().build();
	private Multimap<ChunkPos, ISanctuary> chunksToSanctuaries = MultimapBuilder.hashKeys().hashSetValues().build();

	public CachedSanctuaries(Iterable<ISanctuary> sancts) {
		sancts.forEach((s) -> {
			this.addCompleteSanctuary(s);
		});
	}

	private void addCompleteSanctuary(ISanctuary newSanct) {
		Supplier<IllegalArgumentException> exsup = () -> new IllegalArgumentException(
				"Empty sanctuary cached " + newSanct);
		int mincx = newSanct.boundaryPositions().stream().map((b) -> new ChunkPos(b)).mapToInt((c) -> c.x).min()
				.orElseThrow(exsup);
		int mincz = newSanct.boundaryPositions().stream().map((b) -> new ChunkPos(b)).mapToInt((c) -> c.z).min()
				.orElseThrow(exsup);
		int maxcx = newSanct.boundaryPositions().stream().map((b) -> new ChunkPos(b)).mapToInt((c) -> c.x).max()
				.orElseThrow(exsup);
		int maxcz = newSanct.boundaryPositions().stream().map((b) -> new ChunkPos(b)).mapToInt((c) -> c.z).max()
				.orElseThrow(exsup);
		Set<ChunkPos> chunks = new HashSet<>();
		for (int cx = mincx; cx <= maxcx; cx++) {
			for (int cz = mincz; cz <= maxcz; cz++) {
				ChunkPos testPos = new ChunkPos(cx, cz);
				Rectangle2D chunkRect = new Rectangle(testPos.getMinBlockX(), testPos.getMinBlockZ(),
						testPos.getMaxBlockX() - testPos.getMinBlockX() + 1,
						testPos.getMaxBlockZ() - testPos.getMinBlockZ() + 1);
				if (newSanct.containsOrIntersects(chunkRect)) {
					chunks.add(testPos);
				}
			}
		}

		chunks.forEach((cp) -> {
			this.chunksToSanctuaries.put(cp, newSanct);
			this.sanctuariesToChunks.put(newSanct, cp);
		});
	}

	@Override
	public Stream<ISanctuary> getCompleteSanctuaries() {
		return sanctuariesToChunks.keySet().stream();
	}

	@Override
	public Stream<ISanctuary> getSanctuaries(ChunkPos pos) {
		return chunksToSanctuaries.get(pos).stream();
	}

	@Override
	public boolean canStandAt(BlockPos pos, Entity entity) {
		return chunksToSanctuaries.get(new ChunkPos(pos)).stream().filter((s) -> s.contains(pos))
				.noneMatch((s) -> s.timeUntilForbidden(entity) <= 0);
	}

	@Override
	public Optional<ISanctuary> getSanctuaryAtPos(BlockPos pos) {
		return chunksToSanctuaries.get(new ChunkPos(pos)).stream().filter((s) -> s.contains(pos)).findFirst();
	}

	@Override
	public Optional<ISanctuary> getSanctuaryAtPos(Position pos) {
		return chunksToSanctuaries.get(new ChunkPos(BlockPos.containing(pos))).stream().filter((s) -> s.contains(pos))
				.findFirst();
	}

	public Stream<ISanctuary> stream() {
		return sanctuariesToChunks.keySet().stream();
	}

	@Override
	public int size() {
		return sanctuariesToChunks.keySet().size();
	}
}