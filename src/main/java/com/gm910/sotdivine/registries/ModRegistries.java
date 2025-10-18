package com.gm910.sotdivine.registries;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.language.LanguageGen;
import com.gm910.sotdivine.systems.deity.emanation.EmanationType;
import com.gm910.sotdivine.systems.deity.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.sphere.genres.IGenreType;
import com.gm910.sotdivine.systems.deity.symbol.IDeitySymbol;
import com.gm910.sotdivine.systems.party.resource.PartyResourceType;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ModRegistries {

	public static final ResourceKey<Registry<EmanationType<?>>> EMANATION_TYPES = ResourceKey
			.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "emanation_type"));
	public static final ResourceKey<Registry<ISphere>> SPHERES = ResourceKey
			.createRegistryKey(ModUtils.path("deity_sphere"));
	public static final ResourceKey<Registry<IGenreType<?>>> GENRE_TYPES = ResourceKey
			.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "genre_type"));
	public static final ResourceKey<Registry<PartyResourceType<?>>> PARTY_RESOURCE_TYPES = ResourceKey
			.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "resource_type"));
	public static final ResourceKey<Registry<LanguageGen>> LANGUAGE_GEN = ResourceKey
			.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "language_gen"));
	public static final ResourceKey<Registry<IRitualPattern>> RITUAL_PATTERN = ResourceKey
			.createRegistryKey(ModUtils.path("ritual_pattern"));
	public static final ResourceKey<Registry<IDeitySymbol>> DEITY_SYMBOLS = ResourceKey
			.createRegistryKey(ModUtils.path("deity_symbol"));

	private ModRegistries() {
	}

}
