package com.gm910.sotdivine.magic.sanctuary.storage;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.gm910.sotdivine.common.misc.ParticleSpecification;
import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;
import com.gm910.sotdivine.magic.sanctuary.type.SanctuaryBoundaryProber;
import com.gm910.sotdivine.mixins_assist.pathfinding.IPathTypeCache;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.material.MapColor.Brightness;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * 
 */
class SanctuarySystem extends SavedData implements ISanctuarySystem {
	protected static final WeakHashMap<ServerLevel, SanctuarySystem> cachedSystems = new WeakHashMap<>(1);

	private Set<SanctuaryBoundaryProber> tickingProbes = new HashSet<>();
	private Set<ISanctuary> incomplete = new HashSet<>();
	private Multimap<ISanctuary, ChunkPos> sanctuariesToChunks = MultimapBuilder.hashKeys().hashSetValues().build();
	private Multimap<ChunkPos, ISanctuary> chunksToSanctuaries = MultimapBuilder.hashKeys().hashSetValues().build();
	private Optional<ServerLevel> levelReference = Optional.empty();

	public SanctuarySystem() {
	}

	public SanctuarySystem(Collection<ISanctuary> sancs) {
		sancs.forEach((s) -> this.addSanctuary(s));
	}

	@Override
	public void tick(long ticks, ServerLevel level) {
		for (SanctuaryBoundaryProber prober : new HashSet<>(tickingProbes)) {
			prober.tick(ticks, level);
			if (!prober.isProbing()) {
				tickingProbes.remove(prober);
			}
		}

		Set<ISanctuary> markAsComplete = new HashSet<>();
		Set<ISanctuary> deleteSanctuaries = new HashSet<>();
		for (ISanctuary sanctuary : this.incomplete) {
			if (!sanctuary.boundaryProber().beganSearch()) {
				sanctuary.boundaryProber().beginProbing((bp) -> ISanctuarySystem.selectBlock(bp, level, sanctuary),
						(bp) -> level.getBlockState(bp).isFaceSturdy(level, bp, Direction.UP), (bp, og) -> {
							bannerColoredParticle(ParticleTypes.ENTITY_EFFECT, og, level).sendParticle(level,
									bp.getCenter());
						}, (bp, og) -> bannerColoredParticle(ParticleTypes.TINTED_LEAVES, og, level).sendParticle(level,
								bp.above().getBottomCenter()));
				LogUtils.getLogger().debug(
						"Beginning to search for border for incomplete sanctuary " + sanctuary.boundaryPositions());
				tickingProbes.add(sanctuary.boundaryProber());
			} else {
				if (!sanctuary.boundaryProber().isProbing()) {
					boolean failed = true;
					if (sanctuary.boundaryProber().foundCompleteBoundary()) {
						failed = false;
						sanctuary.boundaryProber().incorporateResult();
						IDeitySymbol symbol = ISanctuarySystem.pickSymbols(level, sanctuary.boundaryPositions());
						if (symbol == null) {
							LogUtils.getLogger().debug("Failed to get sanctuary symbol");
							failed = true;
						} else {
							LogUtils.getLogger().debug("Created sanctuary with symbol " + symbol.toString()
									+ ", deityname = " + sanctuary.deityName());
							sanctuary.setSymbol(symbol);
							markAsComplete.add(sanctuary);
							sanctuary.allPositionsOnBorder()
									.forEach((bp) -> IPathTypeCache.get(level).invalidateSanctuary(bp));
						}
					}
					if (failed) {
						markBorder(sanctuary, level, (bp) -> defaultParticleEffect(DustParticleOptions.REDSTONE)
								.sendParticle(level, bp.getCenter()));
						markUnaccounted(sanctuary, level, (bp) -> defaultParticleEffect(ParticleTypes.PORTAL)
								.sendParticle(level, bp.getCenter()));
						deleteSanctuaries.add(sanctuary);
						LogUtils.getLogger().debug("Removing incomplete sanctuary " + sanctuary.boundaryPositions());
					}
				}
			}
		}
		deleteSanctuaries.forEach((s) -> {
			incomplete.remove(s);
		});
		markAsComplete.forEach((s) -> {
			incomplete.remove(s);
			this.addCompleteSanctuary(s);
		});

		Set<ISanctuary> markAsIncomplete = new HashSet<>();
		for (ISanctuary sanctuary : sanctuariesToChunks.keySet()) {
			// if (sanctuary.deityName() == null) { // only probe if the deity has not
			// claimed; a detiy claim freezes
			// the
			// borders
			if (sanctuariesToChunks.get(sanctuary).stream().noneMatch((ch) -> level.hasChunk(ch.x, ch.z))) {
				continue;
			}
			if (!sanctuary.boundaryProber().isProbing() && sanctuary.boundaryProber().beganSearch()) {
				if (!sanctuary.boundaryProber().foundCompleteBoundary()) {
					LogUtils.getLogger().debug("Marking sanctuary as incomplete " + sanctuary.boundaryPositions());
					sanctuary.allPositionsOnBorder().forEach((bp) -> IPathTypeCache.get(level).invalidateSanctuary(bp));
					markBorder(sanctuary, level,
							(bp) -> defaultParticleEffect(ParticleTypes.FLAME).sendParticle(level, bp.getCenter()));
					markUnaccounted(sanctuary, level,
							(bp) -> defaultParticleEffect(ParticleTypes.EXPLOSION).sendParticle(level, bp.getCenter()));
					markAsIncomplete.add(sanctuary);
				} else {
					sanctuary.boundaryProber().incorporateResult();
					sanctuary.allPositionsOnBorder().forEach((bp) -> IPathTypeCache.get(level).invalidateSanctuary(bp));
				}

			}
			markBorder(sanctuary, level,
					(bp) -> defaultParticleEffect(ParticleTypes.ENCHANT).sendParticle(level, bp.getCenter()));
			// }
		}
		markAsIncomplete.forEach((s) -> {
			remove(s);
			incomplete.add(s);
		});
		this.setDirty();

	}

