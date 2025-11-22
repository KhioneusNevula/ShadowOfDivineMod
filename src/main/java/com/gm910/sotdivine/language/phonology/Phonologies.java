package com.gm910.sotdivine.language.phonology;

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

public class Phonologies extends SimpleJsonResourceReloadListener<IPhonology> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private BiMap<ResourceLocation, IPhonology> PHONOLOGIES = ImmutableBiMap.of();
	private static Optional<Phonologies> INSTANCE = Optional.empty();
	public static final ResourceLocation LANG_PATH = ModUtils.path("phonology");
	private static Codec<IPhonology> CODEC;

	/**
	 * Codec to retrieve languages by resource location
	 */
	public static final Codec<IPhonology> BY_NAME_CODEC = ResourceLocation.CODEC.flatXmap((s) -> {
		IPhonology symbol = Phonologies.instance().PHONOLOGIES.get(s);
		if (symbol == null) {
			return DataResult.error(() -> "No phonology for " + s);
		}
		return DataResult.success(symbol);
	}, (s) -> {
		var rl = Phonologies.instance().PHONOLOGIES.inverse().get(s);
		if (rl == null) {
			return DataResult.error(() -> "Unregistered phonology: " + s.toString());
		}
		return DataResult.success(rl);
	});

	public static final Codec<IPhonology> phonologyCodec() {
		if (CODEC == null)
			CODEC = IPhonology.createCodec();
		return CODEC;
	}

	private Phonologies(Provider prov) {
		super(prov, phonologyCodec(), ModRegistries.PHONOLOGIES);
	}

	@Override
	protected void apply(Map<ResourceLocation, IPhonology> map, ResourceManager rm, ProfilerFiller filler) {
		this.PHONOLOGIES = HashBiMap.create(map);
		LOGGER.info("Loaded phonologies: {}", PHONOLOGIES);
	}

	public static Phonologies instance() {
		return INSTANCE.get();
	}

	/**
	 * Get all PHONOLOGIES
	 * 
	 * @return
	 */
	public BiMap<ResourceLocation, IPhonology> getLanguageMap() {
		return Maps.unmodifiableBiMap(PHONOLOGIES);
	}

	/**
	 * So we can add this reloadable registry to the listener
	 * 
	 * @param event
	 */
	public static void eventAddListener(AddReloadListenerEvent event) {
		INSTANCE = Optional.of(new Phonologies(event.getRegistries()));
		event.addListener(INSTANCE.get());
	}
}
