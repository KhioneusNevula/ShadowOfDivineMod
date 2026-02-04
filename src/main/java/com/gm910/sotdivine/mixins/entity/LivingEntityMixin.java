package com.gm910.sotdivine.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gm910.sotdivine.magic.afterlife.ISoulState;
import com.gm910.sotdivine.magic.afterlife.ISoulState.LifeState;
import com.gm910.sotdivine.mixins_assist.entity.IUndeadable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.WaypointTransmitter;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Attackable, WaypointTransmitter,
		net.minecraftforge.common.extensions.IForgeLivingEntity, IUndeadable {

	public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
		super(p_19870_, p_19871_);
	}

	@Inject(method = "getLastDamageSource", at = @At("RETURN"), require = 1, cancellable = true)
	public void getLastDamageSource(CallbackInfoReturnable<DamageSource> ci) {
		if (!isAlive())
			return;
		ISoulState mySoul = ISoulState.get((LivingEntity) (Object) this);
		if (mySoul.isPossessingOther()
				&& mySoul.getPossessee().getExistingEntity(getServer()).orElse(null) instanceof LivingEntity mount) {
			if (mount.getLastDamageSource() != null)
				ci.setReturnValue(mount.getLastDamageSource());
		}
	}

	@Inject(method = "isInvertedHealAndHarm", at = @At("RETURN"), require = 1, cancellable = true)
	public void isInvertedHealAndHarm(CallbackInfoReturnable<Boolean> ci) {
		if (!isAlive()) {
			return;
		}
		ISoulState soul = ISoulState.get((LivingEntity) (Object) this);
		if (soul.getLifeState() == LifeState.undead) {
			ci.setReturnValue(true);
		} else if (ci.getReturnValueZ() && !soul.getLifeState().isBeyondDeath()) {
			ci.setReturnValue(false);
		}
	}

	@Inject(method = "getAttributeValue", at = @At("RETURN"), require = 1, cancellable = true)
	public void getAttributeValue(Holder<Attribute> attribute, CallbackInfoReturnable<Double> ci) {
		if (!isAlive()) {
			return;
		}
		if (ISoulState.get((LivingEntity) (Object) this).getLifeState().isBeyondDeath()) {
			if (attribute.equals(Attributes.ATTACK_DAMAGE) && ci.getReturnValueD() <= 0) {
				ci.setReturnValue(1.0);
			}
		}
	}

	@Override
	public boolean $isSunBurnTick() {
		if (!isAlive()) {
			return false;
		}
		if (ISoulState.get((LivingEntity) (Object) this).getLifeState() != LifeState.undead) {
			return false;
		}
		if (this.level().isBrightOutside() && !this.level().isClientSide) {
			float f = this.getLightLevelDependentMagicValue();
			BlockPos blockpos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
			boolean flag = this.isInWaterOrRain() || this.isInPowderSnow || this.wasInPowderSnow;
			if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !flag
					&& this.level().canSeeSky(blockpos)) {
				return true;
			}
		}

		return false;
	}
}
