package com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.creator.BlockCreator;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.data.ComponentMapProvider;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.data.other.NbtInternalProvider;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.HolderUtils;
import com.gm910.sotdivine.util.StreamUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Provider for a block
 * 
 * @param fluids Relevant if the fluid-logged state of the block is what we
 *               actually want to check
 */
public record BlockGenreProvider(Either<HolderSet<Block>, HolderSet<Fluid>> kind,
		Optional<StatePropertiesPredicate> properties, Optional<NbtInternalProvider> nbt,
		ComponentMapProvider components, float rarity) implements IPlaceableGenreProvider<BlockPos, BlockCreator> {

	private static Codec<BlockGenreProvider> CODEC = null;

	public static Codec<BlockGenreProvider> codec() {
		if (CODEC == null) {

			Codec<BlockGenreProvider> blockRegistryCodec = RegistryCodecs.homogeneousList(Registries.BLOCK)
					.flatComapMap((p) -> new BlockGenreProvider(Either.left(p), Optional.empty(), Optional.empty(),
							ComponentMapProvider.ANY,
							(float) p.stream().map(Holder::get).flatMap((b) -> Optional.ofNullable(b.asItem()).stream())
									.mapToDouble((s) -> s.getDefaultInstance().getRarity().ordinal() + 0.0).min()
									.orElse(3.0)),
							(b) -> {
								if (b.kind().left().isEmpty()) {
									return DataResult.error(() -> "Fluid-kind cannot be converted into Block: " + b);
								}
								return DataResult.error(() -> "No point converting from complex provider into block",
										b.kind.left().get());
							});
			Codec<BlockGenreProvider> fluidRegistryCodec = RegistryCodecs.homogeneousList(Registries.FLUID)
					.flatComapMap((p) -> new BlockGenreProvider(Either.right(p), Optional.empty(), Optional.empty(),
							ComponentMapProvider.ANY, 0f), (b) -> {
								if (b.kind().right().isEmpty()) {
									return DataResult.error(() -> "Block-kind cannot be converted into fluid: " + b);
								}
								return DataResult.error(
										() -> "No point converting from complex provider into simple fluid",
										b.kind.right().get());
							});
			Codec<BlockGenreProvider> blockOrFluidCodec = Codec.either(blockRegistryCodec, fluidRegistryCodec)
					.xmap(Either::unwrap, (s) -> Either.right(s));

			Codec<BlockGenreProvider> constructionCodec = RecordCodecBuilder
					.create((instance) -> instance
							.group(Codec
									.mapEither(RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks"),
											RegistryCodecs.homogeneousList(Registries.FLUID).fieldOf("fluids"))
									.forGetter(BlockGenreProvider::kind),
									StatePropertiesPredicate.CODEC.optionalFieldOf("properties")
											.forGetter(BlockGenreProvider::properties),
									NbtInternalProvider.CODEC.optionalFieldOf("nbt").forGetter(BlockGenreProvider::nbt),

									ComponentMapProvider.codec().forGetter(BlockGenreProvider::components),
									CodecUtils.enumFloatScaleCodec(Rarity.class).optionalFieldOf("rarity", 0.0f)
											.forGetter(BlockGenreProvider::rarity))
							.apply(instance, BlockGenreProvider::new));
			CODEC = Codec.either(blockOrFluidCodec, constructionCodec).xmap(Either::unwrap, (s) -> Either.right(s));
		}
		return CODEC;
	}

	@Override
	public ProviderType<BlockGenreProvider> providerType() {
		return ProviderType.BLOCK;
	}

	@SafeVarargs
	public static BlockGenreProvider fluid(Holder<Fluid>... fluid) {
		return new BlockGenreProvider(Either.right(HolderSet.direct(fluid)), Optional.empty(), Optional.empty(),
				ComponentMapProvider.ANY, 0f);
	}

	@SafeVarargs
	public static BlockGenreProvider block(Holder<Block>... block) {
		return new BlockGenreProvider(Either.left(HolderSet.direct(block)), Optional.empty(), Optional.empty(),
				ComponentMapProvider.ANY,
				(float) Arrays.stream(block).map(Holder::get).flatMap((b) -> Optional.ofNullable(b.asItem()).stream())
						.mapToDouble((s) -> s.getDefaultInstance().getRarity().ordinal() + 0.0).min().orElse(0.0));
	}

	public static BlockGenreProvider block(HolderSet<Block> block) {
		return new BlockGenreProvider(Either.left(block), Optional.empty(), Optional.empty(), ComponentMapProvider.ANY,
				(float) block.stream().map(Holder::get).flatMap((b) -> Optional.ofNullable(b.asItem()).stream())
						.mapToDouble((s) -> s.getDefaultInstance().getRarity().ordinal() + 0.0).min().orElse(0.0));
	}

	public static BlockGenreProvider fluid(HolderSet<Fluid> fluids) {
		return new BlockGenreProvider(Either.right(fluids), Optional.empty(), Optional.empty(),
				ComponentMapProvider.ANY, 0f);
	}

	/**
	 * Check if this matches an item stack
	 * 
	 * @param level
	 * @param stack
	 * @return
	 */
	public boolean matches(ServerLevel level, ItemStack stack) {
		Holder<Item> holder = stack.getItemHolder();
		if (holder.get() instanceof BlockItem blockItem) {
			var stream = this.kind.map((hs) -> hs.stream().filter((h) -> h.isBound()).map(Holder::get),
					(hs) -> hs.stream().filter((h) -> h.isBound()).map(Holder::get)
							.map((f) -> f.defaultFluidState().createLegacyBlock().getBlock()));
			if (!stream.anyMatch(blockItem.getBlock()::equals)) {
				return false;
			}
			BlockState state = null;
			if (this.properties.isPresent()) {
				BlockItemStateProperties stateProps = stack.get(DataComponents.BLOCK_STATE);
				state = stateProps.apply(state);
				if (!this.properties.get().matches(state)) {
					return false;
				}
			}
			if (!this.components.isEmpty() || this.nbt.isPresent()) {
				CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
				if (data == null)
					return false;

				if (!this.components.isEmpty() || this.nbt.isPresent()) {
					if (blockItem.getBlock() instanceof EntityBlock) {
						BlockEntity entity;
						if (state == null) {
							entity = blockItem.getBlock().getStateDefinition().getPossibleStates().stream()
									.flatMap((x) -> Optional.ofNullable(BlockEntity.loadStatic(BlockPos.ZERO, x,
											data.copyTag(), level.registryAccess())).stream())
									.findFirst().orElse(null);
						} else {
							entity = BlockEntity.loadStatic(BlockPos.ZERO, state, data.copyTag(),
									level.registryAccess());
						}
						if (entity == null)
							return false;
						if (!this.components.isEmpty() && !this.components.matches(level, entity.collectComponents())) {
							return false;
						}
						if (!this.nbt.isEmpty() && !this.nbt.get().matches(level,
								entity.saveWithFullMetadata(level.registryAccess()))) {
							return false;
						}
					} else {
						return false;
					}
				}
			}

		} else {
			if (this.kind.right().isPresent()) {
				if (!this.kind.right().get().stream().filter((h) -> h.isBound()).map(Holder::get)
						.anyMatch((f) -> f.getBucket().equals(holder.get()))) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean matches(ServerLevel level, BlockPos pos) {
		if (!level.isLoaded(pos))
			return false;
		BlockState checkState = level.getBlockState(pos);

		if (!Either.unwrap(kind.mapLeft((b) -> HolderUtils.holderSetContains(b, checkState.getBlockHolder()))
				.mapRight((f) -> HolderUtils.holderSetContains(f, level.getFluidState(pos).holder())))) {
			return false;
		}

		if (properties.isPresent()) {
			if (!properties.get().matches(checkState) && !properties.get().matches(level.getFluidState(pos))) {
				return false;
			}
		}
		if (nbt.isPresent() || !components.isEmpty()) {
			BlockEntity entity = level.getBlockEntity(pos);
			if (nbt.isPresent() && !matchesBlockEntity(level, entity, nbt.get())) {
				return false;
			}
			if (!components.isEmpty() && !matchesComponents(level, entity, components)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean matchesPos(ServerLevel level, BlockPos pos) {
		return this.matches(level, pos);
	}

	private static boolean matchesBlockEntity(ServerLevel level, @Nullable BlockEntity entity,
			NbtInternalProvider nbt) {
		return entity != null && nbt.matches(level, entity.saveWithFullMetadata(level.registryAccess()));
	}

	private static boolean matchesComponents(ServerLevel level, @Nullable BlockEntity entity,
			ComponentMapProvider components) {
		return entity != null && components.matches(level, (DataComponentGetter) entity.collectComponents());
	}

	@Override
	public BlockCreator generateRandom(ServerLevel level, Optional<BlockPos> placer) {
		BlockPos pos = placer.orElse(level.players().getFirst().blockPosition());
		Stream<BlockState> states = kind.map((block) -> {
			List<Block> blocks = Lists.newArrayList(block.stream().filter(Holder::isBound).map(Holder::get).iterator());
			Collections.shuffle(blocks);

			return blocks.stream().flatMap((b) -> {
				List<BlockState> posses = new ArrayList<>(b.getStateDefinition().getPossibleStates());
				Collections.shuffle(blocks);
				return posses.stream();
			}).filter((b) -> properties.isEmpty() ? true : properties.get().matches(b));
		}, (fluid) -> {
			return fluid.stream().filter(Holder::isBound).map(Holder::get)
					.flatMap((f) -> f.getStateDefinition().getPossibleStates().stream())
					.filter((f) -> properties.isEmpty() ? true : properties.get().matches(f))
					.map((f) -> f.getFluidType().getBlockForFluidState(level, pos, f));
		}).filter((s) -> nbt.isPresent() || !components.isEmpty() ? s.hasBlockEntity() : true);

		Stream<Pair<BlockState, Pair<Function<BlockEntity, Boolean>, Supplier<ItemStack>>>> output = states
				.map((s) -> Pair.of(s, Pair.of((b) -> true, () -> {

					if (s.getBlock().asItem() == null)
						return ItemStack.EMPTY;

					ItemStack stack = new ItemStack(s.getBlock().asItem());
					if (!s.equals(s.getBlock().defaultBlockState())) {
						BlockItemStateProperties iprops = new BlockItemStateProperties(Map.of());
						for (Property<?> prop : s.getProperties()) {
							iprops = iprops.with(prop, s);
						}
						stack.set(DataComponents.BLOCK_STATE, iprops);
					}
					return stack;

				})));

		if (!components.isEmpty() || nbt.isPresent()) {
			output = output.flatMap((p) -> {
				BlockState s = p.getFirst();
				EntityBlock b = (EntityBlock) s.getBlock();

				Function<BlockPos, BlockEntity> supplier = (pos2) -> {
					BlockEntity be = b.newBlockEntity(pos2, s);
					if (be == null)
						return null;

					if (!components.isEmpty()) {
						be.applyComponents(components.generateRandom(level, Optional.of(be.collectComponents())),
								DataComponentPatch.EMPTY);
					}
					if (nbt.isPresent()) {
						be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
								nbt.get().tag()));

					}
					return be;
				};

				Supplier<ItemStack> createItem = () -> {
					ItemStack stack = p.getSecond().getSecond().get();
					if (!components.isEmpty() || nbt.isPresent()) {
						BlockEntity e = supplier.apply(pos);
						stack.set(DataComponents.BLOCK_ENTITY_DATA,
								CustomData.of(e.saveWithoutMetadata(level.registryAccess())));
					}

					return stack;
				};

				if (supplier.apply(pos) != null) {
					return Optional.<Pair<BlockState, Pair<Function<BlockEntity, Boolean>, Supplier<ItemStack>>>>of(
							Pair.of(s, Pair.of((be) -> {
								BlockEntity e = supplier.apply(be.getBlockPos());
								if (e == null)
									return false;
								be.getLevel().setBlockEntity(e);
								return true;
							}, createItem))).stream();
				}

				return Optional.<Pair<BlockState, Pair<Function<BlockEntity, Boolean>, Supplier<ItemStack>>>>empty()
						.stream();
			});
		}
		var opOut = output.findAny();
		if (opOut.isEmpty())
			return null;

		BlockState state = opOut.get().getFirst();
		if (this.kind.right().isEmpty() && (this.properties.isEmpty() || !this.properties.get().matches(state))) {
			state = state.trySetValue(BlockStateProperties.WATERLOGGED, false);
		}
		var postPlacement = opOut.get().getSecond();

		return new BlockCreator(state, postPlacement.getFirst(), postPlacement.getSecond());
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof BlockGenreProvider bgp) {
			return Either
					.unwrap(Either.unwrap(this.kind.mapLeft(
							(b) -> bgp.kind.mapLeft((b2) -> HolderUtils.holderSetEquals(b, b2)).mapRight((f2) -> false))
							.mapRight((f) -> bgp.kind.mapLeft((b2) -> false)
									.mapRight((f2) -> HolderUtils.holderSetEquals(f, f2)))))
					&& this.properties.equals(bgp.properties) && this.nbt.equals(bgp.nbt);
		}

		return false;
	}

	@Override
	public final int hashCode() {
		return kind.map(HolderUtils::holderSetHashCode, HolderUtils::holderSetHashCode) + properties.hashCode()
				+ nbt.hashCode();
	}

	@Override
	public final String toString() {
		return "" + (kind.right().isPresent()
				? "Fluid{" + kind.right().get().unwrap().map((t) -> "#" + t.location(),
						(t) -> t.stream().map((g) -> BuiltInRegistries.FLUID.getId(g.get()))
								.collect(StreamUtils.setStringCollector(",")))
				: "Block{" + kind.left().get().unwrap().map((t) -> "#" + t.location(),
						(t) -> t.stream().map((g) -> BuiltInRegistries.BLOCK.getId(g.get()))
								.collect(StreamUtils.setStringCollector(","))))
				+ "}";
	}

	@Override
	public String report() {
		return "GenreProvider"
				+ (kind.right().isPresent()
						? "(Fluid){set=" + kind.right().get().unwrap().map(Object::toString, Object::toString)
						: "(Block){set=" + kind.left().get().unwrap().map(Object::toString, Object::toString))
				+ (properties.isEmpty() ? "" : ",properties=" + properties.get())
				+ (nbt.isEmpty() ? "" : ",nbt=" + nbt.get()) + "}";
	}

}
