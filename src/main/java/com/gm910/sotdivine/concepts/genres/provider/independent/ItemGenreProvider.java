package com.gm910.sotdivine.concepts.genres.provider.independent;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.creator.ItemStackCreator;
import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.concepts.genres.provider.data.ComponentMapProvider;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.HolderUtils;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.gm910.sotdivine.util.WeightedSet;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * A genre for items
 */
public record ItemGenreProvider(Optional<HolderSet<Item>> items, ComponentMapProvider components, float rarity)
		implements IGiveableGenreProvider<ItemStack, ItemStackCreator> {

	private static Codec<ItemGenreProvider> CODEC = null;

	public static final Codec<ItemGenreProvider> codec() {
		if (CODEC == null) {

			Codec<ItemGenreProvider> registryCodec = RegistryCodecs.homogeneousList(Registries.ITEM)
					.flatComapMap((p) -> new ItemGenreProvider(Optional.of(p), ComponentMapProvider.ANY,
							(float) p.stream().map(Holder::get)
									.mapToDouble((i) -> i.getDefaultInstance().getRarity().ordinal() + 0.0).min()
									.orElse(0.0)),
							(p) -> {

								if (p.components.isEmpty()) {
									return DataResult.success(p.items.get());
								}
								return DataResult.error(
										() -> "No point in converting from complex item provider into item set",
										p.items.get());
							});
			Codec<ItemGenreProvider> stackCodec = ItemStack.CODEC
					.flatComapMap(
							(p) -> new ItemGenreProvider(Optional.of(HolderSet.direct(p.getItemHolder())),
									new ComponentMapProvider(DataComponentExactPredicate.allOf(p.getComponents()),
											Map.of()),
									p.getRarity().ordinal() + 0.0f),
							(p) -> DataResult
									.error(() -> "No point in converting from item provider " + p + " to item stack"));
			Codec<ItemGenreProvider> registryOrStackCodec = Codec.either(registryCodec, stackCodec).xmap(Either::unwrap,
					(s) -> Either.right(s));
			Codec<ItemGenreProvider> constructionCodec = RecordCodecBuilder
					.create((
							instance) -> instance
									.group(RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items")
											.forGetter(ItemGenreProvider::items),
											ComponentMapProvider.codec().forGetter(ItemGenreProvider::components),
											CodecUtils.enumFloatScaleCodec(Rarity.class).optionalFieldOf("rarity", 0.0f)
													.forGetter(ItemGenreProvider::rarity))
									.apply(instance, ItemGenreProvider::new));
			CODEC = Codec.either(registryOrStackCodec, constructionCodec).xmap(Either::unwrap, (s) -> Either.right(s));
		}
		return CODEC;
	}

	@Override
	public ProviderType<ItemGenreProvider> providerType() {
		return ProviderType.ITEM;
	}

	@Override
	public boolean matches(ServerLevel level, ItemStack instance) {
		return (items.isEmpty() ? true : HolderUtils.holderSetContains(items.get(), instance.getItemHolder()))
				&& components.matches(level, instance);
	}

	@Override
	public boolean matchesItem(ServerLevel level, ItemStack stack) {
		return this.matches(level, stack);
	}

	@Override
	/**
	 * Generates a stack of 1 item
	 */
	public ItemStackCreator generateRandom(ServerLevel level, Optional<ItemStack> stackPrev) {
		var itemreg = level.registryAccess().lookupOrThrow(Registries.ITEM);
		Optional<Holder<Item>> item = Optional.ofNullable(new WeightedSet<Holder<Item>>(
				(items.isEmpty() ? itemreg.stream().map((s) -> s.builtInRegistryHolder()) : items.get().stream())
						.filter(Holder::isBound),
				(i) -> 1f / (i.get().getDefaultInstance().getRarity().ordinal() + 1)).get(level.random));

		if (item.isEmpty() || !item.get().isBound()) {
			// LogUtils.getLogger().warn("No item generateable..." + item);
			return ItemStackCreator.EMPTY;
		}
		if (stackPrev.isPresent() && stackPrev.get().getCount() == 0) {
			stackPrev = Optional.empty();
		}
		// LogUtils.getLogger().debug("Making stack from item " + item + "...");

		ItemStack stack;
		if (stackPrev.isEmpty()) {
			stack = new ItemStack(item.get());
			// LogUtils.getLogger().debug("Made fresh stack...");
		} else {
			stack = new ItemStack(item.get(), stackPrev.get().getCount(), stackPrev.get().getComponentsPatch());
			// LogUtils.getLogger().debug("Made copied stack... " + stack);
		}
		DataComponentMap map = components.generateRandom(level, Optional.of(stack));
		// LogUtils.getLogger().debug("Setting data for " + stack + " to " + map);
		stack.applyComponents(map);

		// LogUtils.getLogger().debug("Data of " + stack + " is now " +
		// stack.getComponents());
		return new ItemStackCreator(stack);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ItemGenreProvider igp) {
			if (obj == this)
				return true;
			return HolderUtils.optionalEquals(this.items, igp.items, HolderUtils::holderSetEquals)
					&& this.components.equals(igp.components);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return items.map((items) -> HolderUtils.holderSetHashCode(items)).hashCode() + components.hashCode();
	}

	@Override
	public Component translate() {
		return items
				.map((items) -> items.unwrap().map(
						(tg) -> TextUtils.transPrefix("genre.provider.item.tag", ModUtils.toShortString(tg.location())),
						(ls) -> TextUtils.transPrefix("genre.provider.item.list",
								ls.stream().map((g) -> TextUtils.transPrefix("cmd.quote", g.get().getName()))
										.collect(CollectionUtils.componentCollectorCommasPretty()))))
				.orElse(TextUtils.transPrefix("genre.provider.item.data", this.components.translate()));
	}

	@Override
	public String toString() {
		return "Item("
				+ items.map((items) -> items.unwrap().map((tg) -> "#" + ModUtils.toShortString(tg.location()),
						(ls) -> ls.stream().map((g) -> ModUtils.toShortString(BuiltInRegistries.ITEM.getKey(g.get())))
								.collect(CollectionUtils.setStringCollector(","))))
						.orElse("<any>" + this.components + "")
				+ ")";
	}

	@Override
	public String report() {
		return "Item{items="
				+ items.map(items -> items.unwrap().map((tg) -> "#" + ModUtils.toShortString(tg.location()),
						(ls) -> ls.stream().map((s) -> ModUtils.toShortString(BuiltInRegistries.ITEM.getKey(s.get())))
								.collect(CollectionUtils.setStringCollector(","))))
						.orElse("<any>")
				+ (rarity != 0 ? ",rarity=" + rarity : "") + ",components=" + components.report() + "}";
	}

}
