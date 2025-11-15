package com.gm910.sotdivine.concepts.genres.provider.data.other;

import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.mojang.serialization.Codec;

import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Returns just an nbt tag
 */
public record NbtInternalProvider(CompoundTag tag) implements IGenreProvider<Tag, Tag> {

	public static final Codec<NbtInternalProvider> CODEC = CompoundTag.CODEC.xmap(NbtInternalProvider::new,
			NbtInternalProvider::tag);

	public NbtPredicate asPredicate() {
		return new NbtPredicate(this.tag);
	}

	@Override
	public boolean matches(ServerLevel level, Tag instance) {
		return this.asPredicate().matches(instance);
	}

	@Override
	public Tag generateRandom(ServerLevel level, Optional<Tag> prior) {
		return tag;
	}

	@Override
	public ProviderType<NbtInternalProvider> providerType() {
		throw new UnsupportedOperationException("Not a real provider");
	}

	@Override
	public final String toString() {
		return tag.toString();
	}

	@Override
	public Component translate() {
		return Component.literal(tag.toString());
	}

}
