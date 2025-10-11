package com.gm910.sotdivine.systems.deity.symbol;

import java.util.Optional;

import com.gm910.sotdivine.systems.deity.sphere.ISphere;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.block.entity.BannerPattern;

public record DeitySymbol(Holder<BannerPattern> bannerPattern, Optional<HolderSet<ISphere>> preferredSpheres,
		Optional<HolderSet<ISphere>> allowedSpheres, Optional<HolderSet<ISphere>> forbiddenSpheres)
		implements IDeitySymbol {

}