	@Override
	public ISanctuary removeAtPos(BlockPos pos) {
		ChunkPos chunk = new ChunkPos(pos);
		ISanctuary out = null;
		for (ISanctuary sanc : chunksToSanctuaries.get(chunk).stream().filter((s) -> s.contains(pos)).toList()) {
			out = sanc;
			remove(sanc);
		}
		return out;
	}

	@Override
	public void remove(ISanctuary sanctuary) {
		LogUtils.getLogger().debug("Removing sanctuary with boundary " + sanctuary.boundaryPositions());
		levelReference.ifPresent((level) -> {
			this.markBorder(sanctuary, level,
					(bp) -> defaultParticleEffect(ParticleTypes.FLAME).sendParticle(level, bp.getCenter()));
		});
		sanctuariesToChunks.removeAll(sanctuary).forEach((s) -> chunksToSanctuaries.remove(s, sanctuary));
		incomplete.remove(sanctuary);
	}

	private void addCompleteSanctuary(ISanctuary sanctuary) {
		Supplier<IllegalArgumentException> exsup = () -> new IllegalArgumentException(
				"Cannot mark empty sanctuary as complete " + sanctuary);
		int mincx = sanctuary.boundaryPositions().stream().map((b) -> new ChunkPos(b)).mapToInt((c) -> c.x).min()
				.orElseThrow(exsup);
		int mincz = sanctuary.boundaryPositions().stream().map((b) -> new ChunkPos(b)).mapToInt((c) -> c.z).min()
				.orElseThrow(exsup);
		int maxcx = sanctuary.boundaryPositions().stream().map((b) -> new ChunkPos(b)).mapToInt((c) -> c.x).max()
				.orElseThrow(exsup);
		int maxcz = sanctuary.boundaryPositions().stream().map((b) -> new ChunkPos(b)).mapToInt((c) -> c.z).max()
				.orElseThrow(exsup);
		ChunkPos minCP = new ChunkPos(mincx, mincz);
		ChunkPos maxCP = new ChunkPos(maxcx, maxcz);
		Set<ChunkPos> chunks = new HashSet<>();
		for (int cx = minCP.x; cx <= maxCP.x; cx++) {
			for (int cz = minCP.z; cz <= maxCP.z; cz++) {
				ChunkPos testPos = new ChunkPos(cx, cz);
				Rectangle2D chunkRect = new Rectangle(testPos.getMinBlockX(), testPos.getMinBlockZ(),
						testPos.getMaxBlockX() - testPos.getMinBlockX() + 1,
						testPos.getMaxBlockZ() - testPos.getMinBlockZ() + 1);
				if (sanctuary.containsOrIntersects(chunkRect)) {
					chunks.add(testPos);
					for (ISanctuary sanct2 : chunksToSanctuaries.get(testPos)) {
						if (sanctuary.allPositionsOnBorder().stream().anyMatch((bp) -> sanct2.contains(bp))
								|| sanct2.allPositionsOnBorder().stream().anyMatch((bp) -> sanctuary.contains(bp))) {
							LogUtils.getLogger().debug(
									"Could not create new sanctuary because one of its positions is already occupied by another");
							return;
						}
					}
				}
			}
		}
		if (chunks.isEmpty()) {
			throw new IllegalArgumentException("Tried to add sanctuary which intersects no chunk positions");
		} else {
			LogUtils.getLogger().debug("Added new sanctuary " + sanctuary.boundaryPositions());
		}
		levelReference.ifPresent((level) -> markBorder(sanctuary, level,
				(pp) -> defaultParticleEffect(ParticleTypes.HAPPY_VILLAGER).sendParticle(level, pp.getCenter())));
		chunks.forEach((cp) -> {
			this.chunksToSanctuaries.put(cp, sanctuary);
			this.sanctuariesToChunks.put(sanctuary, cp);
		});
		LogUtils.getLogger().debug("Sanctuary crosses chunks " + sanctuariesToChunks.get(sanctuary));
	}

