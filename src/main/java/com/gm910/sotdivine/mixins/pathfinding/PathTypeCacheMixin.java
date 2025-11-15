package com.gm910.sotdivine.mixins.pathfinding;

import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gm910.sotdivine.mixins_assist.pathfinding.IPathTypeCache;
import com.gm910.sotdivine.mixins_assist.pathfinding.PathfindingMixinHelper;
import com.gm910.sotdivine.mixins_assist.pathfinding.WrappedPathType;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathTypeCache;

@Mixin(PathTypeCache.class)
public abstract class PathTypeCacheMixin implements IPathTypeCache {

	private static final int BUFF_SIZE = 4;

	@Shadow
	@Mutable
	@Final
	private long[] positions = new long[4096];
	@Shadow
	@Mutable
	@Final
	private PathType[] pathTypes = new PathType[4096];

	private WrappedPathType[][] wrappedPathTypes;

	@Inject(method = "<init>", at = @At("RETURN"), require = 1)
	public void PathTypeCache(CallbackInfo ci) {
		this.wrappedPathTypes = new WrappedPathType[4096][BUFF_SIZE];
	}

	@Override
	public PathType getOrCompute(BlockGetter getter, BlockPos pos, Mob forMob) {
		long i = pos.asLong();
		int j = index(i);
		int k = index(forMob.getUUID());
		PathType pathtype = get(j, k, i, forMob);
		return pathtype != null ? pathtype : compute(getter, pos, j, k, i, forMob);
	}

	@Shadow
	private PathType get(int j, long i) {
		throw new UnsupportedOperationException("Failed mixinization somehow");
	}

	private PathType get(int j, int k, long i, Mob mob) {
		if (this.positions[j] == i) {
			WrappedPathType wpt = this.wrappedPathTypes[j][k];
			if (wpt == null) {
				return get(j, i);
			}
			if (wpt.mobID().isEmpty() || wpt.mobID().get().equals(mob.getUUID())) {
				return wpt.pathType();
			}
		}
		return null;
	}

	@Shadow
	private PathType compute(BlockGetter getter, BlockPos pos, int j, long i) {
		throw new UnsupportedOperationException("Failed mixinization somehow");
	}

	private PathType compute(BlockGetter getter, BlockPos pos, int j, int k, long i, Mob en) {
		PathType pathtype = PathfindingMixinHelper.tryReturnSanctuaryPathType(pos.getX(), pos.getY(), pos.getZ(), en);
		if (pathtype == null) {
			return compute(getter, pos, j, i);
		}
		this.positions[j] = i;
		this.wrappedPathTypes[j][k] = new WrappedPathType(pathtype, en);
		return pathtype;
	}

	@Override
	public void invalidateSanctuary(BlockPos pos) {
		long i = pos.asLong();
		int j = index(i);
		if (this.positions[j] == i) {
			this.wrappedPathTypes[j] = new WrappedPathType[BUFF_SIZE];
		}

	}

	private static int index(long p_328788_) {
		return (int) HashCommon.mix(p_328788_) & 4095;
	}

	private static int index(UUID p_328788_) {
		return (int) HashCommon.mix(p_328788_.getMostSignificantBits()) & (BUFF_SIZE - 1);
	}

}
