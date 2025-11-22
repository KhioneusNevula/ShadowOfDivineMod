package com.gm910.sotdivine.language;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;

public class Languages extends SimpleJsonResourceReloadListener<ILanguage> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private BiMap<ResourceLocation, ILanguage> LANGUAGES = ImmutableBiMap.of();
	private static Optional<Languages> INSTANCE = Optional.empty();
	public static final ResourceLocation LANG_PATH = ModUtils.path("language");
	private static Codec<ILanguage> CODEC;

	/**
	 * Codec to retrieve languages by resource location
	 */
	public static final Codec<ILanguage> BY_NAME_CODEC = ResourceLocation.CODEC.flatXmap((s) -> {
		ILanguage symbol = Languages.instance().LANGUAGES.get(s);
		if (symbol == null) {
			return DataResult.error(() -> "No language for " + s);
		}
		return DataResult.success(symbol);
	}, (s) -> {
		var rl = Languages.instance().LANGUAGES.inverse().get(s);
		if (rl == null) {
			return DataResult.error(() -> "Unregistered language: " + s.toString());
		}
		return DataResult.success(rl);
	});

	public static final Codec<ILanguage> languageCodec() {
		if (CODEC == null)
			CODEC = ILanguage.createCodec();
		return CODEC;
	}

	private Languages(Provider prov) {
		super(prov, languageCodec(), ModRegistries.LANGUAGES);
	}

	@Override
	protected void apply(Map<ResourceLocation, ILanguage> map, ResourceManager rm, ProfilerFiller filler) {
		this.LANGUAGES = HashBiMap.create(map);
		LOGGER.info("Loaded languages: {}", LANGUAGES.values());
	}

	public static Languages instance() {
		return INSTANCE.get();
	}

	/**
	 * Get all LANGUAGES
	 * 
	 * @return
	 */
	public BiMap<ResourceLocation, ILanguage> getLanguageMap() {
		return Maps.unmodifiableBiMap(LANGUAGES);
	}

	/**
	 * Return a language using a string code
	 * 
	 * @param code
	 * @return
	 */
	public Optional<ILanguage> get(String code) {
		return Optional.ofNullable(LANGUAGES.get(ResourceLocation.withDefaultNamespace(code)));
	}

	/**
	 * So we can add this reloadable registry to the listener
	 * 
	 * @param event
	 */
	public static void eventAddListener(AddReloadListenerEvent event) {
		INSTANCE = Optional.of(new Languages(event.getRegistries()));
		event.addListener(INSTANCE.get());
	}
}
