package com.gm910.sotdivine.concepts.symbol;

import java.util.Optional;

import com.gm910.sotdivine.magic.sphere.ISphere;
import com.gm910.sotdivine.util.HolderUtils;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;

record DeitySymbol(Holder<BannerPattern> bannerPattern, Optional<HolderSet<ISphere>> preferredSpheres,
		Optional<HolderSet<ISphere>> allowedSpheres, Optional<HolderSet<ISphere>> forbiddenSpheres,
		Optional<HolderSet<Block>> effigies) implements IDeitySymbol {
	@Override
	public Holder<BannerPattern> bannerPattern() {
		return bannerPattern;
	}

	@Override
	public Optional<HolderSet<ISphere>> allowedSpheres() {
		return allowedSpheres;
	}

	@Override
	public Optional<HolderSet<ISphere>> forbiddenSpheres() {
		return forbiddenSpheres;
	}

	@Override
	public Optional<HolderSet<ISphere>> preferredSpheres() {
		return preferredSpheres;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == (obj))
			return true;
		if (obj instanceof DeitySymbol sym) {
			return this.bannerPattern.is(sym.bannerPattern)
					&& HolderUtils.optionalEquals(this.preferredSpheres, sym.preferredSpheres,
							HolderUtils::holderSetEquals)
					&& HolderUtils.optionalEquals(this.allowedSpheres, sym.allowedSpheres, HolderUtils::holderSetEquals)
					&& HolderUtils.optionalEquals(this.forbiddenSpheres, sym.forbiddenSpheres,
							HolderUtils::holderSetEquals);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HolderUtils.holderHashCode(this.bannerPattern)
				+ this.preferredSpheres.map(HolderUtils::holderSetHashCode).orElse(0)
				+ this.allowedSpheres.map(HolderUtils::holderSetHashCode).orElse(0)
				+ this.forbiddenSpheres.map(HolderUtils::holderSetHashCode).orElse(0);
	}

	@Override
	public String toString() {
		return (DeitySymbols.instance() == null
				|| !DeitySymbols.instance().getDeitySymbolMap().inverse().containsKey(this))
						? "Symbol{pattern=" + this.bannerPattern
								+ (this.preferredSpheres.map((x) -> ",preferred=" + x).orElse(""))
								+ (this.allowedSpheres.map((x) -> ",allowed=" + x).orElse(""))
								+ (this.forbiddenSpheres.map((x) -> ",forbidden=" + x).orElse("")) + "}"
						: "Symbol(" + DeitySymbols.instance().getDeitySymbolMap().inverse().get(this) + ")";
	}
}
