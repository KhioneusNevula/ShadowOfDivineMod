package com.gm910.sotdivine.concepts.genres.provider.entity_preds;

import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.concepts.genres.provider.independent.ItemGenreProvider;
import com.gm910.sotdivine.util.TextUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;

/**
 * A provider which selects for what is in an item frame
 */
public record ItemFrameStack(ItemGenreProvider item) implements ITypeSpecificProvider<ItemFrame> {

	public static final ResourceLocation PATH = ResourceLocation.withDefaultNamespace("item_frame_stack");

	@Override
	public boolean matches(ServerLevel level, ItemFrame instance) {
		return item.matches(level, instance.getItem());
	}

	@Override
	public ItemFrame generateRandom(ServerLevel level, Optional<ItemFrame> prior) {
		prior.orElseThrow().setItem(item.generateRandom(level, Optional.ofNullable(prior.orElseThrow().getItem()))
				.getAsItem(level, BlockPos.ZERO), true);

		return prior.orElseThrow();
	}

	@Override
	public Class<ItemFrame> entityClass() {
		return ItemFrame.class;
	}

	@Override
	public ProviderType<? extends IGenreProvider<ItemFrame, ItemFrame>> providerType() {
		throw new UnsupportedOperationException("Not a real provider");
	}

	@Override
	public ResourceLocation path() {
		return PATH;
	}

	@Override
	public final String toString() {
		return "FrameHas(" + item + ")";
	}

	@Override
	public Component translate() {
		return TextUtils.transPrefix("sotd.genre.provider.frame_stack", item.translate());
	}

}
