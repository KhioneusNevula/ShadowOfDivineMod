package com.gm910.sotdivine.dimension.powers.types;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.dimension.powers.DimensionPowerType;
import com.gm910.sotdivine.dimension.powers.IDimensionPower;
import com.gm910.sotdivine.magic.impression.spell.ISpellEffect;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Power that stops entities from dying
 */
public enum UndyingPower implements IDimensionPower {
	INSTANCE;

	public static final MapCodec<UndyingPower> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public ISpellEffect invoke(ServerLevel level, Entity invoker, IDeity conduit, BlockPos pos) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void tick(IDeity creator, ServerLevel level, boolean spawnHostiles, boolean spawnFriendlies) {
	}

	@Override
	public boolean effectOnDeath(IDeity creator, LivingDeathEvent event) {
		if (event.getEntity().level().random.nextFloat() <= 0.2f) {
			return true;
		}
		return false;
	}

	@Override
	public Importance getImportance() {
		return Importance.EITHER;
	}

	@Override
	public DimensionPowerType<?> getDimensionPowerType() {
		return DimensionPowerType.UNDYING.get();
	}

	@Override
	public Component dimensionName() {
		return Component.literal("ImmortalityWorld");
	}

	@Override
	public String dimensionPath() {
		return "immortality_world";
	}

}
