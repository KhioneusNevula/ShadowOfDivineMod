package com.gm910.sotdivine.mixins.ai;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gm910.sotdivine.mixins_assist.ai.IDisableable;
import com.mojang.authlib.minecraft.client.MinecraftClient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin implements IDisableable {

	private boolean disabled = false;

	@Final
	@Shadow
	private Set<WrappedGoal> availableGoals;

	@Override
	public boolean isDisabled() {
		return disabled;
	}

	@Override
	public void setDisabled(boolean dis) {
		disabled = dis;
	}

	@Inject(method = "tick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = {
			"ldc=goalUpdate", "log=true" }), require = 1, cancellable = true)
	public void beforeGoalUpdate(CallbackInfo ci) {
		if (disabled) {
			availableGoals.forEach(go -> go.stop());
			ci.cancel();
		}
	}

	@Inject(method = "tickRunningGoals", at = @At(value = "HEAD"), require = 1, cancellable = true)
	public void tickRunningGoals(boolean unknown, CallbackInfo ci) {
		if (disabled) {
			ci.cancel();
		}
	}
}
