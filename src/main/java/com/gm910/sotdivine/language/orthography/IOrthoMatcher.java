package com.gm910.sotdivine.language.orthography;

import java.util.List;
import java.util.Optional;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.language.phonology.phonemes.PhoneSet;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.google.common.collect.Range;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * A constraint on phonological sequences
 */
public interface IOrthoMatcher {

	public static final MapCodec<IOrthoMatcher> MAP_CODEC = RecordCodecBuilder
			.mapCodec(
					instance -> instance
							.group(Codec.list(PhoneSet.CODEC).fieldOf("sequence").forGetter(IOrthoMatcher::sequence),
									Codec.either(Codec.list(Codec.INT, 2, 2),
											Codec.INT.xmap((s) -> List.of(s, s), (l) -> l.getFirst()))
											.xmap((e) -> Either.unwrap(e),
													(e) -> e.getFirst() == e.getLast() ? Either.right(e)
															: Either.left(e))
											.optionalFieldOf("replace")
											.forGetter(
													(o) -> Optional.of(List.of(o.replaceIndices().lowerEndpoint(),
															o.replaceIndices().upperEndpoint()))),
									ComponentSerialization.CODEC.fieldOf("form").forGetter(IOrthoMatcher::surfaceForm))
							.apply(instance,
									(s, li, c) -> new OrthoMatcher(s,
											li.map((r) -> Range.closed(r.getFirst(), r.getLast()))
													.orElse(Range.closed(0, s.size() - 1)),
											c)));

	/**
	 * Return the surface form of this constraint as a text component
	 * 
	 * @return
	 */
	public Component surfaceForm();

	/**
	 * The sequence of phoneSets that this conditionally operates on
	 * 
	 * @return
	 */
	public List<PhoneSet> sequence();

	/**
	 * The range of indices to replace with this symbol in found sequences
	 * 
	 * @return
	 */
	public Range<Integer> replaceIndices();

	/**
	 * Return a collection of indices to be mass-replaced with the character of this
	 * orthographic rule
	 * 
	 * @param phonemes
	 * @return
	 */
	public List<Range<Integer>> findForReplacement(List<Phoneme> phonemes, IPhonology phonology);
}
