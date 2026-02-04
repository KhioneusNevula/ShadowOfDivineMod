package com.gm910.sotdivine.dimension.powers.types;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.dimension.powers.DimensionPowerType;
import com.gm910.sotdivine.dimension.powers.IDimensionPower;
import com.gm910.sotdivine.magic.afterlife.IAfterlife;
import com.gm910.sotdivine.magic.afterlife.ISoulState.LifeState;
import com.gm910.sotdivine.magic.afterlife.Soul;
import com.gm910.sotdivine.magic.afterlife.anchors.SoulAnchor;
import com.gm910.sotdivine.magic.impression.spell.ISpellEffect;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Power that stops entities from dying
 */
public enum GhostResurrectionPower implements IDimensionPower {
	INSTANCE;

	public static final MapCodec<GhostResurrectionPower> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public ISpellEffect invoke(ServerLevel level, Entity invoker, IDeity conduit, BlockPos pos) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void tick(IDeity creator, ServerLevel level, boolean spawnHostiles, boolean spawnFriendlies) {
		if (level.getGameTime() % (5 * 20) == 0) {
			if (level.random.nextFloat() <= 0.2f) {
				if (level.getRandomPlayer() instanceof ServerPlayer player) {
					// LogUtils.getLogger().debug("Trying to spawn ghosts for " + player);
					IAfterlife afterlife = IAfterlife.get(level);
					SoulAnchor anchor = SoulAnchor.from(Soul.createSoulFromEntityUnsafely(player, afterlife));
					var optional = level.random.nextFloat() >= 0.2f
							? afterlife.peekSignificantSoul(anchor).or(() -> afterlife.peekNonsignificantSoul(anchor))
							: afterlife.peekNonsignificantSoul(anchor).or(() -> afterlife.peekSignificantSoul(anchor));
					optional.ifPresent(so -> {
						afterlife.extractEntity(so, level, LifeState.ghost).ifPresent(entity -> {
							LogUtils.getLogger().debug("Raising ghost of " + entity);
							entity.setPos(player.position().add(player.getLookAngle().multiply(-5, -5, -5)));
							level.addFreshEntityWithPassengers(entity);
						});
					});
				}
			}
		}
	}

	@Override
	public Importance getImportance() {
		return Importance.EITHER;
	}

	@Override
	public DimensionPowerType<?> getDimensionPowerType() {
		return DimensionPowerType.GHOST_RESURRECTION.get();
	}

	@Override
	public Component dimensionName() {
		return Component.literal("Afterlife");
	}

	@Override
	public String dimensionPath() {
		return "afterlife";
	}

}
