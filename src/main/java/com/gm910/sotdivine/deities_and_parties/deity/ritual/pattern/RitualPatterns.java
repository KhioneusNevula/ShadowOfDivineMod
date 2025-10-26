package com.gm910.sotdivine.deities_and_parties.deity.ritual.pattern;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.gm910.sotdivine.registries.ModRegistries;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;

public class RitualPatterns extends SimpleJsonResourceReloadListener<IRitualPattern> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private BiMap<ResourceLocation, IRitualPattern> ritualPatterns = ImmutableBiMap.of();
	private static Optional<RitualPatterns> INSTANCE = Optional.empty();

	private static Codec<IRitualPattern> CODEC;

	public static final Codec<IRitualPattern> patternCodec() {
		if (CODEC == null)
			CODEC = RitualPattern.READER_CODEC;
		return CODEC;
	}

	private RitualPatterns(Provider prov) {
		super(prov, patternCodec(), ModRegistries.RITUAL_PATTERN);

	}

	@Override
	protected Map<ResourceLocation, IRitualPattern> prepare(ResourceManager man, ProfilerFiller p_10772_) {

		return super.prepare(man, p_10772_);
	}

	@Override
	protected void apply(Map<ResourceLocation, IRitualPattern> map, ResourceManager rm, ProfilerFiller p_10795_) {
		this.ritualPatterns = HashBiMap.create(map);

		LOGGER.info("Loaded ritual patterns: {}", ritualPatterns.values());
	}

	public static RitualPatterns instance() {
		return INSTANCE.get();
	}

	public static void init() {

	}

	/**
	 * Return random ritual pattern with less than the given number of blocks
	 * 
	 * @param source
	 * @return
	 */
	public IRitualPattern getRandom(RandomSource source, int maxBlocks) {
		return WeightedSet.getRandom(
				ritualPatterns.values().stream().filter((s) -> s.blockCount() <= maxBlocks).toList(), source);
	}

	/**
	 * Get all ritualPatterns
	 * 
	 * @return
	 */
	public BiMap<ResourceLocation, IRitualPattern> getPatternMap() {
		return Maps.unmodifiableBiMap(ritualPatterns);
	}

	public IRitualPattern RitualPattern(ResourceLocation location) {
		return ritualPatterns.get(location);
	}

	public ResourceLocation rl(IRitualPattern from) {
		return ritualPatterns.inverse().get(from);
	}

	/**
	 * So we can add this reloadable registry to the listener
	 * 
	 * @param event
	 */
	public static void eventAddListener(AddReloadListenerEvent event) {
		INSTANCE = Optional.of(new RitualPatterns(event.getRegistries()));
		event.addListener(INSTANCE.get());
	}

}
