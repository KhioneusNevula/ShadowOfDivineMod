package com.gm910.sotdivine.mixins_assist.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ImmovableLookControl extends DummyLookControl {

	public ImmovableLookControl(LivingEntity mob) {
		super(mob);
	}

	@Override
	public boolean immovable() {
		return true;
	}

	@Override
	public void tick() {
	}

	@Override
	protected void clampHeadRotationToBody() {

	}

	@Override
	public void setLookAt(double p_24947_, double p_24948_, double p_24949_) {

	}

	@Override
	public void setLookAt(double p_24951_, double p_24952_, double p_24953_, float p_24954_, float p_24955_) {

	}

	@Override
	public void setLookAt(Entity p_148052_) {

	}

	@Override
	public void setLookAt(Entity p_24961_, float p_24962_, float p_24963_) {

	}

	@Override
	public void setLookAt(Vec3 p_24965_) {

	}

}
