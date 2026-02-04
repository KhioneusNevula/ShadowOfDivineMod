package com.gm910.sotdivine.mixins_assist.block;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.gm910.sotdivine.magic.afterlife.IAfterlife;
import com.gm910.sotdivine.magic.afterlife.anchors.BlockAnchor;
import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;
import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMixinHelper {
	private BlockMixinHelper() {
	}

	public static void onPlace(Level lo, BlockPos pos, BlockState oldState, boolean p_60700_) {
		if (lo instanceof ServerLevel level) {
			if (ISanctuarySystem.getPossibleFirstBlock(pos, level) instanceof BlockPos startPos) {
				ISanctuarySystem system = ISanctuarySystem.get(level);
				if (system.getSanctuaryAtPos(startPos).orElse(null) instanceof ISanctuary exPos) {
					system.reaffirmSanctuary(level, exPos);
				} else {
					system.addSanctuary(ISanctuary.initiate(level, startPos, null));
					LogUtils.getLogger().debug("Placed first block in creating sanctuary ");
				}
			} else {

			}

		}
	}

	public static void onStateChange(LevelReader lo, BlockPos pos, BlockState oldState) {
		if (lo instanceof ServerLevel level) {
			ISanctuarySystem system = ISanctuarySystem.get(level);
			if (system.getSanctuaryAtPos(pos).orElse(null) instanceof ISanctuary exPos) {
				LogUtils.getLogger().debug("broke a block in sanctuary ");
				system.reaffirmSanctuary(level, exPos);
			}
			IAfterlife afterlife = IAfterlife.get(level);
			Set<BlockAnchor> toDelete = new HashSet<>();
			afterlife.getAnchors(GlobalPos.of(level.dimension(), pos)).stream()
					.flatMap(ip -> ip instanceof BlockAnchor ba ? Stream.of(ba) : Stream.empty()).forEach(ip -> {
						if (ip.getAnchor(level).isEmpty()) {
							toDelete.add(ip);
						}
					});
			toDelete.forEach(ip -> afterlife.destroyAnchor(ip));
		}
	}

}
