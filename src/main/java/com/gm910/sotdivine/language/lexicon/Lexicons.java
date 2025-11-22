package com.gm910.sotdivine.language.lexicon;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

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

public class Lexicons extends SimpleJsonResourceReloadListener<ILexicon> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private BiMap<ResourceLocation, ILexicon> LEXICONS = ImmutableBiMap.of();
	private static Optional<Lexicons> INSTANCE = Optional.empty();
	public static final ResourceLocation LANG_PATH = ModUtils.path("lexicon");
	private static Codec<ILexicon> CODEC;

	/**
	 * Codec to retrieve languages by resource location
	 */
	public static final Codec<ILexicon> BY_NAME_CODEC = ResourceLocation.CODEC.flatXmap((s) -> {
		ILexicon symbol = Lexicons.instance().LEXICONS.get(s);
		if (symbol == null) {
			return DataResult.error(() -> "No lexicon for " + s);
		}
		return DataResult.success(symbol);
	}, (s) -> {
		var rl = Lexicons.instance().LEXICONS.inverse().get(s);
		if (rl == null) {
			return DataResult.error(() -> "Unregistered lexicon: " + s.toString());
		}
		return DataResult.success(rl);
	});

	public static final Codec<ILexicon> lexiconCodec() {
		if (CODEC == null)
			CODEC = ILexicon.createCodec();
		return CODEC;
	}

	private Lexicons(Provider prov) {
		super(prov, lexiconCodec(), ModRegistries.LEXICONS);
	}

	@Override
	protected void apply(Map<ResourceLocation, ILexicon> map, ResourceManager rm, ProfilerFiller filler) {
		this.LEXICONS = HashBiMap.create(map);
		LOGGER.info("Loaded lexicons: {}", LEXICONS.values());
	}

	public static Lexicons instance() {
		return INSTANCE.get();
	}

	/**
	 * Get all LEXICONS
	 * 
	 * @return
	 */
	public BiMap<ResourceLocation, ILexicon> getLanguageMap() {
		return Maps.unmodifiableBiMap(LEXICONS);
	}

	/**
	 * So we can add this reloadable registry to the listener
	 * 
	 * @param event
	 */
	public static void eventAddListener(AddReloadListenerEvent event) {
		INSTANCE = Optional.of(new Lexicons(event.getRegistries()));
		event.addListener(INSTANCE.get());
	}
}
