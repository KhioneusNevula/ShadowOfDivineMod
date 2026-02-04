package com.gm910.sotdivine.common.effects.types;

import java.util.List;

import com.gm910.sotdivine.common.effects.ModEffects;
import com.gm910.sotdivine.common.misc.ParticleSpecification;
import com.gm910.sotdivine.magic.afterlife.ISoulState;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.logging.LogUtils;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

public class IntangibleEffect extends MobEffect {

	public IntangibleEffect(MobEffectCategory category, int color) {
		super(category, color);
		MobEffectEvent.Remove.BUS.addListener(this::onRemoved);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity en, int ti) {
		if (en instanceof ServerPlayer player) {
			if (player.gameMode() != GameType.SPECTATOR) {
				LogUtils.getLogger().debug("Removed intangibility because " + en + " is not in spectator");
				return false;
			}
		} else {
			en.setInvulnerable(true);
			en.setInvisible(true);
			en.noPhysics = true;
			en.setGlowingTag(false);

			level.getServer().schedule(level.getServer().wrapRunnable(() -> {
				for (MobEffectInstance eff : List.copyOf(en.getActiveEffects())) {
					if (!eff.getEffect().unwrapKey().get().equals(ModEffects.INTANGIBLE.getKey()))
						en.removeEffect(eff.getEffect());
				}
			}));

		}
		return super.applyEffectTick(level, en, ti);
	}

	@Override
	public void onEffectStarted(LivingEntity en, int ti) {
		if (en.level() instanceof ServerLevel level) {
			var tag = ISoulState.get(en).extraData();
			en.setGlowingTag(true);
			if (en instanceof ServerPlayer player) {
				tag.store("priorGameMode", CodecUtils.caselessEnumCodec(GameType.class), player.gameMode());
				player.setGameMode(GameType.SPECTATOR);

				player.stopRiding();
				EnchantmentHelper.stopLocationBasedEffects(player);
			} else {
				tag.putBoolean("wasInvulnerableBefore", en.isInvulnerable());
				en.setInvulnerable(true);
				tag.putBoolean("wasInvisibleBefore", en.isInvisible());
				en.setInvisible(true);
				tag.putBoolean("wasGlowingBefore", en.isCurrentlyGlowing());
				en.setGlowingTag(false);
				tag.putBoolean("priorPhysics", en.noPhysics);
				en.noPhysics = true;
			}
			tag.store("priorEffects", MobEffectInstance.CODEC.listOf(), en.getActiveEffects().stream()
					.filter(e -> !e.getEffect().unwrapKey().get().equals(ModEffects.INTANGIBLE.getKey())).toList());

			for (MobEffectInstance eff : List.copyOf(en.getActiveEffects())) {
				if (!eff.getEffect().unwrapKey().get().equals(ModEffects.INTANGIBLE.getKey()))
					en.removeEffect(eff.getEffect());
			}
			LogUtils.getLogger().debug("Intangibilized " + en);
			new ParticleSpecification(ParticleTypes.SOUL_FIRE_FLAME, Vec3.ZERO, new Vec3(0.2, 0.2, 0.2), 0, 12, false,
					false).sendParticle((ServerLevel) level, en.position());
		}
	}

	@SubscribeEvent
	public void onRemoved(MobEffectEvent.Remove event) {
		if (event.getEntity().level() instanceof ServerLevel level) {
			if (event.getEffect() == this) {
				LivingEntity en = event.getEntity();
				var tag = ISoulState.get(en).extraData();
				en.setGlowingTag(false);
				if (event.getEntity() instanceof ServerPlayer player) {

					tag.read("priorGameMode", CodecUtils.caselessEnumCodec(GameType.class))
							.ifPresent(mode -> player.setGameMode(mode));
					tag.remove("priorGameMode");
				} else {
					en.setInvulnerable(tag.getBoolean("wasInvulnerableBefore").orElse(false));
					tag.remove("wasInvulnerableBefore");
					en.setInvisible(tag.getBooleanOr("wasInvisibleBefore", false));
					tag.remove("wasInvisibleBefore");
					en.setGlowingTag(tag.getBooleanOr("wasGlowingBefore", false));
					tag.remove("wasGlowingBefore");
					en.noPhysics = (tag.getBoolean("priorPhysics").orElse(false));
					tag.remove("priorPhysics");
				}
				for (MobEffectInstance eff : List.copyOf(en.getActiveEffects())) {
					if (!eff.getEffect().unwrapKey().get().equals(ModEffects.INTANGIBLE.getKey()))
						en.removeEffect(eff.getEffect());
				}
				tag.read("priorEffects", MobEffectInstance.CODEC.listOf())
						.ifPresent(effs -> effs.forEach(eff -> en.addEffect(eff)));
				tag.remove("priorEffects");

				LogUtils.getLogger().debug("Un-intangibilized " + en);

				new ParticleSpecification(ParticleTypes.END_ROD, Vec3.ZERO, new Vec3(0.2, 0.2, 0.2), 0, 12, false,
						false).sendParticle((ServerLevel) level, en.position());
			}
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int p_297908_, int p_301085_) {
		return true;
	}

}
