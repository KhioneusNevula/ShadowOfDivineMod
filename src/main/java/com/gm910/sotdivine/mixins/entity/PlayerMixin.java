package com.gm910.sotdivine.mixins.entity;

import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gm910.sotdivine.mixins_assist.player.PlayerVibrationUser;
import com.mojang.authlib.GameProfile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.extensions.IForgePlayer;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements IForgePlayer, VibrationSystem {

	private DynamicGameEventListener<VibrationSystem.Listener> dynamicGameEventListener;
	private VibrationSystem.User vibrationUser;
	private VibrationSystem.Data vibrationData;

	protected PlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
		super(p_20966_, p_20967_);
	}

	@Inject(method = "<init>", at = @At("RETURN"), require = 1)
	public void Player(Level level, GameProfile profile, CallbackInfo ci) {

		this.vibrationUser = new PlayerVibrationUser((Player) (Object) this);
		this.vibrationData = new VibrationSystem.Data();
		this.dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
	}

	@Override
	public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer) {
		if (this.level() instanceof ServerLevel serverlevel) {
			consumer.accept(this.dynamicGameEventListener, serverlevel);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"), require = 1)
	public void tick(CallbackInfo ci) {
		if (this.level() instanceof ServerLevel serverlevel) {
			VibrationSystem.Ticker.tick(serverlevel, this.vibrationData, this.vibrationUser);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"), require = 1)
	public void addAdditionalSaveData(ValueOutput out, CallbackInfo ci) {
		out.store("sotd_listener", VibrationSystem.Data.CODEC, this.vibrationData);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"), require = 1)
	public void readAdditionalSaveData(ValueInput in, CallbackInfo ci) {
		this.vibrationData = in.read("sotd_listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
	}

	@Override
	public Data getVibrationData() {
		return this.vibrationData;
	}

	@Override
	public User getVibrationUser() {
		return this.vibrationUser;
	}

}
