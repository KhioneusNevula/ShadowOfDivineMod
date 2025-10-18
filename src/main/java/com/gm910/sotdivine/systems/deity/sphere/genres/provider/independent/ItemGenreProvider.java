package com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.systems.deity.sphere.genres.creator.ItemStackCreator;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.data.ComponentMapProvider;
import com.gm910.sotdivine.util.HolderUtils;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A genre for items
 */
public record ItemGenreProvider(HolderSet<Item> items, ComponentMapProvider components)
		implements IGiveableGenreProvider<ItemStack, ItemStackCreator> {

	private static Codec<ItemGenreProvider> CODEC = null;

	public static final Codec<ItemGenreProvider> codec() {
		if (CODEC == null) {

			Codec<ItemGenreProvider> registryCodec = RegistryCodecs.homogeneousList(Registries.ITEM)
					.flatComapMap((p) -> new ItemGenreProvider(p, ComponentMapProvider.ANY), (p) -> {

						if (p.components.isEmpty()) {
							return DataResult.success(p.items);
						}
						return DataResult.error(() -> "No point in converting from complex item provider into item set",
								p.items);
					});
			Codec<ItemGenreProvider> stackCodec = ItemStack.CODEC.flatComapMap(
					(p) -> new ItemGenreProvider(HolderSet.direct(p.getItemHolder()),
							new ComponentMapProvider(DataComponentExactPredicate.allOf(p.getComponents()), Map.of())),
					(p) -> DataResult.error(() -> "No point in converting from item provider " + p + " to item stack"));
			Codec<ItemGenreProvider> registryOrStackCodec = Codec.either(registryCodec, stackCodec).xmap(Either::unwrap,
					(s) -> Either.right(s));
			Codec<ItemGenreProvider> constructionCodec = RecordCodecBuilder.create((instance) -> instance
					.group(RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items")
							.forGetter(ItemGenreProvider::items),
							ComponentMapProvider.codec().forGetter(ItemGenreProvider::components))
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
		return HolderUtils.holderSetContains(items, instance.getItemHolder()) && components.matches(level, instance);
	}

	@Override
	public boolean matchesItem(ServerLevel level, ItemStack stack) {
		return this.matchesItem(level, stack);
	}

	@Override
	/**
	 * Generates a stack of 1 item
	 */
	public ItemStackCreator generateRandom(ServerLevel level, Optional<ItemStack> stackPrev) {

		Optional<Holder<Item>> item = Optional
				.ofNullable(new WeightedSet<Holder<Item>>(items.stream().filter(Holder::isBound),
						(i) -> 1f / (i.get().getDefaultInstance().getRarity().ordinal() + 1)).get(level.random));

		if (item.isEmpty() || !item.get().isBound()) {
			LogUtils.getLogger().warn("No item generateable..." + item);
			return ItemStackCreator.EMPTY;
		}
		if (stackPrev.isPresent() && stackPrev.get().getCount() == 0) {
			stackPrev = Optional.empty();
		}
		LogUtils.getLogger().debug("Making stack from item " + item + "...");

		ItemStack stack;
		if (stackPrev.isEmpty()) {
			stack = new ItemStack(item.get());
			LogUtils.getLogger().debug("Made fresh stack...");
		} else {
			stack = new ItemStack(item.get(), stackPrev.get().getCount(), stackPrev.get().getComponentsPatch());
			LogUtils.getLogger().debug("Made copied stack... " + stack);
		}
		DataComponentMap map = components.generateRandom(level, Optional.of(stack));
		LogUtils.getLogger().debug("Setting data for " + stack + " to " + map);
		stack.applyComponents(map);

		LogUtils.getLogger().debug("Data of " + stack + " is now " + stack.getComponents());
		return new ItemStackCreator(stack);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ItemGenreProvider igp) {
			if (obj == this)
				return true;
			return HolderUtils.holderSetEquals(this.items, igp.items) && this.components.equals(igp.components);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return HolderUtils.holderSetHashCode(items) + components.hashCode();
	}

	@Override
	public String toString() {
		return "ItemGenreProvider(" + items.unwrap().map((tg) -> tg.toString(), (ls) -> ls.toString()) + ")";
	}

	@Override
	public String report() {
		return "ItemGenreProvider{items=" + items.unwrap().map((tg) -> tg.toString(), (ls) -> ls.toString())
				+ (components.isEmpty() ? "}" : ",components=" + components);
	}

}
