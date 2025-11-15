package com.gm910.sotdivine.magic.sanctuary.type;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;
import com.gm910.sotdivine.util.StreamUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * A prober which searches a sanctuary boundary
 */
public class SanctuaryBoundaryProber {

	/**
	 * How far a sanctuary's boundary points can scan for additions
	 */
	public static final int MAX_SCAN_DISTANCE = 32;
	/**
	 * Expected number of probes to tick in a single iteration
	 */
	public static final int PROBES_IN_SINGLE_ITERATION = 32;

	private Sanctuary owner;
	private Set<Probe> activeProbes = new HashSet<>();
	private Set<Probe> finishedProbes = new HashSet<>();
	private Set<BlockPos> unaccountedBlocks = new HashSet<>();
	private Predicate<BlockPos> selectBlocks = (p) -> false;
	private Predicate<BlockPos> blockedBy = (p) -> true;
	private Probe bestProbe;
	private boolean beganSearch = false;

	private BiConsumer<BlockPos, BlockPos> signifyMovement;
	private BiConsumer<BlockPos, BlockPos> signifyFailure;

	SanctuaryBoundaryProber(Sanctuary sanctuary) {
		this.owner = sanctuary;
	}

	public ISanctuary getOwner() {
		return owner;
	}

	/**
	 * Ticks all activeProbes administered by ths prober
	 * 
	 * @param gameTime
	 * @param level
	 */
	public void tick(long gameTime, ServerLevel level) {
		int probesTicked = 0;
		List<Probe> probes = new ArrayList<>(activeProbes);
		Collections.shuffle(probes);

		probingLoop: for (Probe mover : probes) {
			if (probesTicked >= PROBES_IN_SINGLE_ITERATION) {
				break;
			}
			if (!level.isLoaded(mover.pos)) {
				mover.remove();
				LogUtils.getLogger().debug(
						"Probe reached unloaded region (" + mover.pos.toShortString() + ") from " + mover.pivotPos);
				continue probingLoop;
			}
			boolean cmf = cannotMoveFurther(mover.pos, level);
			boolean toolong = mover.length > ISanctuary.BOUNDARY_MAX_LENGTH;
			boolean visited = mover.hasVisited();
			boolean collided = mover.pathBlocks.size() > 1 && mover.pathBlocks.getFirst().equals(mover.pos.immutable());
			if (cmf || toolong || (visited && !collided)) {
				mover.remove();
				if (cmf)
					LogUtils.getLogger()
							.debug("Probe reached world boundary (" + mover.pos.toShortString() + ") from "
									+ mover.pivotPos + ": " + mover.pathBlocks.stream().map(BlockPos::toShortString)
											.map((s) -> "(" + s + ")").collect(StreamUtils.setStringCollector(", ")));
				if (toolong)
					// LogUtils.getLogger().debug("Probe moved too long a distance (length=" +
					// mover.length + ") ("
					// + mover.pos.toShortString() + ") from " + mover.pivotPos);
					if (visited) {

						// LogUtils.getLogger().debug("Probe crossed visited pathType (" +
						// mover.pos.toShortString() + ") from "
						// + mover.pivotPos + ";; visited= " + mover.visited);
					}
				continue probingLoop;
			}

			if ((!mover.fresh() || !mover.isOriented()) && canSelectBlock(mover.pos)) {
				ISanctuarySystem.get(level).getSanctuaryAtPos(mover.pos)
						.ifPresent((san) -> ISanctuarySystem.get(level).reaffirmSanctuary(level, san));
				// if we looped back on our original pathType and the pathType is longer than 3,
				// complete
				if (collided) {
					if (mover.pathBlocks.size() > 2) {
						// LogUtils.getLogger()
						// .debug("Probe finished successfully with pathType of length " +
						// mover.pathBlocks.size());
						if (bestProbe != null && mover.length < bestProbe.length && !owner.complete) {
							mover.remove();
						} else {
							mover.completeAt(mover.pos.immutable());
							signifyMovement(mover);
						}
					} else {
						// LogUtils.getLogger().debug("Probe did not create a long enough pathType");
						signifyFailure(mover);
						mover.remove();
					}
					continue probingLoop;
				}
				mover.addToPath();
				if (!owner.complete) {
					signifyMovement(mover);
				}
				// check columns
				// the starting pos, so we can check our distance
				// two loops so we can also cover diagonals; only horizontal
				for (Direction direction1 : Direction.values()) {
					if (!Direction.Plane.HORIZONTAL.test(direction1)) {
						continue;
					}
					for (Direction direction2 : Direction.values()) {
						if (!Direction.Plane.HORIZONTAL.test(direction2)) {
							continue;
						}
						Probe splitoff = mover.split();
						splitoff.orient(direction1, direction2, mover.pos.immutable());
						probesTicked++;

					}
				}
				mover.remove();
			} else {
				if (isBlockedAt(mover.pos)) {
					this.unaccountedBlocks.add(mover.pos.immutable());
					mover.remove();
				} else if (!mover.isOriented()) {
					this.unaccountedBlocks.add(mover.pos.immutable());
					mover.remove();
				} else {
					if (mover.pos.distSqr(mover.pivotPos) > MAX_SCAN_DISTANCE * MAX_SCAN_DISTANCE) {
						mover.remove();
					} else {
						boolean blocked = isBlockedAt(mover.nextPos());
						boolean unsupported = !isBlockedAt(mover.pos.below());
						if (blocked || unsupported) {
							if (unsupported) {
								mover.adjustDown();
								while (!isBlockedAt(mover.pos.below())
										&& !cannotMoveFurther(mover.pos.below(), level)) {
									mover.visit();
									mover.advance();
								}
								mover.adjustReturn();
							}
							if (blocked) {
								mover.adjustUp();
								while (isBlockedAt(mover.nextPos())
										&& !cannotMoveFurther(mover.pos.immutable(), level)) {
									mover.visit();
									mover.advance();
								}
								mover.adjustReturn();
							}
						}
						mover.visit();

						mover.advance();

					}
				}
			}
		}

		for (Probe probe : this.finishedProbes) {
			if (bestProbe == null || probe.length > bestProbe.length) {
				bestProbe = probe;
			}
		}

		if (activeProbes.isEmpty()) {
			this.finishedProbes.clear();
			if (owner.complete) {
				if (bestProbe != null) {
				} else {
					LogUtils.getLogger().debug("Lost border for completed sanctuary...");
				}
			}
		} else {

		}
	}

