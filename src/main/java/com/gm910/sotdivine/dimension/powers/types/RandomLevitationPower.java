package com.gm910.sotdivine.dimension.powers.types;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.dimension.powers.DimensionPowerType;
import com.gm910.sotdivine.dimension.powers.IDimensionPower;
import com.gm910.sotdivine.magic.impression.spell.ISpellEffect;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;

/**
 * Debug power to make random entities levitate
 */
public enum RandomLevitationPower implements IDimensionPower {
	INSTANCE;

	public static final MapCodec<RandomLevitationPower> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public ISpellEffect invoke(ServerLevel level, Entity invoker, IDeity conduit, BlockPos pos) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void tick(IDeity creator, ServerLevel level, boolean spawnHostiles, boolean spawnFriendlies) {

	}

	@Override
	public void effectOnLivingTick(IDeity creator, LivingTickEvent event) {
		ServerLevel level = (ServerLevel) event.getEntity().level();
		if (level.getGameTime() % 100 == 0) {
			if (level.random.nextFloat() <= 0.4) {
				if (!event.getEntity().hasEffect(MobEffects.LEVITATION)) {
					event.getEntity().addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20 * 10));
				}
			}
		}
	}

	@Override
	public Importance getImportance() {
		return Importance.SECONDARY;
	}

	@Override
	public DimensionPowerType<?> getDimensionPowerType() {
		return DimensionPowerType.RANDOM_LEVITATION.get();
	}

	@Override
	public Component dimensionName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String dimensionPath() {
		throw new UnsupportedOperationException();
	}

}
