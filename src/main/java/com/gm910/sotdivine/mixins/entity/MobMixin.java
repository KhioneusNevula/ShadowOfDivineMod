package com.gm910.sotdivine.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gm910.sotdivine.magic.afterlife.ISoulState;
import com.gm910.sotdivine.magic.afterlife.ISoulState.LifeState;
import com.gm910.sotdivine.mixins_assist.ai.DummyJumpControl;
import com.gm910.sotdivine.mixins_assist.ai.DummyLookControl;
import com.gm910.sotdivine.mixins_assist.ai.DummyMoveControl;
import com.gm910.sotdivine.mixins_assist.ai.DummyPathNavigation;
import com.gm910.sotdivine.mixins_assist.ai.ImmovableLookControl;
import com.gm910.sotdivine.mixins_assist.ai.MovableLivingMoveControl;
import com.gm910.sotdivine.mixins_assist.entity.IUndeadable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentUser;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity implements EquipmentUser, Leashable, Targeting, IUndeadable {

	private DummyPathNavigation defaultNavigation;
	private DummyLookControl defaultLookControl;
	private DummyMoveControl defaultMoveControl;
	private DummyJumpControl defaultJumpControl;
	private FlyingPathNavigation ghostNavigation;
	private FlyingMoveControl ghostMoveControl;

	@Shadow
	private GoalSelector goalSelector;

	@Shadow
	private GoalSelector targetSelector;

	protected MobMixin(EntityType<? extends net.minecraft.world.entity.LivingEntity> p_20966_, Level p_20967_) {
		super(p_20966_, p_20967_);
	}

	@Inject(method = "isSunBurnTick", at = @At("HEAD"), require = 1, cancellable = true)
	protected void isSunBurnTick(CallbackInfoReturnable<Boolean> ci) {
		if (!ISoulState.get((Mob) (Object) this).getLifeState().isUndead()) {
			ci.setReturnValue(false);
		}
	}

	@Shadow
	protected abstract boolean isSunBurnTick();

	@Override
	public boolean $isSunBurnTick() {
		return isSunBurnTick();
	}

	@Inject(method = "getNavigation", at = @At("RETURN"), require = 1, cancellable = true)
	public void getNavigation(CallbackInfoReturnable<PathNavigation> ci) {
		if (!isAlive())
			return;
		ISoulState mySoul = ISoulState.get(this);
		if (mySoul.isPossessingOther()
				&& mySoul.getPossessee().getExistingEntity(getServer()).orElse(null) instanceof LivingEntity mount) {
			ISoulState mountSoul = ISoulState.get(mount);
			if (defaultNavigation == null || !defaultNavigation.entity().equals(mount)) {
				this.defaultNavigation = new DummyPathNavigation(mount, level(), (Mob) (Object) this);
			}
			if (mySoul.getPossessionStrength() >= mountSoul.getPossessionStrength()) {
				if (mount instanceof Mob mountMob) {
					ci.setReturnValue(mountMob.getNavigation());
				} else {

					ci.setReturnValue(defaultNavigation);
				}
			} else {
				ci.setReturnValue(defaultNavigation);
			}
		} else {
			if (mySoul.getLifeState().isSpirit()) {
				if (this.ghostNavigation == null) {
					this.ghostNavigation = new FlyingPathNavigation((Mob) (Object) this, this.level());
				}
				ci.setReturnValue(ghostNavigation);
			}
		}

	}

	@Inject(method = "getLookControl", at = @At("RETURN"), require = 1, cancellable = true)
	public void getLookControl(CallbackInfoReturnable<LookControl> ci) {
		if (!isAlive())
			return;
		ISoulState mySoul = ISoulState.get(this);
		if (mySoul.isPossessingOther()
				&& mySoul.getPossessee().getExistingEntity(getServer()).orElse(null) instanceof LivingEntity mount) {
			ISoulState mountSoul = ISoulState.get(mount);
			if (mySoul.getPossessionStrength() >= mountSoul.getPossessionStrength()) {
				if (mount instanceof Mob mountMob) {
					ci.setReturnValue(mountMob.getLookControl());
				} else {
					if (defaultLookControl == null || !defaultLookControl.entity().equals(mount)) {
						this.defaultLookControl = new DummyLookControl(mount);
					}
					ci.setReturnValue(defaultLookControl);
				}
			} else {
				if (defaultLookControl == null || !defaultLookControl.immovable()) {
					this.defaultLookControl = new ImmovableLookControl(mount);
				}
				ci.setReturnValue(defaultLookControl);
			}
		}
	}

	@Inject(method = "getMoveControl", at = @At("RETURN"), require = 1, cancellable = true)
	public void getMoveControl(CallbackInfoReturnable<MoveControl> ci) {
		if (!isAlive())
			return;
		ISoulState mySoul = ISoulState.get(this);
		if (mySoul.isPossessingOther()
				&& mySoul.getPossessee().getExistingEntity(getServer()).orElse(null) instanceof LivingEntity mount) {
			ISoulState mountSoul = ISoulState.get(mount);

			if (mySoul.getPossessionStrength() >= mountSoul.getPossessionStrength()) {
				if (mount instanceof Mob mountMob) {
					ci.setReturnValue(mountMob.getMoveControl());
				} else {
					if (defaultMoveControl == null || !defaultMoveControl.entity().equals(mount)) {
						this.defaultMoveControl = mount instanceof Player
								? new MovableLivingMoveControl(mount, (Mob) (Object) this)
								: new DummyMoveControl(mount);
					}
					ci.setReturnValue(defaultMoveControl);
				}
			} else {
				if (defaultMoveControl == null || !defaultMoveControl.immovable()) {
					this.defaultMoveControl = new DummyMoveControl(mount);
				}
				ci.setReturnValue(defaultMoveControl);
			}
		} else {
			if (mySoul.getLifeState() == LifeState.ghost) {
				if (this.ghostMoveControl == null) {
					this.ghostMoveControl = new FlyingMoveControl((Mob) (Object) this, 20, true);
				}
				ci.setReturnValue(ghostMoveControl);
			}
		}
	}

	@Inject(method = "getJumpControl", at = @At("RETURN"), require = 1, cancellable = true)
	public void getJumpControl(CallbackInfoReturnable<JumpControl> ci) {
		if (!isAlive())
			return;
		ISoulState mySoul = ISoulState.get(this);
		if (mySoul.isPossessingOther()
				&& mySoul.getPossessee().getExistingEntity(getServer()).orElse(null) instanceof LivingEntity mount) {
			ISoulState mountSoul = ISoulState.get(mount);
			if (defaultJumpControl == null || !defaultJumpControl.entity().equals(mount)) {
				this.defaultJumpControl = mount instanceof Player ? new DummyJumpControl(mount, true)
						: new DummyJumpControl(mount, false);
			}
			if (mySoul.getPossessionStrength() >= mountSoul.getPossessionStrength()) {
				if (mount instanceof Mob mountMob) {
					ci.setReturnValue(mountMob.getJumpControl());
				} else {
					ci.setReturnValue(defaultJumpControl);
				}
			} else {
				if (defaultJumpControl == null || !defaultJumpControl.entity().equals(mount)) {
					this.defaultJumpControl = new DummyJumpControl(mount, false);
				}
				ci.setReturnValue(defaultJumpControl);
			}
		}
	}

	@Inject(method = "getSensing", at = @At("RETURN"), require = 1, cancellable = true)
	public void getSensing(CallbackInfoReturnable<Sensing> ci) {
		if (!isAlive())
			return;
		ISoulState mysoul = ISoulState.get(this);
		if (mysoul.isPossessingOther()
				&& mysoul.getPossessee().getExistingEntity(getServer()).orElse(null) instanceof LivingEntity mount) {
			if (mount instanceof Mob mountMob) {
				ci.setReturnValue(mountMob.getSensing());
			}
		}
	}
}