	/**
	 * Returns all blocks that exist in the sanctuary's current borders that were
	 * unaccounted for on the most recent probing
	 * 
	 * @return
	 */
	public Set<BlockPos> getUnaccountedBlocks() {
		return unaccountedBlocks;
	}

	/**
	 * Takes the results of this search and applies them to the boundary line of the
	 * sanctuary. Return true if the sanctuary was changed. If the boundary is
	 * incomplete, the sanctuary will be marked incomplete
	 * 
	 * @return
	 */
	public boolean incorporateResult() {
		if (!activeProbes.isEmpty()) {
			throw new UnsupportedOperationException("Cannot query while prober is searching");
		}
		this.beganSearch = false;
		if (bestProbe == null) {
			BlockPos first = owner.borderBlocks.getFirst();
			owner.borderBlocks = Lists.newArrayList(first);
			owner.fullBorder = Sets.newHashSet(first);
			owner.boundary = turnIntoPath(owner.borderBlocks);
			owner.recalculateMinMax();
			owner.complete = false;
			return false;
		} else {
			boolean changed = !this.owner.borderBlocks.equals(bestProbe.pathBlocks);
			this.owner.borderBlocks = bestProbe.pathBlocks;
			this.owner.boundary = turnIntoPath(bestProbe.pathBlocks);
			this.owner.fullBorder = bestProbe.visited;
			owner.recalculateMinMax();
			owner.complete = true;
			this.bestProbe = null;
			return changed;
		}
	}

	protected Path2D.Double turnIntoPath(List<BlockPos> positions) {
		Path2D.Double newPath = new Path2D.Double();
		boolean first = true;
		for (BlockPos pos : positions) {
			if (first) {
				newPath.moveTo(pos.getX() + 0.5, pos.getZ() + 0.5);
				first = false;
			} else {
				newPath.lineTo(pos.getX() + 0.5, pos.getZ() + 0.5);
			}
		}
		if (!positions.getLast().equals(positions.getFirst())) {
			newPath.closePath();
			// newPath.lineTo(positions.getFirst().getX() + 0.5, positions.getFirst().getZ()
			// + 0.5);
		}
		return newPath;
	}

