package com.gm910.sotdivine.systems.deity.ritual.trigger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.gm910.sotdivine.systems.deity.ritual.RitualGeneration;
import com.gm910.sotdivine.systems.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.EntityGenreProvider;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

/**
 * A kind of ritual trigger, mainly for storage and registration
 * 
 * @param <T>
 */
public class RitualTriggerType<T extends IRitualTrigger> {

	private static final Map<ResourceLocation, RitualTriggerType<?>> TRIGGERS = new HashMap<>();

	/**
	 * A ritual triggered by a sacrificial act
	 */
	public static final RitualTriggerType<MobSacrificeTrigger> SACRIFICE = register(ModUtils.path("sacrifice"),
			MobSacrificeTrigger.class,
			() -> Codec.list(IGenreProvider.<EntityGenreProvider>castCodec(EntityGenreProvider.class))
					.xmap(MobSacrificeTrigger::new, MobSacrificeTrigger::sacrifice),
			(rg) -> {
				var list = Lists.newArrayList(rg.forDeity().spheres().stream()
						.flatMap((x) -> x.getGenres(GenreTypes.SACRED_MOB).stream()).iterator());
				if (list.size() == 0)
					return null;
				if (list.size() == 1)
					return new MobSacrificeTrigger(list);
				Collections.shuffle(list);
				int picked = rg.level().random.nextInt(1, list.size() + 1);
				return new MobSacrificeTrigger(Lists.newArrayList(list.stream().limit(picked).iterator()));
			});

	private static Codec<RitualTriggerType<?>> CODEC = null;

	private static Codec<IRitualTrigger> TRIGGER_CODEC = null;

	/**
	 * Returns the codec for ritual triggers as a type
	 * 
	 * @return
	 */
	public static Codec<RitualTriggerType<?>> typeCodec() {

		return (CODEC != null ? CODEC : (CODEC = ResourceLocation.CODEC.xmap(TRIGGERS::get, RitualTriggerType::path)));
	}

	/**
	 * Returns the codec for instances of {@link IRitualTrigger}
	 * 
	 * @return
	 */
	public static Codec<IRitualTrigger> instanceCodec() {
		return TRIGGER_CODEC != null ? TRIGGER_CODEC
				: (TRIGGER_CODEC = typeCodec().dispatch(IRitualTrigger::triggerType,
						(x) -> x.codec.get().fieldOf("trigger_type")));
	}

	public static final <T extends IRitualTrigger> RitualTriggerType<T> register(ResourceLocation path,
			Class<? super T> clazz, Supplier<Codec<T>> codec, Function<RitualGeneration, T> factory) {
		var newItem = new RitualTriggerType<>(path, clazz, codec, factory);
		if (TRIGGERS.containsKey(newItem.path)) {
			throw new IllegalArgumentException("Already present: " + path + ", trying to register " + newItem);
		}
		TRIGGERS.put(path, newItem);
		return newItem;
	}

	private Class<? super T> triggerClass;
	private Supplier<Codec<T>> codec;
	private ResourceLocation path;
	private Function<RitualGeneration, T> factory;

	private RitualTriggerType(ResourceLocation path, Class<? super T> triggerClass, Supplier<Codec<T>> codec,
			Function<RitualGeneration, T> factory) {
		this.triggerClass = triggerClass;
		this.codec = Suppliers.memoize(codec);
		this.path = path;
		this.factory = factory;
	}

	public ResourceLocation path() {
		return path;
	}

	public Class<? super T> triggerClass() {
		return triggerClass;
	}

	public Codec<T> codec() {
		return codec.get();
	}

	public T createNew(RitualGeneration info) {
		return this.factory.apply(info);
	}

	@Override
	public String toString() {
		return "RitualTriggerType<" + this.triggerClass.getSimpleName() + ">(" + this.path + ")";
	}

}
