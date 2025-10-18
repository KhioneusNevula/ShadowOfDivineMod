package com.gm910.sotdivine.systems.deity.sphere.genres.provider.data.components;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.util.RandomUtils;
import com.gm910.sotdivine.util.StreamUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;

import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;

public record EnchantmentsComponentMatcher(List<EnchantmentPredicate> enchantments)
		implements IComponentMatcherProvider<ItemEnchantments> {

	public static final ResourceLocation PATH = ResourceLocation.withDefaultNamespace("enchantments");

	public EnchantmentsComponentMatcher(List<EnchantmentPredicate> enchantments) {
		this.enchantments = List.copyOf(enchantments);

	}

	@Override
	public boolean matches(ServerLevel level, DataComponentGetter instance) {
		return EnchantmentsPredicate.enchantments(enchantments).matches(instance);
	}

	@Override
	public TypedDataComponent<ItemEnchantments> generateRandom(ServerLevel level, Optional<DataComponentGetter> prior) {
		var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		// LogUtils.getLogger().debug("Finding random enchantment");
		Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		for (EnchantmentPredicate predicate : enchantments) {
			// LogUtils.getLogger().debug("Checking enchant predicate: " + predicate);
			Stream<Holder<Enchantment>> stream;
			if (predicate.enchantments().isPresent()) {
				stream = predicate.enchantments().get().stream();
			} else {
				stream = registry.stream().map(registry::wrapAsHolder);
			}

			var list = Lists.newArrayList(stream.filter((x) -> x.isBound()).iterator());
			// LogUtils.getLogger().debug("Picking from enchants " + list);
			var weighted = new WeightedSet<>(list, (x) -> 100f / x.get().getAnvilCost());

			Holder<Enchantment> enchant = weighted.get(level.random);

			if (enchant == null)
				throw new IllegalStateException("Could not get enchantment " + this);

			int llower = predicate.level().min().orElse(enchant.get().getMinLevel());
			int lhigher = predicate.level().max().orElse(enchant.get().getMaxLevel());
			int lev;
			if (llower == lhigher)
				lev = llower;
			else {

				lev = RandomUtils.lowBiasedRandomInt(level.random,
						predicate.level().min().orElse(enchant.get().getMinLevel()),
						predicate.level().max().orElse(enchant.get().getMaxLevel()));
			}
			mutable.set(enchant, lev);
		}
		return new TypedDataComponent<ItemEnchantments>(DataComponents.ENCHANTMENTS, mutable.toImmutable());
	}

	@Override
	public ProviderType<EnchantmentsComponentMatcher> providerType() {
		throw new UnsupportedOperationException("Not a real provider");
	}

	private String prettyPrint(EnchantmentPredicate pred) {
		return "Enchant(enchants="
				+ pred.enchantments().map((s) -> Either.<Object>unwrap(s.unwrap()).toString()).orElse("none")
				+ ",levels=" + TextUtils.printMinMaxBounds(pred.level()) + ")";
	}

	@Override
	public String toString() {
		return "(" + PATH + "){"
				+ this.enchantments.stream().map((x) -> prettyPrint(x)).collect(StreamUtils.setStringCollector(","))
				+ "}";
	}

}
