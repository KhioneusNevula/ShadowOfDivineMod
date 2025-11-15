package com.gm910.sotdivine.magic.ritual.pattern;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
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

	private double maxPatternDiameter;

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
		this.maxPatternDiameter = this.ritualPatterns.values().stream().mapToDouble((s) -> Math.sqrt(
				s.minPos().distToLowCornerSqr(s.maxPos().getX() + 1, s.maxPos().getY() + 1, s.maxPos().getZ() + 1)))
				.max().orElse(IPartySystem.SYMBOL_SEARCH_RADIUS);

		LOGGER.info("Loaded ritual patterns: {}", ritualPatterns);
	}

	public static RitualPatterns instance() {
		return INSTANCE.get();
	}

	public static void init() {

	}

	/**
	 * Return random ritual pattern with less than the given number of blocks,
	 * preferring patterns with more blocks
	 * 
	 * @param source
	 * @param pred   if this is specified, only include those patterns
	 * @return
	 */
	public IRitualPattern getRandom(RandomSource source, int maxBlocks, Predicate<IRitualPattern> pred) {
		return WeightedSet.getRandom(
				ritualPatterns.values().stream()
						.filter((s) -> s.blockCount() <= maxBlocks && (pred == null ? true : pred.test(s))).toList(),
				(s) -> Math.pow(2, s.blockCount()), source);
	}

	/**
	 * Get all ritualPatterns
	 * 
	 * @return
	 */
	public BiMap<ResourceLocation, IRitualPattern> getPatternMap() {
		return Maps.unmodifiableBiMap(ritualPatterns);
	}

	/**
	 * Return the diameter of the pattern of maximum size
	 * 
	 * @return
	 */
	public double getMaxPatternDiameter() {
		return maxPatternDiameter;
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
