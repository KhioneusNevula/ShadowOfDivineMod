package com.gm910.sotdivine.concepts.genres.other;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * A magic word used by a deity
 */
public record MagicWord(Component translation) {

	public static final Codec<MagicWord> CODEC = ComponentSerialization.CODEC.xmap(MagicWord::new,
			MagicWord::translation);

}