	/**
	 * Causes this prober to begin searching with the specified conditions
	 * 
	 * @param selectBlocks    selects block positions; e.g., if the probe reaches
	 *                        the base of a pillar, but the block at the top of the
	 *                        pillar is the block we should add to the boundary,
	 *                        then add it
	 * @param blockedBy       what block to not pass through
	 * @param signifyMovement an optional function to create an effect at a position
	 *                        to signify movement to it; first position is
	 *                        currentpos, second is the starting pos
	 * @param signifyFailure  same as signify movement, but for positiosn the mover
	 *                        fails to move to
	 */
	public void beginProbing(Predicate<BlockPos> selectBlocks, Predicate<BlockPos> blockedBy,
			@Nullable BiConsumer<BlockPos, BlockPos> signifyMovement,
			@Nullable BiConsumer<BlockPos, BlockPos> signifyFailure) {
		this.reset();
		this.beganSearch = true;
		this.selectBlocks = selectBlocks;
		this.blockedBy = blockedBy;
		this.signifyMovement = signifyMovement;
		this.signifyFailure = signifyFailure;
		for (BlockPos position : this.owner.borderBlocks) {
			createProbe(position);
		}
	}

	/**
	 * If this is actively probing
	 * 
	 * @return
	 */
	public boolean isProbing() {
		return !this.activeProbes.isEmpty();
	}

	/**
	 * Return true if this has begun searching
	 * 
	 * @return
	 */
	public boolean beganSearch() {
		return beganSearch;
	}

	/**
	 * Return if this prober found a completed boundary line; throws exception if
	 * still searching
	 * 
	 * @return
	 */
	public boolean foundCompleteBoundary() {
		if (!activeProbes.isEmpty()) {
			throw new UnsupportedOperationException("Cannot query while prober is searching");
		}
		return this.bestProbe != null;
	}

	/**
	 * Return what sequence of blocks is most preferred, or null if no paths could
	 * be completed. Throw an error if still in progress of searching.
	 * 
	 * @return
	 */
	public List<BlockPos> bestPath() {
		if (bestProbe != null) {
			return bestProbe.pathBlocks;
		}
		if (!activeProbes.isEmpty()) {
			throw new UnsupportedOperationException("Cannot query while prober is searching");
		}
		return null;
	}

	/**
	 * Clears all activeProbes and such
	 */
	public void reset() {
		this.activeProbes.clear();
		this.finishedProbes.clear();
		this.bestProbe = null;
		this.unaccountedBlocks.clear();
		this.beganSearch = false;
	}

	private boolean canSelectBlock(BlockPos pos) {
		return this.selectBlocks.test(pos);
	}

	/**
	 * Whether we are at a position we cannot move to
	 * 
	 * @param pos
	 * @return
	 */
	private boolean isBlockedAt(BlockPos pos) {
		return this.blockedBy.test(pos);
	}

	/**
	 * Whether we are at the edge of the world
	 * 
	 * @param pos
	 * @param level
	 * @return
	 */
	private boolean cannotMoveFurther(BlockPos pos, ServerLevel level) {
		return !level.isInWorldBounds(pos);
	}

	private void signifyMovement(Probe pro) {
		if (this.signifyMovement != null) {
			for (BlockPos pos : pro.visited) {
				this.signifyMovement.accept(pos,
						pro.pathBlocks.isEmpty() ? pro.pos.immutable() : pro.pathBlocks.getFirst());
			}
		}
	}

	private void signifyFailure(Probe pos) {
		if (this.signifyFailure != null)
			this.signifyFailure.accept(pos.pos.immutable(),
					pos.pathBlocks.isEmpty() ? pos.pos.immutable() : pos.pathBlocks.getFirst());
	}

	private void createProbe(BlockPos pos) {
		activeProbes.add(new Probe(pos));
	}

	private class Probe {
		private MutableBlockPos pos;
		private List<BlockPos> pathBlocks = new ArrayList<>();
		private double length = 0;
		private Set<BlockPos> visited = new HashSet<>();
		private Direction orient1 = null;
		private Direction orient2 = null;
		private BlockPos pivotPos;
		private Direction adjusting = null;
		private boolean fresh = true;

