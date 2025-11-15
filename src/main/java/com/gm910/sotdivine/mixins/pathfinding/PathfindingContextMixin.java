package com.gm910.sotdivine.mixins.pathfinding;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gm910.sotdivine.mixins_assist.pathfinding.IPathTypeCache;
import com.gm910.sotdivine.mixins_assist.pathfinding.PathfindingContextLevelGetter;
import com.gm910.sotdivine.util.MethodUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathTypeCache;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

@Mixin(PathfindingContext.class)
public abstract class PathfindingContextMixin {

	@Shadow
	@Final
	@Mutable
	private CollisionGetter level;
	@Nullable
	@Shadow
	@Final
	@Mutable
	private PathTypeCache cache;
	@Shadow
	@Final
	@Mutable
	private BlockPos mobPosition;
	@Shadow
	@Final
	@Mutable
	private BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

	private Mob mob;

	@Inject(method = "<init>", at = @At("RETURN"), require = 1)
	public void PathfindingContext(CollisionGetter p_335722_, Mob p_329527_, CallbackInfo ci) {
		mob = p_329527_;
		if (p_329527_.level() instanceof ServerLevel) {
			this.level = new PathfindingContextLevelGetter(p_335722_, p_329527_);
		}
	}

	@Inject(method = "getPathTypeFromState", at = @At("RETURN"), require = 1, cancellable = true)
	public void getPathTypeFromState(int x, int y, int z, CallbackInfoReturnable<PathType> ret) {
		BlockPos blockpos = this.mutablePos.set(x, y, z);
		if (this.cache == null) {
			ret.setReturnValue(MethodUtils.callStaticMethod(WalkNodeEvaluator.class, "getPathTypeFromState", "b",
					new Class<?>[] { BlockGetter.class, BlockPos.class }, this.level, blockpos));
		} else {
			ret.setReturnValue(((IPathTypeCache) this.cache).getOrCompute(this.level, blockpos, mob));
		}
	}

}
