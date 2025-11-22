package com.gm910.sotdivine.magic.sanctuary.type;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.magic.sanctuary.cap.ISanctuaryInfo;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sanctuary implementation
 */
public non-sealed class Sanctuary implements ISanctuary {

	String deityName;
	IDeitySymbol deitySymbol;
	Set<BlockPos> symbolBlocks;
	Path2D.Double boundary;
	List<BlockPos> borderBlocks;
	ResourceKey<Level> dimension;
	private SanctuaryBoundaryProber prober = new SanctuaryBoundaryProber(this);
	private int lowestY;
	private int highestY;
	boolean complete = false;
	public Set<BlockPos> fullBorder;
	private String uname;

	protected Sanctuary(String uname, @Nullable String deityName, IDeitySymbol symbol, ResourceKey<Level> dimension,
			List<BlockPos> positions, Collection<BlockPos> sb, boolean complete) {
		this.deityName = deityName;
		this.deitySymbol = symbol;
		this.dimension = dimension;
		this.borderBlocks = positions;
		this.boundary = prober.turnIntoPath(positions);
		this.fullBorder = new HashSet<>(positions);
		this.complete = complete;
		this.uname = uname;
		this.symbolBlocks = new HashSet<>(sb);
	}

	@Override
	public String uniqueName() {
		return uname;
	}

	@Override
	public boolean complete() {
		return complete;
	}

	@Override
	public String deityName() {
		return deityName;
	}

	@Override
	public IDeitySymbol symbol() {
		return deitySymbol;
	}

	@Override
	public void claim(String deity) {
		this.deityName = deity;
	}

	@Override
	public int timeUntilForbidden(Entity entity) {
		// TODO make this specific to the deity
		// if you hurt something, the PROTECTION goes away
		if (entity.getCapability(ISanctuaryInfo.CAPABILITY).orElse(null) instanceof ISanctuaryInfo info) {
			if (info.recentlyBanned(deityName)) {
				return 0;
			} else if (info.recentlyPermitted(deityName)) {
				if (info.permissionTime(uname) > 0) {
					return info.permissionTime(uname);
				}
			}
		}

		if (this.deityName != null && IPartySystem.get((ServerLevel) entity.level()).deityByName(deityName)
				.orElse(null) instanceof IDeity deity) {
			if (deity.permitsInSanctuary((ServerLevel) entity.level(), entity)) {
				return Integer.MAX_VALUE;
			}
			return 0;
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public void setSymbol(IDeitySymbol symbol) {
		this.deitySymbol = symbol;
	}

	protected void recalculateMinMax() {
		this.lowestY = borderBlocks.stream().mapToInt(BlockPos::getY).min().orElse(Integer.MAX_VALUE);
		this.highestY = borderBlocks.stream().mapToInt(BlockPos::getY).min().orElse(Integer.MIN_VALUE);
	}

	@Override
	public int lowestY() {
		return lowestY;
	}

	@Override
	public int highestY() {
		return highestY;
	}

	@Override
	public List<BlockPos> boundaryPositions() {
		return Collections.unmodifiableList(borderBlocks);
	}

	@Override
	public Set<BlockPos> allPositionsOnBorder() {
		return Collections.unmodifiableSet(this.fullBorder);
	}

	@Override
	public SanctuaryBoundaryProber boundaryProber() {
		return prober;
	}

	/**
	 * Whether a sanctuary contains this rectangle
	 * 
	 * @param pos
	 * @return
	 */
	public boolean containsOrIntersects(Rectangle2D rect) {
		return Path2D.intersects(boundary.getPathIterator(null), rect)
				|| Path2D.contains(boundary.getPathIterator(null), rect);
	}

	public boolean contains(BlockPos pos) {
		return (Path2D.intersects(boundary.getPathIterator(null), pos.getX(), pos.getZ(), 1, 1)
				|| Path2D.contains(boundary.getPathIterator(null), pos.getX(), pos.getZ(), 1, 1))
				&& pos.getY() <= this.upperLimitY() && pos.getY() >= this.lowerLimitY();
	}

	public boolean contains(Vec3 pos) {
		return Path2D.contains(boundary.getPathIterator(null), pos.x(), pos.y());
	}

	@Override
	public ResourceKey<Level> dimension() {
		return dimension;
	}

	@Override
	public void addSymbolBlock(ServerLevel level, BlockPos pos) {
		this.symbolBlocks.add(pos);
	}

	@Override
	public void removeSymbolBlock(ServerLevel level, BlockPos pos) {
		this.symbolBlocks.remove(pos);
	}

	@Override
	public boolean isSymbolBlock(BlockPos pos) {
		return symbolBlocks.contains(pos);
	}

	@Override
	public Stream<BlockPos> symbolBlocks() {

		return symbolBlocks.stream();
	}

}
