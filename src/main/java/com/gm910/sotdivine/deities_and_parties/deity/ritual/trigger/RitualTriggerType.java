package com.gm910.sotdivine.deities_and_parties.deity.ritual.trigger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.gm910.sotdivine.deities_and_parties.deity.ritual.RitualGeneration;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.EntityGenreProvider;
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

	public static final Collection<RitualTriggerType<?>> getAllTypes() {
		return Collections.unmodifiableCollection(TRIGGERS.values());
	}

	/**
	 * A ritual triggered by a sacrificial act
	 */
	public static final RitualTriggerType<MobSacrificeTrigger> SACRIFICE = register(ModUtils.path("sacrifice"),
			MobSacrificeTrigger.class,
			() -> Codec.list(IGenreProvider.<EntityGenreProvider>castCodec(EntityGenreProvider.class))
					.xmap(MobSacrificeTrigger::new, MobSacrificeTrigger::sacrifice),
			(rg) -> {
				var list = Lists.newArrayList(
						rg.forDeity().spheres().stream().flatMap((x) -> x.getGenres(GenreTypes.SACRED_MOB).stream())
								.filter((s) -> s.rarity() <= rg.quality().rarity).iterator());
				if (list.size() == 0)
					return null;
				if (list.size() == 1)
					return new MobSacrificeTrigger(list);
				Collections.shuffle(list);
				int picked = rg.level().random.nextInt(1, list.size() + 1);
				return new MobSacrificeTrigger(Lists.newArrayList(list.stream().limit(picked).iterator()));
			});

	/**
	 * Returns a trigger that triggers when an offering is offered
	 */
	public static final RitualTriggerType<OfferItemTrigger> OFFER_ITEM = register(ModUtils.path("offer_item"),
			OfferItemTrigger.class, () -> Codec.unit(OfferItemTrigger.INSTANCE), (rg) -> OfferItemTrigger.INSTANCE);

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

	/**
	 * Factory is permitted to return null if ritual generation parameters are
	 * problematic
	 * 
	 * @param <T>
	 * @param path
	 * @param clazz
	 * @param codec
	 * @param factory
	 * @return
	 */
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
