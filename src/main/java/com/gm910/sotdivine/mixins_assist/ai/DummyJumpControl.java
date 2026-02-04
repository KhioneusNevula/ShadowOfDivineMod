package com.gm910.sotdivine.mixins_assist.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.JumpControl;

public class DummyJumpControl extends JumpControl {

	private LivingEntity mob;
	private boolean canMove;

	public DummyJumpControl(LivingEntity mob, boolean canMove) {
		super(null);
		this.mob = mob;
		this.canMove = canMove;
	}

	public LivingEntity entity() {
		return mob;
	}

	@Override
	public void jump() {
		if (canMove) {
			super.jump();
		}
	}

	public void tick() {
		if (canMove) {
			this.mob.setJumping(this.jump);
		}
		this.jump = false;
	}

}
