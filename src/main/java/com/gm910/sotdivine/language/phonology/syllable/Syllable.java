package com.gm910.sotdivine.language.phonology.syllable;

import java.util.ArrayList;
import java.util.List;

import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.gm910.sotdivine.util.CollectionUtils;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

public record Syllable(ListMultimap<SyllablePosition, Phoneme> phonemes) {

	public Syllable(ListMultimap<SyllablePosition, Phoneme> phonemes) {
		this.phonemes = MultimapBuilder.enumKeys(SyllablePosition.class).arrayListValues().build(phonemes);
	}

	public ListMultimap<SyllablePosition, Phoneme> phonemes() {
		return Multimaps.unmodifiableListMultimap(phonemes);
	}

	/**
	 * Return all phonemes in this as a flat list
	 * 
	 * @return
	 */
	public List<Phoneme> flatList() {
		return new ArrayList<>(phonemes.values());
	}

	/**
	 * Returns a list of phoneme with inserted syllable boundary elements, i.e.
	 * {@link Phoneme#ONSET_NUCLEUS_BOUNDARY}, {@link Phoneme#NUCLEUS_CODA_BOUNDARY}
	 * 
	 * @return
	 */
	public List<Phoneme> boundaryMarkedList() {
		return CollectionUtils.concat(phonemes.get(SyllablePosition.ONSET), List.of(Phoneme.ONSET_NUCLEUS_BOUNDARY),
				phonemes.get(SyllablePosition.NUCLEUS), List.of(Phoneme.NUCLEUS_CODA_BOUNDARY),
				phonemes.get(SyllablePosition.CODA));
	}

	public static enum SyllablePosition {
		ONSET, NUCLEUS, CODA
	}

}
