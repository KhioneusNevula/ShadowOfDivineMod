package com.gm910.sotdivine.magic.sanctuary.storage;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;
import com.google.common.base.Functions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * A dimension-specific system of sanctuaries
 */
public interface ISanctuarySystem {

	public static final String SAVED_DATA_ID = "sotdivine_sanctuaries";
	/**
	 * Codec for sanctuary system
	 */
	public static final Codec<ISanctuarySystem> CODEC = Codec.list(ISanctuary.CODEC).xmap((s) -> new SanctuarySystem(s),
			(s) -> ((SanctuarySystem) s).getCompleteAndIncompleteSanctuaries().toList());

	public static final SavedDataType<SanctuarySystem> SAVE_TYPE = new SavedDataType<>(SAVED_DATA_ID,
			SanctuarySystem::new, CODEC.xmap((x) -> (SanctuarySystem) x, Functions.identity()), null);

	/**
	 * Retrieve an instance of the sanctuary system from the given level.
	 * 
	 * @param level
	 * @return
	 */
	public static ISanctuarySystem get(ServerLevel level) {
		SanctuarySystem obtain = null;

		if (SanctuarySystem.cachedSystems.containsKey(level)) {
			obtain = SanctuarySystem.cachedSystems.get(level);
		} else {
			LogUtils.getLogger()
					.debug("Retrieving and caching instanceof SanctuarySystem for world " + level.toString());

			obtain = level.getDataStorage().computeIfAbsent(SAVE_TYPE);

			SanctuarySystem.cachedSystems.put(level, obtain);
		}
		obtain.updateLevelReference(level);
		return obtain;
	}

	/**
	 * Remove teh sanctuary at the given rawPosition
	 * 
	 * @param pos
	 * @return
	 */
	public ISanctuary removeAtPos(BlockPos pos);

	/**
	 * Remove given sanctuary
	 * 
	 * @param sanctuary
	 */
	public void remove(ISanctuary sanctuary);

	/**
	 * Try to add a new sanctuary to this system. The sanctuary can have a complete
	 * or incomplete boundary
	 * 
	 * @param sanctuary
	 */
	public void addSanctuary(ISanctuary sanctuary);

	/**
	 * Return all sanctuaries in this system
	 * 
	 * @param dimension
	 * @return
	 */
	public Stream<ISanctuary> getCompleteSanctuaries();

	/**
	 * Returns both sanctuaries that are fully built and ones that are not
	 * 
	 * @return
	 */
	Stream<ISanctuary> getCompleteAndIncompleteSanctuaries();

	/**
	 * Returns sanctuaries that are being built
	 * 
	 * @return
	 */
	Stream<ISanctuary> getIncompleteSanctuaries();

	/**
	 * Return all sanctuaries intersecting with this chunk
	 * 
	 * @param dimension
	 * @return
	 */
	public Stream<ISanctuary> getSanctuaries(ChunkPos pos);

	/**
	 * Return all sanctuaries belonging to the specific deity
	 * 
	 * @param dimension
	 * @return
	 */
	public Stream<ISanctuary> getSanctuaries(IDeity owner);

	/**
	 * Return the sanctuary that contains the given rawPosition, or null if none exists
	 * 
	 * @param pos
	 * @return
	 */
	public Optional<ISanctuary> getSanctuaryAtPos(BlockPos pos);

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
	 * Tick this system
	 * 
	 * @param ticks
	 * @param level
	 */
	public void tick(long ticks, ServerLevel level);

	/**
	 * In case we want an internal reference t the level this system was accessed
	 * from
	 */
	void updateLevelReference(ServerLevel level);

	/**
	 * If this block is a possible first block in a sanctuary boundary; return the
	 * rawPosition to start counting from. Else, return null
	 * 
	 * @param pos
	 * @param levelReference
	 * @return
	 */
	public static BlockPos getPossibleFirstBlock(BlockPos pos, ServerLevel level) {
		if (!level.getBlockState(pos).getBlock().builtInRegistryHolder().containsTag(BlockTags.BANNERS)) {
			return null;
		}

		Predicate<BlockPos> isFence = (bp) -> level.getBlockState(bp).getBlockHolder().containsTag(BlockTags.FENCES);
		Predicate<BlockPos> hasSymbols = (bp) -> DeitySymbols.instance().getFromPosition(level, bp).iterator()
				.hasNext();
		if (hasSymbols.test(pos)) {
			MutableBlockPos focalPos = pos.mutable();
			if (isFence.test(focalPos.move(Direction.DOWN))) {
				while (isFence.test(focalPos)) {
					focalPos.move(Direction.DOWN);
				}
				return focalPos.immutable();
			} else {
				return pos;
			}
		} else if (isFence.test(pos)) {
			// IPartySystem system = IPartySystem.get(levelReference);
			MutableBlockPos focalPos = pos.mutable();
			while (isFence.test(focalPos)) {
				focalPos.move(Direction.UP);
			}
			if (hasSymbols.test(focalPos)) {
				return focalPos.immutable();
			}
		}
		return null;
	}

	/**
	 * Selects the majority symbol in this border as the symbol of the entire region
	 * 
	 * @param level
	 * @param completedBorder
	 * @return
	 */
	public static IDeitySymbol pickSymbols(ServerLevel level, Collection<BlockPos> completedBorder) {
		Multiset<IDeitySymbol> symbols = HashMultiset.create();
		for (BlockPos posa : completedBorder) {
			MutableBlockPos focalPos = posa.mutable();
			Predicate<BlockPos> isFence = (bp) -> level.getBlockState(bp).getBlockHolder()
					.containsTag(BlockTags.FENCES);
			while (isFence.test(focalPos)) {
				focalPos.move(Direction.UP);
			}
			DeitySymbols.instance().getFromPosition(level, focalPos).distinct().forEach((s) -> symbols.add(s));
		}
		return symbols.entrySet().stream().max((a, b) -> Integer.compare(a.getCount(), b.getCount()))
				.map((s) -> s.getElement()).orElse(null);
	}

	/**
	 * How to select blocks for the sanctuary boundary to join with
	 * 
	 * @param pos
	 * @param levelReference
	 * @return
	 */
	public static boolean selectBlock(BlockPos pos, ServerLevel level, ISanctuary forSanctuary) {

		// IPartySystem system = IPartySystem.get(levelReference);
		MutableBlockPos focalPos = pos.mutable();
		Predicate<BlockPos> isFence = (bp) -> level.getBlockState(bp).getBlockHolder().containsTag(BlockTags.FENCES);
		while (isFence.test(focalPos)) {
			focalPos.move(Direction.UP);
		}

		if (!level.getBlockState(focalPos).getBlock().builtInRegistryHolder().containsTag(BlockTags.BANNERS)) {
			return false;
		}

		Set<IDeitySymbol> syms;
		if (forSanctuary.symbol() != null) {
			syms = Set.of(forSanctuary.symbol());
		} else {
			if (forSanctuary.boundaryPositions().isEmpty()) {
				syms = Set.of();
			} else {
				syms = DeitySymbols.instance().getFromPosition(level, forSanctuary.boundaryPositions().getFirst())
						.collect(Collectors.toSet());
			}
		}

		if (DeitySymbols.instance().getFromPosition(level, focalPos)
				.anyMatch((sym) -> syms.isEmpty() ? true : syms.contains(sym))) {
			return true;
		}
		return false;
	}

	/**
	 * Trigger a re-probing of this sanctuary's border
	 */
	public void reaffirmSanctuary(ServerLevel level, ISanctuary exPos);

}