		private Probe(BlockPos pos) {
			this.pos = pos.mutable();
			this.pivotPos = pos.immutable();
			activeProbes.add(this);
		}

		public boolean isOriented() {
			return orient1 != null;
		}

		/**
		 * add a certain position to the pathType and also to "visited"
		 * 
		 * @param pos
		 */
		public void addToPath() {

			if (!pathBlocks.isEmpty()) {
				length += Math.sqrt(pos.distSqr(pathBlocks.getLast()));
			}
			pathBlocks.add(pos.immutable());
		}

		/**
		 * adds a blockpos to the list of visited positions
		 * 
		 * @param pos
		 */
		public void visit() {
			visited.add(pos.immutable());
		}

		/**
		 * If this probe has visited a position
		 * 
		 * @return
		 */
		public boolean hasVisited() {
			return visited.contains(pos.immutable());
		}

		/**
		 * Creates a copy of this probe
		 * 
		 * @return
		 */
		public Probe split() {
			Probe newProbe = new Probe(pos);
			newProbe.pathBlocks = new ArrayList<>(pathBlocks);
			newProbe.length = length;
			newProbe.visited = new HashSet<>(visited);
			newProbe.orient1 = orient1;
			newProbe.orient2 = orient2;
			newProbe.pivotPos = pivotPos;
			activeProbes.add(newProbe);
			return newProbe;
		}

		/**
		 * Removes this probe
		 */
		public void remove() {
			activeProbes.remove(this);
		}

		public void orient(Direction d1, Direction d2, BlockPos pivot) {
			this.orient1 = d1;
			this.orient2 = d2;
			this.pivotPos = pivot;
			this.adjusting = null;
		}

		/**
		 * Turn this probe down so it can look for a surface to land on
		 */
		public void adjustDown() {
			this.adjusting = Direction.DOWN;
		}

		/**
		 * Turn this probe up so it can climb over an obstacle
		 */
		public void adjustUp() {
			this.adjusting = Direction.UP;
		}

		/**
		 * Stop adjusting this probe and have it go back to regular movement
		 */
		public void adjustReturn() {
			this.adjusting = null;
		}

		/**
		 * If the directions are the same (or one of them is null), only move once;
		 * otherwise move twice
		 * 
		 * @param d1
		 * @param d2
		 */
		public void advance() {
			if (adjusting != null) {
				pos.move(adjusting);
			} else {
				if (orient1 == null)
					throw new IllegalStateException("Cannot move mover while it is unoriented");
				pos.move(orient1);
				if (orient2 != null && orient2 != orient1) {
					pos.move(orient2);
				}
			}
			fresh = false;
		}

		/**
		 * If this was just created
		 * 
		 * @return
		 */
		public boolean fresh() {
			return fresh;
		}

		/**
		 * The position this mover will move to next, ignoring adjusting
		 * 
		 * @return
		 */
		public BlockPos nextPos() {
			if (orient1 == null)
				throw new IllegalStateException("Cannot project next position mover while it is unoriented");
			BlockPos pos = this.pos.relative(orient1);
			if (orient2 != null && orient2 != orient1) {
				pos = pos.relative(orient2);
			}
			return pos;
		}

		/**
		 * Next pos the mover will adjust to
		 */
		public BlockPos nextAdjustmentPos() {
			if (adjusting == null) {
				throw new IllegalStateException("Cannot project next position mover while it is not adjusting");
			}
			return pos.relative(adjusting);
		}

		/**
		 * Marks this probe as completed and remove section of the pathType that is
		 * before the given position
		 */
		public void completeAt(BlockPos pos) {
			remove();
			finishedProbes.add(this);
			int index = this.pathBlocks.indexOf(pos);
			if (index < 0) {
				throw new IllegalArgumentException(
						"Cannot complete probe at " + pos + " which is not in pathType " + pathBlocks);
			}
			int subLength = 0;
			BlockPos prior = pathBlocks.getFirst();
			for (BlockPos removalPos : pathBlocks) {
				subLength += Math.sqrt(removalPos.distSqr(prior));
				prior = removalPos;
			}
			this.pathBlocks.subList(0, this.pathBlocks.indexOf(pos)).clear();
			this.length -= subLength;
			;
		}
	}

}
