package com.gm910.sotdivine.language.lexicon;

import com.mojang.serialization.Codec;

public interface ILexicon {

	public static Codec<ILexicon> createCodec() {
		return Codec.unit(new ILexicon() {
		});
	}

}