	@Override
	public void reaffirmSanctuary(ServerLevel level, ISanctuary sanctuary) {
		if (!tickingProbes.contains(sanctuary.boundaryProber())) {
			sanctuary.boundaryProber().beginProbing((bp) -> ISanctuarySystem.selectBlock(bp, level, sanctuary),
					(bp) -> level.getBlockState(bp).isFaceSturdy(level, bp, Direction.UP), (bp, og) -> {
						if (level.random.nextFloat() < 0.1)
							bannerColoredParticle(ParticleTypes.TINTED_LEAVES, og, level).sendParticle(level,
									bp.getCenter());
					}, (bp, og) -> defaultParticleEffect(ParticleTypes.END_ROD).sendParticle(level, bp.getCenter()));
			tickingProbes.add(sanctuary.boundaryProber());
		}
	}

	@Override
	public void addSanctuary(ISanctuary sanctuary) {
		if (sanctuary.complete()) {
			this.addCompleteSanctuary(sanctuary);
		} else {
			this.incomplete.add(sanctuary);
		}

	}

	@Override
	public Stream<ISanctuary> getCompleteSanctuaries() {
		return sanctuariesToChunks.keySet().stream();
	}

	@Override
	public Stream<ISanctuary> getCompleteAndIncompleteSanctuaries() {
		return Streams.concat(sanctuariesToChunks.keySet().stream(), incomplete.stream());
	}

	@Override
	public Stream<ISanctuary> getIncompleteSanctuaries() {
		return incomplete.stream();
	}

	@Override
	public Stream<ISanctuary> getSanctuaries(ChunkPos pos) {
		return chunksToSanctuaries.get(pos).stream();
	}

	@Override
	public Stream<ISanctuary> getSanctuaries(IDeity owner) {
		return sanctuariesToChunks.keySet().stream()
				.filter((d) -> d.deityName() != null && d.deityName().equals(owner.uniqueName()));
	}

	@Override
	public Optional<ISanctuary> getSanctuaryAtPos(BlockPos pos) {
		return chunksToSanctuaries.get(new ChunkPos(pos)).stream().filter((s) -> s.contains(pos)).findFirst();
	}

	@Override
	public boolean canStandAt(BlockPos pos, Entity entity) {
		return chunksToSanctuaries.get(new ChunkPos(pos)).stream().filter((s) -> s.contains(pos))
				.noneMatch((s) -> s.timeUntilForbidden(entity) <= 0);
	}

	@Override
	public void updateLevelReference(ServerLevel level) {
		this.levelReference = Optional.of(level);
	}

	/**
	 * Uses some effect to delineate a sanctuary border.
	 * 
	 * @param sanctuary
	 */
	private void markBorder(ISanctuary sanctuary, ServerLevel level, Consumer<BlockPos> effect) {
		for (BlockPos bound : sanctuary.allPositionsOnBorder()) {
			effect.accept(bound);
		}
	}

	/**
	 * Uses some effect to indicate unaccounted blocks in a sanctuary border.
	 * 
	 * @param sanctuary
	 */
	private void markUnaccounted(ISanctuary sanctuary, ServerLevel level, Consumer<BlockPos> effect) {
		for (BlockPos bound : sanctuary.boundaryProber().getUnaccountedBlocks()) {
			effect.accept(bound);
		}
	}

	/**
	 * creates a particle effect
	 * 
	 * @param particle
	 * @return
	 */
	private ParticleSpecification defaultParticleEffect(ParticleOptions particle) {
		return new ParticleSpecification(particle, Vec3.ZERO, new Vec3(0.2, 0.2, 0.2), 0, 12, false, false);
	}

	/**
	 * A particle specification colored like the first block this sanctuary emerged
	 * from
	 * 
	 * @param sanctuary
	 * @param level
	 * @return
	 */
	private ParticleSpecification bannerColoredParticle(ParticleType<ColorParticleOption> type, BlockPos ogpos,
			ServerLevel level) {
		return defaultParticleEffect(ColorParticleOption.create(type,
				level.getExistingBlockEntity(ogpos) instanceof BannerBlockEntity bbe
						? bbe.getBaseColor().getTextureDiffuseColor()
						: level.getBlockState(ogpos).getMapColor(level, ogpos).calculateARGBColor(Brightness.HIGH)));
	}

}
