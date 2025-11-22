package com.gm910.sotdivine.language.phonology.phonemes;

import java.util.Map;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A sound in this language
 */
public record Phoneme(String symbol, Map<String, Object> features) {

	public static final Phoneme WORD_BOUNDARY = new Phoneme("#", Map.of());
	public static final Phoneme MORPHEME_BOUNDARY = new Phoneme("+", Map.of());
	public static final Phoneme SYLLABLE_BOUNDARY = new Phoneme(".", Map.of());
	public static final Phoneme ONSET_NUCLEUS_BOUNDARY = new Phoneme("|", Map.of());
	public static final Phoneme NUCLEUS_CODA_BOUNDARY = new Phoneme("]", Map.of());

	public static final Codec<Phoneme> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.validate(
					(s) -> IPhonology.RESERVED_SYMBOLS.contains(s) ? DataResult.error(() -> "reserved symbol", s)
							: DataResult.success(s))
					.fieldOf("phone").forGetter(Phoneme::symbol),
			Codec.unboundedMap(Codec.STRING, CodecUtils.<Object>multiCodecEither(Codec.BOOL, Codec.INT, Codec.STRING))
					.fieldOf("features").forGetter(Phoneme::features))
			.apply(instance, (a, b) -> new Phoneme(a, b)));

	public PhoneSet asSet() {
		return new PhoneSet(this.symbol);
	}

	@Override
	public final String toString() {
		return "/" + symbol + "/";
	}
}
