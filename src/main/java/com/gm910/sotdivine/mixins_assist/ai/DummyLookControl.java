package com.gm910.sotdivine.mixins_assist.ai;

import java.util.Optional;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.LookControl;

public class DummyLookControl extends LookControl {

	private LivingEntity mob;

	public DummyLookControl(LivingEntity mob) {
		super(null);
		this.mob = mob;
	}

	public boolean immovable() {
		return false;
	}

	public LivingEntity entity() {
		return mob;
	}

	@Override
	public void tick() {
		if (this.resetXRotOnTick()) {
			mob.setXRot(0.0f);
		}

		if (this.lookAtCooldown > 0) {
			this.lookAtCooldown--;
			this.getYRotD().ifPresent(p_359087_ -> this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, p_359087_,
					this.yMaxRotSpeed));
			this.getXRotD().ifPresent(p_405414_ -> this.mob
					.setXRot(this.rotateTowards(this.mob.getXRot(), p_405414_, this.xMaxRotAngle)));
		} else {
			this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0F);
		}

		this.clampHeadRotationToBody();
	}

	@Override
	protected void clampHeadRotationToBody() {

	}

	@Override
	protected Optional<Float> getXRotD() {
		double d0 = this.wantedX - this.mob.getX();
		double d1 = this.wantedY - this.mob.getEyeY();
		double d2 = this.wantedZ - this.mob.getZ();
		double d3 = Math.sqrt(d0 * d0 + d2 * d2);
		return !(Math.abs(d1) > 1.0E-5F) && !(Math.abs(d3) > 1.0E-5F) ? Optional.empty()
				: Optional.of((float) (-(Mth.atan2(d1, d3) * 180.0F / (float) Math.PI)));
	}

	@Override
	protected Optional<Float> getYRotD() {
		double d0 = this.wantedX - this.mob.getX();
		double d1 = this.wantedZ - this.mob.getZ();
		return !(Math.abs(d1) > 1.0E-5F) && !(Math.abs(d0) > 1.0E-5F) ? Optional.empty()
				: Optional.of((float) (Mth.atan2(d1, d0) * 180.0F / (float) Math.PI) - 90.0F);
	}

	@Override
	public void setLookAt(double p_24947_, double p_24948_, double p_24949_) {
		this.setLookAt(p_24947_, p_24948_, p_24949_, 10, 40);
	}

}
