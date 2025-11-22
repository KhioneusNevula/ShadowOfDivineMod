package com.gm910.sotdivine.language.phonology.constraints;

import java.util.List;
import java.util.Map;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.google.common.collect.Range;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * A constraint on phonological sequences
 */
public interface IPhonoConstraint {

	public static Map<String, MapCodec<? extends IPhonoConstraint>> CODECS = Map.of(SequenceConstraint.NAME,
			SequenceConstraint.MAP_CODEC, DuplicationConstraint.NAME, MapCodec.unit(DuplicationConstraint.INSTANCE));

	public static final MapCodec<IPhonoConstraint> MAP_CODEC = Codec
			.mapEither(Codec.STRING.<IPhonoConstraint>dispatchMap("type", IPhonoConstraint::constraintType,
					(t) -> CODECS.get(t)), SequenceConstraint.MAP_CODEC)
			.xmap((s) -> Either.unwrap(s), (x) -> Either.left(x));

	/**
	 * Return the type of this constraint
	 * 
	 * @return
	 */
	public String constraintType();

	/**
	 * Return a set of ranges of indices in the given sequence that violate the
	 * given constraint (or an empty set if it passes). Include morpheme and word
	 * boundaries
	 * 
	 * @param phonemes
	 * @return
	 */
	public List<Range<Integer>> sequenceViolation(List<Phoneme> phonemes, IPhonology phonology);
}
