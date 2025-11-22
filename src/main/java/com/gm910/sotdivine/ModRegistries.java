package com.gm910.sotdivine;

import com.gm910.sotdivine.concepts.genres.IGenreType;
import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.language.ILanguage;
import com.gm910.sotdivine.language.lexicon.ILexicon;
import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.magic.emanation.EmanationType;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.sphere.ISphere;
import com.gm910.sotdivine.util.ModUtils;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ModRegistries {

	public static final ResourceKey<Registry<EmanationType<?>>> EMANATION_TYPES = ResourceKey
			.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "emanation_type"));
	public static final ResourceKey<Registry<ISphere>> SPHERES = ResourceKey.createRegistryKey(ModUtils.path("sphere"));
	public static final ResourceKey<Registry<IGenreType<?>>> GENRE_TYPES = ResourceKey
			.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "genre_type"));
	public static final ResourceKey<Registry<PartyResourceType<?>>> PARTY_RESOURCE_TYPES = ResourceKey
			.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, "resource_type"));
	public static final ResourceKey<Registry<IRitualPattern>> RITUAL_PATTERN = ResourceKey
			.createRegistryKey(ModUtils.path("ritual_pattern"));
	public static final ResourceKey<Registry<IDeitySymbol>> DEITY_SYMBOLS = ResourceKey
			.createRegistryKey(ModUtils.path("symbol"));

	public static final ResourceKey<Registry<ILanguage>> LANGUAGES = ResourceKey
			.createRegistryKey(ModUtils.path("language"));

	public static final ResourceKey<Registry<ILexicon>> LEXICONS = ResourceKey
			.createRegistryKey(ModUtils.path("lexicon"));

	public static final ResourceKey<Registry<IPhonology>> PHONOLOGIES = ResourceKey
			.createRegistryKey(ModUtils.path("phonology"));

	private ModRegistries() {
	}

}
