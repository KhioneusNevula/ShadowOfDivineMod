package com.gm910.sotdivine.concepts.symbol;

import java.util.Optional;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.magic.sphere.ISphere;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * A symbol -- generally, a texture -- which can be used to represent a deity
 */
public interface IDeitySymbol {

	public static Codec<IDeitySymbol> createCodec() {
		return RecordCodecBuilder.create(instance -> // Given an emanation
		instance.group(
				RegistryFixedCodec.create(Registries.BANNER_PATTERN).fieldOf("banner_pattern")
						.forGetter(IDeitySymbol::bannerPattern),
				RegistryCodecs.homogeneousList(ModRegistries.SPHERES).optionalFieldOf("preferred_spheres")
						.forGetter(IDeitySymbol::preferredSpheres),
				RegistryCodecs.homogeneousList(ModRegistries.SPHERES).optionalFieldOf("allowed_spheres")
						.forGetter(IDeitySymbol::allowedSpheres),
				RegistryCodecs.homogeneousList(ModRegistries.SPHERES).optionalFieldOf("forbidden_spheres")
						.forGetter(IDeitySymbol::forbiddenSpheres),
				RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("effigy")
						.forGetter(IDeitySymbol::effigies)

		).apply(instance, (bp, s, s2, s3, j) -> new DeitySymbol(bp, s, s2, s3, j)));
	}

	public Holder<BannerPattern> bannerPattern();

	/**
	 * Returns the set of blocks that function as effigies for this symbol. (It is
	 * assumed these effigy blocks must be instances of SymbolReliefBlock)
	 * 
	 * @return
	 */
	public Optional<HolderSet<Block>> effigies();

	/**
	 * Takes precedence over forbidden spheres
	 * 
	 * @return
	 */
	public Optional<HolderSet<ISphere>> allowedSpheres();

	public Optional<HolderSet<ISphere>> forbiddenSpheres();

	/**
	 * Takes precedence over both allowed and forbidden spheres
	 * 
	 * @return
	 */
	public Optional<HolderSet<ISphere>> preferredSpheres();

	/**
	 * return 0 if not permitted, 1 if permitted, and 2 if preferred
	 * 
	 * @param sphere
	 * @return
	 */
	public default int permittedOrPreferred(ISphere sphere) {

		if (this.preferredSpheres().isPresent()
				&& this.preferredSpheres().get().stream().map(Holder::get).anyMatch(sphere::equals)) {
			return 2;
		}
		if (this.allowedSpheres().isPresent()
				&& this.allowedSpheres().get().stream().map(Holder::get).anyMatch(sphere::equals)) {
			return 1;
		}
		if (this.forbiddenSpheres().isPresent()
				&& this.forbiddenSpheres().get().stream().map(Holder::get).anyMatch(sphere::equals)) {
			return 0;
		}
		return 1;
	}
}
